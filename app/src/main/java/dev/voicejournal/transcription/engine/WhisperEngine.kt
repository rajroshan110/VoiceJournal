package dev.voicejournal.transcription.engine

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.voicejournal.transcription.M4aDecoder
import dev.voicejournal.transcription.WavToFloatConverter
import dev.voicejournal.transcription.WhisperLib
import dev.voicejournal.transcription.model.TranscriptResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.voicejournal.BuildConfig
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechToTextEngine {

    companion object {
        private const val TAG = "WhisperEngine"
        private const val PERF_TAG = "WhisperPerf"
        const val WHISPER_MODEL_EXPECTED_SHA256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"

        private val MODEL_URLS = listOf(
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            "https://github.com/ggerganov/whisper.cpp/raw/master/models/ggml-base-q5_1.bin"
        )

        fun resolveRedirectUrl(currentUrl: String, redirectLocation: String): String {
            val resolved = URL(URL(currentUrl), redirectLocation).toString()
            if (!resolved.startsWith("https://")) {
                throw SecurityException("Insecure redirect to non-HTTPS URL: $resolved")
            }
            return resolved
        }

        fun calculateNumThreads(availableProcessors: Int = Runtime.getRuntime().availableProcessors()): Int =
            availableProcessors.coerceIn(2, 4)
    }

    // Quantized 5-bit Multilingual Model (~59.7 MB, supports English & Hindi)
    private val modelFile = File(context.filesDir, "models/ggml-base-q5_1.bin")
    private val MODEL_NAME = "Whisper Base Q5 (Bilingual)"
    private val MODEL_VERSION = "1.0-q5_1"

    private val mutex = Mutex()

    // Model Context Cache
    private var cachedContextPtr: Long = 0L

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    override val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(isModelValid(modelFile))
    override val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private fun isModelValid(file: File): Boolean {
        // Only do a basic size check for existing models since hashing on every startup is slow.
        // Hashing is strictly enforced post-download before it becomes the production model.
        val valid = file.exists() && file.length() > 25_000_000L
        Log.d(TAG, "isModelValid for ${file.absolutePath}: $valid (size=${file.length()} bytes)")
        return valid
    }

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

    override fun isModelDownloadedSync(): Boolean {
        val valid = isModelValid(modelFile)
        _isModelDownloaded.value = valid
        return valid
    }

    override fun deleteModel(): Boolean {
        releaseContext()
        val deleted = if (modelFile.exists()) {
            modelFile.delete()
        } else {
            true
        }
        _isModelDownloaded.value = false
        _downloadProgress.value = null
        return deleted
    }

    override suspend fun ensureModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        if (isModelValid(modelFile)) {
            _isModelDownloaded.value = true
            return@withContext true
        }

        modelFile.parentFile?.mkdirs()
        val tempFile = File(modelFile.parentFile, "ggml-base-q5_1.bin.tmp")
        if (tempFile.exists()) tempFile.delete()

        for (urlStr in MODEL_URLS) {
            Log.d(TAG, "Attempting to download Whisper Base Q5_1 model from: $urlStr")
            try {
                _downloadProgress.value = 0f
                var currentUrl = urlStr
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 5

                try {
                    while (redirects < maxRedirects) {
                        val url = URL(currentUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0"
                        )
    
                        val status = connection.responseCode
                        Log.d(TAG, "HTTP status $status for $currentUrl")
                        if (status in 300..399) {
                            val newUrl = connection.getHeaderField("Location")
                            connection.disconnect()
                            if (!newUrl.isNullOrEmpty()) {
                                val resolvedUrl = resolveRedirectUrl(currentUrl, newUrl)
                                Log.d(TAG, "Redirecting to: $resolvedUrl")
                                currentUrl = resolvedUrl
                                redirects++
                                continue
                            }
                        }
                        break
                    }

                    if (redirects >= maxRedirects) {
                        Log.e(TAG, "Exceeded maximum redirect limit ($maxRedirects) for $urlStr")
                        continue
                    }
    
                    val finalConn = connection ?: continue
                    val responseCode = finalConn.responseCode
                    if (responseCode !in 200..299) {
                        Log.e(TAG, "Failed HTTP download with code $responseCode")
                        continue
                    }
    
                    val fileLength = finalConn.contentLengthLong
                    Log.d(TAG, "Download started, expected size: $fileLength bytes")
    
                    finalConn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val data = ByteArray(16384)
                            var total: Long = 0
                            var count: Int
                            while (input.read(data).also { count = it } != -1) {
                                total += count
                                if (fileLength > 0) {
                                    _downloadProgress.value = (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                                }
                                output.write(data, 0, count)
                            }
                            output.flush()
                        }
                    }
                } finally {
                    connection?.disconnect()
                }

                Log.d(TAG, "Finished downloading temp file size: ${tempFile.length()} bytes")

                if (tempFile.exists() && tempFile.length() > 25_000_000L) {
                    val actualSha = calculateSha256(tempFile)

                    if (actualSha != WHISPER_MODEL_EXPECTED_SHA256) {
                        throw Exception("SHA-256 verification failed! Expected $WHISPER_MODEL_EXPECTED_SHA256, got $actualSha")
                    }
                    
                    if (modelFile.exists()) modelFile.delete()
                    val renamed = tempFile.renameTo(modelFile)
                    if (renamed) {
                        _isModelDownloaded.value = true
                        _downloadProgress.value = null
                        Log.d(TAG, "Successfully saved Q5_1 model to ${modelFile.absolutePath}")
                        return@withContext true
                    } else {
                        throw Exception("Failed to rename temporary model file.")
                    }
                } else {
                    throw Exception("Downloaded file is too small or does not exist.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading model from $urlStr: ${e.message}", e)
                if (tempFile.exists()) tempFile.delete()
            }
        }

        _downloadProgress.value = null
        _isModelDownloaded.value = false
        false
    }

    private fun getOrInitContext(): Long {
        if (cachedContextPtr != 0L) {
            Log.i(PERF_TAG, "[Pipeline Profiler] Model Loading: 0ms (Reusing cached context ptr: $cachedContextPtr)")
            return cachedContextPtr
        }

        val startLoad = System.currentTimeMillis()
        val ptr = WhisperLib.initContext(modelFile.absolutePath)
        val loadDuration = System.currentTimeMillis() - startLoad
        Log.i(PERF_TAG, "[Pipeline Profiler] Model Loading: ${loadDuration}ms (Model file: ${modelFile.name}, ${modelFile.length()} bytes)")
        if (ptr != 0L) {
            cachedContextPtr = ptr
        }
        return ptr
    }

    fun releaseContext() {
        if (cachedContextPtr != 0L) {
            Log.d(TAG, "Releasing cached Whisper context ptr: $cachedContextPtr")
            WhisperLib.freeContext(cachedContextPtr)
            cachedContextPtr = 0L
        }
    }

    override suspend fun transcribe(
        audioFile: File,
        language: String?,
        onPartialResult: ((String) -> Unit)?
    ): Result<TranscriptResult> = withContext(Dispatchers.Default) {
        mutex.withLock {
            val totalStart = System.currentTimeMillis()
            var accumulatedText = ""
            try {
                Log.d(TAG, "transcribe requested for file: ${audioFile.absolutePath} (size: ${audioFile.length()} bytes)")

                if (!isModelValid(modelFile)) {
                    Log.d(TAG, "Model invalid/missing, downloading Q5_1 model...")
                    val downloaded = ensureModelDownloaded()
                    if (!downloaded) {
                        return@withContext Result.failure(Exception("Whisper Base Q5 model download failed. Please check network connection."))
                    }
                }

                if (!audioFile.exists() || audioFile.length() == 0L) {
                    return@withContext Result.failure(Exception("Audio file does not exist or contains no data."))
                }

                // 1. Audio Decoding & Resampling Timing
                val decodeStart = System.currentTimeMillis()
                val extension = audioFile.extension.lowercase()
                val floatArray = when (extension) {
                    "wav" -> WavToFloatConverter.convertWavToFloatArray(audioFile)
                    "m4a" -> M4aDecoder.decodeM4aToFloatArray(audioFile)
                    else -> return@withContext Result.failure(Exception("Unsupported audio format: .$extension"))
                }
                val decodeDuration = System.currentTimeMillis() - decodeStart
                Log.d(PERF_TAG, "Audio Decode & Resample Time: ${decodeDuration}ms (${floatArray.size} samples at 16kHz)")

                if (floatArray.isEmpty()) {
                    return@withContext Result.failure(Exception("Audio file contains no decodable frames."))
                }

                // 2. Model Context Retrieval
                val contextPtr = getOrInitContext()
                if (contextPtr == 0L) {
                    return@withContext Result.failure(Exception("Failed to initialize Whisper engine context."))
                }

                // 3. Optimal Thread Scheduling & Language Setting (Default: "auto" for auto-detection)
                val numThreads = calculateNumThreads()
                val targetLang = language ?: "auto"

                // 4. Native Whisper Inference & Real-Time Partial Callback
                val inferStart = System.currentTimeMillis()
                Log.d(PERF_TAG, "Starting Whisper inference with $numThreads threads, lang='$targetLang'...")
                val rawText = WhisperLib.fullTranscribe(
                    contextPtr = contextPtr,
                    audioSamples = floatArray,
                    numThreads = numThreads,
                    language = targetLang,
                    callback = { segmentText ->
                        accumulatedText += segmentText
                        onPartialResult?.invoke(accumulatedText.trim())
                    }
                )
                val inferDuration = System.currentTimeMillis() - inferStart
                Log.d(PERF_TAG, "Whisper Inference Time: ${inferDuration}ms")

                val totalDuration = System.currentTimeMillis() - totalStart
                Log.d(PERF_TAG, "=== TOTAL TRANSCRIPTION TIME: ${totalDuration}ms ===")

                if (rawText.startsWith("Error:") || rawText.startsWith("Unsupported language detected")) {
                    return@withContext Result.failure(Exception(rawText))
                }

                if (rawText.isNullOrBlank() || rawText == "Transcription failed") {
                    return@withContext Result.failure(Exception("No clear speech detected in audio."))
                }

                val cleanText = rawText.trim()
                Result.success(
                    TranscriptResult(
                        text = cleanText,
                        createdAt = System.currentTimeMillis(),
                        model = MODEL_NAME,
                        language = targetLang,
                        version = MODEL_VERSION
                    )
                )
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OutOfMemoryError during transcription", e)
                releaseContext()
                Result.failure(Exception("Insufficient memory to run transcription."))
            } catch (e: Exception) {
                Log.e(TAG, "Exception during transcription", e)
                Result.failure(e)
            }
        }
    }
}
