package com.example.notesapp.presentation.notes.trash

import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.trash.TrashRetention

/**
 * Nota de la papelera tal como se muestra. [title] vacío se muestra como «Sin título»; [daysLeft] es nulo con
 * el plazo «Nunca».
 */
data class TrashItemUi(
    val id: Long,
    val title: String,
    val excerpt: String,
    val color: NoteColor,
    val daysLeft: Int?,
)

/** Plazo más corto elegido que borraría [count] notas: espera confirmación. */
data class PendingRetention(val retention: TrashRetention, val count: Int)

data class TrashUiState(
    val isLoading: Boolean = true,
    /** No se pudieron leer las notas: se avisa en lugar de decir que la papelera está vacía. */
    val loadFailed: Boolean = false,
    /** De la que entró más recientemente a la más antigua. */
    val notes: List<TrashItemUi> = emptyList(),
    val retention: TrashRetention = TrashRetention.DEFAULT,
    /** Nota cuya hoja de acciones está abierta. */
    val actionsNoteId: Long? = null,
    /** Nota que espera confirmación para eliminarse definitivamente. */
    val pendingDeleteId: Long? = null,
    val isEmptyConfirmVisible: Boolean = false,
    val isRetentionSheetVisible: Boolean = false,
    val pendingRetention: PendingRetention? = null,
) {
    val isEmpty: Boolean get() = !isLoading && !loadFailed && notes.isEmpty()
    val canEmpty: Boolean get() = !isLoading && !loadFailed && notes.isNotEmpty()

    /** Nota cuya hoja de acciones está abierta; nula si no hay hoja o la nota ya no está en la lista. */
    val actionsNote: TrashItemUi? get() = actionsNoteId?.let { id -> notes.firstOrNull { it.id == id } }
}
