package dev.halim.knobdroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.halim.knobdroid.ui.theme.KnobDroidTheme

@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
  KnobDroidTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
      content()
    }
  }
}
