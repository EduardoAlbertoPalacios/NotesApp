package com.example.notesapp.presentation.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.common.Chevron
import com.example.notesapp.presentation.common.DisabledRow
import com.example.notesapp.presentation.common.GroupDivider
import com.example.notesapp.presentation.common.SearchField
import com.example.notesapp.presentation.common.SettingsGroup
import com.example.notesapp.presentation.common.SettingsTopBar
import com.example.notesapp.presentation.theme.DisabledAlpha
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

object HelpTestTags {
    const val BACK = "help_back"
    const val LIST = "help_list"
    const val SEARCH_FIELD = "help_search_field"
    const val SEARCH_CLEAR = "help_search_clear"
    const val NO_RESULTS = "help_no_results"
    const val RATE_ROW = "help_rate_row"
    const val RATE_NOTE = "help_rate_note"

    fun faq(id: Int): String = "help_faq_$id"
    fun answer(id: Int): String = "help_faq_answer_$id"
}

/** Pantalla Ayuda y comentarios (spec 013, Figma `102:68` y `102:2`). Sin estado: pinta [state] y emite intents. */
@Composable
fun HelpScreen(
    state: HelpUiState,
    onIntent: (HelpIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        SettingsTopBar(
            title = stringResource(R.string.help_title),
            backLabel = stringResource(R.string.help_back),
            onBack = { onIntent(HelpIntent.BackClicked) },
            backTestTag = HelpTestTags.BACK,
        )
        HelpContent(state = state, onIntent = onIntent)
    }
}

@Composable
private fun HelpContent(
    state: HelpUiState,
    onIntent: (HelpIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NotesTheme.spacing
    // Guardable: la posición sobrevive al giro (RF-29).
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag(HelpTestTags.LIST),
        contentPadding = PaddingValues(
            start = spacing.screen,
            top = spacing.ml,
            end = spacing.screen,
            bottom = spacing.screen,
        ),
    ) {
        item(key = "header") { HelpHeader() }
        item(key = "search") {
            SearchField(
                query = state.query,
                placeholder = stringResource(R.string.help_search_placeholder),
                clearLabel = stringResource(R.string.help_search_clear),
                onQueryChange = { onIntent(HelpIntent.QueryChanged(it)) },
                onClear = { onIntent(HelpIntent.ClearQueryClicked) },
                fieldTestTag = HelpTestTags.SEARCH_FIELD,
                clearTestTag = HelpTestTags.SEARCH_CLEAR,
                modifier = Modifier.padding(top = spacing.xxl),
            )
        }
        item(key = "faqs_title") { SectionTitle(text = stringResource(R.string.help_section_faqs)) }
        if (state.showNoResults) {
            item(key = "no_results") { NoResults(query = state.noResultsQuery) }
        } else {
            itemsIndexed(state.faqs, key = { _, faq -> faq.id }) { index, faq ->
                FaqItem(
                    faq = faq,
                    isFirst = index == 0,
                    isLast = index == state.faqs.lastIndex,
                    onClick = { onIntent(HelpIntent.FaqClicked(faq.id)) },
                )
            }
        }
        // Sigue visible también durante la búsqueda (RF-24).
        item(key = "rate") {
            SettingsGroup(titleRes = R.string.help_section_rate, modifier = Modifier.padding(top = spacing.xxl)) {
                DisabledRow(
                    iconRes = R.drawable.ic_star,
                    titleRes = R.string.help_rate,
                    detailRes = R.string.help_rate_detail,
                    testTag = HelpTestTags.RATE_ROW,
                )
            }
        }
        item(key = "rate_note") {
            Text(
                text = stringResource(R.string.help_rate_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxl)
                    .alpha(DisabledAlpha)
                    .testTag(HelpTestTags.RATE_NOTE),
                style = NotesTheme.typography.settingsRowDetail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HelpHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.help_header_title),
            style = NotesTheme.typography.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.help_header_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Título de grupo con el mismo estilo que [SettingsGroup], para grupos que se pintan fila a fila en la lista. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NotesTheme.spacing.xxl, bottom = NotesTheme.spacing.sm),
        style = NotesTheme.typography.settingsSection,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Una pregunta de la tarjeta «Preguntas frecuentes». Cada fila es un elemento de la lista (`key = id`),
 * así que la tarjeta se forma redondeando la primera y la última.
 */
@Composable
private fun FaqItem(
    faq: FaqUi,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radius = CornerSize(NotesTheme.radii.card)
    val top = if (isFirst) radius else ZeroCornerSize
    val bottom = if (isLast) radius else ZeroCornerSize
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
    val stateLabel = stringResource(
        if (faq.isExpanded) R.string.help_question_expanded else R.string.help_question_collapsed,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (!isFirst) GroupDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateLabel }
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(HelpTestTags.faq(faq.id))
                .padding(NotesTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.s),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.s),
            ) {
                Text(
                    text = faq.question,
                    modifier = Modifier.weight(1f),
                    style = NotesTheme.typography.settingsRowTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // Cerrada «›»; abierta, el mismo chevron girado hacia arriba (RF-15).
                Chevron(modifier = Modifier.rotate(if (faq.isExpanded) EXPANDED_CHEVRON_ROTATION else 0f))
            }
            AnimatedVisibility(visible = faq.isExpanded) {
                Text(
                    text = faq.answer,
                    modifier = Modifier.testTag(HelpTestTags.answer(faq.id)),
                    style = NotesTheme.typography.categoryDescription,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoResults(query: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(NotesTheme.radii.card))
            .padding(NotesTheme.spacing.l)
            .testTag(HelpTestTags.NO_RESULTS),
    ) {
        Text(
            text = stringResource(R.string.help_no_results, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Grados que gira el chevron «›» para apuntar hacia arriba con la pregunta abierta. */
private const val EXPANDED_CHEVRON_ROTATION = -90f

private val PreviewFaqs = listOf(
    FaqUi(
        id = 1,
        question = "¿Dónde se guardan mis notas?",
        answer = "Tus notas se guardan en este dispositivo. Para tener una copia, ve a Ajustes → Exportar notas.",
        isExpanded = true,
    ),
    FaqUi(id = 2, question = "¿Cómo recupero una nota eliminada?", answer = "", isExpanded = false),
    FaqUi(id = 3, question = "¿Cómo cambio el color de mis notas?", answer = "", isExpanded = false),
)

private val PreviewState = HelpUiState(faqs = PreviewFaqs, isLoading = false)
private val PreviewNoResultsState = HelpUiState(query = "widget", faqs = emptyList(), isLoading = false)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HelpScreenDarkPreview() {
    NotesAppTheme { HelpScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HelpScreenLightPreview() {
    NotesAppTheme(darkTheme = false) { HelpScreen(state = PreviewState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HelpScreenNoResultsDarkPreview() {
    NotesAppTheme { HelpScreen(state = PreviewNoResultsState, onIntent = {}) }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun HelpScreenNoResultsLightPreview() {
    NotesAppTheme(darkTheme = false) { HelpScreen(state = PreviewNoResultsState, onIntent = {}) }
}
