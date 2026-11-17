package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MoveNoteToTrashUseCaseTest {

    private val repository = FakeNoteRepository()

    private fun createMoveNoteToTrash(now: Long = NOW) = MoveNoteToTrashUseCase(repository, TimeProvider { now })

    @Test
    fun `given note id when moving to trash then marks it with the current time`() = runTest {
        val result = createMoveNoteToTrash(now = NOW + 123)(5)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(5L to NOW + 123), repository.movedToTrash)
    }

    @Test
    fun `given repository failure when moving to trash then returns the error`() = runTest {
        repository.moveToTrashFailure = NoteError.NotFound

        assertEquals(AppResult.Error(NoteError.NotFound), createMoveNoteToTrash()(5))
    }
}
