package com.example.notesapp.presentation.notes.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.notes.common.asText
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

object NoteListTestTags {
    const val NOTE_GRID = "note_list_grid"
    const val NOTE_CARD = "note_list_card_"
    const val CREATE_NOTE = "note_list_create_note"
    const val SETTINGS = "note_list_settings"
    const val LOADING = "note_list_loading"
    const val EMPTY = "note_list_empty"
    const val ERROR = "note_list_error"
    const val FILTER = "note_list_filter_"
}

@Composable
fun NoteListScreen(
    state: NoteListUiState,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    val sizes = NotesTheme.sizes
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(NOTE_COLUMNS),
            modifier = Modifier
                .fillMaxSize()
                .testTag(NoteListTestTags.NOTE_GRID),
            contentPadding = PaddingValues(
                start = insets.calculateStartPadding(layoutDirection) + spacing.screen,
                end = insets.calculateEndPadding(layoutDirection) + spacing.screen,
                top = insets.calculateTopPadding(),
                // Deja libre el espacio del botón flotante para que no tape la última nota.
                bottom = insets.calculateBottomPadding() + sizes.fabHeight + spacing.screen * 2,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalItemSpacing = spacing.m,
        ) {
            fullLineItem { NoteListHeader(state = state, onIntent = onIntent) }
            fullLineItem { SearchField() }
            fullLineItem { Filters(selected = state.selectedFilter, onIntent = onIntent) }
            fullLineItem { SortRow() }
            when {
                state.isLoading -> fullLineItem { LoadingState() }
                state.hasError -> fullLineItem {
                    MessageState(
                        text = stringResource(R.string.note_list_error),
                        modifier = Modifier.testTag(NoteListTestTags.ERROR),
                    )
                }
                state.notes.isEmpty() -> fullLineItem {
                    MessageState(
                        text = stringResource(state.selectedFilter.emptyMessageRes()),
                        modifier = Modifier.testTag(NoteListTestTags.EMPTY),
                    )
                }
                else -> items(items = state.notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onIntent(NoteListIntent.NoteClicked(note.id)) })
                }
            }
        }

        CreateNoteButton(
            onClick = { onIntent(NoteListIntent.CreateNoteClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = spacing.screen, bottom = spacing.xxs),
        )
    }
}

private fun LazyStaggeredGridScope.fullLineItem(content: @Composable () -> Unit) {
    item(span = StaggeredGridItemSpan.FullLine) { content() }
}

@Composable
private fun NoteListHeader(
    state: NoteListUiState,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.l, bottom = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.note_list_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!state.isLoading && !state.hasError) {
                Text(
                    text = pluralStringResource(R.plurals.note_list_summary, state.notes.size, state.notes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledIconButton(
            onClick = { onIntent(NoteListIntent.SettingsClicked) },
            modifier = Modifier
                .size(NotesTheme.sizes.iconAction)
                .testTag(NoteListTestTags.SETTINGS),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.note_list_settings),
            )
        }
    }
}

/** Solo visual por ahora: la búsqueda queda fuera del alcance de v1. */
@Composable
private fun SearchField(modifier: Modifier = Modifier) {
    val spacing = NotesTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NotesTheme.sizes.searchHeight)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(NotesTheme.radii.pill)
            )
            .padding(horizontal = spacing.l),
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.note_list_search_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Filters(
    selected: NoteFilter,
    onIntent: (NoteListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        NoteFilter.entries.forEach { filter ->
            FilterChip(
                text = stringResource(filter.labelRes()),
                isSelected = filter == selected,
                color = when (selected) {
                    NoteFilter.ALL -> MaterialTheme.colorScheme.tertiary
                    NoteFilter.WORK -> MaterialTheme.colorScheme.primary
                    NoteFilter.PERSONAL -> MaterialTheme.colorScheme.secondary
                },
                onClick = { onIntent(NoteListIntent.FilterSelected(filter)) },
                modifier = Modifier.testTag(NoteListTestTags.FILTER + filter.name),
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(NotesTheme.radii.pill)
    Box(
        modifier = modifier
            .height(NotesTheme.sizes.chipHeight)
            .clip(shape)
            .background(color = if (isSelected) color else colors.surfaceContainer)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = NotesTheme.spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SortRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = NotesTheme.spacing.xxxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(R.string.note_list_sort_last_edited),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            painter = painterResource(R.drawable.ic_sort),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    val noteColors = NotesTheme.noteColors
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(NoteListTestTags.NOTE_CARD + note.id),
        shape = RoundedCornerShape(NotesTheme.radii.card),
        color = note.color.toContainerColor(),
        contentColor = noteColors.content,
    ) {
        Column(
            modifier = Modifier.padding(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                Text(
                    text = note.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.isPinned) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = stringResource(R.string.note_list_pinned),
                    )
                }
            }
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = CONTENT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = note.date.asText(),
                modifier = Modifier.padding(top = spacing.xxs),
                style = MaterialTheme.typography.labelSmall,
                color = noteColors.metadata,
            )
        }
    }
}

@Composable
private fun CreateNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .height(NotesTheme.sizes.fabHeight)
            .testTag(NoteListTestTags.CREATE_NOTE),
        shape = RoundedCornerShape(NotesTheme.radii.fab),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NotesTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(R.drawable.ic_plus), contentDescription = null)
            Text(
                text = stringResource(R.string.note_list_create_note),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.note_list_loading)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NotesTheme.spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .testTag(NoteListTestTags.LOADING)
                .semantics { contentDescription = description },
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MessageState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NotesTheme.spacing.screen),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun NoteFilter.labelRes(): Int = when (this) {
    NoteFilter.ALL -> R.string.note_list_filter_all
    NoteFilter.WORK -> R.string.note_list_filter_work
    NoteFilter.PERSONAL -> R.string.note_list_filter_personal
}

private fun NoteFilter.emptyMessageRes(): Int = when (this) {
    NoteFilter.ALL -> R.string.note_list_empty
    NoteFilter.WORK -> R.string.note_list_empty_work
    NoteFilter.PERSONAL -> R.string.note_list_empty_personal
}

@Composable
private fun NoteColor.toContainerColor(): Color {
    val colors = NotesTheme.noteColors
    return when (this) {
        NoteColor.AQUA -> colors.aqua
        NoteColor.SAND -> colors.sand
        NoteColor.MINT -> colors.mint
        NoteColor.ROSE -> colors.rose
        NoteColor.LAVENDER -> colors.lavender
        NoteColor.PEACH -> colors.peach
    }
}

private const val NOTE_COLUMNS = 2
private const val TITLE_MAX_LINES = 2
private const val CONTENT_MAX_LINES = 8

private val previewNotes = listOf(
    NoteItemUi(
        id = 1,
        title = "Reunión de equipo",
        content = "Nuevo lanzamiento:\n• Inicio más sencillo\n• Textos claros\n• Prototipo el viernes",
        color = NoteColor.AQUA,
        isPinned = true,
        date = NoteDateLabel.Today("9:38"),
    ),
    NoteItemUi(
        id = 2,
        title = "La compra",
        content = "☐  Tomates\n☐  Leche de avena\n☑  Pan integral\n☐  Café",
        color = NoteColor.ROSE,
        isPinned = false,
        date = NoteDateLabel.Today("8:15"),
    ),
    NoteItemUi(
        id = 3,
        title = "Ideas sueltas",
        content = "Un café, una libreta y tiempo para crear.",
        color = NoteColor.SAND,
        isPinned = false,
        date = NoteDateLabel.Yesterday,
    ),
    NoteItemUi(
        id = 4,
        title = "Escapada",
        content = "La sierra nos espera.\n\nLlevar la cámara y desconectar.",
        color = NoteColor.LAVENDER,
        isPinned = false,
        date = NoteDateLabel.Yesterday,
    ),
    NoteItemUi(
        id = 5,
        title = "Para leer",
        content = "Hábitos atómicos",
        color = NoteColor.MINT,
        isPinned = false,
        date = NoteDateLabel.Date("4 oct"),
    ),
    NoteItemUi(
        id = 6,
        title = "Una buena idea",
        content = "Hacer más de lo que me hace bien.",
        color = NoteColor.PEACH,
        isPinned = false,
        date = NoteDateLabel.Date("3 oct"),
    ),
)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteListScreenContentPreview() {
    NotesAppTheme {
        NoteListScreen(state = NoteListUiState(isLoading = false, notes = previewNotes), onIntent = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteListScreenLoadingPreview() {
    NotesAppTheme {
        NoteListScreen(state = NoteListUiState(), onIntent = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteListScreenEmptyPreview() {
    NotesAppTheme {
        NoteListScreen(state = NoteListUiState(isLoading = false), onIntent = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun NoteListScreenErrorPreview() {
    NotesAppTheme {
        NoteListScreen(state = NoteListUiState(isLoading = false, hasError = true), onIntent = {})
    }
}
