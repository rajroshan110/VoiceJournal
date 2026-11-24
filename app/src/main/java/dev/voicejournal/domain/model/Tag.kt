package dev.voicejournal.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Tag(
    val id: Long = 0,
    val name: String,
    val type: TagType = TagType.TOPIC,
    val uuid: String = UUID.randomUUID().toString()
)
