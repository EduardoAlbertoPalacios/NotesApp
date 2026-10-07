package com.example.notesapp.presentation.notes.list

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NoteListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notes = MutableSharedFlow<AppResult<List<Note>, NoteError>>(replay = 1)
    private val repository = object : NoteRepository {
        override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = notes
        override suspend fun getNote(id: Long): AppResult<Note, NoteError> = AppResult.Error(NoteError.NotFound)
        override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = AppResult.Error(NoteError.Storage)
        override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)
    }
    private val now = 1_700_000_000_000L
    private val dateFormatter = NoteDateFormatter(
        now = { now },
        locale = Locale.forLanguageTag("es-ES"),
        timeZone = TimeZone.getTimeZone("UTC"),
    )

    private fun createViewModel() = NoteListViewModel(GetNotesUseCase(repository), dateFormatter)

    @Test
    fun `given no emission yet when created then state is loading`() {
        val viewModel = createViewModel()

        assertEquals(NoteListUiState(isLoading = true), viewModel.state.value)
    }

    @Test
    fun `given notes when loaded then exposes them ordered and mapped to ui`() = runTest {
        val viewModel = createViewModel()
        val older = note(id = 1, updatedAt = now - 2 * DAY)
        val pinned = note(id = 2, isPinned = true, updatedAt = now - 3 * DAY)

        notes.emit(AppResult.Success(listOf(older, pinned)))

        val state = viewModel.state.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.hasError)
        assertEquals(listOf(2L, 1L), state.notes.map { it.id })
        assertEquals(
            NoteItemUi(
                id = 2,
                title = "Nota 2",
                content = "Contenido",
                color = NoteColor.MINT,
                isPinned = true,
                date = dateFormatter.format(pinned.updatedAt),
            ),
            state.notes.first(),
        )
    }

    @Test
    fun `given empty repository when loaded then shows empty state`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(emptyList()))

        assertEquals(NoteListUiState(isLoading = false, notes = emptyList()), viewModel.state.value)
    }

    @Test
    fun `given storage error when loading then shows error state`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Error(NoteError.Storage))

        assertEquals(NoteListUiState(isLoading = false, hasError = true), viewModel.state.value)
    }

    @Test
    fun `given note clicked when intent received then navigates to that note`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.NoteClicked(id = 5))
            assertEquals(NoteListEffect.NavigateToNote(5), awaitItem())
        }
    }

    @Test
    fun `given create note clicked when intent received then navigates to a new note`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.CreateNoteClicked)
            assertEquals(NoteListEffect.NavigateToNote(null), awaitItem())
        }
    }

    @Test
    fun `given settings clicked when intent received then navigates to settings`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.SettingsClicked)
            assertEquals(NoteListEffect.NavigateToSettings, awaitItem())
        }
    }

    private fun note(id: Long, isPinned: Boolean = false, updatedAt: Long) = Note(
        id = id,
        title = "Nota $id",
        content = "Contenido",
        color = NoteColor.MINT,
        isPinned = isPinned,
        updatedAt = updatedAt,
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
