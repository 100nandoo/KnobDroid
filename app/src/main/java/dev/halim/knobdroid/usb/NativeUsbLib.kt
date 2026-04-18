package dev.halim.knobdroid.usb

import android.util.Log

object NativeUsbLib {
  private const val TAG = "NativeUsbLib"

  init {
    try {
      System.loadLibrary("usbAndroidTest")
      Log.d(TAG, "Native library loaded successfully")
    } catch (e: UnsatisfiedLinkError) {
      Log.e(TAG, "Failed to load native library: ${e.message}")
      throw e
    }
  }

  external fun initializeNativeDevice(fd: Int): String

  external fun setDeviceVolume(fd: Int, volume: ByteArray): Int
}
