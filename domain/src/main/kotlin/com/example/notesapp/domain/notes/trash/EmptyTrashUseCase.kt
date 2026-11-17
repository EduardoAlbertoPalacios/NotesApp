package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Borra definitivamente todas las notas de la papelera y sus imágenes, todo o nada (RF-32, RF-35). */
class EmptyTrashUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val deleteTrashedNotes: DeleteTrashedNotesUseCase,
) {
    suspend operator fun invoke(): AppResult<Unit, NoteError> =
        when (val trash = repository.observeTrash().first()) {
            is AppResult.Success -> deleteTrashedNotes(trash.data)
            is AppResult.Error -> trash
        }
}
