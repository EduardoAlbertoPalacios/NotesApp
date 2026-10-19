package com.example.notesapp.domain.notes

import java.text.Normalizer
import java.util.Locale

/**
 * Compara el texto buscado con el de las notas sin distinguir mayúsculas, tildes ni diéresis.
 * La «ñ» se conserva como letra propia: «ano» no coincide con «año».
 */
internal object NoteTextMatcher {

    /** Palabras normalizadas del texto buscado, sin los espacios sobrantes; vacía si el texto está en blanco. */
    fun words(query: String): List<String> = normalize(query).split(WHITESPACE).filter { it.isNotEmpty() }

    /**
     * La nota coincide si cada una de [words] aparece, aunque sea como parte de una palabra,
     * en su título o en su contenido. Las palabras pueden repartirse entre ambos campos.
     */
    fun matches(note: Note, words: List<String>): Boolean {
        val title = normalize(note.title)
        val content = normalize(note.content)
        return words.all { word -> word in title || word in content }
    }

    /** Minúsculas y sin marcas diacríticas, salvo la virgulilla de la «ñ». */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return Normalizer.normalize(DIACRITICS.replace(decomposed, ""), Normalizer.Form.NFC)
    }

    // Tras NFD, «ñ» es «n» + U+0303: se quita toda marca combinante menos esa virgulilla detrás de una «n».
    private val DIACRITICS = Regex("""[\p{Mn}&&[^\u0303]]|(?<!n)\u0303""")
    private val WHITESPACE = Regex("""\s+""")
}
