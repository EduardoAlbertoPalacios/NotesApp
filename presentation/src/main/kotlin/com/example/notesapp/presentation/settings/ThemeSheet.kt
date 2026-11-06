package com.example.notesapp.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R

/** Selector de tema: conecta la hoja de opciones con los intents de Ajustes. */
@Composable
internal fun ThemeSheet(
    selected: ThemeMode,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsOptionSheet(
        titleRes = R.string.settings_theme,
        options = ThemeMode.entries,
        selected = selected,
        optionLabelRes = { it.labelRes() },
        optionTestTag = { SettingsTestTags.THEME_OPTION + it.name },
        sheetTestTag = SettingsTestTags.THEME_SHEET,
        onSelected = { onIntent(SettingsIntent.ThemeSelected(it)) },
        onDismiss = { onIntent(SettingsIntent.ThemeSheetDismissed) },
        modifier = modifier,
    )
}
