package com.example.notesapp.presentation.settings

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.SettingsRepository
import com.example.notesapp.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Sin nada emitido hasta [emitStored], para simular la lectura pendiente. Como el real, un fallo al guardar mantiene el tema. */
class FakeSettingsRepository : SettingsRepository {

    private val themeMode = MutableSharedFlow<ThemeMode?>(replay = 1)
    val savedModes = mutableListOf<ThemeMode>()
    var saveResult: AppResult<Unit, SettingsError> = AppResult.Success(Unit)

    suspend fun emitStored(mode: ThemeMode?) = themeMode.emit(mode)

    override fun observeThemeMode(): Flow<ThemeMode?> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> {
        savedModes += mode
        themeMode.emit(mode)
        return saveResult
    }
}
