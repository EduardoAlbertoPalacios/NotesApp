package com.example.notesapp.presentation.notes.list

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDateFormatterTest {

    private val timeZone = TimeZone.getTimeZone("UTC")
    // 6 oct 2026, 12:00 UTC
    private val now = utc(2026, Calendar.OCTOBER, 6, 12, 0)
    private val formatter = NoteDateFormatter(now = { now }, locale = Locale.forLanguageTag("es-ES"), timeZone = timeZone)

    @Test
    fun `given time earlier today when formatting then returns today with time`() {
        assertEquals(NoteDateLabel.Today("9:38"), formatter.format(utc(2026, Calendar.OCTOBER, 6, 9, 38)))
    }

    @Test
    fun `given just after midnight today when formatting then still returns today`() {
        assertTrue(formatter.format(utc(2026, Calendar.OCTOBER, 6, 0, 1)) is NoteDateLabel.Today)
    }

    @Test
    fun `given late yesterday when formatting then returns yesterday`() {
        assertEquals(NoteDateLabel.Yesterday, formatter.format(utc(2026, Calendar.OCTOBER, 5, 23, 59)))
    }

    @Test
    fun `given older date when formatting then returns day and month`() {
        val label = formatter.format(utc(2026, Calendar.OCTOBER, 4, 10, 0))

        assertTrue(label is NoteDateLabel.Date)
        assertTrue((label as NoteDateLabel.Date).dayMonth.startsWith("4 oct"))
    }

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
