package com.example.notesapp.domain.notes

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNotesUseCaseTest {

    @Test
    fun `given unordered notes when invoked then emits pinned first and newest edit first`() = runTest {
        val oldPinned = note(id = 1, isPinned = true, updatedAt = 100)
        val newUnpinned = note(id = 2, isPinned = false, updatedAt = 300)
        val newPinned = note(id = 3, isPinned = true, updatedAt = 200)
        val oldUnpinned = note(id = 4, isPinned = false, updatedAt = 50)
        val repository = FakeNoteRepository(
            AppResult.Success(listOf(oldPinned, newUnpinned, newPinned, oldUnpinned)),
        )

        GetNotesUseCase(repository)().test {
            assertEquals(
                AppResult.Success(listOf(newPinned, oldPinned, newUnpinned, oldUnpinned)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `given repository updates when invoked then emits each new list`() = runTest {
        val repository = FakeNoteRepository()
        val note = note(id = 1)

        GetNotesUseCase(repository)().test {
            assertEquals(AppResult.Success(emptyList<Note>()), awaitItem())
            repository.notes.value = AppResult.Success(listOf(note))
            assertEquals(AppResult.Success(listOf(note)), awaitItem())
        }
    }

    @Test
    fun `given storage error when invoked then emits the error`() = runTest {
        val repository = FakeNoteRepository(AppResult.Error(NoteError.Storage))

        GetNotesUseCase(repository)().test {
            assertEquals(AppResult.Error(NoteError.Storage), awaitItem())
        }
    }

    private fun note(id: Long, isPinned: Boolean = false, updatedAt: Long = 0) = Note(
        id = id,
        title = "Nota $id",
        content = "Contenido",
        color = NoteColor.AQUA,
        isPinned = isPinned,
        updatedAt = updatedAt,
    )
}
