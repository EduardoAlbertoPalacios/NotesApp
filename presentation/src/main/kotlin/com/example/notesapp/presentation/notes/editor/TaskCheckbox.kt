package com.example.notesapp.presentation.notes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesTheme

/**
 * Casilla de una tarea del editor. La zona táctil mide 44 dp, como las demás acciones, alrededor de la casilla de 20 dp;
 * el lector de pantalla la anuncia con el texto de la tarea ([label]) y su estado.
 */
@Composable
internal fun TaskCheckbox(
    isDone: Boolean,
    label: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = NotesTheme.sizes
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(NotesTheme.radii.taskCheckbox)
    val stateText = stringResource(if (isDone) R.string.note_editor_task_done else R.string.note_editor_task_pending)
    Box(
        modifier = modifier
            .size(sizes.iconAction)
            // Un solo nodo accesible con etiqueta, estado y acción; si no, la etiqueta queda en un nodo aparte.
            .clearAndSetSemantics {
                role = Role.Checkbox
                contentDescription = label
                stateDescription = stateText
                toggleableState = ToggleableState(isDone)
                onClick {
                    onToggle()
                    true
                }
            }
            .toggleable(value = isDone, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(sizes.taskCheckbox)
                .then(
                    if (isDone) {
                        Modifier.background(colors.primary, shape)
                    } else {
                        Modifier.border(sizes.taskCheckboxBorder, colors.onSurfaceVariant, shape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.taskCheckIcon),
                    tint = colors.onPrimary,
                )
            }
        }
    }
}
