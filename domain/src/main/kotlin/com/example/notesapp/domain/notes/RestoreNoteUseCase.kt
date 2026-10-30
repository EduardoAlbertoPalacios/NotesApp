package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/**
 * Guarda la nota tal como estaba, conservando su fecha de edición: al deshacer hasta el estado en que se
 * abrió, la nota recupera su fecha original. Una nota sin título ni contenido no se guarda.
 */
class RestoreNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(note: Note): AppResult<Note, NoteError> {
        if (note.isBlank) return AppResult.Error(NoteError.EmptyNote)
        return when (val result = repository.saveNote(note)) {
            is AppResult.Success -> AppResult.Success(note.copy(id = result.data))
            is AppResult.Error -> result
        }
    }
}
