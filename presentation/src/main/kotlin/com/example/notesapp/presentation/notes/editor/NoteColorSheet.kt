package com.example.notesapp.presentation.notes.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteColorPickerSheet

/** Selector de color de la nota abierta: conecta la hoja compartida con los intents del editor. */
@Composable
internal fun NoteColorSheet(
    noteTitle: String,
    pickerColor: NoteColor,
    onIntent: (NoteEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NoteColorPickerSheet(
        title = stringResource(R.string.note_editor_toolbar_color),
        description = stringResource(R.string.note_color_sheet_description),
        previewTitle = noteTitle.ifBlank { stringResource(R.string.note_color_preview_untitled) },
        pickerColor = pickerColor,
        onColorSelected = { onIntent(NoteEditorIntent.PickerColorSelected(it)) },
        onApply = { onIntent(NoteEditorIntent.ColorApplied) },
        onDismiss = { onIntent(NoteEditorIntent.ColorPickerDismissed) },
        modifier = modifier,
    )
}
