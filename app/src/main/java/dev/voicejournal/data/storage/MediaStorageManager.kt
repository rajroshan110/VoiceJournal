package dev.voicejournal.data.storage

import android.content.Context
import java.io.File

/**
 * Single centralized storage utility for all media types.
 * Standardizes storage on:
 * - filesDir/recordings/
 * - filesDir/images/
 */
object MediaStorageManager {

    const val DIR_RECORDINGS = "recordings"
    const val DIR_IMAGES = "images"
    private const val DIR_LEGACY_AUDIO = "audio"
    
    // For transcription temp files
    private const val DIR_TRANSCRIBE_CACHE = "audio_transcribe_cache"

    fun getRecordingsDir(context: Context): File {
        return File(context.filesDir, DIR_RECORDINGS).apply { if (!exists()) mkdirs() }
    }

    fun getImagesDir(context: Context): File {
        return File(context.filesDir, DIR_IMAGES).apply { if (!exists()) mkdirs() }
    }
    
    private fun getLegacyAudioDir(context: Context): File {
        return File(context.filesDir, DIR_LEGACY_AUDIO)
    }

    fun getAllAudioDirs(context: Context): List<File> {
        return listOf(getLegacyAudioDir(context), getRecordingsDir(context))
    }

    fun generateRecordingFile(context: Context, extension: String = "m4a"): File {
        val dir = getRecordingsDir(context)
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        return File(dir, "record_${System.currentTimeMillis()}$ext")
    }
    
    fun generateImageFile(context: Context, extension: String = "jpg"): File {
        val dir = getImagesDir(context)
        return File(dir, "image_${System.currentTimeMillis()}_${(100..999).random()}.$extension")
    }

    fun getAudioFile(context: Context, fileNameOrPath: String): File {
        if (fileNameOrPath.isBlank()) return File("")
        val directFile = File(fileNameOrPath)
        if (directFile.exists() && directFile.length() > 0L) {
            return directFile
        }

        val fileName = if (fileNameOrPath.contains("/")) directFile.name else fileNameOrPath
        // 1. Check legacy audio dir
        val legacyFile = File(getLegacyAudioDir(context), fileName)
        if (legacyFile.exists() && legacyFile.length() > 0L) {
            return legacyFile
        }

        // 2. Check current recordings dir
        val recordingsFile = File(getRecordingsDir(context), fileName)
        if (recordingsFile.exists() && recordingsFile.length() > 0L) {
            return recordingsFile
        }

        // 3. Check app internal files dir root
        val filesDirFile = File(context.filesDir, fileName)
        if (filesDirFile.exists() && filesDirFile.length() > 0L) {
            return filesDirFile
        }

        // 4. Check transcribe cache or cache dir
        val cacheFile = File(getTranscribeCacheDir(context), fileName)
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            return cacheFile
        }

        // 5. If fileName has no extension or different extension, check variants in all audio dirs
        val nameWithoutExt = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        for (dir in getAllAudioDirs(context)) {
            if (dir.exists()) {
                val match = dir.listFiles { f ->
                    f.name == fileName || f.nameWithoutExtension == nameWithoutExt
                }?.firstOrNull { it.length() > 0L }
                if (match != null) return match
            }
        }

        // Fallbacks if not found yet
        if (recordingsFile.exists()) return recordingsFile
        if (legacyFile.exists()) return legacyFile
        if (directFile.exists()) return directFile
        return recordingsFile
    }

    fun getImageFile(context: Context, fileNameOrPath: String): File {
        val fileName = if (fileNameOrPath.contains("/")) File(fileNameOrPath).name else fileNameOrPath
        return File(getImagesDir(context), fileName)
    }
    
    fun getTranscribeCacheDir(context: Context): File {
        return File(context.cacheDir, DIR_TRANSCRIBE_CACHE).apply { if (!exists()) mkdirs() }
    }
    
    // Check if path is internal and strictly within app storage
    fun isInternalMedia(context: Context, path: String): Boolean {
        return try {
            val file = File(path)
            val canonicalPath = file.canonicalPath
            val filesCanonical = context.filesDir.canonicalPath
            val cacheCanonical = context.cacheDir.canonicalPath
            canonicalPath.startsWith(filesCanonical) || canonicalPath.startsWith(cacheCanonical)
        } catch (e: Exception) {
            false
        }
    }

    // Completely clear all generated media
    fun clearAllMedia(context: Context) {
        val dirsToClear = listOf(
            getRecordingsDir(context),
            getImagesDir(context),
            getLegacyAudioDir(context),
            getTranscribeCacheDir(context)
        )
        dirsToClear.forEach { dir ->
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }
}
