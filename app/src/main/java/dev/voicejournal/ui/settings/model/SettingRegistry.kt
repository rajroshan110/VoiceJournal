package dev.voicejournal.ui.settings.model

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val parentCategory: String,
    val targetPreferenceKey: String,
    val targetSubScreen: SettingsSubScreen
)

enum class SettingsSubScreen {
    GENERAL,
    PRIVACY_SECURITY,
    SYNC_BACKUP
}

object SettingRegistry {
    val allSettingItems = listOf(
        SearchResultItem(
            id = "app_theme",
            title = "App UI Theme",
            subtitle = "Switch between Dark Theme and Premium Light Journal palette",
            parentCategory = "General Settings > Appearance",
            targetPreferenceKey = "app_theme",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "audio_format",
            title = "Audio Recording Quality",
            subtitle = "WAV (16kHz uncompressed) vs M4A (AAC 128kbps compressed)",
            parentCategory = "General Settings > Recording & AI Models",
            targetPreferenceKey = "audio_format",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "whisper_model",
            title = "Local Speech-to-Text Model",
            subtitle = "Whisper Base Q5 offline model binary download and status",
            parentCategory = "General Settings > Recording & AI Models",
            targetPreferenceKey = "whisper_model",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "is_markdown_enabled",
            title = "Markdown Editor",
            subtitle = "Enable rich text markdown rendering in journal detail view",
            parentCategory = "General Settings > Journaling & Editor",
            targetPreferenceKey = "is_markdown_enabled",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "insight_date_range",
            title = "Insight Date Range Mode",
            subtitle = "Calculate dashboard metrics by Last 7/30 days or Current Calendar Month",
            parentCategory = "General Settings > Journaling & Editor",
            targetPreferenceKey = "insight_date_range",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "time_format",
            title = "Time Format",
            subtitle = "Choose System Default, 12-Hour (4:32 PM), or 24-Hour (16:32)",
            parentCategory = "General Settings > Journaling & Editor",
            targetPreferenceKey = "time_format",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "start_of_week",
            title = "Start of the Week",
            subtitle = "Set week starting day to System Default, Monday, or Sunday",
            parentCategory = "General Settings > Journaling & Editor",
            targetPreferenceKey = "start_of_week",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "delete_all_journals",
            title = "Delete All Journals",
            subtitle = "Permanently wipe database tables and local media files",
            parentCategory = "General Settings > Data Management",
            targetPreferenceKey = "delete_all_journals",
            targetSubScreen = SettingsSubScreen.GENERAL
        ),
        SearchResultItem(
            id = "app_lock_mode",
            title = "App Lock Mode",
            subtitle = "Protect app with Biometrics or Custom PIN authentication",
            parentCategory = "Privacy & Security > App Lock",
            targetPreferenceKey = "app_lock_mode",
            targetSubScreen = SettingsSubScreen.PRIVACY_SECURITY
        ),
        SearchResultItem(
            id = "is_screen_privacy_enabled",
            title = "Screen Privacy (FLAG_SECURE)",
            subtitle = "Block screenshots and task switcher previews for enhanced privacy",
            parentCategory = "Privacy & Security > Screen Privacy",
            targetPreferenceKey = "is_screen_privacy_enabled",
            targetSubScreen = SettingsSubScreen.PRIVACY_SECURITY
        ),
        SearchResultItem(
            id = "cloud_sync",
            title = "Cloud Sync",
            subtitle = "Automatic cloud backup and sync preferences",
            parentCategory = "Sync & Backup > Cloud Sync",
            targetPreferenceKey = "cloud_sync",
            targetSubScreen = SettingsSubScreen.SYNC_BACKUP
        ),
        SearchResultItem(
            id = "export_backup",
            title = "Export Backup",
            subtitle = "Create a complete ZIP archive of journal entries, tags, and recordings",
            parentCategory = "Sync & Backup > Local Backup",
            targetPreferenceKey = "export_backup",
            targetSubScreen = SettingsSubScreen.SYNC_BACKUP
        ),
        SearchResultItem(
            id = "import_backup",
            title = "Import Backup",
            subtitle = "Restore entries and media from a previously exported ZIP backup",
            parentCategory = "Sync & Backup > Local Backup",
            targetPreferenceKey = "import_backup",
            targetSubScreen = SettingsSubScreen.SYNC_BACKUP
        )
    )

    fun search(query: String): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return allSettingItems.filter { item ->
            item.title.lowercase().contains(q) ||
            item.subtitle.lowercase().contains(q) ||
            item.parentCategory.lowercase().contains(q) ||
            item.targetPreferenceKey.lowercase().contains(q)
        }
    }
}
