package com.example.notesapp.presentation.notes.list

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetNotesViewUseCase
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.notes.common.CARD_TASK_DONE
import com.example.notesapp.presentation.notes.common.CARD_TASK_PENDING
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import com.example.notesapp.presentation.notes.common.TASK_BREAK
import com.example.notesapp.presentation.settings.FakeSettingsRepository
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
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

    private val settings = FakeSettingsRepository()

    /** Ya hay una vista leída: sin ella la carga no termina, y estas pruebas tratan de las notas. */
    @Before
    fun storeNotesView() = runTest {
        settings.emitStoredNotesView(NotesView.GRID)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        settingsRepository: FakeSettingsRepository = settings,
    ) = NoteListViewModel(
        savedStateHandle,
        GetNotesUseCase(repository, mainDispatcherRule.testDispatcher),
        dateFormatter,
        GetNotesViewUseCase(settingsRepository),
        SetNotesViewUseCase(settingsRepository),
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

        assertEquals(NoteListUiState(isLoading = false, notes = emptyList(), notesView = NotesView.GRID), viewModel.state.value)
    }

    @Test
    fun `given storage error when loading then shows error state`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Error(NoteError.Storage))

        assertEquals(NoteListUiState(isLoading = false, hasError = true, notesView = NotesView.GRID), viewModel.state.value)
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
                notesView = NotesView.GRID,
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

    @Test
    fun `given note with tasks when loaded then content has a checkbox at each task and done is struck through`() = runTest {
        val viewModel = createViewModel()
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

        notes.emit(AppResult.Success(listOf(note(id = 1, content = "Tomates\nPan\nNota", contentTasks = tasks))))

        val content = viewModel.state.value.notes.single().content
        assertEquals("Tomates\nPan\nNota", content.text.replace("\u200B", ""))
        assertEquals(
            listOf(CARD_TASK_PENDING to 0, CARD_TASK_DONE to 9),
            content.getStringAnnotations(0, content.length).map { it.item to it.start },
        )
        assertEquals(
            listOf(AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 13)),
            content.spanStyles,
        )
    }

    @Test
    fun `given notes but view not read yet when loaded then keeps loading without showing them`() = runTest {
        val pendingSettings = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepository = pendingSettings)

        notes.emit(AppResult.Success(mixedNotes))

        assertEquals(NoteListUiState(isLoading = true), viewModel.state.value)
    }

    @Test
    fun `given view read but notes pending when loaded then keeps loading with view still null`() = runTest {
        val viewModel = createViewModel()

        assertEquals(NoteListUiState(isLoading = true), viewModel.state.value)
        assertNull(viewModel.state.value.notesView)
    }

    @Test
    fun `given stored list view when notes arrive then first loaded state already has list view`() = runTest {
        val pendingSettings = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepository = pendingSettings)

        viewModel.state.test {
            assertEquals(NoteListUiState(isLoading = true), awaitItem())

            pendingSettings.emitStoredNotesView(NotesView.LIST)
            expectNoEvents()
            notes.emit(AppResult.Success(mixedNotes))

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(NotesView.LIST, loaded.notesView)
            assertEquals(listOf(1L, 2L, 3L), loaded.notes.map { it.id })
            expectNoEvents()
        }
    }

    @Test
    fun `given storage error but view not read yet when loading then keeps loading until view arrives`() = runTest {
        val pendingSettings = FakeSettingsRepository()
        val viewModel = createViewModel(settingsRepository = pendingSettings)

        notes.emit(AppResult.Error(NoteError.Storage))
        assertEquals(NoteListUiState(isLoading = true), viewModel.state.value)

        pendingSettings.emitStoredNotesView(NotesView.LIST)

        assertEquals(
            NoteListUiState(isLoading = false, hasError = true, notesView = NotesView.LIST),
            viewModel.state.value,
        )
    }

    @Test
    fun `given repository without view when loaded then uses grid`() = runTest {
        val emptySettings = FakeSettingsRepository()
        emptySettings.emitStoredNotesView(null)
        val viewModel = createViewModel(settingsRepository = emptySettings)

        notes.emit(AppResult.Success(mixedNotes))

        assertEquals(NotesView.GRID, viewModel.state.value.notesView)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `given stored view when it changes elsewhere then state follows it`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        settings.emitStoredNotesView(NotesView.LIST)

        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
    }

    @Test
    fun `given other view when selected then saves it and state shows it without effects`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.LIST))

            expectNoEvents()
        }

        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
        assertEquals(listOf(NotesView.LIST), settings.savedNotesViews)
    }

    @Test
    fun `given active view when selected again then does not save nor change anything`() = runTest {
        settings.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))
        val before = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.GRID))

            expectNoEvents()
        }

        assertEquals(before, viewModel.state.value)
        assertTrue(settings.savedNotesViews.isEmpty())
    }

    @Test
    fun `given save fails when view selected then emits its error and keeps chosen view`() = runTest {
        settings.notesViewSaveResult = AppResult.Error(SettingsError.Storage)
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        viewModel.effects.test {
            viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.LIST))

            assertEquals(NoteListEffect.ShowNotesViewSaveError, awaitItem())
            expectNoEvents()
        }

        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
    }

    @Test
    fun `given category filter when view changes then keeps filter and its notes`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))
        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.WORK))
        val notesBefore = viewModel.state.value.notes

        viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.LIST))

        val state = viewModel.state.value
        assertEquals(NotesView.LIST, state.notesView)
        assertEquals(NoteFilter.WORK, state.selectedFilter)
        assertEquals(listOf(1L), state.notes.map { it.id })
        assertEquals(notesBefore, state.notes)
    }

    @Test
    fun `given loaded notes when view changes then keeps the same notes instance`() = runTest {
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))
        val notesBefore = viewModel.state.value.notes
        assertEquals(listOf(1L, 2L, 3L), notesBefore.map { it.id })

        viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.LIST))

        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
        assertSame(notesBefore, viewModel.state.value.notes)
    }

    @Test
    fun `given active search when view changes then keeps query and results`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        notes.emit(AppResult.Success(searchNotes))
        viewModel.onIntent(NoteListIntent.QueryChanged("reunion"))

        viewModel.onIntent(NoteListIntent.NotesViewSelected(NotesView.LIST))

        val state = viewModel.state.value
        assertEquals(NotesView.LIST, state.notesView)
        assertEquals("reunion", state.query)
        assertEquals("reunion", savedStateHandle.get<String>("query"))
        assertEquals(listOf(1L), state.notes.map { it.id })
    }

    @Test
    fun `given list view when searching then results are shown in that view`() = runTest {
        settings.emitStoredNotesView(NotesView.LIST)
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(searchNotes))

        viewModel.onIntent(NoteListIntent.QueryChanged("lista"))

        val state = viewModel.state.value
        assertEquals(NotesView.LIST, state.notesView)
        assertEquals(listOf(2L, 3L), state.notes.map { it.id })
    }

    @Test
    fun `given list view when filter changes then keeps the view`() = runTest {
        settings.emitStoredNotesView(NotesView.LIST)
        val viewModel = createViewModel()
        notes.emit(AppResult.Success(mixedNotes))

        viewModel.onIntent(NoteListIntent.FilterSelected(NoteFilter.PERSONAL))

        assertEquals(NotesView.LIST, viewModel.state.value.notesView)
        assertEquals(listOf(2L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given notes with and without category when loaded then ui items carry it`() = runTest {
        val viewModel = createViewModel()

        notes.emit(AppResult.Success(mixedNotes))

        assertEquals(
            listOf(NoteCategory.WORK, NoteCategory.PERSONAL, null),
            viewModel.state.value.notes.map { it.category },
        )
    }

    @Test
    fun `given note without tasks when loaded then list content is the card content`() = runTest {
        val viewModel = createViewModel()
        val spans = listOf(FormatSpan(0, 2, TextFormat.BOLD))

        notes.emit(AppResult.Success(listOf(note(id = 1, content = "Un café\nrápido", contentSpans = spans))))

        val item = viewModel.state.value.notes.single()
        assertSame(item.content, item.listContent)
    }

    @Test
    fun `given note with consecutive tasks when loaded then only list content joins them with a mark`() = runTest {
        val viewModel = createViewModel()
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))

        notes.emit(AppResult.Success(listOf(note(id = 1, content = "Tomates\nPan\nNota", contentTasks = tasks))))

        val item = viewModel.state.value.notes.single()
        assertEquals("Tomates\nPan\nNota", item.content.text.replace("\u200B", ""))
        assertEquals("Tomates${TASK_BREAK}Pan\nNota", item.listContent.text.replace("\u200B", ""))
        assertEquals(
            listOf(AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.LineThrough), 10, 13)),
            item.listContent.spanStyles,
        )
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
        contentTasks: List<ChecklistItem> = emptyList(),
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
        contentTasks = contentTasks,
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
