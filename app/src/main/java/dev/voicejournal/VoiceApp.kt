package dev.voicejournal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import dev.voicejournal.audio.AudioFileRepair

@HiltAndroidApp
class VoiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AudioFileRepair.repairIfNeeded(this)
    }
}
