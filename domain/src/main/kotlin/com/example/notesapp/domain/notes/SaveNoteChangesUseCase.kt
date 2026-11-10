package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import javax.inject.Inject

/**
 * Guarda de una vez los cambios pendientes de una nota: su contenido, las imágenes agregadas ([addedPaths],
 * ya copiadas) y las quitadas ([removedImages]), cuyos archivos se borran solo si el guardado va bien.
 * Usa la fecha actual. Si la nota queda sin título, contenido ni imágenes, devuelve [NoteError.EmptyNote].
 */
class SaveNoteChangesUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        note: Note,
        addedPaths: List<String>,
        removedImages: List<NoteImage>,
    ): AppResult<Note, NoteError> {
        val kept = note.copy(images = note.images - removedImages.toSet())
        if (kept.isBlank && addedPaths.isEmpty()) return AppResult.Error(NoteError.EmptyNote)
        val result = repository.saveNoteChanges(
            note = kept.copy(updatedAt = timeProvider.now()),
            addedPaths = addedPaths,
            removedImageIds = removedImages.map { it.id },
        )
        if (result is AppResult.Success) removedImages.forEach { imageStorage.delete(it.path) }
        return result
    }
}
