package com.example.notesapp.domain.notes

/**
 * Resultado de interpretar un cambio de texto con tareas. Si una regla anuló la tecla pulsada (Intro en
 * una tarea vacía, borrar al inicio de una tarea), [text] es el texto anterior y [cursor] la posición
 * donde debe quedar el cursor; si no, [text] es el texto nuevo y [cursor] es nulo.
 */
data class ChecklistEdit(
    val text: String,
    val tasks: List<ChecklistItem>,
    val cursor: Int? = null,
)
