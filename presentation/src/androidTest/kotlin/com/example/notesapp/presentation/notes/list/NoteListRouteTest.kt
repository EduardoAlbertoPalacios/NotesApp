package com.example.notesapp.presentation.notes.list

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetNotesViewUseCase
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.SettingsRepository
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import com.example.notesapp.presentation.theme.NotesAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteListRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settingsRepository = FailingSaveSettingsRepository()

    @Volatile
    private var openedNotes: List<Long?> = emptyList()

    @Volatile
    private var navigateToSettingsCalls = 0

    private fun setFailingSaveContent() {
        val notesRepository = SingleNoteRepository(note(id = NOTE_ID))
        // En el hilo principal, como pide viewModelScope.
        val viewModel = composeRule.runOnUiThread {
            NoteListViewModel(
                SavedStateHandle(),
                GetNotesUseCase(notesRepository, Dispatchers.Unconfined),
                NoteDateFormatter(),
                GetNotesViewUseCase(settingsRepository),
                SetNotesViewUseCase(settingsRepository),
            )
        }
        composeRule.setContent {
            NotesAppTheme {
                NoteListRoute(
                    onNavigateToNote = { id, _ -> openedNotes = openedNotes + id },
                    onNavigateToSettings = { navigateToSettingsCalls++ },
                    viewModel = viewModel,
                )
            }
        }
        // Hasta terminar la carga no hay vista vigente con la que comparar la elegida.
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(NoteListTestTags.NOTE_CARD + NOTE_ID).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun chooseListView() {
        viewOption(NotesView.LIST).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(string(R.string.notes_view_save_error)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun given_failing_save_when_list_option_clicked_then_shows_error_and_keeps_chosen_view() {
        setFailingSaveContent()
        viewOption(NotesView.GRID).assertIsSelected()

        chooseListView()

        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertIsDisplayed()
        viewOption(NotesView.LIST).assertIsSelected()
        viewOption(NotesView.GRID).assertIsNotSelected()
        assertEquals(listOf(NotesView.LIST), settingsRepository.savedViews)
    }

    @Test
    fun given_visible_save_error_when_note_clicked_then_opens_it_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        chooseListView()
        // Con el reloj parado el aviso no puede cerrarse: si el efecto lo esperara, nunca se navegaría.
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + NOTE_ID).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { openedNotes.isNotEmpty() }

        assertEquals(listOf<Long?>(NOTE_ID), openedNotes)
        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertIsDisplayed()
    }

    @Test
    fun given_visible_save_error_when_settings_clicked_then_navigates_without_waiting_for_error_to_close() {
        setFailingSaveContent()
        chooseListView()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithTag(NoteListTestTags.SETTINGS).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { navigateToSettingsCalls > 0 }

        assertEquals(1, navigateToSettingsCalls)
        composeRule.onNodeWithText(string(R.string.notes_view_save_error)).assertIsDisplayed()
    }

    private fun viewOption(view: NotesView): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(NoteListTestTags.VIEW_OPTION + view.name)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun note(id: Long) = Note(
        id = id,
        title = "Nota $id",
        content = "Contenido",
        color = NoteColor.MINT,
        isPinned = false,
        category = null,
        updatedAt = UPDATED_AT,
    )

    private companion object {
        const val NOTE_ID = 1L
        const val UPDATED_AT = 1_700_000_000_000L
        const val TIMEOUT_MS = 5_000L
    }
}

private class SingleNoteRepository(private val note: Note) : NoteRepository {

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = flowOf(AppResult.Success(listOf(note)))

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> = AppResult.Success(note)

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)
}

/** Como el repositorio real, un fallo al guardar mantiene la vista elegida durante la sesión. */
private class FailingSaveSettingsRepository : SettingsRepository {

    private val notesView = MutableStateFlow<NotesView?>(null)

    @Volatile
    var savedViews: List<NotesView> = emptyList()
        private set

    override fun observeThemeMode(): Flow<ThemeMode?> = flowOf(null)

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> =
        AppResult.Error(SettingsError.Storage)

    override fun observeDefaultNoteColor(): Flow<NoteColor?> = flowOf(null)

    override suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError> =
        AppResult.Error(SettingsError.Storage)

    override fun observeNotesView(): Flow<NotesView?> = notesView

    override suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError> {
        savedViews = savedViews + view
        notesView.value = view
        return AppResult.Error(SettingsError.Storage)
    }
}
