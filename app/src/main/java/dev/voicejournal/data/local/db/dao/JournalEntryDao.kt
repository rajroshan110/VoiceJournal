package dev.voicejournal.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.relation.EntryWithTagsAndImages
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL AND isDraft = 0 ORDER BY createdAt DESC")
    fun getAllEntriesWithTagsAndImages(): Flow<List<EntryWithTagsAndImages>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL AND isArchived = 0 AND isDraft = 0 ORDER BY createdAt DESC")
    fun getActiveEntriesWithTagsAndImages(): Flow<List<EntryWithTagsAndImages>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL AND isArchived = 1 AND isDraft = 0 ORDER BY createdAt DESC")
    fun getArchiveEntriesWithTagsAndImages(): Flow<List<EntryWithTagsAndImages>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL AND isDraft = 1 ORDER BY updatedAt DESC")
    fun getDraftEntriesWithTagsAndImages(): Flow<List<EntryWithTagsAndImages>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NULL AND isDraft = 0 ORDER BY createdAt DESC")
    suspend fun getAllEntriesWithTagsAndImagesList(): List<EntryWithTagsAndImages>

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesForBackup(): List<EntryWithTagsAndImages>

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    suspend fun getAllRawEntriesSync(): List<JournalEntryEntity>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashEntriesWithTagsAndImages(): Flow<List<EntryWithTagsAndImages>>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<EntryWithTagsAndImages?>

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryByIdSync(id: Long): EntryWithTagsAndImages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE journal_entries SET isArchived = :isArchived WHERE id IN (:ids)")
    suspend fun setArchivedEntries(ids: List<Long>, isArchived: Boolean)

    @Query("UPDATE journal_entries SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteEntry(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE journal_entries SET deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteEntries(ids: List<Long>, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE journal_entries SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreEntry(id: Long)

    @Query("UPDATE journal_entries SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreEntries(ids: List<Long>)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM journal_entries WHERE id IN (:ids)")
    suspend fun deleteEntries(ids: List<Long>)

    @Query("DELETE FROM journal_entries WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllTrashEntries()

    @Query("SELECT id FROM journal_entries WHERE deletedAt IS NOT NULL")
    suspend fun getAllTrashEntryIds(): List<Long>

    @Query("DELETE FROM journal_entries WHERE deletedAt IS NOT NULL AND deletedAt < :thresholdTime")
    suspend fun deleteExpiredTrashEntries(thresholdTime: Long)

    @Query("SELECT id FROM journal_entries WHERE deletedAt IS NOT NULL AND deletedAt < :thresholdTime")
    suspend fun getExpiredTrashEntryIds(thresholdTime: Long): List<Long>

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryTagCrossRef(crossRef: EntryTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryTagCrossRefs(crossRefs: List<EntryTagCrossRef>)

    @Query("SELECT * FROM entry_tag_cross_ref")
    suspend fun getAllEntryTagCrossRefsSync(): List<EntryTagCrossRef>

    @Query("DELETE FROM entry_tag_cross_ref WHERE entryId = :entryId")
    suspend fun deleteEntryTagCrossRefs(entryId: Long)

    @Query("DELETE FROM entry_tag_cross_ref WHERE entryId IN (:entryIds) AND tagId = :tagId")
    suspend fun deleteSpecificTagCrossRefs(entryIds: List<Long>, tagId: Long)

    @Query("DELETE FROM entry_tag_cross_ref WHERE entryId IN (:entryIds) AND tagId IN (SELECT id FROM tags WHERE name = :folderName AND type = 'FOLDER')")
    suspend fun removeFolderCrossRefs(entryIds: List<Long>, folderName: String)

    @Query("DELETE FROM entry_tag_cross_ref")
    suspend fun deleteAllCrossRefs()
}
