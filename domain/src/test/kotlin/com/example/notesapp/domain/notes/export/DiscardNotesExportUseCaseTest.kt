package com.example.notesapp.domain.notes.export

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DiscardNotesExportUseCaseTest {

    private lateinit var exporter: FakeNotesExporter
    private lateinit var discardExport: DiscardNotesExportUseCase

    @Before
    fun setUp() {
        exporter = FakeNotesExporter()
        discardExport = DiscardNotesExportUseCase(exporter)
    }

    @Test
    fun `given prepared export when discarding then exporter discards that export`() = runTest {
        val export = PreparedExport(id = "tmp-7", noteCount = 1, missingImageCount = 0)

        discardExport(export)

        assertEquals(listOf(export), exporter.discarded)
        assertEquals(emptyList<String>(), exporter.discardedDestinations)
    }
}
