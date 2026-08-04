package dev.voicejournal.domain.model

import androidx.compose.runtime.Stable
import java.util.UUID

@Stable
data class Tag(
    val id: Long = 0,
    val name: String,
    val type: TagType = TagType.TOPIC,
    val uuid: String = UUID.randomUUID().toString()
)
