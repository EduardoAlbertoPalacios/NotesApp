package com.example.notesapp.domain.notes

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNotesUseCaseTest {

    private val work = note(id = 10, updatedAt = 10, category = NoteCategory.WORK)
    private val personal = note(id = 11, updatedAt = 20, category = NoteCategory.PERSONAL)
    private val uncategorized = note(id = 12, updatedAt = 30, category = null)
    private val mixedNotes = listOf(work, personal, uncategorized)

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

    @Test
    fun `given work category when invoked then emits only work notes`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        GetNotesUseCase(repository)(NoteCategory.WORK).test {
            assertEquals(AppResult.Success(listOf(work)), awaitItem())
        }
    }

    @Test
    fun `given personal category when invoked then emits only personal notes`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        GetNotesUseCase(repository)(NoteCategory.PERSONAL).test {
            assertEquals(AppResult.Success(listOf(personal)), awaitItem())
        }
    }

    @Test
    fun `given null category when invoked then emits every note including uncategorized`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        GetNotesUseCase(repository)(null).test {
            assertEquals(AppResult.Success(listOf(uncategorized, personal, work)), awaitItem())
        }
    }

    @Test
    fun `given only uncategorized notes when filtering by category then emits empty list`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(listOf(uncategorized)))

        GetNotesUseCase(repository)(NoteCategory.WORK).test {
            assertEquals(AppResult.Success(emptyList<Note>()), awaitItem())
        }
    }

    @Test
    fun `given category filter when invoked then keeps pinned first and newest edit first`() = runTest {
        val oldPinned = note(id = 1, isPinned = true, updatedAt = 100, category = NoteCategory.WORK)
        val newUnpinned = note(id = 2, isPinned = false, updatedAt = 300, category = NoteCategory.WORK)
        val otherCategory = note(id = 3, isPinned = true, updatedAt = 400, category = NoteCategory.PERSONAL)
        val newPinned = note(id = 4, isPinned = true, updatedAt = 200, category = NoteCategory.WORK)
        val repository = FakeNoteRepository(
            AppResult.Success(listOf(oldPinned, newUnpinned, otherCategory, newPinned)),
        )

        GetNotesUseCase(repository)(NoteCategory.WORK).test {
            assertEquals(AppResult.Success(listOf(newPinned, oldPinned, newUnpinned)), awaitItem())
        }
    }

    private fun note(
        id: Long,
        isPinned: Boolean = false,
        updatedAt: Long = 0,
        category: NoteCategory? = null,
    ) = Note(
        id = id,
        title = "Nota $id",
        content = "Contenido",
        color = NoteColor.AQUA,
        isPinned = isPinned,
        category = category,
        updatedAt = updatedAt,
    )
}
