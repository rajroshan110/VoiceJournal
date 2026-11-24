package dev.voicejournal.transcription

import android.content.Context
import android.util.Log
import dev.voicejournal.transcription.engine.WhisperEngine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest

class WhisperModelIntegrityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testTrustedSha256ConstantMatchesProduction() {
        val expectedSha = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
        assertEquals(expectedSha, WhisperEngine.WHISPER_MODEL_EXPECTED_SHA256)
    }

    @Test
    fun testValidModelSha256Verification() {
        val sampleFile = tempFolder.newFile("sample_model.bin")
        val sampleData = "Whisper GGML Q5_1 Model Binary Verification Content".toByteArray()
        sampleFile.writeBytes(sampleData)

        val computedSha = calculateSha256(sampleFile)
        val expectedSha = MessageDigest.getInstance("SHA-256")
            .digest(sampleData)
            .joinToString("") { "%02x".format(it) }

        assertEquals(expectedSha, computedSha)
    }

    @Test
    fun testCorruptedModelFailsVerificationAndIsRejected() {
        val modelsDir = tempFolder.newFolder("models")
        val prodModel = File(modelsDir, "ggml-base-q5_1.bin")
        val initialProdBytes = "Valid Previous Production Model".toByteArray()
        prodModel.writeBytes(initialProdBytes)

        val tempDownload = File(modelsDir, "ggml-base-q5_1.bin.tmp")
        val corruptBytes = "Corrupted / Tampered Model Content".toByteArray()
        tempDownload.writeBytes(corruptBytes)

        val computedSha = calculateSha256(tempDownload)
        val expectedTrustedSha = WhisperEngine.WHISPER_MODEL_EXPECTED_SHA256

        // Must not match trusted constant
        assertFalse(computedSha == expectedTrustedSha)

        // Simulate rejection logic in WhisperEngine
        val isVerified = (computedSha == expectedTrustedSha)
        if (!isVerified) {
            tempDownload.delete()
        }

        // Assert temp download deleted and original production file left untouched
        assertFalse(tempDownload.exists())
        assertTrue(prodModel.exists())
        assertEquals(initialProdBytes.size.toLong(), prodModel.length())
    }

    @Test
    fun testAtomicPromotionOnVerifiedModel() {
        val modelsDir = tempFolder.newFolder("models")
        val prodModel = File(modelsDir, "ggml-base-q5_1.bin")
        val tempDownload = File(modelsDir, "ggml-base-q5_1.bin.tmp")

        val validBytes = "Verified Model Payload".toByteArray()
        tempDownload.writeBytes(validBytes)

        val computedSha = calculateSha256(tempDownload)
        val mockExpectedSha = computedSha // Matches

        var isPromoted = false
        if (computedSha == mockExpectedSha) {
            if (prodModel.exists()) prodModel.delete()
            isPromoted = tempDownload.renameTo(prodModel)
        }

        assertTrue(isPromoted)
        assertTrue(prodModel.exists())
        assertFalse(tempDownload.exists())
        assertEquals(validBytes.size.toLong(), prodModel.length())
    }

    @Test
    fun testRedirectUrlResolutionAndHttpsVerification() {
        val currentUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"

        // 1. Relative redirect resolution
        val relativeRedirect = "/cdn-lfs/ggml-base-q5_1.bin"
        val resolved = WhisperEngine.resolveRedirectUrl(currentUrl, relativeRedirect)
        assertEquals("https://huggingface.co/cdn-lfs/ggml-base-q5_1.bin", resolved)
        assertTrue(resolved.startsWith("https://"))

        // 2. Sibling relative redirect resolution
        val siblingRedirect = "ggml-base-q5_1-mirror.bin"
        val resolvedSibling = WhisperEngine.resolveRedirectUrl(currentUrl, siblingRedirect)
        assertEquals("https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1-mirror.bin", resolvedSibling)
        assertTrue(resolvedSibling.startsWith("https://"))

        // 3. Absolute HTTPS redirect resolution (case-insensitive scheme)
        val absoluteHttpsRedirect = "HTTPS://cdn.huggingface.co/model.bin"
        val resolvedAbsolute = WhisperEngine.resolveRedirectUrl(currentUrl, absoluteHttpsRedirect)
        assertEquals("https://cdn.huggingface.co/model.bin", resolvedAbsolute.lowercase())
        assertTrue(java.net.URL(resolvedAbsolute).protocol.equals("https", ignoreCase = true))

        // 4. Insecure HTTP redirect (including mixed-case) must throw SecurityException
        val insecureRedirect = "http://insecure-cdn.com/model.bin"
        assertThrows(SecurityException::class.java) {
            WhisperEngine.resolveRedirectUrl(currentUrl, insecureRedirect)
        }
        assertThrows(SecurityException::class.java) {
            WhisperEngine.resolveRedirectUrl(currentUrl, "HTTP://insecure-cdn.com/model.bin")
        }

        // 5. Insecure non-HTTPS schemes (e.g. file:, ftp:) must throw SecurityException
        assertThrows(SecurityException::class.java) {
            WhisperEngine.resolveRedirectUrl(currentUrl, "file:///etc/passwd")
        }
        assertThrows(SecurityException::class.java) {
            WhisperEngine.resolveRedirectUrl(currentUrl, "ftp://example.com/model.bin")
        }
    }

    @Test
    fun testEnsureModelDownloadedDisablesAutoRedirectsAndBlocksHttpRedirect() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        try {
            val filesDir = tempFolder.newFolder("files")
            val context = mockk<Context>(relaxed = true) {
                every { this@mockk.filesDir } returns filesDir
            }
            val engine = WhisperEngine(context)

            val connections = mutableListOf<HttpURLConnection>()
            engine.connectionFactory = { url ->
                mockk<HttpURLConnection>(relaxed = true) {
                    every { responseCode } returns HttpURLConnection.HTTP_MOVED_TEMP
                    every { getHeaderField("Location") } returns "http://insecure.example.com/model.bin"
                }.also { connections.add(it) }
            }

            val result = engine.ensureModelDownloaded()

            assertFalse(result)
            assertTrue(connections.isNotEmpty())
            connections.forEach { conn ->
                verify { conn.instanceFollowRedirects = false }
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun testEnsureModelDownloadedFollowsHttpsRedirectsWithManualRedirectDisabled() = runBlocking {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        try {
            val filesDir = tempFolder.newFolder("files_https")
            val context = mockk<Context>(relaxed = true) {
                every { this@mockk.filesDir } returns filesDir
            }
            val engine = WhisperEngine(context)

            val requestedUrls = mutableListOf<String>()
            val connections = mutableListOf<HttpURLConnection>()
            engine.connectionFactory = { url ->
                val urlStr = url.toString()
                requestedUrls.add(urlStr)
                val conn = if (requestedUrls.size == 1) {
                    mockk<HttpURLConnection>(relaxed = true) {
                        every { responseCode } returns HttpURLConnection.HTTP_MOVED_PERM
                        every { getHeaderField("Location") } returns "HTTPS://cdn.huggingface.co/verified-model.bin"
                    }
                } else {
                    mockk<HttpURLConnection>(relaxed = true) {
                        every { responseCode } returns HttpURLConnection.HTTP_OK
                        every { contentLengthLong } returns 4L
                        every { inputStream } returns ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
                    }
                }
                connections.add(conn)
                conn
            }

            // Fails size/hash check after following HTTPS redirect, proving redirect happened safely
            val result = engine.ensureModelDownloaded()

            assertFalse(result)
            assertTrue(requestedUrls.any { it.equals("https://cdn.huggingface.co/verified-model.bin", ignoreCase = true) })
            connections.forEach { conn ->
                verify { conn.instanceFollowRedirects = false }
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun testThreadSchedulingClampRange() {
        assertEquals(2, WhisperEngine.calculateNumThreads(0))
        assertEquals(2, WhisperEngine.calculateNumThreads(1))
        assertEquals(2, WhisperEngine.calculateNumThreads(2))
        assertEquals(3, WhisperEngine.calculateNumThreads(3))
        assertEquals(4, WhisperEngine.calculateNumThreads(4))
        assertEquals(4, WhisperEngine.calculateNumThreads(8))
        assertEquals(4, WhisperEngine.calculateNumThreads(16))
    }
}
