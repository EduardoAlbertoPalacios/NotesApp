package com.example.notesapp.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteChecklistTest {

    private fun pending(line: Int) = ChecklistItem(line, isDone = false)
    private fun done(line: Int) = ChecklistItem(line, isDone = true)

    // linesIn

    @Test
    fun `given cursor when getting lines then returns the cursor line`() {
        assertEquals(1..1, NoteChecklist.linesIn("ab\ncd\nef", start = 4, end = 4))
    }

    @Test
    fun `given selection over several lines when getting lines then returns every covered line`() {
        assertEquals(0..2, NoteChecklist.linesIn("ab\ncd\nef", start = 1, end = 7))
    }

    @Test
    fun `given reversed selection when getting lines then returns the same lines`() {
        assertEquals(0..2, NoteChecklist.linesIn("ab\ncd\nef", start = 7, end = 1))
    }

    @Test
    fun `given selection ending right after a line break when getting lines then excludes the next line`() {
        assertEquals(0..0, NoteChecklist.linesIn("ab\ncd\nef", start = 0, end = 3))
    }

    @Test
    fun `given positions out of range when getting lines then clamps them to the content`() {
        assertEquals(0..2, NoteChecklist.linesIn("ab\ncd\nef", start = -5, end = 100))
    }

    @Test
    fun `given cursor after a trailing line break when getting lines then returns the last empty line`() {
        assertEquals(1..1, NoteChecklist.linesIn("ab\n", start = 3, end = 3))
    }

    @Test
    fun `given empty content when getting lines then returns line zero`() {
        assertEquals(0..0, NoteChecklist.linesIn("", start = 0, end = 0))
    }

    // toggleLines

    @Test
    fun `given cursor on normal line when toggling then line becomes pending task`() {
        val result = NoteChecklist.toggleLines("Leche\nPan", emptyList(), start = 7, end = 7)

        assertEquals(listOf(pending(1)), result)
    }

    @Test
    fun `given cursor on task when toggling then line becomes normal text`() {
        val result = NoteChecklist.toggleLines("Leche\nPan", listOf(pending(0), done(1)), start = 7, end = 7)

        assertEquals(listOf(pending(0)), result)
    }

    @Test
    fun `given mixed selection when toggling then every line is a task and existing tasks keep their state`() {
        val tasks = listOf(done(1), pending(3))

        val result = NoteChecklist.toggleLines("a\nb\nc\nd", tasks, start = 0, end = 5)

        assertEquals(listOf(pending(0), done(1), pending(2), pending(3)), result)
    }

    @Test
    fun `given selection of only done and pending tasks when toggling then all become text and lose state`() {
        val tasks = listOf(done(0), pending(1), done(2), pending(3))

        val result = NoteChecklist.toggleLines("a\nb\nc\nd", tasks, start = 0, end = 5)

        assertEquals(listOf(pending(3)), result)
    }

    @Test
    fun `given cursor on empty line when toggling then becomes empty pending task`() {
        val result = NoteChecklist.toggleLines("a\n\nc", emptyList(), start = 2, end = 2)

        assertEquals(listOf(pending(1)), result)
    }

    @Test
    fun `given empty content when toggling then line zero becomes pending task`() {
        val result = NoteChecklist.toggleLines("", emptyList(), start = 0, end = 0)

        assertEquals(listOf(pending(0)), result)
    }

    @Test
    fun `given unordered tasks when toggling then result is sorted and without duplicates`() {
        val tasks = listOf(done(2), pending(0), pending(2), pending(7))

        val result = NoteChecklist.toggleLines("a\nb\nc", tasks, start = 2, end = 2)

        assertEquals(listOf(pending(0), pending(1), done(2)), result)
    }

    // setDone

    @Test
    fun `given pending task when marking done then is done in the same position`() {
        val result = NoteChecklist.setDone(listOf(pending(0), pending(1), pending(2)), line = 1, isDone = true)

        assertEquals(listOf(pending(0), done(1), pending(2)), result)
    }

    @Test
    fun `given done task when unmarking then is pending in the same position`() {
        val result = NoteChecklist.setDone(listOf(pending(0), done(1), pending(2)), line = 1, isDone = false)

        assertEquals(listOf(pending(0), pending(1), pending(2)), result)
    }

    @Test
    fun `given line that is not a task when marking done then tasks do not change`() {
        val tasks = listOf(pending(0), done(2))

        assertEquals(tasks, NoteChecklist.setDone(tasks, line = 1, isDone = true))
    }

    // onTextChanged: Intro en una tarea con texto

    @Test
    fun `given enter at end of task when text changes then adds pending task below`() {
        val result = NoteChecklist.onTextChanged("Leche", "Leche\n", cursor = 6, tasks = listOf(pending(0)))

        assertEquals(ChecklistEdit("Leche\n", listOf(pending(0), pending(1))), result)
    }

    @Test
    fun `given enter at end of done task when text changes then original keeps done and new is pending`() {
        val result = NoteChecklist.onTextChanged("a\nb", "a\n\nb", cursor = 2, tasks = listOf(done(0), pending(1)))

        assertEquals(ChecklistEdit("a\n\nb", listOf(done(0), pending(1), pending(2))), result)
    }

    @Test
    fun `given enter in middle of task when text changes then right part is a new pending task`() {
        val result = NoteChecklist.onTextChanged(
            old = "Leche avena",
            new = "Leche\n avena",
            cursor = 6,
            tasks = listOf(done(0)),
        )

        assertEquals(ChecklistEdit("Leche\n avena", listOf(done(0), pending(1))), result)
    }

    // onTextChanged: Intro en una tarea vacía

    @Test
    fun `given enter on empty task when text changes then cancels enter and line stops being task`() {
        val result = NoteChecklist.onTextChanged("a\n", "a\n\n", cursor = 3, tasks = listOf(pending(0), pending(1)))

        assertEquals(ChecklistEdit("a\n", listOf(pending(0)), cursor = 2), result)
    }

    @Test
    fun `given enter on empty task in middle of list when text changes then tasks below remain tasks`() {
        val tasks = listOf(pending(0), pending(1), done(2))

        val result = NoteChecklist.onTextChanged("a\n\nc", "a\n\n\nc", cursor = 3, tasks = tasks)

        assertEquals(ChecklistEdit("a\n\nc", listOf(pending(0), done(2)), cursor = 2), result)
    }

    @Test
    fun `given enter on empty task between empty lines when text changes then uses cursor to find the task`() {
        val result = NoteChecklist.onTextChanged("\n\n", "\n\n\n", cursor = 2, tasks = listOf(pending(1)))

        assertEquals(ChecklistEdit("\n\n", emptyList(), cursor = 1), result)
    }

    // onTextChanged: borrar al inicio de una tarea

    @Test
    fun `given delete at start of task when text changes then cancels delete and line stops being task`() {
        val result = NoteChecklist.onTextChanged("a\nb", "ab", cursor = 1, tasks = listOf(pending(1)))

        assertEquals(ChecklistEdit("a\nb", emptyList(), cursor = 2), result)
    }

    @Test
    fun `given delete at start of line that was task when deleting again then joins lines`() {
        val first = NoteChecklist.onTextChanged("a\nb", "ab", cursor = 1, tasks = listOf(done(0), pending(1)))

        val second = NoteChecklist.onTextChanged(first.text, "ab", cursor = 1, tasks = first.tasks)

        assertEquals(ChecklistEdit("ab", listOf(done(0))), second)
    }

    @Test
    fun `given whole task text deleted without its line break when text changes then remains empty task`() {
        val result = NoteChecklist.onTextChanged("a\nbc", "a\n", cursor = 2, tasks = listOf(pending(0), done(1)))

        assertEquals(ChecklistEdit("a\n", listOf(pending(0), done(1))), result)
        assertNull(result.cursor)
    }

    // onTextChanged: pegar

    @Test
    fun `given several lines pasted in task when text changes then each line after first is pending task`() {
        val result = NoteChecklist.onTextChanged(
            old = "Pan \nCafé",
            new = "Pan uno\ndos\ntres\nCafé",
            cursor = 16,
            tasks = listOf(done(0), pending(1)),
        )

        assertEquals(
            ChecklistEdit("Pan uno\ndos\ntres\nCafé", listOf(done(0), pending(1), pending(2), pending(3))),
            result,
        )
    }

    @Test
    fun `given several lines pasted in normal line when text changes then shifts tasks below without new ones`() {
        val result = NoteChecklist.onTextChanged("Pan\nCafé", "Panx\ny\nCafé", cursor = 6, tasks = listOf(done(1)))

        assertEquals(ChecklistEdit("Panx\ny\nCafé", listOf(done(2))), result)
    }

    // onTextChanged: desplazamientos

    @Test
    fun `given line break typed in line above task when text changes then task moves down`() {
        val result = NoteChecklist.onTextChanged("ab\nc", "a\nb\nc", cursor = 2, tasks = listOf(done(1)))

        assertEquals(ChecklistEdit("a\nb\nc", listOf(done(2))), result)
    }

    @Test
    fun `given line break deleted above task when text changes then task moves up`() {
        val result = NoteChecklist.onTextChanged("a\n\nb", "a\nb", cursor = 1, tasks = listOf(done(2)))

        assertEquals(ChecklistEdit("a\nb", listOf(done(1))), result)
    }

    // onTextChanged: borrar varias líneas

    @Test
    fun `given several lines deleted from middle of line when text changes then result keeps starting line state`() {
        val tasks = listOf(done(0), pending(1), pending(2), pending(3))

        val result = NoteChecklist.onTextChanged("ab\ncd\nef\ng", "af\ng", cursor = 1, tasks = tasks)

        assertEquals(ChecklistEdit("af\ng", listOf(done(0), pending(1))), result)
    }

    @Test
    fun `given several lines deleted from middle of normal line when text changes then result is not task`() {
        val result = NoteChecklist.onTextChanged("ab\ncd\nef", "af", cursor = 1, tasks = listOf(pending(1), done(2)))

        assertEquals(ChecklistEdit("af", emptyList()), result)
    }

    @Test
    fun `given whole lines deleted from line start when text changes then result keeps last absorbed line state`() {
        val tasks = listOf(pending(1), pending(2), done(3))

        val result = NoteChecklist.onTextChanged("a\nb\nc\nd", "a\nd", cursor = 2, tasks = tasks)

        assertEquals(ChecklistEdit("a\nd", listOf(done(1))), result)
    }

    // onTextChanged: reemplazar y sin cambios

    @Test
    fun `given selection in a line replaced by text when text changes then tasks do not change`() {
        val tasks = listOf(done(0), pending(1))

        val result = NoteChecklist.onTextChanged("Leche\nPan", "Café\nPan", cursor = 4, tasks = tasks)

        assertEquals(ChecklistEdit("Café\nPan", tasks), result)
    }

    @Test
    fun `given selection across lines replaced by text when text changes then keeps starting line state`() {
        val result = NoteChecklist.onTextChanged("ab\ncd", "aXd", cursor = 2, tasks = listOf(pending(0), done(1)))

        assertEquals(ChecklistEdit("aXd", listOf(pending(0))), result)
    }

    @Test
    fun `given same text when text changes then returns normalized tasks`() {
        val tasks = listOf(done(1), pending(0), pending(1), pending(5))

        val result = NoteChecklist.onTextChanged("a\nb", "a\nb", cursor = 1, tasks = tasks)

        assertEquals(ChecklistEdit("a\nb", listOf(pending(0), done(1))), result)
    }

    // fromLegacyMarkers

    @Test
    fun `given pending marker with one space when converting then becomes pending task without marker`() {
        val result = NoteChecklist.fromLegacyMarkers("☐ Leche")

        assertEquals(LegacyConversion("Leche", listOf(pending(0)), listOf(0..1)), result)
    }

    @Test
    fun `given done marker with several spaces when converting then becomes done task without marker`() {
        val result = NoteChecklist.fromLegacyMarkers("☑   Pan")

        assertEquals(LegacyConversion("Pan", listOf(done(0)), listOf(0..3)), result)
    }

    @Test
    fun `given marker without space when converting then removes only the marker`() {
        val result = NoteChecklist.fromLegacyMarkers("☐Café")

        assertEquals(LegacyConversion("Café", listOf(pending(0)), listOf(0..0)), result)
    }

    @Test
    fun `given marker in middle of line when converting then line does not change`() {
        val result = NoteChecklist.fromLegacyMarkers("Comprar ☐ leche\nPan ☑")

        assertEquals(LegacyConversion("Comprar ☐ leche\nPan ☑", emptyList(), emptyList()), result)
    }

    @Test
    fun `given note without markers when converting then keeps text without tasks or ranges`() {
        val result = NoteChecklist.fromLegacyMarkers("Hola\nmundo")

        assertEquals(LegacyConversion("Hola\nmundo", emptyList(), emptyList()), result)
    }

    @Test
    fun `given markers on first and last line when converting then converts both`() {
        val result = NoteChecklist.fromLegacyMarkers("☐ a\nb\n☑ c")

        assertEquals(LegacyConversion("a\nb\nc", listOf(pending(0), done(2)), listOf(0..1, 6..7)), result)
    }

    @Test
    fun `given seed note when converting then returns real tasks and removed ranges`() {
        val original = "☐  Tomates\n☐  Leche de avena\n☑  Pan integral\n☐  Café"

        val result = NoteChecklist.fromLegacyMarkers(original)

        assertEquals("Tomates\nLeche de avena\nPan integral\nCafé", result.text)
        assertEquals(listOf(pending(0), pending(1), done(2), pending(3)), result.tasks)
        assertEquals(listOf(0..2, 11..13, 29..31, 45..47), result.removedRanges)
    }

    @Test
    fun `given converted note when removing ranges from original then equals converted text`() {
        val original = "☐  Tomates\nNota ☐\n☑\tPan\n☐Café"

        val result = NoteChecklist.fromLegacyMarkers(original)

        val withoutRanges = original.indices
            .filterNot { index -> result.removedRanges.any { index in it } }
            .map { original[it] }
            .joinToString("")
        assertEquals(result.text, withoutRanges)
    }
}
