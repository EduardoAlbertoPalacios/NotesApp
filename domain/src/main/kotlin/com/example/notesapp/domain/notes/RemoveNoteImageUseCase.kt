package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/** Quita la imagen de la nota y borra su copia. */
class RemoveNoteImageUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(image: NoteImage): AppResult<Unit, NoteError> =
        repository.removeImage(image.id).also { result ->
            if (result is AppResult.Success) imageStorage.delete(image.path)
        }
}
