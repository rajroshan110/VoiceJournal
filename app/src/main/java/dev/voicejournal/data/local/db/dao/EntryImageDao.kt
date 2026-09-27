package dev.voicejournal.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.voicejournal.data.local.db.entity.EntryImageEntity

@Dao
interface EntryImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntryImages(images: List<EntryImageEntity>)

    @Query("DELETE FROM entry_images WHERE entryId = :entryId")
    suspend fun deleteEntryImages(entryId: Long)

    @Query("SELECT * FROM entry_images")
    suspend fun getAllEntryImagesSync(): List<EntryImageEntity>

    @Query("DELETE FROM entry_images")
    suspend fun deleteAllEntryImages()
}
