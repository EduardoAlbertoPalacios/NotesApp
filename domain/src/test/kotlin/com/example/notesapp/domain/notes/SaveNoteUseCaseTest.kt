package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val saveNote = SaveNoteUseCase(repository, TimeProvider { NOW })

    @Test
    fun `given blank title and content when saving then returns empty note error without saving`() = runTest {
        val result = saveNote(note(title = "  ", content = "\n"))

        assertEquals(AppResult.Error(NoteError.EmptyNote), result)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `given new note when saving then stores it with current time and returns generated id`() = runTest {
        val result = saveNote(note(id = Note.NEW_ID, title = "Ideas"))

        assertEquals(note(id = Note.NEW_ID, title = "Ideas", updatedAt = NOW), repository.saved.single())
        assertEquals(AppResult.Success(note(id = 100, title = "Ideas", updatedAt = NOW)), result)
    }

    @Test
    fun `given existing note with only content when saving then keeps its id`() = runTest {
        val result = saveNote(note(id = 7, title = "", content = "Solo cuerpo"))

        assertEquals(AppResult.Success(note(id = 7, title = "", content = "Solo cuerpo", updatedAt = NOW)), result)
    }

    @Test
    fun `given storage failure when saving then returns the error`() = runTest {
        repository.failure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), saveNote(note(title = "Ideas")))
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
        const val NOW = 1_000L
    }
}
