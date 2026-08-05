package dev.voicejournal.domain.model

import java.util.UUID

data class EntryImage(
    val id: Long = 0,
    val entryId: Long,
    val imagePath: String,
    val displayOrder: Int,
    val uuid: String = UUID.randomUUID().toString()
)
