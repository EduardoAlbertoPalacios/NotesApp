package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Tema elegido; `null` si no hay ninguno guardado o no se puede leer. */
    fun observeThemeMode(): Flow<ThemeMode?>

    /** Si no se puede guardar, devuelve el error pero [observeThemeMode] sigue emitiendo [mode] durante la sesión. */
    suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError>
}
