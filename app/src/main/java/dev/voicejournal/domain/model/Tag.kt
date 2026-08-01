package dev.voicejournal.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Tag(
    val id: Long = 0,
    val name: String,
    val type: TagType = TagType.TOPIC
)
