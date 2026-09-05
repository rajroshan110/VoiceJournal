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
     * Run on app startup. Scans the audio directory for WAV files
     * with corrupt headers and rewrites the size fields based on actual file size.
     */
    fun repairIfNeeded(context: Context) {
        forceRepair(context)
    }

    /**
     * Unconditionally scan and repair all WAV files with corrupted headers.
     * Can be invoked on startup, pull-to-refresh or user-initiated repair.
     */
    fun forceRepair(context: Context) {
        try {
            val audioDirs = MediaStorageManager.getAllAudioDirs(context)
            audioDirs.filter { it.exists() }.forEach { dir ->
                val wavFiles = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }
                wavFiles?.forEach { file ->
                    repairWavFile(file)
                }
            }
        } catch (_: Exception) {
            // Don't crash
        }
    }

    fun repairWavFile(file: File): Boolean {
        if (!file.exists() || file.length() < 44) return false // Too small to be a valid WAV

        try {
            RandomAccessFile(file, "rw").use { raf ->
                // Read the first 4 bytes to verify it's RIFF
                val magic = ByteArray(4)
                raf.seek(0)
                raf.readFully(magic)
                if (String(magic) != "RIFF") {
                    return false
                }

                // Verify format is WAVE (bytes 8-11)
                val format = ByteArray(4)
                raf.seek(8)
                raf.readFully(format)
                if (String(format) != "WAVE") {
                    return false
                }

                // Verify data subchunk marker (bytes 36-39 for standard PCM)
                val dataMarker = ByteArray(4)
                raf.seek(36)
                raf.readFully(dataMarker)
                if (String(dataMarker) != "data") {
                    return false
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
                    return true
                }
                return true
            }
        } catch (_: Exception) {
            return false
        }
    }
}
