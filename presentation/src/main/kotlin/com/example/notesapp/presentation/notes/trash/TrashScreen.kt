package com.example.notesapp.presentation.notes.trash

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.trash.TrashRetention
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.common.ConfirmDialog
import com.example.notesapp.presentation.common.SettingsOptionSheet
import com.example.notesapp.presentation.common.SheetHandle
import com.example.notesapp.presentation.notes.common.toContainerColor
import com.example.notesapp.presentation.notes.common.toContentColor
import com.example.notesapp.presentation.notes.common.toMetadataColor
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

internal object TrashTestTags {
    const val BACK = "trash_back"
    const val RETENTION = "trash_retention"
    const val COUNT = "trash_count"
    const val EMPTY_ACTION = "trash_empty_action"
    const val LIST = "trash_list"
    const val NOTE = "trash_note_"
    const val LOADING = "trash_loading"
    const val EMPTY = "trash_empty"
    const val LOAD_ERROR = "trash_load_error"
    const val ACTIONS_SHEET = "trash_actions_sheet"
    const val RESTORE = "trash_restore"
    const val DELETE_FOREVER = "trash_delete_forever"
    const val DELETE_CONFIRM = "trash_delete_confirm"
    const val DELETE_DISMISS = "trash_delete_dismiss"
    const val EMPTY_CONFIRM = "trash_empty_confirm"
    const val EMPTY_DISMISS = "trash_empty_dismiss"
    const val RETENTION_SHEET = "trash_retention_sheet"
    const val RETENTION_OPTION = "trash_retention_option_"
    const val RETENTION_CONFIRM = "trash_retention_confirm"
    const val RETENTION_DISMISS = "trash_retention_dismiss"
}

/** Papelera: notas eliminadas, que solo se pueden restaurar o eliminar definitivamente (RF-14 a RF-46). */
@Composable
fun TrashScreen(
    state: TrashUiState,
    onIntent: (TrashIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TrashTopBar(
                onBack = { onIntent(TrashIntent.BackClicked) },
                onRetention = { onIntent(TrashIntent.RetentionClicked) },
            )
            when {
                state.isLoading -> LoadingIndicator(Modifier.weight(1f))
                state.loadFailed -> CenteredMessage(
                    textRes = R.string.trash_load_error,
                    modifier = Modifier.weight(1f),
                    testTag = TrashTestTags.LOAD_ERROR,
                )
                state.isEmpty -> CenteredMessage(
                    textRes = R.string.trash_empty,
                    modifier = Modifier.weight(1f),
                    testTag = TrashTestTags.EMPTY,
                )
                else -> {
                    TrashHeader(
                        count = state.notes.size,
                        canEmpty = state.canEmpty,
                        onEmpty = { onIntent(TrashIntent.EmptyClicked) },
                    )
                    TrashList(state = state, onIntent = onIntent, modifier = Modifier.weight(1f))
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    TrashOverlays(state = state, onIntent = onIntent)
}

/** Hojas y diálogos: se controlan desde el estado, así que sobreviven al giro. */
@Composable
private fun TrashOverlays(state: TrashUiState, onIntent: (TrashIntent) -> Unit) {
    state.actionsNote?.let { note ->
        TrashActionsSheet(title = note.title, onIntent = onIntent)
    }
    if (state.isRetentionSheetVisible) {
        SettingsOptionSheet(
            titleRes = R.string.trash_retention,
            options = TrashRetention.entries,
            selected = state.retention,
            optionLabelRes = { it.labelRes() },
            optionTestTag = { TrashTestTags.RETENTION_OPTION + it.name },
            sheetTestTag = TrashTestTags.RETENTION_SHEET,
            onSelected = { onIntent(TrashIntent.RetentionSelected(it)) },
            onDismiss = { onIntent(TrashIntent.RetentionSheetDismissed) },
        )
    }
    if (state.pendingDeleteId != null) {
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_title),
            message = stringResource(R.string.trash_delete_message),
            confirmLabel = stringResource(R.string.trash_delete_confirm),
            dismissLabel = stringResource(R.string.trash_cancel),
            onConfirm = { onIntent(TrashIntent.DeleteForeverConfirmed) },
            onDismiss = { onIntent(TrashIntent.DeleteForeverDismissed) },
            confirmTestTag = TrashTestTags.DELETE_CONFIRM,
            dismissTestTag = TrashTestTags.DELETE_DISMISS,
        )
    }
    if (state.isEmptyConfirmVisible) {
        val count = state.notes.size
        ConfirmDialog(
            title = stringResource(R.string.trash_empty_title),
            message = pluralStringResource(R.plurals.trash_empty_message, count, count),
            confirmLabel = stringResource(R.string.trash_empty_confirm),
            dismissLabel = stringResource(R.string.trash_cancel),
            onConfirm = { onIntent(TrashIntent.EmptyConfirmed) },
            onDismiss = { onIntent(TrashIntent.EmptyDismissed) },
            confirmTestTag = TrashTestTags.EMPTY_CONFIRM,
            dismissTestTag = TrashTestTags.EMPTY_DISMISS,
        )
    }
    state.pendingRetention?.let { pending ->
        ConfirmDialog(
            title = stringResource(R.string.trash_retention_confirm_title),
            message = pluralStringResource(
                R.plurals.trash_retention_confirm_message,
                pending.count,
                pending.count,
                stringResource(pending.retention.labelRes()),
            ),
            confirmLabel = stringResource(R.string.trash_retention_confirm),
            dismissLabel = stringResource(R.string.trash_cancel),
            onConfirm = { onIntent(TrashIntent.RetentionConfirmed) },
            onDismiss = { onIntent(TrashIntent.RetentionDismissed) },
            confirmTestTag = TrashTestTags.RETENTION_CONFIRM,
            dismissTestTag = TrashTestTags.RETENTION_DISMISS,
        )
    }
}

@Composable
private fun TrashTopBar(onBack: () -> Unit, onRetention: () -> Unit, modifier: Modifier = Modifier) {
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
            modifier = Modifier.size(NotesTheme.sizes.iconAction).testTag(TrashTestTags.BACK),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.trash_back),
                modifier = Modifier.size(NotesTheme.sizes.settingsBackIcon),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.trash_title),
            modifier = Modifier.weight(1f),
            style = NotesTheme.typography.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        // Siempre visible, también con la papelera vacía (RF-36).
        IconButton(
            onClick = onRetention,
            modifier = Modifier.size(NotesTheme.sizes.iconAction).testTag(TrashTestTags.RETENTION),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = stringResource(R.string.trash_retention),
                modifier = Modifier.size(NotesTheme.sizes.settingsBackIcon),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(TrashTestTags.LOADING),
            color = NotesTheme.accent.content,
        )
    }
}

/** Aviso en lugar de la lista: papelera vacía o sin poder leerla. */
@Composable
private fun CenteredMessage(@StringRes textRes: Int, testTag: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(NotesTheme.spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.testTag(testTag),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** «N notas» y «Vaciar papelera», que solo aparece si hay notas (RF-29, RF-30). */
@Composable
private fun TrashHeader(count: Int, canEmpty: Boolean, onEmpty: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NotesTheme.sizes.iconAction)
            .padding(horizontal = NotesTheme.spacing.screen, vertical = NotesTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.trash_count, count, count),
            modifier = Modifier.weight(1f).testTag(TrashTestTags.COUNT),
            style = NotesTheme.typography.settingsSection,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canEmpty) {
            TextButton(
                onClick = onEmpty,
                modifier = Modifier.testTag(TrashTestTags.EMPTY_ACTION),
                // El color de acento del tema: el `primary` de Material apenas contrasta sobre el fondo claro.
                colors = ButtonDefaults.textButtonColors(contentColor = NotesTheme.accent.content),
            ) {
                Text(text = stringResource(R.string.trash_empty_action), style = NotesTheme.typography.settingsRowTitle)
            }
        }
    }
}

@Composable
private fun TrashList(state: TrashUiState, onIntent: (TrashIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = NotesTheme.spacing
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TrashTestTags.LIST),
        contentPadding = PaddingValues(start = spacing.screen, end = spacing.screen, bottom = spacing.screen),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        items(items = state.notes, key = { it.id }) { note ->
            TrashNoteCard(
                note = note,
                onClick = { onIntent(TrashIntent.NoteClicked(note.id)) },
                modifier = Modifier.testTag(TrashTestTags.NOTE + note.id),
            )
        }
    }
}

/**
 * Tarjeta con el color y la forma de las de Mis notas, sin fecha ni indicador de fijada (RF-15 a RF-17). Al
 * tocarla se abre la hoja de acciones: la nota no se puede abrir (RF-21). El lector anuncia sus textos juntos.
 */
@Composable
private fun TrashNoteCard(note: TrashItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = NotesTheme.spacing
    val shape = RoundedCornerShape(NotesTheme.radii.card)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                onClickLabel = stringResource(R.string.trash_note_actions),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        color = note.color.toContainerColor(),
        contentColor = note.color.toContentColor(),
    ) {
        Column(
            modifier = Modifier.padding(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = note.title.ifEmpty { stringResource(R.string.trash_untitled) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.excerpt.isNotEmpty()) {
                Text(
                    text = note.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = EXCERPT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            note.daysLeft?.let { days ->
                Text(
                    text = daysLeftText(days),
                    modifier = Modifier.padding(top = spacing.xxs),
                    style = MaterialTheme.typography.labelSmall,
                    color = note.color.toMetadataColor(),
                )
            }
        }
    }
}

/** «Se eliminará en N días», «mañana» con 1 u «hoy» con 0 (RF-16). */
@Composable
private fun daysLeftText(days: Int): String = when (days) {
    0 -> stringResource(R.string.trash_days_left_today)
    1 -> stringResource(R.string.trash_days_left_tomorrow)
    else -> pluralStringResource(R.plurals.trash_days_left, days, days)
}

/** Hoja de acciones de una nota, con el estilo de las hojas de opciones (RF-20). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashActionsSheet(title: String, onIntent: (TrashIntent) -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(TrashIntent.ActionsDismissed) },
        modifier = modifier.testTag(TrashTestTags.ACTIONS_SHEET),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = NotesTheme.radii.pill, topEnd = NotesTheme.radii.pill),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = { SheetHandle() },
    ) {
        TrashActionsContent(title = title, onIntent = onIntent)
    }
}

@Composable
private fun TrashActionsContent(title: String, onIntent: (TrashIntent) -> Unit, modifier: Modifier = Modifier) {
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
            text = title.ifEmpty { stringResource(R.string.trash_untitled) },
            style = NotesTheme.typography.sheetTitle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Column {
            TrashAction(
                iconRes = R.drawable.ic_undo,
                labelRes = R.string.trash_restore,
                onClick = { onIntent(TrashIntent.RestoreClicked) },
                modifier = Modifier.testTag(TrashTestTags.RESTORE),
            )
            TrashAction(
                iconRes = R.drawable.ic_trash,
                labelRes = R.string.trash_delete_forever,
                onClick = { onIntent(TrashIntent.DeleteForeverClicked) },
                modifier = Modifier.testTag(TrashTestTags.DELETE_FOREVER),
            )
        }
    }
}

@Composable
private fun TrashAction(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NotesTheme.sizes.themeOption)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.ml),
    ) {
        // Decorativo: la etiqueta de la fila ya dice la acción.
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(NotesTheme.sizes.settingsRowIcon),
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(labelRes),
            style = NotesTheme.typography.themeOption,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private const val TITLE_MAX_LINES = 2
private const val EXCERPT_MAX_LINES = 3

private val PreviewState = TrashUiState(
    isLoading = false,
    notes = listOf(
        TrashItemUi(id = 1, title = "La compra", excerpt = "Leche, pan, huevos y fruta", color = NoteColor.MINT, daysLeft = 29),
        TrashItemUi(
            id = 2,
            title = "Ideas para el viaje",
            excerpt = "Visitar el museo, reservar el tren y mirar restaurantes cerca del hotel. " +
                "Preguntar por el horario de la playa y por las excursiones de la zona norte.",
            color = NoteColor.NONE,
            daysLeft = 1,
        ),
        TrashItemUi(id = 3, title = "", excerpt = "Llamar al fontanero", color = NoteColor.LAVENDER, daysLeft = 0),
    ),
)

private val PreviewNeverState = PreviewState.copy(
    retention = TrashRetention.NEVER,
    notes = PreviewState.notes.map { it.copy(daysLeft = null) },
)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenDarkPreview() {
    NotesAppTheme { TrashScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenLightPreview() {
    NotesAppTheme(darkTheme = false) { TrashScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenNeverDarkPreview() {
    NotesAppTheme { TrashScreen(state = PreviewNeverState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenNeverLightPreview() {
    NotesAppTheme(darkTheme = false) { TrashScreen(state = PreviewNeverState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenEmptyDarkPreview() {
    NotesAppTheme { TrashScreen(state = TrashUiState(isLoading = false), onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenEmptyLightPreview() {
    NotesAppTheme(darkTheme = false) { TrashScreen(state = TrashUiState(isLoading = false), onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenErrorDarkPreview() {
    NotesAppTheme { TrashScreen(state = TrashUiState(isLoading = false, loadFailed = true), onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun TrashScreenErrorLightPreview() {
    NotesAppTheme(darkTheme = false) {
        TrashScreen(state = TrashUiState(isLoading = false, loadFailed = true), onIntent = {})
    }
}

// La hoja modal no se dibuja en las previews estáticas: se previsualiza su contenido.
@Preview(widthDp = 412)
@Composable
private fun TrashActionsContentDarkPreview() {
    NotesAppTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            TrashActionsContent(title = "La compra", onIntent = {})
        }
    }
}

@Preview(widthDp = 412)
@Composable
private fun TrashActionsContentLightPreview() {
    NotesAppTheme(darkTheme = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            SheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
            TrashActionsContent(title = "", onIntent = {})
        }
    }
}
