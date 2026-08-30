package dev.voicejournal.ui.util

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages transient in-app system intent / SAF picker lifecycles to prevent
 * erroneous App Lock triggers when returning from system pickers.
 */
object AppLockStateManager {
    private val isTransientPickerActive = AtomicBoolean(false)

    /**
     * Call immediately before launching an external system picker (e.g. SAF create/open document).
     */
    fun notifySystemPickerLaunched() {
        isTransientPickerActive.set(true)
    }

    /**
     * Atomically consumes the transient picker flag.
     * Returns `true` if the current resume was caused by returning from a system picker.
     */
    fun consumeTransientPicker(): Boolean {
        return isTransientPickerActive.getAndSet(false)
    }
}
