package com.example.notesapp.presentation.notes.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.AddNoteImagesUseCase
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.GetNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.RemoveNoteImageUseCase
import com.example.notesapp.domain.notes.SaveNoteUseCase
import com.example.notesapp.domain.notes.TextFormat
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val repository = InMemoryNoteRepository()
    private val imageStorage = FakeImageStorage()
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

    private fun createViewModel(noteId: Long? = null, category: String? = null) = NoteEditorViewModel(
        savedStateHandle = SavedStateHandle(
            buildMap {
                noteId?.let { put(NoteEditorViewModel.NOTE_ID_ARG, it) }
                category?.let { put(NoteEditorViewModel.CATEGORY_ARG, it) }
            },
        ),
        getNote = GetNoteUseCase(repository),
        saveNote = SaveNoteUseCase(repository, TimeProvider { NOW }),
        deleteNote = DeleteNoteUseCase(repository, imageStorage),
        addNoteImages = AddNoteImagesUseCase(repository, imageStorage, TimeProvider { NOW }),
        removeNoteImage = RemoveNoteImageUseCase(repository, imageStorage),
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

    @Test
    fun `given category argument when creating new note then starts with that category`() = runTest {
        val viewModel = createViewModel(category = NoteCategory.WORK.name)
        advanceUntilIdle()

        assertEquals(NoteEditorUiState(category = NoteCategory.WORK), viewModel.state.value)
    }

    @Test
    fun `given category argument when new note is autosaved then stores that category`() = runTest {
        val viewModel = createViewModel(category = NoteCategory.PERSONAL.name)

        viewModel.onIntent(NoteEditorIntent.TitleChanged("La compra"))
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
        viewModel.onIntent(NoteEditorIntent.TitleChanged("Escapada"))

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

        viewModel.onIntent(NoteEditorIntent.TitleChanged("Escapada"))
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
            viewModel.onIntent(NoteEditorIntent.TitleChanged("Escapada"))
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

        viewModel.onIntent(NoteEditorIntent.TitleChanged("Recetas"))
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

    private fun task(line: Int, isDone: Boolean = false) = ChecklistItem(line, isDone)

    private class InMemoryNoteRepository : NoteRepository {
        val notes = linkedMapOf<Long, Note>()
        var failure: NoteError? = null
        var addImagesFailure: NoteError? = null
        var removeImageFailure: NoteError? = null
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
    }

    private class FakeImageStorage : ImageStorage {
        val unavailableUris = mutableSetOf<String>()
        val savedUris = mutableListOf<String>()
        val deletedPaths = mutableListOf<String>()

        override suspend fun save(sourceUri: String): AppResult<String, NoteError> {
            if (sourceUri in unavailableUris) return AppResult.Error(NoteError.ImageUnavailable)
            savedUris += sourceUri
            return AppResult.Success(copyOf(sourceUri))
        }

        override suspend fun delete(path: String) {
            deletedPaths += path
        }
    }

    private val imageA = NoteImage(id = 1, path = "/files/note_images/a.jpg")
    private val imageB = NoteImage(id = 2, path = "/files/note_images/b.jpg")

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val TEN_DAYS_MS = 10 * 24 * 60 * 60 * 1_000L
        const val URI_A = "content://media/picker/a"
        const val URI_B = "content://media/picker/b"

        fun copyOf(uri: String) = "/files/note_images/copy-${uri.substringAfterLast('/')}.jpg"
    }
}
