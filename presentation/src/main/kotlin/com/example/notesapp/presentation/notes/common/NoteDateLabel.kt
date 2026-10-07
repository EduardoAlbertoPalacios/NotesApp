package com.example.notesapp.presentation.notes.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notesapp.presentation.R

sealed interface NoteDateLabel {
    data class Today(val time: String) : NoteDateLabel
    data object Yesterday : NoteDateLabel
    data class Date(val dayMonth: String) : NoteDateLabel
}

@Composable
fun NoteDateLabel.asText(): String = when (this) {
    is NoteDateLabel.Today -> stringResource(R.string.note_date_today, time)
    NoteDateLabel.Yesterday -> stringResource(R.string.note_date_yesterday)
    is NoteDateLabel.Date -> dayMonth
}
