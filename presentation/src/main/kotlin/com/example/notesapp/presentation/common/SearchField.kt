package com.example.notesapp.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesTheme

/**
 * Buscador con fondo de píldora, icono de lupa y botón para limpiar la búsqueda. Los textos ya vienen resueltos.
 * Sin [enabled] el buscador se ve atenuado y no acepta texto.
 */
@Composable
internal fun SearchField(
    query: String,
    placeholder: String,
    clearLabel: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    fieldTestTag: String,
    clearTestTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = NotesTheme.spacing
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge
    val hintColor = if (enabled) colors.onSurfaceVariant else colors.outlineVariant
    val keyboardController = LocalSoftwareKeyboardController.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        // El marcador hace de etiqueta del campo, también cuando ya tiene texto.
        modifier = modifier
            .fillMaxWidth()
            .testTag(fieldTestTag)
            .semantics { contentDescription = placeholder },
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle.copy(color = colors.onBackground),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        cursorBrush = SolidColor(NotesTheme.accent.content),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NotesTheme.sizes.searchHeight)
                    .background(colors.surfaceContainer, RoundedCornerShape(NotesTheme.radii.pill))
                    .padding(horizontal = spacing.l),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = hintColor,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        // Ya lo anuncia la etiqueta del campo: se oculta para no leerlo dos veces.
                        Text(
                            text = placeholder,
                            modifier = Modifier.clearAndSetSemantics {},
                            style = textStyle,
                            color = hintColor,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(NotesTheme.sizes.imageAction)
                            .testTag(clearTestTag),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = clearLabel,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
