package com.example.notesapp.presentation.notes.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NoteListRoute(
    onNavigateToNote: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToNote by rememberUpdatedState(onNavigateToNote)
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToNote -> currentOnNavigateToNote(effect.id)
                NoteListEffect.NavigateToSettings -> currentOnNavigateToSettings()
            }
        }
    }

    NoteListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
