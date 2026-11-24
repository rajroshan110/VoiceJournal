package dev.voicejournal.transcription

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavToFloatConverterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun writeWavFile(
        file: File,
        channels: Short,
        sampleRate: Int,
        bitsPerSample: Short,
        samples: ShortArray,
        headerDataSizeOverride: Int? = null
    ) {
        val pcmBytes = ByteArray(samples.size * 2)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)
        val dataSize = headerDataSizeOverride ?: pcmBytes.size

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(channels)
            putInt(sampleRate)
            putInt(sampleRate * channels * (bitsPerSample / 8))
            putShort((channels * (bitsPerSample / 8)).toShort())
            putShort(bitsPerSample)
            put("data".toByteArray())
            putInt(dataSize)
        }.array()

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
    }

    @Test
    fun testStreamingMonoWavAcrossMultipleChunks() {
        // 12,000 samples * 2 bytes = 24,000 bytes (> 16 KB streaming buffer)
        val sampleCount = 12_000
        val samples = ShortArray(sampleCount) { i ->
            when (i % 4) {
                0 -> 0
                1 -> 16384
                2 -> -16384
                else -> Short.MAX_VALUE
            }
        }
        val wavFile = tempFolder.newFile("mono_stream.wav")
        writeWavFile(wavFile, channels = 1, sampleRate = 16000, bitsPerSample = 16, samples = samples)

        val result = WavToFloatConverter.convertWavToFloatArray(wavFile)

        assertEquals(sampleCount, result.size)
        assertEquals(0.0f, result[0], 1e-5f)
        assertEquals(0.5f, result[1], 1e-5f)
        assertEquals(-0.5f, result[2], 1e-5f)
        assertEquals(32767.0f / 32768.0f, result[3], 1e-5f)
        // Verify tail samples past the 16 KB boundary
        assertEquals(0.5f, result[sampleCount - 3], 1e-5f)
        assertEquals(-0.5f, result[sampleCount - 2], 1e-5f)
        assertEquals(32767.0f / 32768.0f, result[sampleCount - 1], 1e-5f)
    }

    @Test
    fun testStreamingStereoWavDownmixesToMonoAcrossBufferBoundary() {
        // 5,000 stereo frames * 4 bytes/frame = 20,000 bytes (> 16 KB buffer, tests frame boundary)
        // Plus add 2 bytes past 16 KB alignment by having 4,097 frames (16,388 bytes)
        val frameCount = 4_500
        val interleaved = ShortArray(frameCount * 2) { idx ->
            val frame = idx / 2
            val isLeft = idx % 2 == 0
            if (isLeft) {
                if (frame % 2 == 0) 16384 else -8192
            } else {
                if (frame % 2 == 0) 0 else -16384
            }
        }
        val wavFile = tempFolder.newFile("stereo_stream.wav")
        writeWavFile(wavFile, channels = 2, sampleRate = 16000, bitsPerSample = 16, samples = interleaved)

        val result = WavToFloatConverter.convertWavToFloatArray(wavFile)

        assertEquals(frameCount, result.size)
        // Even frames: (16384/32768 + 0/32768) / 2 = 0.25
        assertEquals(0.25f, result[0], 1e-5f)
        assertEquals(0.25f, result[frameCount - 2], 1e-5f)
        // Odd frames: (-8192/32768 + -16384/32768) / 2 = (-0.25 - 0.5) / 2 = -0.375
        assertEquals(-0.375f, result[1], 1e-5f)
        assertEquals(-0.375f, result[frameCount - 1], 1e-5f)
    }

    @Test
    fun testOversizedHeaderDataSizeClampedToRemainingFileBytes() {
        val samples = shortArrayOf(8192, -8192, 16384, -16384)
        val wavFile = tempFolder.newFile("clamped_size.wav")
        writeWavFile(
            wavFile,
            channels = 1,
            sampleRate = 16000,
            bitsPerSample = 16,
            samples = samples,
            headerDataSizeOverride = Int.MAX_VALUE
        )

        val result = WavToFloatConverter.convertWavToFloatArray(wavFile)

        assertEquals(4, result.size)
        assertEquals(0.25f, result[0], 1e-5f)
        assertEquals(-0.25f, result[1], 1e-5f)
        assertEquals(0.5f, result[2], 1e-5f)
        assertEquals(-0.5f, result[3], 1e-5f)
    }

    @Test
    fun testCorruptNegativeDataSizeClampedSafely() {
        val samples = shortArrayOf(16384, -16384)
        val wavFile = tempFolder.newFile("negative_size.wav")
        writeWavFile(
            wavFile,
            channels = 1,
            sampleRate = 16000,
            bitsPerSample = 16,
            samples = samples,
            headerDataSizeOverride = -1
        )

        val result = WavToFloatConverter.convertWavToFloatArray(wavFile)

        assertEquals(2, result.size)
        assertEquals(0.5f, result[0], 1e-5f)
        assertEquals(-0.5f, result[1], 1e-5f)
    }

    @Test
    fun testInvalidWavThrowsIllegalArgument() {
        val shortFile = tempFolder.newFile("short.wav").apply { writeBytes(ByteArray(10)) }
        assertThrows(IllegalArgumentException::class.java) {
            WavToFloatConverter.convertWavToFloatArray(shortFile)
        }

        val badBitsFile = tempFolder.newFile("8bit.wav")
        writeWavFile(badBitsFile, channels = 1, sampleRate = 16000, bitsPerSample = 8, samples = shortArrayOf(100))
        assertThrows(IllegalArgumentException::class.java) {
            WavToFloatConverter.convertWavToFloatArray(badBitsFile)
        }
    }
}
