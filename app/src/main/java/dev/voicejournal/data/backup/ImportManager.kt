package dev.voicejournal.data.backup

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.local.db.AppDatabase
import dev.voicejournal.data.local.db.entity.EntryTagCrossRef
import dev.voicejournal.data.local.db.entity.JournalEntryEntity
import dev.voicejournal.data.local.db.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import dev.voicejournal.data.storage.MediaStorageManager
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    suspend fun importData(zipFile: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val extractDir = File(context.cacheDir, "import_temp")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(extractDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            val jsonFile = File(extractDir, "backup_data.json")
            if (!jsonFile.exists()) {
                return@withContext Result.failure(Exception("backup_data.json not found in archive"))
            }

            val jsonString = jsonFile.readText()
            val jsonRoot = JSONObject(jsonString)

            val entriesArray = jsonRoot.optJSONArray("entries")
            var importedEntriesCount = 0

            database.withTransaction {
                if (entriesArray != null) {
                    for (i in 0 until entriesArray.length()) {
                        val obj = entriesArray.getJSONObject(i)
                        database.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                id = obj.optLong("id", 0L),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                                audioPath = obj.optString("audioPath", ""),
                                audioFormat = obj.optString("audioFormat", "WAV_16KHZ"),
                                duration = obj.optLong("duration", 0L),
                                transcript = obj.optString("transcript").takeIf { it.isNotEmpty() },
                                userText = obj.optString("userText").takeIf { it.isNotEmpty() },
                                moodEmoji = obj.optString("moodEmoji").takeIf { it.isNotEmpty() },
                                moodLevel = obj.optInt("moodLevel", 0).takeIf { it > 0 },
                                hasTranscript = obj.optBoolean("hasTranscript", false)
                            )
                        )
                        importedEntriesCount++
                    }
                }

                val tagsArray = jsonRoot.optJSONArray("tags")
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        val obj = tagsArray.getJSONObject(i)
                        database.tagDao().insertTag(
                            TagEntity(
                                id = obj.optLong("id", 0L),
                                name = obj.getString("name"),
                                type = obj.optString("type", "TOPIC")
                            )
                        )
                    }
                }

                val crossRefsArray = jsonRoot.optJSONArray("crossRefs")
                if (crossRefsArray != null) {
                    for (i in 0 until crossRefsArray.length()) {
                        val obj = crossRefsArray.getJSONObject(i)
                        database.journalEntryDao().insertEntryTagCrossRef(
                            EntryTagCrossRef(
                                entryId = obj.getLong("entryId"),
                                tagId = obj.getLong("tagId")
                            )
                        )
                    }
                }
            }

            // Copy audio files to internal storage
            val recordingsDir = File(extractDir, "media/recordings")
            if (recordingsDir.exists() && recordingsDir.isDirectory) {
                recordingsDir.listFiles()?.forEach { audioFile ->
                    val dest = File(MediaStorageManager.getRecordingsDir(context), audioFile.name)
                    audioFile.copyTo(dest, overwrite = true)
                }
            }
            
            // Copy image files to internal storage
            val imagesDir = File(extractDir, "media/images")
            if (imagesDir.exists() && imagesDir.isDirectory) {
                imagesDir.listFiles()?.forEach { imageFile ->
                    val dest = File(MediaStorageManager.getImagesDir(context), imageFile.name)
                    imageFile.copyTo(dest, overwrite = true)
                }
            }

            extractDir.deleteRecursively()
            Result.success(importedEntriesCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
