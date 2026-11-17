package com.example.notesapp.presentation.startup

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.trash.DeleteTrashedNotesUseCase
import com.example.notesapp.domain.notes.trash.GetTrashRetentionUseCase
import com.example.notesapp.domain.notes.trash.PurgeExpiredTrashUseCase
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.domain.notes.trash.TrashedNote
import com.example.notesapp.presentation.MainDispatcherRule
import com.example.notesapp.presentation.notes.trash.FakeTrashNoteRepository
import com.example.notesapp.presentation.notes.trash.RecordingImageStorage
import com.example.notesapp.presentation.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppStartupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTrashNoteRepository()
    private val settings = FakeSettingsRepository()
    private val imageStorage = RecordingImageStorage()

    @Before
    fun storeRetention() = runTest {
        settings.emitStoredTrashRetention(TrashRetention.DAYS_30)
    }

    private fun createViewModel(): AppStartupViewModel {
        val getRetention = GetTrashRetentionUseCase(settings)
        return AppStartupViewModel(
            PurgeExpiredTrashUseCase(
                repository,
                getRetention,
                DeleteTrashedNotesUseCase(repository, imageStorage),
                TimeProvider { NOW },
            ),
        )
    }

    @Test
    fun `given expired and recent trashed notes when created then purges only the expired ones with their images`() =
        runTest {
            repository.emitTrash(
                trashed(id = 1, daysAgo = 31, images = listOf(IMAGE_A)),
                trashed(id = 2, daysAgo = 3),
            )

            createViewModel()

            assertEquals(listOf(listOf(1L)), repository.deletedBatches)
            assertEquals(listOf(IMAGE_A.path), imageStorage.deletedPaths)
        }

    @Test
    fun `given created when trash changes afterwards then does not purge again`() = runTest {
        repository.emitTrash(trashed(id = 1, daysAgo = 31))
        createViewModel()

        repository.emitTrash(trashed(id = 2, daysAgo = 40))

        assertEquals(listOf(listOf(1L)), repository.deletedBatches)
    }

    @Test
    fun `given trash cannot be read when created then does not fail nor delete anything`() = runTest {
        repository.emitTrashError()

        createViewModel()

        assertTrue(repository.deletedBatches.isEmpty())
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given purge deletion fails when created then does not fail nor delete images`() = runTest {
        repository.deleteNotesResult = AppResult.Error(NoteError.Storage)
        repository.emitTrash(trashed(id = 1, daysAgo = 31, images = listOf(IMAGE_A)))

        createViewModel()

        assertEquals(listOf(listOf(1L)), repository.deletedBatches)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    private fun trashed(id: Long, daysAgo: Int, images: List<NoteImage> = emptyList()) = TrashedNote(
        note = Note(
            id = id,
            title = "Nota $id",
            content = "Contenido",
            color = NoteColor.MINT,
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
    }
}
