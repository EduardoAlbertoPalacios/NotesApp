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
    /** Notas fuera de la papelera. */
    @Transaction
    @Query("SELECT * FROM notes WHERE trashed_at IS NULL")
    fun observeAll(): Flow<List<NoteWithImages>>

    /** Notas de la papelera. */
    @Transaction
    @Query("SELECT * FROM notes WHERE trashed_at IS NOT NULL")
    fun observeTrashed(): Flow<List<NoteWithImages>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteWithImages?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>)

    /**
     * Devuelve cuántas filas se actualizaron (0 si la nota ya no existe). No toca `trashed_at`: ver
     * [NoteContentUpdate].
     */
    @Update(entity = NoteEntity::class)
    suspend fun updateContent(note: NoteContentUpdate): Int

    suspend fun update(note: NoteEntity): Int = updateContent(note.toContentUpdate())

    /** Mete la nota en la papelera con [trashedAt], o la saca con `null`. Devuelve cuántas filas cambiaron. */
    @Query("UPDATE notes SET trashed_at = :trashedAt WHERE id = :id")
    suspend fun setTrashedAt(id: Long, trashedAt: Long?): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Borra las notas [ids] en una sola sentencia; sus imágenes se borran en cascada. Ver [deleteAllByIds]. */
    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Borra las notas [ids] todo o nada, en bloques: SQLite anterior a 3.32 (Android 11 o inferior) admite como
     * máximo 999 parámetros por sentencia.
     */
    @Transaction
    suspend fun deleteAllByIds(ids: List<Long>) {
        ids.chunked(MAX_IDS_PER_STATEMENT).forEach { deleteByIds(it) }
    }

    companion object {
        /** Por debajo del límite de 999 parámetros de SQLite antiguo. */
        const val MAX_IDS_PER_STATEMENT = 900
    }

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
