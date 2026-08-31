package dev.voicejournal.data.backup.v1

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val prefsManager: UserPreferencesManager
) {

    private val serializer = BackupSerializer()
    private val mediaCollector = BackupMediaCollector(context)
    private val manifestBuilder = BackupManifestBuilder(context)
    private val zipWriter = BackupZipWriter()

    /**
     * Exports complete journal data into a byte-for-byte compliant Backup Format v1 archive
     * written directly to [outputStream] (e.g. from Storage Access Framework).
     */
    suspend fun exportToStream(
        outputStream: OutputStream,
        onProgress: ((String) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d("Backup", "Export started")
        onProgress?.invoke("Preparing backup…")

        runCatching {
            // 1. Fetch entries and tags from database
            onProgress?.invoke("Reading notes…")
            val entries = database.journalEntryDao().getAllEntriesForBackup()
            val tags = database.tagDao().getAllTagsSync()
            Log.d("Backup", "Entries exported: ${entries.size}")
            Log.d("Backup", "Tags exported: ${tags.size}")

            // 2. Collect media & construct attachment DTOs
            val collectedMedia = mediaCollector.collectMediaForEntries(entries)
            val attachmentsByEntryUuid = collectedMedia
                .map { it.attachment }
                .groupBy { it.entryUuid }

            Log.d("Backup", "Attachments exported: ${collectedMedia.size}")
            Log.d("Backup", "Media copied: ${collectedMedia.size}")
            onProgress?.invoke("Copying media (${collectedMedia.size} / ${collectedMedia.size})…")

            // 3. Collect DataStore preferences
            val audioFormat = prefsManager.audioFormat.first().name
            val whisperModel = prefsManager.whisperModel.first()
            val dailyReminder = prefsManager.dailyReminder.first()
            val themeMode = prefsManager.appThemeMode.first().name
            val timeFormat = prefsManager.timeFormat.first().name
            val startOfWeek = prefsManager.startOfWeek.first().name
            val appLockMode = prefsManager.appLockMode.first().name
            val appLockTimeout = prefsManager.appLockTimeout.first().name
            val insightDateRangeMode = prefsManager.insightDateRangeMode.first().name
            val sortOption = prefsManager.sortOption.first().name
            val folderGridView = prefsManager.folderIsGridView.first()
            val tagGridView = prefsManager.tagIsGridView.first()
            val markdownEnabled = prefsManager.isMarkdownEnabled.first()
            val screenPrivacyEnabled = prefsManager.isScreenPrivacyEnabled.first()
            val speechToTextEnabled = prefsManager.isSpeechToTextEnabled.first()
            val isFolderEnabled = prefsManager.isFolderEnabled.first()
            val isNotesOrganisationEnabled = prefsManager.isNotesOrganisationEnabled.first()
            val isTopicsEnabled = prefsManager.isTopicsEnabled.first()
            val isPeopleEnabled = prefsManager.isPeopleEnabled.first()
            val isMoodEnabled = prefsManager.isMoodEnabled.first()

            val colorTheme = "Default"
            val autoTranscribe = true
            val trashRetentionDays = 7

            // 4. Serialize core DTOs into JSON byte arrays
            val entriesJsonStr = serializer.serializeEntries(entries, attachmentsByEntryUuid)
            val tagsJsonStr = serializer.serializeTags(tags)
            val attachmentsJsonStr = serializer.serializeAttachments(collectedMedia.map { it.attachment })
            val preferencesJsonStr = serializer.serializePreferences(
                audioFormat = audioFormat,
                whisperModel = whisperModel,
                dailyReminder = dailyReminder,
                themeMode = themeMode,
                colorTheme = colorTheme,
                timeFormat = timeFormat,
                startOfWeek = startOfWeek,
                appLockMode = appLockMode,
                appLockTimeout = appLockTimeout,
                insightDateRangeMode = insightDateRangeMode,
                autoTranscribe = autoTranscribe,
                trashRetentionDays = trashRetentionDays,
                sortOption = sortOption,
                folderGridView = folderGridView,
                tagGridView = tagGridView,
                markdownEnabled = markdownEnabled,
                screenPrivacyEnabled = screenPrivacyEnabled,
                speechToTextEnabled = speechToTextEnabled,
                isFolderEnabled = isFolderEnabled,
                isNotesOrganisationEnabled = isNotesOrganisationEnabled,
                isTopicsEnabled = isTopicsEnabled,
                isPeopleEnabled = isPeopleEnabled,
                isMoodEnabled = isMoodEnabled
            )

            val entriesJsonBytes = entriesJsonStr.toByteArray(StandardCharsets.UTF_8)
            val tagsJsonBytes = tagsJsonStr.toByteArray(StandardCharsets.UTF_8)
            val attachmentsJsonBytes = attachmentsJsonStr.toByteArray(StandardCharsets.UTF_8)
            val preferencesJsonBytes = preferencesJsonStr.toByteArray(StandardCharsets.UTF_8)

            // 5. Build manifest JSON
            val manifestJsonStr = manifestBuilder.buildManifest(
                entriesCount = entries.size,
                tagsCount = tags.size,
                attachmentsCount = collectedMedia.size,
                mediaFilesCount = collectedMedia.size,
                entriesJsonBytes = entriesJsonBytes,
                tagsJsonBytes = tagsJsonBytes,
                attachmentsJsonBytes = attachmentsJsonBytes,
                preferencesJsonBytes = preferencesJsonBytes
            )
            val manifestJsonBytes = manifestJsonStr.toByteArray(StandardCharsets.UTF_8)

            // 6. Stream package into ZIP archive
            onProgress?.invoke("Creating archive…")
            zipWriter.writeBackup(
                outputStream = outputStream,
                manifestJsonBytes = manifestJsonBytes,
                entriesJsonBytes = entriesJsonBytes,
                tagsJsonBytes = tagsJsonBytes,
                attachmentsJsonBytes = attachmentsJsonBytes,
                preferencesJsonBytes = preferencesJsonBytes,
                collectedMedia = collectedMedia
            )
            Log.d("Backup", "Archive created")

            onProgress?.invoke("Finalizing backup…")
            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d("Backup", "Export completed in ${"%.1f".format(elapsedSec)} s")
            onProgress?.invoke("Backup completed.")
            Unit
        }.onFailure { e ->
            Log.e("Backup", "Export failed: ${e.message}", e)
        }
    }

    /**
     * Exports complete journal data into a target local [targetFile].
     * Uses atomic temporary file writing to ensure partial archives are cleaned up on failure.
     */
    suspend fun exportToFile(
        targetFile: File,
        onProgress: ((String) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempFile = File(targetFile.parentFile ?: context.cacheDir, "${targetFile.name}.tmp")
        try {
            if (tempFile.exists()) tempFile.delete()
            FileOutputStream(tempFile).use { fos ->
                val result = exportToStream(fos, onProgress)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Export stream failed")
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }
}
