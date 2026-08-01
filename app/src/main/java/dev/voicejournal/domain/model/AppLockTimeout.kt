package dev.voicejournal.domain.model

enum class AppLockTimeout(val displayName: String, val timeoutMillis: Long) {
    IMMEDIATELY("Immediately", 0L),
    AFTER_1_MIN("After 1 minute", 60_000L)
}
