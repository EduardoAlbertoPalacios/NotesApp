package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.time.TimeProvider
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class GetExportArchiveNameUseCaseTest {

    private val utc = ZoneId.of("UTC")
    private val mexicoCity = ZoneId.of("America/Mexico_City")

    private fun createUseCase(now: String, zone: ZoneId) =
        GetExportArchiveNameUseCase(TimeProvider { Instant.parse(now).toEpochMilli() }) { zone }

    @Test
    fun `given current time when getting archive name then uses date from time provider`() {
        val getName = createUseCase("2026-11-12T15:30:00Z", utc)

        assertEquals("Notas 2026-11-12.zip", getName())
    }

    @Test
    fun `given early utc instant when zone is utc then uses that same day`() {
        val getName = createUseCase("2026-11-13T03:00:00Z", utc)

        assertEquals("Notas 2026-11-13.zip", getName())
    }

    @Test
    fun `given early utc instant when zone is mexico city then uses previous day`() {
        // 03:00 UTC del día 13 todavía es el día 12 en Ciudad de México (UTC-6)
        val getName = createUseCase("2026-11-13T03:00:00Z", mexicoCity)

        assertEquals("Notas 2026-11-12.zip", getName())
    }

    @Test
    fun `given zone supplier when getting archive name twice then reads zone on each call`() {
        var zone = utc
        val timeProvider = TimeProvider { Instant.parse("2026-11-13T03:00:00Z").toEpochMilli() }
        val getName = GetExportArchiveNameUseCase(timeProvider) { zone }

        val first = getName()
        zone = mexicoCity
        val second = getName()

        assertEquals("Notas 2026-11-13.zip", first)
        assertEquals("Notas 2026-11-12.zip", second)
    }
}
