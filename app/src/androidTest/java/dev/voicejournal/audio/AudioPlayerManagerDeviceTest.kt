package dev.voicejournal.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AudioPlayerManagerDeviceTest {

    private lateinit var context: Context
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var testAudioFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        audioPlayerManager = AudioPlayerManager(context)

        // Generate a minimal valid WAV file
        testAudioFile = File(context.cacheDir, "test_tone.wav")
        createSineWaveWavFile(testAudioFile, durationSeconds = 1)
    }

    @After
    fun tearDown() {
        runBlocking(Dispatchers.Main) {
            audioPlayerManager.release()
        }
        if (testAudioFile.exists()) {
            testAudioFile.delete()
        }
    }

    @Test
    fun testEmptyPathEmitsErrorImmediately() = runBlocking(Dispatchers.Main) {
        audioPlayerManager.play("", "Empty Path Test", 101L)
        val state = audioPlayerManager.playbackState.value
        assertTrue("Empty path must emit PlayerState.Error", state is PlayerState.Error)
        assertEquals("Audio path is empty", (state as PlayerState.Error).message)
    }

    @Test
    fun testNonExistentFileEmitsErrorImmediately() = runBlocking(Dispatchers.Main) {
        audioPlayerManager.play("/path/to/non_existent_audio_file.wav", "Missing File Test", 102L)
        val state = audioPlayerManager.playbackState.value
        assertTrue("Non-existent file must emit PlayerState.Error", state is PlayerState.Error)
        assertEquals("Audio file does not exist or is empty", (state as PlayerState.Error).message)
    }

    @Test
    fun testValidAudioPlaybackAndRetryAfterError() = runBlocking(Dispatchers.Main) {
        // Step 1: Trigger an error first with non-existent file
        audioPlayerManager.play("/path/to/missing.wav", "Error Test", 103L)
        assertTrue(audioPlayerManager.playbackState.value is PlayerState.Error)

        // Step 2: Now start valid playback - it must clear the error and proceed
        audioPlayerManager.play(testAudioFile.absolutePath, "Valid Playback Test", 104L)
        
        // Allow player to prepare and start
        delay(500)
        
        val playingOrPausedState = audioPlayerManager.playbackState.value
        assertTrue(
            "State after valid play() must not remain Error; got: $playingOrPausedState",
            playingOrPausedState is PlayerState.Playing || playingOrPausedState is PlayerState.Paused
        )

        // Step 3: Pause and stop
        audioPlayerManager.pause()
        delay(100)
        assertTrue(audioPlayerManager.playbackState.value is PlayerState.Paused)

        audioPlayerManager.stop()
        delay(100)
        assertEquals(PlayerState.Idle, audioPlayerManager.playbackState.value)
    }

    private fun createSineWaveWavFile(file: File, durationSeconds: Int = 1) {
        val sampleRate = 16000
        val numSamples = sampleRate * durationSeconds
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = numSamples * blockAlign
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToByteArray(totalSize))
            out.write("WAVE".toByteArray())

            // fmt subchunk
            out.write("fmt ".toByteArray())
            out.write(intToByteArray(16))
            out.write(shortToByteArray(1.toShort()))
            out.write(shortToByteArray(numChannels.toShort()))
            out.write(intToByteArray(sampleRate))
            out.write(intToByteArray(byteRate))
            out.write(shortToByteArray(blockAlign.toShort()))
            out.write(shortToByteArray(bitsPerSample.toShort()))

            // data subchunk
            out.write("data".toByteArray())
            out.write(intToByteArray(dataSize))

            // 440 Hz Sine wave samples
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * 440.0 * i / sampleRate
                val sample = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.3).toInt().toShort()
                out.write(shortToByteArray(sample))
            }
        }
    }

    private fun intToByteArray(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortToByteArray(value: Short): ByteArray = byteArrayOf(
        (value.toInt() and 0xFF).toByte(),
        ((value.toInt() shr 8) and 0xFF).toByte()
    )
}
