package dev.voicejournal.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "entry_images",
    indices = [
        Index("entryId"),
        Index(value = ["uuid"], unique = true)
    ]
)
data class EntryImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val imagePath: String,
    val displayOrder: Int,
    val uuid: String = UUID.randomUUID().toString()
)
