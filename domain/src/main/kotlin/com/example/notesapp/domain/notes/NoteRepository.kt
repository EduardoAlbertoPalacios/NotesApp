package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.trash.TrashedNote
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** Notas fuera de la papelera. */
    fun observeNotes(): Flow<AppResult<List<Note>, NoteError>>

    suspend fun getNote(id: Long): AppResult<Note, NoteError>

    /** Crea la nota si `id` es [Note.NEW_ID] o la actualiza si ya existe; no toca sus imágenes. Devuelve su id. */
    suspend fun saveNote(note: Note): AppResult<Long, NoteError>

    /** Borra la nota definitivamente, esté o no en la papelera. No borra los archivos de sus imágenes. */
    suspend fun deleteNote(id: Long): AppResult<Unit, NoteError>

    /** Asocia a la nota las imágenes ya copiadas en [paths]. */
    suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError>

    suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError>

    /**
     * Guarda la nota, le asocia las imágenes ya copiadas en [addedPaths] y le quita las de [removedImageIds],
     * todo o nada. Devuelve la nota guardada con sus imágenes.
     */
    suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError>

    /** Rutas de todas las imágenes asociadas a alguna nota, incluidas las de la papelera. */
    suspend fun getImagePaths(): AppResult<Set<String>, NoteError>

    /** Mueve la nota a la papelera, marcándola con [trashedAt]; no cambia nada más de ella. */
    suspend fun moveToTrash(id: Long, trashedAt: Long): AppResult<Unit, NoteError>

    /** Saca la nota de la papelera tal como estaba, sin cambiar su fecha de edición. */
    suspend fun restoreFromTrash(id: Long): AppResult<Unit, NoteError>

    /** Notas de la papelera, sin orden definido. */
    fun observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>>

    /** Borra definitivamente las notas [ids], todo o nada. No borra los archivos de sus imágenes. */
    suspend fun deleteNotes(ids: List<Long>): AppResult<Unit, NoteError>
}
