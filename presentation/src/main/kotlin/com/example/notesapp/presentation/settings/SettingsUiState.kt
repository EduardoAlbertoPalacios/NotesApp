package com.example.notesapp.presentation.settings

import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val isThemeSheetVisible: Boolean = false,
    val defaultNoteColor: NoteColor = NoteColor.DEFAULT,
    /** Opción marcada en el selector de color, sin aplicar todavía; nula si la hoja está cerrada. */
    val defaultColorPicker: NoteColor? = null,
)
