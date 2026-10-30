package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val restoreNote = RestoreNoteUseCase(repository)

    @Test
    fun `given existing note when restoring then keeps its original edit time`() = runTest {
        val original = note(id = 7, title = "Ideas", updatedAt = ORIGINAL_TIME)

        val result = restoreNote(original)

        assertEquals(listOf(original), repository.saved)
        assertEquals(AppResult.Success(original), result)
    }

    @Test
    fun `given blank note when restoring then returns empty note error without saving`() = runTest {
        val result = restoreNote(note(title = "  ", content = "\n"))

        assertEquals(AppResult.Error(NoteError.EmptyNote), result)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `given new note when restoring then returns it with the id assigned by the repository`() = runTest {
        val result = restoreNote(note(id = Note.NEW_ID, title = "Ideas", updatedAt = ORIGINAL_TIME))

        assertEquals(AppResult.Success(note(id = 100, title = "Ideas", updatedAt = ORIGINAL_TIME)), result)
    }

    @Test
    fun `given storage failure when restoring then returns the error`() = runTest {
        repository.failure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), restoreNote(note(title = "Ideas")))
    }

    private fun note(
        id: Long = 1,
        title: String = "Título",
        content: String = "",
        updatedAt: Long = 0,
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = NoteColor.AQUA,
        isPinned = false,
        category = null,
        updatedAt = updatedAt,
    )

    private companion object {
        const val ORIGINAL_TIME = 500L
    }
}
