package dev.voicejournal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.voicejournal.audio.AudioFocusManager
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.AudioRecorderManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioFocusManager(@ApplicationContext context: Context): AudioFocusManager {
        return AudioFocusManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioRecorderManager(@ApplicationContext context: Context): AudioRecorderManager {
        return AudioRecorderManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioPlayerManager(@ApplicationContext context: Context): AudioPlayerManager {
        return AudioPlayerManager(context)
    }
}
