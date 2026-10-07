package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(id: Long): AppResult<Note, NoteError> = repository.getNote(id)
}
