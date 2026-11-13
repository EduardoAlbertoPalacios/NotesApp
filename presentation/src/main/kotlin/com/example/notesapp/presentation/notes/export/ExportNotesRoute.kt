package com.example.notesapp.presentation.notes.export

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * @param onExported se llama al guardar el .zip, con las notas exportadas y las imágenes omitidas, para que
 * Ajustes muestre los avisos al volver.
 */
@Composable
fun ExportNotesRoute(
    onNavigateBack: () -> Unit,
    onExported: (exportedCount: Int, missingImageCount: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportNotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnExported by rememberUpdatedState(onExported)

    // Selector de documentos del sistema: el usuario elige dónde se crea el .zip, sin permisos de almacenamiento.
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP_MIME_TYPE)) { uri ->
        viewModel.onIntent(ExportNotesIntent.DestinationPicked(uri?.toString()))
    }

    BackHandler { viewModel.onIntent(ExportNotesIntent.BackClicked) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportNotesEffect.OpenDocumentPicker -> documentPicker.launch(effect.suggestedName)
                is ExportNotesEffect.Finished -> currentOnExported(effect.exportedCount, effect.missingImageCount)
                // En su propia corrutina: esperar al aviso retrasaría el siguiente efecto.
                is ExportNotesEffect.ShowMessage ->
                    launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                ExportNotesEffect.NavigateBack -> currentOnNavigateBack()
            }
        }
    }

    ExportNotesScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

private const val ZIP_MIME_TYPE = "application/zip"
