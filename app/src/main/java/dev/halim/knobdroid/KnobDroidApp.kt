package dev.halim.knobdroid

import android.app.Application
import android.content.Context
import org.acra.config.toast
import org.acra.ktx.initAcra

class KnobDroidApp : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initAcra {
      toast { text = getString(R.string.crash_toast_text) }
    }
  }
}
