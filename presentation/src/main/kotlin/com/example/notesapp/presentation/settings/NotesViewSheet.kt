package com.example.notesapp.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.labelRes

/** Selector de la vista de Mis notas: conecta la hoja de opciones con los intents de Ajustes. */
@Composable
internal fun NotesViewSheet(
    selected: NotesView,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsOptionSheet(
        titleRes = R.string.settings_notes_view,
        options = NotesView.entries,
        selected = selected,
        optionLabelRes = { it.labelRes() },
        optionTestTag = { SettingsTestTags.NOTES_VIEW_OPTION + it.name },
        sheetTestTag = SettingsTestTags.NOTES_VIEW_SHEET,
        onSelected = { onIntent(SettingsIntent.NotesViewSelected(it)) },
        onDismiss = { onIntent(SettingsIntent.NotesViewSheetDismissed) },
        modifier = modifier,
    )
}
