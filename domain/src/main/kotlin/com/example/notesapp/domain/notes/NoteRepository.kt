package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<AppResult<List<Note>, NoteError>>
}
