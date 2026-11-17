package com.example.notesapp.presentation.notes.list

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
    /** Nota que el editor acaba de mover a la papelera; nula si no hay aviso pendiente. */
    movedToTrashNoteId: Long? = null,
    /** Se llama cuando el aviso de [movedToTrashNoteId] ya se pidió, para no repetirlo. */
    onMovedToTrashShown: () -> Unit = {},
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnNavigateToNote by rememberUpdatedState(onNavigateToNote)
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)
    val snackbarHostState = remember { SnackbarHostState() }
    val notesViewErrorMessage = stringResource(R.string.notes_view_save_error)
    val movedToTrashMessage = stringResource(R.string.note_list_moved_to_trash)
    val undoLabel = stringResource(R.string.note_list_undo)
    val restoreErrorMessage = stringResource(R.string.note_list_restore_error)
    val currentOnMovedToTrashShown by rememberUpdatedState(onMovedToTrashShown)

    LaunchedEffect(movedToTrashNoteId) {
        movedToTrashNoteId?.let { id ->
            viewModel.onIntent(NoteListIntent.NoteMovedToTrash(id))
            currentOnMovedToTrashShown()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NoteListEffect.NavigateToNote -> currentOnNavigateToNote(effect.id, effect.category)
                NoteListEffect.NavigateToSettings -> currentOnNavigateToSettings()
                // En su propia corrutina: esperar al aviso retrasaría el siguiente efecto, como abrir una nota.
                NoteListEffect.ShowNotesViewSaveError -> launch { snackbarHostState.showSnackbar(notesViewErrorMessage) }
                is NoteListEffect.ShowMovedToTrash -> launch {
                    // Cada aviso sustituye al anterior: «Deshacer» actúa solo sobre la última nota (spec 012).
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = movedToTrashMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(NoteListIntent.UndoMoveToTrashClicked(effect.id))
                    }
                }
                NoteListEffect.ShowRestoreError -> launch { snackbarHostState.showSnackbar(restoreErrorMessage) }
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
