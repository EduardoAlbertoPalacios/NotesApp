package com.example.notesapp.presentation.help

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.notesapp.domain.help.Faq
import com.example.notesapp.domain.help.SearchFaqsUseCase
import com.example.notesapp.presentation.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HelpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val faqs = FakeFaqRepository.defaultFaqs

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = HelpViewModel(savedStateHandle, SearchFaqsUseCase(FakeFaqRepository()))

    private fun Faq.toUi(isExpanded: Boolean = false) = FaqUi(
        id = id,
        question = question,
        answer = answer,
        isExpanded = isExpanded,
    )

    private fun HelpViewModel.expandedIds(): List<Int> =
        state.value.faqs.filter { it.isExpanded }.map { it.id }

    private fun HelpViewModel.visibleIds(): List<Int> = state.value.faqs.map { it.id }

    @Test
    fun `given faqs when created then state shows all of them collapsed and not loading`() {
        val viewModel = createViewModel()

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertEquals(faqs.map { it.toUi() }, state.faqs)
        assertFalse(state.isLoading)
        assertFalse(state.showNoResults)
    }

    @Test
    fun `given collapsed faq when clicked then opens only that one`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.FaqClicked(2))

        assertEquals(
            listOf(faqs[0].toUi(), faqs[1].toUi(isExpanded = true), faqs[2].toUi()),
            viewModel.state.value.faqs,
        )
    }

    @Test
    fun `given open faq when clicked again then closes it`() {
        val viewModel = createViewModel()
        viewModel.onIntent(HelpIntent.FaqClicked(2))

        viewModel.onIntent(HelpIntent.FaqClicked(2))

        assertEquals(faqs.map { it.toUi() }, viewModel.state.value.faqs)
    }

    @Test
    fun `given several faqs clicked when state read then all of them stay open`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.FaqClicked(1))
        viewModel.onIntent(HelpIntent.FaqClicked(3))

        assertEquals(listOf(1, 3), viewModel.expandedIds())
    }

    @Test
    fun `given several open faqs when one is closed then the others stay open`() {
        val viewModel = createViewModel()
        viewModel.onIntent(HelpIntent.FaqClicked(1))
        viewModel.onIntent(HelpIntent.FaqClicked(2))
        viewModel.onIntent(HelpIntent.FaqClicked(3))

        viewModel.onIntent(HelpIntent.FaqClicked(2))

        assertEquals(listOf(1, 3), viewModel.expandedIds())
    }

    @Test
    fun `given query without accents when it matches a question with accents then shows only that faq`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("DONDE"))

        val state = viewModel.state.value
        assertEquals("DONDE", state.query)
        assertEquals(listOf(faqs[0].toUi()), state.faqs)
        assertFalse(state.showNoResults)
    }

    @Test
    fun `given query that matches only an answer when changed then shows that faq`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("papelera"))

        assertEquals(listOf(2), viewModel.visibleIds())
    }

    @Test
    fun `given query without accents when it matches an answer with accents then shows that faq`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("manten pulsada"))

        assertEquals(listOf(3), viewModel.visibleIds())
    }

    @Test
    fun `given query matching several faqs when changed then keeps their original order`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("como"))

        assertEquals(listOf(2, 3), viewModel.visibleIds())
    }

    @Test
    fun `given query without matches when changed then shows no results`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("widget"))

        val state = viewModel.state.value
        assertEquals(emptyList<FaqUi>(), state.faqs)
        assertTrue(state.showNoResults)
    }

    @Test
    fun `given query with outer spaces and no matches when changed then no results query is trimmed`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("  widget  "))

        val state = viewModel.state.value
        assertTrue(state.showNoResults)
        assertEquals("  widget  ", state.query)
        assertEquals("widget", state.noResultsQuery)
    }

    @Test
    fun `given whitespace only query when changed then shows all faqs without no results`() {
        val viewModel = createViewModel()

        viewModel.onIntent(HelpIntent.QueryChanged("   "))

        val state = viewModel.state.value
        assertEquals("   ", state.query)
        assertEquals(faqs.map { it.toUi() }, state.faqs)
        assertFalse(state.showNoResults)
    }

    @Test
    fun `given filtered list when clear query clicked then restores empty query and all faqs`() {
        val viewModel = createViewModel()
        viewModel.onIntent(HelpIntent.QueryChanged("widget"))

        viewModel.onIntent(HelpIntent.ClearQueryClicked)

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertEquals(faqs.map { it.toUi() }, state.faqs)
        assertFalse(state.showNoResults)
    }

    @Test
    fun `given open faq when filtered out and back in then is still open`() {
        val viewModel = createViewModel()
        viewModel.onIntent(HelpIntent.FaqClicked(1))
        viewModel.onIntent(HelpIntent.QueryChanged("papelera"))

        viewModel.onIntent(HelpIntent.ClearQueryClicked)

        assertEquals(listOf(1), viewModel.expandedIds())
    }

    @Test
    fun `given open faq that matches the query when filtering then is shown open`() {
        val viewModel = createViewModel()
        viewModel.onIntent(HelpIntent.FaqClicked(3))

        viewModel.onIntent(HelpIntent.QueryChanged("como"))

        assertEquals(listOf(faqs[1].toUi(), faqs[2].toUi(isExpanded = true)), viewModel.state.value.faqs)
    }

    @Test
    fun `given saved query and open faqs when created then restores both`() {
        val savedStateHandle = SavedStateHandle(
            mapOf("help_query" to "como", "help_expanded_ids" to intArrayOf(2)),
        )

        val viewModel = createViewModel(savedStateHandle)

        val state = viewModel.state.value
        assertEquals("como", state.query)
        assertEquals(listOf(faqs[1].toUi(isExpanded = true), faqs[2].toUi()), state.faqs)
        assertFalse(state.isLoading)
    }

    @Test
    fun `given saved open faq hidden by saved query when query cleared then is shown open`() {
        val savedStateHandle = SavedStateHandle(
            mapOf("help_query" to "papelera", "help_expanded_ids" to intArrayOf(1)),
        )
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onIntent(HelpIntent.ClearQueryClicked)

        assertEquals(listOf(1), viewModel.expandedIds())
    }

    @Test
    fun `given query changed when saved state read then stores the query`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onIntent(HelpIntent.QueryChanged("papelera"))

        assertEquals("papelera", savedStateHandle.get<String>("help_query"))
    }

    @Test
    fun `given query when cleared then saved state stores empty query`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.onIntent(HelpIntent.QueryChanged("papelera"))

        viewModel.onIntent(HelpIntent.ClearQueryClicked)

        assertEquals("", savedStateHandle.get<String>("help_query"))
    }

    @Test
    fun `given faqs clicked when saved state read then stores open ids`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onIntent(HelpIntent.FaqClicked(3))
        viewModel.onIntent(HelpIntent.FaqClicked(1))

        assertArrayEquals(intArrayOf(3, 1), savedStateHandle.get<IntArray>("help_expanded_ids"))
    }

    @Test
    fun `given help screen when back clicked then emits navigate back without changing state`() = runTest {
        val viewModel = createViewModel()
        val stateBefore = viewModel.state.value

        viewModel.effects.test {
            viewModel.onIntent(HelpIntent.BackClicked)

            assertEquals(HelpEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
        assertEquals(stateBefore, viewModel.state.value)
    }
}
