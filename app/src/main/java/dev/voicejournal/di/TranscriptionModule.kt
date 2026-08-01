package dev.voicejournal.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.voicejournal.transcription.engine.SpeechToTextEngine
import dev.voicejournal.transcription.engine.WhisperEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranscriptionModule {

    @Binds
    @Singleton
    abstract fun bindSpeechToTextEngine(
        whisperEngine: WhisperEngine
    ): SpeechToTextEngine
}
