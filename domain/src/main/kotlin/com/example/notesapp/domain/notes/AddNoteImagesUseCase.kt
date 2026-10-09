package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import javax.inject.Inject

/**
 * Copia las imágenes elegidas y las asocia a la nota. Agregar imágenes es una edición: guarda la nota
 * con la fecha actual y, si todavía no existe, la crea aunque no tenga texto, porque una nota con
 * imágenes ya no está vacía. Las imágenes que no se pueden copiar se omiten y se informan en
 * [AddedImages.failedCount]; si no se copia ninguna, devuelve [NoteError.ImageUnavailable].
 */
class AddNoteImagesUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(note: Note, sourceUris: List<String>): AppResult<AddedImages, NoteError> {
        if (sourceUris.isEmpty()) return AppResult.Success(AddedImages(note, failedCount = 0))

        val paths = sourceUris.mapNotNull { uri -> (imageStorage.save(uri) as? AppResult.Success)?.data }
        if (paths.isEmpty()) return AppResult.Error(NoteError.ImageUnavailable)

        val toSave = note.copy(updatedAt = timeProvider.now())
        val stored = when (val result = repository.saveNote(toSave)) {
            is AppResult.Success -> toSave.copy(id = result.data)
            is AppResult.Error -> return result.also { discard(paths) }
        }

        return when (val result = repository.addImages(stored.id, paths)) {
            is AppResult.Success -> AppResult.Success(
                AddedImages(
                    note = stored.copy(images = stored.images + result.data),
                    failedCount = sourceUris.size - paths.size,
                ),
            )
            is AppResult.Error -> result.also { discard(paths) }
        }
    }

    private suspend fun discard(paths: List<String>) {
        paths.forEach { imageStorage.delete(it) }
    }
}
