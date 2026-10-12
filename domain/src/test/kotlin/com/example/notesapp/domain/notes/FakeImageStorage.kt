package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult

class FakeImageStorage : ImageStorage {
    /** Uris que no se pueden copiar. */
    val unavailableUris = mutableSetOf<String>()
    val savedUris = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    override suspend fun save(sourceUri: String): AppResult<String, NoteError> {
        if (sourceUri in unavailableUris) return AppResult.Error(NoteError.ImageUnavailable)
        savedUris += sourceUri
        return AppResult.Success(copyOf(sourceUri))
    }

    override suspend fun delete(path: String) {
        deletedPaths += path
    }

    companion object {
        fun copyOf(uri: String) = "/files/note_images/${uri.substringAfterLast('/')}.jpg"
    }
}
