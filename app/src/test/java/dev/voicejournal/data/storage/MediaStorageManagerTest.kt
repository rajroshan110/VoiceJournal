package dev.voicejournal.data.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MediaStorageManagerTest {

    private lateinit var mockContext: Context
    private lateinit var tempFilesDir: File
    private lateinit var tempCacheDir: File

    @Before
    fun setup() {
        tempFilesDir = Files.createTempDirectory("vj_test_files").toFile()
        tempCacheDir = Files.createTempDirectory("vj_test_cache").toFile()
        
        mockContext = mockk()
        every { mockContext.filesDir } returns tempFilesDir
        every { mockContext.cacheDir } returns tempCacheDir
    }

    @After
    fun teardown() {
        tempFilesDir.deleteRecursively()
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun `test valid internal media paths`() {
        val validFile = File(tempFilesDir, "recordings/audio.m4a")
        // It does not need to exist for isInternalMedia check, but let's make the directories
        validFile.parentFile?.mkdirs()
        validFile.createNewFile()

        assertTrue(MediaStorageManager.isInternalMedia(mockContext, validFile.absolutePath))

        val validCache = File(tempCacheDir, "audio_transcribe_cache/temp.wav")
        validCache.parentFile?.mkdirs()
        validCache.createNewFile()

        assertTrue(MediaStorageManager.isInternalMedia(mockContext, validCache.absolutePath))
    }

    @Test
    fun `test path traversal rejection`() {
        // Attempt to traverse out of filesDir
        val maliciousPath = "${tempFilesDir.absolutePath}/recordings/../../malicious.txt"
        
        // Canonical path will resolve outside of filesDir and cacheDir
        assertFalse(MediaStorageManager.isInternalMedia(mockContext, maliciousPath))
    }

    @Test
    fun `test completely external path rejection`() {
        val externalDir = Files.createTempDirectory("vj_external").toFile()
        val externalFile = File(externalDir, "external_audio.m4a")
        externalFile.createNewFile()

        assertFalse(MediaStorageManager.isInternalMedia(mockContext, externalFile.absolutePath))
        
        externalDir.deleteRecursively()
    }
}
