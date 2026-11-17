package com.example.notesapp.domain.notes.trash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashRulesTest {

    @Test
    fun `given one millisecond before the period ends when checking expiry then is not expired`() {
        assertFalse(TrashRules.isExpired(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 7 * DAY - 1))
    }

    @Test
    fun `given exactly the period elapsed when checking expiry then is expired`() {
        assertTrue(TrashRules.isExpired(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 7 * DAY))
    }

    @Test
    fun `given more than the period elapsed when checking expiry then is expired`() {
        assertTrue(TrashRules.isExpired(trashedAt = NOW, TrashRetention.DAYS_30, now = NOW + 45 * DAY))
    }

    @Test
    fun `given never retention when checking expiry long after then is not expired`() {
        assertFalse(TrashRules.isExpired(trashedAt = 0, TrashRetention.NEVER, now = NOW))
    }

    @Test
    fun `given clock behind trash time when checking expiry then is not expired`() {
        assertFalse(TrashRules.isExpired(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW - 10 * DAY))
    }

    @Test
    fun `given note just trashed when computing days left then returns the full period`() {
        assertEquals(7, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW))
    }

    @Test
    fun `given one millisecond into the first day when computing days left then counts whole blocks of 24 hours`() {
        assertEquals(6, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 1))
    }

    @Test
    fun `given exactly one day elapsed when computing days left then returns period minus one`() {
        assertEquals(29, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_30, now = NOW + DAY))
    }

    @Test
    fun `given less than 24 hours left when computing days left then returns zero`() {
        assertEquals(0, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 7 * DAY - 1))
    }

    @Test
    fun `given exactly 24 hours left when computing days left then returns one`() {
        assertEquals(1, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 6 * DAY))
    }

    @Test
    fun `given period already elapsed when computing days left then returns zero`() {
        assertEquals(0, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_7, now = NOW + 20 * DAY))
    }

    @Test
    fun `given clock behind trash time when computing days left then caps at the period`() {
        assertEquals(90, TrashRules.daysLeft(trashedAt = NOW, TrashRetention.DAYS_90, now = NOW - 15 * DAY))
    }

    @Test
    fun `given never retention when computing days left then returns null`() {
        assertNull(TrashRules.daysLeft(trashedAt = NOW, TrashRetention.NEVER, now = NOW + 1_000 * DAY))
    }

    @Test
    fun `given notes of different ages when filtering expired then returns only expired in the same order`() {
        val old = trashedNote(id = 1, trashedAt = NOW - 10 * DAY)
        val recent = trashedNote(id = 2, trashedAt = NOW - DAY)
        val boundary = trashedNote(id = 3, trashedAt = NOW - 7 * DAY)
        val older = trashedNote(id = 4, trashedAt = NOW - 40 * DAY)

        val expired = TrashRules.expiredBy(listOf(old, recent, boundary, older), TrashRetention.DAYS_7, NOW)

        assertEquals(listOf(old, boundary, older), expired)
    }

    @Test
    fun `given never retention when filtering expired then returns none`() {
        val notes = listOf(trashedNote(id = 1, trashedAt = 0), trashedNote(id = 2, trashedAt = NOW - 400 * DAY))

        assertTrue(TrashRules.expiredBy(notes, TrashRetention.NEVER, NOW).isEmpty())
    }

    @Test
    fun `given empty trash when filtering expired then returns empty list`() {
        assertTrue(TrashRules.expiredBy(emptyList(), TrashRetention.DAYS_7, NOW).isEmpty())
    }

    @Test
    fun `given fewer days when comparing periods then is shorter`() {
        assertTrue(TrashRules.isShorter(TrashRetention.DAYS_7, TrashRetention.DAYS_30))
        assertTrue(TrashRules.isShorter(TrashRetention.DAYS_30, TrashRetention.DAYS_90))
    }

    @Test
    fun `given more days when comparing periods then is not shorter`() {
        assertFalse(TrashRules.isShorter(TrashRetention.DAYS_90, TrashRetention.DAYS_7))
    }

    @Test
    fun `given same period when comparing then is not shorter`() {
        TrashRetention.entries.forEach { assertFalse(TrashRules.isShorter(it, it)) }
    }

    @Test
    fun `given current never when comparing any period in days then is shorter`() {
        assertTrue(TrashRules.isShorter(TrashRetention.DAYS_90, TrashRetention.NEVER))
        assertTrue(TrashRules.isShorter(TrashRetention.DAYS_7, TrashRetention.NEVER))
    }

    @Test
    fun `given new never when comparing with any period then is not shorter`() {
        assertFalse(TrashRules.isShorter(TrashRetention.NEVER, TrashRetention.DAYS_7))
        assertFalse(TrashRules.isShorter(TrashRetention.NEVER, TrashRetention.DAYS_90))
    }

    @Test
    fun `given no stored choice when reading default then is thirty days`() {
        assertEquals(TrashRetention.DAYS_30, TrashRetention.DEFAULT)
    }
}
