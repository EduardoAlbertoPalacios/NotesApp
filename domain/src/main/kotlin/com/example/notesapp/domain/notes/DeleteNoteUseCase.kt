package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/** Elimina la nota y borra las copias de sus imágenes. */
class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(id: Long): AppResult<Unit, NoteError> {
        val images = (repository.getNote(id) as? AppResult.Success)?.data?.images.orEmpty()
        return repository.deleteNote(id).also { result ->
            if (result is AppResult.Success) images.forEach { imageStorage.delete(it.path) }
        }
    }
}
