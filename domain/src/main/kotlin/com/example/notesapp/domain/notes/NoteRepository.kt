package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<AppResult<List<Note>, NoteError>>

    suspend fun getNote(id: Long): AppResult<Note, NoteError>

    /** Crea la nota si `id` es [Note.NEW_ID] o la actualiza si ya existe; no toca sus imágenes. Devuelve su id. */
    suspend fun saveNote(note: Note): AppResult<Long, NoteError>

    suspend fun deleteNote(id: Long): AppResult<Unit, NoteError>

    /** Asocia a la nota las imágenes ya copiadas en [paths]. */
    suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError>

    suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError>
}
