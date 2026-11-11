package com.example.notesapp.presentation.notes.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.AddNoteImagesUseCase
import com.example.notesapp.domain.notes.BuildNoteShareUseCase
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.CleanUpImageFilesUseCase
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.DiscardImageCopiesUseCase
import com.example.notesapp.domain.notes.EditField
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.ImportNoteImagesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.RemoveNoteImageUseCase
import com.example.notesapp.domain.notes.RestoreNoteUseCase
import com.example.notesapp.domain.notes.SaveNoteChangesUseCase
import com.example.notesapp.domain.notes.SaveNoteUseCase
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.domain.settings.GetAutoSaveEnabledUseCase
import com.example.notesapp.domain.settings.GetDefaultNoteColorUseCase
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateFormatter
import com.example.notesapp.presentation.settings.FakeSettingsRepository
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = InMemoryNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val settingsRepository = FakeSettingsRepository()
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
        category = null,
        updatedAt = NOW - 1_000,
    )

    /**
     * Las pruebas sin modo explícito son del guardado automático: siguen comprobando lo mismo que antes de
     * existir el ajuste (RF-9). Las del modo manual lo apagan antes de crear el ViewModel.
     */
    @Before
    fun setUp() = runTest {
        settingsRepository.emitStoredAutoSaveEnabled(true)
    }

    /** Una nota nueva recibe ya su color predeterminado; así cada prueba empieza con la lectura resuelta. */
    private suspend fun createViewModel(
        noteId: Long? = null,
        category: String? = null,
        defaultColor: NoteColor? = NoteColor.DEFAULT,
        savedStateHandle: SavedStateHandle = savedStateHandle(noteId, category),
    ): NoteEditorViewModel {
        val viewModel = buildViewModel(noteId, category, savedStateHandle)
        if (noteId == null) {
            settingsRepository.emitStoredDefaultNoteColor(defaultColor)
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        }
        return viewModel
    }

    /** Sin emitir el color predeterminado: la lectura queda pendiente hasta que la prueba lo emita. */
    private fun buildViewModel(
        noteId: Long? = null,
        category: String? = null,
        savedStateHandle: SavedStateHandle = savedStateHandle(noteId, category),
        settings: FakeSettingsRepository = settingsRepository,
    ) = NoteEditorViewModel(
        savedStateHandle = savedStateHandle,
        getNote = GetNoteUseCase(repository),
        saveNote = SaveNoteUseCase(repository, TimeProvider { NOW }),
        restoreNote = RestoreNoteUseCase(repository),
        deleteNote = DeleteNoteUseCase(repository, imageStorage),
        addNoteImages = AddNoteImagesUseCase(repository, imageStorage, TimeProvider { NOW }),
        removeNoteImage = RemoveNoteImageUseCase(repository, imageStorage),
        dateFormatter = dateFormatter,
        getDefaultNoteColor = GetDefaultNoteColorUseCase(settings),
        getAutoSaveEnabled = GetAutoSaveEnabledUseCase(settings),
        importNoteImages = ImportNoteImagesUseCase(imageStorage),
        saveNoteChanges = SaveNoteChangesUseCase(repository, imageStorage, TimeProvider { NOW }),
        discardImageCopies = DiscardImageCopiesUseCase(imageStorage),
        cleanUpImageFiles = CleanUpImageFilesUseCase(repository, imageStorage),
        buildNoteShare = BuildNoteShareUseCase(imageStorage),
    )

    private fun savedStateHandle(noteId: Long? = null, category: String? = null) = SavedStateHandle(
        buildMap {
            noteId?.let { put(NoteEditorViewModel.NOTE_ID_ARG, it) }
            category?.let { put(NoteEditorViewModel.CATEGORY_ARG, it) }
        },
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
        assertEquals(existing.title, state.title.text)
        assertEquals(existing.content, state.content.text)
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

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ide")))
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
        runCurrent()

        assertTrue(repository.notes.isEmpty())
        assertEquals("Ide", viewModel.state.value.title.text)
    }

    @Test
    fun `given typing stops when autosave delay elapses then creates the note once`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ide")))
        advanceTimeBy(200)
        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ideas")))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Libreta")))
        advanceUntilIdle()

        val saved = repository.notes.values.single()
        assertEquals("Ideas", saved.title)
        assertEquals("Libreta", saved.content)
        assertEquals(NOW, saved.updatedAt)
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Libreta y café")))
        advanceUntilIdle()

        assertEquals("Libreta y café", repository.notes.values.single().content)
    }

    @Test
    fun `given existing note when edited then updates it keeping id and color`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Nuevo texto")))
        advanceUntilIdle()

        assertEquals(existing.copy(content = "Nuevo texto", updatedAt = NOW), repository.notes[existing.id])
    }

    @Test
    fun `given blank new note when back clicked then navigates back without creating it`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("   ")))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given pending changes when back clicked then saves before navigating back`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Para leer")))
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
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Para leer")))
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
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Borrador")))
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

    @Test
    fun `given category argument when creating new note then starts with that category`() = runTest {
        val viewModel = createViewModel(category = NoteCategory.WORK.name)
        advanceUntilIdle()

        assertEquals(NoteEditorUiState(category = NoteCategory.WORK), viewModel.state.value)
    }

    @Test
    fun `given category argument when new note is autosaved then stores that category`() = runTest {
        val viewModel = createViewModel(category = NoteCategory.PERSONAL.name)

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("La compra")))
        advanceUntilIdle()

        assertEquals(NoteCategory.PERSONAL, repository.notes.values.single().category)
    }

    @Test
    fun `given unknown category argument when creating new note then has no category`() = runTest {
        val viewModel = createViewModel(category = "HOBBY")
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.category)
    }

    @Test
    fun `given existing note with category when loaded then exposes its category`() = runTest {
        repository.notes[existing.id] = existing.copy(category = NoteCategory.WORK)
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(NoteCategory.WORK, viewModel.state.value.category)
    }

    @Test
    fun `given existing note when category clicked then assigns it and saves immediately`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.PERSONAL))
        runCurrent()

        assertEquals(NoteCategory.PERSONAL, viewModel.state.value.category)
        assertEquals(NoteCategory.PERSONAL, repository.notes[existing.id]?.category)
    }

    @Test
    fun `given note with category when same category clicked then removes it and saves`() = runTest {
        repository.notes[existing.id] = existing.copy(category = NoteCategory.WORK)
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
        runCurrent()

        assertEquals(null, viewModel.state.value.category)
        assertEquals(null, repository.notes[existing.id]?.category)
    }

    @Test
    fun `given note with category when other category clicked then replaces it`() = runTest {
        repository.notes[existing.id] = existing.copy(category = NoteCategory.WORK)
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.PERSONAL))
        runCurrent()

        assertEquals(NoteCategory.PERSONAL, viewModel.state.value.category)
        assertEquals(NoteCategory.PERSONAL, repository.notes[existing.id]?.category)
    }

    @Test
    fun `given category changed when back clicked then navigates back`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            advanceUntilIdle()
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `given existing note with color and images when loaded then exposes them`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(NoteColor.ROSE, viewModel.state.value.color)
        assertEquals(listOf(imageA), viewModel.state.value.images)
    }

    @Test
    fun `given editor when add image clicked then opens the image picker`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.AddImageClicked)
            assertEquals(NoteEditorEffect.OpenImagePicker, awaitItem())
        }
    }

    @Test
    fun `given picker closed without images when images picked then does nothing`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val before = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(emptyList()))
            advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(before, viewModel.state.value)
        assertTrue(imageStorage.savedUris.isEmpty())
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given existing note when images picked then shows them and marks the note saved`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_B)))
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(listOf(imageA.path, copyOf(URI_B)), stored.images.map { it.path })
        assertEquals(NOW, stored.updatedAt)
        val state = viewModel.state.value
        assertEquals(stored.images, state.images)
        assertEquals(dateFormatter.format(NOW), state.date)
        assertEquals(SaveStatus.Saved, state.saveStatus)
    }

    @Test
    fun `given new note without text when images picked then creates the note with the image`() = runTest {
        val viewModel = createViewModel(category = NoteCategory.WORK.name)

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals("", stored.title)
        assertEquals(NoteCategory.WORK, stored.category)
        assertEquals(listOf(copyOf(URI_A)), stored.images.map { it.path })
        assertEquals(stored.images, viewModel.state.value.images)
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given new note created by adding image when back clicked then navigates back`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertEquals(1, repository.notes.size)
    }

    @Test
    fun `given unsaved text when images picked then saves text and images in a single note`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Escapada")))

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals("Escapada", stored.title)
        assertEquals(listOf(copyOf(URI_A)), stored.images.map { it.path })
    }

    @Test
    fun `given note created by adding image when text typed then updates that same note`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Escapada")))
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals("Escapada", stored.title)
        assertEquals(listOf(copyOf(URI_A)), stored.images.map { it.path })
    }

    @Test
    fun `given image that cannot be copied when images picked then shows image error`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        imageStorage.unavailableUris += URI_A

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error), awaitItem())
        }
        assertTrue(viewModel.state.value.images.isEmpty())
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given storage failure when images picked then shows image error and keeps images`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        repository.addImagesFailure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_B)))
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error), awaitItem())
        }
        assertEquals(listOf(imageA), viewModel.state.value.images)
    }

    @Test
    fun `given some images cannot be copied when images picked then adds the rest and shows image error`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        imageStorage.unavailableUris += URI_A

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A, URI_B)))
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error), awaitItem())
        }
        assertEquals(listOf(copyOf(URI_B)), viewModel.state.value.images.map { it.path })
        assertEquals(listOf(copyOf(URI_B)), repository.notes[existing.id]?.images?.map { it.path })
    }

    @Test
    fun `given every image copied when images picked then shows no message`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A, URI_B)))
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `given new note created by adding image and image removed when back clicked then deletes it and navigates back`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(viewModel.state.value.images.single()))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.BackClicked)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            }
            assertTrue(repository.notes.isEmpty())
        }

    @Test
    fun `given note with text and image removed when back clicked then keeps the note`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given new note with title created by adding image and image removed when back clicked then keeps the note`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Escapada")))
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(viewModel.state.value.images.single()))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.BackClicked)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            }
            assertEquals("Escapada", repository.notes.values.single().title)
            assertTrue(repository.notes.values.single().images.isEmpty())
        }

    @Test
    fun `given note with images when remove image clicked then removes it and deletes its file`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA, imageB))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        assertEquals(listOf(imageB), viewModel.state.value.images)
        assertEquals(listOf(imageB), repository.notes[existing.id]?.images)
        assertEquals(listOf(imageA.path), imageStorage.deletedPaths)
    }

    @Test
    fun `given remove failure when remove image clicked then shows message and keeps the image`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        repository.removeImageFailure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_remove_error), awaitItem())
        }
        assertEquals(listOf(imageA), viewModel.state.value.images)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given image removed when note is edited then the image does not come back`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_B)))
        advanceUntilIdle()

        assertEquals(listOf(copyOf(URI_B)), viewModel.state.value.images.map { it.path })
    }

    @Test
    fun `given note with images when delete confirmed then deletes their files`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA, imageB))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertEquals(listOf(imageA.path, imageB.path), imageStorage.deletedPaths)
    }

    @Test
    fun `given editor when format button clicked and menu dismissed then toggles the format menu`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.FormatButtonClicked)
        assertTrue(viewModel.state.value.isFormatMenuVisible)

        viewModel.onIntent(NoteEditorIntent.FormatMenuDismissed)
        assertFalse(viewModel.state.value.isFormatMenuVisible)
    }

    @Test
    fun `given content focus changes when intents received then updates content focus`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.ContentFocusChanged(isFocused = true))
        assertTrue(viewModel.state.value.isContentFocused)

        viewModel.onIntent(NoteEditorIntent.ContentFocusChanged(isFocused = false))
        assertFalse(viewModel.state.value.isContentFocused)
    }

    @Test
    fun `given selected text when format selected then applies it keeping selection and closing the menu`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val selection = TextRange(0, 5)
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, selection)))
        viewModel.onIntent(NoteEditorIntent.FormatButtonClicked)

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        val state = viewModel.state.value
        assertEquals(listOf(FormatSpan(0, 5, TextFormat.BOLD)), state.contentSpans)
        assertEquals(selection, state.content.selection)
        assertEquals(existing.content, state.content.text)
        assertEquals(setOf(TextFormat.BOLD), state.activeFormats)
        assertFalse(state.isFormatMenuVisible)
    }

    @Test
    fun `given format applied to selection when autosave delay elapses then saves spans with a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(old.content, TextRange(6, 11))))

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.UNDERLINE))
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
        runCurrent()

        assertEquals(old, repository.notes[old.id])

        advanceUntilIdle()

        val stored = repository.notes.getValue(old.id)
        assertEquals(listOf(FormatSpan(6, 11, TextFormat.UNDERLINE)), stored.contentSpans)
        assertEquals(old.content, stored.content)
        assertEquals(NOW, stored.updatedAt)
        assertEquals(dateFormatter.format(NOW), viewModel.state.value.date)
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given selection fully formatted when format selected then removes it and saves`() = runTest {
        repository.notes[existing.id] = existing.copy(contentSpans = listOf(FormatSpan(0, 11, TextFormat.BOLD)))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(0, 5))))

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
        advanceUntilIdle()

        val expected = listOf(FormatSpan(5, 11, TextFormat.BOLD))
        assertEquals(expected, viewModel.state.value.contentSpans)
        assertEquals(emptySet<TextFormat>(), viewModel.state.value.activeFormats)
        assertEquals(expected, repository.notes.getValue(existing.id).contentSpans)
    }

    @Test
    fun `given cursor without selection when format selected then toggles active formats without saving`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(5))))
        viewModel.onIntent(NoteEditorIntent.FormatButtonClicked)

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        assertEquals(setOf(TextFormat.BOLD), viewModel.state.value.activeFormats)
        assertFalse(viewModel.state.value.isFormatMenuVisible)
        assertTrue(viewModel.state.value.contentSpans.isEmpty())

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        assertEquals(emptySet<TextFormat>(), viewModel.state.value.activeFormats)
    }

    @Test
    fun `given format chosen without selection when nothing typed then the note and its date do not change`() =
        runTest {
            repository.notes[existing.id] = existing
            val viewModel = createViewModel(existing.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(5))))

            viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.STRIKETHROUGH))
            advanceUntilIdle()

            assertEquals(existing, repository.notes[existing.id])
            assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
        }

    @Test
    fun `given format chosen without selection when back clicked then navigates back without saving`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given active formats when typing then typed text gets them and is saved`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("H", TextRange(1))))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Ho", TextRange(2))))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Hola", TextRange(4))))
        advanceUntilIdle()

        val expected = listOf(FormatSpan(0, 4, TextFormat.BOLD))
        assertEquals(expected, viewModel.state.value.contentSpans)
        assertEquals(setOf(TextFormat.BOLD), viewModel.state.value.activeFormats)
        val stored = repository.notes.values.single()
        assertEquals("Hola", stored.content)
        assertEquals(expected, stored.contentSpans)
    }

    @Test
    fun `given no active formats when typing after formatted text then typed text stays plain`() = runTest {
        repository.notes[existing.id] = existing.copy(content = "Hola", contentSpans = listOf(FormatSpan(0, 4, TextFormat.BOLD)))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Hola", TextRange(4))))
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Hola!", TextRange(5))))

        assertEquals(listOf(FormatSpan(0, 4, TextFormat.BOLD)), viewModel.state.value.contentSpans)
    }

    @Test
    fun `given active formats when pasting several letters then pasted text gets all of them`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val end = existing.content.length
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(end))))
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.UNDERLINE))

        val pasted = existing.content + " Pegado"
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(pasted, TextRange(pasted.length))))
        advanceUntilIdle()

        val expected = listOf(
            FormatSpan(end, pasted.length, TextFormat.BOLD),
            FormatSpan(end, pasted.length, TextFormat.UNDERLINE),
        )
        assertEquals(expected, viewModel.state.value.contentSpans)
        assertEquals(expected, repository.notes.getValue(existing.id).contentSpans)
        assertEquals(pasted, repository.notes.getValue(existing.id).content)
    }

    @Test
    fun `given cursor moved into formatted text when selection changes then active formats follow without saving`() =
        runTest {
            val formatted = existing.copy(contentSpans = listOf(FormatSpan(0, 5, TextFormat.BOLD)))
            repository.notes[formatted.id] = formatted
            val viewModel = createViewModel(formatted.id)
            advanceUntilIdle()

            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(formatted.content, TextRange(3))))
            assertEquals(setOf(TextFormat.BOLD), viewModel.state.value.activeFormats)

            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(formatted.content, TextRange(10))))
            assertEquals(emptySet<TextFormat>(), viewModel.state.value.activeFormats)

            advanceUntilIdle()
            assertEquals(formatted, repository.notes[formatted.id])
            assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
            assertEquals(TextRange(10), viewModel.state.value.content.selection)
        }

    @Test
    fun `given format activated without typing when cursor moves elsewhere then format is no longer active`() =
        runTest {
            repository.notes[existing.id] = existing
            val viewModel = createViewModel(existing.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(5))))
            viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(12))))
            advanceUntilIdle()

            assertEquals(emptySet<TextFormat>(), viewModel.state.value.activeFormats)
            assertEquals(existing, repository.notes[existing.id])
        }

    @Test
    fun `given range fully covered by a format when selected then that format is active`() = runTest {
        val formatted = existing.copy(
            contentSpans = listOf(FormatSpan(0, 5, TextFormat.BOLD), FormatSpan(3, 11, TextFormat.UNDERLINE)),
        )
        repository.notes[formatted.id] = formatted
        val viewModel = createViewModel(formatted.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(formatted.content, TextRange(3, 5))))
        assertEquals(setOf(TextFormat.BOLD, TextFormat.UNDERLINE), viewModel.state.value.activeFormats)

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(formatted.content, TextRange(0, 5))))
        assertEquals(setOf(TextFormat.BOLD), viewModel.state.value.activeFormats)

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(formatted.content, TextRange(0, 11))))
        assertEquals(emptySet<TextFormat>(), viewModel.state.value.activeFormats)

        advanceUntilIdle()
        assertEquals(formatted, repository.notes[formatted.id])
    }

    @Test
    fun `given note with spans when loaded then exposes its spans`() = runTest {
        val spans = listOf(FormatSpan(0, 5, TextFormat.BOLD), FormatSpan(13, 21, TextFormat.STRIKETHROUGH))
        repository.notes[existing.id] = existing.copy(contentSpans = spans)

        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(spans, viewModel.state.value.contentSpans)
        assertEquals(existing.content, viewModel.state.value.content.text)
    }

    @Test
    fun `given note without spans when loaded and edited then stays plain text`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.contentSpans.isEmpty())
        assertTrue(viewModel.state.value.activeFormats.isEmpty())

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content + "!")))
        advanceUntilIdle()

        assertTrue(repository.notes.getValue(existing.id).contentSpans.isEmpty())
    }

    @Test
    fun `given new note when color button clicked then opens the picker with the current color`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)

        assertEquals(NoteColor.AQUA, viewModel.state.value.pickerColor)
        assertEquals(NoteColor.AQUA, viewModel.state.value.color)
    }

    @Test
    fun `given existing note with other color when color button clicked then opens the picker with its color`() =
        runTest {
            repository.notes[existing.id] = existing.copy(color = NoteColor.SAND)
            val viewModel = createViewModel(existing.id)
            advanceUntilIdle()

            viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)

            assertEquals(NoteColor.SAND, viewModel.state.value.pickerColor)
        }

    @Test
    fun `given open picker when other color selected then only changes the picker without saving`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)

        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.MINT))
        advanceUntilIdle()

        assertEquals(NoteColor.MINT, viewModel.state.value.pickerColor)
        assertEquals(NoteColor.ROSE, viewModel.state.value.color)
        assertEquals(0, repository.saveCount)
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given saved note when other color applied then assigns it closes the picker and saves once`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.LAVENDER))

        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(NoteColor.LAVENDER, state.color)
        assertEquals(null, state.pickerColor)
        assertEquals(1, repository.saveCount)
        assertEquals(old.copy(color = NoteColor.LAVENDER, updatedAt = NOW), repository.notes[old.id])
        assertEquals(dateFormatter.format(NOW), state.date)
        assertEquals(SaveStatus.Saved, state.saveStatus)
    }

    @Test
    fun `given saved note when same color applied then closes the picker without saving`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.MINT))
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.ROSE))

        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(NoteColor.ROSE, state.color)
        assertEquals(null, state.pickerColor)
        assertEquals(0, repository.saveCount)
        assertEquals(old, repository.notes[old.id])
        assertEquals(dateFormatter.format(old.updatedAt), state.date)
        assertEquals(SaveStatus.Idle, state.saveStatus)
    }

    @Test
    fun `given other color selected when picker dismissed then keeps the color without saving`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.PEACH))

        viewModel.onIntent(NoteEditorIntent.ColorPickerDismissed)
        advanceUntilIdle()

        assertEquals(NoteColor.ROSE, viewModel.state.value.color)
        assertEquals(null, viewModel.state.value.pickerColor)
        assertEquals(0, repository.saveCount)
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given dismissed color choice when picker reopened then shows the current color`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.PEACH))
        viewModel.onIntent(NoteEditorIntent.ColorPickerDismissed)

        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)

        assertEquals(NoteColor.ROSE, viewModel.state.value.pickerColor)
    }

    @Test
    fun `given blank new note when color applied then no note is stored`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.VANILLA))

        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        assertTrue(repository.notes.isEmpty())
        assertEquals(NoteColor.VANILLA, viewModel.state.value.color)
        assertEquals(null, viewModel.state.value.pickerColor)
        assertEquals(null, viewModel.state.value.date)
    }

    @Test
    fun `given color applied to blank new note when title typed then saves the note with that color`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.VANILLA))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Recetas")))
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals("Recetas", stored.title)
        assertEquals(NoteColor.VANILLA, stored.color)
    }

    @Test
    fun `given note with lavender color when loaded then exposes its color`() = runTest {
        repository.notes[existing.id] = existing.copy(color = NoteColor.LAVENDER)

        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(NoteColor.LAVENDER, viewModel.state.value.color)
        assertEquals(null, viewModel.state.value.pickerColor)
    }

    @Test
    fun `given note without color when loaded then exposes none`() = runTest {
        repository.notes[existing.id] = existing.copy(color = NoteColor.NONE)

        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(NoteColor.NONE, viewModel.state.value.color)
    }

    @Test
    fun `given color applied to blank new note when images picked then stores the note with that color`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.MINT))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals(NoteColor.MINT, stored.color)
        assertEquals(listOf(copyOf(URI_A)), stored.images.map { it.path })
        assertEquals(NoteColor.MINT, viewModel.state.value.color)
    }

    @Test
    fun `given color applied to existing note when images picked then keeps that color`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.PEACH))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_B)))
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(NoteColor.PEACH, stored.color)
        assertEquals(listOf(copyOf(URI_B)), stored.images.map { it.path })
    }

    @Test
    fun `given cursor on a line when checklist clicked then makes it a pending task and autosaves with a new date`() =
        runTest {
            val old = existing.copy(content = "Leche\nPan", updatedAt = NOW - TEN_DAYS_MS)
            repository.notes[old.id] = old
            val viewModel = createViewModel(old.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(old.content, TextRange(7))))

            viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
            advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
            runCurrent()

            assertEquals(listOf(task(1)), viewModel.state.value.contentTasks)
            assertEquals(old, repository.notes[old.id])

            advanceUntilIdle()

            val stored = repository.notes.getValue(old.id)
            assertEquals(listOf(task(1)), stored.contentTasks)
            assertEquals(NOW, stored.updatedAt)
            assertEquals(dateFormatter.format(NOW), viewModel.state.value.date)
            assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)
        }

    @Test
    fun `given cursor on a task when checklist clicked then returns it to text and saves`() = runTest {
        val withTask = existing.copy(content = "Leche\nPan", contentTasks = listOf(task(1)))
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(withTask.content, TextRange(7))))

        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.contentTasks.isEmpty())
        assertTrue(repository.notes.getValue(withTask.id).contentTasks.isEmpty())
    }

    @Test
    fun `given selection over mixed lines when checklist clicked then all become tasks keeping existing ones`() =
        runTest {
            val mixed = existing.copy(content = "a\nb\nc", contentTasks = listOf(task(1, isDone = true)))
            repository.notes[mixed.id] = mixed
            val viewModel = createViewModel(mixed.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(mixed.content, TextRange(0, 5))))

            viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
            advanceUntilIdle()

            val expected = listOf(task(0), task(1, isDone = true), task(2))
            assertEquals(expected, viewModel.state.value.contentTasks)
            assertEquals(expected, repository.notes.getValue(mixed.id).contentTasks)
        }

    @Test
    fun `given selection over lines that are all tasks when checklist clicked then all return to text`() = runTest {
        val allTasks = existing.copy(
            content = "a\nb\nc",
            contentTasks = listOf(task(0), task(1, isDone = true), task(2)),
        )
        repository.notes[allTasks.id] = allTasks
        val viewModel = createViewModel(allTasks.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(allTasks.content, TextRange(0, 5))))

        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.contentTasks.isEmpty())
        assertTrue(repository.notes.getValue(allTasks.id).contentTasks.isEmpty())
    }

    @Test
    fun `given pending task when toggled then marks it done and saves immediately keeping spans`() = runTest {
        val spans = listOf(FormatSpan(0, 5, TextFormat.BOLD))
        val old = existing.copy(
            content = "Leche\nPan",
            contentSpans = spans,
            contentTasks = listOf(task(0), task(1)),
            updatedAt = NOW - TEN_DAYS_MS,
        )
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.TaskToggled(line = 1))
        runCurrent()

        val expected = listOf(task(0), task(1, isDone = true))
        val state = viewModel.state.value
        assertEquals(expected, state.contentTasks)
        assertEquals(spans, state.contentSpans)
        val stored = repository.notes.getValue(old.id)
        assertEquals(expected, stored.contentTasks)
        assertEquals(spans, stored.contentSpans)
        assertEquals(NOW, stored.updatedAt)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun `given done task when toggled then marks it pending and saves immediately`() = runTest {
        val done = existing.copy(content = "Leche\nPan", contentTasks = listOf(task(0, isDone = true)))
        repository.notes[done.id] = done
        val viewModel = createViewModel(done.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.TaskToggled(line = 0))
        runCurrent()

        assertEquals(listOf(task(0)), viewModel.state.value.contentTasks)
        assertEquals(listOf(task(0)), repository.notes.getValue(done.id).contentTasks)
    }

    @Test
    fun `given line that is not a task when toggled then nothing changes and nothing is saved`() = runTest {
        val withTask = existing.copy(content = "Leche\nPan", contentTasks = listOf(task(0)))
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()
        val before = viewModel.state.value

        viewModel.onIntent(NoteEditorIntent.TaskToggled(line = 1))
        advanceUntilIdle()

        assertEquals(before, viewModel.state.value)
        assertEquals(0, repository.saveCount)
        assertEquals(withTask, repository.notes[withTask.id])
    }

    @Test
    fun `given empty task when enter typed then cancels the line break and turns it into text`() = runTest {
        val spans = listOf(FormatSpan(0, 1, TextFormat.BOLD))
        val emptyTask = existing.copy(content = "A\n", contentSpans = spans, contentTasks = listOf(task(1)))
        repository.notes[emptyTask.id] = emptyTask
        val viewModel = createViewModel(emptyTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("A\n", TextRange(2))))

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("A\n\n", TextRange(3))))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(TextFieldValue("A\n", TextRange(2)), state.content)
        assertTrue(state.contentTasks.isEmpty())
        assertEquals(spans, state.contentSpans)
        val stored = repository.notes.getValue(emptyTask.id)
        assertEquals("A\n", stored.content)
        assertTrue(stored.contentTasks.isEmpty())
    }

    @Test
    fun `given cursor at start of a task when backspace pressed then cancels the deletion and turns it into text`() =
        runTest {
            val spans = listOf(FormatSpan(2, 3, TextFormat.BOLD))
            val withTask = existing.copy(content = "a\nb", contentSpans = spans, contentTasks = listOf(task(1)))
            repository.notes[withTask.id] = withTask
            val viewModel = createViewModel(withTask.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("a\nb", TextRange(2))))

            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("ab", TextRange(1))))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(TextFieldValue("a\nb", TextRange(2)), state.content)
            assertTrue(state.contentTasks.isEmpty())
            assertEquals(spans, state.contentSpans)
            val stored = repository.notes.getValue(withTask.id)
            assertEquals("a\nb", stored.content)
            assertEquals(spans, stored.contentSpans)
            assertTrue(stored.contentTasks.isEmpty())
        }

    @Test
    fun `given task with text when enter typed at its end then next line is a pending task`() = runTest {
        val withTask = existing.copy(content = "Leche", contentTasks = listOf(task(0, isDone = true)))
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Leche", TextRange(5))))

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Leche\n", TextRange(6))))
        advanceUntilIdle()

        val expected = listOf(task(0, isDone = true), task(1))
        assertEquals(TextFieldValue("Leche\n", TextRange(6)), viewModel.state.value.content)
        assertEquals(expected, viewModel.state.value.contentTasks)
        assertEquals(expected, repository.notes.getValue(withTask.id).contentTasks)
    }

    @Test
    fun `given task when several lines pasted into it then the following lines are pending tasks`() = runTest {
        val withTask = existing.copy(content = "Leche", contentTasks = listOf(task(0)))
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Leche", TextRange(5))))

        val pasted = "Leche\nPan\nHuevos"
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(pasted, TextRange(pasted.length))))
        advanceUntilIdle()

        val expected = listOf(task(0), task(1), task(2))
        assertEquals(pasted, viewModel.state.value.content.text)
        assertEquals(expected, viewModel.state.value.contentTasks)
        val stored = repository.notes.getValue(withTask.id)
        assertEquals(pasted, stored.content)
        assertEquals(expected, stored.contentTasks)
    }

    @Test
    fun `given note with tasks when loaded then exposes its tasks`() = runTest {
        val tasks = listOf(task(0, isDone = true), task(2))
        repository.notes[existing.id] = existing.copy(content = "Leche\nPan\nHuevos", contentTasks = tasks)

        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(tasks, viewModel.state.value.contentTasks)
    }

    @Test
    fun `given unsaved tasks when images picked then saves the tasks with the images`() = runTest {
        val plain = existing.copy(content = "Leche\nPan")
        repository.notes[plain.id] = plain
        val viewModel = createViewModel(plain.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(plain.content, TextRange(0))))
        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        runCurrent()

        val stored = repository.notes.getValue(plain.id)
        assertEquals(listOf(task(0)), stored.contentTasks)
        assertEquals(listOf(copyOf(URI_A)), stored.images.map { it.path })

        advanceUntilIdle()

        assertEquals(1, repository.saveCount)
        assertEquals(listOf(task(0)), viewModel.state.value.contentTasks)
    }

    @Test
    fun `given note with tasks when only the cursor moves then tasks do not change and nothing is saved`() = runTest {
        val tasks = listOf(task(1, isDone = true))
        val withTask = existing.copy(content = "Leche\nPan", contentTasks = tasks)
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(withTask.content, TextRange(2))))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(withTask.content, TextRange(6, 9))))
        advanceUntilIdle()

        assertEquals(tasks, viewModel.state.value.contentTasks)
        assertEquals(TextRange(6, 9), viewModel.state.value.content.selection)
        assertEquals(0, repository.saveCount)
        assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
        assertEquals(withTask, repository.notes[withTask.id])
    }

    @Test
    fun `given new note when created then cannot undo`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given existing note when loaded then cannot undo`() = runTest {
        repository.notes[existing.id] = existing

        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given words typed letter by letter when undo clicked twice then removes one word at a time`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInContent("Hola mundo")
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        assertEquals("Hola ", viewModel.state.value.content.text)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        assertEquals("", viewModel.state.value.content.text)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given pause while typing a word when undo clicked then removes each part separately`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInContent("Para")
        advanceTimeBy(NoteEditorViewModel.UNDO_PAUSE_MS)
        runCurrent()
        viewModel.typeInContent("guas")

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        assertEquals("Para", viewModel.state.value.content.text)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        assertEquals("", viewModel.state.value.content.text)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given pause shorter than undo pause while typing a word when undo clicked then removes the whole word`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.typeInContent("Para")
            advanceTimeBy(NoteEditorViewModel.UNDO_PAUSE_MS - 1)
            runCurrent()
            viewModel.typeInContent("guas")

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            assertEquals("", viewModel.state.value.content.text)
            assertFalse(viewModel.state.value.canUndo)
        }

    @Test
    fun `given cursor moved in content between typing when undo clicked then removes only the last typing`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.typeInContent("Hola")
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Hola", TextRange(0))))

            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("¡Hola", TextRange(1))))
            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            assertEquals(TextFieldValue("Hola", TextRange(0)), viewModel.state.value.content)
            assertTrue(viewModel.state.value.canUndo)
        }

    @Test
    fun `given cursor moved in title between typing when undo clicked then removes only the last typing`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInTitle("Hola")
        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Hola", TextRange(0))))

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("¡Hola", TextRange(1))))
        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        assertEquals(TextFieldValue("Hola", TextRange(0)), viewModel.state.value.title)
        assertTrue(viewModel.state.value.canUndo)
    }

    @Test
    fun `given title then content typed when undo clicked twice then undoes content first and then title`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.typeInTitle("Compra")
            viewModel.typeInContent("Pan")

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val afterFirst = viewModel.state.value
            assertEquals("Compra", afterFirst.title.text)
            assertEquals("", afterFirst.content.text)
            assertEquals(EditField.CONTENT, afterFirst.focusRequest?.field)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val afterSecond = viewModel.state.value
            assertEquals("", afterSecond.title.text)
            assertEquals("", afterSecond.content.text)
            assertEquals(EditField.TITLE, afterSecond.focusRequest?.field)
            assertTrue(afterSecond.focusRequest?.id != afterFirst.focusRequest?.id)
            assertFalse(afterSecond.canUndo)
        }

    @Test
    fun `given format applied to selection when undo clicked then restores spans and selection`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val selection = TextRange(0, 5)
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, selection)))
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val state = viewModel.state.value
        assertTrue(state.contentSpans.isEmpty())
        assertEquals(TextFieldValue(existing.content, selection), state.content)
        assertEquals(emptySet<TextFormat>(), state.activeFormats)
        assertFalse(state.canUndo)
    }

    @Test
    fun `given checklist clicked when undo clicked then line returns to text`() = runTest {
        val plain = existing.copy(content = "Leche\nPan")
        repository.notes[plain.id] = plain
        val viewModel = createViewModel(plain.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(plain.content, TextRange(7))))
        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val state = viewModel.state.value
        assertTrue(state.contentTasks.isEmpty())
        assertEquals(TextFieldValue(plain.content, TextRange(7)), state.content)
        assertFalse(state.canUndo)
    }

    @Test
    fun `given task toggled with cursor in title when undo clicked then task is pending and focus goes to content`() =
        runTest {
            val withTasks = existing.copy(content = "Leche\nPan", contentTasks = listOf(task(0), task(1)))
            repository.notes[withTasks.id] = withTasks
            val viewModel = createViewModel(withTasks.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(withTasks.content, TextRange(3))))
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue(withTasks.title, TextRange(2))))
            viewModel.onIntent(NoteEditorIntent.TaskToggled(line = 1))
            assertTrue(viewModel.state.value.canUndo)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val state = viewModel.state.value
            assertEquals(listOf(task(0), task(1)), state.contentTasks)
            assertEquals(EditField.CONTENT, state.focusRequest?.field)
            assertEquals(TextRange(3), state.content.selection)
            assertFalse(state.canUndo)
        }

    @Test
    fun `given format chosen without selection when nothing typed then cannot undo`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(5))))

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))

        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given other color applied when intents received then cannot undo`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.LAVENDER))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        assertEquals(NoteColor.LAVENDER, viewModel.state.value.color)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given category clicked when intent received then cannot undo`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.entries.first()))
        advanceUntilIdle()

        assertEquals(NoteCategory.entries.first(), viewModel.state.value.category)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given pin clicked when intent received then cannot undo`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isPinned)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given typing then color changed when undo clicked then undoes typing and keeps the color`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.LAVENDER))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(existing.content, state.content.text)
        assertEquals(NoteColor.LAVENDER, state.color)
        assertEquals(NoteColor.LAVENDER, repository.notes.getValue(existing.id).color)
    }

    @Test
    fun `given selection replaced by typing when undo clicked then restores that selection in content`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val selection = TextRange(0, 5)
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, selection)))
        val replaced = "X" + existing.content.substring(5)
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(replaced, TextRange(1))))

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val state = viewModel.state.value
        assertEquals(TextFieldValue(existing.content, selection), state.content)
        assertEquals(FocusRequest(EditField.CONTENT, id = 1), state.focusRequest)
    }

    @Test
    fun `given bold activated without selection and text typed when undo clicked then text and bold go away`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
            viewModel.typeInContent("Hola")
            assertEquals(listOf(FormatSpan(0, 4, TextFormat.BOLD)), viewModel.state.value.contentSpans)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val state = viewModel.state.value
            assertEquals("", state.content.text)
            assertTrue(state.contentSpans.isEmpty())
            assertEquals(emptySet<TextFormat>(), state.activeFormats)
            assertFalse(state.canUndo)
        }

    @Test
    fun `given cursor moved out of bold text after typing when undo clicked then active formats follow restored cursor`() =
        runTest {
            val spans = listOf(FormatSpan(0, 5, TextFormat.BOLD))
            val formatted = existing.copy(contentSpans = spans)
            repository.notes[formatted.id] = formatted
            val viewModel = createViewModel(formatted.id)
            advanceUntilIdle()
            viewModel.moveContentCursorToEnd()
            viewModel.typeInContent("!")
            val typed = viewModel.state.value.content.text
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(typed, TextRange(5))))
            assertEquals(setOf(TextFormat.BOLD), viewModel.state.value.activeFormats)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val state = viewModel.state.value
            assertEquals(TextFieldValue(formatted.content, TextRange(formatted.content.length)), state.content)
            assertEquals(emptySet<TextFormat>(), state.activeFormats)
        }

    @Test
    fun `given two steps on existing note when one undone then saves undone text with a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent(" Sí")
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
        runCurrent()

        assertEquals(old.content + " Sí", repository.notes.getValue(old.id).content)

        advanceUntilIdle()

        val stored = repository.notes.getValue(old.id)
        assertEquals(old.content + " ", stored.content)
        assertEquals(NOW, stored.updatedAt)
        assertEquals(dateFormatter.format(NOW), viewModel.state.value.date)
        assertTrue(viewModel.state.value.canUndo)
    }

    @Test
    fun `given existing note when undone back to opened state then keeps its original date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        advanceUntilIdle()
        assertEquals(NOW, repository.notes.getValue(old.id).updatedAt)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        assertEquals(old, repository.notes[old.id])
        assertEquals(dateFormatter.format(old.updatedAt), viewModel.state.value.date)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `given color changed when undone back to opened text then saves with a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.LAVENDER))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        assertEquals(old.copy(color = NoteColor.LAVENDER, updatedAt = NOW), repository.notes[old.id])
        assertEquals(dateFormatter.format(NOW), viewModel.state.value.date)
    }

    @Test
    fun `given undo back to opened state when edited before autosave then saves with a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        repository.notes[old.id] = old
        val viewModel = createViewModel(old.id)
        advanceUntilIdle()
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceTimeBy(NoteEditorViewModel.AUTOSAVE_DELAY_MS - 1)
        runCurrent()
        // Escribe y borra un carácter: el texto vuelve al de apertura, pero la última edición ya no es deshacer.
        viewModel.typeInContent("?")
        val restored = viewModel.state.value.content.text.dropLast(1)
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(restored, TextRange(restored.length))))
        advanceUntilIdle()

        val stored = repository.notes.getValue(old.id)
        assertEquals(old.content, stored.content)
        assertEquals(NOW, stored.updatedAt)
        assertEquals(dateFormatter.format(NOW), viewModel.state.value.date)
    }

    @Test
    fun `given new note when every step undone then is empty and nothing is stored`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInTitle("Compra")
        viewModel.typeInContent("Pan y leche")

        repeat(4) { viewModel.onIntent(NoteEditorIntent.UndoClicked) }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.title.text)
        assertEquals("", state.content.text)
        assertFalse(state.canUndo)
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given enter typed on an empty task when undo clicked then the line is a task again`() = runTest {
        val emptyTask = existing.copy(content = "A\n", contentTasks = listOf(task(1)))
        repository.notes[emptyTask.id] = emptyTask
        val viewModel = createViewModel(emptyTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("A\n", TextRange(2))))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("A\n\n", TextRange(3))))
        assertTrue(viewModel.state.value.contentTasks.isEmpty())
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val state = viewModel.state.value
        assertEquals(listOf(task(1)), state.contentTasks)
        assertEquals(TextFieldValue("A\n", TextRange(2)), state.content)
        assertFalse(state.canUndo)
    }

    @Test
    fun `given backspace at start of a task when undo clicked then the line is a task again`() = runTest {
        val withTask = existing.copy(content = "a\nb", contentTasks = listOf(task(1)))
        repository.notes[withTask.id] = withTask
        val viewModel = createViewModel(withTask.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("a\nb", TextRange(2))))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("ab", TextRange(1))))
        assertTrue(viewModel.state.value.contentTasks.isEmpty())
        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val state = viewModel.state.value
        assertEquals(listOf(task(1)), state.contentTasks)
        assertEquals(TextFieldValue("a\nb", TextRange(2)), state.content)
        assertFalse(state.canUndo)
    }

    @Test
    fun `given no steps when undo clicked then state does not change and nothing is saved`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val before = viewModel.state.value

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        assertEquals(before, viewModel.state.value)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `given focus request when handled with its id then focus request is cleared`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInContent("Hola")
        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        assertEquals(FocusRequest(EditField.CONTENT, id = 1), viewModel.state.value.focusRequest)

        viewModel.onIntent(NoteEditorIntent.FocusRequestHandled(id = 1))

        assertEquals(null, viewModel.state.value.focusRequest)
    }

    @Test
    fun `given newer focus request when older id handled then state does not change`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInTitle("Compra")
        viewModel.typeInContent("Pan")
        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        val before = viewModel.state.value
        assertEquals(FocusRequest(EditField.TITLE, id = 2), before.focusRequest)

        viewModel.onIntent(NoteEditorIntent.FocusRequestHandled(id = 1))

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun `given focus request handled when undo clicked again then new request has a greater id`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInTitle("Compra")
        viewModel.typeInContent("Pan")
        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        val first = viewModel.state.value.focusRequest
        assertEquals(FocusRequest(EditField.CONTENT, id = 1), first)
        viewModel.onIntent(NoteEditorIntent.FocusRequestHandled(id = 1))
        assertEquals(null, viewModel.state.value.focusRequest)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)

        val second = viewModel.state.value.focusRequest
        assertEquals(EditField.TITLE, second?.field)
        assertTrue((second?.id ?: 0) > (first?.id ?: 0))
    }

    @Test
    fun `given selection with format and tasks when text pasted over it and undo clicked then restores text spans and tasks in one step`() =
        runTest {
            val spans = listOf(FormatSpan(6, 9, TextFormat.BOLD))
            val tasks = listOf(task(1, isDone = true), task(2))
            val rich = existing.copy(content = "Leche\nPan\nHuevos", contentSpans = spans, contentTasks = tasks)
            repository.notes[rich.id] = rich
            val viewModel = createViewModel(rich.id)
            advanceUntilIdle()
            val selection = TextRange(3, 12)
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(rich.content, selection)))
            val pasted = "Lec" + "XYZ" + rich.content.substring(12)
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(pasted, TextRange(6))))
            val afterPaste = viewModel.state.value
            assertEquals(pasted, afterPaste.content.text)
            assertTrue(afterPaste.contentSpans != spans)
            assertTrue(afterPaste.contentTasks != tasks)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val state = viewModel.state.value
            assertEquals(TextFieldValue(rich.content, selection), state.content)
            assertEquals(spans, state.contentSpans)
            assertEquals(tasks, state.contentTasks)
            assertFalse(state.canUndo)
        }

    @Test
    fun `given task with text when enter typed at its end and undo clicked then removes break and new task`() =
        runTest {
            val withTask = existing.copy(content = "Leche", contentTasks = listOf(task(0, isDone = true)))
            repository.notes[withTask.id] = withTask
            val viewModel = createViewModel(withTask.id)
            advanceUntilIdle()
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Leche", TextRange(5))))
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Leche\n", TextRange(6))))
            assertEquals(listOf(task(0, isDone = true), task(1)), viewModel.state.value.contentTasks)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            val state = viewModel.state.value
            assertEquals(TextFieldValue("Leche", TextRange(5)), state.content)
            assertEquals(listOf(task(0, isDone = true)), state.contentTasks)
            assertFalse(state.canUndo)
        }

    @Test
    fun `given long word typed for more than a second without pauses when undo clicked then removes the whole word`() =
        runTest {
            val viewModel = createViewModel()
            val word = "Paralelepi"
            word.forEachIndexed { index, char ->
                if (index > 0) advanceTimeBy(LETTER_INTERVAL_MS)
                viewModel.typeInContent(char.toString())
            }
            runCurrent()
            assertTrue((word.length - 1) * LETTER_INTERVAL_MS > 1_000)
            assertEquals(word, viewModel.state.value.content.text)

            viewModel.onIntent(NoteEditorIntent.UndoClicked)

            assertEquals("", viewModel.state.value.content.text)
            assertFalse(viewModel.state.value.canUndo)
        }

    @Test
    fun `given pending default color when new note created then stays loading until it arrives`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isNoteVisible)
        assertTrue(viewModel.state.value.isNewNote)

        settingsRepository.emitStoredDefaultNoteColor(NoteColor.MINT)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isNoteVisible)
        assertTrue(state.isNewNote)
        assertEquals(NoteColor.MINT, state.color)
    }

    @Test
    fun `given pending default color when new note has category then keeps it while loading`() = runTest {
        val viewModel = buildViewModel(category = NoteCategory.WORK.name)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoading)
        assertEquals(NoteCategory.WORK, viewModel.state.value.category)
    }

    @Test
    fun `given no stored default color when new note created then opens in aqua`() = runTest {
        val viewModel = createViewModel(defaultColor = null)

        assertEquals(NoteColor.AQUA, viewModel.state.value.color)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `given no color as default when new note created then opens without color`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.NONE)

        assertEquals(NoteColor.NONE, viewModel.state.value.color)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `given default color when new note is written then saves it with that color`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.VANILLA)

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ideas")))
        advanceUntilIdle()

        assertEquals(NoteColor.VANILLA, repository.notes.values.single().color)
    }

    @Test
    fun `given no color as default when new note is written then saves it without color`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.NONE)

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ideas")))
        advanceUntilIdle()

        assertEquals(NoteColor.NONE, repository.notes.values.single().color)
    }

    @Test
    fun `given default color when color picker opened on new note then marks it`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.SAND)

        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)

        assertEquals(NoteColor.SAND, viewModel.state.value.pickerColor)
    }

    @Test
    fun `given new note when other color applied then keeps it on save without changing default`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.VANILLA)

        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.PEACH))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("Ideas")))
        advanceUntilIdle()

        assertEquals(NoteColor.PEACH, viewModel.state.value.color)
        assertEquals(NoteColor.PEACH, repository.notes.values.single().color)
        assertTrue(settingsRepository.savedDefaultNoteColors.isEmpty())
    }

    @Test
    fun `given new note open when default color changes then keeps the color it opened with`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.VANILLA)

        settingsRepository.emitStoredDefaultNoteColor(NoteColor.MINT)
        advanceUntilIdle()

        assertEquals(NoteColor.VANILLA, viewModel.state.value.color)
    }

    @Test
    fun `given new note without text when back clicked then saves nothing and keeps default`() = runTest {
        val viewModel = createViewModel(defaultColor = NoteColor.VANILLA)

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)

            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }

        assertTrue(repository.notes.isEmpty())
        assertEquals(0, repository.saveCount)
        assertTrue(settingsRepository.savedDefaultNoteColors.isEmpty())
    }

    @Test
    fun `given pending default color when saved note opened then loads without waiting for it`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = buildViewModel(existing.id)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isNewNote)
        assertEquals(NoteColor.ROSE, viewModel.state.value.color)
    }

    @Test
    fun `given other default color when saved note opened and edited then keeps its own color`() = runTest {
        repository.notes[existing.id] = existing
        settingsRepository.emitStoredDefaultNoteColor(NoteColor.MINT)
        val viewModel = buildViewModel(existing.id)
        advanceUntilIdle()

        assertEquals(NoteColor.ROSE, viewModel.state.value.color)

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("Nuevo texto")))
        advanceUntilIdle()

        assertEquals(NoteColor.ROSE, repository.notes.getValue(existing.id).color)
    }

    @Test
    fun `given saved note when other color applied then default color does not change`() = runTest {
        repository.notes[existing.id] = existing
        settingsRepository.emitStoredDefaultNoteColor(NoteColor.MINT)
        val viewModel = buildViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ColorButtonClicked)
        viewModel.onIntent(NoteEditorIntent.PickerColorSelected(NoteColor.SAND))
        viewModel.onIntent(NoteEditorIntent.ColorApplied)
        advanceUntilIdle()

        assertEquals(NoteColor.SAND, repository.notes.getValue(existing.id).color)
        assertTrue(settingsRepository.savedDefaultNoteColors.isEmpty())
    }

    // --- Guardado automático apagado (spec 009) ---

    /** Abre el editor con el guardado automático apagado y espera a que termine de cargar. */
    private suspend fun createManualViewModel(
        noteId: Long? = null,
        category: String? = null,
        savedStateHandle: SavedStateHandle = savedStateHandle(noteId, category),
    ): NoteEditorViewModel {
        settingsRepository.emitStoredAutoSaveEnabled(false)
        val viewModel = createViewModel(noteId, category, savedStateHandle = savedStateHandle)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    /** Guarda [note] en el repositorio y la abre en modo manual. */
    private suspend fun openManually(
        note: Note = existing,
        savedStateHandle: SavedStateHandle = savedStateHandle(note.id),
    ): NoteEditorViewModel {
        repository.notes[note.id] = note
        return createManualViewModel(note.id, savedStateHandle = savedStateHandle)
    }

    /** La nota sigue como estaba guardada, nada se escribió en la base y el editor muestra cambios pendientes. */
    private fun NoteEditorViewModel.assertPendingAndNothingStored(stored: Note) {
        assertEquals(stored, repository.notes[stored.id])
        assertEquals(0, repository.saveCount)
        assertTrue(state.value.hasUnsavedChanges)
        assertEquals(SaveStatus.Unsaved, state.value.saveStatus)
    }

    private fun NoteEditorViewModel.assertNoPendingChanges() {
        assertFalse(state.value.hasUnsavedChanges)
        assertTrue(state.value.saveStatus != SaveStatus.Unsaved)
    }

    private fun NoteEditorViewModel.applyColor(color: NoteColor) {
        onIntent(NoteEditorIntent.ColorButtonClicked)
        onIntent(NoteEditorIntent.PickerColorSelected(color))
        onIntent(NoteEditorIntent.ColorApplied)
    }

    @Test
    fun `given auto save off when existing note opened then editor is manual without pending changes`() = runTest {
        val viewModel = openManually()

        val state = viewModel.state.value
        assertTrue(state.isManualSave)
        assertFalse(state.isLoading)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(SaveStatus.Idle, state.saveStatus)
        assertEquals(existing.title, state.title.text)
    }

    @Test
    fun `given auto save off when new note opened then editor is manual without pending changes`() = runTest {
        val viewModel = createManualViewModel()

        assertEquals(NoteEditorUiState(isManualSave = true), viewModel.state.value)
    }

    @Test
    fun `given auto save on when note opened then editor is not manual`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isManualSave)
    }

    @Test
    fun `given auto save setting not read yet when existing note opened then stays loading until it arrives`() = runTest {
        repository.notes[existing.id] = existing
        val settings = FakeSettingsRepository()
        val viewModel = buildViewModel(existing.id, settings = settings)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoading)

        settings.emitStoredAutoSaveEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isManualSave)
        assertEquals(existing.title, viewModel.state.value.title.text)
    }

    @Test
    fun `given auto save setting not read yet when new note opened then stays loading until it arrives`() = runTest {
        val settings = FakeSettingsRepository()
        settings.emitStoredDefaultNoteColor(NoteColor.MINT)
        val viewModel = buildViewModel(settings = settings)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoading)

        settings.emitStoredAutoSaveEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isManualSave)
        assertEquals(NoteColor.MINT, viewModel.state.value.color)
    }

    @Test
    fun `given no stored auto save setting when note opened then editor saves automatically`() = runTest {
        repository.notes[existing.id] = existing
        settingsRepository.emitStoredAutoSaveEnabled(null)
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isManualSave)
        assertTrue(repository.notes.getValue(existing.id).isPinned)
    }

    @Test
    fun `given manual editor open when setting turned on then it stays manual and next editor saves automatically`() =
        runTest {
            val manual = openManually()

            settingsRepository.emitStoredAutoSaveEnabled(true)
            advanceUntilIdle()
            manual.onIntent(NoteEditorIntent.PinClicked)
            advanceUntilIdle()

            assertTrue(manual.state.value.isManualSave)
            manual.assertPendingAndNothingStored(existing)

            val automatic = createViewModel(existing.id)
            advanceUntilIdle()
            automatic.onIntent(NoteEditorIntent.PinClicked)
            advanceUntilIdle()

            assertFalse(automatic.state.value.isManualSave)
            assertTrue(repository.notes.getValue(existing.id).isPinned)
        }

    @Test
    fun `given automatic editor open when setting turned off then it keeps saving automatically`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        settingsRepository.emitStoredAutoSaveEnabled(false)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isManualSave)
        assertTrue(repository.notes.getValue(existing.id).isPinned)
    }

    @Test
    fun `given auto save on when edited and back clicked then never shows unsaved status nor leave dialog`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.typeInTitle("!")
            assertFalse(viewModel.state.value.hasUnsavedChanges)
            assertTrue(viewModel.state.value.saveStatus != SaveStatus.Unsaved)
            viewModel.onIntent(NoteEditorIntent.BackClicked)

            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(existing.title + "!", repository.notes.getValue(existing.id).title)
    }

    @Test
    fun `given manual mode when title typed then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()

        viewModel.typeInTitle("!")
        advanceUntilIdle()

        viewModel.assertPendingAndNothingStored(existing)
        assertEquals(existing.title + "!", viewModel.state.value.title.text)
        assertEquals(dateFormatter.format(existing.updatedAt), viewModel.state.value.date)
    }

    @Test
    fun `given manual mode when content typed then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()
        viewModel.moveContentCursorToEnd()

        viewModel.typeInContent(" Sí")
        advanceUntilIdle()

        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when format applied to selection then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(0, 5))))

        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
        advanceUntilIdle()

        assertEquals(listOf(FormatSpan(0, 5, TextFormat.BOLD)), viewModel.state.value.contentSpans)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when checklist clicked then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()

        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        advanceUntilIdle()

        assertEquals(listOf(task(0)), viewModel.state.value.contentTasks)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when task toggled then nothing is saved and changes are pending`() = runTest {
        val withTask = existing.copy(contentTasks = listOf(task(0)))
        val viewModel = openManually(withTask)

        viewModel.onIntent(NoteEditorIntent.TaskToggled(0))
        advanceUntilIdle()

        assertEquals(listOf(task(0, isDone = true)), viewModel.state.value.contentTasks)
        viewModel.assertPendingAndNothingStored(withTask)
    }

    @Test
    fun `given manual mode when pin clicked then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isPinned)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when category clicked then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()

        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
        advanceUntilIdle()

        assertEquals(NoteCategory.WORK, viewModel.state.value.category)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when other color applied then nothing is saved and changes are pending`() = runTest {
        val viewModel = openManually()

        viewModel.applyColor(NoteColor.LAVENDER)
        advanceUntilIdle()

        assertEquals(NoteColor.LAVENDER, viewModel.state.value.color)
        assertEquals(null, viewModel.state.value.pickerColor)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given manual mode when images picked then copies them without associating and changes are pending`() = runTest {
        val withImage = existing.copy(images = listOf(imageA))
        val viewModel = openManually(withImage)

        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        assertEquals(listOf(URI_A), imageStorage.savedUris)
        assertEquals(listOf(imageA.path, copyOf(URI_A)), viewModel.state.value.images.map { it.path })
        assertTrue(imageStorage.deletedPaths.isEmpty())
        viewModel.assertPendingAndNothingStored(withImage)
    }

    @Test
    fun `given manual mode when saved image removed then hides it without deleting its file`() = runTest {
        val withImages = existing.copy(images = listOf(imageA, imageB))
        val viewModel = openManually(withImages)

        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        assertEquals(listOf(imageB), viewModel.state.value.images)
        assertTrue(imageStorage.deletedPaths.isEmpty())
        viewModel.assertPendingAndNothingStored(withImages)
    }

    @Test
    fun `given manual mode when pending image removed then deletes its copy and has no pending changes`() = runTest {
        val viewModel = openManually()
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(viewModel.state.value.images.single()))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.images.isEmpty())
        assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
        viewModel.assertNoPendingChanges()
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given some images cannot be copied in manual mode when picked then keeps the rest pending and shows image error`() =
        runTest {
            val viewModel = openManually()
            imageStorage.unavailableUris += URI_B

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A, URI_B)))
                assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error), awaitItem())
            }
            assertEquals(listOf(copyOf(URI_A)), viewModel.state.value.images.map { it.path })
            viewModel.assertPendingAndNothingStored(existing)
        }

    @Test
    fun `given no image can be copied in manual mode when picked then shows image error without pending changes`() =
        runTest {
            val viewModel = openManually()
            imageStorage.unavailableUris += URI_A

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
                assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_image_error), awaitItem())
            }
            assertTrue(viewModel.state.value.images.isEmpty())
            viewModel.assertNoPendingChanges()
        }

    @Test
    fun `given pin clicked twice in manual mode when back to saved state then has no pending changes`() = runTest {
        val viewModel = openManually()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        viewModel.assertNoPendingChanges()
        assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given typing undone in manual mode when back to saved state then has no pending changes`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        assertEquals(existing.title, viewModel.state.value.title.text)
        viewModel.assertNoPendingChanges()
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `given category chosen and removed in manual mode when back to saved state then has no pending changes`() =
        runTest {
            val viewModel = openManually()

            viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
            viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
            advanceUntilIdle()

            viewModel.assertNoPendingChanges()
        }

    @Test
    fun `given color changed and restored in manual mode when back to saved state then has no pending changes`() =
        runTest {
            val viewModel = openManually()

            viewModel.applyColor(NoteColor.LAVENDER)
            viewModel.applyColor(existing.color)
            advanceUntilIdle()

            viewModel.assertNoPendingChanges()
        }

    @Test
    fun `given text typed and deleted by hand in manual mode when back to saved state then has no pending changes`() =
        runTest {
            val viewModel = openManually()
            viewModel.typeInTitle("!")

            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue(existing.title, TextRange(existing.title.length))))
            advanceUntilIdle()

            viewModel.assertNoPendingChanges()
        }

    @Test
    fun `given blank new note with color applied in manual mode when back clicked then leaves without dialog nor saving`() =
        runTest {
            val viewModel = createManualViewModel()
            viewModel.applyColor(NoteColor.LAVENDER)
            assertFalse(viewModel.state.value.hasUnsavedChanges)

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.BackClicked)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            }
            assertFalse(viewModel.state.value.isLeaveDialogVisible)
            assertTrue(repository.notes.isEmpty())
        }

    @Test
    fun `given new note with blank title in manual mode when edited then has no pending changes`() = runTest {
        val viewModel = createManualViewModel(category = NoteCategory.WORK.name)

        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("   ")))
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        viewModel.assertNoPendingChanges()
        assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given new note with only images in manual mode when back clicked then shows leave dialog`() = runTest {
        val viewModel = createManualViewModel()
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.hasUnsavedChanges)

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(viewModel.state.value.isLeaveDialogVisible)
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given pending changes in manual mode when save clicked then stores them all with a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        val viewModel = openManually(old)
        viewModel.typeInTitle("!")
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent(" Sí")
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.PERSONAL))
        viewModel.applyColor(NoteColor.MINT)

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val expected = old.copy(
            title = old.title + "!",
            content = old.content + " Sí",
            isPinned = true,
            category = NoteCategory.PERSONAL,
            color = NoteColor.MINT,
            updatedAt = NOW,
        )
        assertEquals(expected, repository.notes[old.id])
        assertEquals(1, repository.saveCount)
        val state = viewModel.state.value
        assertEquals(dateFormatter.format(NOW), state.date)
        assertEquals(SaveStatus.Saved, state.saveStatus)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `given format and tasks pending in manual mode when save clicked then stores spans and tasks`() = runTest {
        val viewModel = openManually()
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(0, 5))))
        viewModel.onIntent(NoteEditorIntent.FormatSelected(TextFormat.UNDERLINE))
        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        viewModel.onIntent(NoteEditorIntent.TaskToggled(0))

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(listOf(FormatSpan(0, 5, TextFormat.UNDERLINE)), stored.contentSpans)
        assertEquals(listOf(task(0, isDone = true)), stored.contentTasks)
    }

    @Test
    fun `given saved changes in manual mode when save clicked again then button is disabled and nothing is saved`() =
        runTest {
            val viewModel = openManually()
            viewModel.typeInTitle("!")
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            advanceUntilIdle()

            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasUnsavedChanges)
            assertEquals(1, repository.saveCount)
        }

    @Test
    fun `given no changes in manual mode when save clicked then nothing is saved`() = runTest {
        val viewModel = openManually()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(0, repository.saveCount)
        assertEquals(SaveStatus.Idle, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given new note in manual mode when saved twice with edits then updates the same note`() = runTest {
        val viewModel = createManualViewModel(category = NoteCategory.WORK.name)
        viewModel.typeInTitle("Compra")
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        viewModel.typeInContent("Pan")
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val stored = repository.notes.values.single()
        assertEquals("Compra", stored.title)
        assertEquals("Pan", stored.content)
        assertEquals(NoteCategory.WORK, stored.category)
        assertEquals(NoteColor.DEFAULT, stored.color)
        assertEquals(NOW, stored.updatedAt)
    }

    @Test
    fun `given pending and removed images in manual mode when save clicked then associates and deletes them`() = runTest {
        val viewModel = openManually(existing.copy(images = listOf(imageA, imageB)))
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(listOf(imageB.path, copyOf(URI_A)), stored.images.map { it.path })
        assertEquals(NOW, stored.updatedAt)
        assertEquals(listOf(imageA.path), imageStorage.deletedPaths)
        assertEquals(stored.images, viewModel.state.value.images)
        viewModel.assertNoPendingChanges()
    }

    @Test
    fun `given saved changes in manual mode when saved then keeps undo history`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canUndo)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        advanceUntilIdle()

        assertEquals(existing.title, viewModel.state.value.title.text)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
        assertEquals(SaveStatus.Unsaved, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given saved in manual mode when pin toggled twice then status shows saved`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.PinClicked)
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        viewModel.assertNoPendingChanges()
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given save then undo back to opened note in manual mode when saved then restores its original date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        val viewModel = openManually(old)
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()
        assertEquals(NOW, repository.notes.getValue(old.id).updatedAt)

        viewModel.onIntent(NoteEditorIntent.UndoClicked)
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(old, repository.notes[old.id])
        assertEquals(dateFormatter.format(old.updatedAt), viewModel.state.value.date)
        viewModel.assertNoPendingChanges()
    }

    @Test
    fun `given save then text restored by hand to opened note in manual mode when saved then uses a new date`() = runTest {
        val old = existing.copy(updatedAt = NOW - TEN_DAYS_MS)
        val viewModel = openManually(old)
        viewModel.moveContentCursorToEnd()
        viewModel.typeInContent("!")
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(old.content, TextRange(old.content.length))))
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(old.copy(updatedAt = NOW), repository.notes[old.id])
    }

    @Test
    fun `given save failure in manual mode when save clicked then shows error and keeps changes pending`() = runTest {
        val viewModel = openManually(existing.copy(images = listOf(imageA)))
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()
        repository.failure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_save_error), awaitItem())
            expectNoEvents()
        }
        val state = viewModel.state.value
        assertTrue(state.hasUnsavedChanges)
        assertEquals(SaveStatus.Unsaved, state.saveStatus)
        assertEquals(existing.title + "!", state.title.text)
        assertEquals(listOf(copyOf(URI_A)), state.images.map { it.path })
        assertEquals(existing.copy(images = listOf(imageA)), repository.notes[existing.id])
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given failed save in manual mode when save clicked again then stores the pending changes`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        repository.failure = NoteError.Storage
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()
        repository.failure = null

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(existing.title + "!", repository.notes.getValue(existing.id).title)
        assertEquals(SaveStatus.Saved, viewModel.state.value.saveStatus)
        viewModel.assertNoPendingChanges()
    }

    @Test
    fun `given existing note emptied in manual mode when save clicked then neither saves nor warns`() = runTest {
        val viewModel = openManually()
        viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("")))
        viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("")))

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(existing, repository.notes[existing.id])
        assertEquals(SaveStatus.Unsaved, viewModel.state.value.saveStatus)
    }

    @Test
    fun `given existing note emptied in manual mode when saved from leave dialog then leaves keeping last version`() =
        runTest {
            val viewModel = openManually()
            viewModel.onIntent(NoteEditorIntent.TitleChanged(TextFieldValue("")))
            viewModel.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue("")))
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            runCurrent()
            assertTrue(viewModel.state.value.isLeaveDialogVisible)

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.LeaveSaveClicked)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
                expectNoEvents()
            }
            assertEquals(existing, repository.notes[existing.id])
        }

    @Test
    fun `given pending changes in manual mode when back clicked then shows leave dialog without navigating`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(viewModel.state.value.isLeaveDialogVisible)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given no pending changes in manual mode when back clicked then navigates back without dialog`() = runTest {
        val viewModel = openManually()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `given leave dialog when save chosen then stores changes closes dialog and navigates back`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveSaveClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(existing.copy(title = existing.title + "!", updatedAt = NOW), repository.notes[existing.id])
    }

    @Test
    fun `given leave dialog and save failure when save chosen then shows error and stays with pending changes`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()
        repository.failure = NoteError.Storage

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveSaveClicked)
            assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_save_error), awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
        assertEquals(existing.title + "!", viewModel.state.value.title.text)
        assertEquals(existing, repository.notes[existing.id])
    }

    @Test
    fun `given failed save from leave dialog when back clicked again then dialog shows again`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()
        repository.failure = NoteError.Storage
        viewModel.onIntent(NoteEditorIntent.LeaveSaveClicked)
        advanceUntilIdle()

        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        assertTrue(viewModel.state.value.isLeaveDialogVisible)
    }

    @Test
    fun `given leave dialog when discard chosen then navigates back leaving the note as last saved`() = runTest {
        val withImages = existing.copy(images = listOf(imageA, imageB))
        val viewModel = openManually(withImages)
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A, URI_B)))
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveDiscardClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(withImages, repository.notes[existing.id])
        assertEquals(0, repository.saveCount)
        assertEquals(setOf(copyOf(URI_A), copyOf(URI_B)), imageStorage.deletedPaths.toSet())
        assertFalse(imageA.path in imageStorage.deletedPaths)
    }

    @Test
    fun `given new note with text when discard chosen then the note is not created`() = runTest {
        val viewModel = createManualViewModel()
        viewModel.typeInTitle("Compra")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveDiscardClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
        }
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given leave dialog when dismissed then closes it and stays with pending changes`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveDialogDismissed)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isLeaveDialogVisible)
        assertEquals(existing.title + "!", viewModel.state.value.title.text)
        viewModel.assertPendingAndNothingStored(existing)
    }

    @Test
    fun `given pending changes in manual mode when delete confirmed then deletes note and discards copies without dialog`() =
        runTest {
            val viewModel = openManually(existing.copy(images = listOf(imageA)))
            viewModel.typeInTitle("!")
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.DeleteClicked)
                viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            }
            assertFalse(viewModel.state.value.isLeaveDialogVisible)
            assertFalse(viewModel.state.value.isDeleteDialogVisible)
            assertTrue(repository.notes.isEmpty())
            assertEquals(0, repository.saveCount)
            assertTrue(copyOf(URI_A) in imageStorage.deletedPaths)
            assertTrue(imageA.path in imageStorage.deletedPaths)
        }

    @Test
    fun `given new note with pending images in manual mode when delete confirmed then discards copies and stores nothing`() =
        runTest {
            val viewModel = createManualViewModel()
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            }
            assertTrue(repository.notes.isEmpty())
            assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
        }

    @Test
    fun `given orphan image file when leaving without changes in manual mode then deletes it before navigating back`() =
        runTest {
            val viewModel = openManually()
            imageStorage.save(URI_C)

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.BackClicked)
                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
                assertEquals(listOf(copyOf(URI_C)), imageStorage.deletedPaths)
            }
        }

    @Test
    fun `given orphan image file when leaving with discard then deletes it before navigating back`() = runTest {
        val viewModel = openManually()
        imageStorage.save(URI_C)
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveDiscardClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            assertEquals(listOf(copyOf(URI_C)), imageStorage.deletedPaths)
        }
    }

    @Test
    fun `given orphan image file when leaving after saving from dialog then deletes it but keeps saved images`() = runTest {
        val viewModel = openManually()
        imageStorage.save(URI_C)
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.LeaveSaveClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            assertEquals(listOf(copyOf(URI_C)), imageStorage.deletedPaths)
        }
        assertEquals(listOf(copyOf(URI_A)), repository.notes.getValue(existing.id).images.map { it.path })
    }

    @Test
    fun `given orphan image file when note deleted in manual mode then deletes it before navigating back`() = runTest {
        val viewModel = openManually()
        imageStorage.save(URI_C)

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            assertEquals(listOf(copyOf(URI_C)), imageStorage.deletedPaths)
        }
    }

    @Test
    fun `given orphan image file when leaving in automatic mode then deletes it before navigating back`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        imageStorage.save(URI_C)

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
            assertEquals(listOf(copyOf(URI_C)), imageStorage.deletedPaths)
        }
    }

    @Test
    fun `given pending changes in manual mode when edited then keeps a draft in saved state`() = runTest {
        val handle = savedStateHandle(existing.id)
        val viewModel = openManually(existing.copy(images = listOf(imageA, imageB)), handle)

        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        viewModel.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
        viewModel.applyColor(NoteColor.MINT)
        viewModel.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        assertEquals(
            EditorDraft(
                title = existing.title + "!",
                content = existing.content,
                contentSpans = emptyList(),
                contentTasks = listOf(task(0)),
                isPinned = true,
                category = NoteCategory.WORK,
                color = NoteColor.MINT,
                pendingImagePaths = listOf(copyOf(URI_A)),
                removedImageIds = listOf(imageA.id),
            ),
            EditorDraftCodec.read(handle),
        )
    }

    @Test
    fun `given draft in saved state when editor recreated then shows recovered changes still pending`() = runTest {
        val withImages = existing.copy(images = listOf(imageA, imageB))
        val handle = savedStateHandle(existing.id)
        val first = openManually(withImages, handle)
        first.typeInTitle("!")
        first.onIntent(NoteEditorIntent.PinClicked)
        first.onIntent(NoteEditorIntent.CategoryClicked(NoteCategory.WORK))
        first.applyColor(NoteColor.MINT)
        first.onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(existing.content, TextRange(0, 5))))
        first.onIntent(NoteEditorIntent.FormatSelected(TextFormat.BOLD))
        first.onIntent(NoteEditorIntent.ChecklistButtonClicked)
        first.onIntent(NoteEditorIntent.TaskToggled(0))
        first.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        first.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()

        val recreated = createManualViewModel(existing.id, savedStateHandle = handle)

        val state = recreated.state.value
        assertEquals(existing.title + "!", state.title.text)
        assertEquals(existing.content, state.content.text)
        assertEquals(listOf(FormatSpan(0, 5, TextFormat.BOLD)), state.contentSpans)
        assertEquals(listOf(task(0, isDone = true)), state.contentTasks)
        assertTrue(state.isPinned)
        assertEquals(NoteCategory.WORK, state.category)
        assertEquals(NoteColor.MINT, state.color)
        assertEquals(listOf(imageB.path, copyOf(URI_A)), state.images.map { it.path })
        assertTrue(state.hasUnsavedChanges)
        assertEquals(SaveStatus.Unsaved, state.saveStatus)
        assertFalse(state.canUndo)
        assertEquals(withImages, repository.notes[existing.id])
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `given recovered draft when save clicked then stores recovered changes and images`() = runTest {
        val handle = savedStateHandle(existing.id)
        val first = openManually(existing.copy(images = listOf(imageA, imageB)), handle)
        first.typeInTitle("!")
        first.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        first.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()
        val recreated = createManualViewModel(existing.id, savedStateHandle = handle)

        recreated.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(existing.title + "!", stored.title)
        assertEquals(listOf(imageB.path, copyOf(URI_A)), stored.images.map { it.path })
        assertEquals(listOf(imageA.path), imageStorage.deletedPaths)
        recreated.assertNoPendingChanges()
        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given recovered draft when leaving then asks to save the recovered changes`() = runTest {
        val handle = savedStateHandle(existing.id)
        openManually(savedStateHandle = handle).typeInTitle("!")
        val recreated = createManualViewModel(existing.id, savedStateHandle = handle)

        recreated.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        assertTrue(recreated.state.value.isLeaveDialogVisible)
    }

    @Test
    fun `given recovered pending image when removed then deletes its copy`() = runTest {
        val handle = savedStateHandle(existing.id)
        val first = openManually(savedStateHandle = handle)
        first.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        advanceUntilIdle()
        val recreated = createManualViewModel(existing.id, savedStateHandle = handle)

        recreated.onIntent(NoteEditorIntent.RemoveImageClicked(recreated.state.value.images.single()))
        advanceUntilIdle()

        assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
        recreated.assertNoPendingChanges()
    }

    @Test
    fun `given draft of a new note when editor recreated then recovers it without creating the note`() = runTest {
        val handle = savedStateHandle(category = NoteCategory.PERSONAL.name)
        val first = createManualViewModel(category = NoteCategory.PERSONAL.name, savedStateHandle = handle)
        first.typeInTitle("Compra")
        first.typeInContent("Pan")

        val recreated = createManualViewModel(category = NoteCategory.PERSONAL.name, savedStateHandle = handle)

        val state = recreated.state.value
        assertEquals("Compra", state.title.text)
        assertEquals("Pan", state.content.text)
        assertEquals(NoteCategory.PERSONAL, state.category)
        assertTrue(state.hasUnsavedChanges)
        assertTrue(repository.notes.isEmpty())
    }

    @Test
    fun `given no draft in saved state when editor recreated then shows the note as saved`() = runTest {
        val handle = savedStateHandle(existing.id)
        openManually(savedStateHandle = handle)
        val fresh = createManualViewModel(existing.id).state.value

        val recreated = createManualViewModel(existing.id, savedStateHandle = handle)

        assertEquals(fresh, recreated.state.value)
        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given changes back to saved state when checked then draft is removed`() = runTest {
        val handle = savedStateHandle(existing.id)
        val viewModel = openManually(savedStateHandle = handle)
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        assertNotNull(EditorDraftCodec.read(handle))

        viewModel.onIntent(NoteEditorIntent.PinClicked)

        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given pending changes when saved then draft is removed`() = runTest {
        val handle = savedStateHandle(existing.id)
        val viewModel = openManually(savedStateHandle = handle)
        viewModel.typeInTitle("!")

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given pending changes when save fails then draft is kept`() = runTest {
        val handle = savedStateHandle(existing.id)
        val viewModel = openManually(savedStateHandle = handle)
        viewModel.typeInTitle("!")
        repository.failure = NoteError.Storage

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(existing.title + "!", EditorDraftCodec.read(handle)?.title)
    }

    @Test
    fun `given pending changes when discarded then draft is removed`() = runTest {
        val handle = savedStateHandle(existing.id)
        val viewModel = openManually(savedStateHandle = handle)
        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.BackClicked)
        runCurrent()

        viewModel.onIntent(NoteEditorIntent.LeaveDiscardClicked)
        advanceUntilIdle()

        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given automatic mode when edited then no draft is kept`() = runTest {
        repository.notes[existing.id] = existing
        val handle = savedStateHandle(existing.id)
        val viewModel = createViewModel(existing.id, savedStateHandle = handle)
        advanceUntilIdle()

        viewModel.typeInTitle("!")
        viewModel.onIntent(NoteEditorIntent.PinClicked)
        advanceUntilIdle()

        assertNull(EditorDraftCodec.read(handle))
    }

    @Test
    fun `given draft in saved state when setting turned on before recreation then ignores the draft`() = runTest {
        val handle = savedStateHandle(existing.id)
        openManually(savedStateHandle = handle).typeInTitle("!")
        settingsRepository.emitStoredAutoSaveEnabled(true)

        val recreated = createViewModel(existing.id, savedStateHandle = handle)
        advanceUntilIdle()

        assertFalse(recreated.state.value.isManualSave)
        assertEquals(existing.title, recreated.state.value.title.text)
        assertFalse(recreated.state.value.hasUnsavedChanges)
    }

    @Test
    fun `given image added while manual save is in progress when save ends then it stays pending and visible`() =
        runTest {
            val handle = savedStateHandle(existing.id)
            val viewModel = openManually(existing.copy(images = listOf(imageA)), handle)
            viewModel.typeInTitle("!")
            val gate = CompletableDeferred<Unit>()
            repository.saveChangesGate = gate
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            runCurrent()

            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            val stored = repository.notes.getValue(existing.id)
            assertEquals(existing.title + "!", stored.title)
            assertEquals(listOf(imageA), stored.images)
            val state = viewModel.state.value
            assertEquals(listOf(imageA.path, copyOf(URI_A)), state.images.map { it.path })
            assertTrue(state.hasUnsavedChanges)
            assertEquals(SaveStatus.Unsaved, state.saveStatus)
            assertEquals(listOf(copyOf(URI_A)), EditorDraftCodec.read(handle)?.pendingImagePaths)
            assertTrue(imageStorage.deletedPaths.isEmpty())
        }

    @Test
    fun `given saved image removed while manual save is in progress when save ends then removal stays pending`() =
        runTest {
            val handle = savedStateHandle(existing.id)
            val viewModel = openManually(existing.copy(images = listOf(imageA, imageB)), handle)
            viewModel.typeInTitle("!")
            val gate = CompletableDeferred<Unit>()
            repository.saveChangesGate = gate
            viewModel.onIntent(NoteEditorIntent.SaveClicked)
            runCurrent()

            viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageB))
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf(imageA, imageB), repository.notes.getValue(existing.id).images)
            val state = viewModel.state.value
            assertEquals(listOf(imageA), state.images)
            assertTrue(state.hasUnsavedChanges)
            assertEquals(SaveStatus.Unsaved, state.saveStatus)
            assertEquals(listOf(imageB.id), EditorDraftCodec.read(handle)?.removedImageIds)
            assertTrue(imageStorage.deletedPaths.isEmpty())
        }

    @Test
    fun `given image changes made while manual save is in progress when saved again then stores them`() = runTest {
        val viewModel = openManually(existing.copy(images = listOf(imageA, imageB)))
        viewModel.typeInTitle("!")
        val gate = CompletableDeferred<Unit>()
        repository.saveChangesGate = gate
        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        runCurrent()
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageB))
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        repository.saveChangesGate = null

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        val stored = repository.notes.getValue(existing.id)
        assertEquals(listOf(imageA.path, copyOf(URI_A)), stored.images.map { it.path })
        assertEquals(listOf(imageB.path), imageStorage.deletedPaths)
        assertEquals(stored.images, viewModel.state.value.images)
        viewModel.assertNoPendingChanges()
    }

    @Test
    fun `given delete failure in manual mode when delete confirmed then keeps copies images pending changes and draft`() =
        runTest {
            val handle = savedStateHandle(existing.id)
            val withImages = existing.copy(images = listOf(imageA, imageB))
            val viewModel = openManually(withImages, handle)
            viewModel.typeInTitle("!")
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageB))
            advanceUntilIdle()
            val draft = EditorDraftCodec.read(handle)
            repository.failure = NoteError.Storage

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.DeleteClicked)
                viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
                assertEquals(NoteEditorEffect.ShowMessage(R.string.note_editor_delete_error), awaitItem())
                expectNoEvents()
            }

            assertTrue(imageStorage.deletedPaths.isEmpty())
            val state = viewModel.state.value
            assertEquals(listOf(imageA.path, copyOf(URI_A)), state.images.map { it.path })
            assertTrue(state.hasUnsavedChanges)
            assertEquals(SaveStatus.Unsaved, state.saveStatus)
            assertNotNull(draft)
            assertEquals(draft, EditorDraftCodec.read(handle))
            assertEquals(withImages, repository.notes[existing.id])
        }

    @Test
    fun `given failed delete in manual mode when save clicked then stores the pending image changes`() = runTest {
        val viewModel = openManually(existing.copy(images = listOf(imageA, imageB)))
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageB))
        advanceUntilIdle()
        repository.failure = NoteError.Storage
        viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
        advanceUntilIdle()
        repository.failure = null

        viewModel.onIntent(NoteEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            listOf(imageA.path, copyOf(URI_A)),
            repository.notes.getValue(existing.id).images.map { it.path },
        )
        assertEquals(listOf(imageB.path), imageStorage.deletedPaths)
        viewModel.assertNoPendingChanges()
    }

    @Test
    fun `given image being copied without other changes in manual mode when back clicked then ends showing leave dialog`() =
        runTest {
            val viewModel = openManually()
            val gate = CompletableDeferred<Unit>()
            imageStorage.saveGate = gate
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            runCurrent()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.BackClicked)
                runCurrent()
                assertFalse(viewModel.state.value.isLeaveDialogVisible)

                gate.complete(Unit)
                advanceUntilIdle()

                assertTrue(viewModel.state.value.isLeaveDialogVisible)
                expectNoEvents()
            }
            assertTrue(viewModel.state.value.hasUnsavedChanges)
            assertTrue(imageStorage.deletedPaths.isEmpty())
        }

    @Test
    fun `given image being copied when discard chosen then waits for the copy and deletes it before navigating back`() =
        runTest {
            val viewModel = openManually()
            viewModel.typeInTitle("!")
            viewModel.onIntent(NoteEditorIntent.BackClicked)
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            imageStorage.saveGate = gate
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            runCurrent()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.LeaveDiscardClicked)
                runCurrent()
                expectNoEvents()

                gate.complete(Unit)

                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
                assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
            }
            assertEquals(existing, repository.notes[existing.id])
        }

    @Test
    fun `given image being copied in new manual note when delete confirmed then waits for the copy and deletes it`() =
        runTest {
            val viewModel = createManualViewModel()
            viewModel.typeInTitle("Viaje")
            val gate = CompletableDeferred<Unit>()
            imageStorage.saveGate = gate
            viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
            runCurrent()

            viewModel.effects.test {
                viewModel.onIntent(NoteEditorIntent.DeleteConfirmed)
                runCurrent()
                expectNoEvents()

                gate.complete(Unit)

                assertEquals(NoteEditorEffect.NavigateBack, awaitItem())
                assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
            }
            assertTrue(repository.notes.isEmpty())
        }

    // --- Compartir (spec 010) ---

    private fun shareMessage() = NoteEditorEffect.ShowMessage(R.string.note_editor_share_missing_images)

    @Test
    fun `given new empty note when created then cannot share`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.canShare)
    }

    @Test
    fun `given only spaces typed when editing then cannot share`() = runTest {
        val viewModel = createViewModel()

        viewModel.typeInTitle("  ")
        viewModel.typeInContent(" \n ")

        assertFalse(viewModel.state.value.canShare)
    }

    @Test
    fun `given title typed when editing then can share`() = runTest {
        val viewModel = createViewModel()

        viewModel.typeInTitle("Ideas")

        assertTrue(viewModel.state.value.canShare)
    }

    @Test
    fun `given content typed when editing then can share`() = runTest {
        val viewModel = createViewModel()

        viewModel.typeInContent("Libreta")

        assertTrue(viewModel.state.value.canShare)
    }

    @Test
    fun `given note with only images when opened then can share`() = runTest {
        repository.notes[existing.id] = existing.copy(title = "", content = "", images = listOf(imageA))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canShare)
    }

    @Test
    fun `given existing note when share clicked then emits share with title content and images`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA, imageB))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(
                NoteEditorEffect.ShareNote(
                    subject = existing.title,
                    text = "${existing.title}\n\n${existing.content}",
                    imagePaths = listOf(imageA.path, imageB.path),
                ),
                awaitItem(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `given note with tasks when share clicked then text marks done and pending tasks`() = runTest {
        repository.notes[existing.id] = existing.copy(
            content = "Comprar pan\nLlamar a Ana",
            contentTasks = listOf(task(0, isDone = true), task(1)),
        )
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            val share = awaitItem() as NoteEditorEffect.ShareNote
            assertEquals("${existing.title}\n\n☑ Comprar pan\n☐ Llamar a Ana", share.text)
        }
    }

    @Test
    fun `given unsaved typing in automatic mode when share clicked then shares visible text without saving`() =
        runTest {
            repository.notes[existing.id] = existing
            val viewModel = createViewModel(existing.id)
            advanceUntilIdle()
            viewModel.typeInTitle("!")

            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            runCurrent()

            assertEquals(0, repository.saveCount)
            assertEquals(existing, repository.notes[existing.id])
            viewModel.effects.test {
                val share = awaitItem() as NoteEditorEffect.ShareNote
                assertEquals(existing.title + "!", share.subject)
                assertEquals("${existing.title}!\n\n${existing.content}", share.text)
            }
        }

    @Test
    fun `given removed image in automatic mode when share clicked then shares only the visible images`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA, imageB))
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(listOf(imageB.path), (awaitItem() as NoteEditorEffect.ShareNote).imagePaths)
        }
    }

    @Test
    fun `given saved note in automatic mode when share clicked then nothing is saved nor its date changed`() = runTest {
        repository.notes[existing.id] = existing
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()
        val stateBefore = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            awaitItem()
        }
        advanceUntilIdle()

        assertEquals(0, repository.saveCount)
        assertEquals(existing, repository.notes[existing.id])
        assertEquals(stateBefore, viewModel.state.value)
    }

    @Test
    fun `given pending changes in manual mode when share clicked then shares them and keeps them pending`() = runTest {
        val viewModel = openManually()
        viewModel.typeInTitle("!")
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            val share = awaitItem() as NoteEditorEffect.ShareNote
            assertEquals(existing.title + "!", share.subject)
            assertEquals("${existing.title}!\n\n${existing.content}", share.text)
        }
        advanceUntilIdle()

        viewModel.assertPendingAndNothingStored(existing)
        assertEquals(dateFormatter.format(existing.updatedAt), viewModel.state.value.date)
    }

    @Test
    fun `given pending and removed images in manual mode when share clicked then shares the visible ones`() = runTest {
        val withImages = existing.copy(images = listOf(imageA, imageB))
        val viewModel = openManually(withImages)
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(listOf(URI_A)))
        viewModel.onIntent(NoteEditorIntent.RemoveImageClicked(imageA))
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(
                listOf(imageB.path, copyOf(URI_A)),
                (awaitItem() as NoteEditorEffect.ShareNote).imagePaths,
            )
        }
        advanceUntilIdle()

        viewModel.assertPendingAndNothingStored(withImages)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given some images missing when share clicked then shares the rest and then warns`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA, imageB))
        imageStorage.missingPaths += imageA.path
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(
                NoteEditorEffect.ShareNote(
                    subject = existing.title,
                    text = "${existing.title}\n\n${existing.content}",
                    imagePaths = listOf(imageB.path),
                ),
                awaitItem(),
            )
            assertEquals(shareMessage(), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given only images and all missing when share clicked then only warns`() = runTest {
        repository.notes[existing.id] = existing.copy(title = "", content = "", images = listOf(imageA, imageB))
        imageStorage.missingPaths += listOf(imageA.path, imageB.path)
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(shareMessage(), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given text and all images missing when share clicked then shares text and warns`() = runTest {
        repository.notes[existing.id] = existing.copy(images = listOf(imageA))
        imageStorage.missingPaths += imageA.path
        val viewModel = createViewModel(existing.id)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)

            assertEquals(emptyList<String>(), (awaitItem() as NoteEditorEffect.ShareNote).imagePaths)
            assertEquals(shareMessage(), awaitItem())
        }
    }

    @Test
    fun `given empty note when share clicked then nothing is emitted`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `given only spaces when share clicked then nothing is emitted`() = runTest {
        val viewModel = createViewModel()
        viewModel.typeInTitle("   ")

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `given note still loading with text when share clicked then nothing is emitted`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.typeInTitle("Ideas")
        assertTrue(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.canShare)

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `given load error when share clicked then nothing is emitted`() = runTest {
        val viewModel = createViewModel(noteId = 99)
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onIntent(NoteEditorIntent.ShareClicked)
            runCurrent()

            expectNoEvents()
        }
    }

    /** Escribe [text] en el contenido letra a letra, con el cursor al final, como un teclado. */
    private fun NoteEditorViewModel.typeInContent(text: String) {
        text.forEach { char ->
            val typed = state.value.content.text + char
            onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(typed, TextRange(typed.length))))
        }
    }

    /** Escribe [text] en el título letra a letra, con el cursor al final, como un teclado. */
    private fun NoteEditorViewModel.typeInTitle(text: String) {
        text.forEach { char ->
            val typed = state.value.title.text + char
            onIntent(NoteEditorIntent.TitleChanged(TextFieldValue(typed, TextRange(typed.length))))
        }
    }

    /** Lleva el cursor del contenido al final sin cambiar el texto. */
    private fun NoteEditorViewModel.moveContentCursorToEnd() {
        val text = state.value.content.text
        onIntent(NoteEditorIntent.ContentChanged(TextFieldValue(text, TextRange(text.length))))
    }

    private fun task(line: Int, isDone: Boolean = false) = ChecklistItem(line, isDone)

    private class InMemoryNoteRepository : NoteRepository {
        val notes = linkedMapOf<Long, Note>()
        var failure: NoteError? = null
        var addImagesFailure: NoteError? = null
        var removeImageFailure: NoteError? = null
        /** Si no es nulo, `saveNoteChanges` espera a que se complete: simula un guardado en curso. */
        var saveChangesGate: CompletableDeferred<Unit>? = null
        var saveCount = 0
            private set
        private var nextId = 1L
        private var nextImageId = 100L

        override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = emptyFlow()

        override suspend fun getNote(id: Long): AppResult<Note, NoteError> =
            notes[id]?.let { AppResult.Success(it) } ?: AppResult.Error(NoteError.NotFound)

        override suspend fun saveNote(note: Note): AppResult<Long, NoteError> {
            saveCount++
            failure?.let { return AppResult.Error(it) }
            val id = if (note.id == Note.NEW_ID) nextId++ else note.id
            // Como la implementación real, guardar la nota no toca sus imágenes.
            notes[id] = note.copy(id = id, images = notes[id]?.images.orEmpty())
            return AppResult.Success(id)
        }

        override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> {
            failure?.let { return AppResult.Error(it) }
            notes.remove(id)
            return AppResult.Success(Unit)
        }

        override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> {
            (failure ?: addImagesFailure)?.let { return AppResult.Error(it) }
            val note = notes[noteId] ?: return AppResult.Error(NoteError.NotFound)
            val added = paths.map { NoteImage(id = nextImageId++, path = it) }
            notes[noteId] = note.copy(images = note.images + added)
            return AppResult.Success(added)
        }

        override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> {
            (failure ?: removeImageFailure)?.let { return AppResult.Error(it) }
            notes.replaceAll { _, note -> note.copy(images = note.images.filterNot { it.id == imageId }) }
            return AppResult.Success(Unit)
        }

        override suspend fun saveNoteChanges(
            note: Note,
            addedPaths: List<String>,
            removedImageIds: List<Long>,
        ): AppResult<Note, NoteError> {
            saveCount++
            saveChangesGate?.await()
            failure?.let { return AppResult.Error(it) }
            if (note.id != Note.NEW_ID && note.id !in notes) return AppResult.Error(NoteError.NotFound)
            val id = if (note.id == Note.NEW_ID) nextId++ else note.id
            // Como la implementación real, todo o nada: imágenes guardadas menos las quitadas, más las nuevas.
            val images = notes[id]?.images.orEmpty().filterNot { it.id in removedImageIds } +
                addedPaths.map { NoteImage(id = nextImageId++, path = it) }
            return AppResult.Success(note.copy(id = id, images = images).also { notes[id] = it })
        }

        override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> {
            failure?.let { return AppResult.Error(it) }
            return AppResult.Success(notes.values.flatMap { note -> note.images.map { it.path } }.toSet())
        }
    }

    private class FakeImageStorage : ImageStorage {
        val unavailableUris = mutableSetOf<String>()
        val savedUris = mutableListOf<String>()
        val deletedPaths = mutableListOf<String>()
        /** Si no es nulo, cada copia espera a que se complete: simula una copia en curso. */
        var saveGate: CompletableDeferred<Unit>? = null

        override suspend fun save(sourceUri: String): AppResult<String, NoteError> {
            saveGate?.await()
            if (sourceUri in unavailableUris) return AppResult.Error(NoteError.ImageUnavailable)
            savedUris += sourceUri
            return AppResult.Success(copyOf(sourceUri))
        }

        override suspend fun delete(path: String) {
            deletedPaths += path
        }

        override suspend fun listPaths(): Set<String> = savedUris.map(::copyOf).toSet() - deletedPaths.toSet()

        /** Rutas cuyo archivo ya no existe, aunque la nota las siga mostrando. */
        val missingPaths = mutableSetOf<String>()

        override suspend fun exists(path: String): Boolean = path !in missingPaths && path !in deletedPaths
    }

    private val imageA = NoteImage(id = 1, path = "/files/note_images/a.jpg")
    private val imageB = NoteImage(id = 2, path = "/files/note_images/b.jpg")

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val TEN_DAYS_MS = 10 * 24 * 60 * 60 * 1_000L
        /** Intervalo entre letras de un tecleo continuo: menor que la pausa que cierra el paso. */
        const val LETTER_INTERVAL_MS = 300L
        const val URI_A = "content://media/picker/a"
        const val URI_B = "content://media/picker/b"
        const val URI_C = "content://media/picker/c"

        fun copyOf(uri: String) = "/files/note_images/copy-${uri.substringAfterLast('/')}.jpg"
    }
}
