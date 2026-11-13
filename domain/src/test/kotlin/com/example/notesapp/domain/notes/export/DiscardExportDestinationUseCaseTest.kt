package com.example.notesapp.domain.notes.export

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DiscardExportDestinationUseCaseTest {

    private lateinit var exporter: FakeNotesExporter
    private lateinit var discardDestination: DiscardExportDestinationUseCase

    @Before
    fun setUp() {
        exporter = FakeNotesExporter()
        discardDestination = DiscardExportDestinationUseCase(exporter)
    }

    @Test
    fun `given destination when discarding then exporter discards that destination`() = runTest {
        discardDestination(DESTINATION)

        assertEquals(listOf(DESTINATION), exporter.discardedDestinations)
        assertEquals(emptyList<PreparedExport>(), exporter.discarded)
    }

    private companion object {
        const val DESTINATION = "content://documents/notas.zip"
    }
}
