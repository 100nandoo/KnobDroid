package dev.halim.knobdroid.usb

import android.util.Log

object NativeUsbLib {
  private const val TAG = "NativeUsbLib"

  // Indices into the status array filled by applyVolume(); keep in sync with usbfs_jni.c.
  private const val STATUS_WRITE_SUCCEEDED = 0
  private const val STATUS_TARGET_RAW = 1
  private const val STATUS_CLAIM_RC = 2
  private const val STATUS_CLAIM_ERRNO = 3
  private const val STATUS_LEFT_RC = 4
  private const val STATUS_LEFT_ERRNO = 5
  private const val STATUS_RIGHT_RC = 6
  private const val STATUS_RIGHT_ERRNO = 7
  private const val STATUS_POST_LEFT_OK = 8
  private const val STATUS_POST_LEFT_RAW = 9
  private const val STATUS_POST_RIGHT_OK = 10
  private const val STATUS_POST_RIGHT_RAW = 11
  private const val STATUS_CONNECT0_RC = 12
  private const val STATUS_COUNT = 13

  init {
    try {
      System.loadLibrary("knobdroid_usbfs")
      Log.d(TAG, "Native library loaded successfully")
    } catch (e: UnsatisfiedLinkError) {
      Log.e(TAG, "Failed to load native library: ${e.message}")
      throw e
    }
  }

  data class ApplyResult(
    val writeSucceeded: Boolean,
    val targetRaw: Int,
    val claimRc: Int,
    val claimErrno: Int,
    val leftRc: Int,
    val leftErrno: Int,
    val rightRc: Int,
    val rightErrno: Int,
    /** Volume read back from the left channel after the write, or null if unreadable. */
    val postLeftRaw: Int?,
    /** Volume read back from the right channel after the write, or null if unreadable. */
    val postRightRaw: Int?,
    /** Result of re-attaching snd-usb-audio to interface 0 (0 on success). */
    val connect0Rc: Int,
    val log: String,
  ) {
    /** The first errno from the step that failed, for user-facing error messages. */
    val failureErrno: Int
      get() =
        when {
          claimRc < 0 -> claimErrno
          leftRc != 2 -> leftErrno
          rightRc != 2 -> rightErrno
          else -> 0
        }
  }

  /**
   * Detaches the kernel drivers, writes [targetRaw] (1/256 dB, clamped to -30..0 dB) to both
   * channels of Feature Unit 2 and re-attaches the drivers. Blocks for roughly 600 ms.
   */
  fun applyVolume(fd: Int, targetRaw: Int): ApplyResult {
    val status = IntArray(STATUS_COUNT)
    val log = applyVolume(fd, targetRaw, status)
    return ApplyResult(
      writeSucceeded = status[STATUS_WRITE_SUCCEEDED] != 0,
      targetRaw = status[STATUS_TARGET_RAW],
      claimRc = status[STATUS_CLAIM_RC],
      claimErrno = status[STATUS_CLAIM_ERRNO],
      leftRc = status[STATUS_LEFT_RC],
      leftErrno = status[STATUS_LEFT_ERRNO],
      rightRc = status[STATUS_RIGHT_RC],
      rightErrno = status[STATUS_RIGHT_ERRNO],
      postLeftRaw = status[STATUS_POST_LEFT_RAW].takeIf { status[STATUS_POST_LEFT_OK] != 0 },
      postRightRaw = status[STATUS_POST_RIGHT_RAW].takeIf { status[STATUS_POST_RIGHT_OK] != 0 },
      connect0Rc = status[STATUS_CONNECT0_RC],
      log = log,
    )
  }

  private external fun applyVolume(fd: Int, targetRaw: Int, status: IntArray): String
}
