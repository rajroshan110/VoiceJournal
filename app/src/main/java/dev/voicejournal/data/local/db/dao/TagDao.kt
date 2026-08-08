package dev.voicejournal.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.voicejournal.data.local.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE type = :type ORDER BY name ASC")
    fun getTagsByType(type: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getTagByNameAndType(name: String, type: String): TagEntity?

    @Query("SELECT * FROM tags WHERE uuid = :uuid LIMIT 1")
    suspend fun getTagByUuid(uuid: String): TagEntity?

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagInternal(id: Long)

    @Transaction
    suspend fun deleteTag(id: Long) {
        deleteTagCrossRefsByTagId(id)
        deleteTagInternal(id)
    }

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("UPDATE tags SET name = :newName WHERE id = :id")
    suspend fun updateTagName(id: Long, newName: String)

    @Query("INSERT OR IGNORE INTO entry_tag_cross_ref (entryId, tagId) SELECT entryId, :targetTagId FROM entry_tag_cross_ref WHERE tagId = :sourceTagId")
    suspend fun relinkCrossRefs(sourceTagId: Long, targetTagId: Long)

    @Query("DELETE FROM entry_tag_cross_ref WHERE tagId = :sourceTagId")
    suspend fun deleteTagCrossRefsByTagId(sourceTagId: Long)

    @Transaction
    suspend fun mergeTags(sourceTagId: Long, targetTagId: Long) {
        relinkCrossRefs(sourceTagId, targetTagId)
        deleteTagCrossRefsByTagId(sourceTagId)
        deleteTagInternal(sourceTagId)
    }
}
