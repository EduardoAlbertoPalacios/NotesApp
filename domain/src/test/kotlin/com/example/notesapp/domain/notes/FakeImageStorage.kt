package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult

class FakeImageStorage : ImageStorage {
    /** Uris que no se pueden copiar. */
    val unavailableUris = mutableSetOf<String>()
    val savedUris = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    /** Copias que existen en el almacenamiento; se añaden al guardar y se quitan al borrar. */
    val storedPaths = mutableSetOf<String>()

    override suspend fun save(sourceUri: String): AppResult<String, NoteError> {
        if (sourceUri in unavailableUris) return AppResult.Error(NoteError.ImageUnavailable)
        savedUris += sourceUri
        storedPaths += copyOf(sourceUri)
        return AppResult.Success(copyOf(sourceUri))
    }

    override suspend fun delete(path: String) {
        deletedPaths += path
        storedPaths -= path
    }

    override suspend fun listPaths(): Set<String> = storedPaths.toSet()

    override suspend fun exists(path: String): Boolean = path in storedPaths

    companion object {
        fun copyOf(uri: String) = "/files/note_images/${uri.substringAfterLast('/')}.jpg"
    }
}
