package com.example.notesapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NoteDao {
    @Query("SELECT * FROM notes")
    fun observeAll(): Flow<List<NoteEntity>>

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>)
}
