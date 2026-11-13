package com.example.notesapp.presentation.settings

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToExport : SettingsEffect
    data class ShowExportDone(val exportedCount: Int) : SettingsEffect
    data object ShowExportMissingImages : SettingsEffect
    data object ShowThemeSaveError : SettingsEffect
    data object ShowDefaultColorSaveError : SettingsEffect
    data object ShowNotesViewSaveError : SettingsEffect
    data object ShowAutoSaveSaveError : SettingsEffect
}
