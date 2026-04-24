package dev.halim.knobdroid.acra

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashFileSenderFactory : ReportSenderFactory {
  override fun create(context: Context, config: CoreConfiguration): ReportSender =
    CrashFileReportSender()
}

class CrashFileReportSender : ReportSender {
  override fun send(context: Context, errorContent: CrashReportData) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "crash_$timestamp.txt"
    val content = errorContent.toJSON()
    val values = ContentValues().apply {
      put(MediaStore.Downloads.DISPLAY_NAME, filename)
      put(MediaStore.Downloads.MIME_TYPE, "text/plain")
      put(MediaStore.Downloads.RELATIVE_PATH, "Download/KnobDroid")
      put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
      ?: return
    context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
    values.clear()
    values.put(MediaStore.Downloads.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
  }
}
