package dev.voicejournal.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    suspend fun exportData(outputZipFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val entries = database.journalEntryDao().getAllEntriesSync()
            val tags = database.tagDao().getAllTagsSync()
            val crossRefs = database.journalEntryDao().getAllEntryTagCrossRefsSync()

            val jsonRoot = JSONObject()

            val entriesArray = JSONArray()
            entries.forEach { entry ->
                val entryObj = JSONObject().apply {
                    put("id", entry.id)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                    put("audioPath", entry.audioPath)
                    put("audioFormat", entry.audioFormat)
                    put("duration", entry.duration)
                    put("transcript", entry.transcript ?: "")
                    put("userText", entry.userText ?: "")
                    put("moodEmoji", entry.moodEmoji ?: "")
                    put("moodLevel", entry.moodLevel ?: 0)
                    put("hasTranscript", entry.hasTranscript)
                }
                entriesArray.put(entryObj)
            }
            jsonRoot.put("entries", entriesArray)

            val tagsArray = JSONArray()
            tags.forEach { tag ->
                val tagObj = JSONObject().apply {
                    put("id", tag.id)
                    put("name", tag.name)
                    put("type", tag.type)
                }
                tagsArray.put(tagObj)
            }
            jsonRoot.put("tags", tagsArray)

            val crossRefsArray = JSONArray()
            crossRefs.forEach { ref ->
                val refObj = JSONObject().apply {
                    put("entryId", ref.entryId)
                    put("tagId", ref.tagId)
                }
                crossRefsArray.put(refObj)
            }
            jsonRoot.put("crossRefs", crossRefsArray)

            val jsonString = jsonRoot.toString(2)
            val tempJsonFile = File(context.cacheDir, "backup_data.json")
            tempJsonFile.writeText(jsonString)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
                // Add JSON
                zos.putNextEntry(ZipEntry("backup_data.json"))
                tempJsonFile.inputStream().copyTo(zos)
                zos.closeEntry()

                // Add Audio files
                entries.forEach { entry ->
                    if (entry.audioPath.isNotEmpty()) {
                        val audioFile = File(context.filesDir, entry.audioPath)
                        if (audioFile.exists()) {
                            zos.putNextEntry(ZipEntry("audio/${audioFile.name}"))
                            audioFile.inputStream().copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
            tempJsonFile.delete()
            Result.success(outputZipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
