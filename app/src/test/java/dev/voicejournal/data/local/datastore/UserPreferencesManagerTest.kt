package dev.voicejournal.data.local.datastore

import dev.voicejournal.domain.model.AppLockMode
import dev.voicejournal.domain.model.AudioFormat
import dev.voicejournal.domain.model.InsightDateRangeMode
import dev.voicejournal.domain.model.StartOfWeek
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.ui.theme.AppThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesManagerTest {

    @Test
    fun defaultEnumNames_matchRequirements() {
        assertEquals("WAV_16KHZ", AudioFormat.WAV_16KHZ.name)
        assertEquals("M4A_AAC_128KBPS", AudioFormat.M4A_AAC_128KBPS.name)
        assertEquals("DARK", AppThemeMode.DARK.name)
        assertEquals("LIGHT_PREMIUM", AppThemeMode.LIGHT_PREMIUM.name)
        assertEquals("LAST_DAYS", InsightDateRangeMode.LAST_DAYS.name)
        assertEquals("CURRENT_CALENDAR", InsightDateRangeMode.CURRENT_CALENDAR.name)
        assertEquals("NONE", AppLockMode.NONE.name)
        assertEquals("BIOMETRIC", AppLockMode.BIOMETRIC.name)
        assertEquals("CUSTOM_PIN", AppLockMode.CUSTOM_PIN.name)
        assertEquals("SYSTEM_DEFAULT", TimeFormat.SYSTEM_DEFAULT.name)
        assertEquals("TWELVE_HOUR", TimeFormat.TWELVE_HOUR.name)
        assertEquals("TWENTY_FOUR_HOUR", TimeFormat.TWENTY_FOUR_HOUR.name)
        assertEquals("SYSTEM_DEFAULT", StartOfWeek.SYSTEM_DEFAULT.name)
        assertEquals("MONDAY", StartOfWeek.MONDAY.name)
        assertEquals("SUNDAY", StartOfWeek.SUNDAY.name)
    }
}
