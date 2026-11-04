package com.example.notesapp.presentation.notes.list

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.presentation.R
import kotlinx.coroutines.launch

@Composable
fun NoteListRoute(
    onNavigateToNote: (id: Long?, category: NoteCategory?) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToNote by rememberUpdatedState(onNavigateToNote)
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)
    val snackbarHostState = remember { SnackbarHostState() }
    val notesViewErrorMessage = stringResource(R.string.notes_view_save_error)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToNote -> currentOnNavigateToNote(effect.id, effect.category)
                NoteListEffect.NavigateToSettings -> currentOnNavigateToSettings()
                // En su propia corrutina: esperar al aviso retrasaría el siguiente efecto, como abrir una nota.
                NoteListEffect.ShowNotesViewSaveError -> launch { snackbarHostState.showSnackbar(notesViewErrorMessage) }
            }
        }
    }

    NoteListScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
