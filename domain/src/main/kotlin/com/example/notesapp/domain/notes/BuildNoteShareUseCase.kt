package com.example.notesapp.domain.notes

import javax.inject.Inject

/**
 * Arma lo que se comparte a partir de la nota tal como se ve en el editor, sin leer ni guardar nada.
 *
 * - El texto es el título, una línea en blanco y el contenido, o solo la parte que exista.
 * - Cada tarea empieza por «☐ » (pendiente) o «☑ » (hecha). El contenido va sin formato.
 * - El título, si existe, es también el asunto.
 * - Las imágenes que ya no existen se omiten y se cuentan.
 *
 * Título y contenido se recortan en los extremos; si solo tienen espacios, cuentan como ausentes.
 */
class BuildNoteShareUseCase @Inject constructor(
    private val imageStorage: ImageStorage,
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        tasks: List<ChecklistItem>,
        imagePaths: List<String>,
    ): NoteShare {
        val sharedTitle = title.trim().ifEmpty { null }
        val sharedContent = contentWithMarkers(content, tasks)
        val text = listOfNotNull(sharedTitle, sharedContent).joinToString("\n\n").ifEmpty { null }
        val existing = imagePaths.filter { imageStorage.exists(it) }
        return NoteShare(
            subject = sharedTitle,
            text = text,
            imagePaths = existing,
            missingImageCount = imagePaths.size - existing.size,
        )
    }

    /**
     * Antepone el marcador a cada tarea y quita las líneas en blanco de los extremos y los espacios de
     * los bordes. Una línea con tarea nunca se quita ni se recorta, para que una tarea vacía quede «☐ ».
     */
    private fun contentWithMarkers(content: String, tasks: List<ChecklistItem>): String? {
        val doneByLine = tasks.associate { it.line to it.isDone }
        val lines = content.split('\n').mapIndexed { index, line ->
            when (doneByLine[index]) {
                null -> SharedLine(line, isTask = false)
                false -> SharedLine("$PENDING_MARKER$line", isTask = true)
                true -> SharedLine("$DONE_MARKER$line", isTask = true)
            }
        }
        val first = lines.indexOfFirst { it.isTask || it.text.isNotBlank() }
        if (first == -1) return null
        val last = lines.indexOfLast { it.isTask || it.text.isNotBlank() }
        return lines.subList(first, last + 1)
            .mapIndexed { index, line ->
                var text = line.text
                if (!line.isTask && index == 0) text = text.trimStart()
                if (!line.isTask && index == last - first) text = text.trimEnd()
                text
            }
            .joinToString("\n")
    }

    private data class SharedLine(val text: String, val isTask: Boolean)

    private companion object {
        const val PENDING_MARKER = "☐ "
        const val DONE_MARKER = "☑ "
    }
}
