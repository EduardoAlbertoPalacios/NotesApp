package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

internal class FakeNoteDao : NoteDao {
    private val notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    var failure: Throwable? = null

    override fun observeAll(): Flow<List<NoteEntity>> = failure?.let { error -> flow { throw error } } ?: notes

    override suspend fun insertAll(notes: List<NoteEntity>) {
        this.notes.update { it + notes }
    }
}
