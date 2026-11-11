package com.example.notesapp.presentation.notes.editor

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import java.io.File

@Composable
fun NoteEditorRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    // Selector de fotos del sistema: no requiere permisos de almacenamiento.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.onIntent(NoteEditorIntent.ImagesPicked(uris.map { it.toString() }))
    }

    // El gesto de volver también guarda los cambios pendientes antes de salir.
    BackHandler { viewModel.onIntent(NoteEditorIntent.BackClicked) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NoteEditorEffect.NavigateBack -> currentOnNavigateBack()
                NoteEditorEffect.OpenImagePicker -> imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                is NoteEditorEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                is NoteEditorEffect.ShareNote -> try {
                    context.startActivity(shareIntent(context, effect))
                } catch (error: ActivityNotFoundException) {
                    snackbarHostState.showSnackbar(context.getString(R.string.note_editor_share_no_apps))
                }
            }
        }
    }

    // Sobre el color de la nota, los íconos de la barra de estado pasan a oscuros; al salir se restauran.
    val useDarkStatusBarIcons = state.isNoteVisible && state.color != NoteColor.NONE
    val window = LocalActivity.current?.window
    val view = LocalView.current
    DisposableEffect(window, useDarkStatusBarIcons) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        if (useDarkStatusBarIcons) controller?.isAppearanceLightStatusBars = true
        onDispose {
            if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }

    NoteEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Menú de compartir del sistema con el texto y las imágenes de [share]. Las imágenes viajan como URI del
 * `FileProvider` de la app, con permiso temporal de solo lectura; también van en `ClipData` para que el
 * permiso llegue a la app elegida a través del selector.
 */
private fun shareIntent(context: Context, share: NoteEditorEffect.ShareNote): Intent {
    // Debe coincidir con `android:authorities` del provider en el manifiesto de :app.
    val authority = "${context.packageName}.fileprovider"
    val uris = share.imagePaths.map { FileProvider.getUriForFile(context, authority, File(it)) }
    val send = Intent().apply {
        action = if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        // Con imágenes el tipo es `image/*` aunque haya texto: con `*/*` apps como WhatsApp solo toman el
        // texto. El texto viaja igual en EXTRA_TEXT y las apps lo usan como pie o cuerpo.
        type = if (uris.isEmpty()) "text/plain" else "image/*"
        share.text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        share.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        when (uris.size) {
            0 -> Unit
            1 -> putExtra(Intent.EXTRA_STREAM, uris.single())
            else -> putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }
        if (uris.isNotEmpty()) {
            clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return Intent.createChooser(send, null).apply {
        // El selector reenvía el permiso de lectura a la app elegida.
        if (uris.isNotEmpty()) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
