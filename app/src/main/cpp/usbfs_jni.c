#include "usbfs_core.h"

#include <android/log.h>
#include <jni.h>
#include <stdatomic.h>

#define LOG_TAG "KnobDroidUsbfs"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/* Remembers whether the adapter answered UAC2 CUR (0x01) or UAC1 GET_CUR (0x81). */
static _Atomic uint8_t g_cached_get_req = 0u;

/*
 * Layout of the status array filled for NativeUsbLib.applyVolume().
 * Keep in sync with NativeUsbLib.kt.
 */
enum {
    STATUS_WRITE_SUCCEEDED = 0,
    STATUS_TARGET_RAW,
    STATUS_CLAIM_RC,
    STATUS_CLAIM_ERRNO,
    STATUS_LEFT_RC,
    STATUS_LEFT_ERRNO,
    STATUS_RIGHT_RC,
    STATUS_RIGHT_ERRNO,
    STATUS_POST_LEFT_OK,
    STATUS_POST_LEFT_RAW,
    STATUS_POST_RIGHT_OK,
    STATUS_POST_RIGHT_RAW,
    STATUS_CONNECT0_RC,
    STATUS_COUNT
};

JNIEXPORT jstring JNICALL
Java_dev_halim_knobdroid_usb_NativeUsbLib_applyVolume(
        JNIEnv *env,
        jobject thiz,
        jint fd,
        jint targetRaw,
        jintArray outStatus) {
    (void)thiz;

    uint8_t cached_req = atomic_load(&g_cached_get_req);
    usbfs_unlock_result_t res;
    usbfs_apply_gain((int)fd, (int32_t)targetRaw, &cached_req, &res);
    if (cached_req != 0u) {
        atomic_store(&g_cached_get_req, cached_req);
    }

    LOGI("applyVolume fd=%d target=%d writeSucceeded=%d claimRc=%d leftRc=%d rightRc=%d "
         "conn0=%d conn3=%d conn4=%d\n%s",
         (int)fd,
         (int)res.target_raw,
         res.write_succeeded ? 1 : 0,
         res.claim_rc,
         res.left_rc,
         res.right_rc,
         res.connect0_rc,
         res.connect3_rc,
         res.connect4_rc,
         res.log_buf);

    if (outStatus != NULL && (*env)->GetArrayLength(env, outStatus) >= STATUS_COUNT) {
        jint status[STATUS_COUNT];
        status[STATUS_WRITE_SUCCEEDED] = res.write_succeeded ? 1 : 0;
        status[STATUS_TARGET_RAW] = res.target_raw;
        status[STATUS_CLAIM_RC] = res.claim_rc;
        status[STATUS_CLAIM_ERRNO] = res.claim_errno;
        status[STATUS_LEFT_RC] = res.left_rc;
        status[STATUS_LEFT_ERRNO] = res.left_errno;
        status[STATUS_RIGHT_RC] = res.right_rc;
        status[STATUS_RIGHT_ERRNO] = res.right_errno;
        status[STATUS_POST_LEFT_OK] = res.post_left_ok ? 1 : 0;
        status[STATUS_POST_LEFT_RAW] = res.post_left_raw;
        status[STATUS_POST_RIGHT_OK] = res.post_right_ok ? 1 : 0;
        status[STATUS_POST_RIGHT_RAW] = res.post_right_raw;
        status[STATUS_CONNECT0_RC] = res.connect0_rc;
        (*env)->SetIntArrayRegion(env, outStatus, 0, STATUS_COUNT, status);
    }

    return (*env)->NewStringUTF(env, res.log_buf);
}
