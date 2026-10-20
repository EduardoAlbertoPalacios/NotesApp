package com.example.notesapp.domain.notes

/**
 * Reglas del formato del contenido. Los rangos son semiabiertos: de `start` (incluido) a `end` (excluido).
 *
 * Todas las funciones devuelven tramos normalizados: sin tramos vacíos, sin solapes del mismo formato
 * y con los contiguos del mismo formato fusionados.
 */
object NoteFormatting {

    /** Si todo el rango tiene [format]. Un rango vacío nunca lo tiene. */
    fun isApplied(spans: List<FormatSpan>, start: Int, end: Int, format: TextFormat): Boolean {
        if (start >= end) return false
        var covered = start
        for (span in normalize(spans).filter { it.format == format }) {
            if (span.start > covered) break
            if (span.end > covered) covered = span.end
            if (covered >= end) return true
        }
        return false
    }

    /**
     * Si el rango no tiene [format] completo, lo aplica a todo el rango; si lo tiene completo, lo quita.
     * Los demás formatos del rango no cambian.
     */
    fun toggle(spans: List<FormatSpan>, start: Int, end: Int, format: TextFormat): List<FormatSpan> {
        if (start >= end) return normalize(spans)
        return if (isApplied(spans, start, end, format)) {
            normalize(spans.flatMap { span -> if (span.format == format) span.without(start, end) else listOf(span) })
        } else {
            normalize(spans + FormatSpan(start, end, format))
        }
    }

    /** Formatos del carácter anterior a [position]: los que tendrá lo que se escriba ahí. Ninguno al inicio. */
    fun formatsAt(spans: List<FormatSpan>, position: Int): Set<TextFormat> {
        if (position <= 0) return emptySet()
        val index = position - 1
        return spans.filter { index >= it.start && index < it.end }.mapTo(mutableSetOf()) { it.format }
    }

    /**
     * Formatos activos para la selección de [start] a [end]: con cursor ([start] == [end]), los del carácter
     * anterior; con selección, los que cubren toda la selección.
     */
    fun activeFormats(spans: List<FormatSpan>, start: Int, end: Int): Set<TextFormat> =
        if (start == end) {
            formatsAt(spans, start)
        } else {
            TextFormat.entries.filterTo(mutableSetOf()) { isApplied(spans, start, end, it) }
        }

    /**
     * Ajusta los tramos tras cambiar el texto de [old] a [new].
     *
     * Lo insertado (escrito o pegado) toma exactamente [activeFormats], no el formato de los tramos vecinos:
     * un tramo que cruza el punto de edición se parte en dos. Lo borrado se quita de los tramos.
     */
    fun onTextChanged(
        spans: List<FormatSpan>,
        old: String,
        new: String,
        activeFormats: Set<TextFormat>,
    ): List<FormatSpan> {
        if (old == new) return normalize(spans)
        val maxCommon = minOf(old.length, new.length)
        var prefix = 0
        while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        val oldEnd = old.length - suffix
        val newEnd = new.length - suffix
        val delta = newEnd - oldEnd

        val shifted = spans.flatMap { span ->
            buildList {
                if (span.start < prefix) add(span.copy(end = minOf(span.end, prefix)))
                if (span.end > oldEnd) add(span.copy(start = maxOf(span.start, oldEnd) + delta, end = span.end + delta))
            }
        }
        val inserted = if (newEnd > prefix) activeFormats.map { FormatSpan(prefix, newEnd, it) } else emptyList()
        return normalize(shifted + inserted)
    }

    private fun FormatSpan.without(start: Int, end: Int): List<FormatSpan> = listOf(
        copy(end = minOf(this.end, start)),
        copy(start = maxOf(this.start, end)),
    ).filter { it.start < it.end }

    private fun normalize(spans: List<FormatSpan>): List<FormatSpan> =
        spans
            .filter { it.start >= 0 && it.start < it.end }
            .groupBy { it.format }
            .flatMap { (_, sameFormat) ->
                sameFormat.sortedBy { it.start }.fold(mutableListOf<FormatSpan>()) { merged, span ->
                    val last = merged.lastOrNull()
                    // Solapados o contiguos: se fusionan en uno solo.
                    if (last != null && span.start <= last.end) {
                        merged[merged.lastIndex] = last.copy(end = maxOf(last.end, span.end))
                    } else {
                        merged += span
                    }
                    merged
                }
            }
            .sortedWith(compareBy({ it.start }, { it.format.ordinal }))
}
