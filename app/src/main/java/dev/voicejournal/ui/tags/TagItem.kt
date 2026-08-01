package dev.voicejournal.ui.tags

import dev.voicejournal.domain.model.Tag

data class TagItem(
    val tag: Tag,
    val noteCount: Int,
    val latestNoteDateMillis: Long = 0L,
    val previewNoteTitle: String? = null
)
