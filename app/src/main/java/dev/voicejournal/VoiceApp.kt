package dev.voicejournal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import dev.voicejournal.audio.AudioFileRepair
import dev.voicejournal.domain.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceApp : Application() {

    @Inject
    lateinit var journalRepository: JournalRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AudioFileRepair.repairIfNeeded(this@VoiceApp)
            } catch (_: Exception) {}
            try {
                journalRepository.refreshAndHealData()
            } catch (_: Exception) {}
        }
    }
}
