package com.example.notesapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.settings.GetThemeModeUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        getThemeMode()
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.BackClicked -> send(SettingsEffect.NavigateBack)
            SettingsIntent.ThemeRowClicked -> _state.update { it.copy(isThemeSheetVisible = true) }
            SettingsIntent.ThemeSheetDismissed -> _state.update { it.copy(isThemeSheetVisible = false) }
            is SettingsIntent.ThemeSelected -> selectTheme(intent.mode)
        }
    }

    private fun selectTheme(mode: ThemeMode) {
        if (mode == _state.value.themeMode) return
        viewModelScope.launch {
            if (setThemeMode(mode) is AppResult.Error) _effects.send(SettingsEffect.ShowThemeSaveError)
        }
    }

    private fun send(effect: SettingsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
