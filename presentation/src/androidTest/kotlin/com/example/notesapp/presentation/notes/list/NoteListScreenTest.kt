package com.example.notesapp.presentation.notes.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.notes.common.buildFormattedContent
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val note = NoteItemUi(
        id = 42,
        title = "Reunión de equipo",
        content = AnnotatedString("Prototipo el viernes"),
        color = NoteColor.AQUA,
        isPinned = true,
        date = NoteDateLabel.Yesterday,
    )
    private val intents = mutableListOf<NoteListIntent>()

    private fun setContent(state: NoteListUiState) {
        composeRule.setContent {
            NotesAppTheme {
                NoteListScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    /** Estado que aplica los intents de búsqueda, como el ViewModel, para que el campo no reenvíe el valor viejo. */
    private fun setStatefulContent(initial: NoteListUiState) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            NotesAppTheme {
                NoteListScreen(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is NoteListIntent.QueryChanged -> state.copy(query = intent.query)
                            NoteListIntent.ClearQueryClicked -> state.copy(query = "")
                            else -> state
                        }
                    },
                )
            }
        }
    }

    @Test
    fun given_notes_when_displayed_then_shows_title_content_and_pin() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithText(note.title).assertIsDisplayed()
        composeRule.onNodeWithText(note.content.text).assertIsDisplayed()
        composeRule.onNodeWithText("1 nota · un espacio para tus ideas").assertIsDisplayed()
    }

    @Test
    fun given_note_when_clicked_then_sends_note_clicked() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + note.id).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NoteClicked(note.id)), intents)
    }

    @Test
    fun given_screen_when_create_and_settings_clicked_then_sends_intents() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.CREATE_NOTE).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.SETTINGS).performClick()

        assertEquals(listOf(NoteListIntent.CreateNoteClicked, NoteListIntent.SettingsClicked), intents)
    }

    @Test
    fun given_loading_state_when_displayed_then_shows_progress() {
        setContent(NoteListUiState(isLoading = true))

        composeRule.onNodeWithTag(NoteListTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun given_no_notes_when_displayed_then_shows_empty_message() {
        setContent(NoteListUiState(isLoading = false))

        composeRule.onNodeWithTag(NoteListTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun given_error_when_displayed_then_shows_friendly_error() {
        setContent(NoteListUiState(isLoading = false, hasError = true))

        composeRule.onNodeWithTag(NoteListTestTags.ERROR).assertIsDisplayed()
    }

    @Test
    fun given_filter_chips_when_clicked_then_sends_filter_selected() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.WORK.name).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.PERSONAL.name).performClick()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.ALL.name).performClick()

        assertEquals(
            listOf(
                NoteListIntent.FilterSelected(NoteFilter.WORK),
                NoteListIntent.FilterSelected(NoteFilter.PERSONAL),
                NoteListIntent.FilterSelected(NoteFilter.ALL),
            ),
            intents,
        )
    }

    @Test
    fun given_selected_filter_when_displayed_then_only_that_chip_is_selected() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(note), selectedFilter = NoteFilter.WORK))

        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.WORK.name).assertIsSelected()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.ALL.name).assertIsNotSelected()
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.PERSONAL.name).assertIsNotSelected()
    }

    @Test
    fun given_no_notes_and_all_filter_when_displayed_then_shows_general_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.ALL))

        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertIsDisplayed()
    }

    @Test
    fun given_no_notes_and_work_filter_when_displayed_then_shows_work_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.WORK))

        composeRule.onNodeWithText(string(R.string.note_list_empty_work)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertDoesNotExist()
    }

    @Test
    fun given_no_notes_and_personal_filter_when_displayed_then_shows_personal_empty_message() {
        setContent(NoteListUiState(isLoading = false, selectedFilter = NoteFilter.PERSONAL))

        composeRule.onNodeWithText(string(R.string.note_list_empty_personal)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_list_empty)).assertDoesNotExist()
    }

    @Test
    fun given_notes_when_typing_in_search_field_then_sends_query_changed() {
        setStatefulContent(searchableState())

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_FIELD).performTextInput("reunión")

        assertEquals(listOf<NoteListIntent>(NoteListIntent.QueryChanged("reunión")), intents)
    }

    @Test
    fun given_empty_query_when_displayed_then_clear_button_does_not_exist() {
        setContent(searchableState(query = ""))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_CLEAR).assertDoesNotExist()
    }

    @Test
    fun given_query_when_clear_clicked_then_sends_clear_query_clicked() {
        setStatefulContent(searchableState(query = "reunión"))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_CLEAR)
            .assertContentDescriptionEquals(string(R.string.note_list_search_clear))
            .performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.ClearQueryClicked), intents)
        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_CLEAR).assertDoesNotExist()
    }

    @Test
    fun given_search_with_one_match_when_displayed_then_summary_shows_singular_results() {
        setContent(searchableState(query = "reunión", notes = listOf(note)))

        composeRule.onNodeWithTag(NoteListTestTags.SUMMARY).assertTextEquals(quantityString(R.plurals.note_list_search_results, 1))
        composeRule.onNodeWithText(quantityString(R.plurals.note_list_summary, 1)).assertDoesNotExist()
    }

    @Test
    fun given_search_with_two_matches_when_displayed_then_summary_shows_plural_results() {
        setContent(searchableState(query = "e", notes = listOf(note, note.copy(id = 43, title = "Escapada"))))

        composeRule.onNodeWithTag(NoteListTestTags.SUMMARY).assertTextEquals(quantityString(R.plurals.note_list_search_results, 2))
        composeRule.onNodeWithText(quantityString(R.plurals.note_list_summary, 2)).assertDoesNotExist()
    }

    @Test
    fun given_search_without_matches_when_displayed_then_shows_no_results_with_trimmed_query_and_not_empty() {
        setContent(searchableState(query = "  viaje  ", notes = emptyList()))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_NO_RESULTS)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.note_list_search_no_results, "viaje"))
        composeRule.onNodeWithTag(NoteListTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun given_no_notes_at_all_when_displayed_then_search_field_is_disabled_and_shows_empty() {
        setContent(NoteListUiState(isLoading = false, totalCount = 0))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_FIELD).assertIsNotEnabled()
        composeRule.onNodeWithTag(NoteListTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun given_active_search_when_displayed_then_chips_are_inactive_and_not_selected() {
        setContent(searchableState(query = "reunión", notes = listOf(note), selectedFilter = NoteFilter.WORK))

        val inactive = SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            string(R.string.note_list_filter_inactive),
        )
        NoteFilter.entries.forEach { filter ->
            composeRule.onNodeWithTag(NoteListTestTags.FILTER + filter.name)
                .assert(inactive)
                .assertIsNotSelected()
        }
    }

    @Test
    fun given_active_search_when_chip_clicked_then_sends_filter_selected() {
        setContent(searchableState(query = "reunión", notes = listOf(note), selectedFilter = NoteFilter.WORK))

        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.PERSONAL.name).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.FilterSelected(NoteFilter.PERSONAL)), intents)
    }

    @Test
    fun given_no_search_when_displayed_then_chips_have_no_inactive_state_and_selected_chip_is_selected() {
        setContent(searchableState(query = "", notes = listOf(note), selectedFilter = NoteFilter.WORK))

        NoteFilter.entries.forEach { filter ->
            composeRule.onNodeWithTag(NoteListTestTags.FILTER + filter.name)
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }
        composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.WORK.name).assertIsSelected()
    }

    @Test
    fun given_note_with_formatted_content_when_displayed_then_card_shows_text_with_its_styles() {
        val text = "Un café rápido"
        val formatted = note.copy(
            content = buildFormattedContent(
                text,
                listOf(FormatSpan(0, 2, TextFormat.BOLD), FormatSpan(8, 14, TextFormat.UNDERLINE)),
            ),
        )
        setContent(NoteListUiState(isLoading = false, notes = listOf(formatted)))

        val content = composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()

        assertEquals(
            listOf(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 8, 14),
            ),
            content.textLayout().layoutInput.text.spanStyles,
        )
    }

    @Test
    fun given_long_formatted_content_when_displayed_then_card_is_shown_and_content_is_clipped() {
        val text = (1..30).joinToString(separator = "\n") { "Línea $it con formato" }
        val formatted = note.copy(
            content = buildFormattedContent(
                text,
                listOf(FormatSpan(0, text.length, TextFormat.BOLD), FormatSpan(10, 200, TextFormat.STRIKETHROUGH)),
            ),
        )
        setContent(NoteListUiState(isLoading = false, notes = listOf(formatted)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + formatted.id).assertIsDisplayed()
        val layout = composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed().textLayout()

        assertTrue(layout.hasVisualOverflow)
        assertEquals(CONTENT_MAX_LINES, layout.lineCount)
    }

    @Test
    fun given_note_without_color_when_displayed_then_card_shows_title_and_content() {
        val uncolored = note.copy(color = NoteColor.NONE)
        setContent(NoteListUiState(isLoading = false, notes = listOf(uncolored)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + uncolored.id).assertIsDisplayed()
        composeRule.onNodeWithText(uncolored.title).assertIsDisplayed()
        composeRule.onNodeWithText(uncolored.content.text).assertIsDisplayed()
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun searchableState(
        query: String = "",
        notes: List<NoteItemUi> = listOf(note),
        selectedFilter: NoteFilter = NoteFilter.ALL,
    ) = NoteListUiState(
        isLoading = false,
        notes = notes,
        selectedFilter = selectedFilter,
        query = query,
        totalCount = TOTAL_COUNT,
    )

    private val resources get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun string(id: Int, vararg args: Any): String = resources.getString(id, *args)

    private fun quantityString(id: Int, count: Int): String = resources.getQuantityString(id, count, count)

    private companion object {
        const val TOTAL_COUNT = 3
        const val CONTENT_MAX_LINES = 8
    }
}
