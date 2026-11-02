package com.example.notesapp.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.DisabledAlpha
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

internal object SettingsTestTags {
    const val BACK = "settings_back"
    const val THEME_ROW = "settings_theme_row"
    const val DEFAULT_COLOR_ROW = "settings_default_color_row"
    const val NOTES_VIEW_ROW = "settings_notes_view_row"
    const val AUTO_SAVE_ROW = "settings_auto_save_row"
    const val EXPORT_ROW = "settings_export_row"
    const val TRASH_ROW = "settings_trash_row"
    const val HELP_ROW = "settings_help_row"
    const val VERSION = "settings_version"
    const val THEME_SHEET = "settings_theme_sheet"
    const val THEME_OPTION = "settings_theme_option_"
}

/** Solo la fila Tema funciona; las demás se ven deshabilitadas hasta tener su propia spec. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            SettingsTopBar(onBack = { onIntent(SettingsIntent.BackClicked) })
            SettingsContent(state = state, appVersion = appVersion, onIntent = onIntent)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
    if (state.isThemeSheetVisible) {
        ThemeSheet(selected = state.themeMode, onIntent = onIntent)
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            modifier = Modifier.size(NotesTheme.sizes.iconAction).testTag(SettingsTestTags.BACK),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.settings_back),
                modifier = Modifier.size(NotesTheme.sizes.settingsBackIcon),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = NotesTheme.typography.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    appVersion: String,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(
                start = NotesTheme.spacing.screen,
                top = NotesTheme.spacing.ml,
                end = NotesTheme.spacing.screen,
                bottom = NotesTheme.spacing.screen,
            ),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xl),
    ) {
        PrivacyNotice()
        SettingsGroup(titleRes = R.string.settings_section_appearance) {
            ThemeRow(mode = state.themeMode, onClick = { onIntent(SettingsIntent.ThemeRowClicked) })
            GroupDivider()
            DisabledRow(
                iconRes = R.drawable.ic_palette,
                titleRes = R.string.settings_default_color,
                detailRes = R.string.settings_default_color_detail,
                testTag = SettingsTestTags.DEFAULT_COLOR_ROW,
                valueRes = R.string.settings_default_color_value,
            )
            GroupDivider()
            DisabledRow(
                iconRes = R.drawable.ic_grid,
                titleRes = R.string.settings_notes_view,
                detailRes = R.string.settings_notes_view_detail,
                testTag = SettingsTestTags.NOTES_VIEW_ROW,
                valueRes = R.string.settings_notes_view_value,
            )
        }
        SettingsGroup(titleRes = R.string.settings_section_notes) {
            DisabledRow(
                iconRes = R.drawable.ic_save,
                titleRes = R.string.settings_auto_save,
                detailRes = R.string.settings_auto_save_detail,
                testTag = SettingsTestTags.AUTO_SAVE_ROW,
                valueRes = R.string.settings_auto_save_on,
                showSwitch = true,
            )
            GroupDivider()
            DisabledRow(
                iconRes = R.drawable.ic_export,
                titleRes = R.string.settings_export,
                detailRes = R.string.settings_export_detail,
                testTag = SettingsTestTags.EXPORT_ROW,
            )
            GroupDivider()
            DisabledRow(
                iconRes = R.drawable.ic_trash,
                titleRes = R.string.settings_trash,
                detailRes = R.string.settings_coming_soon,
                testTag = SettingsTestTags.TRASH_ROW,
            )
        }
        SettingsGroup(titleRes = R.string.settings_section_about) {
            DisabledRow(
                iconRes = R.drawable.ic_help,
                titleRes = R.string.settings_help,
                detailRes = R.string.settings_help_detail,
                testTag = SettingsTestTags.HELP_ROW,
            )
        }
        Text(
            text = stringResource(R.string.settings_version, appVersion),
            modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.VERSION),
            style = NotesTheme.typography.settingsRowDetail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrivacyNotice(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer, RoundedCornerShape(NotesTheme.radii.card))
            .padding(NotesTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(NotesTheme.sizes.iconAction)
                .background(colors.primary, RoundedCornerShape(NotesTheme.radii.settingsNoticeIcon)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                modifier = Modifier.size(NotesTheme.sizes.settingsNoticeIcon),
                tint = colors.onPrimary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xsm)) {
            Text(
                text = stringResource(R.string.settings_privacy_title),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_privacy_detail),
                style = NotesTheme.typography.settingsNoticeDetail,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(titleRes).uppercase(),
            style = NotesTheme.typography.settingsSection,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NotesTheme.radii.card))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            content()
        }
    }
}

@Composable
private fun GroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = NotesTheme.sizes.border,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ThemeRow(mode: ThemeMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsRow(
        iconRes = R.drawable.ic_moon,
        titleRes = R.string.settings_theme,
        detailRes = R.string.settings_theme_detail,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(SettingsTestTags.THEME_ROW),
    ) {
        Text(
            text = stringResource(mode.labelRes()),
            style = NotesTheme.typography.settingsActiveValue,
            color = NotesTheme.accent.settingsValue,
        )
        Chevron()
    }
}

/**
 * Fila atenuada y sin acción. [valueRes] es lo que se muestra a la derecha o, con [showSwitch],
 * el estado que anuncia el lector de pantalla.
 */
@Composable
private fun DisabledRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes detailRes: Int,
    testTag: String,
    modifier: Modifier = Modifier,
    @StringRes valueRes: Int? = null,
    showSwitch: Boolean = false,
) {
    val value = valueRes?.let { stringResource(it) }
    val description = listOfNotNull(stringResource(titleRes), stringResource(detailRes), value).joinToString()
    SettingsRow(
        iconRes = iconRes,
        titleRes = titleRes,
        detailRes = detailRes,
        modifier = modifier
            .alpha(DisabledAlpha)
            .clearAndSetSemantics {
                this.testTag = testTag
                contentDescription = description
                disabled()
            },
    ) {
        if (showSwitch) {
            SwitchOn()
        } else {
            if (value != null) {
                Text(
                    text = value,
                    style = NotesTheme.typography.settingsValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Chevron()
        }
    }
}

@Composable
private fun SettingsRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes detailRes: Int,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NotesTheme.sizes.settingsRow)
            .padding(horizontal = NotesTheme.spacing.l, vertical = NotesTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.ml),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(NotesTheme.sizes.settingsRowIcon),
            tint = NotesTheme.accent.content,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(titleRes),
                style = NotesTheme.typography.settingsRowTitle,
                color = colors.onBackground,
            )
            Text(
                text = stringResource(detailRes),
                style = NotesTheme.typography.settingsRowDetail,
                color = colors.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NotesTheme.spacing.xsm),
            content = trailing,
        )
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        modifier = modifier.size(NotesTheme.sizes.settingsChevron),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Interruptor del diseño, solo visual: la fila Guardado automático aún no se puede cambiar. */
@Composable
private fun SwitchOn(modifier: Modifier = Modifier) {
    val sizes = NotesTheme.sizes
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(width = sizes.settingsSwitchWidth, height = sizes.settingsSwitchHeight)
            .background(colors.primary, CircleShape)
            .padding(horizontal = (sizes.settingsSwitchHeight - sizes.settingsSwitchThumb) / 2),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .size(sizes.settingsSwitchThumb)
                .background(colors.onPrimary, CircleShape),
        )
    }
}

@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

private val PreviewDarkState = SettingsUiState(themeMode = ThemeMode.DARK)
private val PreviewLightState = SettingsUiState(themeMode = ThemeMode.LIGHT)

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenDarkPreview() {
    NotesAppTheme {
        SettingsScreen(state = PreviewDarkState, appVersion = "1.0", onIntent = {})
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenLightPreview() {
    NotesAppTheme(darkTheme = false) {
        SettingsScreen(state = PreviewLightState, appVersion = "1.0", onIntent = {})
    }
}
