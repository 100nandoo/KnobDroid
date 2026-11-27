package dev.halim.knobdroid.usb

import android.hardware.usb.UsbManager
import android.util.Log

object VolumeController {
    private const val TAG = "VolumeController"

    sealed class Result {
        data class Success(val deviceName: String) : Result()
        object DeviceNotFound : Result()
        data class Error(val message: String) : Result()
        data class VolumeError(val code: Int) : Result()
    }

    fun execute(usbManager: UsbManager, volumePercent: Int): Result {
        return try {
            Log.d(TAG, "Starting USB volume apply")
            val device = UsbHelper.findAppleDongle(usbManager)
            if (device == null) {
                Log.w(TAG, "Apple USB DAC not found")
                return Result.DeviceNotFound
            }
            Log.d(TAG, "Found dongle: ${device.deviceName}")

            val targetRaw = UsbHelper.volumePercentToRaw(volumePercent)
            UsbHelper.connectAndDo(device, usbManager) { fd ->
                NativeUsbLib.applyVolume(fd, targetRaw)
            }.fold(
                onSuccess = { applyResult ->
                    Log.d(TAG, "Native apply log:\n${applyResult.log}")
                    if (applyResult.writeSucceeded) {
                        Log.d(
                            TAG,
                            "Applied raw=${applyResult.targetRaw} readback " +
                                "L=${applyResult.postLeftRaw} R=${applyResult.postRightRaw} " +
                                "reattach=${applyResult.connect0Rc}"
                        )
                        Result.Success(device.productName ?: device.deviceName)
                    } else {
                        Log.e(TAG, "Volume write failed, errno=${applyResult.failureErrno}")
                        Result.VolumeError(applyResult.failureErrno)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to connect or apply volume", e)
                    Result.Error(e.message ?: "Unknown")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during volume apply", e)
            Result.Error(e.message ?: "Unknown")
        }
    }
}
