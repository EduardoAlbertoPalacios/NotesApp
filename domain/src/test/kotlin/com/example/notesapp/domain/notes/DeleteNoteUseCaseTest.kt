package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val deleteNote = DeleteNoteUseCase(repository, imageStorage)

    @Test
    fun `given note id when deleting then removes it from repository`() = runTest {
        val result = deleteNote(5)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(5L), repository.deletedIds)
    }

    @Test
    fun `given storage failure when deleting then returns the error`() = runTest {
        repository.failure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), deleteNote(5))
    }

    @Test
    fun `given note with images when deleting then deletes their files`() = runTest {
        repository.notes.value = AppResult.Success(listOf(noteWithImages()))

        val result = deleteNote(5)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("/a.jpg", "/b.jpg"), imageStorage.deletedPaths)
    }

    @Test
    fun `given note with images when delete fails then keeps their files`() = runTest {
        repository.notes.value = AppResult.Success(listOf(noteWithImages()))
        repository.deleteFailure = NoteError.Storage

        val result = deleteNote(5)

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given note without images when deleting then deletes no files`() = runTest {
        repository.notes.value = AppResult.Success(listOf(noteWithImages().copy(images = emptyList())))

        deleteNote(5)

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    private fun noteWithImages() = Note(
        id = 5,
        title = "Escapada",
        content = "",
        color = NoteColor.LAVENDER,
        isPinned = false,
        category = null,
        updatedAt = 10,
        images = listOf(NoteImage(id = 1, path = "/a.jpg"), NoteImage(id = 2, path = "/b.jpg")),
    )
}
