package com.example.notesapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notesapp.domain.notes.Note
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes")
    fun observeAll(): Flow<List<NoteWithImages>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteWithImages?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>)

    /** Devuelve cuántas filas se actualizaron (0 si la nota ya no existe). */
    @Update
    suspend fun update(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertImages(images: List<NoteImageEntity>): List<Long>

    @Query("DELETE FROM note_images WHERE id = :id")
    suspend fun deleteImage(id: Long)

    @Query("SELECT path FROM note_images")
    suspend fun getImagePaths(): List<String>

    /**
     * Guarda la nota y cambia sus imágenes en una sola transacción, y la devuelve leída dentro de ella: así,
     * si la lectura falla, tampoco queda nada guardado y un reintento no la duplica. Devuelve `null` si la nota
     * no es nueva y ya no existe; en ese caso no cambia nada.
     */
    @Transaction
    suspend fun saveChanges(
        note: NoteEntity,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): NoteWithImages? {
        val id = when {
            note.id == Note.NEW_ID -> insert(note)
            update(note) > 0 -> note.id
            else -> return null
        }
        removedImageIds.forEach { deleteImage(it) }
        insertImages(addedPaths.map { NoteImageEntity(noteId = id, path = it) })
        return getById(id)
    }
}
