package com.example.notesapp.presentation.settings

import com.example.notesapp.domain.settings.ThemeMode

sealed interface SettingsIntent {
    data object BackClicked : SettingsIntent
    data object ThemeRowClicked : SettingsIntent
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent
    data object ThemeSheetDismissed : SettingsIntent
}
