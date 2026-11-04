package com.example.notesapp.presentation.notes.common

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
import com.example.notesapp.presentation.common.SheetHandle
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme
import kotlinx.coroutines.launch

internal object ColorPickerTestTags {
    const val SHEET = "color_picker_sheet"
    const val OPTION = "color_picker_option_"
    const val PREVIEW = "color_picker_preview"
    const val APPLY = "color_picker_apply"
    const val CLOSE = "color_picker_close"
}

/**
 * Hoja para elegir un color de nota, con vista previa. La opción elegida ([pickerColor]) no se aplica hasta
 * pulsar «Aplicar color»; cerrar la hoja de cualquier otra forma la descarta. La usan el editor y Ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteColorPickerSheet(
    title: String,
    description: String,
    previewTitle: String,
    pickerColor: NoteColor,
    onColorSelected: (NoteColor) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Anima el cierre antes de avisar, porque es quien llama quien quita la hoja de la composición.
    val hideThen = { action: () -> Unit ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(ColorPickerTestTags.SHEET),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NotesTheme.radii.pill, topEnd = NotesTheme.radii.pill),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = { SheetHandle() },
    ) {
        NoteColorPickerContent(
            title = title,
            description = description,
            previewTitle = previewTitle,
            pickerColor = pickerColor,
            onColorSelected = onColorSelected,
            onClose = { hideThen(onDismiss) },
            onApply = { hideThen(onApply) },
        )
    }
}

@Composable
private fun NoteColorPickerContent(
    title: String,
    description: String,
    previewTitle: String,
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
        SheetHeader(title = title, description = description, onClose = onClose)
        ColorPalette(selected = pickerColor, onColorSelected = onColorSelected)
        ColorPreview(title = previewTitle, color = pickerColor)
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(NotesTheme.sizes.sheetButtonHeight)
                .testTag(ColorPickerTestTags.APPLY),
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
    title: String,
    description: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xsm),
        ) {
            Text(
                text = title,
                style = NotesTheme.typography.sheetTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(NotesTheme.sizes.iconAction)
                .testTag(ColorPickerTestTags.CLOSE),
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
            .testTag(ColorPickerTestTags.OPTION + color.name),
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
                    color = if (isSelected) noteColors.swatchSelectedBorder else noteColors.swatchBorder,
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
            color = if (isSelected) NotesTheme.accent.content else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ColorPreview(
    title: String,
    color: NoteColor,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotesTheme.radii.card))
            .background(color.toContainerColor())
            .padding(NotesTheme.spacing.l)
            .testTag(ColorPickerTestTags.PREVIEW),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.previewLines),
    ) {
        Text(
            text = title,
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
private fun NoteColorPickerContentPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            NoteColorPickerContent(
                title = stringResource(R.string.note_editor_toolbar_color),
                description = stringResource(R.string.note_color_sheet_description),
                previewTitle = "Reunión de equipo",
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
private fun NoteColorPickerContentLightPreview() {
    NotesAppTheme(darkTheme = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            NoteColorPickerContent(
                title = stringResource(R.string.note_editor_toolbar_color),
                description = stringResource(R.string.note_color_sheet_description),
                previewTitle = "Reunión de equipo",
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
private fun NoteColorPickerContentNoColorPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            NoteColorPickerContent(
                title = stringResource(R.string.note_editor_toolbar_color),
                description = stringResource(R.string.note_color_sheet_description),
                previewTitle = stringResource(R.string.note_color_preview_untitled),
                pickerColor = NoteColor.NONE,
                onColorSelected = {},
                onClose = {},
                onApply = {},
            )
        }
    }
}
