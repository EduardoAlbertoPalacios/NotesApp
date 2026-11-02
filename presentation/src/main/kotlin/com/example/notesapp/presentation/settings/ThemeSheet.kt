package com.example.notesapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.common.SheetHandle
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme
import kotlinx.coroutines.launch

/** Hoja para elegir el tema: la opción se aplica al tocarla y la hoja se cierra después con su animación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSheet(
    selected: ThemeMode,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { onIntent(SettingsIntent.ThemeSheetDismissed) },
        modifier = modifier.testTag(SettingsTestTags.THEME_SHEET),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NotesTheme.radii.pill, topEnd = NotesTheme.radii.pill),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = { SheetHandle() },
    ) {
        ThemeSheetContent(
            selected = selected,
            onSelected = { mode ->
                onIntent(SettingsIntent.ThemeSelected(mode))
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { onIntent(SettingsIntent.ThemeSheetDismissed) }
            },
        )
    }
}

@Composable
private fun ThemeSheetContent(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = NotesTheme.spacing.screen,
                end = NotesTheme.spacing.screen,
                bottom = NotesTheme.spacing.screen,
            ),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.sheetSection),
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = NotesTheme.typography.sheetTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                ThemeOption(mode = mode, isSelected = mode == selected, onClick = { onSelected(mode) })
            }
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NotesTheme.sizes.themeOption)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .testTag(SettingsTestTags.THEME_OPTION + mode.name),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(mode.labelRes()),
            style = if (isSelected) MaterialTheme.typography.titleSmall else NotesTheme.typography.themeOption,
            color = if (isSelected) NotesTheme.accent.content else MaterialTheme.colorScheme.onBackground,
        )
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(NotesTheme.sizes.themeOptionCheck),
                tint = NotesTheme.accent.content,
            )
        }
    }
}

// La hoja modal no se dibuja en las previews estáticas: se previsualiza su contenido.
@Preview(widthDp = 412)
@Composable
private fun ThemeSheetContentDarkPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            ThemeSheetContent(selected = ThemeMode.DARK, onSelected = {})
        }
    }
}

@Preview(widthDp = 412)
@Composable
private fun ThemeSheetContentLightPreview() {
    NotesAppTheme(darkTheme = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            ThemeSheetContent(selected = ThemeMode.LIGHT, onSelected = {})
        }
    }
}
