package dev.voicejournal.data.local.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import dev.voicejournal.data.local.db.entity.EntryImageEntity
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity

data class EntryWithTagsAndImages(
    @Embedded val entry: JournalEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val images: List<EntryImageEntity>
)
