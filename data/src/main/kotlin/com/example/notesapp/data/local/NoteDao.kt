package com.example.notesapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
}
