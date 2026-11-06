package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/**
 * Copia las imágenes elegidas sin asociarlas a ninguna nota: quedan pendientes hasta que la nota se guarde.
 * La copia se hace ya porque el acceso a la imagen original es temporal. Si no se copia ninguna, devuelve
 * [NoteError.ImageUnavailable].
 */
class ImportNoteImagesUseCase @Inject constructor(
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(sourceUris: List<String>): AppResult<ImportedImages, NoteError> {
        if (sourceUris.isEmpty()) return AppResult.Success(ImportedImages(paths = emptyList(), failedCount = 0))
        val paths = sourceUris.mapNotNull { uri -> (imageStorage.save(uri) as? AppResult.Success)?.data }
        if (paths.isEmpty()) return AppResult.Error(NoteError.ImageUnavailable)
        return AppResult.Success(ImportedImages(paths, failedCount = sourceUris.size - paths.size))
    }
}
