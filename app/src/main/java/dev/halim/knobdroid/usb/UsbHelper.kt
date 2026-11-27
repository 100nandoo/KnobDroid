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

  fun <T> connectAndDo(
    device: UsbDevice,
    usbManager: UsbManager,
    block: (Int) -> T,
  ): Result<T> {
    val connection =
      usbManager.openDevice(device)
        ?: return Result.failure(
          Exception("Failed to open USB connection (check USB and microphone permission)")
        )

    return try {
      val fd = connection.fileDescriptor
      if (fd < 0) {
        Result.failure(Exception("Invalid USB file descriptor: $fd"))
      } else {
        Result.success(block(fd))
      }
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      connection.close()
    }
  }

  /**
   * Maps 0..100 % onto the adapter's hardware volume in 1/256 dB units: 100 % is the hardware
   * maximum (0 dB), 0 % is the adapter's stock power-on level (-20 dB).
   */
  fun volumePercentToRaw(percent: Int): Int {
    val clamped = percent.coerceIn(0, 100)
    val stock = AppConstants.VOLUME_RAW_STOCK
    return stock + ((AppConstants.VOLUME_RAW_MAX - stock) * clamped) / 100
  }
}
