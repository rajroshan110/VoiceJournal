package dev.voicejournal.audio

import android.content.Context
import dev.voicejournal.data.storage.MediaStorageManager
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One-time migration utility to repair WAV files that were saved with
 * corrupt headers (0-byte data size) due to the stopRecording() race condition.
 *
 * WAV header layout (44 bytes):
 *   Bytes 0-3:   "RIFF"
 *   Bytes 4-7:   ChunkSize = 36 + dataSize (little-endian Int)
 *   Bytes 8-11:  "WAVE"
 *   Bytes 12-15: "fmt "
 *   Bytes 16-19: 16 (SubChunk1Size)
 *   ...
 *   Bytes 36-39: "data"
 *   Bytes 40-43: dataSize (little-endian Int)
 *   Bytes 44+:   actual PCM data
 */
object AudioFileRepair {

    private const val PREFS_NAME = "audio_repair_prefs"
    private const val KEY_REPAIRED = "wav_headers_repaired_v1"

    /**
     * Run once on app startup. Scans the audio directory for WAV files
     * with corrupt headers and rewrites the size fields based on actual file size.
     */
    fun repairIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REPAIRED, false)) return

        try {
            val audioDirs = MediaStorageManager.getAllAudioDirs(context)
            if (audioDirs.none { it.exists() }) {
                prefs.edit().putBoolean(KEY_REPAIRED, true).apply()
                return
            }

            audioDirs.filter { it.exists() }.forEach { dir ->
                val wavFiles = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }
                wavFiles?.forEach { file ->
                    repairWavHeader(file)
                }
            }

            prefs.edit().putBoolean(KEY_REPAIRED, true).apply()
        } catch (e: Exception) {
            // Don't crash the app on repair failure — will retry next launch
        }
    }

    private fun repairWavHeader(file: File) {
        if (file.length() < 44) return // Too small to be a valid WAV

        try {
            val raf = RandomAccessFile(file, "rw")

            // Read the first 4 bytes to verify it's RIFF
            val magic = ByteArray(4)
            raf.seek(0)
            raf.readFully(magic)
            if (String(magic) != "RIFF") {
                raf.close()
                return
            }

            // Read current ChunkSize (bytes 4-7)
            raf.seek(4)
            val chunkSizeBytes = ByteArray(4)
            raf.readFully(chunkSizeBytes)
            val currentChunkSize = ByteBuffer.wrap(chunkSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int

            // Read current DataSize (bytes 40-43)
            raf.seek(40)
            val dataSizeBytes = ByteArray(4)
            raf.readFully(dataSizeBytes)
            val currentDataSize = ByteBuffer.wrap(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int

            // Calculate what the correct values should be
            val actualDataSize = (file.length() - 44).toInt().coerceAtLeast(0)
            val actualChunkSize = 36 + actualDataSize

            // Only repair if the header values are wrong (typically 0 or 36)
            if (currentDataSize != actualDataSize || currentChunkSize != actualChunkSize) {
                // Write corrected ChunkSize at bytes 4-7
                raf.seek(4)
                val fixedChunkSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(actualChunkSize).array()
                raf.write(fixedChunkSize)

                // Write corrected DataSize at bytes 40-43
                raf.seek(40)
                val fixedDataSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(actualDataSize).array()
                raf.write(fixedDataSize)
            }

            raf.close()
        } catch (e: Exception) {
            // Skip this file if repair fails
        }
    }
}
