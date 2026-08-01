package dev.voicejournal.domain.model

enum class InsightDateRangeMode {
    LAST_DAYS,         // Last 7 days for Week, Last 30 days for Month
    CURRENT_CALENDAR   // Current Week (Mon-Sun), Current Month (1st to today)
}
