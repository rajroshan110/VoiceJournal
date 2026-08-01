package dev.voicejournal.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String? = null,
    val audioPath: String,
    val audioFormat: String,
    val duration: Long,
    val transcript: String? = null,
    val transcriptCreatedAt: Long? = null,
    val transcriptModel: String? = null,
    val transcriptLanguage: String? = null,
    val transcriptVersion: String? = null,
    val userText: String? = null,
    val moodEmoji: String? = null,
    val moodLevel: Int? = null,
    val hasTranscript: Boolean = false,
    val audioTracksJson: String = "",
    val deletedAt: Long? = null,
    val isArchived: Boolean = false,
    val isDraft: Boolean = false
)
