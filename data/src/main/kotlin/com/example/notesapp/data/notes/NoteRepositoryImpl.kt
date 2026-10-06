package com.example.notesapp.data.notes

import android.database.SQLException
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.local.NoteDao
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteError
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
}
