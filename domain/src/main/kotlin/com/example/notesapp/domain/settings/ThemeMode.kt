package com.example.notesapp.domain.settings

enum class ThemeMode {
    LIGHT,
    DARK,
    ;

    companion object {
        /** Tema si no hay ninguno guardado o no se puede leer. */
        val DEFAULT = DARK
    }
}
