package dev.halim.knobdroid

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import dev.halim.knobdroid.ui.screen.UsbControlScreen
import dev.halim.knobdroid.ui.theme.KnobDroidTheme
import dev.halim.knobdroid.usb.UsbVolumeService

class MainActivity : ComponentActivity() {
  private lateinit var sharedPreferences: SharedPreferences

  companion object {
    init {
      try {
        System.loadLibrary("usbAndroidTest")
        android.util.Log.d("MainActivity", "Native library loaded in companion init")
      } catch (e: UnsatisfiedLinkError) {
        android.util.Log.e("MainActivity", "Failed to load native library: ${e.message}", e)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    sharedPreferences = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)

    setContent {
      KnobDroidTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          UsbControlScreen(
            modifier = Modifier.padding(innerPadding),
            sharedPreferences = sharedPreferences,
            onApplyVolume = { volumePercent -> startVolumeService(volumePercent) },
          )
        }
      }
    }

    handleUsbIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleUsbIntent(intent)
  }

  private fun handleUsbIntent(intent: Intent) {
    if (intent.action == AppConstants.IntentActions.USB_DEVICE_ATTACHED) {
      val volumePercent = sharedPreferences.getInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, 50)
      startVolumeService(volumePercent)
    }
  }

  private fun startVolumeService(volumePercent: Int) {
    sharedPreferences.edit { putInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, volumePercent) }

    Intent(this, UsbVolumeService::class.java).apply {
      putExtra("volume_percent", volumePercent)
      startService(this)
    }
  }
}
