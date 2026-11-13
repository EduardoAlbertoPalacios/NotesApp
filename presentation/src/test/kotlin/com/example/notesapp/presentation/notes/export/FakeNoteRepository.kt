package com.example.notesapp.presentation.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Repositorio que emite lo que la prueba indique y registra cualquier escritura, que exportar no debe hacer. */
class FakeNoteRepository : NoteRepository {

    private val notes = MutableSharedFlow<AppResult<List<Note>, NoteError>>(replay = 1)

    /** Nombre de cada método de escritura llamado, en orden. */
    val writes = mutableListOf<String>()

    suspend fun emit(result: AppResult<List<Note>, NoteError>) = notes.emit(result)

    suspend fun emitNotes(vararg list: Note) = emit(AppResult.Success(list.toList()))

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = notes

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> = AppResult.Error(NoteError.NotFound)

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> {
        writes += "saveNote"
        return AppResult.Error(NoteError.Storage)
    }

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> {
        writes += "deleteNote"
        return AppResult.Error(NoteError.Storage)
    }

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> {
        writes += "addImages"
        return AppResult.Error(NoteError.Storage)
    }

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> {
        writes += "removeImage"
        return AppResult.Error(NoteError.Storage)
    }

    override suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError> {
        writes += "saveNoteChanges"
        return AppResult.Error(NoteError.Storage)
    }

    override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> = AppResult.Success(emptySet())
}
