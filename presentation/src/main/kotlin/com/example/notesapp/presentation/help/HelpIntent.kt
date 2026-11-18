package com.example.notesapp.presentation.help

sealed interface HelpIntent {
    data class QueryChanged(val query: String) : HelpIntent
    data object ClearQueryClicked : HelpIntent
    data class FaqClicked(val id: Int) : HelpIntent
    data object BackClicked : HelpIntent
}
