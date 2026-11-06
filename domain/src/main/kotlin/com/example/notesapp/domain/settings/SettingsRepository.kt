package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Tema elegido; `null` si no hay ninguno guardado o no se puede leer. */
    fun observeThemeMode(): Flow<ThemeMode?>

    /** Si no se puede guardar, devuelve el error pero [observeThemeMode] sigue emitiendo [mode] durante la sesión. */
    suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError>

    /** Color predeterminado de las notas nuevas; `null` si no hay ninguno guardado o no se puede leer. */
    fun observeDefaultNoteColor(): Flow<NoteColor?>

    /** Si no se puede guardar, devuelve el error pero [observeDefaultNoteColor] sigue emitiendo [color] durante la sesión. */
    suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError>

    /** Vista de Mis notas; `null` si no hay ninguna guardada o no se puede leer. */
    fun observeNotesView(): Flow<NotesView?>

    /** Si no se puede guardar, devuelve el error pero [observeNotesView] sigue emitiendo [view] durante la sesión. */
    suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError>

    /** Si el guardado automático está encendido; `null` si no hay valor guardado o no se puede leer. */
    fun observeAutoSaveEnabled(): Flow<Boolean?>

    /** Si no se puede guardar, devuelve el error pero [observeAutoSaveEnabled] sigue emitiendo [enabled] durante la sesión. */
    suspend fun setAutoSaveEnabled(enabled: Boolean): AppResult<Unit, SettingsError>
}
