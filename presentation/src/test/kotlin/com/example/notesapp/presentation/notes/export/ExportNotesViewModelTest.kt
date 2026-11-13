package com.example.notesapp.presentation.notes.export

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.GetNotesUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.export.DiscardExportDestinationUseCase
import com.example.notesapp.domain.notes.export.DiscardNotesExportUseCase
import com.example.notesapp.domain.notes.export.ExportError
import com.example.notesapp.domain.notes.export.GetExportArchiveNameUseCase
import com.example.notesapp.domain.notes.export.PrepareNotesExportUseCase
import com.example.notesapp.domain.notes.export.SaveNotesExportUseCase
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.R
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExportNotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()
    private val exporter = FakeNotesExporter()

    private fun createViewModel() = ExportNotesViewModel(
        getNotes = GetNotesUseCase(repository, mainDispatcherRule.testDispatcher),
        prepareExport = PrepareNotesExportUseCase(exporter),
        saveExport = SaveNotesExportUseCase(exporter),
        discardExport = DiscardNotesExportUseCase(exporter),
        discardDestination = DiscardExportDestinationUseCase(exporter),
        getArchiveName = GetExportArchiveNameUseCase(TimeProvider { NOW }),
    )

    private fun note(
        id: Long,
        title: String = "Nota $id",
        isPinned: Boolean = false,
        updatedAt: Long = NOW - id,
    ) = Note(
        id = id,
        title = title,
        content = "Contenido $id",
        color = NoteColor.NONE,
        isPinned = isPinned,
        category = null,
        updatedAt = updatedAt,
    )

    /** ViewModel con [notes] cargadas y [selected] seleccionadas. */
    private suspend fun loadedViewModel(
        notes: List<Note> = listOf(note(1), note(2), note(3)),
        selected: List<Long> = emptyList(),
    ): ExportNotesViewModel {
        repository.emitNotes(*notes.toTypedArray())
        return createViewModel().apply {
            selected.forEach { onIntent(ExportNotesIntent.NoteToggled(it)) }
        }
    }

    /** ViewModel con el .zip ya preparado y el selector de documentos abierto. */
    private suspend fun pickerOpenViewModel(selected: List<Long> = listOf(1, 2)): ExportNotesViewModel =
        loadedViewModel(selected = selected).apply { onIntent(ExportNotesIntent.ExportClicked) }

    @Test
    fun `given no emission yet when created then state is loading`() {
        val viewModel = createViewModel()

        assertEquals(ExportNotesUiState(isLoading = true), viewModel.state.value)
    }

    @Test
    fun `given notes when loaded then lists them pinned first then most recent with trimmed titles`() = runTest {
        repository.emitNotes(
            note(id = 1, title = "  Antigua  ", updatedAt = 100),
            note(id = 2, title = "Fijada", isPinned = true, updatedAt = 50),
            note(id = 3, title = "Reciente ", updatedAt = 300),
            note(id = 4, title = "   ", updatedAt = 200),
        )

        val viewModel = createViewModel()

        assertEquals(
            ExportNotesUiState(
                isLoading = false,
                notes = listOf(
                    ExportNoteItem(id = 2, title = "Fijada"),
                    ExportNoteItem(id = 3, title = "Reciente"),
                    ExportNoteItem(id = 4, title = ""),
                    ExportNoteItem(id = 1, title = "Antigua"),
                ),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `given notes when loaded then none is selected`() = runTest {
        val viewModel = loadedViewModel()

        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertFalse(viewModel.state.value.canExport)
    }

    @Test
    fun `given no notes when loaded then state is empty`() = runTest {
        val viewModel = loadedViewModel(notes = emptyList())

        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `given storage error when loading then state reports load failure instead of empty`() = runTest {
        repository.emit(AppResult.Error(NoteError.Storage))

        val viewModel = createViewModel()

        val state = viewModel.state.value
        assertTrue(state.loadFailed)
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertTrue(state.notes.isEmpty())
    }

    @Test
    fun `given notes when loading then load failure is not reported`() = runTest {
        val viewModel = loadedViewModel()

        assertFalse(viewModel.state.value.loadFailed)
    }

    @Test
    fun `given unselected note when toggled then becomes selected`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onIntent(ExportNotesIntent.NoteToggled(2))

        assertEquals(setOf(2L), viewModel.state.value.selectedIds)
        assertEquals(1, viewModel.state.value.selectedCount)
    }

    @Test
    fun `given selected note when toggled then becomes unselected`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1, 2))

        viewModel.onIntent(ExportNotesIntent.NoteToggled(1))

        assertEquals(setOf(2L), viewModel.state.value.selectedIds)
    }

    @Test
    fun `given some selected when select all clicked then every note is selected`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(2))

        viewModel.onIntent(ExportNotesIntent.SelectAllClicked)

        assertEquals(setOf(1L, 2L, 3L), viewModel.state.value.selectedIds)
        assertTrue(viewModel.state.value.allSelected)
    }

    @Test
    fun `given all selected when clear selection clicked then nothing is selected`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1, 2, 3))

        viewModel.onIntent(ExportNotesIntent.ClearSelectionClicked)

        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertFalse(viewModel.state.value.allSelected)
    }

    @Test
    fun `given selected note when it disappears from the list then it is no longer selected`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1, 2))

        repository.emitNotes(note(1), note(3))

        assertEquals(setOf(1L), viewModel.state.value.selectedIds)
        assertEquals(listOf(1L, 3L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given selection when export clicked then state is exporting`() = runTest {
        exporter.prepareGate = CompletableDeferred()
        val viewModel = loadedViewModel(selected = listOf(1))

        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        assertTrue(viewModel.state.value.isExporting)
        assertFalse(viewModel.state.value.canExport)
    }

    @Test
    fun `given selection when export clicked then prepares only selected notes in list order`() = runTest {
        val notes = listOf(note(1), note(2), note(3))
        val viewModel = loadedViewModel(notes = notes, selected = listOf(3, 1))

        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        assertEquals(listOf(listOf(notes[0], notes[2])), exporter.preparedNotes)
    }

    @Test
    fun `given export prepared when export clicked then opens picker with dated archive name`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1))
        val date = Instant.ofEpochMilli(NOW).atZone(ZoneId.systemDefault()).toLocalDate()

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.ExportClicked)

            assertEquals(ExportNotesEffect.OpenDocumentPicker("Notas $date.zip"), awaitItem())
        }
        assertTrue(viewModel.state.value.isExporting)
    }

    @Test
    fun `given exporting when selection intents received then selection does not change`() = runTest {
        exporter.prepareGate = CompletableDeferred()
        val viewModel = loadedViewModel(selected = listOf(1))
        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        viewModel.onIntent(ExportNotesIntent.NoteToggled(1))
        viewModel.onIntent(ExportNotesIntent.NoteToggled(2))
        viewModel.onIntent(ExportNotesIntent.SelectAllClicked)
        viewModel.onIntent(ExportNotesIntent.ClearSelectionClicked)

        assertEquals(setOf(1L), viewModel.state.value.selectedIds)
    }

    @Test
    fun `given picker open when selection intents received then selection does not change`() = runTest {
        val viewModel = pickerOpenViewModel(selected = listOf(1))

        viewModel.onIntent(ExportNotesIntent.SelectAllClicked)

        assertEquals(setOf(1L), viewModel.state.value.selectedIds)
    }

    @Test
    fun `given nothing selected when export clicked then nothing happens`() = runTest {
        val viewModel = loadedViewModel()

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.ExportClicked)

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isExporting)
        assertTrue(exporter.preparedNotes.isEmpty())
    }

    @Test
    fun `given exporting when export clicked again then prepares only once`() = runTest {
        exporter.prepareGate = CompletableDeferred()
        val viewModel = loadedViewModel(selected = listOf(1))
        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        assertEquals(1, exporter.preparedNotes.size)
    }

    @Test
    fun `given prepare fails when exporting then stops and shows error`() = runTest {
        exporter.prepareResult = AppResult.Error(ExportError.WriteFailed)
        val viewModel = loadedViewModel(selected = listOf(1))

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.ExportClicked)

            assertEquals(ExportNotesEffect.ShowMessage(R.string.export_error), awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isExporting)
        assertEquals(setOf(1L), viewModel.state.value.selectedIds)
    }

    @Test
    fun `given picker open when closed without destination then discards export and keeps selection`() = runTest {
        val viewModel = pickerOpenViewModel(selected = listOf(1, 3))

        viewModel.effects.test {
            assertTrue(awaitItem() is ExportNotesEffect.OpenDocumentPicker)

            viewModel.onIntent(ExportNotesIntent.DestinationPicked(null))

            expectNoEvents()
        }
        assertEquals(listOf(FakeNotesExporter.PREPARED), exporter.discarded)
        assertTrue(exporter.saves.isEmpty())
        assertFalse(viewModel.state.value.isExporting)
        assertEquals(setOf(1L, 3L), viewModel.state.value.selectedIds)
        assertTrue(viewModel.state.value.canExport)
    }

    @Test
    fun `given picker open when destination picked then saves there and finishes with counts`() = runTest {
        val viewModel = pickerOpenViewModel()

        viewModel.effects.test {
            assertTrue(awaitItem() is ExportNotesEffect.OpenDocumentPicker)

            viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

            assertEquals(
                ExportNotesEffect.Finished(
                    exportedCount = FakeNotesExporter.PREPARED.noteCount,
                    missingImageCount = FakeNotesExporter.PREPARED.missingImageCount,
                ),
                awaitItem(),
            )
        }
        assertEquals(listOf(FakeNotesExporter.PREPARED to DESTINATION), exporter.saves)
        assertEquals(listOf(FakeNotesExporter.PREPARED), exporter.discarded)
        assertFalse(viewModel.state.value.isExporting)
    }

    @Test
    fun `given saving when destination picked then stays exporting until saved`() = runTest {
        exporter.saveGate = CompletableDeferred()
        val viewModel = pickerOpenViewModel()

        viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

        assertTrue(viewModel.state.value.isExporting)
    }

    @Test
    fun `given save fails when destination picked then shows error and discards export`() = runTest {
        exporter.saveResult = AppResult.Error(ExportError.WriteFailed)
        val viewModel = pickerOpenViewModel()

        viewModel.effects.test {
            assertTrue(awaitItem() is ExportNotesEffect.OpenDocumentPicker)

            viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

            assertEquals(ExportNotesEffect.ShowMessage(R.string.export_error), awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(FakeNotesExporter.PREPARED), exporter.discarded)
        assertFalse(viewModel.state.value.isExporting)
    }

    @Test
    fun `given no prepared export when destination picked then discards destination and shows error`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1))

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

            assertEquals(ExportNotesEffect.ShowMessage(R.string.export_error), awaitItem())
        }
        assertEquals(listOf(DESTINATION), exporter.discardedDestinations)
        assertTrue(exporter.saves.isEmpty())
        assertFalse(viewModel.state.value.isExporting)
    }

    @Test
    fun `given preparing when back clicked then cancels export and navigates back`() = runTest {
        exporter.prepareGate = CompletableDeferred()
        val viewModel = loadedViewModel(selected = listOf(1))
        viewModel.onIntent(ExportNotesIntent.ExportClicked)

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.BackClicked)
            exporter.prepareGate?.complete(Unit)

            assertEquals(ExportNotesEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given picker open when back clicked then discards prepared export and navigates back`() = runTest {
        val viewModel = pickerOpenViewModel()

        viewModel.effects.test {
            assertTrue(awaitItem() is ExportNotesEffect.OpenDocumentPicker)

            viewModel.onIntent(ExportNotesIntent.BackClicked)

            assertEquals(ExportNotesEffect.NavigateBack, awaitItem())
        }
        assertEquals(listOf(FakeNotesExporter.PREPARED), exporter.discarded)
    }

    @Test
    fun `given saving when back clicked then cancels save discards export and navigates back`() = runTest {
        exporter.saveGate = CompletableDeferred()
        val viewModel = pickerOpenViewModel()
        viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

        viewModel.effects.test {
            assertTrue(awaitItem() is ExportNotesEffect.OpenDocumentPicker)

            viewModel.onIntent(ExportNotesIntent.BackClicked)

            assertEquals(ExportNotesEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(FakeNotesExporter.PREPARED), exporter.discarded)
    }

    @Test
    fun `given idle when back clicked then navigates back without touching exporter`() = runTest {
        val viewModel = loadedViewModel(selected = listOf(1))

        viewModel.effects.test {
            viewModel.onIntent(ExportNotesIntent.BackClicked)

            assertEquals(ExportNotesEffect.NavigateBack, awaitItem())
        }
        assertTrue(exporter.discarded.isEmpty())
        assertTrue(exporter.discardedDestinations.isEmpty())
    }

    @Test
    fun `given full export when finished then no note is written and exported notes are unchanged`() = runTest {
        val notes = listOf(note(1), note(2))
        val viewModel = loadedViewModel(notes = notes, selected = listOf(1, 2))

        viewModel.onIntent(ExportNotesIntent.ExportClicked)
        viewModel.onIntent(ExportNotesIntent.DestinationPicked(DESTINATION))

        assertTrue(repository.writes.isEmpty())
        val exported = exporter.preparedNotes.single()
        assertEquals(2, exported.size)
        assertSame(notes[0], exported[0])
        assertSame(notes[1], exported[1])
    }

    private companion object {
        const val NOW = 1_794_484_800_000L
        const val DESTINATION = "content://documents/document/notas.zip"
    }
}
