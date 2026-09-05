package dev.voicejournal.data.repository

import android.content.Context
import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.data.storage.MediaStorageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class JournalRefreshHealingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var recordingsDir: File

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        recordingsDir = tempFolder.newFolder("recordings")
        val cacheDir = tempFolder.newFolder("cache")

        mockkObject(MediaStorageManager)
        every { MediaStorageManager.getAllAudioDirs(mockContext) } returns listOf(recordingsDir)
        every { MediaStorageManager.getTranscribeCacheDir(mockContext) } returns cacheDir
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testForceRepairFixesCorruptedWavHeader() {
        val corruptedWav = File(recordingsDir, "test_corrupt.wav")
        val pcmData = ByteArray(3200) { 1 } // 3200 bytes of mock PCM data
        val totalFileSize = 44 + pcmData.size

        val raf = RandomAccessFile(corruptedWav, "rw")
        // Write RIFF header with corrupted chunk size (36) and data size (0)
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array()) // Corrupt ChunkSize
        raf.write("WAVEfmt ".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // PCM
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // Mono
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16000).array()) // 16kHz
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32000).array()) // Byte rate
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array()) // Block align
        raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array()) // Bits per sample
        raf.write("data".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()) // Corrupt DataSize = 0
        raf.write(pcmData)
        raf.close()

        assertEquals(totalFileSize.toLong(), corruptedWav.length())

        // Run forceRepair
        AudioFileRepair.forceRepair(mockContext)

        // Verify that headers have been repaired based on actual file size
        val checkRaf = RandomAccessFile(corruptedWav, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val repairedChunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36 + pcmData.size, repairedChunkSize)

        checkRaf.seek(40)
        val dataSizeBuf = ByteArray(4)
        checkRaf.readFully(dataSizeBuf)
        val repairedDataSize = ByteBuffer.wrap(dataSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(pcmData.size, repairedDataSize)

        checkRaf.close()
    }

    @Test
    fun testForceRepairIgnoresNonWavRiffFiles() {
        val nonWavRiff = File(recordingsDir, "test_webp.wav")
        val content = ByteArray(100) { 0 }
        val raf = RandomAccessFile(nonWavRiff, "rw")
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array())
        raf.write("WEBP".toByteArray()) // Not WAVE!
        raf.write(content)
        raf.close()

        AudioFileRepair.forceRepair(mockContext)

        // Verify ChunkSize was NOT touched
        val checkRaf = RandomAccessFile(nonWavRiff, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val chunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36, chunkSize)
        checkRaf.close()
    }

    @Test
    fun testForceRepairIgnoresNonStandardDataMarker() {
        val nonStandardWav = File(recordingsDir, "test_nonstandard.wav")
        val content = ByteArray(100) { 0 }
        val raf = RandomAccessFile(nonStandardWav, "rw")
        raf.write("RIFF".toByteArray())
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36).array())
        raf.write("WAVEfmt ".toByteArray())
        raf.write(ByteArray(24) { 0 })
        raf.write("junk".toByteArray()) // Not 'data' marker at byte 36
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())
        raf.write(content)
        raf.close()

        AudioFileRepair.forceRepair(mockContext)

        val checkRaf = RandomAccessFile(nonStandardWav, "r")
        checkRaf.seek(4)
        val chunkSizeBuf = ByteArray(4)
        checkRaf.readFully(chunkSizeBuf)
        val chunkSize = ByteBuffer.wrap(chunkSizeBuf).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(36, chunkSize)
        checkRaf.close()
    }

    @Test
    fun testForceRepairHandlesTruncatedFileGracefully() {
        val truncated = File(recordingsDir, "truncated.wav")
        val raf = RandomAccessFile(truncated, "rw")
        raf.write("RIFF1234".toByteArray()) // 8 bytes only
        raf.close()

        // Should not throw or crash
        AudioFileRepair.forceRepair(mockContext)
        assertEquals(8L, truncated.length())
    }
}
