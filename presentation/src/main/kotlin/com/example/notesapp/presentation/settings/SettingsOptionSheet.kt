package com.example.notesapp.presentation.settings

import androidx.annotation.StringRes
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

/** Hoja de opciones de Ajustes: la opción se aplica al tocarla y la hoja se cierra después con su animación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsOptionSheet(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    optionLabelRes: (T) -> Int,
    optionTestTag: (T) -> String,
    sheetTestTag: String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(sheetTestTag),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NotesTheme.radii.pill, topEnd = NotesTheme.radii.pill),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = { SheetHandle() },
    ) {
        SettingsOptionSheetContent(
            titleRes = titleRes,
            options = options,
            selected = selected,
            optionLabelRes = optionLabelRes,
            optionTestTag = optionTestTag,
            onSelected = { option ->
                onSelected(option)
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
        )
    }
}

@Composable
private fun <T> SettingsOptionSheetContent(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    optionLabelRes: (T) -> Int,
    optionTestTag: (T) -> String,
    onSelected: (T) -> Unit,
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
            text = stringResource(titleRes),
            style = NotesTheme.typography.sheetTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                SettingsOption(
                    labelRes = optionLabelRes(option),
                    isSelected = option == selected,
                    onClick = { onSelected(option) },
                    modifier = Modifier.testTag(optionTestTag(option)),
                )
            }
        }
    }
}

@Composable
private fun SettingsOption(
    @StringRes labelRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NotesTheme.sizes.themeOption)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(labelRes),
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
private fun SettingsOptionSheetContentDarkPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            SettingsOptionSheetContent(
                titleRes = R.string.settings_theme,
                options = ThemeMode.entries,
                selected = ThemeMode.DARK,
                optionLabelRes = { it.labelRes() },
                optionTestTag = { it.name },
                onSelected = {},
            )
        }
    }
}

@Preview(widthDp = 412)
@Composable
private fun SettingsOptionSheetContentLightPreview() {
    NotesAppTheme(darkTheme = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            SettingsOptionSheetContent(
                titleRes = R.string.settings_theme,
                options = ThemeMode.entries,
                selected = ThemeMode.LIGHT,
                optionLabelRes = { it.labelRes() },
                optionTestTag = { it.name },
                onSelected = {},
            )
        }
    }
}
