package com.example.notesapp.presentation.help

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Pantalla Ayuda y comentarios (spec 013), en ambos temas. */
@RunWith(Parameterized::class)
class HelpScreenTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HelpIntent>()

    private fun setContent(state: HelpUiState) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                HelpScreen(state = state, onIntent = { intents += it })
            }
        }
    }

    /**
     * Estado que aplica los intents como el ViewModel (sin filtrar): con un estado fijo el campo de texto
     * volvería a enviar el valor anterior al perder el foco.
     */
    private fun setStatefulContent(initial: HelpUiState) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            NotesAppTheme(darkTheme = darkTheme) {
                HelpScreen(
                    state = state,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is HelpIntent.QueryChanged -> state.copy(query = intent.query)
                            HelpIntent.ClearQueryClicked -> state.copy(query = "")
                            is HelpIntent.FaqClicked -> state.copy(
                                faqs = state.faqs.map { faq ->
                                    if (faq.id == intent.id) faq.copy(isExpanded = !faq.isExpanded) else faq
                                },
                            )
                            HelpIntent.BackClicked -> state
                        }
                    },
                )
            }
        }
    }

    private fun faq(id: Int, isExpanded: Boolean = false) = FaqUi(
        id = id,
        question = "Pregunta $id",
        answer = "Respuesta $id",
        isExpanded = isExpanded,
    )

    private fun loadedState(
        query: String = "",
        faqs: List<FaqUi> = listOf(faq(id = 1), faq(id = 2), faq(id = 3)),
    ) = HelpUiState(query = query, faqs = faqs, isLoading = false)

    @Test
    fun given_help_when_displayed_then_shows_title_and_labelled_back_button() {
        setContent(loadedState())

        composeRule.onNodeWithText(string(R.string.help_title)).assertIsDisplayed()
        node(HelpTestTags.BACK)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertContentDescriptionEquals(string(R.string.help_back))
    }

    @Test
    fun given_help_when_back_clicked_then_sends_back_clicked() {
        setContent(loadedState())

        node(HelpTestTags.BACK).performClick()

        assertEquals(listOf<HelpIntent>(HelpIntent.BackClicked), intents)
    }

    @Test
    fun given_help_when_displayed_then_shows_header_title_and_detail() {
        setContent(loadedState())

        composeRule.onNodeWithText(string(R.string.help_header_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.help_header_detail)).assertIsDisplayed()
    }

    @Test
    fun given_faqs_when_displayed_then_shows_section_title_and_every_question() {
        val faqs = listOf(faq(id = 1), faq(id = 2), faq(id = 3))
        setContent(loadedState(faqs = faqs))

        composeRule.onNodeWithText(string(R.string.help_section_faqs).uppercase()).assertIsDisplayed()
        faqs.forEach { faq ->
            scrollTo(HelpTestTags.faq(faq.id))
            node(HelpTestTags.faq(faq.id)).assertIsDisplayed().assert(hasText(faq.question))
        }
    }

    @Test
    fun given_collapsed_question_when_displayed_then_hides_answer_and_announces_closed() {
        setContent(loadedState(faqs = listOf(faq(id = 1, isExpanded = false))))

        node(HelpTestTags.faq(1))
            .assert(hasStateDescription(string(R.string.help_question_collapsed)))
            .assert(!hasText("Respuesta 1"))
        unmergedNode(HelpTestTags.answer(1)).assertDoesNotExist()
    }

    @Test
    fun given_expanded_question_when_displayed_then_shows_answer_and_announces_open() {
        setContent(loadedState(faqs = listOf(faq(id = 1, isExpanded = true))))

        node(HelpTestTags.faq(1)).assert(hasStateDescription(string(R.string.help_question_expanded)))
        unmergedNode(HelpTestTags.answer(1)).assertIsDisplayed().assertTextEquals("Respuesta 1")
    }

    @Test
    fun given_mixed_questions_when_displayed_then_only_expanded_one_shows_its_answer() {
        setContent(loadedState(faqs = listOf(faq(id = 1), faq(id = 2, isExpanded = true), faq(id = 3))))

        unmergedNode(HelpTestTags.answer(1)).assertDoesNotExist()
        unmergedNode(HelpTestTags.answer(2)).assertIsDisplayed()
        unmergedNode(HelpTestTags.answer(3)).assertDoesNotExist()
    }

    @Test
    fun given_question_when_displayed_then_is_button_with_click_action() {
        setContent(loadedState())

        node(HelpTestTags.faq(1)).assertHasClickAction().assert(hasRole(Role.Button))
    }

    @Test
    fun given_question_when_clicked_then_sends_faq_clicked_with_its_id() {
        setContent(loadedState())

        node(HelpTestTags.faq(2)).performClick()

        assertEquals(listOf<HelpIntent>(HelpIntent.FaqClicked(2)), intents)
    }

    @Test
    fun given_stateful_screen_when_question_clicked_twice_then_opens_and_closes_it() {
        setStatefulContent(loadedState())

        node(HelpTestTags.faq(1)).performClick()
        composeRule.waitForIdle()
        unmergedNode(HelpTestTags.answer(1)).assertIsDisplayed()
        node(HelpTestTags.faq(1)).assert(hasStateDescription(string(R.string.help_question_expanded)))
        node(HelpTestTags.faq(1)).performClick()
        composeRule.waitForIdle()

        unmergedNode(HelpTestTags.answer(1)).assertDoesNotExist()
        node(HelpTestTags.faq(1)).assert(hasStateDescription(string(R.string.help_question_collapsed)))
        assertEquals(listOf<HelpIntent>(HelpIntent.FaqClicked(1), HelpIntent.FaqClicked(1)), intents)
    }

    @Test
    fun given_search_field_when_typing_then_sends_query_changed() {
        setStatefulContent(loadedState())

        node(HelpTestTags.SEARCH_FIELD).performTextInput("nota")

        assertEquals(listOf<HelpIntent>(HelpIntent.QueryChanged("nota")), intents)
    }

    @Test
    fun given_search_field_when_displayed_then_is_labelled_with_placeholder() {
        setContent(loadedState())

        node(HelpTestTags.SEARCH_FIELD).assertContentDescriptionEquals(string(R.string.help_search_placeholder))
    }

    @Test
    fun given_empty_query_when_displayed_then_clear_button_does_not_exist() {
        setContent(loadedState(query = ""))

        node(HelpTestTags.SEARCH_CLEAR).assertDoesNotExist()
    }

    @Test
    fun given_query_when_displayed_then_shows_labelled_clear_button() {
        setContent(loadedState(query = "nota"))

        node(HelpTestTags.SEARCH_CLEAR)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(string(R.string.help_search_clear))
    }

    @Test
    fun given_query_when_clear_clicked_then_sends_clear_query_clicked_and_hides_button() {
        setStatefulContent(loadedState(query = "nota"))

        node(HelpTestTags.SEARCH_CLEAR).performClick()

        assertEquals(listOf<HelpIntent>(HelpIntent.ClearQueryClicked), intents)
        node(HelpTestTags.SEARCH_CLEAR).assertDoesNotExist()
    }

    @Test
    fun given_stateful_screen_when_typing_then_clear_button_appears() {
        setStatefulContent(loadedState())

        node(HelpTestTags.SEARCH_FIELD).performTextInput("nota")

        node(HelpTestTags.SEARCH_CLEAR).assertIsDisplayed()
    }

    @Test
    fun given_query_without_matches_when_displayed_then_shows_no_results_with_query_and_no_questions() {
        setContent(loadedState(query = "widget", faqs = emptyList()))

        node(HelpTestTags.NO_RESULTS)
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText(string(R.string.help_no_results, "widget"))))
        node(HelpTestTags.faq(1)).assertDoesNotExist()
    }

    @Test
    fun given_query_with_spaces_and_no_matches_when_displayed_then_no_results_shows_trimmed_query() {
        setContent(loadedState(query = "  widget  ", faqs = emptyList()))

        node(HelpTestTags.NO_RESULTS).assert(hasAnyDescendant(hasText(string(R.string.help_no_results, "widget"))))
    }

    @Test
    fun given_matches_when_displayed_then_no_results_does_not_exist() {
        setContent(loadedState(query = "pregunta"))

        node(HelpTestTags.NO_RESULTS).assertDoesNotExist()
    }

    @Test
    fun given_loading_with_query_when_displayed_then_no_results_does_not_exist() {
        setContent(HelpUiState(query = "widget", faqs = emptyList(), isLoading = true))

        node(HelpTestTags.NO_RESULTS).assertDoesNotExist()
    }

    @Test
    fun given_help_when_displayed_then_shows_rate_section_row_and_note() {
        setContent(loadedState())

        assertRateSectionDisplayed()
    }

    @Test
    fun given_query_with_matches_when_displayed_then_rate_section_is_still_visible() {
        setContent(loadedState(query = "pregunta", faqs = listOf(faq(id = 2))))

        assertRateSectionDisplayed()
    }

    @Test
    fun given_query_without_matches_when_displayed_then_rate_section_is_still_visible() {
        setContent(loadedState(query = "widget", faqs = emptyList()))

        assertRateSectionDisplayed()
    }

    @Test
    fun given_rate_row_when_displayed_then_is_disabled_without_click_action_and_labelled() {
        setContent(loadedState())

        scrollTo(HelpTestTags.RATE_ROW)
        node(HelpTestTags.RATE_ROW)
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assertHasNoClickAction()
            .assertContentDescriptionEquals(
                listOf(string(R.string.help_rate), string(R.string.help_rate_detail)).joinToString(),
            )
    }

    @Test
    fun given_rate_row_when_clicked_then_sends_no_intent() {
        setContent(loadedState())

        scrollTo(HelpTestTags.RATE_ROW)
        node(HelpTestTags.RATE_ROW).performClick()
        composeRule.waitForIdle()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun given_resources_when_read_then_visible_texts_match_the_spec() {
        val expected = mapOf(
            R.string.help_title to "Ayuda y comentarios",
            R.string.help_header_title to "¿En qué podemos ayudarte?",
            R.string.help_header_detail to "Un poco de ayuda para que tus ideas sigan fluyendo.",
            R.string.help_search_clear to "Limpiar búsqueda",
            R.string.help_section_rate to "Valora la app",
            R.string.help_rate to "Calificar en Google Play",
            R.string.help_rate_note to "Se abrirá Google Play para dejar tu reseña.",
            R.string.help_question_expanded to "Abierta",
            R.string.help_question_collapsed to "Cerrada",
        )

        expected.forEach { (id, text) -> assertEquals(text, string(id)) }
        assertEquals("No hay preguntas que coincidan con «x»", string(R.string.help_no_results, "x"))
    }

    private fun assertRateSectionDisplayed() {
        scrollTo(HelpTestTags.RATE_NOTE)
        composeRule.onNodeWithText(string(R.string.help_section_rate).uppercase()).assertIsDisplayed()
        node(HelpTestTags.RATE_ROW).assertIsDisplayed()
        node(HelpTestTags.RATE_NOTE)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.help_rate_note))
    }

    private fun scrollTo(tag: String) {
        node(HelpTestTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    private fun node(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun unmergedNode(tag: String): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true)

    private fun hasStateDescription(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)
    }
}
