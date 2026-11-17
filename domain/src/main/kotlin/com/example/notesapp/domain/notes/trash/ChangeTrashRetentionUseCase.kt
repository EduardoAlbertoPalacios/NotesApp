package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.NoteRepository
import com.example.notesapp.domain.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Resultado de cambiar el plazo de la papelera. */
sealed interface TrashRetentionChange {
    /** El plazo se guardó; las notas que ya lo cumplían se borraron. */
    data object Applied : TrashRetentionChange

    /** El plazo es más corto y borraría [count] notas: no se cambió nada hasta que se confirme. */
    data class NeedsConfirmation(val count: Int) : TrashRetentionChange

    /**
     * No se pudo guardar el plazo, o no se pudo leer la papelera para saber qué notas borraría uno más corto:
     * se mantiene el anterior y no se borró ninguna nota.
     */
    data object Failed : TrashRetentionChange
}

/**
 * Cambia el plazo de la papelera (RF-40 a RF-46). Un plazo más corto que dejaría notas vencidas pide
 * confirmación antes de cambiar nada, salvo que ya esté [confirmed]. El plazo se guarda antes de borrar, y
 * solo se borra si se guardó. Si el borrado falla, el plazo queda aplicado: esas notas ya no se muestran
 * (RF-48) y se borrarán la próxima vez que se abra la app.
 */
class ChangeTrashRetentionUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val getTrashRetention: GetTrashRetentionUseCase,
    private val deleteTrashedNotes: DeleteTrashedNotesUseCase,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(retention: TrashRetention, confirmed: Boolean = false): TrashRetentionChange {
        val current = getTrashRetention().first()
        val shorter = TrashRules.isShorter(retention, current)
        val expired = if (shorter) {
            when (val trash = noteRepository.observeTrash().first()) {
                is AppResult.Success -> TrashRules.expiredBy(trash.data, retention, timeProvider.now())
                // Sin saber qué notas borraría, no se puede pedir confirmación: no se cambia nada.
                is AppResult.Error -> return TrashRetentionChange.Failed
            }
        } else {
            emptyList()
        }
        if (expired.isNotEmpty() && !confirmed) return TrashRetentionChange.NeedsConfirmation(expired.size)
        if (settingsRepository.setTrashRetention(retention) is AppResult.Error) return TrashRetentionChange.Failed
        deleteTrashedNotes(expired)
        return TrashRetentionChange.Applied
    }
}
