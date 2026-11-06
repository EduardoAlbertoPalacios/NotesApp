package com.example.notesapp.presentation.settings

import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.ThemeMode

sealed interface SettingsIntent {
    data object BackClicked : SettingsIntent
    data object ThemeRowClicked : SettingsIntent
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent
    data object ThemeSheetDismissed : SettingsIntent
    data object DefaultColorRowClicked : SettingsIntent
    data class DefaultColorOptionSelected(val color: NoteColor) : SettingsIntent
    data object DefaultColorApplied : SettingsIntent
    data object DefaultColorPickerDismissed : SettingsIntent
    data object NotesViewRowClicked : SettingsIntent
    data class NotesViewSelected(val view: NotesView) : SettingsIntent
    data object NotesViewSheetDismissed : SettingsIntent
    data object AutoSaveToggled : SettingsIntent
}
