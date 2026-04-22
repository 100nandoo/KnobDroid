package dev.halim.knobdroid

object AppConstants {
  const val PREFS_NAME = "knobdroid_prefs"
  const val MAX_VOLUME_HEX = 0x7FFF

  object PreferenceKeys {
    const val VOLUME_ENABLED = "volume_enabled"
    const val DEFAULT_VOLUME_ENABLED = true
  }

  object IntentActions {
    const val USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
  }
}
