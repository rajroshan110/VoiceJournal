package dev.voicejournal.domain.model

enum class AppLockMode {
    NONE,
    BIOMETRIC,
    CUSTOM_PIN
}

enum class TimeFormat {
    SYSTEM_DEFAULT,
    TWELVE_HOUR,
    TWENTY_FOUR_HOUR
}

enum class StartOfWeek {
    SYSTEM_DEFAULT,
    MONDAY,
    SUNDAY
}

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState
    data class Downloading(val progress: Float) : ModelDownloadState
    data object Downloaded : ModelDownloadState
    data class Error(val message: String) : ModelDownloadState
}
