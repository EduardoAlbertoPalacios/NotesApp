package com.example.notesapp.presentation.settings

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
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
    private val defaultNoteColor = MutableSharedFlow<NoteColor?>(replay = 1)
    val savedDefaultNoteColors = mutableListOf<NoteColor>()
    var defaultNoteColorSaveResult: AppResult<Unit, SettingsError> = AppResult.Success(Unit)
    private val notesView = MutableSharedFlow<NotesView?>(replay = 1)
    val savedNotesViews = mutableListOf<NotesView>()
    var notesViewSaveResult: AppResult<Unit, SettingsError> = AppResult.Success(Unit)

    suspend fun emitStored(mode: ThemeMode?) = themeMode.emit(mode)

    suspend fun emitStoredDefaultNoteColor(color: NoteColor?) = defaultNoteColor.emit(color)

    suspend fun emitStoredNotesView(view: NotesView?) = notesView.emit(view)

    override fun observeThemeMode(): Flow<ThemeMode?> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> {
        savedModes += mode
        themeMode.emit(mode)
        return saveResult
    }

    override fun observeDefaultNoteColor(): Flow<NoteColor?> = defaultNoteColor

    override suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError> {
        savedDefaultNoteColors += color
        defaultNoteColor.emit(color)
        return defaultNoteColorSaveResult
    }

    override fun observeNotesView(): Flow<NotesView?> = notesView

    override suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError> {
        savedNotesViews += view
        notesView.emit(view)
        return notesViewSaveResult
    }
}
