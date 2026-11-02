package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import javax.inject.Inject

/** Guarda el tema elegido. */
class SetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode): AppResult<Unit, SettingsError> =
        repository.setThemeMode(mode)
}
