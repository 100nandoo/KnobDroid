package dev.halim.knobdroid.usb

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import java.util.concurrent.Executors

object VolumeActionHelper {
    private const val TAG = "VolumeActionHelper"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun applyVolume(
        context: Context,
        usbManager: UsbManager,
        sharedPreferences: SharedPreferences,
        requestedVolume: Int? = null,
        onComplete: () -> Unit = {}
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Audio permission not granted")
            onComplete()
            return
        }

        val volumePercent = requestedVolume ?: run {
            val enabled = sharedPreferences.getBoolean(AppConstants.PreferenceKeys.VOLUME_ENABLED, AppConstants.PreferenceKeys.DEFAULT_VOLUME_ENABLED)
            if (enabled) 100 else 0
        }

        if (requestedVolume != null) {
            sharedPreferences.edit { putBoolean(AppConstants.PreferenceKeys.VOLUME_ENABLED, volumePercent == 100) }
        }

        executor.execute {
            val result = VolumeController.execute(usbManager, volumePercent)
            val appContext = context.applicationContext

            mainHandler.post {
                when (result) {
                    is VolumeController.Result.Success -> {
                        Log.d(TAG, "Connected to device and applied volume: ${result.deviceName}")
                        showToast(appContext, appContext.getString(R.string.toast_volume_success))
                    }
                    is VolumeController.Result.DeviceNotFound -> {
                        Log.w(TAG, "Apple USB DAC not found")
                        showToast(appContext, appContext.getString(R.string.toast_apple_dac_not_found))
                    }
                    is VolumeController.Result.Error -> {
                        Log.e(TAG, "Error applying volume: ${result.message}")
                        showToast(appContext, appContext.getString(R.string.toast_error_prefix, result.message))
                    }
                    is VolumeController.Result.VolumeError -> {
                        Log.e(TAG, "Volume error: ${result.code}")
                        showToast(appContext, appContext.getString(R.string.toast_volume_error, result.code))
                    }
                }
                onComplete()
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
