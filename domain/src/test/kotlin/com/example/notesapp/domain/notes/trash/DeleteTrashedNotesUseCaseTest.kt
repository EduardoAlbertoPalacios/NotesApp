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

class DeleteTrashedNotesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()

    private fun createDeleteTrashedNotes() = DeleteTrashedNotesUseCase(repository, imageStorage)

    @Test
    fun `given trashed notes when deleting then deletes them in one call`() = runTest {
        val notes = listOf(trashedNote(id = 1), trashedNote(id = 2))

        val result = createDeleteTrashedNotes()(notes)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(listOf(1L, 2L)), repository.deletedNoteIdBatches)
    }

    @Test
    fun `given notes with images when deleting then deletes their files`() = runTest {
        val notes = listOf(
            trashedNote(id = 1, images = listOf(NoteImage(id = 1, path = "/a.jpg"))),
            trashedNote(id = 2, images = listOf(NoteImage(id = 2, path = "/b.jpg"), NoteImage(id = 3, path = "/c.jpg"))),
        )

        createDeleteTrashedNotes()(notes)

        assertEquals(listOf("/a.jpg", "/b.jpg", "/c.jpg"), imageStorage.deletedPaths)
    }

    @Test
    fun `given repository failure when deleting then returns the error and keeps the files`() = runTest {
        repository.deleteNotesFailure = NoteError.Storage
        val notes = listOf(trashedNote(id = 1, images = listOf(NoteImage(id = 1, path = "/a.jpg"))))

        val result = createDeleteTrashedNotes()(notes)

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given empty list when deleting then succeeds without calling the repository`() = runTest {
        repository.deleteNotesFailure = NoteError.Storage

        val result = createDeleteTrashedNotes()(emptyList())

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }
}
