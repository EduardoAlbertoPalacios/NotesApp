package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.FakeImageStorage
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyTrashUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()

    private fun createEmptyTrash() = EmptyTrashUseCase(
        repository = repository,
        deleteTrashedNotes = DeleteTrashedNotesUseCase(repository, imageStorage),
    )

    @Test
    fun `given notes in trash when emptying then deletes all of them and their images`() = runTest {
        repository.trash.value = AppResult.Success(
            listOf(
                trashedNote(id = 1, images = listOf(NoteImage(id = 1, path = "/a.jpg"))),
                trashedNote(id = 2, trashedAt = NOW - 100 * DAY, images = listOf(NoteImage(id = 2, path = "/b.jpg"))),
            ),
        )

        val result = createEmptyTrash()()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(listOf(1L, 2L)), repository.deletedNoteIdBatches)
        assertEquals(listOf("/a.jpg", "/b.jpg"), imageStorage.deletedPaths)
    }

    @Test
    fun `given empty trash when emptying then succeeds without deleting anything`() = runTest {
        val result = createEmptyTrash()()

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given trash cannot be read when emptying then returns the error without deleting`() = runTest {
        repository.trash.value = AppResult.Error(NoteError.Storage)

        val result = createEmptyTrash()()

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given delete failure when emptying then returns the error and keeps the images`() = runTest {
        repository.trash.value = AppResult.Success(
            listOf(trashedNote(id = 1, images = listOf(NoteImage(id = 1, path = "/a.jpg")))),
        )
        repository.deleteNotesFailure = NoteError.Storage

        val result = createEmptyTrash()()

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }
}
