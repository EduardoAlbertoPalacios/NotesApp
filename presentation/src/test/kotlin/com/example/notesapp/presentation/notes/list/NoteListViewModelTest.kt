package com.example.notesapp.presentation.notes.list

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
            AppResult.Error(NoteError.Storage)
        override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)
    }
    private val now = 1_700_000_000_000L
    private val dateFormatter = NoteDateFormatter(
        now = { now },
        locale = Locale.forLanguageTag("es-ES"),
        timeZone = TimeZone.getTimeZone("UTC"),
    )

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = NoteListViewModel(
        savedStateHandle,
        GetNotesUseCase(repository, mainDispatcherRule.testDispatcher),
        dateFormatter,
    )

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
                content = AnnotatedString("Contenido"),
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
            assertEquals(NoteListEffect.NavigateToNote(id = null, category = null), awaitItem())
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

    @Test
    fun `given created when no filter selected then selected filter is all and shows every note`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(mixedNotes))

        assertEquals(NoteFilter.ALL, viewModel.state.value.selectedFilter)
        assertEquals(listOf(1L, 2L, 3L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given work filter selected when intent received then shows only work notes`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        assertEquals(NoteFilter.WORK, viewModel.state.value.selectedFilter)
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given personal filter selected when intent received then shows only personal notes`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.PERSONAL))

        assertEquals(NoteFilter.PERSONAL, viewModel.state.value.selectedFilter)
        assertEquals(listOf(2L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given category filter when switching back to all then shows every note again`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.ALL))

        assertEquals(NoteFilter.ALL, viewModel.state.value.selectedFilter)
        assertEquals(listOf(1L, 2L, 3L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given work filter and repository update when new work note arrives then list updates`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        notes.emit(AppResult.Success(mixedNotes + note(id = 4, updatedAt = now, category = NoteCategory.WORK)))

        assertEquals(listOf(4L, 1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given filter without matching notes when selected then shows empty list`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(listOf(note(id = 3))))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        assertEquals(
            NoteListUiState(
                isLoading = false,
                notes = emptyList(),
                selectedFilter = NoteFilter.WORK,
                totalCount = 1,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `given work filter when create note clicked then navigates to new note in work`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.CreateNoteClicked)
            assertEquals(NoteListEffect.NavigateToNote(id = null, category = NoteCategory.WORK), awaitItem())
        }
    }

    @Test
    fun `given personal filter when create note clicked then navigates to new note in personal`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.PERSONAL))

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.CreateNoteClicked)
            assertEquals(NoteListEffect.NavigateToNote(id = null, category = NoteCategory.PERSONAL), awaitItem())
        }
    }

    @Test
    fun `given note without color when loaded then ui item has color none`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(listOf(note(id = 1, color = NoteColor.NONE))))

        assertEquals(NoteColor.NONE, viewModel.state.value.notes.single().color)
    }

    @Test
    fun `given note with images when loaded then image path is its first image`() = runTest {
        val viewModel = createViewModel()

        notes.emit(
            AppResult.Success(
                listOf(note(id = 1, images = listOf(NoteImage(10, "/primera.jpg"), NoteImage(11, "/segunda.jpg")))),
            ),
        )

        assertEquals("/primera.jpg", viewModel.state.value.notes.single().imagePath)
    }

    @Test
    fun `given note without images when loaded then image path is null`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(listOf(note(id = 1))))

        assertEquals(null, viewModel.state.value.notes.single().imagePath)
    }

    @Test
    fun `given note gains an image when repository updates then image path appears`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(listOf(note(id = 1))))

        notes.emit(AppResult.Success(listOf(note(id = 1, images = listOf(NoteImage(10, "/nueva.jpg"))))))

        assertEquals("/nueva.jpg", viewModel.state.value.notes.single().imagePath)
    }

    @Test
    fun `given notes when query changed then updates query and filters flexibly`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))

        viewModel.onIntent(NoteListIntent.QueryChanged("reunion"))

        val state = viewModel.state.value
        assertEquals("reunion", state.query)
        assertTrue(state.isSearching)
        assertEquals(listOf(1L), state.notes.map { it.id })
    }

    @Test
    fun `given work filter when searching then results include notes from other categories`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        assertEquals(NoteFilter.WORK, viewModel.state.value.selectedFilter)
        assertEquals(listOf(2L, 3L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given several notes when searching then notes size is matches and total count is all notes`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))

        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        assertEquals(2, viewModel.state.value.notes.size)
        assertEquals(3, viewModel.state.value.totalCount)
    }

    @Test
    fun `given notes when query matches nothing then shows no results`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))

        viewModel.onIntent(NoteListIntent.QueryChanged("inexistente"))

        val state = viewModel.state.value
        assertTrue(state.notes.isEmpty())
        assertTrue(state.showNoResults)
    }

    @Test
    fun `given no notes when query in saved state then does not show no results`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("query" to "reunion")))

        notes.emit(AppResult.Success(emptyList()))

        val state = viewModel.state.value
        assertEquals(0, state.totalCount)
        assertFalse(state.showNoResults)
    }

    @Test
    fun `given active search when filter selected then clears query and shows that category`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertFalse(state.isSearching)
        assertEquals(NoteFilter.WORK, state.selectedFilter)
        assertEquals(listOf(1L), state.notes.map { it.id })
        assertEquals("", savedStateHandle.get<String>("query"))
    }

    @Test
    fun `given search over work filter when query emptied then shows work notes again`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))
        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        viewModel.onIntent(NoteListIntent.QueryChanged(""))

        val state = viewModel.state.value
        assertFalse(state.isSearching)
        assertEquals(NoteFilter.WORK, state.selectedFilter)
        assertEquals(listOf(1L), state.notes.map { it.id })
    }

    @Test
    fun `given search over personal filter when clear query clicked then clears query and keeps filter`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.PERSONAL))
        viewModel.onIntent(NoteListIntent.QueryChanged("reunion"))

        viewModel.onIntent(NoteListIntent.ClearQueryClicked)

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertEquals(NoteFilter.PERSONAL, state.selectedFilter)
        assertEquals(listOf(2L), state.notes.map { it.id })
        assertEquals("", savedStateHandle.get<String>("query"))
    }

    @Test
    fun `given query changed when intent received then writes it to saved state handle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        notes.emit(AppResult.Success(searchNotes))

        viewModel.onIntent(NoteListIntent.QueryChanged("reunion"))

        assertEquals("reunion", savedStateHandle.get<String>("query"))
    }

    @Test
    fun `given query in saved state handle when created then restores query and filtered results`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("query" to "reunion")))

        notes.emit(AppResult.Success(searchNotes))

        val state = viewModel.state.value
        assertEquals("reunion", state.query)
        assertTrue(state.isSearching)
        assertEquals(listOf(1L), state.notes.map { it.id })
    }

    @Test
    fun `given active search when repository emits edited and deleted notes then recalculates results`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        notes.emit(
            AppResult.Success(
                listOf(
                    note(id = 1, title = "Lista de la reunión", updatedAt = now, category = NoteCategory.WORK),
                    searchNotes[1],
                ),
            ),
        )

        val state = viewModel.state.value
        assertEquals(listOf(1L, 2L), state.notes.map { it.id })
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `given active search when last note deleted then clears query and disables search`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.QueryChanged("reunion"))

        notes.emit(AppResult.Success(emptyList()))

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertEquals(0, state.totalCount)
        assertFalse(state.isSearchEnabled)
        assertEquals("", savedStateHandle.get<String>("query"))
    }

    @Test
    fun `given search over work filter when create note clicked then navigates to new note in work`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))
        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.CreateNoteClicked)
            assertEquals(NoteListEffect.NavigateToNote(id = null, category = NoteCategory.WORK), awaitItem())
        }
    }

    @Test
    fun `given note with bold and underline spans when loaded then content keeps text and has their styles`() = runTest {
        val viewModel = createViewModel()
        val spans = listOf(FormatSpan(0, 2, TextFormat.BOLD), FormatSpan(8, 14, TextFormat.UNDERLINE))

        notes.emit(AppResult.Success(listOf(note(id = 1, content = "Un café rápido", contentSpans = spans))))

        val content = viewModel.state.value.notes.single().content
        assertEquals("Un café rápido", content.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 8, 14),
            ),
            content.spanStyles,
        )
    }

    @Test
    fun `given note without spans when loaded then content has no styles`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(listOf(note(id = 1, content = "Texto plano"))))

        val content = viewModel.state.value.notes.single().content
        assertEquals("Texto plano", content.text)
        assertTrue(content.spanStyles.isEmpty())
    }

    @Test
    fun `given note with formatted content when searching a word across its spans then finds it`() = runTest {
        val viewModel = createViewModel()
        val formatted = note(
            id = 4,
            title = "Sin coincidencia",
            content = "Comprar pan integral",
            updatedAt = now - 4 * DAY,
            contentSpans = listOf(FormatSpan(0, 10, TextFormat.BOLD), FormatSpan(8, 20, TextFormat.UNDERLINE)),
        )
        notes.emit(AppResult.Success(searchNotes + formatted))

        viewModel.onIntent(NoteListIntent.QueryChanged("pan integral"))

        val match = viewModel.state.value.notes.single()
        assertEquals(4L, match.id)
        assertEquals("Comprar pan integral", match.content.text)
        assertFalse(match.content.spanStyles.isEmpty())
    }

    private val searchNotes
        get() = listOf(
            note(id = 1, title = "Reunión de equipo", updatedAt = now - 1 * DAY, category = NoteCategory.WORK),
            note(id = 2, title = "Lista de compras", updatedAt = now - 2 * DAY, category = NoteCategory.PERSONAL),
            note(id = 3, title = "Ideas", content = "Lista de libros", updatedAt = now - 3 * DAY, category = null),
        )

    private val mixedNotes
        get() = listOf(
            note(id = 1, updatedAt = now - 1 * DAY, category = NoteCategory.WORK),
            note(id = 2, updatedAt = now - 2 * DAY, category = NoteCategory.PERSONAL),
            note(id = 3, updatedAt = now - 3 * DAY, category = null),
        )

    private fun note(
        id: Long,
        title: String = "Nota $id",
        content: String = "Contenido",
        isPinned: Boolean = false,
        updatedAt: Long = now,
        category: NoteCategory? = null,
        images: List<NoteImage> = emptyList(),
        contentSpans: List<FormatSpan> = emptyList(),
        color: NoteColor = NoteColor.MINT,
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = color,
        isPinned = isPinned,
        category = category,
        updatedAt = updatedAt,
        images = images,
        contentSpans = contentSpans,
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
