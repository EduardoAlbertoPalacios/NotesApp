package com.example.notesapp.presentation.notes.trash

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.DeleteNoteUseCase
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.trash.ChangeTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.DeleteTrashedNotesUseCase
import com.example.notesapp.domain.notes.trash.EmptyTrashUseCase
import com.example.notesapp.domain.notes.trash.GetTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.GetTrashUseCase
import com.example.notesapp.domain.notes.trash.RestoreFromTrashUseCase
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.domain.notes.trash.TrashedNote
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TrashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTrashNoteRepository()
    private val settings = FakeSettingsRepository()
    private val imageStorage = RecordingImageStorage()
    private val timeProvider = TimeProvider { NOW }

    /** Plazo leído: sin él la papelera no termina de cargar, y la mayoría de pruebas no tratan del plazo. */
    @Before
    fun storeRetention() = runTest {
        settings.emitStoredTrashRetention(TrashRetention.DAYS_30)
    }

    private fun createViewModel(): TrashViewModel {
        val getRetention = GetTrashRetentionUseCase(settings)
        val deleteTrashed = DeleteTrashedNotesUseCase(repository, imageStorage)
        return TrashViewModel(
            getTrash = GetTrashUseCase(repository, getRetention, timeProvider),
            getTrashRetention = getRetention,
            restoreFromTrash = RestoreFromTrashUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository, imageStorage),
            emptyTrash = EmptyTrashUseCase(repository, deleteTrashed),
            changeTrashRetention = ChangeTrashRetentionUseCase(
                repository,
                settings,
                getRetention,
                deleteTrashed,
                timeProvider,
            ),
        )
    }

    private suspend fun createLoadedViewModel(vararg notes: TrashedNote): TrashViewModel {
        repository.emitTrash(*notes)
        return createViewModel()
    }

    // --- Carga ---

    @Test
    fun `given no trash emission yet when created then state is loading`() {
        val viewModel = createViewModel()

        assertEquals(TrashUiState(retention = TrashRetention.DAYS_30), viewModel.state.value)
        assertFalse(viewModel.state.value.isEmpty)
        assertFalse(viewModel.state.value.canEmpty)
    }

    @Test
    fun `given trashed notes when loaded then shows them newest first with their days left`() = runTest {
        val older = trashed(id = 1, daysAgo = 5, title = "  Compras  ", content = " Leche ", color = NoteColor.ROSE)
        val newer = trashed(id = 2, daysAgo = 1)

        val viewModel = createLoadedViewModel(older, newer)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertTrue(state.canEmpty)
        assertEquals(
            listOf(
                TrashItemUi(id = 2, title = "Nota 2", excerpt = "Contenido", color = NoteColor.MINT, daysLeft = 29),
                TrashItemUi(id = 1, title = "Compras", excerpt = "Leche", color = NoteColor.ROSE, daysLeft = 25),
            ),
            state.notes,
        )
    }

    @Test
    fun `given never retention when loaded then notes have no days left`() = runTest {
        settings.emitStoredTrashRetention(TrashRetention.NEVER)

        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 400))

        val state = viewModel.state.value
        assertEquals(TrashRetention.NEVER, state.retention)
        assertEquals(listOf(1L), state.notes.map { it.id })
        assertNull(state.notes.single().daysLeft)
    }

    @Test
    fun `given empty trash when loaded then state is empty and cannot be emptied`() = runTest {
        val viewModel = createLoadedViewModel()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertFalse(state.canEmpty)
    }

    @Test
    fun `given storage error when loading then shows load failure instead of empty trash`() = runTest {
        repository.emitTrashError()

        val viewModel = createViewModel()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.loadFailed)
        assertTrue(state.notes.isEmpty())
        assertFalse(state.isEmpty)
        assertFalse(state.canEmpty)
    }

    @Test
    fun `given trash changes elsewhere when loaded then state follows it`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 3))

        repository.emitTrash(trashed(id = 1, daysAgo = 3), trashed(id = 2, daysAgo = 1))

        assertEquals(listOf(2L, 1L), viewModel.state.value.notes.map { it.id })
    }

    // --- Hoja de acciones ---

    @Test
    fun `given loaded note when clicked then opens its actions sheet`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.onIntent(TrashIntent.NoteClicked(1))

        assertEquals(1L, viewModel.state.value.actionsNoteId)
    }

    @Test
    fun `given note not in trash when clicked then does not open actions sheet`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.onIntent(TrashIntent.NoteClicked(99))

        assertNull(viewModel.state.value.actionsNoteId)
    }

    @Test
    fun `given open actions sheet when dismissed then closes it`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        viewModel.onIntent(TrashIntent.ActionsDismissed)

        assertNull(viewModel.state.value.actionsNoteId)
    }

    // --- Restaurar ---

    @Test
    fun `given open actions sheet when restore clicked then restores note closes sheet and shows restored notice`() =
        runTest {
            val viewModel = createLoadedViewModel(trashed(id = 1), trashed(id = 2))
            viewModel.onIntent(TrashIntent.NoteClicked(1))

            viewModel.effects.test {
                viewModel.onIntent(TrashIntent.RestoreClicked)

                assertEquals(TrashEffect.ShowMessage(R.string.trash_restored), awaitItem())
            }
            assertEquals(listOf(1L), repository.restoredIds)
            assertNull(viewModel.state.value.actionsNoteId)
            assertEquals(listOf(2L), viewModel.state.value.notes.map { it.id })
        }

    @Test
    fun `given restore fails when restore clicked then shows restore error and keeps note`() = runTest {
        repository.restoreResult = AppResult.Error(NoteError.Storage)
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.RestoreClicked)

            assertEquals(TrashEffect.ShowMessage(R.string.trash_restore_error), awaitItem())
        }
        assertNull(viewModel.state.value.actionsNoteId)
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given no open actions sheet when restore clicked then does nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.RestoreClicked)

            expectNoEvents()
        }
        assertTrue(repository.restoredIds.isEmpty())
    }

    // --- Eliminar definitivamente ---

    @Test
    fun `given open actions sheet when delete forever clicked then closes sheet and asks for confirmation`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        viewModel.onIntent(TrashIntent.DeleteForeverClicked)

        assertNull(viewModel.state.value.actionsNoteId)
        assertEquals(1L, viewModel.state.value.pendingDeleteId)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `given pending delete when confirmed then deletes note and its images without effects`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, images = listOf(IMAGE_A, IMAGE_B)), trashed(id = 2))
        viewModel.onIntent(TrashIntent.NoteClicked(1))
        viewModel.onIntent(TrashIntent.DeleteForeverClicked)

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.DeleteForeverConfirmed)

            expectNoEvents()
        }
        assertEquals(listOf(1L), repository.deletedIds)
        assertEquals(listOf(IMAGE_A.path, IMAGE_B.path), imageStorage.deletedPaths)
        assertNull(viewModel.state.value.pendingDeleteId)
        assertEquals(listOf(2L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given pending delete when dismissed then closes dialog and deletes nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, images = listOf(IMAGE_A)))
        viewModel.onIntent(TrashIntent.NoteClicked(1))
        viewModel.onIntent(TrashIntent.DeleteForeverClicked)

        viewModel.onIntent(TrashIntent.DeleteForeverDismissed)

        assertNull(viewModel.state.value.pendingDeleteId)
        assertTrue(repository.deletedIds.isEmpty())
        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given delete fails when confirmed then shows delete error and keeps note and images`() = runTest {
        repository.deleteResult = AppResult.Error(NoteError.Storage)
        val viewModel = createLoadedViewModel(trashed(id = 1, images = listOf(IMAGE_A)))
        viewModel.onIntent(TrashIntent.NoteClicked(1))
        viewModel.onIntent(TrashIntent.DeleteForeverClicked)

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.DeleteForeverConfirmed)

            assertEquals(TrashEffect.ShowMessage(R.string.trash_delete_error), awaitItem())
        }
        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given no pending delete when delete confirmed then deletes nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.onIntent(TrashIntent.DeleteForeverConfirmed)

        assertTrue(repository.deletedIds.isEmpty())
    }

    // --- Vaciar ---

    @Test
    fun `given notes in trash when empty clicked then asks for confirmation`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.onIntent(TrashIntent.EmptyClicked)

        assertTrue(viewModel.state.value.isEmptyConfirmVisible)
    }

    @Test
    fun `given empty confirmation when confirmed then deletes every note and their images`() = runTest {
        val viewModel = createLoadedViewModel(
            trashed(id = 1, images = listOf(IMAGE_A)),
            trashed(id = 2, images = listOf(IMAGE_B)),
        )
        viewModel.onIntent(TrashIntent.EmptyClicked)

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.EmptyConfirmed)

            expectNoEvents()
        }
        assertEquals(listOf(listOf(1L, 2L)), repository.deletedBatches)
        assertEquals(listOf(IMAGE_A.path, IMAGE_B.path), imageStorage.deletedPaths)
        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `given empty confirmation when dismissed then closes it and deletes nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.EmptyClicked)

        viewModel.onIntent(TrashIntent.EmptyDismissed)

        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
        assertTrue(repository.deletedBatches.isEmpty())
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given empty fails when confirmed then shows empty error and keeps notes and images`() = runTest {
        repository.deleteNotesResult = AppResult.Error(NoteError.Storage)
        val viewModel = createLoadedViewModel(trashed(id = 1, images = listOf(IMAGE_A)))
        viewModel.onIntent(TrashIntent.EmptyClicked)

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.EmptyConfirmed)

            assertEquals(TrashEffect.ShowMessage(R.string.trash_empty_error), awaitItem())
        }
        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
    }

    @Test
    fun `given empty trash when empty clicked then does not ask for confirmation`() = runTest {
        val viewModel = createLoadedViewModel()

        viewModel.onIntent(TrashIntent.EmptyClicked)

        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
    }

    @Test
    fun `given load failure when empty clicked then does not ask for confirmation`() = runTest {
        repository.emitTrashError()
        val viewModel = createViewModel()

        viewModel.onIntent(TrashIntent.EmptyClicked)

        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
    }

    @Test
    fun `given no empty confirmation when empty confirmed then deletes nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))

        viewModel.onIntent(TrashIntent.EmptyConfirmed)

        assertTrue(repository.deletedBatches.isEmpty())
    }

    // --- Plazo ---

    @Test
    fun `given trash when retention clicked and sheet dismissed then toggles retention sheet`() = runTest {
        val viewModel = createLoadedViewModel()

        viewModel.onIntent(TrashIntent.RetentionClicked)
        assertTrue(viewModel.state.value.isRetentionSheetVisible)

        viewModel.onIntent(TrashIntent.RetentionSheetDismissed)
        assertFalse(viewModel.state.value.isRetentionSheetVisible)
    }

    @Test
    fun `given current retention when selected again then closes sheet and saves nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))
        viewModel.onIntent(TrashIntent.RetentionClicked)

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_30))

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isRetentionSheetVisible)
        assertTrue(settings.savedTrashRetentions.isEmpty())
        assertEquals(TrashRetention.DAYS_30, viewModel.state.value.retention)
    }

    @Test
    fun `given longer retention when selected then applies it without confirmation`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))
        viewModel.onIntent(TrashIntent.RetentionClicked)

        viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_90))

        val state = viewModel.state.value
        assertFalse(state.isRetentionSheetVisible)
        assertNull(state.pendingRetention)
        assertEquals(listOf(TrashRetention.DAYS_90), settings.savedTrashRetentions)
        assertEquals(TrashRetention.DAYS_90, state.retention)
        assertEquals(80, state.notes.single().daysLeft)
        assertTrue(repository.deletedBatches.isEmpty())
    }

    @Test
    fun `given shorter retention without affected notes when selected then applies it without confirmation`() =
        runTest {
            val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 2))

            viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_7))

            assertNull(viewModel.state.value.pendingRetention)
            assertEquals(listOf(TrashRetention.DAYS_7), settings.savedTrashRetentions)
            assertEquals(TrashRetention.DAYS_7, viewModel.state.value.retention)
            assertTrue(repository.deletedBatches.isEmpty())
        }

    @Test
    fun `given shorter retention with affected notes when selected then asks confirmation with their count`() =
        runTest {
            val viewModel = createLoadedViewModel(
                trashed(id = 1, daysAgo = 10),
                trashed(id = 2, daysAgo = 8),
                trashed(id = 3, daysAgo = 2),
            )
            viewModel.onIntent(TrashIntent.RetentionClicked)

            viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_7))

            val state = viewModel.state.value
            assertFalse(state.isRetentionSheetVisible)
            assertEquals(PendingRetention(TrashRetention.DAYS_7, 2), state.pendingRetention)
            assertTrue(settings.savedTrashRetentions.isEmpty())
            assertEquals(TrashRetention.DAYS_30, state.retention)
            assertTrue(repository.deletedBatches.isEmpty())
        }

    @Test
    fun `given pending shorter retention when confirmed then applies it and deletes affected notes and images`() =
        runTest {
            val viewModel = createLoadedViewModel(
                trashed(id = 1, daysAgo = 10, images = listOf(IMAGE_A)),
                trashed(id = 2, daysAgo = 2),
            )
            viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_7))

            viewModel.effects.test {
                viewModel.onIntent(TrashIntent.RetentionConfirmed)

                expectNoEvents()
            }
            val state = viewModel.state.value
            assertNull(state.pendingRetention)
            assertEquals(listOf(TrashRetention.DAYS_7), settings.savedTrashRetentions)
            assertEquals(TrashRetention.DAYS_7, state.retention)
            assertEquals(listOf(listOf(1L)), repository.deletedBatches)
            assertEquals(listOf(IMAGE_A.path), imageStorage.deletedPaths)
            assertEquals(listOf(2L), state.notes.map { it.id })
        }

    @Test
    fun `given pending shorter retention when dismissed then keeps retention and deletes nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))
        viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_7))

        viewModel.onIntent(TrashIntent.RetentionDismissed)

        val state = viewModel.state.value
        assertNull(state.pendingRetention)
        assertTrue(settings.savedTrashRetentions.isEmpty())
        assertEquals(TrashRetention.DAYS_30, state.retention)
        assertTrue(repository.deletedBatches.isEmpty())
        assertEquals(listOf(1L), state.notes.map { it.id })
    }

    @Test
    fun `given no pending retention when retention confirmed then saves nothing`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))

        viewModel.onIntent(TrashIntent.RetentionConfirmed)

        assertTrue(settings.savedTrashRetentions.isEmpty())
    }

    @Test
    fun `given retention save fails when longer retention selected then shows save error and keeps retention`() =
        runTest {
            settings.trashRetentionSaveResult = AppResult.Error(SettingsError.Storage)
            val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))

            viewModel.effects.test {
                viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_90))

                assertEquals(TrashEffect.ShowMessage(R.string.trash_retention_save_error), awaitItem())
            }
            assertEquals(TrashRetention.DAYS_30, viewModel.state.value.retention)
        }

    @Test
    fun `given retention save fails when shorter retention confirmed then shows save error and deletes nothing`() =
        runTest {
            settings.trashRetentionSaveResult = AppResult.Error(SettingsError.Storage)
            val viewModel = createLoadedViewModel(trashed(id = 1, daysAgo = 10))
            viewModel.onIntent(TrashIntent.RetentionSelected(TrashRetention.DAYS_7))

            viewModel.effects.test {
                viewModel.onIntent(TrashIntent.RetentionConfirmed)

                assertEquals(TrashEffect.ShowMessage(R.string.trash_retention_save_error), awaitItem())
            }
            assertEquals(TrashRetention.DAYS_30, viewModel.state.value.retention)
            assertTrue(repository.deletedBatches.isEmpty())
            assertEquals(listOf(1L), viewModel.state.value.notes.map { it.id })
        }

    // --- La nota desaparece ---

    @Test
    fun `given open actions sheet when its note disappears then closes the sheet`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1), trashed(id = 2))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        repository.emitTrash(trashed(id = 2))

        assertNull(viewModel.state.value.actionsNoteId)
    }

    @Test
    fun `given open actions sheet when another note disappears then keeps the sheet open`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1), trashed(id = 2))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        repository.emitTrash(trashed(id = 1))

        assertEquals(1L, viewModel.state.value.actionsNoteId)
    }

    @Test
    fun `given pending delete when its note disappears then closes the dialog`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1), trashed(id = 2))
        viewModel.onIntent(TrashIntent.NoteClicked(1))
        viewModel.onIntent(TrashIntent.DeleteForeverClicked)

        repository.emitTrash(trashed(id = 2))

        assertNull(viewModel.state.value.pendingDeleteId)
    }

    @Test
    fun `given empty confirmation when every note disappears then closes the confirmation`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.EmptyClicked)

        repository.emitTrash()

        assertFalse(viewModel.state.value.isEmptyConfirmVisible)
    }

    @Test
    fun `given open actions sheet when trash fails to load then closes the sheet`() = runTest {
        val viewModel = createLoadedViewModel(trashed(id = 1))
        viewModel.onIntent(TrashIntent.NoteClicked(1))

        repository.emitTrashError()

        assertNull(viewModel.state.value.actionsNoteId)
        assertTrue(viewModel.state.value.loadFailed)
    }

    // --- Navegación ---

    @Test
    fun `given trash when back clicked then navigates back`() = runTest {
        val viewModel = createLoadedViewModel()

        viewModel.effects.test {
            viewModel.onIntent(TrashIntent.BackClicked)

            assertEquals(TrashEffect.NavigateBack, awaitItem())
        }
    }

    private fun trashed(
        id: Long,
        daysAgo: Int = 0,
        title: String = "Nota $id",
        content: String = "Contenido",
        color: NoteColor = NoteColor.MINT,
        images: List<NoteImage> = emptyList(),
    ) = TrashedNote(
        note = Note(
            id = id,
            title = title,
            content = content,
            color = color,
            isPinned = false,
            category = null,
            updatedAt = NOW - 100 * DAY,
            images = images,
        ),
        trashedAt = NOW - daysAgo * DAY,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L
        val IMAGE_A = NoteImage(id = 10, path = "/files/note_images/a.jpg")
        val IMAGE_B = NoteImage(id = 11, path = "/files/note_images/b.jpg")
    }
}
