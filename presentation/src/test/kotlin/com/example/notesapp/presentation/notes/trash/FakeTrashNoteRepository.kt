package com.example.notesapp.presentation.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.notes.trash.TrashedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Repositorio centrado en la papelera: no emite nada hasta [emitTrash] o [emitTrashError], para simular la lectura
 * pendiente. Restaurar y borrar, si van bien, sacan las notas de la papelera y la vuelven a emitir, como el real.
 */
class FakeTrashNoteRepository : NoteRepository {

    private val trash = MutableSharedFlow<AppResult<List<TrashedNote>, NoteError>>(replay = 1)
    private var current: List<TrashedNote> = emptyList()

    var restoreResult: AppResult<Unit, NoteError> = AppResult.Success(Unit)
    var deleteResult: AppResult<Unit, NoteError> = AppResult.Success(Unit)
    var deleteNotesResult: AppResult<Unit, NoteError> = AppResult.Success(Unit)

    val restoredIds = mutableListOf<Long>()
    val deletedIds = mutableListOf<Long>()
    val deletedBatches = mutableListOf<List<Long>>()

    suspend fun emitTrash(vararg notes: TrashedNote) {
        current = notes.toList()
        trash.emit(AppResult.Success(current))
    }

    suspend fun emitTrashError() = trash.emit(AppResult.Error(NoteError.Storage))

    override fun observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>> = trash

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> =
        current.firstOrNull { it.note.id == id }?.let { AppResult.Success(it.note) }
            ?: AppResult.Error(NoteError.NotFound)

    override suspend fun restoreFromTrash(id: Long): AppResult<Unit, NoteError> {
        restoredIds += id
        return restoreResult.also { if (it is AppResult.Success) remove(listOf(id)) }
    }

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> {
        deletedIds += id
        return deleteResult.also { if (it is AppResult.Success) remove(listOf(id)) }
    }

    override suspend fun deleteNotes(ids: List<Long>): AppResult<Unit, NoteError> {
        deletedBatches += ids
        return deleteNotesResult.also { if (it is AppResult.Success) remove(ids) }
    }

    private suspend fun remove(ids: List<Long>) {
        current = current.filterNot { it.note.id in ids }
        trash.emit(AppResult.Success(current))
    }

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> = emptyFlow()

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
        AppResult.Error(NoteError.Storage)

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun moveToTrash(id: Long, trashedAt: Long): AppResult<Unit, NoteError> =
        AppResult.Error(NoteError.Storage)
}

/** Registra las rutas borradas. */
class RecordingImageStorage : ImageStorage {
    val deletedPaths = mutableListOf<String>()

    override suspend fun save(sourceUri: String): AppResult<String, NoteError> = AppResult.Error(NoteError.Storage)

    override suspend fun delete(path: String) {
        deletedPaths += path
    }

    override suspend fun listPaths(): Set<String> = emptySet()

    override suspend fun exists(path: String): Boolean = false
}
