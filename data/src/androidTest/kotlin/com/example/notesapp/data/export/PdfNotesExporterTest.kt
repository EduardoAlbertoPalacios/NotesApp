package com.example.notesapp.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.R
import com.example.notesapp.domain.notes.ChecklistItem
import com.example.notesapp.domain.notes.FormatSpan
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.notes.TextFormat
import com.example.notesapp.domain.notes.export.ExportError
import com.example.notesapp.domain.notes.export.PreparedExport
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfNotesExporterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** La carpeta de temporales que usa [PdfNotesExporter]. */
    private val exportsDir = File(context.cacheDir, "exports")

    /** Imágenes de las notas y PDF extraídos del .zip para abrirlos con [PdfRenderer]. */
    private val workDir = File(context.cacheDir, "pdf_notes_exporter_test")

    @Before
    fun setUp() {
        exportsDir.deleteRecursively()
        workDir.deleteRecursively()
        workDir.mkdirs()
        TestDocumentsProvider.reset(context)
    }

    @After
    fun tearDown() {
        exportsDir.deleteRecursively()
        workDir.deleteRecursively()
        TestDocumentsProvider.reset(context)
    }

    // region prepare: el .zip

    @Test
    fun given_no_notes_when_preparing_then_returns_nothing_to_export() = runTest {
        val result = createExporter().prepare(emptyList())

        assertEquals(AppResult.Error(ExportError.NothingToExport), result)
    }

    @Test
    fun given_notes_when_preparing_then_zip_in_exports_folder_has_one_entry_per_note() = runTest {
        val notes = listOf(note(id = 1), note(id = 2), note(id = 3))

        val export = prepared(createExporter().prepare(notes))

        val archive = archiveOf(export)
        assertEquals(exportsDir.canonicalFile, archive.canonicalFile.parentFile)
        assertTrue(archive.name.endsWith(".zip"))
        assertEquals(3, entryNames(archive).size)
        assertEquals(3, export.noteCount)
    }

    @Test
    fun given_titles_untitled_and_duplicates_when_preparing_then_entries_follow_naming_in_note_order() = runTest {
        val untitled = context.getString(R.string.export_pdf_untitled)
        val notes = listOf(
            note(id = 1, title = "Compras"),
            note(id = 2, title = ""),
            note(id = 3, title = "compras"),
            note(id = 4, title = "Compras"),
            note(id = 5, title = "   "),
            note(id = 6, title = "a/b"),
        )

        val export = prepared(createExporter().prepare(notes))

        assertEquals(
            listOf(
                "Compras.pdf",
                "$untitled.pdf",
                "compras (2).pdf",
                "Compras (3).pdf",
                "$untitled (2).pdf",
                "a_b.pdf",
            ),
            entryNames(archiveOf(export)),
        )
    }

    @Test
    fun given_file_left_in_exports_folder_when_preparing_then_only_new_zip_remains() = runTest {
        val old = File(exportsDir.apply { mkdirs() }, "anterior.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val export = prepared(createExporter().prepare(listOf(note())))

        assertFalse(old.exists())
        assertEquals(listOf(export.id), exportsDir.list()?.toList())
    }

    @Test
    fun given_previous_export_when_preparing_again_then_previous_zip_is_removed() = runTest {
        val exporter = createExporter()
        val first = prepared(exporter.prepare(listOf(note())))

        val second = prepared(exporter.prepare(listOf(note())))

        assertFalse(archiveOf(first).exists())
        assertEquals(listOf(second.id), exportsDir.list()?.toList())
    }

    @Test
    fun given_cancellation_while_preparing_when_zip_is_half_written_then_no_zip_remains() = runTest {
        lateinit var job: Job
        val notes = CancellingList(
            notes = listOf(note(id = 1), note(id = 2), note(id = 3)),
            cancelAt = 1,
            onCancel = { job.cancel() },
        )
        val exporter = createExporter()

        job = launch { exporter.prepare(notes) }
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(exportsDir.listFiles().orEmpty().isEmpty())
    }

    // endregion

    // region prepare: los PDF

    @Test
    fun given_notes_when_preparing_then_every_entry_is_a_pdf_with_a4_pages() = runTest {
        val notes = listOf(
            note(id = 1, title = "Corta"),
            note(id = 2, title = "Larga", content = longContent()),
            note(id = 3, title = "", category = NoteCategory.WORK),
        )

        val export = prepared(createExporter().prepare(notes))

        val sizes = extractPdfs(archiveOf(export)).flatMap { pdf ->
            withPdf(pdf) { renderer ->
                (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page -> page.width to page.height }
                }
            }
        }
        assertTrue(sizes.isNotEmpty())
        assertTrue(sizes.all { it == A4_WIDTH to A4_HEIGHT })
    }

    @Test
    fun given_short_note_when_preparing_then_pdf_has_one_page() = runTest {
        val note = note(title = "Compras", content = "Pan\nLeche", category = NoteCategory.PERSONAL)

        val export = prepared(createExporter().prepare(listOf(note)))

        assertEquals(1, pageCount(extractPdfs(archiveOf(export)).single()))
    }

    @Test
    fun given_content_longer_than_a_page_when_preparing_then_pdf_continues_on_more_pages() = runTest {
        val note = note(title = "Diario", content = longContent())

        val export = prepared(createExporter().prepare(listOf(note)))

        assertTrue(pageCount(extractPdfs(archiveOf(export)).single()) > 1)
    }

    @Test
    fun given_image_taller_than_page_when_preparing_then_it_is_reduced_to_fit_one_page() = runTest {
        val image = savePng("alta.png", width = 200, height = 4000, color = Color.RED)
        val note = note(title = "Captura", content = "", images = listOf(NoteImage(id = 1, path = image.path)))

        val export = prepared(createExporter().prepare(listOf(note)))

        val pdf = extractPdfs(archiveOf(export)).single()
        assertEquals(0, export.missingImageCount)
        // El texto ocupa la primera página; la imagen, reducida, ocupa una sola página más.
        assertEquals(2, pageCount(pdf))
        val imagePage = renderPage(pdf, index = 1)
        // Centrada y reducida: rojo en el centro y blanco junto al margen izquierdo (sin reducir llenaría el ancho).
        assertEquals(Color.RED, imagePage.getPixel(A4_WIDTH / 2, A4_HEIGHT / 2))
        assertEquals(Color.WHITE, imagePage.getPixel(MARGIN + 10, A4_HEIGHT / 2))
    }

    @Test
    fun given_images_missing_on_disk_when_preparing_then_they_are_counted_and_pdf_is_still_written() = runTest {
        val valid = savePng("valida.png", width = 100, height = 100, color = Color.BLUE)
        val notes = listOf(
            note(
                id = 1,
                title = "Con huecos",
                images = listOf(
                    NoteImage(id = 1, path = File(workDir, "borrada.jpg").path),
                    NoteImage(id = 2, path = valid.path),
                ),
            ),
            note(id = 2, title = "Otra", images = listOf(NoteImage(id = 3, path = File(workDir, "falta.png").path))),
        )

        val export = prepared(createExporter().prepare(notes))

        assertEquals(2, export.missingImageCount)
        assertEquals(listOf(1, 1), extractPdfs(archiveOf(export)).map(::pageCount))
    }

    @Test
    fun given_undecodable_image_when_preparing_then_it_is_counted_as_missing() = runTest {
        val broken = File(workDir, "rota.jpg").apply { writeBytes("esto no es una imagen".toByteArray()) }
        val note = note(images = listOf(NoteImage(id = 1, path = broken.path)))

        val export = prepared(createExporter().prepare(listOf(note)))

        assertEquals(1, export.missingImageCount)
        assertEquals(1, pageCount(extractPdfs(archiveOf(export)).single()))
    }

    @Test
    fun given_colored_note_when_rendering_page_then_background_is_white() = runTest {
        val note = note(title = "Rosa", color = NoteColor.ROSE)

        val export = prepared(createExporter().prepare(listOf(note)))

        val page = renderPage(extractPdfs(archiveOf(export)).single(), index = 0)
        assertEquals(Color.WHITE, page.getPixel(5, 5))
        assertEquals(Color.WHITE, page.getPixel(MARGIN / 2, MARGIN / 2))
    }

    @Test
    fun given_formatting_spans_crossing_task_lines_when_preparing_then_pdf_is_written() = runTest {
        val content = "Comprar pan\nLlamar a Ana\nNota final"
        val note = note(
            content = content,
            spans = listOf(
                FormatSpan(start = 5, end = 18, format = TextFormat.BOLD),
                FormatSpan(start = 0, end = content.length, format = TextFormat.UNDERLINE),
                FormatSpan(start = 11, end = 12, format = TextFormat.STRIKETHROUGH),
                FormatSpan(start = 12, end = 13, format = TextFormat.STRIKETHROUGH),
                FormatSpan(start = 24, end = 26, format = TextFormat.BOLD),
                FormatSpan(start = 30, end = 500, format = TextFormat.BOLD),
            ),
            tasks = listOf(
                ChecklistItem(line = 0, isDone = false),
                ChecklistItem(line = 1, isDone = true),
                ChecklistItem(line = 2, isDone = false),
            ),
        )

        val export = prepared(createExporter().prepare(listOf(note)))

        assertEquals(1, pageCount(extractPdfs(archiveOf(export)).single()))
    }

    // endregion

    // region save

    @Test
    fun given_prepared_export_when_saving_to_document_then_document_has_zip_bytes_and_temp_is_kept() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note(id = 1), note(id = 2))))
        val destination = TestDocumentsProvider.uriOf("destino.zip")

        val result = exporter.save(export, destination.toString())

        assertEquals(AppResult.Success(Unit), result)
        assertArrayEquals(archiveOf(export).readBytes(), documentFile("destino.zip").readBytes())
        assertTrue(archiveOf(export).exists())
        assertTrue(TestDocumentsProvider.deletedIds.isEmpty())
    }

    @Test
    fun given_document_with_older_longer_content_when_saving_then_it_is_truncated_to_the_zip() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val document = documentFile("destino.zip").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(archiveOf(export).length().toInt() * 4) { 7 })
        }

        exporter.save(export, TestDocumentsProvider.uriOf("destino.zip").toString())

        assertArrayEquals(archiveOf(export).readBytes(), document.readBytes())
    }

    @Test
    fun given_file_uri_destination_when_saving_then_file_has_zip_bytes() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val destination = File(workDir, "destino.zip")

        val result = exporter.save(export, Uri.fromFile(destination).toString())

        assertEquals(AppResult.Success(Unit), result)
        assertArrayEquals(archiveOf(export).readBytes(), destination.readBytes())
    }

    @Test
    fun given_document_that_fails_on_write_when_saving_then_returns_write_failed_and_deletes_it() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val id = TestDocumentsProvider.READ_ONLY_PREFIX + "destino.zip"

        val result = exporter.save(export, TestDocumentsProvider.uriOf(id).toString())

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
        assertEquals(setOf(id), TestDocumentsProvider.deletedIds.toSet())
        assertFalse(documentFile(id).exists())
    }

    @Test
    fun given_document_that_cannot_be_opened_when_saving_then_returns_write_failed_and_deletes_it() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val id = TestDocumentsProvider.MISSING_PREFIX + "destino.zip"

        val result = exporter.save(export, TestDocumentsProvider.uriOf(id).toString())

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
        assertEquals(setOf(id), TestDocumentsProvider.deletedIds.toSet())
    }

    @Test
    fun given_failed_save_when_it_finishes_then_temp_zip_is_kept() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val id = TestDocumentsProvider.READ_ONLY_PREFIX + "destino.zip"

        exporter.save(export, TestDocumentsProvider.uriOf(id).toString())

        assertTrue(archiveOf(export).exists())
    }

    @Test
    fun given_unwritable_file_uri_when_saving_then_returns_write_failed() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        val destination = File(workDir, "no-existe/destino.zip")

        val result = exporter.save(export, Uri.fromFile(destination).toString())

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
        assertTrue(archiveOf(export).exists())
    }

    @Test
    fun given_export_whose_temp_is_gone_when_saving_then_returns_write_failed_and_deletes_destination() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        exporter.discard(export)

        val result = exporter.save(export, TestDocumentsProvider.uriOf("destino.zip").toString())

        assertEquals(AppResult.Error(ExportError.WriteFailed), result)
        assertEquals(setOf("destino.zip"), TestDocumentsProvider.deletedIds.toSet())
        assertFalse(documentFile("destino.zip").exists())
    }

    @Test
    fun given_cancellation_while_saving_when_copy_starts_then_destination_is_deleted() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))
        lateinit var job: Job
        TestDocumentsProvider.onOpen = { job.cancel() }

        job = launch { exporter.save(export, TestDocumentsProvider.uriOf("destino.zip").toString()) }
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(setOf("destino.zip"), TestDocumentsProvider.deletedIds.toSet())
        assertTrue(archiveOf(export).exists())
    }

    // endregion

    // region discard

    @Test
    fun given_prepared_export_when_discarding_then_temp_zip_is_deleted() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))

        exporter.discard(export)

        assertFalse(archiveOf(export).exists())
    }

    @Test
    fun given_unknown_id_when_discarding_then_nothing_happens() = runTest {
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))

        exporter.discard(export.copy(id = "desconocido.zip"))

        assertTrue(archiveOf(export).exists())
    }

    @Test
    fun given_id_with_path_segments_when_discarding_then_file_outside_folder_is_kept() = runTest {
        val outside = File(context.cacheDir, "fuera.zip").apply { writeBytes(byteArrayOf(1)) }
        val exporter = createExporter()
        val export = prepared(exporter.prepare(listOf(note())))

        exporter.discard(export.copy(id = "../fuera.zip"))

        val kept = outside.exists()
        outside.delete()
        assertTrue(kept)
        assertTrue(archiveOf(export).exists())
    }

    @Test
    fun given_document_destination_when_discarding_destination_then_it_is_deleted() = runTest {
        documentFile("destino.zip").apply { parentFile?.mkdirs() }.writeBytes(byteArrayOf())

        createExporter().discardDestination(TestDocumentsProvider.uriOf("destino.zip").toString())

        assertEquals(setOf("destino.zip"), TestDocumentsProvider.deletedIds.toSet())
        assertFalse(documentFile("destino.zip").exists())
    }

    @Test
    fun given_non_document_uri_when_discarding_destination_then_it_does_not_fail() = runTest {
        val destination = File(workDir, "destino.zip").apply { writeBytes(byteArrayOf(1)) }

        createExporter().discardDestination(Uri.fromFile(destination).toString())

        assertTrue(destination.exists())
    }

    // endregion

    private fun TestScope.createExporter() = PdfNotesExporter(context, StandardTestDispatcher(testScheduler))

    private fun note(
        id: Long = 1,
        title: String = "Título",
        content: String = "Contenido",
        color: NoteColor = NoteColor.NONE,
        category: NoteCategory? = null,
        images: List<NoteImage> = emptyList(),
        spans: List<FormatSpan> = emptyList(),
        tasks: List<ChecklistItem> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = color,
        isPinned = false,
        category = category,
        updatedAt = 1_794_480_600_000,
        images = images,
        contentSpans = spans,
        contentTasks = tasks,
    )

    private fun longContent(): String = (1..300).joinToString("\n") { "Línea número $it del contenido" }

    private fun prepared(result: AppResult<PreparedExport, ExportError>): PreparedExport =
        (result as AppResult.Success).data

    private fun archiveOf(export: PreparedExport) = File(exportsDir, export.id)

    private fun documentFile(documentId: String) = File(TestDocumentsProvider.directory(context), documentId)

    private fun entryNames(archive: File): List<String> =
        ZipInputStream(archive.inputStream()).use { zip -> generateSequence { zip.nextEntry }.map { it.name }.toList() }

    /** Copia cada entrada del .zip a un archivo, porque [PdfRenderer] necesita un descriptor con acceso aleatorio. */
    private fun extractPdfs(archive: File): List<File> = ZipInputStream(archive.inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.mapIndexed { index, _ ->
            File(workDir, "pdf-$index.pdf").apply { outputStream().use { zip.copyTo(it) } }
        }.toList()
    }

    private fun <T> withPdf(pdf: File, block: (PdfRenderer) -> T): T =
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use(block)
        }

    private fun pageCount(pdf: File): Int = withPdf(pdf) { it.pageCount }

    /** Dibuja la página a 72 ppp (un píxel por punto) sobre un bitmap transparente. */
    private fun renderPage(pdf: File, index: Int): Bitmap = withPdf(pdf) { renderer ->
        renderer.openPage(index).use { page ->
            Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also { bitmap ->
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    private fun savePng(name: String, width: Int, height: Int, color: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        return File(workDir, name).apply {
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    /**
     * Lista que cancela la exportación al leer por segunda vez la nota [cancelAt]: la primera lectura es al
     * calcular los nombres y la segunda, al empezar a escribir su PDF, con el .zip ya a medias.
     */
    private class CancellingList(
        private val notes: List<Note>,
        private val cancelAt: Int,
        private val onCancel: () -> Unit,
    ) : AbstractList<Note>() {
        private var reads = 0

        override val size: Int get() = notes.size

        override fun get(index: Int): Note {
            if (index == cancelAt && ++reads == 2) onCancel()
            return notes[index]
        }
    }

    private companion object {
        const val A4_WIDTH = 595
        const val A4_HEIGHT = 842
        const val MARGIN = 48
    }
}
