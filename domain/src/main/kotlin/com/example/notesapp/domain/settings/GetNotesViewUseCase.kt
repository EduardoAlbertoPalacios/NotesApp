package com.example.notesapp.domain.settings

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observa la vista de Mis notas, con [NotesView.DEFAULT] si no hay ninguna disponible. */
class GetNotesViewUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<NotesView> =
        repository.observeNotesView().map { it ?: NotesView.DEFAULT }
}
