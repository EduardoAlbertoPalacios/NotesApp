package com.example.notesapp.domain.notes

/**
 * Reglas de las tareas del contenido. Una tarea es una línea entera (separada por saltos de línea),
 * identificada por su índice desde 0.
 *
 * Las funciones que reciben el contenido devuelven listas normalizadas: ordenadas por línea, sin líneas
 * repetidas y sin líneas que no existen en el contenido.
 */
object NoteChecklist {

    /** Si la línea [line] es una tarea. */
    fun isTask(tasks: List<ChecklistItem>, line: Int): Boolean = tasks.any { it.line == line }

    /**
     * Líneas que abarca la selección de [start] a [end]; con cursor ([start] == [end]), la línea del cursor.
     * Una selección que termina justo después de un salto de línea no incluye la línea siguiente.
     */
    fun linesIn(content: String, start: Int, end: Int): IntRange {
        val from = minOf(start, end).coerceIn(0, content.length)
        val to = maxOf(start, end).coerceIn(0, content.length)
        val lastPosition = if (to > from && content[to - 1] == '\n') to - 1 else to
        return lineAt(content, from)..lineAt(content, lastPosition)
    }

    /**
     * Si alguna línea abarcada por la selección no es tarea, la convierte en tarea pendiente y deja
     * las que ya lo eran como estaban; si todas lo son, las convierte en texto normal.
     */
    fun toggleLines(content: String, tasks: List<ChecklistItem>, start: Int, end: Int): List<ChecklistItem> {
        val lines = linesIn(content, start, end)
        val toggled = if (lines.all { isTask(tasks, it) }) {
            tasks.filterNot { it.line in lines }
        } else {
            tasks + lines.filterNot { isTask(tasks, it) }.map { ChecklistItem(it, isDone = false) }
        }
        return normalize(toggled, lineCount(content))
    }

    /** Marca la tarea de la línea [line] como hecha o pendiente. Si la línea no es tarea, no cambia nada. */
    fun setDone(tasks: List<ChecklistItem>, line: Int, isDone: Boolean): List<ChecklistItem> =
        tasks.map { if (it.line == line) it.copy(isDone = isDone) else it }

    /**
     * Ajusta las tareas tras cambiar el texto de [old] a [new]; [cursor] es la posición del cursor después
     * del cambio y sirve para saber dónde ocurrió cuando el texto no basta (por ejemplo, Intro entre dos
     * líneas vacías).
     *
     * - Intro en una tarea vacía: se anula el salto y la línea deja de ser tarea.
     * - Borrar el salto de línea que precede a una tarea: se anula el borrado y la línea deja de ser tarea.
     * - Insertar saltos de línea en una tarea (Intro o pegar): las líneas nuevas son tareas pendientes.
     * - Borrar varias líneas: las absorbidas dejan de ser tareas; la resultante conserva el estado de la
     *   línea donde empezó el borrado.
     * - En todos los casos, las tareas de más abajo se desplazan las líneas que se agregaron o quitaron.
     */
    fun onTextChanged(old: String, new: String, cursor: Int, tasks: List<ChecklistItem>): ChecklistEdit {
        if (old == new) return ChecklistEdit(new, normalize(tasks, lineCount(new)))
        val edit = findEdit(old, new, cursor)
        val removed = old.substring(edit.start, edit.oldEnd)
        val inserted = new.substring(edit.start, edit.newEnd)
        val startLine = lineAt(old, edit.start)

        if (removed.isEmpty() && inserted == "\n" && isTask(tasks, startLine) && lineText(old, startLine).isEmpty()) {
            return ChecklistEdit(old, tasks.filterNot { it.line == startLine }, cursor = edit.start)
        }
        if (inserted.isEmpty() && removed == "\n" && cursor == edit.start && isTask(tasks, startLine + 1)) {
            return ChecklistEdit(old, tasks.filterNot { it.line == startLine + 1 }, cursor = edit.start + 1)
        }

        val removedLines = removed.count { it == '\n' }
        val insertedLines = inserted.count { it == '\n' }
        // Borrar líneas enteras desde el inicio de una línea deja como resultante la última absorbida,
        // así que conserva su estado y no el de la primera.
        val keptLine = if (removedLines > 0 && insertedLines == 0 && isLineStart(old, edit.start)) {
            startLine + removedLines
        } else {
            startLine
        }
        val shifted = tasks.mapNotNull { task ->
            when {
                task.line < startLine -> task
                task.line == keptLine -> task.copy(line = startLine)
                task.line <= startLine + removedLines -> null
                else -> task.copy(line = task.line - removedLines + insertedLines)
            }
        }
        val added = if (isTask(tasks, startLine)) {
            (1..insertedLines).map { ChecklistItem(startLine + it, isDone = false) }
        } else {
            emptyList()
        }
        return ChecklistEdit(new, normalize(shifted + added, lineCount(new)))
    }

    /**
     * Convierte en tarea pendiente cada línea que empieza por «☐» y en tarea hecha cada una que empieza
     * por «☑», quitando el carácter y los espacios que lo siguen. Las demás líneas no cambian.
     */
    fun fromLegacyMarkers(content: String): LegacyConversion {
        val text = StringBuilder()
        val tasks = mutableListOf<ChecklistItem>()
        val removedRanges = mutableListOf<IntRange>()
        var lineStart = 0
        content.split('\n').forEachIndexed { index, line ->
            if (index > 0) text.append('\n')
            val isDone = when (line.firstOrNull()) {
                PENDING_MARKER -> false
                DONE_MARKER -> true
                else -> null
            }
            if (isDone == null) {
                text.append(line)
            } else {
                val markerLength = 1 + line.drop(1).takeWhile { it.isWhitespace() }.length
                text.append(line, markerLength, line.length)
                tasks += ChecklistItem(index, isDone)
                removedRanges += lineStart until lineStart + markerLength
            }
            lineStart += line.length + 1
        }
        return LegacyConversion(text.toString(), tasks, removedRanges)
    }

    private const val PENDING_MARKER = '☐'
    private const val DONE_MARKER = '☑'

    /** Rango editado: de [start] a [oldEnd] en el texto anterior y de [start] a [newEnd] en el nuevo. */
    private data class TextEdit(val start: Int, val oldEnd: Int, val newEnd: Int)

    /**
     * Ubica el cambio. Primero supone que lo escrito termina en el cursor, como al teclear, borrar o pegar;
     * si eso no cuadra con los textos, usa el prefijo y el sufijo comunes más largos.
     */
    private fun findEdit(old: String, new: String, cursor: Int): TextEdit {
        var prefix = 0
        val maxCommon = minOf(old.length, new.length)
        while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++

        if (cursor in 0..new.length) {
            val tailLength = new.length - cursor
            val oldEnd = old.length - tailLength
            // El inicio no puede pasar del cursor ni del final de lo quitado: así un Intro entre líneas
            // vacías se ubica en la línea del cursor y no en la última de las iguales.
            val start = minOf(prefix, cursor, oldEnd)
            if (start >= 0 && old.regionMatches(oldEnd, new, cursor, tailLength)) {
                return TextEdit(start, oldEnd, cursor)
            }
        }
        var suffix = 0
        while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        return TextEdit(prefix, old.length - suffix, new.length - suffix)
    }

    private fun isLineStart(content: String, position: Int): Boolean = position == 0 || content[position - 1] == '\n'

    private fun lineText(content: String, line: Int): String = content.split('\n').getOrElse(line) { "" }

    private fun lineAt(content: String, position: Int): Int =
        (0 until position).count { content[it] == '\n' }

    private fun lineCount(content: String): Int = content.count { it == '\n' } + 1

    private fun normalize(tasks: List<ChecklistItem>, lineCount: Int): List<ChecklistItem> =
        tasks.filter { it.line in 0 until lineCount }.distinctBy { it.line }.sortedBy { it.line }
}
