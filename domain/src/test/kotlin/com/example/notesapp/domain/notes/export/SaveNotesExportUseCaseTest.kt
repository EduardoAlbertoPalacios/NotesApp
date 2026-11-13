package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveNotesExportUseCaseTest {

    private lateinit var exporter: FakeNotesExporter
    private lateinit var saveExport: SaveNotesExportUseCase

    private val export = PreparedExport(id = "tmp-1", noteCount = 3, missingImageCount = 1)

    @Before
    fun setUp() {
        exporter = FakeNotesExporter()
        saveExport = SaveNotesExportUseCase(exporter)
    }

    @Test
    fun `given export and destination when saving then delegates both to exporter`() = runTest {
        saveExport(export, DESTINATION)

        assertEquals(listOf(export to DESTINATION), exporter.saves)
    }

    @Test
    fun `given exporter succeeds when saving then returns success and discards temp`() = runTest {
        val result = saveExport(export, DESTINATION)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(export), exporter.discarded)
    }

    @Test
    fun `given exporter fails when saving then returns write failed and discards temp`() = runTest {
        exporter.saveResult = AppResult.Error(ExportError.WriteFailed)

        val result = saveExport(export, DESTINATION)

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
        assertEquals(listOf(export), exporter.discarded)
    }

    @Test
    fun `given exporter throws when saving then propagates exception and discards temp`() = runTest {
        val failure = IOException("fake failure")
        exporter.saveFailure = failure

        val thrown = runCatching { saveExport(export, DESTINATION) }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(listOf(export), exporter.discarded)
    }

    @Test
    fun `given save in progress when coroutine is cancelled then still discards temp`() = runTest {
        exporter.saveGate = CompletableDeferred()
        val job = launch { saveExport(export, DESTINATION) }
        runCurrent()
        assertTrue(exporter.discarded.isEmpty())

        job.cancelAndJoin()

        // El fake suspende en discard: sin NonCancellable, la cancelación impediría registrarlo
        assertEquals(listOf(export), exporter.discarded)
    }

    private companion object {
        const val DESTINATION = "content://documents/notas.zip"
    }
}
