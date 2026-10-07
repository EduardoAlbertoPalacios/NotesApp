package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteNoteUseCaseTest {

    private val repository = FakeNoteRepository()

    @Test
    fun `given note id when deleting then removes it from repository`() = runTest {
        val result = DeleteNoteUseCase(repository)(5)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(5L), repository.deletedIds)
    }

    @Test
    fun `given storage failure when deleting then returns the error`() = runTest {
        repository.failure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), DeleteNoteUseCase(repository)(5))
    }
}
