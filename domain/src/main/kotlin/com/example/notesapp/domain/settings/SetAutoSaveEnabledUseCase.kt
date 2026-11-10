package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/** Guarda si el guardado automático está encendido. */
class SetAutoSaveEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit, SettingsError> =
        repository.setAutoSaveEnabled(enabled)
}
