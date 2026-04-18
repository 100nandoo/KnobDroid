package dev.halim.knobdroid.ui.screen

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import kotlin.math.roundToInt

@Composable
fun UsbControlScreen(
  modifier: Modifier = Modifier,
  sharedPreferences: SharedPreferences,
  onApplyVolume: (Int) -> Unit,
) {
  val defaultStatusText = stringResource(R.string.device_status_checking)
  val volumePercent = remember {
    mutableIntStateOf(sharedPreferences.getInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, 50))
  }
  val autoApply = remember {
    mutableStateOf(sharedPreferences.getBoolean(AppConstants.PreferenceKeys.AUTO_APPLY, false))
  }
  val deviceName = remember { mutableStateOf(defaultStatusText) }

  // Calculate hex value from percentage
  val hexValue = (volumePercent.intValue * 0xFFFF) / 100
  val hexString = String.format("%04X", hexValue)

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(stringResource(R.string.volume_control_title), modifier = Modifier.padding(bottom = 16.dp))

    Text(
      stringResource(R.string.volume_percent_format, volumePercent.intValue),
      modifier = Modifier.padding(8.dp),
    )

    OutlinedTextField(
      value = hexString,
      onValueChange = {},
      label = { Text(stringResource(R.string.hex_value_label)) },
      readOnly = true,
      modifier = Modifier.fillMaxWidth().padding(8.dp),
    )

    Slider(
      value = volumePercent.intValue.toFloat() / 100f,
      onValueChange = { newValue ->
        volumePercent.intValue = (newValue * 100).roundToInt()
        sharedPreferences.edit {
          putInt(AppConstants.PreferenceKeys.VOLUME_PERCENT, volumePercent.intValue)
        }
      },
      modifier = Modifier.fillMaxWidth().padding(8.dp),
    )

    // Auto-apply checkbox
    Checkbox(
      checked = autoApply.value,
      onCheckedChange = { newValue ->
        autoApply.value = newValue
        sharedPreferences.edit { putBoolean(AppConstants.PreferenceKeys.AUTO_APPLY, newValue) }
      },
      modifier = Modifier.padding(8.dp),
    )
    Text(stringResource(R.string.auto_apply_label), modifier = Modifier.padding(start = 8.dp))

    Button(
      onClick = { onApplyVolume(volumePercent.intValue) },
      modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
      Text(stringResource(R.string.apply_volume_button))
    }

    Text(deviceName.value, modifier = Modifier.padding(top = 24.dp))
  }
}
