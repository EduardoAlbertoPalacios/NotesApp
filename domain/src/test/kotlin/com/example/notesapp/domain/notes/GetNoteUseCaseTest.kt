package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNoteUseCaseTest {

    private val note = Note(
        id = 3,
        title = "Para leer",
        content = "Hábitos atómicos",
        color = NoteColor.MINT,
        isPinned = false,
        category = null,
        updatedAt = 10,
    )

    @Test
    fun `given stored note when getting by id then returns it`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(listOf(note)))

        assertEquals(AppResult.Success(note), GetNoteUseCase(repository)(3))
    }

    @Test
    fun `given missing note when getting by id then returns not found`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(listOf(note)))

        assertEquals(AppResult.Error(NoteError.NotFound), GetNoteUseCase(repository)(99))
    }
}
