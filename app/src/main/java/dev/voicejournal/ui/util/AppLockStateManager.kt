package dev.voicejournal.ui.util

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages transient in-app system intent / SAF picker lifecycles to prevent
 * erroneous App Lock triggers when returning from system pickers.
 */
object AppLockStateManager {
    internal const val TRANSIENT_PICKER_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    private const val NO_ACTIVE_PICKER = -1L

    private val transientPickerLaunchTimeMs = AtomicLong(NO_ACTIVE_PICKER)

    /**
     * Call immediately before launching an external system picker (e.g. SAF create/open document).
     */
    fun notifySystemPickerLaunched() {
        notifySystemPickerLaunched(SystemClock.elapsedRealtime())
    }

    internal fun notifySystemPickerLaunched(nowMs: Long) {
        transientPickerLaunchTimeMs.set(nowMs)
    }

    /**
     * Clears any pending transient picker flag (e.g., if launching the picker was cancelled or failed).
     */
    fun clearTransientPicker() {
        transientPickerLaunchTimeMs.set(NO_ACTIVE_PICKER)
    }

    /**
     * Atomically consumes the transient picker flag.
     * Returns `true` if the current resume was caused by returning from a system picker
     * within [TRANSIENT_PICKER_TIMEOUT_MS].
     */
    fun consumeTransientPicker(): Boolean {
        return consumeTransientPicker(SystemClock.elapsedRealtime())
    }

    internal fun consumeTransientPicker(nowMs: Long): Boolean {
        val launchedAt = transientPickerLaunchTimeMs.getAndSet(NO_ACTIVE_PICKER)
        if (launchedAt == NO_ACTIVE_PICKER) return false
        val elapsed = nowMs - launchedAt
        return elapsed in 0L until TRANSIENT_PICKER_TIMEOUT_MS
    }
}
