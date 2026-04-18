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
            if (device != null) {
                Log.d(TAG, "Found dongle: ${device.deviceName}")
                var volResult: Result = Result.Error("Unknown error")
                
                val connectionResult = UsbHelper.connectAndDo(device, usbManager) { fd ->
                    val status = sendVolumeToDevice(fd, volumePercent)
                    volResult = if (status >= 0) {
                        Result.Success("") // Temporary, will be filled from connectionResult
                    } else {
                        Result.VolumeError(status)
                    }
                }

                connectionResult.fold(
                    onSuccess = { deviceName ->
                        if (volResult is Result.Success) {
                            Result.Success(deviceName)
                        } else {
                            volResult
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to connect or apply volume", e)
                        Result.Error(e.message ?: "Unknown")
                    }
                )
            } else {
                Log.w(TAG, "Apple USB DAC not found")
                Result.DeviceNotFound
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during volume apply", e)
            Result.Error(e.message ?: "Unknown")
        }
    }

    fun sendVolumeToDevice(fd: Int, volumePercent: Int): Int {
        val hexValue = UsbHelper.calculateVolumeHex(volumePercent)
        val lowByte = (hexValue and 0xFF).toByte()
        val highByte = ((hexValue shr 8) and 0xFF).toByte()
        val volumeBytes = byteArrayOf(lowByte, highByte)

        return NativeUsbLib.setDeviceVolume(fd, volumeBytes)
    }
}
