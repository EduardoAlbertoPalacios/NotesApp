package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveNoteImageUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val removeNoteImage = RemoveNoteImageUseCase(repository, imageStorage)
    private val image = NoteImage(id = 3, path = "/files/note_images/foto.jpg")

    @Test
    fun `given image when removing then removes its row and deletes its file`() = runTest {
        val result = removeNoteImage(image)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(3L), repository.removedImageIds)
        assertEquals(listOf(image.path), imageStorage.deletedPaths)
    }

    @Test
    fun `given repository failure when removing then returns error and keeps the file`() = runTest {
        repository.removeImageFailure = NoteError.Storage

        val result = removeNoteImage(image)

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }
}
