package com.example.notesapp.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteUndoTest {

    // classify

    @Test
    fun `given one letter typed when classifying then is typing that keeps the step open`() {
        assertEquals(UndoEdit(EditKind.TYPING, closesStep = false), NoteUndo.classify("Hol", "Hola"))
    }

    @Test
    fun `given one letter typed in the middle when classifying then is typing`() {
        assertEquals(UndoEdit(EditKind.TYPING, closesStep = false), NoteUndo.classify("Hla", "Hola"))
    }

    @Test
    fun `given a space typed when classifying then is typing that closes the step`() {
        assertEquals(UndoEdit(EditKind.TYPING, closesStep = true), NoteUndo.classify("Hola", "Hola "))
    }

    @Test
    fun `given a line break typed when classifying then is typing that closes the step`() {
        assertEquals(UndoEdit(EditKind.TYPING, closesStep = true), NoteUndo.classify("Leche", "Leche\n"))
    }

    @Test
    fun `given a repeated letter typed next to the same letter when classifying then is typing`() {
        assertEquals(UndoEdit(EditKind.TYPING, closesStep = false), NoteUndo.classify("aa", "aaa"))
    }

    @Test
    fun `given one letter deleted when classifying then is deleting`() {
        assertEquals(UndoEdit(EditKind.DELETING), NoteUndo.classify("Hola", "Hol"))
    }

    @Test
    fun `given several characters pasted when classifying then is standalone`() {
        assertEquals(UndoEdit(EditKind.STANDALONE), NoteUndo.classify("Hola ", "Hola mundo"))
    }

    @Test
    fun `given a selection replaced by other text when classifying then is standalone`() {
        assertEquals(UndoEdit(EditKind.STANDALONE), NoteUndo.classify("Hola mundo", "Hola gente"))
    }

    @Test
    fun `given one character replaced by a different one in the same position when classifying then is standalone`() {
        // Borra 1 e inserta 1: es una sustitución de selección, no escritura.
        assertEquals(UndoEdit(EditKind.STANDALONE), NoteUndo.classify("abc", "axc"))
    }

    @Test
    fun `given a selection of several characters deleted when classifying then is standalone`() {
        assertEquals(UndoEdit(EditKind.STANDALONE), NoteUndo.classify("Hola mundo", "Hola "))
    }

    @Test
    fun `given the same text when classifying then is standalone`() {
        // Intro en una tarea vacía o borrar al inicio de una tarea: el texto no cambia, cambian las tareas.
        assertEquals(UndoEdit(EditKind.STANDALONE), NoteUndo.classify("Leche\n", "Leche\n"))
    }

    // record

    @Test
    fun `given hola mundo typed letter by letter when recording then leaves two steps`() {
        val history = typeInContent(UndoHistory(), from = "", text = "Hola mundo")

        assertEquals(listOf("", "Hola "), history.steps.map { it.content })
    }

    @Test
    fun `given hola mundo typed letter by letter when undoing then first undo leaves hola with space`() {
        val history = typeInContent(UndoHistory(), from = "", text = "Hola mundo")

        val (_, snapshot) = requireNotNull(NoteUndo.undo(history))

        assertEquals("Hola ", snapshot.content)
    }

    @Test
    fun `given typing then deleting in the same field when recording then leaves two steps`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Hola")

        val history = NoteUndo.record(typed, snapshot(content = "Hola"), NoteUndo.classify("Hola", "Hol"))
            .let { NoteUndo.record(it, snapshot(content = "Hol"), NoteUndo.classify("Hol", "Ho")) }

        assertEquals(listOf("", "Hola"), history.steps.map { it.content })
        assertEquals(OpenStep(EditKind.DELETING, EditField.CONTENT), history.open)
    }

    @Test
    fun `given deleting then typing in the same field when recording then leaves two steps`() {
        val deleted = NoteUndo.record(UndoHistory(), snapshot(content = "Hola"), UndoEdit(EditKind.DELETING))

        val history = typeInContent(deleted, from = "Hol", text = "a")

        assertEquals(listOf("Hola", "Hol"), history.steps.map { it.content })
        assertEquals(OpenStep(EditKind.TYPING, EditField.CONTENT), history.open)
    }

    @Test
    fun `given typing in title then in content when undoing then returns content snapshot first`() {
        val inTitle = NoteUndo.record(UndoHistory(), snapshot(title = "", field = EditField.TITLE), typing())
            .let { NoteUndo.record(it, snapshot(title = "I", field = EditField.TITLE), typing()) }
        val inContent = NoteUndo.record(inTitle, snapshot(title = "Id", content = "", field = EditField.CONTENT), typing())

        val (afterFirst, first) = requireNotNull(NoteUndo.undo(inContent))
        val (_, second) = requireNotNull(NoteUndo.undo(afterFirst))

        assertEquals(2, inContent.steps.size)
        assertEquals(snapshot(title = "Id", content = "", field = EditField.CONTENT), first)
        assertEquals(snapshot(title = "", field = EditField.TITLE), second)
    }

    @Test
    fun `given open typing step when recording standalone then stores new snapshot and leaves nothing open`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Ho")

        val history = NoteUndo.record(typed, snapshot(content = "Ho"), UndoEdit(EditKind.STANDALONE))

        assertEquals(listOf("", "Ho"), history.steps.map { it.content })
        assertNull(history.open)
    }

    @Test
    fun `given standalone after typing when typing next letter then starts another step`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Ho")
        val pasted = NoteUndo.record(typed, snapshot(content = "Ho"), UndoEdit(EditKind.STANDALONE))

        val history = typeInContent(pasted, from = "Hola", text = "s")

        assertEquals(listOf("", "Ho", "Hola"), history.steps.map { it.content })
        assertEquals(OpenStep(EditKind.TYPING, EditField.CONTENT), history.open)
    }

    @Test
    fun `given standalone followed by standalone when recording then leaves two steps`() {
        val history = NoteUndo.record(UndoHistory(), snapshot(content = "a"), UndoEdit(EditKind.STANDALONE))
            .let { NoteUndo.record(it, snapshot(content = "b"), UndoEdit(EditKind.STANDALONE)) }

        assertEquals(listOf("a", "b"), history.steps.map { it.content })
        assertNull(history.open)
    }

    @Test
    fun `given one more step than the maximum when recording then keeps the most recent and drops the first`() {
        val history = (0..NoteUndo.MAX_STEPS).fold(UndoHistory()) { acc, i ->
            NoteUndo.record(acc, snapshot(content = "paso $i"), UndoEdit(EditKind.STANDALONE))
        }

        assertEquals(NoteUndo.MAX_STEPS, history.steps.size)
        assertEquals((1..NoteUndo.MAX_STEPS).map { "paso $it" }, history.steps.map { it.content })
    }

    @Test
    fun `given closing typing without open step when recording then stores snapshot and stays closed`() {
        val history = NoteUndo.record(UndoHistory(), snapshot(content = "Hola"), UndoEdit(EditKind.TYPING, closesStep = true))

        assertEquals(listOf(snapshot(content = "Hola")), history.steps)
        assertNull(history.open)
    }

    // close

    @Test
    fun `given open typing step when closing then next letter starts a new step`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Ho")

        val history = typeInContent(NoteUndo.close(typed), from = "Ho", text = "la")

        assertEquals(listOf("", "Ho"), history.steps.map { it.content })
    }

    @Test
    fun `given open step when closing then keeps the steps and leaves nothing open`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Ho")

        val closed = NoteUndo.close(typed)

        assertEquals(typed.steps, closed.steps)
        assertNull(closed.open)
    }

    // undo

    @Test
    fun `given several steps when undoing repeatedly then returns them from most recent to oldest`() {
        val history = listOf("a", "b", "c").fold(UndoHistory()) { acc, content ->
            NoteUndo.record(acc, snapshot(content = content), UndoEdit(EditKind.STANDALONE))
        }

        val undone = generateSequence(NoteUndo.undo(history)) { (rest, _) -> NoteUndo.undo(rest) }
            .map { (_, snapshot) -> snapshot.content }
            .toList()

        assertEquals(listOf("c", "b", "a"), undone)
    }

    @Test
    fun `given open step when undoing then leaves nothing open`() {
        val typed = typeInContent(UndoHistory(), from = "", text = "Ho")

        val (history, _) = requireNotNull(NoteUndo.undo(typed))

        assertNull(history.open)
    }

    @Test
    fun `given empty history when undoing then returns null`() {
        assertNull(NoteUndo.undo(UndoHistory()))
    }

    @Test
    fun `given last step when undoing then can undo becomes false`() {
        val history = NoteUndo.record(UndoHistory(), snapshot(content = "a"), UndoEdit(EditKind.STANDALONE))
        assertTrue(history.canUndo)

        val (emptied, _) = requireNotNull(NoteUndo.undo(history))

        assertFalse(emptied.canUndo)
    }

    /** Escribe [text] letra a letra en el contenido a partir de [from], como lo haría el editor. */
    private fun typeInContent(history: UndoHistory, from: String, text: String): UndoHistory {
        var current = from
        var result = history
        for (char in text) {
            val next = current + char
            result = NoteUndo.record(result, snapshot(content = current), NoteUndo.classify(current, next))
            current = next
        }
        return result
    }

    private fun typing() = UndoEdit(EditKind.TYPING)

    private fun snapshot(
        title: String = "",
        content: String = "",
        field: EditField = EditField.CONTENT,
    ) = UndoSnapshot(
        title = title,
        content = content,
        contentSpans = emptyList(),
        contentTasks = emptyList(),
        field = field,
        selectionStart = if (field == EditField.CONTENT) content.length else title.length,
        selectionEnd = if (field == EditField.CONTENT) content.length else title.length,
    )
}
