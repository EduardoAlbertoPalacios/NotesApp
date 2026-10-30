package com.example.notesapp.domain.notes

/** Paso de escritura o borrado que todavía admite más caracteres del mismo [kind] en el mismo [field]. */
data class OpenStep(
    val kind: EditKind,
    val field: EditField,
)
