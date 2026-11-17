package com.example.notesapp.presentation.notes.trash

import androidx.annotation.StringRes

sealed interface TrashEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : TrashEffect
    data object NavigateBack : TrashEffect
}
