package dev.voicejournal.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "entry_tag_cross_ref",
    primaryKeys = ["entryId", "tagId"],
    indices = [
        Index("entryId"),
        Index("tagId")
    ]
)
data class EntryTagCrossRef(
    val entryId: Long,
    val tagId: Long
)
