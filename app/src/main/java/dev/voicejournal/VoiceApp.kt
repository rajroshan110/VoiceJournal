package dev.voicejournal

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import dev.voicejournal.domain.repository.JournalRepository
import dev.voicejournal.transcription.engine.SpeechToTextEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var journalRepository: JournalRepository

    @Inject
    lateinit var speechToTextEngine: SpeechToTextEngine

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                journalRepository.refreshAndHealData()
            } catch (_: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_RUNNING_LOW ||
            level == TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= TRIM_MEMORY_UI_HIDDEN
        ) {
            if (::speechToTextEngine.isInitialized) {
                speechToTextEngine.releaseContext()
            }
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
