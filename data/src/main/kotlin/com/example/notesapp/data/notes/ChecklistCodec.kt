package com.example.notesapp.data.notes

import com.example.notesapp.domain.notes.ChecklistItem

// Tareas guardadas en la columna `content_tasks`: `línea,estado` separados por `;`, con estado 0 (pendiente)
// o 1 (hecha). Ejemplo: "0,0;2,1". Cadena vacía = sin tareas.

private const val TASK_SEPARATOR = ";"
private const val FIELD_SEPARATOR = ","
private const val PENDING = "0"
private const val DONE = "1"

internal fun List<ChecklistItem>.encode(): String =
    joinToString(TASK_SEPARATOR) { task ->
        listOf(task.line, if (task.isDone) DONE else PENDING).joinToString(FIELD_SEPARATOR)
    }

/**
 * Lee las tareas guardadas. Descarta las entradas mal formadas, las repetidas y las de líneas que no
 * existen en [content], para que un dato corrupto nunca impida abrir la nota.
 */
internal fun String.decodeTasks(content: String): List<ChecklistItem> {
    val lineCount = content.count { it == '\n' } + 1
    return split(TASK_SEPARATOR).mapNotNull { entry ->
        val fields = entry.split(FIELD_SEPARATOR)
        if (fields.size != 2) return@mapNotNull null
        val line = fields[0].toIntOrNull()?.takeIf { it in 0 until lineCount } ?: return@mapNotNull null
        val isDone = when (fields[1]) {
            PENDING -> false
            DONE -> true
            else -> return@mapNotNull null
        }
        ChecklistItem(line, isDone)
    }.distinctBy { it.line }.sortedBy { it.line }
}
