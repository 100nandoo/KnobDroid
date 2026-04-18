package dev.halim.knobdroid

import android.content.SharedPreferences
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dev.halim.knobdroid.ui.screen.UsbControlScreen
import dev.halim.knobdroid.ui.theme.KnobDroidTheme
import dev.halim.knobdroid.usb.VolumeActionHelper

class MainActivity : ComponentActivity() {
  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var usbManager: UsbManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    sharedPreferences = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
    usbManager = getSystemService(USB_SERVICE) as UsbManager

    setContent {
      KnobDroidTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          UsbControlScreen(
            modifier = Modifier.padding(innerPadding),
            sharedPreferences = sharedPreferences,
            onApplyVolume = { volumePercent ->
              VolumeActionHelper.applyVolume(
                context = this@MainActivity,
                usbManager = usbManager,
                sharedPreferences = sharedPreferences,
                requestedVolume = volumePercent
              )
            },
          )
        }
      }
    }
  }
}
