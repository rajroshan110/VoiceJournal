package dev.voicejournal.domain.model

data class EntryImage(
    val id: Long = 0,
    val entryId: Long,
    val imagePath: String,
    val displayOrder: Int
)
