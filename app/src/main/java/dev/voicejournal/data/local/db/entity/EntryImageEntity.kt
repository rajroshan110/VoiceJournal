package dev.voicejournal.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_images",
    indices = [Index("entryId")]
)
data class EntryImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val imagePath: String,
    val displayOrder: Int
)
