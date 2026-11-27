package dev.halim.knobdroid

object AppConstants {
  const val PREFS_NAME = "knobdroid_prefs"

  /**
   * Hardware volume of the Apple adapter's Feature Unit 2, in 1/256 dB (UAC). 0 dB is the
   * device's maximum; values above it (such as 0x7FFF) are out of range.
   */
  const val VOLUME_RAW_MAX = 0

  /** The -20 dB (0xEC00) level the adapter powers up with. */
  const val VOLUME_RAW_STOCK = -5120

  object PreferenceKeys {
    const val VOLUME_ENABLED = "volume_enabled"
    const val DEFAULT_VOLUME_ENABLED = true
  }

  object IntentActions {
    const val USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
  }
}
