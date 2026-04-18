package dev.halim.knobdroid

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.halim.knobdroid.ui.screen.UsbControlScreen
import dev.halim.knobdroid.ui.theme.KnobDroidTheme
import dev.halim.knobdroid.usb.NativeUsbLib
import dev.halim.knobdroid.usb.UsbHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var usbManager: UsbManager
  private lateinit var executorService: ExecutorService

  companion object {
    private const val TAG = "MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    sharedPreferences = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
    usbManager = getSystemService(USB_SERVICE) as UsbManager
    executorService = Executors.newSingleThreadExecutor()

    setContent {
      KnobDroidTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          UsbControlScreen(
            modifier = Modifier.padding(innerPadding),
            sharedPreferences = sharedPreferences,
            onApplyVolume = { volumePercent -> applyVolume(volumePercent, false) },
          )
        }
      }
    }

    handleUsbIntent(intent, isNewActivity = true)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleUsbIntent(intent, isNewActivity = false)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (::executorService.isInitialized) {
      executorService.shutdown()
    }
  }

  private fun handleUsbIntent(intent: Intent, isNewActivity: Boolean) {
    if (intent.action == AppConstants.IntentActions.USB_DEVICE_ATTACHED) {
      val volumePercent = sharedPreferences.getInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, 50)
      applyVolume(volumePercent, isNewActivity)
    }
  }

  private fun applyVolume(volumePercent: Int, finishOnComplete: Boolean) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED) {
      if (finishOnComplete) finishAndRemoveTask()
      return
    }

    sharedPreferences.edit { putInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, volumePercent) }

    executorService.execute {
      try {
        Log.d(TAG, "Starting USB volume apply")
        val device = UsbHelper.findAppleDongle(usbManager)
        if (device != null) {
          Log.d(TAG, "Found dongle: ${device.deviceName}")
          val result = UsbHelper.connectAndDo(device, usbManager) { fd ->
            sendVolumeToDevice(fd, volumePercent)
          }

          result.onSuccess { name ->
            Log.d(TAG, "Connected to device and applied volume: $name")
          }.onFailure { e ->
            Log.e(TAG, "Failed to connect or apply volume", e)
            showToast(getString(R.string.toast_error_prefix, e.message ?: "Unknown"))
          }
        } else {
          Log.w(TAG, "Apple USB DAC not found")
          showToast(getString(R.string.toast_apple_dac_not_found))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Exception during volume apply", e)
        showToast(getString(R.string.toast_error_prefix, e.message ?: "Unknown"))
      } finally {
        if (finishOnComplete) {
          runOnUiThread { finishAndRemoveTask() }
        }
      }
    }
  }

  private fun sendVolumeToDevice(fd: Int, volumePercent: Int) {
    val hexValue = UsbHelper.calculateVolumeHex(volumePercent)
    val lowByte = (hexValue and 0xFF).toByte()
    val highByte = ((hexValue shr 8) and 0xFF).toByte()
    val volumeBytes = byteArrayOf(lowByte, highByte)

    val result = NativeUsbLib.setDeviceVolume(fd, volumeBytes)
    if (result >= 0) {
      showToast(getString(R.string.toast_volume_success))
    } else {
      showToast(getString(R.string.toast_volume_error, result))
    }
  }

  private fun showToast(message: String) {
    runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
  }
}
