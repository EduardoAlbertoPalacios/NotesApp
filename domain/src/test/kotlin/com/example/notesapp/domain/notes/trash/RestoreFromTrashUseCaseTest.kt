package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreFromTrashUseCaseTest {

    private val repository = FakeNoteRepository()

    private fun createRestoreFromTrash() = RestoreFromTrashUseCase(repository)

    @Test
    fun `given trashed note id when restoring then restores it in repository`() = runTest {
        val result = createRestoreFromTrash()(7)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(7L), repository.restoredFromTrashIds)
    }

    @Test
    fun `given repository failure when restoring then returns the error`() = runTest {
        repository.restoreFromTrashFailure = NoteError.Storage

        assertEquals(AppResult.Error(NoteError.Storage), createRestoreFromTrash()(7))
    }
}
