package com.example.notesapp.presentation.notes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.notes.common.asText
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

object NoteEditorTestTags {
    const val BACK = "note_editor_back"
    const val PIN = "note_editor_pin"
    const val MORE = "note_editor_more"
    const val DELETE = "note_editor_delete"
    const val TITLE = "note_editor_title"
    const val CONTENT = "note_editor_content"
    const val SAVE_STATUS = "note_editor_save_status"
    const val LOADING = "note_editor_loading"
    const val LOAD_ERROR = "note_editor_load_error"
}

@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Incluye el teclado: el contenido y el estado de guardado quedan por encima de él.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteEditorTopBar(
                isPinned = state.isPinned,
                showNoteActions = !state.isLoading && state.loadError == null,
                onIntent = onIntent,
            )
            when {
                state.isLoading -> LoadingState(modifier = Modifier.weight(1f))
                state.loadError != null -> LoadErrorState(error = state.loadError, modifier = Modifier.weight(1f))
                else -> {
                    NoteContent(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                    SaveStatusRow(status = state.saveStatus)
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (state.isDeleteDialogVisible) {
        DeleteNoteDialog(onIntent = onIntent)
    }
}

@Composable
private fun NoteEditorTopBar(
    isPinned: Boolean,
    showNoteActions: Boolean,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NotesTheme.sizes.topBarHeight)
            .padding(horizontal = NotesTheme.spacing.l),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorIconButton(
            iconRes = R.drawable.ic_arrow_left,
            contentDescription = stringResource(R.string.note_editor_back),
            onClick = { onIntent(NoteEditorIntent.BackClicked) },
            modifier = Modifier.testTag(NoteEditorTestTags.BACK),
        )
        if (showNoteActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xxs)) {
                EditorIconButton(
                    iconRes = R.drawable.ic_pin_outline,
                    contentDescription = stringResource(
                        if (isPinned) R.string.note_editor_unpin else R.string.note_editor_pin,
                    ),
                    onClick = { onIntent(NoteEditorIntent.PinClicked) },
                    modifier = Modifier.testTag(NoteEditorTestTags.PIN),
                    isActive = isPinned,
                )
                MoreOptions(onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun MoreOptions(
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        EditorIconButton(
            iconRes = R.drawable.ic_more,
            contentDescription = stringResource(R.string.note_editor_more_options),
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(NoteEditorTestTags.MORE),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.note_editor_delete)) },
                onClick = {
                    isExpanded = false
                    onIntent(NoteEditorIntent.DeleteClicked)
                },
                modifier = Modifier.testTag(NoteEditorTestTags.DELETE),
            )
        }
    }
}

@Composable
private fun EditorIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(NotesTheme.sizes.iconAction),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = contentDescription)
    }
}

@Composable
private fun NoteContent(
    state: NoteEditorUiState,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    val colors = MaterialTheme.colorScheme
    val titleFocusRequester = remember { FocusRequester() }
    // En una nota nueva el cursor empieza en el título para escribir de inmediato.
    LaunchedEffect(Unit) {
        if (state.isNewNote) titleFocusRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = spacing.screen,
                end = spacing.screen,
                top = spacing.xxl,
                bottom = spacing.screen
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.screen),
    ) {
        state.date?.let { date ->
            Text(
                text = date.asText(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        EditorTextField(
            value = state.title,
            onValueChange = { onIntent(NoteEditorIntent.TitleChanged(it)) },
            placeholder = stringResource(R.string.note_editor_title_placeholder),
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .focusRequester(titleFocusRequester)
                .testTag(NoteEditorTestTags.TITLE),
        )
        EditorTextField(
            value = state.content,
            onValueChange = { onIntent(NoteEditorIntent.ContentChanged(it)) },
            placeholder = stringResource(R.string.note_editor_content_placeholder),
            textStyle = NotesTheme.typography.editorBody,
            modifier = Modifier.testTag(NoteEditorTestTags.CONTENT),
        )
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = placeholder },
        textStyle = textStyle.copy(color = colors.onBackground),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle, color = colors.onSurfaceVariant)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun SaveStatusRow(
    status: SaveStatus,
    modifier: Modifier = Modifier,
) {
    if (status == SaveStatus.Idle) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NotesTheme.spacing.screen, vertical = NotesTheme.spacing.l)
            .testTag(NoteEditorTestTags.SAVE_STATUS),
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xsm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == SaveStatus.Saved) {
            Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = color)
        }
        Text(
            text = stringResource(
                if (status == SaveStatus.Saved) R.string.note_editor_saved else R.string.note_editor_saving,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun DeleteNoteDialog(
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { onIntent(NoteEditorIntent.DeleteDismissed) },
        confirmButton = {
            TextButton(onClick = { onIntent(NoteEditorIntent.DeleteConfirmed) }) {
                Text(text = stringResource(R.string.note_editor_delete_confirm))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = { onIntent(NoteEditorIntent.DeleteDismissed) }) {
                Text(text = stringResource(R.string.note_editor_delete_cancel))
            }
        },
        title = { Text(text = stringResource(R.string.note_editor_delete_title)) },
        text = { Text(text = stringResource(R.string.note_editor_delete_message)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(NoteEditorTestTags.LOADING),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LoadErrorState(
    error: LoadError,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(NotesTheme.spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                when (error) {
                    LoadError.NotFound -> R.string.note_editor_not_found
                    LoadError.Storage -> R.string.note_editor_load_error
                },
            ),
            modifier = Modifier.testTag(NoteEditorTestTags.LOAD_ERROR),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteEditorScreenPreview() {
    NotesAppTheme {
        NoteEditorScreen(
            state = NoteEditorUiState(
                isNewNote = false,
                title = "Reunión de equipo",
                content = "Ideas y próximos pasos para el nuevo lanzamiento. Menos ruido, más foco.\n\n" +
                    "Una idea para recordar: dejar espacio para lo importante.",
                isPinned = true,
                date = NoteDateLabel.Today("9:38"),
                saveStatus = SaveStatus.Saved,
            ),
            onIntent = {},
        )
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteEditorScreenNewNotePreview() {
    NotesAppTheme {
        NoteEditorScreen(state = NoteEditorUiState(), onIntent = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteEditorScreenNotFoundPreview() {
    NotesAppTheme {
        NoteEditorScreen(state = NoteEditorUiState(loadError = LoadError.NotFound), onIntent = {})
    }
}
