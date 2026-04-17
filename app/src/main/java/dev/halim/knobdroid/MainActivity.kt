package dev.halim.knobdroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.knobdroid.ui.theme.KnobDroidTheme
import dev.halim.knobdroid.usb.UsbVolumeService
import kotlin.math.roundToInt

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

        sharedPreferences = getSharedPreferences("knobdroid_prefs", Context.MODE_PRIVATE)

        setContent {
            KnobDroidTheme(darkTheme = true) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UsbControlScreen(
                        modifier = Modifier.padding(innerPadding),
                        sharedPreferences = sharedPreferences,
                        onApplyVolume = { volumePercent ->
                            startVolumeService(volumePercent)
                        }
                    )
                }
            }
        }

        // Handle USB device attachment intent
        handleUsbIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent) {
        if (intent.action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
            val autoApply = sharedPreferences.getBoolean("auto_apply", false)
            if (autoApply) {
                val volumePercent = sharedPreferences.getInt("volume_percent", 50)
                startVolumeService(volumePercent)
            }
        }
    }

    private fun startVolumeService(volumePercent: Int) {
        sharedPreferences.edit().putInt("volume_percent", volumePercent).apply()

        val intent = Intent(this, UsbVolumeService::class.java)
        intent.putExtra("volume_percent", volumePercent)
        intent.putExtra("auto_apply", sharedPreferences.getBoolean("auto_apply", false))
        startService(intent)
    }
}

@Composable
fun UsbControlScreen(
    modifier: Modifier = Modifier,
    sharedPreferences: SharedPreferences,
    onApplyVolume: (Int) -> Unit
) {
    val volumePercent = remember {
        mutableIntStateOf(
            sharedPreferences.getInt("volume_percent", 50)
        )
    }
    val autoApply = remember {
        mutableStateOf(
            sharedPreferences.getBoolean("auto_apply", false)
        )
    }
    val deviceName = remember { mutableStateOf("Checking for USB device...") }

    // Calculate hex value from percentage
    val hexValue = (volumePercent.value * 0xFFFF) / 100
    val hexString = String.format("%04X", hexValue)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Volume Control", modifier = Modifier.padding(bottom = 16.dp))

        Text("Volume: ${volumePercent.value}%", modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = hexString,
            onValueChange = {},
            label = { Text("Hex Value") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Slider(
            value = volumePercent.value.toFloat() / 100f,
            onValueChange = { newValue ->
                volumePercent.value = (newValue * 100).roundToInt()
                sharedPreferences.edit().putInt("volume_percent", volumePercent.value).apply()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Auto-apply checkbox
        Checkbox(
            checked = autoApply.value,
            onCheckedChange = { newValue ->
                autoApply.value = newValue
                sharedPreferences.edit().putBoolean("auto_apply", newValue).apply()
            },
            modifier = Modifier.padding(8.dp)
        )
        Text("Auto-apply on device connect", modifier = Modifier.padding(start = 8.dp))

        Button(
            onClick = { onApplyVolume(volumePercent.value) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Apply Volume")
        }

        Text(
            deviceName.value,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
