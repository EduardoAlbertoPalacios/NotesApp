package com.example.notesapp.presentation.settings

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object ShowThemeSaveError : SettingsEffect
    data object ShowDefaultColorSaveError : SettingsEffect
    data object ShowNotesViewSaveError : SettingsEffect
}
