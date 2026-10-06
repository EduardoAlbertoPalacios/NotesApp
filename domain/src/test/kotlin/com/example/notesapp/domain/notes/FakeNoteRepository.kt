package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNoteRepository(
    initial: AppResult<List<Note>, NoteError> = AppResult.Success(emptyList()),
) : NoteRepository {
    val notes = MutableStateFlow(initial)

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = notes
}
