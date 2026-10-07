package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNoteRepository(
    initial: AppResult<List<Note>, NoteError> = AppResult.Success(emptyList()),
) : NoteRepository {
    val notes = MutableStateFlow(initial)
    val saved = mutableListOf<Note>()
    val deletedIds = mutableListOf<Long>()
    var failure: NoteError? = null
    private var nextId = 100L

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = notes

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> {
        failure?.let { return AppResult.Error(it) }
        val stored = (notes.value as? AppResult.Success)?.data.orEmpty()
        return stored.firstOrNull { it.id == id }?.let { AppResult.Success(it) }
            ?: AppResult.Error(NoteError.NotFound)
    }

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> {
        failure?.let { return AppResult.Error(it) }
        saved += note
        return AppResult.Success(if (note.id == Note.NEW_ID) nextId++ else note.id)
    }

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> {
        failure?.let { return AppResult.Error(it) }
        deletedIds += id
        return AppResult.Success(Unit)
    }
}
