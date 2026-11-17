package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeImageStorage
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurgeExpiredTrashUseCaseTest {

    private val repository = FakeNoteRepository()
    private val settings = FakeSettingsRepository()
    private val imageStorage = FakeImageStorage()

    private fun createPurgeExpiredTrash() = PurgeExpiredTrashUseCase(
        repository = repository,
        getTrashRetention = GetTrashRetentionUseCase(settings),
        deleteTrashedNotes = DeleteTrashedNotesUseCase(repository, imageStorage),
        timeProvider = TimeProvider { NOW },
    )

    @Test
    fun `given expired and current notes when purging then deletes only expired ones and their images`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        repository.trash.value = AppResult.Success(
            listOf(
                trashedNote(id = 1, trashedAt = NOW - 7 * DAY, images = listOf(NoteImage(id = 1, path = "/a.jpg"))),
                trashedNote(id = 2, trashedAt = NOW - 7 * DAY + 1, images = listOf(NoteImage(id = 2, path = "/b.jpg"))),
                trashedNote(id = 3, trashedAt = NOW - 30 * DAY),
            ),
        )

        val result = createPurgeExpiredTrash()()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(listOf(1L, 3L)), repository.deletedNoteIdBatches)
        assertEquals(listOf("/a.jpg"), imageStorage.deletedPaths)
    }

    @Test
    fun `given no stored retention when purging then uses thirty days`() = runTest {
        repository.trash.value = AppResult.Success(
            listOf(trashedNote(id = 1, trashedAt = NOW - 29 * DAY), trashedNote(id = 2, trashedAt = NOW - 30 * DAY)),
        )

        createPurgeExpiredTrash()()

        assertEquals(listOf(listOf(2L)), repository.deletedNoteIdBatches)
    }

    @Test
    fun `given no expired notes when purging then deletes nothing`() = runTest {
        repository.trash.value = AppResult.Success(listOf(trashedNote(id = 1, trashedAt = NOW - DAY)))

        val result = createPurgeExpiredTrash()()

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given never retention when purging then neither reads nor deletes anything`() = runTest {
        settings.trashRetention.value = TrashRetention.NEVER
        repository.trash.value = AppResult.Success(listOf(trashedNote(id = 1, trashedAt = 0)))

        val result = createPurgeExpiredTrash()()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(0, repository.observeTrashCalls)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given trash cannot be read when purging then returns the error`() = runTest {
        repository.trash.value = AppResult.Error(NoteError.Storage)

        val result = createPurgeExpiredTrash()()

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given delete failure when purging then returns the error and keeps the images`() = runTest {
        repository.trash.value = AppResult.Success(
            listOf(trashedNote(id = 1, trashedAt = 0, images = listOf(NoteImage(id = 1, path = "/a.jpg")))),
        )
        repository.deleteNotesFailure = NoteError.Storage

        val result = createPurgeExpiredTrash()()

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }
}
