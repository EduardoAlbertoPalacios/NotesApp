package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import javax.inject.Inject

/** Guarda el color elegido para las notas nuevas. */
class SetDefaultNoteColorUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(color: NoteColor): AppResult<Unit, SettingsError> =
        repository.setDefaultNoteColor(color)
}
