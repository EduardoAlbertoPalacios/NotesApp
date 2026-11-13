package com.example.notesapp.domain.notes.export

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Reglas de nombres de la exportación: el del .zip y el de cada PDF que contiene. */
object NoteExportNaming {

    /** Máximo de caracteres (puntos de código) del nombre de un PDF, sin la extensión. */
    const val MAX_NAME_LENGTH = 100

    private const val ARCHIVE_PREFIX = "Notas "
    private const val ARCHIVE_EXTENSION = ".zip"
    private const val PDF_EXTENSION = ".pdf"
    private const val REPLACEMENT = '_'
    private val InvalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    private val ArchiveDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Nombre propuesto para el .zip: «Notas AAAA-MM-DD.zip», con la fecha de [epochMillis] en [zone]. */
    fun archiveName(epochMillis: Long, zone: ZoneId): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return ARCHIVE_PREFIX + ArchiveDate.format(date) + ARCHIVE_EXTENSION
    }

    /**
     * Un nombre de PDF por cada título de [titles], en el mismo orden. Un título vacío usa [untitled]. Los
     * nombres repetidos, sin distinguir mayúsculas, reciben « (2)», « (3)», … en el orden de la lista.
     */
    fun pdfNames(titles: List<String>, untitled: String): List<String> {
        val used = mutableSetOf<String>()
        return titles.map { title ->
            val base = baseName(title).ifEmpty { untitled }
            var name = base
            var copy = 1
            while (!used.add(name.lowercase())) {
                copy++
                name = "$base ($copy)"
            }
            name + PDF_EXTENSION
        }
    }

    /** Título recortado, sin caracteres no permitidos y con [MAX_NAME_LENGTH] caracteres como máximo. */
    private fun baseName(title: String): String {
        val sanitized = title.trim().map { if (it in InvalidChars) REPLACEMENT else it }.joinToString("")
        return sanitized.takeCodePoints(MAX_NAME_LENGTH).trim()
    }

    /** Los [count] primeros puntos de código, sin partir un emoji ni otro par sustituto. */
    private fun String.takeCodePoints(count: Int): String =
        if (codePointCount(0, length) <= count) this else substring(0, offsetByCodePoints(0, count))
}
