package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/**
 * Borra las copias de imágenes que ninguna nota usa, por ejemplo las pendientes de una nota cuyos cambios se
 * perdieron al cerrar la app desde las apps recientes. Solo debe llamarse cuando no hay cambios pendientes
 * en ningún editor.
 *
 * Compara por nombre de archivo, no por ruta: la ruta guardada puede diferir de la actual aunque el archivo sea
 * el mismo (por ejemplo `/data/data` frente a `/data/user/0`, o la app movida de almacenamiento). Si hay
 * imágenes en uso y ninguna coincide con los archivos, algo no cuadra y no se borra nada. Tampoco se borra
 * nada si no se pueden leer las imágenes en uso.
 */
class CleanUpImageFilesUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke() {
        val used = (repository.getImagePaths() as? AppResult.Success)?.data ?: return
        val usedNames = used.mapTo(mutableSetOf()) { it.fileName() }
        val files = imageStorage.listPaths()
        if (usedNames.isNotEmpty() && files.none { it.fileName() in usedNames }) return
        files.filter { it.fileName() !in usedNames }.forEach { imageStorage.delete(it) }
    }

    private fun String.fileName(): String = substringAfterLast('/')
}
