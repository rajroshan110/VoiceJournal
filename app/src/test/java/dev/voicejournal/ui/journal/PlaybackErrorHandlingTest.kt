package dev.voicejournal.ui.journal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.AudioRecorderManager
import dev.voicejournal.audio.PlayerState
import dev.voicejournal.data.backup.ImportManager
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.AudioTrack
import dev.voicejournal.domain.model.JournalEntry
import dev.voicejournal.domain.repository.JournalRepository
import dev.voicejournal.domain.usecase.*
import dev.voicejournal.transcription.engine.SpeechToTextEngine
import dev.voicejournal.ui.notedetail.NoteDetailViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackErrorHandlingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var userPreferencesManager: UserPreferencesManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true) {
            every { filesDir } returns File("/tmp/mock_files_dir")
            every { cacheDir } returns File("/tmp/mock_cache_dir")
        }
        audioPlayerManager = mockk(relaxed = true) {
            every { playbackState } returns MutableStateFlow<PlayerState>(PlayerState.Idle)
        }
        userPreferencesManager = mockk(relaxed = true) {
            every { isFolderEnabledState } returns MutableStateFlow(true)
            every { isTopicsEnabledState } returns MutableStateFlow(true)
            every { isPeopleEnabledState } returns MutableStateFlow(true)
            every { isMoodEnabledState } returns MutableStateFlow(true)
            every { isFolderEnabled } returns flowOf(true)
            every { isTopicsEnabled } returns flowOf(true)
            every { isPeopleEnabled } returns flowOf(true)
            every { isMoodEnabled } returns flowOf(true)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testJournalViewModelClearPlaybackError() {
        val viewModel = JournalViewModel(
            context = mockContext,
            savedStateHandle = SavedStateHandle(),
            getAllEntriesUseCase = mockk(relaxed = true),
            getAllTagsUseCase = mockk(relaxed = true),
            deleteEntryUseCase = mockk(relaxed = true),
            journalRepository = mockk(relaxed = true),
            audioPlayerManager = audioPlayerManager,
            userPreferencesManager = userPreferencesManager,
            importManager = mockk(relaxed = true)
        )

        // Attempt playback on a non-existent audio track
        val entry = JournalEntry(id = 100L, audioPath = "")
        val nonExistentTrack = AudioTrack(id = "track_1", path = "/non/existent/path/record.wav")
        viewModel.playTrack(entry, nonExistentTrack)

        val errorState = viewModel.cardPlaybackState.value
        assertEquals(PlaybackStatus.Error, errorState.status)
        assertEquals("Audio track file unavailable", errorState.errorMessage)

        // Clear playback error (simulating footer tab swipe or dialog dismissal)
        viewModel.clearPlaybackError()

        val clearedState = viewModel.cardPlaybackState.value
        assertEquals(PlaybackStatus.Idle, clearedState.status)
        assertNull(clearedState.errorMessage)
    }

    @Test
    fun testNoteDetailViewModelReportsErrorWhenTrackUnavailable() {
        val noteDetailViewModel = NoteDetailViewModel(
            context = mockContext,
            getEntryByIdUseCase = mockk(relaxed = true),
            saveEntryUseCase = mockk(relaxed = true),
            deleteEntryUseCase = mockk(relaxed = true),
            getAllTagsUseCase = mockk(relaxed = true),
            audioRecorderManager = mockk(relaxed = true),
            audioPlayerManager = audioPlayerManager,
            userPreferencesManager = userPreferencesManager,
            speechToTextEngine = mockk(relaxed = true),
            generateTranscriptUseCase = mockk(relaxed = true)
        )

        val nonExistentTrack = AudioTrack(id = "missing_track", path = "/non/existent/track.m4a")
        noteDetailViewModel.togglePlaybackForTrack(nonExistentTrack)

        val uiState = noteDetailViewModel.uiState.value
        assertEquals("Audio track file unavailable", uiState.errorMessage)
        assertFalse(uiState.isPlaying)

        // Clear error message (simulating dialog dismissal)
        noteDetailViewModel.clearErrorMessage()
        assertNull(noteDetailViewModel.uiState.value.errorMessage)
    }
}
