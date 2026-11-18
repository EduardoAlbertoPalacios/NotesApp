package com.example.notesapp.presentation.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.testTag
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.DisabledAlpha
import com.example.notesapp.presentation.theme.NotesTheme

// Componentes de Ajustes compartidos con sus subpantallas (barra superior, grupos y filas).

/** Barra superior de Ajustes y sus subpantallas: botón de volver y título. Los textos ya vienen resueltos. */
@Composable
internal fun SettingsTopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    backTestTag: String,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier.size(NotesTheme.sizes.iconAction).testTag(backTestTag),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = backLabel,
                modifier = Modifier.size(NotesTheme.sizes.settingsBackIcon),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = title,
            style = NotesTheme.typography.settingsTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
internal fun SettingsGroup(
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
internal fun GroupDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = NotesTheme.sizes.border,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Fila atenuada y sin acción. [valueRes] es lo que se muestra a la derecha.
 */
@Composable
internal fun DisabledRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes detailRes: Int,
    testTag: String,
    modifier: Modifier = Modifier,
    @StringRes valueRes: Int? = null,
) {
    val value = valueRes?.let { stringResource(it) }
    val description = listOfNotNull(stringResource(titleRes), stringResource(detailRes), value).joinToString()
    SettingsRow(
        iconRes = iconRes,
        titleRes = titleRes,
        detail = stringResource(detailRes),
        modifier = modifier
            .alpha(DisabledAlpha)
            .clearAndSetSemantics {
                this.testTag = testTag
                contentDescription = description
                disabled()
            },
    ) {
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

@Composable
internal fun SettingsRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    detail: String,
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
                text = detail,
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
internal fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        modifier = modifier.size(NotesTheme.sizes.settingsChevron),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
