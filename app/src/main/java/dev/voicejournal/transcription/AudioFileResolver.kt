package dev.voicejournal.transcription

import android.content.Context
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File

object AudioFileResolver {
    /**
     * Resolves direct file paths, file:// URIs, and content:// URIs into a physical, readable File.
     */
    fun resolveAudioFile(context: Context, pathOrUri: String): File? {
        if (pathOrUri.isBlank()) return null

        // 1. Direct local file path
        if (pathOrUri.startsWith("/")) {
            val file = File(pathOrUri)
            if (file.exists() && file.length() > 0L) return file
        }

        // 2. file:// URI schema
        if (pathOrUri.startsWith("file://")) {
            val file = File(pathOrUri.removePrefix("file://"))
            if (file.exists() && file.length() > 0L) return file
        }

        // 3. content:// URI or external storage URI
        try {
            val uri = android.net.Uri.parse(pathOrUri)
            val cacheDir = MediaStorageManager.getTranscribeCacheDir(context)
            val tempFile = File(cacheDir, "transcribe_${System.currentTimeMillis()}.m4a")

            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0L) {
                return tempFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
