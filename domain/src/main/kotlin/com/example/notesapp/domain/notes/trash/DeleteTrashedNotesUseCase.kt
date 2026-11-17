package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject

/**
 * Borra definitivamente notas de la papelera y, solo si eso fue bien, los archivos de sus imágenes. Lo
 * comparten vaciar, purgar y acortar el plazo (RF-26, RF-32, RF-43, RF-49).
 */
class DeleteTrashedNotesUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(notes: List<TrashedNote>): AppResult<Unit, NoteError> {
        if (notes.isEmpty()) return AppResult.Success(Unit)
        return repository.deleteNotes(notes.map { it.note.id }).also { result ->
            if (result is AppResult.Success) {
                notes.flatMap { it.note.images }.forEach { imageStorage.delete(it.path) }
            }
        }
    }
}
