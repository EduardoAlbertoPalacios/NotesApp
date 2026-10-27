package com.example.notesapp.data.notes

import com.example.notesapp.domain.notes.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistCodecTest {

    @Test
    fun `given pending and done tasks when encoding then uses line and state separated by semicolons`() {
        val tasks = listOf(task(0, isDone = false), task(2, isDone = true), task(3, isDone = false))

        assertEquals("0,0;2,1;3,0", tasks.encode())
    }

    @Test
    fun `given tasks when encoding and decoding then returns the same tasks`() {
        val tasks = listOf(task(0, isDone = false), task(1, isDone = true), task(3, isDone = false))

        assertEquals(tasks, tasks.encode().decodeTasks(content = "Tomates\nLeche\nPan\nCafé"))
    }

    @Test
    fun `given empty list when encoding then returns empty string`() {
        assertEquals("", emptyList<ChecklistItem>().encode())
    }

    @Test
    fun `given empty string when decoding then returns empty list`() {
        assertEquals(emptyList<ChecklistItem>(), "".decodeTasks(content = "Tomates\nLeche"))
    }

    @Test
    fun `given malformed entries when decoding then discards them and keeps the valid ones`() {
        val stored = listOf(
            "0,0",
            "1",
            "1,0,1",
            "a,1",
            "1,",
            ",1",
            "",
            "2,1",
        ).joinToString(";")

        assertEquals(
            listOf(task(0, isDone = false), task(2, isDone = true)),
            stored.decodeTasks(content = "a\nb\nc"),
        )
    }

    @Test
    fun `given entries with invalid state when decoding then discards them`() {
        val stored = "0,2;1,-1;2,x;3,true;4,1"

        assertEquals(listOf(task(4, isDone = true)), stored.decodeTasks(content = "a\nb\nc\nd\ne"))
    }

    @Test
    fun `given negative line when decoding then discards it`() {
        assertEquals(listOf(task(1, isDone = false)), "-1,1;1,0".decodeTasks(content = "a\nb"))
    }

    @Test
    fun `given lines beyond the content when decoding then discards them`() {
        assertEquals(
            listOf(task(0, isDone = true), task(2, isDone = false)),
            "0,1;2,0;3,0;99,1".decodeTasks(content = "a\nb\nc"),
        )
    }

    @Test
    fun `given empty content when decoding then only line zero is valid`() {
        assertEquals(listOf(task(0, isDone = false)), "0,0;1,1".decodeTasks(content = ""))
    }

    @Test
    fun `given content ending with line break when decoding then the last empty line is valid`() {
        assertEquals(listOf(task(1, isDone = true)), "1,1".decodeTasks(content = "a\n"))
    }

    @Test
    fun `given repeated lines when decoding then keeps only the first entry of each line`() {
        assertEquals(
            listOf(task(0, isDone = false), task(1, isDone = true)),
            "0,0;1,1;0,1;1,0".decodeTasks(content = "a\nb"),
        )
    }

    @Test
    fun `given unordered entries when decoding then returns them ordered by line`() {
        assertEquals(
            listOf(task(0, isDone = true), task(1, isDone = false), task(3, isDone = false)),
            "3,0;0,1;1,0".decodeTasks(content = "a\nb\nc\nd"),
        )
    }

    private fun task(line: Int, isDone: Boolean) = ChecklistItem(line, isDone)
}
