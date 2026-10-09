package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult

/** Copia imágenes elegidas por el usuario al almacenamiento de la app y las borra cuando ya no se usan. */
interface ImageStorage {
    /** Copia la imagen de [sourceUri] y devuelve la ruta de la copia. */
    suspend fun save(sourceUri: String): AppResult<String, NoteError>

    suspend fun delete(path: String)
}
