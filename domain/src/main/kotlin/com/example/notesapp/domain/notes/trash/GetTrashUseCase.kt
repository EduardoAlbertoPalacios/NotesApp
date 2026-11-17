package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.result.map
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Notas de la papelera, de la que entró más recientemente a la más antigua, con los días que les quedan.
 * Las que ya cumplieron el plazo no se devuelven aunque todavía no se hayan borrado (RF-14, RF-16, RF-48).
 * Los días se calculan con la hora de cada emisión.
 */
class GetTrashUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val getTrashRetention: GetTrashRetentionUseCase,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(): Flow<AppResult<List<TrashEntry>, NoteError>> =
        combine(repository.observeTrash(), getTrashRetention()) { result, retention ->
            val now = timeProvider.now()
            result.map { notes ->
                notes
                    .filterNot { TrashRules.isExpired(it.trashedAt, retention, now) }
                    .sortedByDescending { it.trashedAt }
                    .map { TrashEntry(it, TrashRules.daysLeft(it.trashedAt, retention, now)) }
            }
        }
}
