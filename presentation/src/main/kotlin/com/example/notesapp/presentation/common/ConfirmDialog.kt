package com.example.notesapp.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.notesapp.presentation.R
import com.example.notesapp.presentation.theme.NotesAppTheme
import com.example.notesapp.presentation.theme.NotesTheme

/**
 * Diálogo de confirmación con el estilo del antiguo diálogo de eliminar nota del editor: título, mensaje y dos
 * botones de texto con el color de acento. Los textos ya vienen resueltos para admitir plurales y formatos.
 */
@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTestTag: String? = null,
    dismissTestTag: String? = null,
) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = NotesTheme.accent.content)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.optionalTestTag(confirmTestTag), colors = buttonColors) {
                Text(text = confirmLabel)
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.optionalTestTag(dismissTestTag), colors = buttonColors) {
                Text(text = dismissLabel)
            }
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

private fun Modifier.optionalTestTag(tag: String?): Modifier = if (tag == null) this else testTag(tag)

@Preview
@Composable
private fun ConfirmDialogDarkPreview() {
    NotesAppTheme {
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_title),
            message = stringResource(R.string.trash_delete_message),
            confirmLabel = stringResource(R.string.trash_delete_confirm),
            dismissLabel = stringResource(R.string.trash_cancel),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmDialogLightPreview() {
    NotesAppTheme(darkTheme = false) {
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_title),
            message = stringResource(R.string.trash_delete_message),
            confirmLabel = stringResource(R.string.trash_delete_confirm),
            dismissLabel = stringResource(R.string.trash_cancel),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
