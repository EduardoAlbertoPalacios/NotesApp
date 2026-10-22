package com.example.notesapp.presentation.notes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.labelRes
import com.example.notesapp.presentation.notes.common.toContainerColor
import com.example.notesapp.presentation.notes.common.toContentColor
import com.example.notesapp.presentation.notes.common.toMetadataColor
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme
import kotlinx.coroutines.launch

/**
 * Hoja para elegir el color de la nota. La opción elegida ([pickerColor]) no se aplica hasta pulsar
 * «Aplicar color»; cerrar la hoja de cualquier otra forma la descarta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteColorSheet(
    noteTitle: String,
    pickerColor: NoteColor,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Anima el cierre antes de avisar al ViewModel, que es quien quita la hoja de la composición.
    val hideThen = { intent: NoteEditorIntent ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { onIntent(intent) }
    }
    ModalBottomSheet(
        onDismissRequest = { onIntent(NoteEditorIntent.ColorPickerDismissed) },
        modifier = modifier.testTag(NoteEditorTestTags.COLOR_SHEET),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NotesTheme.radii.pill, topEnd = NotesTheme.radii.pill),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = { SheetHandle() },
    ) {
        NoteColorSheetContent(
            noteTitle = noteTitle,
            pickerColor = pickerColor,
            onColorSelected = { onIntent(NoteEditorIntent.PickerColorSelected(it)) },
            onClose = { hideThen(NoteEditorIntent.ColorPickerDismissed) },
            onApply = { hideThen(NoteEditorIntent.ColorApplied) },
        )
    }
}

@Composable
private fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = NotesTheme.spacing.sm, bottom = NotesTheme.spacing.sheetSection)
            .size(width = NotesTheme.sizes.sheetHandleWidth, height = NotesTheme.sizes.sheetHandleHeight)
            .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(NotesTheme.radii.sheetHandle)),
    )
}

@Composable
private fun NoteColorSheetContent(
    noteTitle: String,
    pickerColor: NoteColor,
    onColorSelected: (NoteColor) -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // En horizontal la hoja no cabe entera: se desplaza para llegar a «Aplicar color».
            .verticalScroll(rememberScrollState())
            .padding(start = NotesTheme.spacing.screen, end = NotesTheme.spacing.screen, bottom = NotesTheme.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.sheetSection),
    ) {
        SheetHeader(onClose = onClose)
        ColorPalette(selected = pickerColor, onColorSelected = onColorSelected)
        ColorPreview(noteTitle = noteTitle, color = pickerColor)
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(NotesTheme.sizes.sheetButtonHeight)
                .testTag(NoteEditorTestTags.COLOR_APPLY),
            shape = RoundedCornerShape(NotesTheme.radii.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.note_color_apply), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xsm),
        ) {
            Text(
                text = stringResource(R.string.note_editor_toolbar_color),
                style = NotesTheme.typography.sheetTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.note_color_sheet_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(NotesTheme.sizes.iconAction)
                .testTag(NoteEditorTestTags.COLOR_CLOSE),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.note_color_close),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ColorPalette(
    selected: NoteColor,
    onColorSelected: (NoteColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.l),
    ) {
        // Dos filas de cuatro, en el orden del enum, que es el de la paleta del diseño.
        NoteColor.entries.chunked(PALETTE_COLUMNS).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { color ->
                    ColorOption(
                        color = color,
                        isSelected = color == selected,
                        onClick = { onColorSelected(color) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: NoteColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = NotesTheme.sizes
    val noteColors = NotesTheme.noteColors
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(sizes.colorOptionWidth)
            // La etiqueta del color es el texto de la opción; el rol anuncia si está seleccionada.
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .testTag(NoteEditorTestTags.COLOR_OPTION + color.name),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.s),
    ) {
        Box(
            modifier = Modifier
                .size(sizes.colorSwatch)
                .clip(CircleShape)
                .background(if (color == NoteColor.NONE) noteColors.swatch else color.toContainerColor())
                .border(
                    width = if (isSelected) sizes.colorSwatchSelectedBorder else sizes.border,
                    color = if (isSelected) colors.onBackground else noteColors.swatchBorder,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isSelected -> Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.colorCheckIcon),
                    tint = color.toContentColor(),
                )
                color == NoteColor.NONE -> Icon(
                    painter = painterResource(R.drawable.ic_slash),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.colorSwatchIcon),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(color.labelRes()),
            style = if (isSelected) NotesTheme.typography.colorLabelSelected else NotesTheme.typography.colorLabel,
            color = if (isSelected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ColorPreview(
    noteTitle: String,
    color: NoteColor,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotesTheme.radii.card))
            .background(color.toContainerColor())
            .padding(NotesTheme.spacing.l)
            .testTag(NoteEditorTestTags.COLOR_PREVIEW),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.previewLines),
    ) {
        Text(
            text = noteTitle.ifBlank { stringResource(R.string.note_color_preview_untitled) },
            style = NotesTheme.typography.colorPreviewTitle,
            color = color.toContentColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.note_color_preview_caption, stringResource(color.labelRes())),
            style = MaterialTheme.typography.labelMedium,
            color = color.toMetadataColor(),
        )
    }
}

private const val PALETTE_COLUMNS = 4

// La hoja modal no se dibuja en las previews estáticas: se previsualiza su contenido.
@Preview(widthDp = 412)
@Composable
private fun NoteColorSheetContentPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            NoteColorSheetContent(
                noteTitle = "Reunión de equipo",
                pickerColor = NoteColor.AQUA,
                onColorSelected = {},
                onClose = {},
                onApply = {},
            )
        }
    }
}

@Preview(widthDp = 412)
@Composable
private fun NoteColorSheetContentNoColorPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            NoteColorSheetContent(
                noteTitle = "",
                pickerColor = NoteColor.NONE,
                onColorSelected = {},
                onClose = {},
                onApply = {},
            )
        }
    }
}
