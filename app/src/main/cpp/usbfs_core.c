#include "usbfs_core.h"

#include <errno.h>
#include <linux/usbdevice_fs.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>

static void log_append(usbfs_unlock_result_t *res, size_t *pos, const char *fmt, ...) {
    if (*pos >= USBFS_LOG_BUF_SIZE - 1) {
        return;
    }
    va_list ap;
    va_start(ap, fmt);
    int n = vsnprintf(res->log_buf + *pos, USBFS_LOG_BUF_SIZE - *pos, fmt, ap);
    va_end(ap);
    if (n > 0) {
        size_t added = (size_t)n;
        if (*pos + added >= USBFS_LOG_BUF_SIZE) {
            *pos = USBFS_LOG_BUF_SIZE - 1;
        } else {
            *pos += added;
        }
    }
}

static int default_ioctl_fn(int fd, unsigned long req, void *arg) {
#if defined(__ANDROID__)
    return ioctl(fd, (unsigned int)req, arg);
#else
    return ioctl(fd, req, arg);
#endif
}

static int default_nanosleep_fn(const struct timespec *req, struct timespec *rem) {
    return nanosleep(req, rem);
}

static int ioctl_retry(const usbfs_ops_t *ops, int fd, unsigned long req, void *arg, int *out_errno) {
    int rc;
    int saved_errno = 0;
    do {
        errno = 0;
        rc = ops->ioctl_fn(fd, req, arg);
        saved_errno = (rc < 0) ? errno : 0;
    } while (rc < 0 && saved_errno == EINTR);
    if (out_errno != NULL) {
        *out_errno = saved_errno;
    }
    return rc;
}

static void sleep_ms_uninterrupted(const usbfs_ops_t *ops, int ms) {
    if (ms <= 0) {
        return;
    }
    struct timespec req;
    struct timespec rem;
    req.tv_sec = ms / 1000;
    req.tv_nsec = (long)(ms % 1000) * 1000000L;
    while (true) {
        errno = 0;
        if (ops->nanosleep_fn(&req, &rem) == 0) {
            break;
        }
        if (errno != EINTR) {
            break;
        }
        req = rem;
    }
}

int32_t usbfs_clamp_volume_raw(int32_t raw) {
    if (raw < KNOB_VOL_MIN_RAW) {
        return KNOB_VOL_MIN_RAW;
    }
    if (raw > KNOB_VOL_MAX_RAW) {
        return KNOB_VOL_MAX_RAW;
    }
    return raw;
}

void usbfs_encode_volume_le(int32_t clamped_raw, uint8_t out_le[2]) {
    int32_t safe = usbfs_clamp_volume_raw(clamped_raw);
    uint16_t u = (uint16_t)(int16_t)safe;
    out_le[0] = (uint8_t)(u & 0xFFu);
    out_le[1] = (uint8_t)((u >> 8) & 0xFFu);
}

int16_t usbfs_decode_volume_le(const uint8_t in_le[2]) {
    uint16_t u = (uint16_t)in_le[0] | (uint16_t)((uint16_t)in_le[1] << 8);
    return (int16_t)u;
}

static int send_uac_ctrl(
        const usbfs_ops_t *ops,
        int fd,
        uint8_t req_type,
        uint8_t req,
        uint16_t wValue,
        uint16_t wIndex,
        uint8_t *data,
        uint16_t wLength,
        int *out_errno) {
    struct usbdevfs_ctrltransfer ctrl;
    memset(&ctrl, 0, sizeof(ctrl));
    ctrl.bRequestType = req_type;
    ctrl.bRequest = req;
    ctrl.wValue = wValue;
    ctrl.wIndex = wIndex;
    ctrl.wLength = wLength;
    ctrl.timeout = UAC_CTRL_TIMEOUT_MS;
    ctrl.data = data;
    return ioctl_retry(ops, fd, (unsigned long)USBDEVFS_CONTROL, &ctrl, out_errno);
}

static bool read_channel_volume(
        const usbfs_ops_t *ops,
        int fd,
        uint16_t wValue,
        uint8_t *cached_req,
        int16_t *out_raw,
        usbfs_unlock_result_t *res,
        size_t *log_pos,
        const char *label) {
    uint8_t candidates[2];
    if (*cached_req == UAC_REQ_GET_CUR_UAC1) {
        candidates[0] = UAC_REQ_GET_CUR_UAC1;
        candidates[1] = UAC_REQ_CUR;
    } else {
        candidates[0] = UAC_REQ_CUR;
        candidates[1] = UAC_REQ_GET_CUR_UAC1;
    }

    for (int i = 0; i < 2; i++) {
        uint8_t req_code = candidates[i];
        uint8_t buf[2] = { 0, 0 };
        int err = 0;
        int rc = send_uac_ctrl(
                ops,
                fd,
                UAC_REQ_TYPE_IN,
                req_code,
                wValue,
                UAC_WINDEX_FU2_IFACE0,
                buf,
                2,
                &err);
        if (rc == 2) {
            *cached_req = req_code;
            *out_raw = usbfs_decode_volume_le(buf);
            double db = (double)(*out_raw) / 256.0;
            log_append(res, log_pos,
                    "read %s (bReq=0x%02X): ok raw=%d (0x%04X, %.2f dB)\n",
                    label, req_code, (int)(*out_raw), (unsigned int)(uint16_t)(*out_raw), db);
            return true;
        }
        log_append(res, log_pos,
                "read %s (bReq=0x%02X): rc=%d errno=%d\n",
                label, req_code, rc, err);
    }
    return false;
}

static int driver_ioctl(
        const usbfs_ops_t *ops,
        int fd,
        int iface,
        int ioctl_code,
        int *out_errno) {
    struct usbdevfs_ioctl cmd;
    memset(&cmd, 0, sizeof(cmd));
    cmd.ifno = iface;
    cmd.ioctl_code = ioctl_code;
    cmd.data = NULL;
    return ioctl_retry(ops, fd, (unsigned long)USBDEVFS_IOCTL, &cmd, out_errno);
}

void usbfs_apply_gain_with_ops(
        int fd,
        int32_t requested_raw,
        uint8_t *cached_get_req_inout,
        const usbfs_ops_t *ops,
        usbfs_unlock_result_t *out_result) {
    if (out_result == NULL) {
        return;
    }
    memset(out_result, 0, sizeof(*out_result));
    out_result->claim_rc = -1;
    out_result->unmute_rc = -1;
    out_result->left_rc = -1;
    out_result->right_rc = -1;
    out_result->release_rc = -1;
    out_result->connect0_rc = -1;
    out_result->connect3_rc = -1;
    out_result->connect4_rc = -1;

    size_t log_pos = 0;
    const int32_t target_raw = usbfs_clamp_volume_raw(requested_raw);
    out_result->target_raw = target_raw;

    uint8_t local_cached_req = (cached_get_req_inout != NULL) ? *cached_get_req_inout : 0u;
    out_result->cached_get_req = local_cached_req;

    if (fd < 0 || ops == NULL || ops->ioctl_fn == NULL || ops->nanosleep_fn == NULL) {
        log_append(out_result, &log_pos, "abort: invalid fd=%d or ops\n", fd);
        return;
    }

    uint8_t vol_payload[2];
    usbfs_encode_volume_le(target_raw, vol_payload);
    log_append(out_result, &log_pos,
            "target: %d (%.2f dB) payload={0x%02X, 0x%02X}\n",
            (int)target_raw,
            (double)target_raw / 256.0,
            vol_payload[0],
            vol_payload[1]);

    for (int iface = KNOB_NUM_INTERFACES - 1; iface >= 0; iface--) {
        struct usbdevfs_getdriver drv;
        memset(&drv, 0, sizeof(drv));
        drv.interface = (unsigned int)iface;
        int drv_err = 0;
        int drv_rc = ioctl_retry(ops, fd, (unsigned long)USBDEVFS_GETDRIVER, &drv, &drv_err);
        if (drv_rc == 0) {
            drv.driver[USBDEVFS_MAXDRIVERNAME] = '\0';
            log_append(out_result, &log_pos, "iface %d driver: %s\n", iface, drv.driver);
        } else {
            log_append(out_result, &log_pos,
                    "iface %d driver: <none> (rc=%d errno=%d)\n", iface, drv_rc, drv_err);
        }

        int disc_err = 0;
        int disc_rc = driver_ioctl(ops, fd, iface, (int)USBDEVFS_DISCONNECT, &disc_err);
        log_append(out_result, &log_pos,
                "disconnect iface %d: rc=%d errno=%d\n", iface, disc_rc, disc_err);
    }

    unsigned int iface0 = KNOB_IFACE_AC;
    out_result->claim_rc = ioctl_retry(
            ops,
            fd,
            (unsigned long)USBDEVFS_CLAIMINTERFACE,
            &iface0,
            &out_result->claim_errno);
    log_append(out_result, &log_pos,
            "claim iface 0: rc=%d errno=%d\n",
            out_result->claim_rc,
            out_result->claim_errno);

    if (out_result->claim_rc >= 0) {
        out_result->pre_left_ok = read_channel_volume(
                ops, fd, UAC_WVALUE_VOL_LEFT, &local_cached_req,
                &out_result->pre_left_raw, out_result, &log_pos, "pre-L");
        out_result->pre_right_ok = read_channel_volume(
                ops, fd, UAC_WVALUE_VOL_RIGHT, &local_cached_req,
                &out_result->pre_right_raw, out_result, &log_pos, "pre-R");

        uint8_t unmute_data[1] = { 0x00u };
        out_result->unmute_rc = send_uac_ctrl(
                ops,
                fd,
                UAC_REQ_TYPE_OUT,
                UAC_REQ_CUR,
                UAC_WVALUE_MUTE_MASTER,
                UAC_WINDEX_FU2_IFACE0,
                unmute_data,
                1,
                &out_result->unmute_errno);
        log_append(out_result, &log_pos,
                "unmute master: rc=%d errno=%d (best-effort)\n",
                out_result->unmute_rc,
                out_result->unmute_errno);

        out_result->left_rc = send_uac_ctrl(
                ops,
                fd,
                UAC_REQ_TYPE_OUT,
                UAC_REQ_CUR,
                UAC_WVALUE_VOL_LEFT,
                UAC_WINDEX_FU2_IFACE0,
                vol_payload,
                2,
                &out_result->left_errno);
        log_append(out_result, &log_pos,
                "write L (0x0201): rc=%d errno=%d\n",
                out_result->left_rc,
                out_result->left_errno);

        if (out_result->left_rc == 2) {
            out_result->right_rc = send_uac_ctrl(
                    ops,
                    fd,
                    UAC_REQ_TYPE_OUT,
                    UAC_REQ_CUR,
                    UAC_WVALUE_VOL_RIGHT,
                    UAC_WINDEX_FU2_IFACE0,
                    vol_payload,
                    2,
                    &out_result->right_errno);
            log_append(out_result, &log_pos,
                    "write R (0x0202): rc=%d errno=%d\n",
                    out_result->right_rc,
                    out_result->right_errno);
        }

        out_result->write_succeeded = (out_result->left_rc == 2 && out_result->right_rc == 2);

        out_result->post_left_ok = read_channel_volume(
                ops, fd, UAC_WVALUE_VOL_LEFT, &local_cached_req,
                &out_result->post_left_raw, out_result, &log_pos, "post-L");
        out_result->post_right_ok = read_channel_volume(
                ops, fd, UAC_WVALUE_VOL_RIGHT, &local_cached_req,
                &out_result->post_right_raw, out_result, &log_pos, "post-R");

        unsigned int rel_iface0 = KNOB_IFACE_AC;
        out_result->release_rc = ioctl_retry(
                ops,
                fd,
                (unsigned long)USBDEVFS_RELEASEINTERFACE,
                &rel_iface0,
                &out_result->release_errno);
        log_append(out_result, &log_pos,
                "release iface 0: rc=%d errno=%d\n",
                out_result->release_rc,
                out_result->release_errno);
    }

    log_append(out_result, &log_pos, "sleep %d ms before connect iface 0\n",
            REATTACH_DELAY_BEFORE_AUDIO_MS);
    sleep_ms_uninterrupted(ops, REATTACH_DELAY_BEFORE_AUDIO_MS);

    out_result->connect0_rc = driver_ioctl(
            ops, fd, KNOB_IFACE_AC, (int)USBDEVFS_CONNECT, &out_result->connect0_errno);
    log_append(out_result, &log_pos,
            "connect iface 0 (snd-usb-audio): rc=%d errno=%d\n",
            out_result->connect0_rc,
            out_result->connect0_errno);

    log_append(out_result, &log_pos, "sleep %d ms before connect iface 3,4\n",
            REATTACH_DELAY_BEFORE_HID_MS);
    sleep_ms_uninterrupted(ops, REATTACH_DELAY_BEFORE_HID_MS);

    out_result->connect3_rc = driver_ioctl(
            ops, fd, KNOB_IFACE_HID_3, (int)USBDEVFS_CONNECT, &out_result->connect3_errno);
    log_append(out_result, &log_pos,
            "connect iface 3 (usbhid): rc=%d errno=%d\n",
            out_result->connect3_rc,
            out_result->connect3_errno);

    out_result->connect4_rc = driver_ioctl(
            ops, fd, KNOB_IFACE_HID_4, (int)USBDEVFS_CONNECT, &out_result->connect4_errno);
    log_append(out_result, &log_pos,
            "connect iface 4 (usbhid): rc=%d errno=%d\n",
            out_result->connect4_rc,
            out_result->connect4_errno);

    out_result->cached_get_req = local_cached_req;
    if (cached_get_req_inout != NULL) {
        *cached_get_req_inout = local_cached_req;
    }
}

void usbfs_apply_gain(
        int fd,
        int32_t requested_raw,
        uint8_t *cached_get_req_inout,
        usbfs_unlock_result_t *out_result) {
    static const usbfs_ops_t kDefaultOps = {
        .ioctl_fn = default_ioctl_fn,
        .nanosleep_fn = default_nanosleep_fn
    };
    usbfs_apply_gain_with_ops(fd, requested_raw, cached_get_req_inout, &kDefaultOps, out_result);
}
