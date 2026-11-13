package com.example.notesapp.domain.notes.export

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PrepareNotesExportUseCaseTest {

    private lateinit var exporter: FakeNotesExporter
    private lateinit var prepareExport: PrepareNotesExportUseCase

    @Before
    fun setUp() {
        exporter = FakeNotesExporter()
        prepareExport = PrepareNotesExportUseCase(exporter)
    }

    @Test
    fun `given empty list when preparing then returns nothing to export without calling exporter`() = runTest {
        val result = prepareExport(emptyList())

        assertEquals(AppResult.Error(ExportError.NothingToExport), result)
        assertTrue(exporter.preparedNotes.isEmpty())
    }

    @Test
    fun `given notes when preparing then delegates the same notes in the same order`() = runTest {
        val notes = listOf(note(id = 3, title = "C"), note(id = 1, title = "A"), note(id = 2, title = "B"))

        prepareExport(notes)

        assertEquals(listOf(notes), exporter.preparedNotes)
    }

    @Test
    fun `given exporter succeeds when preparing then returns prepared export`() = runTest {
        val prepared = PreparedExport(id = "tmp-42", noteCount = 1, missingImageCount = 3)
        exporter.prepareResult = AppResult.Success(prepared)

        val result = prepareExport(listOf(note()))

        assertEquals(AppResult.Success(prepared), result)
    }

    @Test
    fun `given exporter fails when preparing then returns write failed`() = runTest {
        exporter.prepareResult = AppResult.Error(ExportError.WriteFailed)

        val result = prepareExport(listOf(note()))

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
    }

    private fun note(id: Long = 1, title: String = "Título") = Note(
        id = id,
        title = title,
        content = "",
        color = NoteColor.AQUA,
        isPinned = false,
        category = null,
        updatedAt = 0,
    )
}
