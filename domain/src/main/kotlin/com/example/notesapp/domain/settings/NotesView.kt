package com.example.notesapp.domain.settings

/** Cómo se muestran las notas en Mis notas. */
enum class NotesView {
    GRID,
    LIST,
    ;

    companion object {
        /** Vista si el usuario no ha elegido otra. */
        val DEFAULT = GRID
    }
}
