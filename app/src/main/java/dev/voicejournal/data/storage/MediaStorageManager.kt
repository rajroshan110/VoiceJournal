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
        return File(dir, "record_${System.currentTimeMillis()}$extension")
    }
    
    fun generateImageFile(context: Context, extension: String = "jpg"): File {
        val dir = getImagesDir(context)
        return File(dir, "image_${System.currentTimeMillis()}_${(100..999).random()}.$extension")
    }

    fun getAudioFile(context: Context, fileNameOrPath: String): File {
        val fileName = if (fileNameOrPath.contains("/")) File(fileNameOrPath).name else fileNameOrPath
        // Check legacy first for backward compatibility with existing files
        val legacyFile = File(getLegacyAudioDir(context), fileName)
        if (legacyFile.exists()) {
            return legacyFile
        }
        return File(getRecordingsDir(context), fileName)
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
