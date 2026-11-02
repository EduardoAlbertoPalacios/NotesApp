package com.example.notesapp.presentation.settings

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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.ThemeTestColors
import com.example.notesapp.presentation.assertColorEquals
import com.example.notesapp.presentation.containsColor
import com.example.notesapp.presentation.textColor
import com.example.notesapp.presentation.theme.NotesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsScreenTest(private val darkTheme: Boolean) {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<SettingsIntent>()
    private val activeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    private val otherMode = if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK

    private fun setContent(
        state: SettingsUiState = SettingsUiState(themeMode = activeMode),
        appVersion: String = APP_VERSION,
    ) {
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = appVersion, onIntent = { intents += it })
            }
        }
    }

    /** Estado que aplica los intents como el ViewModel, para que la hoja se cierre de verdad. */
    private fun setStatefulContent(initial: SettingsUiState) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(
                    state = state,
                    appVersion = APP_VERSION,
                    onIntent = { intent ->
                        intents += intent
                        state = when (intent) {
                            is SettingsIntent.ThemeSelected -> state.copy(themeMode = intent.mode)
                            SettingsIntent.ThemeRowClicked -> state.copy(isThemeSheetVisible = true)
                            SettingsIntent.ThemeSheetDismissed -> state.copy(isThemeSheetVisible = false)
                            SettingsIntent.BackClicked -> state
                        }
                    },
                )
            }
        }
    }

    @Test
    fun given_settings_when_displayed_then_shows_privacy_notice() {
        setContent()

        composeRule.onNodeWithText(string(R.string.settings_privacy_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_privacy_detail)).assertIsDisplayed()
    }

    @Test
    fun given_settings_when_displayed_then_sections_and_rows_follow_design_order() {
        setContent()

        val nodes = listOf(
            composeRule.onNodeWithText(string(R.string.settings_privacy_title)),
            sectionHeader(R.string.settings_section_appearance),
            row(SettingsTestTags.THEME_ROW),
            row(SettingsTestTags.DEFAULT_COLOR_ROW),
            row(SettingsTestTags.NOTES_VIEW_ROW),
            sectionHeader(R.string.settings_section_notes),
            row(SettingsTestTags.AUTO_SAVE_ROW),
            row(SettingsTestTags.EXPORT_ROW),
            row(SettingsTestTags.TRASH_ROW),
            sectionHeader(R.string.settings_section_about),
            row(SettingsTestTags.HELP_ROW),
            row(SettingsTestTags.VERSION),
        )

        val tops = nodes.map { it.getUnclippedBoundsInRoot().top }
        assertEquals(tops.sortedBy { it.value }, tops)
        assertEquals(tops.size, tops.distinct().size)
    }

    @Test
    fun given_settings_when_displayed_then_each_row_has_its_name() {
        setContent()

        row(SettingsTestTags.THEME_ROW).assert(hasText(string(R.string.settings_theme)))
        disabledRowDescriptions.forEach { (tag, description) ->
            row(tag).assertContentDescriptionEquals(description)
        }
    }

    @Test
    fun given_app_version_when_displayed_then_shows_received_version_instead_of_design_one() {
        setContent(appVersion = APP_VERSION)

        row(SettingsTestTags.VERSION)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.settings_version, APP_VERSION))
        composeRule.onNodeWithText(string(R.string.settings_version, DESIGN_VERSION)).assertDoesNotExist()
    }

    @Test
    fun given_settings_when_displayed_then_unavailable_rows_are_disabled_and_have_no_click_action() {
        setContent()

        disabledRowDescriptions.keys.forEach { tag ->
            row(tag).assertIsNotEnabled().assertHasNoClickAction()
        }
    }

    @Test
    fun given_unavailable_rows_when_clicked_then_sends_no_intent() {
        setContent()

        disabledRowDescriptions.keys.forEach { tag ->
            row(tag).performScrollTo().performClick()
        }
        composeRule.waitForIdle()

        assertTrue(intents.isEmpty())
    }

    @Test
    fun given_trash_row_when_displayed_then_only_describes_coming_soon_without_value() {
        setContent()

        row(SettingsTestTags.TRASH_ROW).assertContentDescriptionEquals(
            listOf(string(R.string.settings_trash), string(R.string.settings_coming_soon)).joinToString(),
        )
        composeRule.onNodeWithText(DESIGN_TRASH_COUNT, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(DESIGN_TRASH_DETAIL, substring = true).assertDoesNotExist()
    }

    @Test
    fun given_active_theme_when_displayed_then_theme_row_shows_its_name() {
        setContent()

        row(SettingsTestTags.THEME_ROW)
            .assert(hasText(string(activeMode.labelRes())))
            .assert(hasNoText(string(otherMode.labelRes())))
    }

    @Test
    fun given_theme_changes_when_screen_is_open_then_theme_row_shows_new_name() {
        var state by mutableStateOf(SettingsUiState(themeMode = activeMode))
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = APP_VERSION, onIntent = {})
            }
        }

        state = state.copy(themeMode = otherMode)

        row(SettingsTestTags.THEME_ROW).assert(hasText(string(otherMode.labelRes())))
    }

    @Test
    fun given_theme_row_when_clicked_then_sends_theme_row_clicked() {
        setContent()

        row(SettingsTestTags.THEME_ROW).assertIsEnabled().assertHasClickAction().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.ThemeRowClicked), intents)
    }

    @Test
    fun given_back_button_when_clicked_then_sends_back_clicked_and_is_labelled() {
        setContent()

        row(SettingsTestTags.BACK)
            .assertContentDescriptionEquals(string(R.string.settings_back))
            .performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.BackClicked), intents)
    }

    @Test
    fun given_closed_sheet_state_when_displayed_then_sheet_does_not_exist() {
        setContent()

        row(SettingsTestTags.THEME_SHEET).assertDoesNotExist()
    }

    @Test
    fun given_stateful_screen_when_theme_row_clicked_then_opens_sheet() {
        setStatefulContent(SettingsUiState(themeMode = activeMode))

        row(SettingsTestTags.THEME_ROW).performClick()

        row(SettingsTestTags.THEME_SHEET).assertIsDisplayed()
    }

    @Test
    fun given_visible_sheet_when_displayed_then_shows_both_options_as_radio_buttons() {
        setContent(openSheetState())

        row(SettingsTestTags.THEME_SHEET).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.settings_theme)) and inThemeSheet).assertIsDisplayed()
        ThemeMode.entries.forEach { mode ->
            themeOption(mode)
                .assertIsDisplayed()
                .assertTextEquals(string(mode.labelRes()))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assertHasClickAction()
        }
    }

    @Test
    fun given_visible_sheet_when_displayed_then_only_active_theme_is_selected() {
        setContent(openSheetState())

        themeOption(activeMode).assertIsSelected()
        themeOption(otherMode).assertIsNotSelected()
    }

    @Test
    fun given_visible_sheet_when_other_option_clicked_then_sends_selected_then_dismissed_and_closes() {
        setStatefulContent(openSheetState())

        themeOption(otherMode).performClick()
        awaitSheetClosed()

        assertEquals(
            listOf(SettingsIntent.ThemeSelected(otherMode), SettingsIntent.ThemeSheetDismissed),
            intents,
        )
        row(SettingsTestTags.THEME_SHEET).assertDoesNotExist()
        row(SettingsTestTags.THEME_ROW).assert(hasText(string(otherMode.labelRes())))
    }

    @Test
    fun given_visible_sheet_when_active_option_clicked_then_sends_selected_then_dismissed_and_closes() {
        setStatefulContent(openSheetState())

        themeOption(activeMode).performClick()
        awaitSheetClosed()

        assertEquals(
            listOf(SettingsIntent.ThemeSelected(activeMode), SettingsIntent.ThemeSheetDismissed),
            intents,
        )
        row(SettingsTestTags.THEME_SHEET).assertDoesNotExist()
        row(SettingsTestTags.THEME_ROW).assert(hasText(string(activeMode.labelRes())))
    }

    @Test
    fun given_visible_sheet_when_swiped_away_then_only_sends_dismissed_and_keeps_theme() {
        setStatefulContent(openSheetState())

        row(SettingsTestTags.THEME_SHEET).performTouchInput { swipeDown() }
        awaitSheetClosed()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.ThemeSheetDismissed), intents)
        row(SettingsTestTags.THEME_SHEET).assertDoesNotExist()
        row(SettingsTestTags.THEME_ROW).assert(hasText(string(activeMode.labelRes())))
    }

    @Test
    fun given_interactive_elements_when_displayed_then_touch_targets_are_at_least_44_dp() {
        setContent(openSheetState())

        val nodes = listOf(row(SettingsTestTags.BACK), row(SettingsTestTags.THEME_ROW)) +
            ThemeMode.entries.map { themeOption(it) }
        nodes.forEach { node ->
            node.assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    fun given_resources_when_read_then_visible_texts_match_the_spec() {
        val expected = mapOf(
            R.string.settings_back to "Volver",
            R.string.settings_privacy_title to "Tus ideas, solo tuyas",
            R.string.settings_privacy_detail to "Tus notas se guardan en este dispositivo.",
            R.string.settings_section_appearance to "Apariencia",
            R.string.settings_section_notes to "Tus notas",
            R.string.settings_section_about to "Acerca de",
            R.string.settings_theme to "Tema",
            R.string.settings_theme_light to "Claro",
            R.string.settings_theme_dark to "Oscuro",
            R.string.settings_default_color to "Color predeterminado",
            R.string.settings_default_color_value to "Cielo",
            R.string.settings_notes_view to "Vista de notas",
            R.string.settings_notes_view_value to "Cuadrícula",
            R.string.settings_auto_save to "Guardado automático",
            R.string.settings_auto_save_on to "Activado",
            R.string.settings_export to "Exportar notas",
            R.string.settings_trash to "Papelera",
            R.string.settings_coming_soon to "Próximamente",
            R.string.settings_help to "Ayuda y comentarios",
            R.string.settings_theme_save_error to "No se pudo guardar el tema",
        )

        expected.forEach { (id, text) -> assertEquals(text, string(id)) }
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_theme_when_displayed_then_screen_uses_its_background_and_surface() {
        setContent()

        val screen = composeRule.onRoot().captureToImage()

        assertTrue(screen.containsColor(if (darkTheme) ThemeTestColors.DarkBackground else ThemeTestColors.LightBackground))
        assertTrue(screen.containsColor(if (darkTheme) ThemeTestColors.DarkSurface else ThemeTestColors.LightSurface))
    }

    @Test
    fun given_theme_when_displayed_then_theme_value_uses_its_color() {
        setContent()

        val value = composeRule.onNode(
            hasText(string(activeMode.labelRes())) and hasAnyAncestor(hasTestTag(SettingsTestTags.THEME_ROW)),
            useUnmergedTree = true,
        )

        assertColorEquals(
            "Valor de la fila Tema",
            if (darkTheme) ThemeTestColors.DarkSecondaryText else ThemeTestColors.LightAccent,
            value.textColor(),
        )
    }

    private val disabledRowDescriptions
        get() = mapOf(
            SettingsTestTags.DEFAULT_COLOR_ROW to description(
                R.string.settings_default_color,
                R.string.settings_default_color_detail,
                R.string.settings_default_color_value,
            ),
            SettingsTestTags.NOTES_VIEW_ROW to description(
                R.string.settings_notes_view,
                R.string.settings_notes_view_detail,
                R.string.settings_notes_view_value,
            ),
            SettingsTestTags.AUTO_SAVE_ROW to description(
                R.string.settings_auto_save,
                R.string.settings_auto_save_detail,
                R.string.settings_auto_save_on,
            ),
            SettingsTestTags.EXPORT_ROW to description(R.string.settings_export, R.string.settings_export_detail),
            SettingsTestTags.TRASH_ROW to description(R.string.settings_trash, R.string.settings_coming_soon),
            SettingsTestTags.HELP_ROW to description(R.string.settings_help, R.string.settings_help_detail),
        )

    private fun description(vararg ids: Int): String = ids.joinToString { string(it) }

    private fun openSheetState() = SettingsUiState(themeMode = activeMode, isThemeSheetVisible = true)

    private fun awaitSheetClosed() {
        composeRule.waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) {
            SettingsIntent.ThemeSheetDismissed in intents
        }
        composeRule.waitForIdle()
    }

    private fun row(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun themeOption(mode: ThemeMode) = row(SettingsTestTags.THEME_OPTION + mode.name)

    private fun sectionHeader(id: Int) = composeRule.onNodeWithText(string(id).uppercase())

    private val inThemeSheet = hasAnyAncestor(hasTestTag(SettingsTestTags.THEME_SHEET))

    private fun hasNoText(text: String) = !hasText(text)

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "darkTheme={0}")
        fun themes(): List<Boolean> = listOf(true, false)

        private const val APP_VERSION = "9.8.7-test"
        private const val DESIGN_VERSION = "1.0.0"
        private const val DESIGN_TRASH_COUNT = "2 notas"
        private const val DESIGN_TRASH_DETAIL = "30 días"
        private const val SHEET_TIMEOUT_MS = 5_000L
        private const val CAPTURE_MIN_SDK = 26
        private val MIN_TOUCH_TARGET = 44.dp
    }
}
