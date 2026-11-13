package com.example.notesapp.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.ThemeTestColors
import com.example.notesapp.presentation.assertColorEquals
import com.example.notesapp.presentation.containsColor
import com.example.notesapp.presentation.dominantColor
import com.example.notesapp.presentation.notes.common.ColorPickerTestTags
import com.example.notesapp.presentation.notes.common.labelRes
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
                            SettingsIntent.DefaultColorRowClicked -> state.copy(defaultColorPicker = state.defaultNoteColor)
                            is SettingsIntent.DefaultColorOptionSelected -> state.copy(defaultColorPicker = intent.color)
                            SettingsIntent.DefaultColorApplied -> state.copy(
                                defaultNoteColor = state.defaultColorPicker ?: state.defaultNoteColor,
                                defaultColorPicker = null,
                            )
                            SettingsIntent.DefaultColorPickerDismissed -> state.copy(defaultColorPicker = null)
                            SettingsIntent.NotesViewRowClicked -> state.copy(isNotesViewSheetVisible = true)
                            is SettingsIntent.NotesViewSelected -> state.copy(notesView = intent.view)
                            SettingsIntent.NotesViewSheetDismissed -> state.copy(isNotesViewSheetVisible = false)
                            SettingsIntent.AutoSaveToggled -> state.copy(isAutoSaveEnabled = !state.isAutoSaveEnabled)
                            SettingsIntent.ExportRowClicked -> state
                            is SettingsIntent.ExportFinished -> state
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
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.settings_default_color)))
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.settings_notes_view)))
        row(SettingsTestTags.AUTO_SAVE_ROW).assert(hasText(string(R.string.settings_auto_save)))
        row(SettingsTestTags.EXPORT_ROW).assert(hasText(string(R.string.settings_export)))
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
            R.string.settings_default_color_detail to "Para tus nuevas notas",
            R.string.settings_default_color_preview_title to "Nota nueva",
            R.string.settings_default_color_save_error to "No se pudo guardar el color predeterminado",
            R.string.note_color_none to "Sin color",
            R.string.note_color_aqua to "Cielo",
            R.string.note_color_rose to "Rosa",
            R.string.note_color_vanilla to "Vainilla",
            R.string.note_color_lavender to "Lavanda",
            R.string.note_color_mint to "Menta",
            R.string.note_color_peach to "Melocotón",
            R.string.note_color_sand to "Arena",
            R.string.note_color_apply to "Aplicar color",
            R.string.settings_notes_view to "Vista de notas",
            R.string.notes_view_grid to "Cuadrícula",
            R.string.notes_view_list to "Lista",
            R.string.notes_view_save_error to "No se pudo guardar la vista",
            R.string.settings_auto_save to "Guardado automático",
            R.string.settings_auto_save_detail to "Guarda mientras escribes",
            R.string.settings_auto_save_error to "No se pudo guardar el guardado automático",
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

    @Test
    fun given_default_color_when_displayed_then_row_is_enabled_with_description_and_color_name() {
        setContent(SettingsUiState(themeMode = activeMode, defaultNoteColor = NoteColor.MINT))

        row(SettingsTestTags.DEFAULT_COLOR_ROW)
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(hasText(string(R.string.settings_default_color)))
            .assert(hasText(string(R.string.settings_default_color_detail)))
            .assert(hasText(string(R.string.note_color_mint)))
    }

    @Test
    fun given_each_default_color_when_displayed_then_row_shows_its_name() {
        var state by mutableStateOf(SettingsUiState(themeMode = activeMode))
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = APP_VERSION, onIntent = {})
            }
        }

        colorLabels.forEach { (color, labelRes) ->
            state = state.copy(defaultNoteColor = color)

            row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(labelRes)))
        }
    }

    @Test
    fun given_theme_when_displayed_then_default_color_value_is_gray_and_regular() {
        setContent()

        val value = composeRule.onNode(
            hasText(string(R.string.note_color_aqua)) and hasAnyAncestor(hasTestTag(SettingsTestTags.DEFAULT_COLOR_ROW)),
            useUnmergedTree = true,
        )

        assertColorEquals(
            "Valor de la fila Color predeterminado",
            if (darkTheme) ThemeTestColors.DarkSecondaryText else ThemeTestColors.LightSecondaryText,
            value.textColor(),
        )
        assertEquals(FontWeight.Normal, value.textFontWeight())
    }

    @Test
    fun given_default_color_row_when_clicked_then_sends_row_clicked() {
        setContent()

        row(SettingsTestTags.DEFAULT_COLOR_ROW).performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.DefaultColorRowClicked), intents)
    }

    @Test
    fun given_closed_picker_state_when_displayed_then_color_sheet_does_not_exist() {
        setContent()

        row(ColorPickerTestTags.SHEET).assertDoesNotExist()
    }

    @Test
    fun given_stateful_screen_when_default_color_row_clicked_then_opens_sheet_with_current_color_selected() {
        setStatefulContent(SettingsUiState(themeMode = activeMode, defaultNoteColor = NoteColor.SAND))

        row(SettingsTestTags.DEFAULT_COLOR_ROW).performClick()

        row(ColorPickerTestTags.SHEET).assertIsDisplayed()
        colorOption(NoteColor.SAND).assertIsSelected()
    }

    @Test
    fun given_open_picker_when_displayed_then_sheet_shows_title_description_and_apply() {
        setContent(openPickerState())

        row(ColorPickerTestTags.SHEET).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.settings_default_color)) and inColorSheet).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.settings_default_color_detail)) and inColorSheet).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_editor_toolbar_color)) and inColorSheet).assertDoesNotExist()
        composeRule.onNode(hasText(string(R.string.note_color_sheet_description)) and inColorSheet).assertDoesNotExist()
        row(ColorPickerTestTags.APPLY).assertTextEquals(string(R.string.note_color_apply))
    }

    @Test
    fun given_open_picker_when_displayed_then_eight_options_follow_spec_order_in_two_rows_of_four() {
        setContent(openPickerState())

        composeRule.onAllNodes(hasRole(Role.RadioButton) and inColorSheet).assertCountEquals(colorLabels.size)
        val bounds = colorLabels.keys.map { colorOption(it).getUnclippedBoundsInRoot() }

        val (firstRow, secondRow) = bounds.chunked(4)
        listOf(firstRow, secondRow).forEach { boundsRow ->
            assertTrue(boundsRow.all { it.top == boundsRow.first().top })
            assertTrue(boundsRow.zipWithNext().all { (left, right) -> left.right <= right.left })
        }
        assertTrue(firstRow.first().bottom <= secondRow.first().top)
    }

    @Test
    fun given_open_picker_when_displayed_then_options_are_labelled_radio_buttons() {
        setContent(openPickerState())

        colorLabels.forEach { (color, labelRes) ->
            colorOption(color)
                .assertIsDisplayed()
                .assertTextEquals(string(labelRes))
                .assert(hasRole(Role.RadioButton))
                .assertHasClickAction()
        }
    }

    @Test
    fun given_open_picker_when_displayed_then_only_picked_option_is_selected() {
        setContent(openPickerState(picked = NoteColor.LAVENDER))

        colorOption(NoteColor.LAVENDER).assertIsSelected()
        colorLabels.keys.filter { it != NoteColor.LAVENDER }.forEach { color ->
            colorOption(color).assertIsNotSelected()
        }
    }

    @Test
    fun given_no_color_picked_when_displayed_then_no_color_option_is_selected() {
        setContent(openPickerState(picked = NoteColor.NONE))

        colorOption(NoteColor.NONE).assertIsSelected()
        colorOption(NoteColor.AQUA).assertIsNotSelected()
    }

    @Test
    fun given_open_picker_when_displayed_then_picked_label_is_highlighted() {
        setContent(openPickerState(picked = NoteColor.LAVENDER))

        assertColorEquals(
            "Elegida",
            if (darkTheme) ThemeTestColors.NoteContainers.getValue(NoteColor.AQUA) else ThemeTestColors.LightAccent,
            colorOptionLabel(NoteColor.LAVENDER).textColor(),
        )
        assertColorEquals(
            "Otra",
            if (darkTheme) ThemeTestColors.DarkSecondaryText else ThemeTestColors.LightSecondaryText,
            colorOptionLabel(NoteColor.ROSE).textColor(),
        )
    }

    @Test
    fun given_open_picker_when_options_clicked_then_sends_each_selection() {
        setContent(openPickerState())

        colorOption(NoteColor.ROSE).performClick()
        colorOption(NoteColor.NONE).performClick()

        assertEquals(
            listOf(
                SettingsIntent.DefaultColorOptionSelected(NoteColor.ROSE),
                SettingsIntent.DefaultColorOptionSelected(NoteColor.NONE),
            ),
            intents,
        )
    }

    @Test
    fun given_stateful_picker_when_option_clicked_then_selection_and_caption_follow_it() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))

        colorOption(NoteColor.PEACH).performClick()

        colorOption(NoteColor.PEACH).assertIsSelected()
        colorOption(NoteColor.AQUA).assertIsNotSelected()
        previewCaption(R.string.note_color_peach).assertIsDisplayed()
        previewCaption(R.string.note_color_aqua).assertDoesNotExist()
    }

    @Test
    fun given_open_picker_when_displayed_then_preview_shows_new_note_title_and_color_caption() {
        setContent(openPickerState(picked = NoteColor.NONE))

        composeRule.onNode(
            hasText(string(R.string.settings_default_color_preview_title)) and inColorPreview,
        ).assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.note_color_preview_untitled)) and inColorPreview).assertDoesNotExist()
        previewCaption(R.string.note_color_none).assertIsDisplayed()
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_each_picked_color_when_displayed_then_preview_uses_it() {
        var state by mutableStateOf(openPickerState())
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = APP_VERSION, onIntent = {})
            }
        }

        ThemeTestColors.NoteContainers.forEach { (color, expected) ->
            state = state.copy(defaultColorPicker = color)
            composeRule.waitForIdle()

            assertColorEquals("Vista previa $color", expected, row(ColorPickerTestTags.PREVIEW).captureToImage().dominantColor())
        }
    }

    @Test
    fun given_open_picker_when_apply_clicked_then_sends_applied_closes_and_row_shows_new_color() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))
        colorOption(NoteColor.MINT).performClick()

        row(ColorPickerTestTags.APPLY).performClick()
        awaitIntent(SettingsIntent.DefaultColorApplied)

        assertEquals(
            listOf(SettingsIntent.DefaultColorOptionSelected(NoteColor.MINT), SettingsIntent.DefaultColorApplied),
            intents,
        )
        row(ColorPickerTestTags.SHEET).assertDoesNotExist()
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_mint)))
    }

    @Test
    fun given_same_color_when_apply_clicked_then_closes_and_row_does_not_change() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))

        row(ColorPickerTestTags.APPLY).performClick()
        awaitIntent(SettingsIntent.DefaultColorApplied)

        row(ColorPickerTestTags.SHEET).assertDoesNotExist()
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_aqua)))
    }

    @Test
    fun given_changed_option_when_close_clicked_then_sends_dismissed_closes_and_keeps_color() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))
        colorOption(NoteColor.MINT).performClick()

        row(ColorPickerTestTags.CLOSE).performClick()
        awaitIntent(SettingsIntent.DefaultColorPickerDismissed)

        assertEquals(
            listOf(SettingsIntent.DefaultColorOptionSelected(NoteColor.MINT), SettingsIntent.DefaultColorPickerDismissed),
            intents,
        )
        row(ColorPickerTestTags.SHEET).assertDoesNotExist()
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_aqua)))
    }

    @Test
    fun given_changed_option_when_sheet_swiped_away_then_sends_dismissed_and_keeps_color() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))
        colorOption(NoteColor.MINT).performClick()

        row(ColorPickerTestTags.SHEET).performTouchInput { swipeDown() }
        awaitIntent(SettingsIntent.DefaultColorPickerDismissed)

        row(ColorPickerTestTags.SHEET).assertDoesNotExist()
        row(SettingsTestTags.DEFAULT_COLOR_ROW).assert(hasText(string(R.string.note_color_aqua)))
    }

    @Test
    fun given_picker_closed_without_applying_when_reopened_then_marks_current_color() {
        setStatefulContent(openPickerState(picked = NoteColor.AQUA))
        colorOption(NoteColor.MINT).performClick()
        row(ColorPickerTestTags.CLOSE).performClick()
        awaitIntent(SettingsIntent.DefaultColorPickerDismissed)

        row(SettingsTestTags.DEFAULT_COLOR_ROW).performClick()

        colorOption(NoteColor.AQUA).assertIsSelected()
        colorOption(NoteColor.MINT).assertIsNotSelected()
    }

    @Test
    fun given_open_picker_when_displayed_then_close_button_has_accessible_label() {
        setContent(openPickerState())

        row(ColorPickerTestTags.CLOSE)
            .assertContentDescriptionEquals(string(R.string.note_color_close))
            .assertHasClickAction()
    }

    @Test
    fun given_default_color_elements_when_displayed_then_touch_targets_are_at_least_44_dp() {
        setContent(openPickerState())

        val nodes = listOf(row(SettingsTestTags.DEFAULT_COLOR_ROW), row(ColorPickerTestTags.CLOSE)) +
            colorLabels.keys.map { colorOption(it) }
        nodes.forEach { node ->
            node.assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    fun given_notes_view_row_when_displayed_then_is_enabled_with_title_detail_and_click_action() {
        setContent()

        row(SettingsTestTags.NOTES_VIEW_ROW)
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(hasText(string(R.string.settings_notes_view)))
            .assert(hasText(string(R.string.settings_notes_view_detail)))
    }

    @Test
    fun given_each_notes_view_when_displayed_then_row_shows_its_name_and_not_the_other() {
        var state by mutableStateOf(SettingsUiState(themeMode = activeMode))
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = APP_VERSION, onIntent = {})
            }
        }

        NotesView.entries.forEach { view ->
            state = state.copy(notesView = view)

            row(SettingsTestTags.NOTES_VIEW_ROW)
                .assert(hasText(string(view.labelRes())))
                .assert(hasNoText(string(otherView(view).labelRes())))
        }
    }

    @Test
    fun given_theme_when_displayed_then_notes_view_value_is_gray_and_regular() {
        setContent(SettingsUiState(themeMode = activeMode, notesView = NotesView.LIST))

        val value = composeRule.onNode(
            hasText(string(R.string.notes_view_list)) and hasAnyAncestor(hasTestTag(SettingsTestTags.NOTES_VIEW_ROW)),
            useUnmergedTree = true,
        )

        assertColorEquals(
            "Valor de la fila Vista de notas",
            if (darkTheme) ThemeTestColors.DarkSecondaryText else ThemeTestColors.LightSecondaryText,
            value.textColor(),
        )
        assertEquals(FontWeight.Normal, value.textFontWeight())
    }

    @Test
    fun given_notes_view_row_when_clicked_then_sends_row_clicked() {
        setContent()

        row(SettingsTestTags.NOTES_VIEW_ROW).performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.NotesViewRowClicked), intents)
    }

    @Test
    fun given_closed_notes_view_sheet_when_displayed_then_sheet_does_not_exist() {
        setContent()

        row(SettingsTestTags.NOTES_VIEW_SHEET).assertDoesNotExist()
    }

    @Test
    fun given_stateful_screen_when_notes_view_row_clicked_then_opens_sheet_with_current_view_selected() {
        setStatefulContent(SettingsUiState(themeMode = activeMode, notesView = NotesView.LIST))

        row(SettingsTestTags.NOTES_VIEW_ROW).performClick()

        row(SettingsTestTags.NOTES_VIEW_SHEET).assertIsDisplayed()
        notesViewOption(NotesView.LIST).assertIsSelected()
        notesViewOption(NotesView.GRID).assertIsNotSelected()
    }

    @Test
    fun given_open_notes_view_sheet_when_displayed_then_shows_title_and_options_in_order_as_radio_buttons() {
        setContent(openNotesViewSheetState())

        composeRule.onNode(hasText(string(R.string.settings_notes_view)) and inNotesViewSheet).assertIsDisplayed()
        composeRule.onAllNodes(hasRole(Role.RadioButton) and inNotesViewSheet).assertCountEquals(NotesView.entries.size)
        notesViewOptionLabels.forEach { (view, labelRes) ->
            notesViewOption(view)
                .assertIsDisplayed()
                .assertTextEquals(string(labelRes))
                .assert(hasRole(Role.RadioButton))
                .assertHasClickAction()
        }
        val tops = notesViewOptionLabels.keys.map { notesViewOption(it).getUnclippedBoundsInRoot().top.value }
        assertEquals(tops.sorted(), tops)
        assertTrue(tops.first() < tops.last())
    }

    @Test
    fun given_each_notes_view_when_sheet_open_then_only_that_option_is_selected() {
        var state by mutableStateOf(openNotesViewSheetState())
        composeRule.setContent {
            NotesAppTheme(darkTheme = darkTheme) {
                SettingsScreen(state = state, appVersion = APP_VERSION, onIntent = {})
            }
        }

        NotesView.entries.forEach { view ->
            state = state.copy(notesView = view)

            notesViewOption(view).assertIsSelected()
            notesViewOption(otherView(view)).assertIsNotSelected()
        }
    }

    @Test
    fun given_open_notes_view_sheet_when_other_option_clicked_then_sends_selected_then_dismissed_and_closes() {
        setStatefulContent(openNotesViewSheetState(NotesView.GRID))

        notesViewOption(NotesView.LIST).performClick()
        awaitIntent(SettingsIntent.NotesViewSheetDismissed)

        assertEquals(
            listOf(SettingsIntent.NotesViewSelected(NotesView.LIST), SettingsIntent.NotesViewSheetDismissed),
            intents,
        )
        row(SettingsTestTags.NOTES_VIEW_SHEET).assertDoesNotExist()
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.notes_view_list)))
    }

    @Test
    fun given_open_notes_view_sheet_when_active_option_clicked_then_closes_and_row_does_not_change() {
        setStatefulContent(openNotesViewSheetState(NotesView.LIST))

        notesViewOption(NotesView.LIST).performClick()
        awaitIntent(SettingsIntent.NotesViewSheetDismissed)

        assertEquals(
            listOf(SettingsIntent.NotesViewSelected(NotesView.LIST), SettingsIntent.NotesViewSheetDismissed),
            intents,
        )
        row(SettingsTestTags.NOTES_VIEW_SHEET).assertDoesNotExist()
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.notes_view_list)))
    }

    @Test
    fun given_open_notes_view_sheet_when_swiped_away_then_only_sends_dismissed_and_keeps_view() {
        setStatefulContent(openNotesViewSheetState(NotesView.LIST))

        row(SettingsTestTags.NOTES_VIEW_SHEET).performTouchInput { swipeDown() }
        awaitIntent(SettingsIntent.NotesViewSheetDismissed)

        assertEquals(listOf<SettingsIntent>(SettingsIntent.NotesViewSheetDismissed), intents)
        row(SettingsTestTags.NOTES_VIEW_SHEET).assertDoesNotExist()
        row(SettingsTestTags.NOTES_VIEW_ROW).assert(hasText(string(R.string.notes_view_list)))
    }

    @Test
    fun given_notes_view_elements_when_displayed_then_touch_targets_are_at_least_44_dp() {
        setContent(openNotesViewSheetState())

        val nodes = listOf(row(SettingsTestTags.NOTES_VIEW_ROW)) + NotesView.entries.map { notesViewOption(it) }
        nodes.forEach { node ->
            node.assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    fun given_auto_save_row_when_displayed_then_is_enabled_with_title_detail_and_switch_role() {
        setContent()

        row(SettingsTestTags.AUTO_SAVE_ROW)
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(hasText(string(R.string.settings_auto_save)))
            .assert(hasText(string(R.string.settings_auto_save_detail)))
            .assert(hasRole(Role.Switch))
    }

    @Test
    fun given_auto_save_enabled_when_displayed_then_switch_announces_on() {
        setContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = true))

        row(SettingsTestTags.AUTO_SAVE_ROW)
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun given_auto_save_disabled_when_displayed_then_switch_announces_off() {
        setContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = false))

        row(SettingsTestTags.AUTO_SAVE_ROW)
            .assertIsEnabled()
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
    }

    @Test
    fun given_auto_save_row_when_clicked_then_sends_auto_save_toggled() {
        setContent()

        row(SettingsTestTags.AUTO_SAVE_ROW).performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.AutoSaveToggled), intents)
    }

    @Test
    fun given_auto_save_disabled_when_row_clicked_then_sends_auto_save_toggled() {
        setContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = false))

        row(SettingsTestTags.AUTO_SAVE_ROW).performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.AutoSaveToggled), intents)
    }

    @Test
    fun given_stateful_screen_when_auto_save_row_clicked_twice_then_switch_turns_off_and_on() {
        setStatefulContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = true))

        row(SettingsTestTags.AUTO_SAVE_ROW).performScrollTo().performClick()
        row(SettingsTestTags.AUTO_SAVE_ROW).assertIsOff()
        row(SettingsTestTags.AUTO_SAVE_ROW).performClick()

        row(SettingsTestTags.AUTO_SAVE_ROW).assertIsOn()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.AutoSaveToggled, SettingsIntent.AutoSaveToggled), intents)
    }

    @Test
    fun given_auto_save_row_when_displayed_then_does_not_show_a_chevron_value_or_disabled_state() {
        setContent()

        row(SettingsTestTags.AUTO_SAVE_ROW)
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun given_export_row_when_displayed_then_is_enabled_button_with_title_and_detail() {
        setContent()

        row(SettingsTestTags.EXPORT_ROW)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(hasRole(Role.Button))
            .assert(hasText(string(R.string.settings_export)))
            .assert(hasText(string(R.string.settings_export_detail)))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test
    fun given_export_row_when_clicked_then_sends_export_row_clicked() {
        setContent()

        row(SettingsTestTags.EXPORT_ROW).performScrollTo().performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.ExportRowClicked), intents)
    }

    @Test
    fun given_export_row_when_displayed_then_touch_target_is_at_least_44_dp() {
        setContent()

        row(SettingsTestTags.EXPORT_ROW).assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun given_auto_save_row_when_displayed_then_touch_target_is_at_least_44_dp() {
        setContent()

        row(SettingsTestTags.AUTO_SAVE_ROW).assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_auto_save_enabled_when_displayed_then_switch_thumb_is_on_the_right_over_primary_track() {
        setContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = true))

        val pixels = switchPixels()

        assertColorEquals("Círculo encendido", ThemeTestColors.NoteText, pixels.thumbRight)
        assertColorEquals("Pista a la izquierda del círculo", ThemeTestColors.NoteContainers.getValue(NoteColor.AQUA), pixels.thumbLeft)
    }

    @Test
    @SdkSuppress(minSdkVersion = CAPTURE_MIN_SDK)
    fun given_auto_save_disabled_when_displayed_then_switch_is_visible_with_thumb_on_the_left_and_off_colors() {
        setContent(SettingsUiState(themeMode = activeMode, isAutoSaveEnabled = false))

        val pixels = switchPixels()

        assertColorEquals(
            "Círculo apagado",
            if (darkTheme) ThemeTestColors.DarkSecondaryText else ThemeTestColors.LightSecondaryText,
            pixels.thumbLeft,
        )
        assertColorEquals("Pista a la derecha del círculo", if (darkTheme) OFF_TRACK_DARK else OFF_TRACK_LIGHT, pixels.thumbRight)
    }

    /** Lee el centro de las dos posiciones del círculo; el interruptor va al final de la fila, tras su margen. */
    private fun switchPixels(): SwitchPixels {
        val node = row(SettingsTestTags.AUTO_SAVE_ROW).performScrollTo()
        val image = node.captureToImage().toPixelMap()
        val density = composeRule.density.density
        fun px(dp: Float) = (dp * density).toInt()
        val switchLeft = image.width - px(ROW_HORIZONTAL_PADDING_DP) - px(SWITCH_WIDTH_DP)
        val centerY = image.height / 2
        return SwitchPixels(
            thumbLeft = image[switchLeft + px(SWITCH_LEFT_THUMB_CENTER_DP), centerY],
            thumbRight = image[switchLeft + px(SWITCH_RIGHT_THUMB_CENTER_DP), centerY],
        )
    }

    private data class SwitchPixels(val thumbLeft: Color, val thumbRight: Color)

    private fun openNotesViewSheetState(view: NotesView = NotesView.GRID) =
        SettingsUiState(themeMode = activeMode, notesView = view, isNotesViewSheetVisible = true)

    private fun notesViewOption(view: NotesView) = row(SettingsTestTags.NOTES_VIEW_OPTION + view.name)

    private fun otherView(view: NotesView) = NotesView.entries.first { it != view }

    private val inNotesViewSheet = hasAnyAncestor(hasTestTag(SettingsTestTags.NOTES_VIEW_SHEET))

    /** En el orden de la spec (RF-9), escrito aparte del enum para detectar un cambio de orden. */
    private val notesViewOptionLabels = linkedMapOf(
        NotesView.GRID to R.string.notes_view_grid,
        NotesView.LIST to R.string.notes_view_list,
    )

    private fun openPickerState(picked: NoteColor = NoteColor.AQUA) =
        SettingsUiState(themeMode = activeMode, defaultColorPicker = picked)

    private fun awaitIntent(intent: SettingsIntent) {
        composeRule.waitUntil(timeoutMillis = SHEET_TIMEOUT_MS) { intent in intents }
        composeRule.waitForIdle()
    }

    private fun colorOption(color: NoteColor) = row(ColorPickerTestTags.OPTION + color.name)

    private fun colorOptionLabel(color: NoteColor) = composeRule.onNode(
        hasText(string(colorLabels.getValue(color))) and hasAnyAncestor(hasTestTag(ColorPickerTestTags.OPTION + color.name)),
        useUnmergedTree = true,
    )

    private fun previewCaption(labelRes: Int) = composeRule.onNode(
        hasText(string(R.string.note_color_preview_caption, string(labelRes))) and inColorPreview,
    )

    private val inColorSheet = hasAnyAncestor(hasTestTag(ColorPickerTestTags.SHEET))

    private val inColorPreview = hasAnyAncestor(hasTestTag(ColorPickerTestTags.PREVIEW))

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun SemanticsNodeInteraction.textFontWeight(): FontWeight? {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single().layoutInput.style.fontWeight
    }

    /** En el orden de la spec (RF-5), escrito aparte del enum para detectar un cambio de orden. */
    private val colorLabels = linkedMapOf(
        NoteColor.NONE to R.string.note_color_none,
        NoteColor.AQUA to R.string.note_color_aqua,
        NoteColor.ROSE to R.string.note_color_rose,
        NoteColor.VANILLA to R.string.note_color_vanilla,
        NoteColor.LAVENDER to R.string.note_color_lavender,
        NoteColor.MINT to R.string.note_color_mint,
        NoteColor.PEACH to R.string.note_color_peach,
        NoteColor.SAND to R.string.note_color_sand,
    )

    private val disabledRowDescriptions
        get() = mapOf(
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

        /** Medidas del diseño (42 × 26 dp, círculo de 20 dp a 3 dp del borde, fila con 16 dp de margen). */
        private const val ROW_HORIZONTAL_PADDING_DP = 16f
        private const val SWITCH_WIDTH_DP = 42f
        private const val SWITCH_LEFT_THUMB_CENTER_DP = 13f
        private const val SWITCH_RIGHT_THUMB_CENTER_DP = 29f

        /** Pista apagada: el `outlineVariant` de cada tema. */
        private val OFF_TRACK_DARK = Color(0xFF2A2A2E)
        private val OFF_TRACK_LIGHT = Color(0xFFDCE2E5)
    }
}
