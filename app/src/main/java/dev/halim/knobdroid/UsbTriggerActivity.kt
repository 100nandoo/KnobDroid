package dev.halim.knobdroid

import android.app.Activity
import android.content.SharedPreferences
import android.hardware.usb.UsbManager
import android.os.Bundle
import dev.halim.knobdroid.usb.VolumeActionHelper

class UsbTriggerActivity : Activity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usbManager: UsbManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        usbManager = getSystemService(USB_SERVICE) as UsbManager

        VolumeActionHelper.applyVolume(
            context = applicationContext,
            usbManager = usbManager,
            sharedPreferences = sharedPreferences,
            onComplete = { finish() }
        )
    }
}
