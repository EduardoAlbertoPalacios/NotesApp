package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject

/** Mueve la nota a la papelera con la hora actual (RF-1, RF-6). */
class MoveNoteToTrashUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(id: Long): AppResult<Unit, NoteError> =
        repository.moveToTrash(id, timeProvider.now())
}
