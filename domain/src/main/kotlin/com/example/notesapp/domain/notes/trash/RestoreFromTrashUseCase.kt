package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject

/** Devuelve la nota a Mis notas tal como estaba; lo usan «Deshacer» y «Restaurar» (RF-4, RF-22). */
class RestoreFromTrashUseCase @Inject constructor(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(id: Long): AppResult<Unit, NoteError> = repository.restoreFromTrash(id)
}
