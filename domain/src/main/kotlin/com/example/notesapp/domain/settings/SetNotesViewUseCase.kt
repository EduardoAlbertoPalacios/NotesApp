package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/** Guarda la vista elegida para Mis notas. */
class SetNotesViewUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(view: NotesView): AppResult<Unit, SettingsError> =
        repository.setNotesView(view)
}
