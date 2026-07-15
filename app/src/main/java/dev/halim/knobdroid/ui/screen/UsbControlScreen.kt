package dev.halim.knobdroid.ui.screen

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import dev.halim.knobdroid.ui.theme.KnobDroidTheme
import dev.halim.knobdroid.usb.UsbHelper

@Composable
fun UsbControlScreen(
  modifier: Modifier = Modifier,
  sharedPreferences: SharedPreferences,
  onApplyVolume: (Int) -> Unit,
) {
  val androidContext = LocalContext.current
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(androidContext, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      hasAudioPermission = isGranted
    }

  val defaultStatusText = stringResource(R.string.device_status_checking)
  val deviceNoUsbText = stringResource(R.string.device_status_no_usb)
  val appleDongleText = stringResource(R.string.us_apple_dongle)
  val usbDetected = stringResource(R.string.usb_device_detected)

  val volumeEnabled = remember {
    mutableStateOf(
      sharedPreferences.getBoolean(
        AppConstants.PreferenceKeys.VOLUME_ENABLED,
        AppConstants.PreferenceKeys.DEFAULT_VOLUME_ENABLED,
      )
    )
  }
  val deviceName = remember { mutableStateOf(defaultStatusText) }

  DisposableEffect(androidContext) {
    val usbManager = androidContext.getSystemService(Context.USB_SERVICE) as UsbManager

    fun updateDeviceName() {
      val appleDongle = UsbHelper.findAppleDongle(usbManager)
      deviceName.value =
        when {
          appleDongle != null -> appleDongleText
          UsbHelper.hasAnyUsbDevice(usbManager) -> usbDetected
          else -> deviceNoUsbText
        }
    }

    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          updateDeviceName()
        }
      }

    val filter =
      IntentFilter().apply {
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
      }

    androidContext.registerReceiver(receiver, filter)

    // Initial check
    updateDeviceName()

    onDispose { androidContext.unregisterReceiver(receiver) }
  }

  if (!hasAudioPermission) {
    PermissionRequestScreen(
      onGrantClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    )
  } else {
    UsbControlScreenContent(
      modifier = modifier,
      volumeEnabled = volumeEnabled.value,
      deviceStatus = deviceName.value,
      onVolumeChange = { isChecked ->
        volumeEnabled.value = isChecked
        sharedPreferences.edit { putBoolean(AppConstants.PreferenceKeys.VOLUME_ENABLED, isChecked) }
      },
      onApplyVolume = { onApplyVolume(if (volumeEnabled.value) 100 else 0) },
    )
  }
}

@Composable
fun UsbControlScreenContent(
  modifier: Modifier = Modifier,
  volumeEnabled: Boolean,
  deviceStatus: String,
  onVolumeChange: (Boolean) -> Unit,
  onApplyVolume: () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.weight(1f))
    DeviceStatusText(deviceStatus)
    Spacer(modifier = Modifier.weight(1f))

    AutoApplySwitch(volumeEnabled = volumeEnabled, onVolumeChange = onVolumeChange)

    Spacer(modifier = Modifier.height(16.dp))

    ApplyButton(onClick = onApplyVolume)
  }
}

@Composable
private fun AutoApplySwitch(volumeEnabled: Boolean, onVolumeChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.padding(8.dp).fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      stringResource(R.string.auto_apply),
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(16.dp))

    Switch(checked = volumeEnabled, onCheckedChange = onVolumeChange)
  }
}

@Composable
private fun ApplyButton(onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
    shape = RoundedCornerShape(32.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
  ) {
    Text(
      stringResource(R.string.apply_volume_fix),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun DeviceStatusText(status: String) {
  Text(
    text = status,
    style = MaterialTheme.typography.bodyLarge,
    modifier = Modifier.padding(bottom = 16.dp),
  )
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
  KnobDroidTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
      content()
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewUsbControlScreenContent() {
  PreviewSurface {
    UsbControlScreenContent(
      volumeEnabled = true,
      deviceStatus = "Apple USB-C to 3.5mm",
      onVolumeChange = {},
      onApplyVolume = {},
    )
  }
}