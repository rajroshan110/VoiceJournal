package dev.voicejournal.ui.journal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dev.voicejournal.audio.AudioPlayerManager
import dev.voicejournal.audio.PlayerState
import dev.voicejournal.data.backup.ImportManager
import dev.voicejournal.data.local.datastore.UserPreferencesManager
import dev.voicejournal.domain.model.TimeFormat
import dev.voicejournal.domain.repository.JournalRepository
import dev.voicejournal.domain.usecase.DeleteEntryUseCase
import dev.voicejournal.domain.usecase.ExtractUnifiedTagsUseCase
import dev.voicejournal.domain.usecase.GetAllEntriesUseCase
import dev.voicejournal.domain.usecase.GetAllTagsUseCase
import dev.voicejournal.ui.journal.components.SortOption
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var getAllEntriesUseCase: GetAllEntriesUseCase
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase
    private lateinit var deleteEntryUseCase: DeleteEntryUseCase
    private lateinit var extractUnifiedTagsUseCase: ExtractUnifiedTagsUseCase
    private lateinit var journalRepository: JournalRepository
    private lateinit var importManager: ImportManager
    private lateinit var viewModel: JournalViewModel

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
            every { timeFormat } returns flowOf(TimeFormat.SYSTEM_DEFAULT)
            every { isFolderEnabledState } returns MutableStateFlow(true)
            every { isNotesOrganisationEnabledState } returns MutableStateFlow(true)
            every { isTopicsEnabledState } returns MutableStateFlow(true)
            every { isPeopleEnabledState } returns MutableStateFlow(true)
            every { isMoodEnabledState } returns MutableStateFlow(true)
            every { isFolderEnabled } returns flowOf(true)
            every { isNotesOrganisationEnabled } returns flowOf(true)
            every { isTopicsEnabled } returns flowOf(true)
            every { isPeopleEnabled } returns flowOf(true)
            every { isMoodEnabled } returns flowOf(true)
            every { sortOption } returns flowOf(SortOption.MODIFIED_DESC)
        }
        getAllEntriesUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke() } returns flowOf(emptyList())
        }
        getAllTagsUseCase = mockk(relaxed = true) {
            every { this@mockk.invoke() } returns flowOf(emptyList())
        }
        deleteEntryUseCase = mockk(relaxed = true)
        extractUnifiedTagsUseCase = ExtractUnifiedTagsUseCase()
        journalRepository = mockk(relaxed = true)
        importManager = mockk(relaxed = true)

        viewModel = JournalViewModel(
            context = mockContext,
            savedStateHandle = SavedStateHandle(),
            getAllEntriesUseCase = getAllEntriesUseCase,
            getAllTagsUseCase = getAllTagsUseCase,
            deleteEntryUseCase = deleteEntryUseCase,
            extractUnifiedTagsUseCase = extractUnifiedTagsUseCase,
            journalRepository = journalRepository,
            audioPlayerManager = audioPlayerManager,
            userPreferencesManager = userPreferencesManager,
            importManager = importManager,
            defaultDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testToggleTagFilterCaseInsensitive() {
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle "#Songs" (adds "Songs")
        viewModel.toggleTagFilter("#Songs")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(setOf("Songs"), viewModel.uiState.value.filterState.selectedTags)

        // Toggle "songs" (case-insensitive match, removes "Songs")
        viewModel.toggleTagFilter("songs")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filterState.selectedTags.isEmpty())

        // Toggle "ideas" (adds "ideas")
        viewModel.toggleTagFilter("ideas")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(setOf("ideas"), viewModel.uiState.value.filterState.selectedTags)

        // Toggle "#IDEAS" (case-insensitive match, removes "ideas")
        viewModel.toggleTagFilter("#IDEAS")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filterState.selectedTags.isEmpty())
    }

    @Test
    fun testTogglePersonFilterCaseInsensitive() {
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle "@Bob" (adds "Bob")
        viewModel.togglePersonFilter("@Bob")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(setOf("Bob"), viewModel.uiState.value.filterState.selectedPeople)

        // Toggle "bob" (case-insensitive match, removes "Bob")
        viewModel.togglePersonFilter("bob")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filterState.selectedPeople.isEmpty())
    }

    @Test
    fun testClearAllFiltersResetsSearchAndFilters() {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleTagFilter("music")
        viewModel.togglePersonFilter("alice")
        viewModel.updateSearchQuery("test query")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearAllFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.filterState.selectedTags.isEmpty())
        assertTrue(viewModel.uiState.value.filterState.selectedPeople.isEmpty())
        assertEquals("", viewModel.uiState.value.searchQuery)
    }
}
