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

  fun findAppleDongle(usbManager: UsbManager): UsbDevice? {
    return usbManager.deviceList.values.find { isAppleDongle(it) }
  }

  fun hasAnyUsbDevice(usbManager: UsbManager): Boolean {
    return usbManager.deviceList.isNotEmpty()
  }

  fun connectAndDo(
    device: UsbDevice,
    usbManager: UsbManager,
    block: (Int) -> Unit,
  ): Result<String> {
    val connection =
      usbManager.openDevice(device)
        ?: return Result.failure(Exception("Failed to open USB connection"))

    return try {
      val fd = connection.fileDescriptor
      val deviceName = NativeUsbLib.initializeNativeDevice(fd)
      block(fd)
      Result.success(deviceName)
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection.close()
    }
  }

  fun calculateVolumeHex(percent: Int): Int {
    return (percent * AppConstants.MAX_VOLUME_HEX) / 100
  }
}
