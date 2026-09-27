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

    @Test
    fun testSwitchingAudioTracksImmediatelyResetsPlaybackPosition() {
        val tempFile1 = File.createTempFile("track1_", ".m4a").apply { writeBytes(ByteArray(100)) }
        val tempFile2 = File.createTempFile("track2_", ".m4a").apply { writeBytes(ByteArray(100)) }

        try {
            val playbackFlow = MutableStateFlow<PlayerState>(PlayerState.Idle)
            val mockPlayerManager = mockk<AudioPlayerManager>(relaxed = true) {
                every { playbackState } returns playbackFlow
            }

            val track1 = AudioTrack(id = "track_1", path = tempFile1.absolutePath, durationMs = 10000L)
            val track2 = AudioTrack(id = "track_2", path = tempFile2.absolutePath, durationMs = 15000L)

            val mockGetEntryByIdUseCase = mockk<GetEntryByIdUseCase>()
            every { mockGetEntryByIdUseCase(1L) } returns flowOf(
                JournalEntry(
                    id = 1L,
                    title = "Test Note",
                    userText = "",
                    audioTracks = listOf(track1, track2)
                )
            )

            val noteDetailViewModel = NoteDetailViewModel(
                context = mockContext,
                getEntryByIdUseCase = mockGetEntryByIdUseCase,
                saveEntryUseCase = mockk(relaxed = true),
                deleteEntryUseCase = mockk(relaxed = true),
                getAllTagsUseCase = mockk(relaxed = true),
                audioRecorderManager = mockk(relaxed = true),
                audioPlayerManager = mockPlayerManager,
                userPreferencesManager = userPreferencesManager,
                speechToTextEngine = mockk(relaxed = true),
                generateTranscriptUseCase = mockk(relaxed = true)
            )

            noteDetailViewModel.loadEntry(1L)
            testDispatcher.scheduler.advanceUntilIdle()

            // Start track 1 and simulate it being played/paused at 5000ms
            noteDetailViewModel.togglePlaybackForTrack(track1)
            playbackFlow.value = PlayerState.Paused(1L, 5000L, 10000L, tempFile1.absolutePath)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("track_1", noteDetailViewModel.uiState.value.playingTrackId)
            assertEquals(5000L, noteDetailViewModel.uiState.value.currentPositionMs)

            // Now switch to track 2
            noteDetailViewModel.togglePlaybackForTrack(track2)

            // Verify track 2 immediately starts at 0ms with zero residual "shadow" from track 1
            val stateAfterSwitch = noteDetailViewModel.uiState.value
            assertEquals("track_2", stateAfterSwitch.playingTrackId)
            assertEquals(0L, stateAfterSwitch.currentPositionMs)
            assertTrue(stateAfterSwitch.isPlaying)
        } finally {
            tempFile1.delete()
            tempFile2.delete()
        }
    }
}
