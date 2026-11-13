package com.example.notesapp.presentation.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notesapp.presentation.R
import kotlinx.coroutines.launch

/**
 * @param exportedCount notas que exportó la pantalla Exportar notas al volver de ella, o nulo si no hay aviso
 * pendiente. Con [missingImageCount] mayor que cero se avisa además de las imágenes omitidas.
 * @param onExportResultShown se llama al pasar el resultado al ViewModel, para no repetir los avisos.
 */
@Composable
fun SettingsRoute(
    appVersion: String,
    onNavigateBack: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier,
    exportedCount: Int? = null,
    missingImageCount: Int = 0,
    onExportResultShown: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(R.string.settings_theme_save_error)
    val defaultColorErrorMessage = stringResource(R.string.settings_default_color_save_error)
    val notesViewErrorMessage = stringResource(R.string.notes_view_save_error)
    val autoSaveErrorMessage = stringResource(R.string.settings_auto_save_error)
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val currentOnNavigateToExport by rememberUpdatedState(onNavigateToExport)
    val currentOnExportResultShown by rememberUpdatedState(onExportResultShown)
    val context = LocalContext.current

    // El resultado de Exportar notas pasa al ViewModel, que emite los avisos (RF-20, RF-45); se marca como
    // recibido para que no se repita al girar.
    LaunchedEffect(exportedCount) {
        val count = exportedCount ?: return@LaunchedEffect
        viewModel.onIntent(SettingsIntent.ExportFinished(count, missingImageCount))
        currentOnExportResultShown()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> currentOnNavigateBack()
                SettingsEffect.NavigateToExport -> currentOnNavigateToExport()
                is SettingsEffect.ShowExportDone -> launch {
                    val count = effect.exportedCount
                    snackbarHostState.showSnackbar(context.resources.getQuantityString(R.plurals.export_done, count, count))
                }
                SettingsEffect.ShowExportMissingImages ->
                    launch { snackbarHostState.showSnackbar(context.getString(R.string.export_missing_images)) }
                // En su propia corrutina: esperar al aviso retrasaría el siguiente efecto, como volver.
                SettingsEffect.ShowThemeSaveError -> launch { snackbarHostState.showSnackbar(saveErrorMessage) }
                SettingsEffect.ShowDefaultColorSaveError -> launch { snackbarHostState.showSnackbar(defaultColorErrorMessage) }
                SettingsEffect.ShowNotesViewSaveError -> launch { snackbarHostState.showSnackbar(notesViewErrorMessage) }
                SettingsEffect.ShowAutoSaveSaveError -> launch { snackbarHostState.showSnackbar(autoSaveErrorMessage) }
            }
        }
    }

    SettingsScreen(
        state = state,
        appVersion = appVersion,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
