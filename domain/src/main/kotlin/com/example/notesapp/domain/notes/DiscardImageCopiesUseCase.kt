package com.example.notesapp.domain.notes

import javax.inject.Inject

/** Borra copias de imágenes que nunca llegaron a asociarse a una nota. */
class DiscardImageCopiesUseCase @Inject constructor(
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(paths: List<String>) {
        paths.forEach { imageStorage.delete(it) }
    }
}
