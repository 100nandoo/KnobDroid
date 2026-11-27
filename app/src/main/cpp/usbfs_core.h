/*
 * Hardware volume unlock for the Apple USB-C to 3.5 mm adapter (05AC:110A)
 * using raw Linux usbfs ioctls on the fd from UsbDeviceConnection.
 *
 * Sequence:
 *   1. Disconnect the kernel drivers from every interface (4..0) so that
 *      snd-usb-audio fully releases the device (ifaces 0-2) along with
 *      usbhid (ifaces 3-4).
 *   2. Claim interface 0, clear the master mute (best effort) and send UAC
 *      SET_CUR Volume (L and R) to Feature Unit 2 with an in-range value
 *      (0 dB is the adapter's maximum), reading the value back around it.
 *   3. Release interface 0 and re-attach snd-usb-audio and usbhid with
 *      USBDEVFS_CONNECT.
 *
 * The device is deliberately never reset: a USB bus reset can put the
 * adapter back to its power-on defaults (the -20 dB attenuation this is
 * meant to undo), so drivers are re-attached with USBDEVFS_CONNECT instead.
 *
 * Based on the usbfs sequence from DAC Gain (public domain, Unlicense).
 */

#ifndef KNOBDROID_USBFS_CORE_H
#define KNOBDROID_USBFS_CORE_H

#include <stdbool.h>
#include <stdint.h>
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

#define KNOB_NUM_INTERFACES          5
#define KNOB_IFACE_AC                0
#define KNOB_IFACE_HID_3             3
#define KNOB_IFACE_HID_4             4

#define KNOB_VOL_MIN_RAW             (-7680)
#define KNOB_VOL_MAX_RAW             (0)
#define KNOB_VOL_STOCK_DEFAULT_RAW   (-5120)

#define UAC_REQ_TYPE_OUT                 0x21u
#define UAC_REQ_TYPE_IN                  0xA1u
#define UAC_REQ_CUR                      0x01u
#define UAC_REQ_GET_CUR_UAC1             0x81u
#define UAC_WVALUE_MUTE_MASTER           0x0100u
#define UAC_WVALUE_VOL_LEFT              0x0201u
#define UAC_WVALUE_VOL_RIGHT             0x0202u
#define UAC_WINDEX_FU2_IFACE0            0x0200u
#define UAC_CTRL_TIMEOUT_MS              1000u

#define REATTACH_DELAY_BEFORE_AUDIO_MS   350
#define REATTACH_DELAY_BEFORE_HID_MS     250

#define USBFS_LOG_BUF_SIZE               2048

typedef int (*usbfs_ioctl_fn)(int fd, unsigned long req, void *arg);
typedef int (*usbfs_nanosleep_fn)(const struct timespec *req, struct timespec *rem);

typedef struct {
    usbfs_ioctl_fn ioctl_fn;
    usbfs_nanosleep_fn nanosleep_fn;
} usbfs_ops_t;

typedef struct {
    bool write_succeeded;
    int32_t target_raw;

    int claim_rc;
    int claim_errno;

    int unmute_rc;
    int unmute_errno;

    int left_rc;
    int left_errno;

    int right_rc;
    int right_errno;

    int release_rc;
    int release_errno;

    int connect0_rc;
    int connect0_errno;
    int connect3_rc;
    int connect3_errno;
    int connect4_rc;
    int connect4_errno;

    bool pre_left_ok;
    int16_t pre_left_raw;
    bool pre_right_ok;
    int16_t pre_right_raw;

    bool post_left_ok;
    int16_t post_left_raw;
    bool post_right_ok;
    int16_t post_right_raw;

    uint8_t cached_get_req;

    char log_buf[USBFS_LOG_BUF_SIZE];
} usbfs_unlock_result_t;

int32_t usbfs_clamp_volume_raw(int32_t raw);

void usbfs_encode_volume_le(int32_t clamped_raw, uint8_t out_le[2]);

int16_t usbfs_decode_volume_le(const uint8_t in_le[2]);

void usbfs_apply_gain_with_ops(
        int fd,
        int32_t requested_raw,
        uint8_t *cached_get_req_inout,
        const usbfs_ops_t *ops,
        usbfs_unlock_result_t *out_result);

void usbfs_apply_gain(
        int fd,
        int32_t requested_raw,
        uint8_t *cached_get_req_inout,
        usbfs_unlock_result_t *out_result);

#ifdef __cplusplus
}
#endif

#endif
