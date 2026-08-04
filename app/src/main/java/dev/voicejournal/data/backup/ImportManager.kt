package dev.voicejournal.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.backup.v1.BackupImporter
import dev.voicejournal.data.backup.v1.dto.ImportResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupImporter: BackupImporter
) {

    suspend fun importData(sourceFile: File): Result<Int> {
        val importResult = backupImporter.importFromFile(sourceFile)
        return if (importResult.success) {
            Result.success(importResult.stats.entriesRestored)
        } else {
            Result.failure(Exception(importResult.validationErrors.joinToString("; ")))
        }
    }

    suspend fun importFromUri(uri: Uri): ImportResult {
        return backupImporter.importFromUri(uri)
    }
}
