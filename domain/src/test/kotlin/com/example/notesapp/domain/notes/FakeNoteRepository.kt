package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.trash.TrashedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNoteRepository(
    initial: AppResult<List<Note>, NoteError> = AppResult.Success(emptyList()),
) : NoteRepository {
    val notes = MutableStateFlow(initial)
    val saved = mutableListOf<Note>()
    val deletedIds = mutableListOf<Long>()
    val addedImages = mutableListOf<Pair<Long, List<String>>>()
    val removedImageIds = mutableListOf<Long>()

    /** Llamadas a [saveNoteChanges], en orden. */
    val savedChanges = mutableListOf<SavedChanges>()

    /** Rutas que devuelve [getImagePaths]. */
    var imagePaths: Set<String> = emptySet()

    /** Falla en todas las operaciones. */
    var failure: NoteError? = null
    var deleteFailure: NoteError? = null
    var addImagesFailure: NoteError? = null
    var removeImageFailure: NoteError? = null
    var saveNoteChangesFailure: NoteError? = null
    var getImagePathsFailure: NoteError? = null

    /** Notas de la papelera que emite [observeTrash]; [deleteNotes] quita de aquí las que borra. */
    val trash = MutableStateFlow<AppResult<List<TrashedNote>, NoteError>>(AppResult.Success(emptyList()))

    /** Llamadas a [moveToTrash] como (id, trashedAt), en orden. */
    val movedToTrash = mutableListOf<Pair<Long, Long>>()
    val restoredFromTrashIds = mutableListOf<Long>()

    /** Llamadas a [deleteNotes], en orden. */
    val deletedNoteIdBatches = mutableListOf<List<Long>>()

    /** Veces que se llamó a [observeTrash]. */
    var observeTrashCalls = 0
        private set
    var moveToTrashFailure: NoteError? = null
    var restoreFromTrashFailure: NoteError? = null
    var deleteNotesFailure: NoteError? = null
    private var nextId = 100L
    private var nextImageId = 500L

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
        (failure ?: deleteFailure)?.let { return AppResult.Error(it) }
        deletedIds += id
        return AppResult.Success(Unit)
    }

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> {
        (failure ?: addImagesFailure)?.let { return AppResult.Error(it) }
        addedImages += noteId to paths
        return AppResult.Success(paths.map { NoteImage(id = nextImageId++, path = it) })
    }

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> {
        (failure ?: removeImageFailure)?.let { return AppResult.Error(it) }
        removedImageIds += imageId
        return AppResult.Success(Unit)
    }

    override suspend fun saveNoteChanges(
        note: Note,
        addedPaths: List<String>,
        removedImageIds: List<Long>,
    ): AppResult<Note, NoteError> {
        (failure ?: saveNoteChangesFailure)?.let { return AppResult.Error(it) }
        savedChanges += SavedChanges(note, addedPaths, removedImageIds)
        val id = if (note.id == Note.NEW_ID) nextId++ else note.id
        val images = note.images.filterNot { it.id in removedImageIds } +
            addedPaths.map { NoteImage(id = nextImageId++, path = it) }
        return AppResult.Success(note.copy(id = id, images = images))
    }

    override suspend fun getImagePaths(): AppResult<Set<String>, NoteError> {
        (failure ?: getImagePathsFailure)?.let { return AppResult.Error(it) }
        return AppResult.Success(imagePaths)
    }

    override suspend fun moveToTrash(id: Long, trashedAt: Long): AppResult<Unit, NoteError> {
        (failure ?: moveToTrashFailure)?.let { return AppResult.Error(it) }
        movedToTrash += id to trashedAt
        return AppResult.Success(Unit)
    }

    override suspend fun restoreFromTrash(id: Long): AppResult<Unit, NoteError> {
        (failure ?: restoreFromTrashFailure)?.let { return AppResult.Error(it) }
        restoredFromTrashIds += id
        return AppResult.Success(Unit)
    }

    override fun observeTrash(): Flow<AppResult<List<TrashedNote>, NoteError>> {
        observeTrashCalls++
        return trash
    }

    override suspend fun deleteNotes(ids: List<Long>): AppResult<Unit, NoteError> {
        (failure ?: deleteNotesFailure)?.let { return AppResult.Error(it) }
        deletedNoteIdBatches += ids
        (trash.value as? AppResult.Success)?.let { current ->
            trash.value = AppResult.Success(current.data.filterNot { it.note.id in ids })
        }
        return AppResult.Success(Unit)
    }

    data class SavedChanges(
        val note: Note,
        val addedPaths: List<String>,
        val removedImageIds: List<Long>,
    )
}
