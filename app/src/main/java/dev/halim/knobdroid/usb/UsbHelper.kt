package dev.halim.knobdroid.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import dev.halim.knobdroid.AppConstants

object UsbHelper {
  private const val APPLE_VENDOR_ID = 0x5AC
  private const val APPLE_PRODUCT_ID = 0x110A

  fun isAppleDongle(device: UsbDevice): Boolean {
    return device.vendorId == APPLE_VENDOR_ID && device.productId == APPLE_PRODUCT_ID
  }

  fun connectDevice(device: UsbDevice, usbManager: UsbManager): Pair<Int, String> {
    val connection =
      usbManager.openDevice(device) ?: return Pair(-1, "Failed to open USB connection")

    return try {
      val fd = connection.fileDescriptor
      val deviceName = NativeUsbLib.initializeNativeDevice(fd)
      Pair(fd, deviceName)
    } catch (e: Exception) {
      connection.close()
      Pair(-1, "Error: ${e.message}")
    }
  }

  fun calculateVolumeHex(percent: Int): Int {
    return (percent * AppConstants.MAX_VOLUME_HEX) / 100
  }
}
