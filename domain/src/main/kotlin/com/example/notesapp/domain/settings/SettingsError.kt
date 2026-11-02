package com.example.notesapp.domain.settings

sealed interface SettingsError {
    data object Storage : SettingsError
}
