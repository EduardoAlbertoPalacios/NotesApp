package com.example.notesapp.presentation.help

sealed interface HelpEffect {
    data object NavigateBack : HelpEffect
}
