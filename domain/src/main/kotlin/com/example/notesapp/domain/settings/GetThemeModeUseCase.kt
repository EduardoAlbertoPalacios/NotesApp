package com.example.notesapp.domain.settings

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observa el tema de la app, con [ThemeMode.DEFAULT] si no hay ninguno disponible. */
class GetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> =
        repository.observeThemeMode().map { it ?: ThemeMode.DEFAULT }
}
