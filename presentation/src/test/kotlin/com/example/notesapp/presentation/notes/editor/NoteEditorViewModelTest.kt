package com.example.notesapp.presentation.notes.editor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.SaveNoteUseCase
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = InMemoryNoteRepository()
    private val dateFormatter = NoteDateFormatter(
        now = { NOW },
        locale = Locale.forLanguageTag("es-ES"),
        timeZone = TimeZone.getTimeZone("UTC"),
    )
    private val existing = Note(
        id = 7,
        title = "Reunión de equipo",
        content = "Menos ruido, más foco.",
        color = NoteColor.ROSE,
        isPinned = false,
        updatedAt = NOW - 1_000,
    )

    private fun createViewModel(noteId: Long? = null) = NoteEditorViewModel(
        savedStateHandle = SavedStateHandle(noteId?.let { mapOf(NoteEditorViewModel.NOTE_ID_ARG to it) }.orEmpty()),
        getNote = GetNoteUseCase(repository),
        saveNote = SaveNoteUseCase(repository, TimeProvider { NOW }),
        deleteNote = DeleteNoteUseCase(repository),
        dateFormatter = dateFormatter,
    )

    @Test
    fun `given new note when created then shows empty editor`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(NoteEditorUiState(), viewModel.state.value)
    }

    @Test
    fun `given existing note id when created then loads its fields`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)

        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(existing.title, state.title)
        assertEquals(existing.content, state.content)
        assertEquals(dateFormatter.format(existing.updatedAt), state.date)
    }

    @Test
    fun `given missing note id when created then shows not found`() = runTest {
        val viewModel = createViewModel(noteId = 99)
        advanceUntilIdle()

        assertEquals(LoadError.NotFound, viewModel.state.value.loadError)
    }

    @Test
    fun `given typing when autosave delay has not elapsed then nothing is saved`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.TitleChanged("Ide"))
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
        runCurrent()

        assertTrue(repository.notes.isEmpty())
        assertEquals("Ide", viewModel.state.value.title)
    }

    @Test
    fun `given typing stops when autosave delay elapses then creates the note once`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.TitleChanged("Ide"))
        advanceTimeBy(200)
        viewModel.onIntent(NoteEditorIntent.TitleChanged("Ideas"))
        viewModel.onIntent(NoteEditorIntent.ContentChanged("Libreta"))
        advanceUntilIdle()

        val saved = repository.notes.values.single()
        assertEquals("Ideas", saved.title)
        assertEquals("Libreta", saved.content)
        assertEquals(NOW, saved.updatedAt)
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)

        viewModel.onIntent(NoteEditorIntent.ContentChanged("Libreta y café"))
        advanceUntilIdle()

        assertEquals("Libreta y café", repository.notes.values.single().content)
    }

    @Test
    fun `given existing note when edited then updates it keeping id and color`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ContentChanged("Nuevo texto"))
        advanceUntilIdle()

        assertEquals(existing.copy(content = "Nuevo texto", updatedAt = NOW), repository.notes[existing.id])
    }

    @Test
    fun `given blank new note when back clicked then navigates back without creating it`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged("   "))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given pending changes when back clicked then saves before navigating back`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged("Para leer"))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertEquals("Para leer", repository.notes.values.single().title)
    }

    @Test
    fun `given save failure when back clicked then shows message and stays`() = runTest {
        val viewModel = createViewModel()
        repository.failure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged("Para leer"))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_save_error), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given existing note when pin clicked then saves it pinned immediately`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        runCurrent()

        assertTrue(viewModel.state.value.isPinned)
        assertEquals(true, repository.notes[existing.id]?.isPinned)
    }

    @Test
    fun `given delete clicked and dismissed when intents received then toggles the dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.DeleteClicked)
        assertTrue(viewModel.state.value.isDeleteDialogVisible)

        viewModel.onIntent(NoteEditorIntent.DeleteDismissed)
        assertEquals(false, viewModel.state.value.isDeleteDialogVisible)
    }

    @Test
    fun `given existing note when delete confirmed then deletes it and navigates back`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.DeleteClicked)
            viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertTrue(repository.notes.isEmpty())
        assertEquals(false, viewModel.state.value.isDeleteDialogVisible)
    }

    @Test
    fun `given unsaved new note when delete confirmed then discards it and navigates back`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged("Borrador"))
            viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        advanceUntilIdle()
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given delete failure when delete confirmed then shows message`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        repository.failure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_delete_error), awaitItem())
        }
    }

    private class InMemoryNoteRepository : NoteRepository {
        val notes = linkedMapOf<Long, Note>()
        var failure: NoteError? = null
        private var nextId = 1L

        override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = emptyFlow()

        override suspend fun getNote(id: Long): AppResult<Note, NoteError> =
            notes[id]?.let { AppResult.Success(it) } ?: AppResult.Error(NoteError.NotFound)

        override suspend fun saveNote(note: Note): AppResult<Long, NoteError> {
            failure?.let { return AppResult.Error(it) }
            val id = if (note.id == Note.NEW_ID) nextId++ else note.id
            notes[id] = note.copy(id = id)
            return AppResult.Success(id)
        }

        override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> {
            failure?.let { return AppResult.Error(it) }
            notes.remove(id)
            return AppResult.Success(Unit)
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
