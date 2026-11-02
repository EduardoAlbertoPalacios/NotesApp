package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {

    val themeMode = MutableStateFlow<ThemeMode?>(null)
    val savedModes = mutableListOf<ThemeMode>()
    var failure: SettingsError? = null

    override fun observeThemeMode(): Flow<ThemeMode?> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> {
        failure?.let { return AppResult.Error(it) }
        savedModes += mode
        themeMode.value = mode
        return AppResult.Success(Unit)
    }
}
