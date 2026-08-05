package dev.voicejournal.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.data.backup.v1.BackupExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter
) {

    suspend fun exportData(outputZipFile: File, onProgress: ((String) -> Unit)? = null): Result<File> {
        return backupExporter.exportToFile(outputZipFile, onProgress)
    }

    suspend fun exportToUri(targetUri: Uri, onProgress: ((String) -> Unit)? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { os ->
                val result = backupExporter.exportToStream(os, onProgress)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Export failed")
                }
            } ?: throw IllegalStateException("Could not open output stream for URI: $targetUri")
        }
    }
}
