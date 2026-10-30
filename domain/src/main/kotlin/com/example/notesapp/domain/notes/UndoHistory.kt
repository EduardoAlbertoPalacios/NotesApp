package com.example.notesapp.domain.notes

/**
 * Historial de deshacer del editor. [steps] guarda una foto por paso, de la más antigua a la más reciente;
 * [open] es el paso que aún admite más caracteres, o nulo si no hay ninguno abierto.
 */
data class UndoHistory(
    val steps: List<UndoSnapshot> = emptyList(),
    val open: OpenStep? = null,
) {
    val canUndo: Boolean get() = steps.isNotEmpty()
}
