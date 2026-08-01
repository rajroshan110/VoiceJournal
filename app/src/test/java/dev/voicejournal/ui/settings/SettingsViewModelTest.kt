package dev.voicejournal.ui.settings

import dev.voicejournal.ui.settings.model.SettingRegistry
import dev.voicejournal.ui.settings.model.SettingsSubScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun settingRegistry_search_filtersResultsCorrectly() {
        val themeResults = SettingRegistry.search("Theme")
        assertTrue(themeResults.any { it.id == "app_theme" })

        val formatResults = SettingRegistry.search("WAV")
        assertTrue(formatResults.any { it.id == "audio_format" })

        val emptyResults = SettingRegistry.search("non_existent_query_xyz")
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun settingRegistry_allSettingItems_containsRequiredCategories() {
        val items = SettingRegistry.allSettingItems
        assertTrue(items.any { it.targetSubScreen == SettingsSubScreen.GENERAL })
        assertTrue(items.any { it.targetSubScreen == SettingsSubScreen.PRIVACY_SECURITY })
        assertTrue(items.any { it.targetSubScreen == SettingsSubScreen.SYNC_BACKUP })
    }
}
