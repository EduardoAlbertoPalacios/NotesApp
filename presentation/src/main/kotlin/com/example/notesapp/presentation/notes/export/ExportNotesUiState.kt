package com.example.notesapp.presentation.notes.export

/** Nota de la lista de selección. [title] vacío se muestra como «Sin título». */
data class ExportNoteItem(val id: Long, val title: String)

data class ExportNotesUiState(
    val isLoading: Boolean = true,
    /** Todas las notas, en el orden de Mis notas con el filtro «Todas». */
    val notes: List<ExportNoteItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    /** Se están generando los PDF o copiando el .zip: la selección no se puede cambiar. */
    val isExporting: Boolean = false,
    /** No se pudieron leer las notas: se avisa en lugar de decir que no hay ninguna. */
    val loadFailed: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && !loadFailed && notes.isEmpty()
    val selectedCount: Int get() = notes.count { it.id in selectedIds }
    val allSelected: Boolean get() = notes.isNotEmpty() && selectedCount == notes.size
    val canExport: Boolean get() = selectedCount > 0 && !isExporting
}
