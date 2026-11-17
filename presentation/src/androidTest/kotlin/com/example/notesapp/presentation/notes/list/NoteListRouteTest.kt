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
import com.example.notesapp.domain.notes.trash.RestoreFromTrashUseCase
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.domain.notes.trash.TrashedNote
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

    @Volatile
    private var movedToTrashShownCalls = 0

    private val notesRepository = SingleNoteRepository(note(id = NOTE_ID))

    private fun setFailingSaveContent(movedToTrashNoteId: Long? = null) {
        // En el hilo principal, como pide viewModelScope.
        val viewModel = composeRule.runOnUiThread {
            NoteListViewModel(
                SavedStateHandle(),
                GetNotesUseCase(notesRepository, Dispatchers.Unconfined),
                NoteDateFormatter(),
                GetNotesViewUseCase(settingsRepository),
                SetNotesViewUseCase(settingsRepository),
                RestoreFromTrashUseCase(notesRepository),
            )
        }
        composeRule.setContent {
            NotesAppTheme {
                NoteListRoute(
                    onNavigateToNote = { id, _ -> openedNotes = openedNotes + id },
                    onNavigateToSettings = { navigateToSettingsCalls++ },
                    movedToTrashNoteId = movedToTrashNoteId,
                    onMovedToTrashShown = { movedToTrashShownCalls++ },
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

    @Test
    fun given_note_moved_to_trash_when_displayed_then_shows_notice_with_undo_and_reports_it_shown() {
        setFailingSaveContent(movedToTrashNoteId = TRASHED_NOTE_ID)

        waitForText(string(R.string.note_list_moved_to_trash))

        composeRule.onNodeWithText(string(R.string.note_list_moved_to_trash)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_list_undo)).assertIsDisplayed()
        assertEquals(1, movedToTrashShownCalls)
    }

    @Test
    fun given_moved_to_trash_notice_when_undo_clicked_then_restores_that_note() {
        setFailingSaveContent(movedToTrashNoteId = TRASHED_NOTE_ID)
        waitForText(string(R.string.note_list_undo))

        composeRule.onNodeWithText(string(R.string.note_list_undo)).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) { notesRepository.restoredIds.isNotEmpty() }

        assertEquals(listOf(TRASHED_NOTE_ID), notesRepository.restoredIds)
    }

    @Test
    fun given_failing_restore_when_undo_clicked_then_shows_restore_error() {
        notesRepository.restoreSucceeds = false
        setFailingSaveContent(movedToTrashNoteId = TRASHED_NOTE_ID)
        waitForText(string(R.string.note_list_undo))

        composeRule.onNodeWithText(string(R.string.note_list_undo)).performClick()
        waitForText(string(R.string.note_list_restore_error))

        composeRule.onNodeWithText(string(R.string.note_list_restore_error)).assertIsDisplayed()
    }

    @Test
    fun given_no_note_moved_to_trash_when_displayed_then_shows_no_notice() {
        setFailingSaveContent()

        composeRule.onNodeWithText(string(R.string.note_list_moved_to_trash)).assertDoesNotExist()
        assertEquals(0, movedToTrashShownCalls)
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
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
        const val TRASHED_NOTE_ID = 7L
        const val UPDATED_AT = 1_700_000_000_000L
        const val TIMEOUT_MS = 5_000L
    }
}

private class SingleNoteRepository(private val note: Note) : NoteRepository {

    @Volatile
    var restoreSucceeds = true

    @Volatile
    var restoredIds: List<Long> = emptyList()
        private set

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = flowOf(AppResult.Success(listOf(note)))

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> = AppResult.Success(note)

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun moveToTrash(id: Long, trashedAt: Long): AppResult<Unit, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun restoreFromTrash(id: Long): AppResult<Unit, NoteError> {
        restoredIds = restoredIds + id
        return if (restoreSucceeds) AppResult.Success(Unit) else AppResult.Error(NoteError.Storage)
    }

    override fun observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>> = flowOf(AppResult.Success(emptyList()))

    override suspend fun deleteNotes(ids: List<Long>): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)
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

    override fun observeAutoSaveEnabled(): Flow<Boolean?> = flowOf(null)

    override suspend fun setAutoSaveEnabled(enabled: Boolean): AppResult<Unit, SettingsError> =
        AppResult.Error(SettingsError.Storage)

    override fun observeTrashRetention(): Flow<TrashRetention?> = flowOf(null)

    override suspend fun setTrashRetention(retention: TrashRetention): AppResult<Unit, SettingsError> =
        AppResult.Error(SettingsError.Storage)
}
