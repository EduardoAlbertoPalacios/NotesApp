package com.example.notesapp.presentation.notes.list

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.ThemeTestColors
import com.example.notesapp.presentation.assertColorEquals
import com.example.notesapp.presentation.dominantColor
import com.example.notesapp.presentation.notes.common.NoteDateLabel
import com.example.notesapp.presentation.notes.common.TASK_BREAK
import com.example.notesapp.presentation.notes.common.TASK_SEPARATOR
import com.example.notesapp.presentation.notes.common.buildCardContent
import com.example.notesapp.presentation.notes.common.buildFormattedContent
import com.example.notesapp.presentation.textColor
import com.example.notesapp.presentation.theme.NotesAppTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun setContent(state: NoteListUiState, darkTheme: Boolean = true) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
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
                            is NoteListIntent.NotesViewSelected -> state.copy(notesView = intent.view)
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

    @Test
    fun given_note_with_tasks_when_displayed_then_card_shows_task_text() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(checklistNote)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + checklistNote.id).assertIsDisplayed()
        composeRule.onNodeWithText("Tomates", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Pan", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun given_note_with_done_and_pending_tasks_when_displayed_then_checkboxes_announce_their_state() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(checklistNote)))

        composeRule.onNodeWithContentDescription(string(R.string.note_editor_task_done), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.note_editor_task_pending), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun given_note_with_tasks_when_card_clicked_then_sends_note_clicked() {
        setContent(NoteListUiState(isLoading = false, notes = listOf(checklistNote)))

        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + checklistNote.id).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NoteClicked(checklistNote.id)), intents)
    }

    /** Una sola nota a la vez: con los ocho colores, parte de la cuadrícula quedaría fuera de la pantalla. */
    private fun assertCardColors(darkTheme: Boolean, noneColor: Color, view: NotesView? = null) {
        var color by mutableStateOf(NoteColor.NONE)
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                NoteListScreen(
                    state = NoteListUiState(isLoading = false, notes = listOf(note.copy(color = color)), notesView = view),
                    onIntent = {},
                )
            }
        }

        (ThemeTestColors.NoteContainers + (NoteColor.NONE to noneColor)).forEach { (noteColor, expected) ->
            color = noteColor
            composeRule.waitForIdle()

            assertColorEquals(
                "Tarjeta $noteColor",
                expected,
                composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + note.id).captureToImage().dominantColor(),
            )
        }
    }

    private fun setLightContent(card: NoteItemUi) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = false) {
                NoteListScreen(state = NoteListUiState(isLoading = false, notes = listOf(card)), onIntent = {})
            }
        }
    }

    private fun cardText(text: String, substring: Boolean = false) =
        composeRule.onNodeWithText(text, substring = substring, useUnmergedTree = true)

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_light_theme_when_displayed_then_each_card_is_painted_with_its_color() {
        assertCardColors(darkTheme = false, noneColor = ThemeTestColors.LightSurface)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_dark_theme_when_displayed_then_each_card_is_painted_with_its_color() {
        assertCardColors(darkTheme = true, noneColor = ThemeTestColors.DarkSurface)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_light_theme_when_displayed_then_screen_background_is_light() {
        setLightContent(note)

        assertColorEquals(
            "Fondo de Mis notas",
            ThemeTestColors.LightBackground,
            composeRule.onRoot().captureToImage().dominantColor(),
        )
    }

    @Test
    fun given_light_theme_and_note_without_color_when_displayed_then_card_texts_are_dark() {
        setLightContent(note.copy(color = NoteColor.NONE))

        assertColorEquals("Título", ThemeTestColors.LightText, cardText(note.title).textColor())
        assertColorEquals("Contenido", ThemeTestColors.LightText, cardText(note.content.text).textColor())
        assertColorEquals(
            "Fecha",
            ThemeTestColors.LightSecondaryText,
            cardText(string(R.string.note_date_yesterday)).textColor(),
        )
    }

    @Test
    fun given_light_theme_and_colored_note_when_displayed_then_card_texts_use_note_colors() {
        setLightContent(note.copy(color = NoteColor.ROSE))

        assertColorEquals("Título", ThemeTestColors.NoteText, cardText(note.title).textColor())
        assertColorEquals("Contenido", ThemeTestColors.NoteText, cardText(note.content.text).textColor())
        assertColorEquals(
            "Fecha",
            ThemeTestColors.NoteMetadata,
            cardText(string(R.string.note_date_yesterday)).textColor(),
        )
    }

    @Test
    fun given_screen_when_displayed_then_selector_is_below_filters_with_grid_then_list() {
        setContent(gridState())

        val selector = viewSelectorBounds()
        val filter = composeRule.onNodeWithTag(NoteListTestTags.FILTER + NoteFilter.ALL.name).getUnclippedBoundsInRoot()
        val grid = viewOption(NotesView.GRID).getUnclippedBoundsInRoot()
        val list = viewOption(NotesView.LIST).getUnclippedBoundsInRoot()

        assertTrue(filter.bottom <= selector.top)
        assertTrue(grid.right <= list.left)
        assertEquals(grid.top, list.top)
    }

    @Test
    fun given_each_view_when_displayed_then_only_that_option_is_selected() {
        var state by mutableStateOf(gridState())
        composeRule.setContent {
            NotesAppTheme {
                NoteListScreen(state = state, onIntent = {})
            }
        }

        NotesView.entries.forEach { view ->
            state = state.copy(notesView = view)

            viewOption(view).assertIsSelected()
            NotesView.entries.filter { it != view }.forEach { viewOption(it).assertIsNotSelected() }
        }
    }

    @Test
    fun given_view_not_read_yet_when_displayed_then_no_option_is_selected() {
        setContent(NoteListUiState())

        NotesView.entries.forEach { viewOption(it).assertIsNotSelected() }
    }

    @Test
    fun given_view_not_read_yet_when_displayed_then_shows_loading_and_no_cards() {
        setContent(NoteListUiState(isLoading = true, notes = listOf(note), notesView = null))

        composeRule.onNodeWithTag(NoteListTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + note.id).assertDoesNotExist()
    }

    @Test
    fun given_grid_view_when_list_option_clicked_then_sends_notes_view_selected() {
        setContent(gridState())

        viewOption(NotesView.LIST).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.LIST)), intents)
    }

    @Test
    fun given_list_view_when_grid_option_clicked_then_sends_notes_view_selected() {
        setContent(listState())

        viewOption(NotesView.GRID).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.GRID)), intents)
    }

    @Test
    fun given_marked_option_when_clicked_then_sends_it_and_selection_does_not_change() {
        setStatefulContent(gridState())

        viewOption(NotesView.GRID).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.GRID)), intents)
        viewOption(NotesView.GRID).assertIsSelected()
    }

    @Test
    fun given_two_notes_in_grid_when_list_option_clicked_then_cards_move_to_one_per_row() {
        setStatefulContent(gridState(listOf(listNote(id = 1), listNote(id = 2))))
        assertEquals(card(1).getUnclippedBoundsInRoot().top, card(2).getUnclippedBoundsInRoot().top)

        viewOption(NotesView.LIST).performClick()

        viewOption(NotesView.LIST).assertIsSelected()
        assertTrue(card(1).getUnclippedBoundsInRoot().bottom <= card(2).getUnclippedBoundsInRoot().top)
    }

    @Test
    fun given_active_search_when_displayed_then_selector_stays_active_and_sends_selection() {
        setContent(searchableState(query = "reunión", notes = listOf(note)).copy(notesView = NotesView.GRID))

        viewOption(NotesView.GRID).assertIsEnabled().assertIsSelected()
        viewOption(NotesView.LIST).assertIsEnabled().assertHasClickAction().performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.LIST)), intents)
    }

    @Test
    fun given_search_without_results_when_displayed_then_selector_stays_active_and_sends_selection() {
        setContent(searchableState(query = "viaje", notes = emptyList()).copy(notesView = NotesView.LIST))

        viewOption(NotesView.LIST).assertIsEnabled().assertIsSelected()
        viewOption(NotesView.GRID).assertIsEnabled().assertHasClickAction().performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.GRID)), intents)
    }

    @Test
    fun given_no_notes_at_all_when_displayed_then_selector_stays_active_and_sends_selection() {
        setContent(NoteListUiState(isLoading = false, totalCount = 0, notesView = NotesView.GRID))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_FIELD).assertIsNotEnabled()
        viewOption(NotesView.GRID).assertIsEnabled().assertIsSelected()
        viewOption(NotesView.LIST).assertIsEnabled().assertHasClickAction().performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NotesViewSelected(NotesView.LIST)), intents)
    }

    @Test
    fun given_selector_options_when_displayed_then_are_radio_buttons_labelled_with_view_name() {
        setContent(gridState())

        mapOf(NotesView.GRID to R.string.notes_view_grid, NotesView.LIST to R.string.notes_view_list).forEach { (view, label) ->
            viewOption(view)
                .assertTextEquals(string(label))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assertHasClickAction()
        }
    }

    @Test
    fun given_selector_options_when_displayed_then_touch_targets_are_at_least_44_dp() {
        setContent(gridState())

        NotesView.entries.forEach { view ->
            viewOption(view).assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    private fun assertSelectorColors(darkTheme: Boolean, unselectedText: Color) {
        setContent(gridState(), darkTheme)

        assertColorEquals(
            "Opción marcada",
            ThemeTestColors.NoteContainers.getValue(NoteColor.AQUA),
            viewOption(NotesView.GRID).captureToImage().dominantColor(),
        )
        assertColorEquals(
            "Texto de la opción sin marcar",
            unselectedText,
            cardText(string(R.string.notes_view_list)).textColor(),
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_dark_theme_when_displayed_then_selector_marks_option_with_theme_colors() {
        assertSelectorColors(darkTheme = true, unselectedText = ThemeTestColors.DarkSecondaryText)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_light_theme_when_displayed_then_selector_marks_option_with_theme_colors() {
        assertSelectorColors(darkTheme = false, unselectedText = ThemeTestColors.LightSecondaryText)
    }

    @Test
    fun given_grid_view_when_displayed_then_cards_share_row_in_two_columns() {
        setContent(gridState(listOf(listNote(id = 1), listNote(id = 2))))

        val first = card(1).getUnclippedBoundsInRoot()
        val second = card(2).getUnclippedBoundsInRoot()

        assertEquals(first.top, second.top)
        assertTrue(first.right <= second.left)
        val content = contentBounds()
        assertTrue(first.right - first.left < (content.right - content.left) / 2)
    }

    @Test
    fun given_grid_view_when_note_has_category_then_shows_no_tag_and_grid_content() {
        val categorized = listNote(id = 1, category = NoteCategory.WORK)
            .copy(content = AnnotatedString("Solo en cuadrícula"), listContent = AnnotatedString("Solo en lista"))
        setContent(gridState(listOf(categorized)))

        categoryTag(1).assertDoesNotExist()
        composeRule.onNodeWithText("Solo en cuadrícula").assertIsDisplayed()
        composeRule.onNodeWithText("Solo en lista").assertDoesNotExist()
        composeRule.onNode(
            hasText(string(R.string.note_category_work)) and hasAnyAncestor(hasTestTag(NoteListTestTags.NOTE_CARD + 1)),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun given_view_not_read_yet_with_notes_loaded_when_displayed_then_uses_grid_columns() {
        setContent(gridState(listOf(listNote(id = 1), listNote(id = 2))).copy(notesView = null))

        assertEquals(card(1).getUnclippedBoundsInRoot().top, card(2).getUnclippedBoundsInRoot().top)
    }

    @Test
    fun given_list_view_when_displayed_then_each_card_has_its_row_at_full_width_keeping_order() {
        setContent(listState(listOf(listNote(id = 1), listNote(id = 2), listNote(id = 3))))

        val content = contentBounds()
        val bounds = listOf(1L, 2L, 3L).map { card(it).getUnclippedBoundsInRoot() }

        bounds.zipWithNext().forEach { (upper, lower) -> assertTrue(upper.bottom <= lower.top) }
        bounds.forEach { card ->
            assertEquals(content.left, card.left)
            assertEquals(content.right, card.right)
        }
    }

    @Test
    fun given_list_view_when_displayed_then_shows_list_content_instead_of_grid_content() {
        val note = listNote(id = 1).copy(content = AnnotatedString("Solo en cuadrícula"), listContent = AnnotatedString("Solo en lista"))
        setContent(listState(listOf(note)))

        composeRule.onNodeWithText("Solo en lista").assertIsDisplayed()
        composeRule.onNodeWithText("Solo en cuadrícula").assertDoesNotExist()
    }

    @Test
    fun given_short_content_in_list_view_when_displayed_then_is_not_clipped() {
        val text = "Línea uno\nLínea dos\nLínea tres\nLínea cuatro"
        setContent(listState(listOf(listNote(id = 1, content = text))))

        val layout = cardText(text).assertIsDisplayed().textLayout()

        assertEquals(LIST_CONTENT_MAX_LINES, layout.lineCount)
        assertFalse(layout.hasVisualOverflow)
    }

    @Test
    fun given_long_content_in_list_view_when_displayed_then_is_clipped_to_four_lines_with_ellipsis() {
        val text = (1..30).joinToString(separator = "\n") { "Línea $it" }
        setContent(listState(listOf(listNote(id = 1, content = text))))

        val layout = cardText(text).assertIsDisplayed().textLayout()

        assertTrue(layout.hasVisualOverflow)
        assertEquals(LIST_CONTENT_MAX_LINES, layout.lineCount)
        assertEquals(TextOverflow.Ellipsis, layout.layoutInput.overflow)
    }

    @Test
    fun given_short_consecutive_tasks_in_list_view_when_displayed_then_share_one_line_without_mark() {
        setContent(listState(listOf(taskNote("Pan\nSal\nTé", isDone = listOf(false, false, false)))))

        val layout = cardText("Pan", substring = true).textLayout()

        assertEquals(
            "${ZERO_WIDTH}Pan$TASK_SEPARATOR${ZERO_WIDTH}Sal$TASK_SEPARATOR${ZERO_WIDTH}Té",
            layout.layoutInput.text.text,
        )
        assertEquals(1, layout.lineCount)
    }

    @Test
    fun given_tasks_that_do_not_share_a_line_when_displayed_then_second_starts_next_line_with_its_checkbox() {
        setContent(listState(listOf(taskNote("$LONG_TASK uno\n$LONG_TASK dos", isDone = listOf(false, false)))))

        val layout = cardText(LONG_TASK, substring = true).textLayout()
        val text = layout.layoutInput.text.text

        assertEquals("$ZERO_WIDTH$LONG_TASK uno\n$ZERO_WIDTH$LONG_TASK dos", text)
        assertEquals(2, layout.lineCount)
        assertEquals(text.indexOf('\n') + 1, layout.getLineStart(1))
    }

    @Test
    fun given_tasks_in_grid_view_when_displayed_then_each_keeps_its_line_without_mark() {
        setContent(gridState(listOf(taskNote("Pan\nSal", isDone = listOf(false, false)))))

        val text = cardText("Pan", substring = true).textLayout().layoutInput.text.text

        assertEquals("${ZERO_WIDTH}Pan\n${ZERO_WIDTH}Sal", text)
        assertFalse(TASK_BREAK in text)
    }

    @Test
    fun given_done_task_in_list_view_when_displayed_then_only_its_text_is_struck_through() {
        setContent(listState(listOf(taskNote("Pan\nSal", isDone = listOf(false, true)))))

        val laidOut = cardText("Pan", substring = true).textLayout().layoutInput.text

        val doneStart = laidOut.text.indexOf("Sal")
        assertEquals(
            listOf(AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.LineThrough), doneStart, doneStart + 3)),
            laidOut.spanStyles,
        )
    }

    @Test
    fun given_tasks_in_list_view_when_displayed_then_checkboxes_announce_their_state() {
        setContent(listState(listOf(taskNote("Pan\nSal", isDone = listOf(false, true)))))

        composeRule.onNodeWithContentDescription(string(R.string.note_editor_task_pending), useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.note_editor_task_done), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun given_each_category_in_list_view_when_displayed_then_card_shows_its_tag() {
        setContent(
            listState(
                listOf(
                    listNote(id = 1, category = NoteCategory.WORK),
                    listNote(id = 2, category = NoteCategory.PERSONAL),
                ),
            ),
        )

        categoryTag(1).assertIsDisplayed().assertTextEquals(string(R.string.note_category_work))
        categoryTag(2).assertIsDisplayed().assertTextEquals(string(R.string.note_category_personal))
    }

    @Test
    fun given_note_without_category_in_list_view_when_displayed_then_shows_no_tag_and_date_stays_right() {
        setContent(listState(listOf(listNote(id = 1))))

        categoryTag(1).assertDoesNotExist()
        assertDateIsAlignedRight(cardId = 1)
    }

    @Test
    fun given_note_with_category_in_list_view_when_displayed_then_date_is_on_tag_line_aligned_right() {
        setContent(listState(listOf(listNote(id = 1, category = NoteCategory.WORK))))

        val tag = categoryTag(1).getUnclippedBoundsInRoot()
        val date = cardText(string(R.string.note_date_yesterday)).getUnclippedBoundsInRoot()

        assertTrue(tag.right < date.left)
        val dateCenter = (date.top + date.bottom) / 2
        assertTrue(tag.top <= dateCenter && dateCenter <= tag.bottom)
        assertDateIsAlignedRight(cardId = 1)
    }

    @Test
    fun given_note_without_content_in_list_view_when_displayed_then_shows_title_tag_and_date() {
        setContent(listState(listOf(listNote(id = 1, content = "", category = NoteCategory.WORK))))

        cardText(TITLE).assertIsDisplayed()
        categoryTag(1).assertIsDisplayed()
        cardText(string(R.string.note_date_yesterday)).assertIsDisplayed()
    }

    @Test
    fun given_pinned_note_in_list_view_when_displayed_then_shows_pin_icon() {
        setContent(listState(listOf(listNote(id = 1, isPinned = true))))

        composeRule.onNodeWithContentDescription(string(R.string.note_list_pinned), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun given_not_pinned_note_in_list_view_when_displayed_then_has_no_pin_icon() {
        setContent(listState(listOf(listNote(id = 1, isPinned = false))))

        composeRule.onNodeWithContentDescription(string(R.string.note_list_pinned), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun given_note_with_image_in_list_view_when_displayed_then_image_is_on_top_at_card_width() {
        setContent(listState(listOf(listNote(id = 1).copy(imagePath = "/no/existe.jpg"))))

        val card = card(1).getUnclippedBoundsInRoot()
        val image = composeRule
            .onNodeWithContentDescription(string(R.string.note_list_image), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(card.top, image.top)
        assertEquals(card.left, image.left)
        assertEquals(card.right, image.right)
        assertTrue(image.bottom <= cardText(TITLE).getUnclippedBoundsInRoot().top)
    }

    @Test
    fun given_note_without_image_in_list_view_when_displayed_then_has_no_image() {
        setContent(listState(listOf(listNote(id = 1))))

        composeRule.onNodeWithContentDescription(string(R.string.note_list_image), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun given_formatted_note_in_list_view_when_displayed_then_card_keeps_its_styles() {
        val text = "Un café rápido"
        val formatted = buildFormattedContent(
            text,
            listOf(FormatSpan(0, 2, TextFormat.BOLD), FormatSpan(8, 14, TextFormat.UNDERLINE)),
        )
        setContent(listState(listOf(listNote(id = 1).copy(content = formatted, listContent = formatted))))

        assertEquals(
            listOf(
                AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), 0, 2),
                AnnotatedString.Range(SpanStyle(textDecoration = TextDecoration.Underline), 8, 14),
            ),
            cardText(text).textLayout().layoutInput.text.spanStyles,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_dark_theme_and_list_view_when_displayed_then_each_card_is_painted_with_its_color() {
        assertCardColors(darkTheme = true, noneColor = ThemeTestColors.DarkSurface, view = NotesView.LIST)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_light_theme_and_list_view_when_displayed_then_each_card_is_painted_with_its_color() {
        assertCardColors(darkTheme = false, noneColor = ThemeTestColors.LightSurface, view = NotesView.LIST)
    }

    @Test
    fun given_colored_note_in_list_view_when_displayed_then_texts_use_note_colors() {
        setContent(listState(listOf(listNote(id = 1, category = NoteCategory.WORK).copy(color = NoteColor.ROSE))))

        assertColorEquals("Título", ThemeTestColors.NoteText, cardText(TITLE).textColor())
        assertColorEquals("Contenido", ThemeTestColors.NoteText, cardText(CONTENT).textColor())
        assertColorEquals("Etiqueta", ThemeTestColors.NoteMetadata, categoryTag(1).textColor())
        assertColorEquals("Fecha", ThemeTestColors.NoteMetadata, cardText(string(R.string.note_date_yesterday)).textColor())
    }

    @Test
    fun given_dark_theme_and_note_without_color_in_list_view_when_displayed_then_tag_uses_secondary_text() {
        setContent(listState(listOf(listNote(id = 1, category = NoteCategory.WORK).copy(color = NoteColor.NONE))), darkTheme = true)

        assertColorEquals("Etiqueta", ThemeTestColors.DarkSecondaryText, categoryTag(1).textColor())
    }

    @Test
    fun given_light_theme_and_note_without_color_in_list_view_when_displayed_then_tag_uses_secondary_text() {
        setContent(listState(listOf(listNote(id = 1, category = NoteCategory.WORK).copy(color = NoteColor.NONE))), darkTheme = false)

        assertColorEquals("Etiqueta", ThemeTestColors.LightSecondaryText, categoryTag(1).textColor())
    }

    @Test
    fun given_card_in_list_view_when_clicked_then_sends_note_clicked() {
        setContent(listState(listOf(listNote(id = 1))))

        card(1).performClick()

        assertEquals(listOf<NoteListIntent>(NoteListIntent.NoteClicked(1)), intents)
    }

    @Test
    fun given_list_view_when_loading_then_shows_same_progress() {
        setContent(NoteListUiState(isLoading = true, notesView = NotesView.LIST))

        composeRule.onNodeWithTag(NoteListTestTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun given_list_view_without_notes_when_displayed_then_shows_same_empty_message() {
        setContent(NoteListUiState(isLoading = false, notesView = NotesView.LIST))

        composeRule.onNodeWithTag(NoteListTestTags.EMPTY).assertIsDisplayed().assertTextEquals(string(R.string.note_list_empty))
    }

    @Test
    fun given_list_view_with_error_when_displayed_then_shows_same_error() {
        setContent(NoteListUiState(isLoading = false, hasError = true, notesView = NotesView.LIST))

        composeRule.onNodeWithTag(NoteListTestTags.ERROR).assertIsDisplayed().assertTextEquals(string(R.string.note_list_error))
    }

    @Test
    fun given_list_view_and_search_without_matches_when_displayed_then_shows_same_no_results() {
        setContent(searchableState(query = "viaje", notes = emptyList()).copy(notesView = NotesView.LIST))

        composeRule.onNodeWithTag(NoteListTestTags.SEARCH_NO_RESULTS)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.note_list_search_no_results, "viaje"))
    }

    @Test
    fun given_save_error_snackbar_when_shown_then_is_above_create_button() {
        val message = string(R.string.notes_view_save_error)
        val hostState = SnackbarHostState()
        composeRule.setContent {
            NotesAppTheme {
                NoteListScreen(state = gridState(), onIntent = {}, snackbarHostState = hostState)
            }
            LaunchedEffect(Unit) { hostState.showSnackbar(message, duration = SnackbarDuration.Indefinite) }
        }
        composeRule.waitUntil(timeoutMillis = SNACKBAR_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }

        val snackbar = composeRule.onNodeWithText(message).assertIsDisplayed().getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithTag(NoteListTestTags.CREATE_NOTE).assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(snackbar.bottom <= button.top)
    }

    private fun gridState(notes: List<NoteItemUi> = listOf(note)) =
        NoteListUiState(isLoading = false, notes = notes, notesView = NotesView.GRID)

    private fun listState(notes: List<NoteItemUi> = listOf(note)) =
        NoteListUiState(isLoading = false, notes = notes, notesView = NotesView.LIST)

    private fun listNote(
        id: Long,
        content: String = CONTENT,
        category: NoteCategory? = null,
        isPinned: Boolean = false,
    ) = NoteItemUi(
        id = id,
        title = TITLE,
        content = AnnotatedString(content),
        color = NoteColor.AQUA,
        isPinned = isPinned,
        date = NoteDateLabel.Yesterday,
        category = category,
    )

    private fun taskNote(text: String, isDone: List<Boolean>): NoteItemUi {
        val tasks = isDone.mapIndexed { line, done -> ChecklistItem(line = line, isDone = done) }
        return listNote(id = 1).copy(
            content = buildCardContent(text, emptyList(), tasks),
            listContent = buildCardContent(text, emptyList(), tasks, flowTasks = true),
        )
    }

    private fun viewOption(view: NotesView) = composeRule.onNodeWithTag(NoteListTestTags.VIEW_OPTION + view.name)

    private fun viewSelectorBounds() = composeRule.onNodeWithTag(NoteListTestTags.VIEW_SELECTOR).getUnclippedBoundsInRoot()

    /** El buscador ocupa todo el ancho del contenido. */
    private fun contentBounds() = composeRule.onNodeWithTag(NoteListTestTags.SEARCH_FIELD).getUnclippedBoundsInRoot()

    private fun card(id: Long) = composeRule.onNodeWithTag(NoteListTestTags.NOTE_CARD + id)

    private fun categoryTag(id: Long) = composeRule.onNodeWithTag(NoteListTestTags.CATEGORY_TAG + id, useUnmergedTree = true)

    /** El hueco a la derecha de la fecha es el mismo que a la izquierda del título: el relleno de la tarjeta. */
    private fun assertDateIsAlignedRight(cardId: Long) {
        val card = card(cardId).getUnclippedBoundsInRoot()
        val title = cardText(TITLE).getUnclippedBoundsInRoot()
        val date = cardText(string(R.string.note_date_yesterday)).getUnclippedBoundsInRoot()

        val leftGap = title.left - card.left
        val rightGap = card.right - date.right
        assertTrue("Izquierda $leftGap, derecha $rightGap", abs(leftGap.value - rightGap.value) <= 1f)
    }

    private val checklistNote
        get() = note.copy(
            id = 7,
            title = "Compra",
            content = buildCardContent(
                "Tomates\nPan",
                emptyList(),
                listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true)),
            ),
        )

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
        const val CAPTURE_MIN_SDK = 26
        const val LIST_CONTENT_MAX_LINES = 4
        const val SNACKBAR_TIMEOUT_MS = 5_000L
        const val TITLE = "Reunión de equipo"
        const val CONTENT = "Prototipo el viernes"
        const val ZERO_WIDTH = "\u200B"
        const val LONG_TASK = "Comprar tomates rojos maduros"
        val MIN_TOUCH_TARGET = 44.dp
    }
}
