package com.example.notesapp.presentation.notes.export

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

internal object ExportNotesTestTags {
    const val BACK = "export_back"
    const val SELECTED_COUNT = "export_selected_count"
    const val SELECT_ALL = "export_select_all"
    const val LIST = "export_list"
    const val NOTE = "export_note_"
    const val EXPORT = "export_action"
    const val PROGRESS = "export_progress"
    const val EMPTY = "export_empty"
}

/** Selección de las notas que se exportan como PDF. No tiene diseño en Figma: usa los componentes de Ajustes. */
@Composable
fun ExportNotesScreen(
    state: ExportNotesUiState,
    onIntent: (ExportNotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExportTopBar(onBack = { onIntent(ExportNotesIntent.BackClicked) })
            when {
                state.isLoading -> Unit
                state.loadFailed -> CenteredMessage(R.string.export_load_error, Modifier.weight(1f))
                state.isEmpty -> CenteredMessage(R.string.export_empty, Modifier.weight(1f))
                else -> {
                    SelectionHeader(state = state, onIntent = onIntent)
                    NoteSelectionList(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                    ExportButton(
                        enabled = state.canExport,
                        isExporting = state.isExporting,
                        onClick = { onIntent(ExportNotesIntent.ExportClicked) },
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun ExportTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NotesTheme.sizes.topBarHeight)
            .padding(horizontal = NotesTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.s),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(NotesTheme.sizes.iconAction).testTag(ExportNotesTestTags.BACK),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.export_back),
                modifier = Modifier.size(NotesTheme.sizes.settingsBackIcon),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.export_title),
            style = NotesTheme.typography.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Aviso en lugar de la lista: sin notas o sin poder leerlas. */
@Composable
private fun CenteredMessage(@StringRes textRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(NotesTheme.spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.testTag(ExportNotesTestTags.EMPTY),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectionHeader(
    state: ExportNotesUiState,
    onIntent: (ExportNotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NotesTheme.spacing.screen, vertical = NotesTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.export_selected_count, state.selectedCount, state.selectedCount),
            modifier = Modifier.weight(1f).testTag(ExportNotesTestTags.SELECTED_COUNT),
            style = NotesTheme.typography.settingsSection,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = {
                onIntent(
                    if (state.allSelected) ExportNotesIntent.ClearSelectionClicked else ExportNotesIntent.SelectAllClicked,
                )
            },
            enabled = !state.isExporting,
            modifier = Modifier.testTag(ExportNotesTestTags.SELECT_ALL),
            // El color de acento del tema: el `primary` de Material apenas contrasta sobre el fondo claro.
            colors = ButtonDefaults.textButtonColors(contentColor = NotesTheme.accent.content),
        ) {
            Text(
                text = stringResource(
                    if (state.allSelected) R.string.export_clear_selection else R.string.export_select_all,
                ),
                style = NotesTheme.typography.settingsRowTitle,
            )
        }
    }
}

@Composable
private fun NoteSelectionList(
    state: ExportNotesUiState,
    onIntent: (ExportNotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val radius = NotesTheme.radii.card
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NotesTheme.spacing.screen)
            .testTag(ExportNotesTestTags.LIST),
    ) {
        itemsIndexed(state.notes, key = { _, note -> note.id }) { index, note ->
            // Las filas forman un solo grupo, como en Ajustes: solo la primera y la última redondean sus esquinas.
            val top = if (index == 0) radius else 0.dp
            val bottom = if (index == state.notes.lastIndex) radius else 0.dp
            val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, shape)) {
                if (index > 0) {
                    HorizontalDivider(
                        thickness = NotesTheme.sizes.border,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                NoteSelectionRow(
                    title = note.title,
                    isSelected = note.id in state.selectedIds,
                    enabled = !state.isExporting,
                    onToggle = { onIntent(ExportNotesIntent.NoteToggled(note.id)) },
                    modifier = Modifier.testTag(ExportNotesTestTags.NOTE + note.id),
                )
            }
        }
    }
}

/** Toda la fila cambia la casilla; el lector de pantalla anuncia el título y si está seleccionada. */
@Composable
private fun NoteSelectionRow(
    title: String,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = isSelected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = NotesTheme.sizes.settingsRow)
            .padding(horizontal = NotesTheme.spacing.l, vertical = NotesTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.ml),
    ) {
        SelectionBox(isSelected = isSelected)
        Text(
            text = title.ifEmpty { stringResource(R.string.export_untitled) },
            modifier = Modifier.weight(1f),
            style = NotesTheme.typography.settingsRowTitle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Casilla con el aspecto de las tareas del editor. Solo dibuja: la fila la cambia. */
@Composable
private fun SelectionBox(isSelected: Boolean, modifier: Modifier = Modifier) {
    val sizes = NotesTheme.sizes
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(NotesTheme.radii.taskCheckbox)
    Box(
        modifier = modifier
            .size(sizes.taskCheckbox)
            .then(
                if (isSelected) {
                    Modifier.background(colors.primary, shape)
                } else {
                    Modifier.border(sizes.taskCheckboxBorder, colors.onSurfaceVariant, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(sizes.taskCheckIcon),
                tint = colors.onPrimary,
            )
        }
    }
}

@Composable
private fun ExportButton(
    enabled: Boolean,
    isExporting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressLabel = stringResource(R.string.export_in_progress)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(NotesTheme.spacing.screen)
            .height(NotesTheme.sizes.sheetButtonHeight)
            .testTag(ExportNotesTestTags.EXPORT),
        shape = RoundedCornerShape(NotesTheme.radii.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(NotesTheme.sizes.taskCheckbox)
                    .testTag(ExportNotesTestTags.PROGRESS)
                    .semantics { contentDescription = progressLabel },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                strokeWidth = NotesTheme.sizes.taskCheckboxBorder,
            )
        } else {
            Text(text = stringResource(R.string.export_action), style = MaterialTheme.typography.titleSmall)
        }
    }
}

private val PreviewState = ExportNotesUiState(
    isLoading = false,
    notes = listOf(
        ExportNoteItem(id = 1, title = "La compra"),
        ExportNoteItem(id = 2, title = "Ideas para el viaje"),
        ExportNoteItem(id = 3, title = ""),
        ExportNoteItem(id = 4, title = "Reunión del lunes con el equipo de diseño y los nuevos proveedores"),
    ),
    selectedIds = setOf(1, 3),
)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun ExportNotesScreenDarkPreview() {
    NotesAppTheme { ExportNotesScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun ExportNotesScreenLightPreview() {
    NotesAppTheme(darkTheme = false) { ExportNotesScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun ExportNotesScreenExportingPreview() {
    NotesAppTheme { ExportNotesScreen(state = PreviewState.copy(isExporting = true), onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun ExportNotesScreenEmptyPreview() {
    NotesAppTheme(darkTheme = false) { ExportNotesScreen(state = PreviewState.copy(notes = emptyList()), onIntent = {}) }
}
