package com.example.notesapp.domain.notes

/** Un cambio clasificado para el historial; [closesStep] cierra el paso tras unirse a él (espacio o salto de línea). */
data class UndoEdit(
    val kind: EditKind,
    val closesStep: Boolean = false,
)
