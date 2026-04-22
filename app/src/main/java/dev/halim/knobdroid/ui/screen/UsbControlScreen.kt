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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    PermissionRequestView(
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

    Text(
      stringResource(R.string.volume_control_title),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 24.dp),
    )

    VolumeCard(volumeEnabled = volumeEnabled, onVolumeChange = onVolumeChange)

    Spacer(modifier = Modifier.height(16.dp))

    ApplyButton(onClick = onApplyVolume)

    Spacer(modifier = Modifier.height(16.dp))

    DeviceStatusText(deviceStatus)
  }
}

@Composable
private fun VolumeCard(volumeEnabled: Boolean, onVolumeChange: (Boolean) -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(8.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ),
    shape = RoundedCornerShape(24.dp),
  ) {
    Column(
      modifier = Modifier.padding(24.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(if (volumeEnabled) R.string.volume_on else R.string.volume_off),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Switch(checked = volumeEnabled, onCheckedChange = onVolumeChange)
    }
  }
}

@Composable
private fun ApplyButton(onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
    shape = RoundedCornerShape(16.dp),
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
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(bottom = 16.dp),
  )
}

@Composable
fun PermissionRequestView(onGrantClick: () -> Unit) {
  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Background gradient for a premium look
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                  MaterialTheme.colorScheme.background,
                )
            )
          )
    )

    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier =
          Modifier.size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(R.drawable.warning),
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = stringResource(R.string.permission_required_title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.permission_required_desc),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 24.sp,
      )

      Spacer(modifier = Modifier.height(48.dp))

      Button(
        onClick = onGrantClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text(
          text = stringResource(R.string.grant_permission_button),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimary,
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewVolumeCardOn() {
  KnobDroidTheme { VolumeCard(volumeEnabled = true, onVolumeChange = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewVolumeCardOff() {
  KnobDroidTheme { VolumeCard(volumeEnabled = false, onVolumeChange = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewUsbControlScreenContent() {
  KnobDroidTheme {
    UsbControlScreenContent(
      volumeEnabled = true,
      deviceStatus = "Apple USB-C to 3.5mm",
      onVolumeChange = {},
      onApplyVolume = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewApplyButton() {
  KnobDroidTheme { ApplyButton(onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDeviceStatusText() {
  KnobDroidTheme { DeviceStatusText(status = "Apple USB-C to 3.5mm") }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPermissionRequestView() {
  KnobDroidTheme { PermissionRequestView(onGrantClick = {}) }
}
