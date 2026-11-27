package dev.halim.knobdroid

import dev.halim.knobdroid.usb.UsbHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeMappingTest {
  @Test
  fun fullVolumeIsHardwareMaximum() {
    // 0 dB is the top of the adapter's range; 0x7FFF (+128 dB) is out of range.
    assertEquals(0, UsbHelper.volumePercentToRaw(100))
  }

  @Test
  fun zeroVolumeIsStockLevel() {
    assertEquals(-5120, UsbHelper.volumePercentToRaw(0)) // -20 dB, 0xEC00
  }

  @Test
  fun midpointIsMinusTenDb() {
    assertEquals(-2560, UsbHelper.volumePercentToRaw(50))
  }

  @Test
  fun outOfRangePercentIsClamped() {
    assertEquals(0, UsbHelper.volumePercentToRaw(150))
    assertEquals(-5120, UsbHelper.volumePercentToRaw(-10))
  }
}
