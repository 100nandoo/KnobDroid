package dev.halim.knobdroid.usb

import android.media.AudioManager
import android.os.SystemClock

/**
 * Applying the fix briefly detaches and re-attaches the adapter's kernel audio driver, which cuts
 * off anything that is playing or recording through it. Wait for the audio system to be idle first.
 */
object AudioIdleProbe {
    private const val REQUIRED_QUIET_MS = 1_000L
    private const val POLL_INTERVAL_MS = 50L
    private const val MAX_WAIT_MS = 5_000L

    /**
     * Blocks until audio has been idle for [REQUIRED_QUIET_MS]. Returns null once it has, or the
     * reason audio is still busy if that doesn't happen within [MAX_WAIT_MS]. Call off the main thread.
     */
    fun awaitQuiet(audioManager: AudioManager): String? {
        val deadline = SystemClock.elapsedRealtime() + MAX_WAIT_MS
        var quietSince = -1L
        while (true) {
            val now = SystemClock.elapsedRealtime()
            val busyReason = busyReason(audioManager)
            if (busyReason == null) {
                if (quietSince < 0) quietSince = now
                if (now - quietSince >= REQUIRED_QUIET_MS) return null
            } else {
                quietSince = -1L
                if (now >= deadline) return busyReason
            }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
    }

    private fun busyReason(audioManager: AudioManager): String? {
        val reasons = mutableListOf<String>()
        if (audioManager.isMusicActive) reasons += "music playing"
        if (audioManager.mode != AudioManager.MODE_NORMAL) reasons += "call or ringtone active"
        if (audioManager.activePlaybackConfigurations.isNotEmpty()) reasons += "audio playing"
        if (audioManager.activeRecordingConfigurations.any { !it.isClientSilenced }) {
            reasons += "recording active"
        }
        return reasons.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}
