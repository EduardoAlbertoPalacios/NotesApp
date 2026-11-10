package com.example.notesapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.GetAutoSaveEnabledUseCase
import com.example.notesapp.domain.settings.GetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.GetNotesViewUseCase
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SetAutoSaveEnabledUseCase
import com.example.notesapp.domain.settings.SetDefaultNoteColorUseCase
import com.example.notesapp.domain.settings.SetNotesViewUseCase
import com.example.notesapp.domain.settings.SetThemeModeUseCase
import com.example.notesapp.domain.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    getDefaultNoteColor: GetDefaultNoteColorUseCase,
    private val setDefaultNoteColor: SetDefaultNoteColorUseCase,
    getNotesView: GetNotesViewUseCase,
    private val setNotesView: SetNotesViewUseCase,
    getAutoSaveEnabled: GetAutoSaveEnabledUseCase,
    private val setAutoSaveEnabled: SetAutoSaveEnabledUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        getThemeMode()
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
        getDefaultNoteColor()
            .onEach { color -> _state.update { it.copy(defaultNoteColor = color) } }
            .launchIn(viewModelScope)
        getNotesView()
            .onEach { view -> _state.update { it.copy(notesView = view) } }
            .launchIn(viewModelScope)
        getAutoSaveEnabled()
            .onEach { enabled -> _state.update { it.copy(isAutoSaveEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.BackClicked -> send(SettingsEffect.NavigateBack)
            SettingsIntent.ThemeRowClicked -> _state.update { it.copy(isThemeSheetVisible = true) }
            SettingsIntent.ThemeSheetDismissed -> _state.update { it.copy(isThemeSheetVisible = false) }
            is SettingsIntent.ThemeSelected -> selectTheme(intent.mode)
            SettingsIntent.DefaultColorRowClicked -> _state.update { it.copy(defaultColorPicker = it.defaultNoteColor) }
            is SettingsIntent.DefaultColorOptionSelected -> _state.update { it.copy(defaultColorPicker = intent.color) }
            SettingsIntent.DefaultColorApplied -> applyDefaultColor()
            SettingsIntent.DefaultColorPickerDismissed -> _state.update { it.copy(defaultColorPicker = null) }
            SettingsIntent.NotesViewRowClicked -> _state.update { it.copy(isNotesViewSheetVisible = true) }
            SettingsIntent.NotesViewSheetDismissed -> _state.update { it.copy(isNotesViewSheetVisible = false) }
            is SettingsIntent.NotesViewSelected -> selectNotesView(intent.view)
            SettingsIntent.AutoSaveToggled -> toggleAutoSave()
        }
    }

    private fun selectTheme(mode: ThemeMode) {
        if (mode == _state.value.themeMode) return
        viewModelScope.launch {
            if (setThemeMode(mode) is AppResult.Error) _effects.send(SettingsEffect.ShowThemeSaveError)
        }
    }

    /** Cierra el selector y guarda la opción solo si cambió; así, aplicar el mismo color no hace nada más. */
    private fun applyDefaultColor() {
        val current = _state.value
        val picked: NoteColor = current.defaultColorPicker ?: return
        _state.update { it.copy(defaultColorPicker = null) }
        if (picked == current.defaultNoteColor) return
        viewModelScope.launch {
            if (setDefaultNoteColor(picked) is AppResult.Error) _effects.send(SettingsEffect.ShowDefaultColorSaveError)
        }
    }

    private fun selectNotesView(view: NotesView) {
        if (view == _state.value.notesView) return
        viewModelScope.launch {
            if (setNotesView(view) is AppResult.Error) _effects.send(SettingsEffect.ShowNotesViewSaveError)
        }
    }

    private fun toggleAutoSave() {
        val enabled = !_state.value.isAutoSaveEnabled
        viewModelScope.launch {
            if (setAutoSaveEnabled(enabled) is AppResult.Error) _effects.send(SettingsEffect.ShowAutoSaveSaveError)
        }
    }

    private fun send(effect: SettingsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
