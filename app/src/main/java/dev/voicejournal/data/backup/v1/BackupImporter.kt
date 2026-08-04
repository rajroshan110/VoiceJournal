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
    suspend fun importFromStream(inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        var stagingDir: File? = null
        try {
            // 1. Unpack Zip Archive safely
            stagingDir = unpacker.unpack(inputStream)

            // 2. Validate Archive against Format v1 Specification
            val report = validator.validate(stagingDir)
            if (!report.isValid) {
                unpacker.cleanup(stagingDir)
                return@withContext ImportResult(
                    success = false,
                    validationErrors = report.errors
                )
            }

            // 3. Perform Atomic Restore
            val stats = restorer.restore(
                stagingDir = stagingDir,
                entries = report.entries,
                tags = report.tags,
                attachments = report.attachments,
                preferences = report.preferences
            )

            // 4. Clean up staging directory
            unpacker.cleanup(stagingDir)

            ImportResult(
                success = true,
                stats = stats
            )
        } catch (e: Exception) {
            unpacker.cleanup(stagingDir)
            ImportResult(
                success = false,
                validationErrors = listOf(e.localizedMessage ?: "Import failed due to an unexpected error.")
            )
        }
    }

    /**
     * Imports journal data from a local [zipFile].
     */
    suspend fun importFromFile(zipFile: File): ImportResult = withContext(Dispatchers.IO) {
        if (!zipFile.exists() || !zipFile.isFile) {
            return@withContext ImportResult(
                success = false,
                validationErrors = listOf("Source backup file does not exist: ${zipFile.absolutePath}")
            )
        }
        FileInputStream(zipFile).use { fis ->
            importFromStream(fis)
        }
    }

    /**
     * Imports journal data from a Storage Access Framework [uri].
     */
    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { isStream ->
                importFromStream(isStream)
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
