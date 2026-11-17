package com.example.notesapp.domain.notes.trash

import com.example.notesapp.domain.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observa el plazo de la papelera, con [TrashRetention.DEFAULT] si no hay ninguno disponible (RF-38). */
class GetTrashRetentionUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<TrashRetention> =
        repository.observeTrashRetention().map { it ?: TrashRetention.DEFAULT }
}
