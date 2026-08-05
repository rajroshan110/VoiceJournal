package dev.voicejournal.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name", "type"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val uuid: String = UUID.randomUUID().toString()
)
