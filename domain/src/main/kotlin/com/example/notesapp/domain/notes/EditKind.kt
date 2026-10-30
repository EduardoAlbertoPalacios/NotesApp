package com.example.notesapp.domain.notes

/** Tipo de un cambio para el historial de deshacer. */
enum class EditKind {
    /** Un carácter escrito: se une al paso de escritura abierto en el mismo campo. */
    TYPING,

    /** Un carácter borrado: se une al paso de borrado abierto en el mismo campo. */
    DELETING,

    /** Pegar, cortar, sustituir o borrar una selección, formato, tareas: siempre es un paso completo. */
    STANDALONE,
}
