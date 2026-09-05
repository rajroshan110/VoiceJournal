package dev.voicejournal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import dev.voicejournal.audio.AudioFileRepair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class VoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            AudioFileRepair.repairIfNeeded(this@VoiceApp)
        }
    }
}
