package dev.halim.knobdroid.ui.screen

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import dev.halim.knobdroid.usb.UsbHelper
import kotlin.math.roundToInt

@Composable
fun UsbControlScreen(
  modifier: Modifier = Modifier,
  sharedPreferences: SharedPreferences,
  onApplyVolume: (Int) -> Unit,
) {
  val context = LocalContext.current
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      hasAudioPermission = isGranted
    }

  val defaultStatusText = stringResource(R.string.device_status_checking)
  val volumePercent = remember {
    mutableIntStateOf(sharedPreferences.getInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, 50))
  }
  val deviceName = remember { mutableStateOf(defaultStatusText) }

  val hexValue = UsbHelper.calculateVolumeHex(volumePercent.intValue)
  val hexString = String.format("%04X", hexValue)

  if (!hasAudioPermission) {
    PermissionRequestView(
      onGrantClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    )
  } else {
    Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        stringResource(R.string.volume_control_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 24.dp),
      )

      Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
        shape = RoundedCornerShape(24.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(
              stringResource(R.string.volume_percent_format, volumePercent.intValue),
              style = MaterialTheme.typography.displaySmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "0x$hexString",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 6.dp),
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Slider(
            value = volumePercent.intValue.toFloat() / 100f,
            onValueChange = { newValue ->
              volumePercent.intValue = (newValue * 100).roundToInt()
              sharedPreferences.edit {
                putInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, volumePercent.intValue)
              }
            },
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { onApplyVolume(volumePercent.intValue) },
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
      ) {
        Text(
          stringResource(R.string.apply_volume_button),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      Text(
        deviceName.value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
      )
    }
  }
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
