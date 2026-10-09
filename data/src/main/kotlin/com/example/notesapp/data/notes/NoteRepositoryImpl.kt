package com.example.notesapp.data.notes

import android.database.SQLException
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.data.local.NoteImageEntity
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {

    override fun observeNotes(): Flow<AppResult<List<Note>, NoteError>> =
        noteDao.observeAll()
            .map<_, AppResult<List<Note>, NoteError>> { entities ->
                AppResult.Success(entities.map { it.toDomain() })
            }
            .catch { error ->
                if (error is SQLException) emit(AppResult.Error(NoteError.Storage)) else throw error
            }

    override suspend fun getNote(id: Long): AppResult<Note, NoteError> = storage {
        noteDao.getById(id)?.let { AppResult.Success(it.toDomain()) } ?: AppResult.Error(NoteError.NotFound)
    }

    override suspend fun saveNote(note: Note): AppResult<Long, NoteError> = storage {
        if (note.id == Note.NEW_ID) {
            AppResult.Success(noteDao.insert(note.toEntity()))
        } else if (noteDao.update(note.toEntity()) > 0) {
            AppResult.Success(note.id)
        } else {
            AppResult.Error(NoteError.NotFound)
        }
    }

    override suspend fun deleteNote(id: Long): AppResult<Unit, NoteError> = storage {
        noteDao.deleteById(id)
        AppResult.Success(Unit)
    }

    override suspend fun addImages(noteId: Long, paths: List<String>): AppResult<List<NoteImage>, NoteError> =
        storage {
            val entities = paths.map { NoteImageEntity(noteId = noteId, path = it) }
            val ids = noteDao.insertImages(entities)
            AppResult.Success(entities.zip(ids) { entity, id -> entity.copy(id = id).toDomain() })
        }

    override suspend fun removeImage(imageId: Long): AppResult<Unit, NoteError> = storage {
        noteDao.deleteImage(imageId)
        AppResult.Success(Unit)
    }

    private inline fun <T> storage(block: () -> AppResult<T, NoteError>): AppResult<T, NoteError> =
        try {
            block()
        } catch (error: SQLException) {
            AppResult.Error(NoteError.Storage)
        }
}
