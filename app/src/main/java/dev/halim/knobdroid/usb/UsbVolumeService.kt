package dev.halim.knobdroid.usb

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.halim.knobdroid.AppConstants
import dev.halim.knobdroid.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UsbVolumeService : Service() {
  private lateinit var usbManager: UsbManager
  private lateinit var executorService: ExecutorService
  private var currentFileDescriptor: Int = -1

  companion object {
    private const val TAG = "UsbVolumeService"
  }

  private val usbReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        // Handle USB permission responses if needed
      }
    }

  override fun onCreate() {
    super.onCreate()
    usbManager = getSystemService(USB_SERVICE) as UsbManager
    executorService = Executors.newSingleThreadExecutor()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    registerUsbReceiver()

    val volumePercent = intent?.getIntExtra(AppConstants.PreferenceKeys.VOLUME_PERCENT, 50) ?: 50

    Log.d(TAG, "Service started - Device ABI: ${Build.SUPPORTED_ABIS.joinToString()}")

    executorService.execute {
      try {
        Log.d(TAG, "Starting USB volume transfer")
        val device = findConnectedDongle()
        if (device != null) {
          Log.d(TAG, "Found dongle: ${device.deviceName}")
          val result =
            UsbHelper.connectAndDo(device, usbManager) { fd ->
              currentFileDescriptor = fd // Optional: if you still need it elsewhere
              sendVolumeToDevice(fd, volumePercent)
            }

          result
            .onSuccess { name -> Log.d(TAG, "Connected to device and applied volume: $name") }
            .onFailure { e ->
              Log.e(TAG, "Failed to connect or apply volume", e)
              showToast(getString(R.string.toast_error_prefix, e.message ?: "Unknown"))
            }
        } else {
          Log.w(TAG, "Apple USB DAC not found")
          showToast(getString(R.string.toast_apple_dac_not_found))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Exception during volume transfer", e)
        showToast(getString(R.string.toast_error_prefix, e.message ?: "Unknown"))
      }
      stopSelf()
    }

    return START_NOT_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(usbReceiver)
    } catch (e: Exception) {
      // Receiver not registered
    }
    executorService.shutdown()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun registerUsbReceiver() {
    val filter = IntentFilter("com.android.example.USB_PERMISSION")
    ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
  }

  private fun findConnectedDongle(): UsbDevice? {
    return UsbHelper.findAppleDongle(usbManager)
  }

  private fun sendVolumeToDevice(fd: Int, volumePercent: Int) {
    val hexValue = UsbHelper.calculateVolumeHex(volumePercent)
    val lowByte = (hexValue and 0xFF).toByte()
    val highByte = ((hexValue shr 8) and 0xFF).toByte()
    val volumeBytes = byteArrayOf(lowByte, highByte)

    val result = NativeUsbLib.setDeviceVolume(fd, volumeBytes)
    if (result >= 0) {
      showToast(getString(R.string.toast_volume_success))
    } else {
      showToast(getString(R.string.toast_volume_error, result))
    }
  }

  private fun showToast(message: String) {
    runOnUiThread { Toast.makeText(this@UsbVolumeService, message, Toast.LENGTH_SHORT).show() }
  }

  private fun runOnUiThread(runnable: Runnable) {
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    handler.post(runnable)
  }
}
