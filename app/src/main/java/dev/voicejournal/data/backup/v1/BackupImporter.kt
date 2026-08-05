package dev.voicejournal.data.backup.v1

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.backup.v1.dto.ImportResult
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val prefsManager: UserPreferencesManager
) {

    private val unpacker = BackupUnpacker(context)
    private val validator = BackupValidator()
    private val restorer = BackupRestorer(context, database, prefsManager)

    /**
     * Imports journal data from an InputStream (e.g. from Storage Access Framework).
     */
    suspend fun importFromStream(
        inputStream: InputStream,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d("Backup", "Restore started")
        onProgress?.invoke("Validating backup…")

        var stagingDir: File? = null
        try {
            // 1. Unpack Zip Archive safely
            stagingDir = unpacker.unpack(inputStream)

            // 2. Validate Archive against Format v1 Specification
            val report = validator.validate(stagingDir)
            if (!report.isValid) {
                unpacker.cleanup(stagingDir)
                Log.d("Backup", "Validation failed")
                report.errors.forEach { err -> Log.d("Backup", err) }
                Log.d("Backup", "Restore aborted")
                return@withContext ImportResult(
                    success = false,
                    validationErrors = report.errors
                )
            }
            Log.d("Backup", "Backup validated")

            // 3. Perform Atomic Restore
            onProgress?.invoke("Reading backup…")
            val stats = restorer.restore(
                stagingDir = stagingDir,
                entries = report.entries,
                tags = report.tags,
                attachments = report.attachments,
                preferences = report.preferences,
                onProgress = onProgress
            )

            // 4. Clean up staging directory
            onProgress?.invoke("Finalizing restore…")
            unpacker.cleanup(stagingDir)

            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d("Backup", "Restore completed in ${"%.1f".format(elapsedSec)} s")
            onProgress?.invoke("Restore completed.")

            ImportResult(
                success = true,
                stats = stats
            )
        } catch (e: Exception) {
            unpacker.cleanup(stagingDir)
            Log.e("Backup", "Restore failed: ${e.message}", e)
            ImportResult(
                success = false,
                validationErrors = listOf(e.localizedMessage ?: "Import failed due to an unexpected error.")
            )
        }
    }

    /**
     * Imports journal data from a local [zipFile].
     */
    suspend fun importFromFile(
        zipFile: File,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        if (!zipFile.exists() || !zipFile.isFile) {
            return@withContext ImportResult(
                success = false,
                validationErrors = listOf("Source backup file does not exist: ${zipFile.absolutePath}")
            )
        }
        FileInputStream(zipFile).use { fis ->
            importFromStream(fis, onProgress)
        }
    }

    /**
     * Imports journal data from a Storage Access Framework [uri].
     */
    suspend fun importFromUri(
        uri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { isStream ->
                importFromStream(isStream, onProgress)
            } ?: ImportResult(
                success = false,
                validationErrors = listOf("Could not open input stream for URI: $uri")
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                validationErrors = listOf(e.localizedMessage ?: "Failed to open backup document URI.")
            )
        }
    }
}
