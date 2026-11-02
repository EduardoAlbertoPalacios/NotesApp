package com.example.notesapp.presentation.settings

import com.example.notesapp.domain.settings.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val isThemeSheetVisible: Boolean = false,
)
