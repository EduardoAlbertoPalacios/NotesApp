package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Borra las notas que cumplieron el plazo vigente, con sus imágenes; con «Nunca», ninguna (RF-49, RF-50). */
class PurgeExpiredTrashUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val getTrashRetention: GetTrashRetentionUseCase,
    private val deleteTrashedNotes: DeleteTrashedNotesUseCase,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(): AppResult<Unit, NoteError> {
        val retention = getTrashRetention().first()
        if (retention.days == null) return AppResult.Success(Unit)
        return when (val trash = repository.observeTrash().first()) {
            is AppResult.Success -> deleteTrashedNotes(TrashRules.expiredBy(trash.data, retention, timeProvider.now()))
            is AppResult.Error -> trash
        }
    }
}
