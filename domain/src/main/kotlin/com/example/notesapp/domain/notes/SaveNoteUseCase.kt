package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import javax.inject.Inject

/** Guarda la nota con la fecha de edición actual. Una nota sin título ni contenido no se guarda. */
class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(note: Note): AppResult<Note, NoteError> {
        if (note.isBlank) return AppResult.Error(NoteError.EmptyNote)
        val toSave = note.copy(updatedAt = timeProvider.now())
        return when (val result = repository.saveNote(toSave)) {
            is AppResult.Success -> AppResult.Success(toSave.copy(id = result.data))
            is AppResult.Error -> result
        }
    }
}
