package com.example.notesapp.data.notes

import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

internal class FakeNoteDao : NoteDao {
    private val notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    private var nextId = 1L
    var failure: Throwable? = null

    val stored: List<NoteEntity> get() = notes.value

    override fun observeAll(): Flow<List<NoteEntity>> = failure?.let { error -> flow { throw error } } ?: notes

    override suspend fun getById(id: Long): NoteEntity? {
        failure?.let { throw it }
        return notes.value.firstOrNull { it.id == id }
    }

    override suspend fun insert(note: NoteEntity): Long {
        failure?.let { throw it }
        val id = nextId++
        notes.update { it + note.copy(id = id) }
        return id
    }

    override suspend fun insertAll(notes: List<NoteEntity>) {
        notes.forEach { insert(it) }
    }

    override suspend fun update(note: NoteEntity): Int {
        failure?.let { throw it }
        if (notes.value.none { it.id == note.id }) return 0
        notes.update { list -> list.map { if (it.id == note.id) note else it } }
        return 1
    }

    override suspend fun deleteById(id: Long) {
        failure?.let { throw it }
        notes.update { list -> list.filterNot { it.id == id } }
    }
}
