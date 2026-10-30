package com.example.notesapp.domain.notes

/**
 * Reglas del historial de deshacer del editor. Cada paso guarda una foto de la nota antes de empezar
 * ([UndoSnapshot]); escribir o borrar letra a letra se agrupa en un mismo paso hasta que este se cierra.
 */
object NoteUndo {

    /**
     * Clasifica el cambio de texto de [old] a [new], ya aplicadas las reglas de tareas:
     * - un carácter insertado y nada borrado es [EditKind.TYPING], y cierra el paso si es un espacio o un salto de línea;
     * - un carácter borrado y nada insertado es [EditKind.DELETING];
     * - cualquier otro cambio es [EditKind.STANDALONE]: pegar, cortar, sustituir o borrar una selección,
     *   una palabra entera del teclado, o un texto igual cuyas tareas cambiaron (Intro en una tarea vacía,
     *   borrar al inicio de una tarea).
     */
    fun classify(old: String, new: String): UndoEdit {
        // Lo cambiado está entre el prefijo y el sufijo comunes.
        val maxCommon = minOf(old.length, new.length)
        var prefix = 0
        while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        val removed = old.length - suffix - prefix
        val inserted = new.length - suffix - prefix
        return when {
            removed == 0 && inserted == 1 -> UndoEdit(EditKind.TYPING, closesStep = new[prefix] in STEP_BREAKS)
            removed == 1 && inserted == 0 -> UndoEdit(EditKind.DELETING)
            else -> UndoEdit(EditKind.STANDALONE)
        }
    }

    /**
     * Registra un cambio. [before] es la foto de la nota antes del cambio, con el campo que cambia.
     * - Escribir o borrar con un paso abierto del mismo tipo y campo se une a él: su foto ya existe.
     * - Si no, [before] empieza un paso nuevo, que queda abierto para los caracteres siguientes.
     * - [EditKind.STANDALONE] es siempre un paso nuevo y completo: no deja nada abierto.
     * - Un cambio que cierra el paso ([UndoEdit.closesStep]) se une o lo empieza y después lo cierra.
     *
     * Con más de [MAX_STEPS] pasos se descarta el más antiguo.
     */
    fun record(history: UndoHistory, before: UndoSnapshot, edit: UndoEdit): UndoHistory {
        val open = history.open
        val joinsOpen = edit.kind != EditKind.STANDALONE && open == OpenStep(edit.kind, before.field)
        val steps = if (joinsOpen) history.steps else (history.steps + before).takeLast(MAX_STEPS)
        val staysOpen = edit.kind != EditKind.STANDALONE && !edit.closesStep
        return UndoHistory(steps = steps, open = if (staysOpen) OpenStep(edit.kind, before.field) else null)
    }

    /** Cierra el paso abierto, por una pausa al escribir o por mover el cursor: lo siguiente empieza otro paso. */
    fun close(history: UndoHistory): UndoHistory = history.copy(open = null)

    /** Saca el paso más reciente y devuelve su foto, para volver a ponerla en el editor; nulo si no hay pasos. */
    fun undo(history: UndoHistory): Pair<UndoHistory, UndoSnapshot>? {
        val last = history.steps.lastOrNull() ?: return null
        return UndoHistory(steps = history.steps.dropLast(1), open = null) to last
    }

    /** Número máximo de pasos que guarda el historial. */
    const val MAX_STEPS = 100

    /** Caracteres que, al escribirse, cierran el paso de escritura que los contiene. */
    private val STEP_BREAKS = setOf(' ', '\n')
}
