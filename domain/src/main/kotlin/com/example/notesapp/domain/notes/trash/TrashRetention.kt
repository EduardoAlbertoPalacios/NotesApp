package com.example.notesapp.domain.notes.trash

/** Cuánto tiempo se guarda una nota en la papelera antes de borrarse sola. */
enum class TrashRetention(
    /** Días de plazo; `null` en [NEVER], que nunca borra solo. */
    val days: Int?,
) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),
    NEVER(null),
    ;

    companion object {
        /** Plazo si el usuario no ha elegido otro. */
        val DEFAULT = DAYS_30
    }
}
