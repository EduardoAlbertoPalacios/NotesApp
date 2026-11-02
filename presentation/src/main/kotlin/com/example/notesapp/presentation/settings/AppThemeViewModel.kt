package com.example.notesapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.domain.settings.GetThemeModeUseCase
import com.example.notesapp.domain.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Tema de toda la app, para la actividad. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    getThemeMode: GetThemeModeUseCase,
) : ViewModel() {

    /** `null` hasta la primera lectura: mientras tanto la actividad no dibuja. */
    val themeMode: StateFlow<ThemeMode?> = getThemeMode()
        .map<ThemeMode, ThemeMode?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
