package dev.voicejournal.ui.navigation

sealed class Screen(val route: String) {
    object Journal : Screen("Journal?person={person}&tag={tag}") {
        fun createRoute(person: String? = null, tag: String? = null): String {
            val params = mutableListOf<String>()
            if (!person.isNullOrEmpty()) params.add("person=$person")
            if (!tag.isNullOrEmpty()) params.add("tag=$tag")
            return if (params.isNotEmpty()) "Journal?${params.joinToString("&")}" else "Journal"
        }
    }
    object Calendar : Screen("Calendar")
    object Insight : Screen("Insight")
    object Settings : Screen("Settings")
    object Folders : Screen("Folders")
    object Tags : Screen("Tags")
    object Archive : Screen("Archive")
    object Draft : Screen("Draft")
    object Trash : Screen("Trash")
    object NoteDetail : Screen("NoteDetail/{entryId}?initialFolder={initialFolder}&initialTag={initialTag}&initialTagType={initialTagType}") {
        fun createRoute(
            entryId: Long = -1L,
            initialFolder: String? = null,
            initialTag: String? = null,
            initialTagType: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (!initialFolder.isNullOrEmpty()) params.add("initialFolder=$initialFolder")
            if (!initialTag.isNullOrEmpty()) params.add("initialTag=$initialTag")
            if (!initialTagType.isNullOrEmpty()) params.add("initialTagType=$initialTagType")
            return if (params.isNotEmpty()) "NoteDetail/$entryId?${params.joinToString("&")}" else "NoteDetail/$entryId"
        }
    }
}
