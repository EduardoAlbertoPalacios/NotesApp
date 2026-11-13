package com.example.notesapp.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.notesapp.common.dispatchers.IoDispatcher
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.R
import com.example.notesapp.domain.notes.Note
import com.example.notesapp.domain.notes.NoteCategory
import com.example.notesapp.domain.notes.export.ExportError
import com.example.notesapp.domain.notes.export.NoteExportNaming
import com.example.notesapp.domain.notes.export.NotesExporter
import com.example.notesapp.domain.notes.export.PreparedExport
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Exporta notas como un .zip con un PDF por nota (spec 011).
 *
 * El .zip se genera primero en `cacheDir/exports/` y después se copia al documento que creó el selector del
 * sistema (decisión D1). Solo se convierten en [ExportError.WriteFailed] los errores de E/S y de permisos;
 * la cancelación se propaga, pero antes se borra lo que se haya escrito a medias.
 */
internal class PdfNotesExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NotesExporter {

    private val directory: File get() = File(context.cacheDir, DIRECTORY)

    private val renderer = NotePdfRenderer(categoryLabel = { note -> note.category?.let(::labelOf) })

    override suspend fun prepare(notes: List<Note>): AppResult<PreparedExport, ExportError> =
        withContext(ioDispatcher) {
            if (notes.isEmpty()) return@withContext AppResult.Error(ExportError.NothingToExport)
            // Cada exportación vacía la carpeta: así nunca quedan restos de una anterior.
            directory.deleteRecursively()
            directory.mkdirs()
            val archive = File(directory, "${UUID.randomUUID()}$ARCHIVE_EXTENSION")
            var completed = false
            try {
                val missingImages = writeArchive(notes, archive)
                completed = true
                AppResult.Success(PreparedExport(archive.name, notes.size, missingImages))
            } catch (error: IOException) {
                AppResult.Error(ExportError.WriteFailed)
            } catch (error: SecurityException) {
                AppResult.Error(ExportError.WriteFailed)
            } finally {
                // También al cancelar: un .zip a medias no sirve para nada.
                if (!completed) archive.delete()
            }
        }

    /** Escribe cada PDF directamente en su entrada del .zip, una nota cada vez, para no tenerlas todas en memoria. */
    private suspend fun writeArchive(notes: List<Note>, archive: File): Int {
        val names = NoteExportNaming.pdfNames(notes.map { it.title }, context.getString(R.string.export_pdf_untitled))
        var missingImages = 0
        ZipOutputStream(archive.outputStream().buffered(), Charsets.UTF_8).use { zip ->
            notes.forEachIndexed { index, note ->
                currentCoroutineContext().ensureActive()
                zip.putNextEntry(ZipEntry(names[index]))
                missingImages += renderer.render(note, zip)
                zip.closeEntry()
            }
        }
        return missingImages
    }

    override suspend fun save(export: PreparedExport, destination: String): AppResult<Unit, ExportError> =
        withContext(ioDispatcher) {
            val uri = Uri.parse(destination)
            var completed = false
            try {
                val output = context.contentResolver.openOutputStream(uri, WRITE_TRUNCATE_MODE)
                    ?: return@withContext AppResult.Error(ExportError.WriteFailed)
                output.use { target -> archiveFile(export).inputStream().use { it.copyCancellableTo(target) } }
                completed = true
                AppResult.Success(Unit)
            } catch (error: IOException) {
                AppResult.Error(ExportError.WriteFailed)
            } catch (error: SecurityException) {
                AppResult.Error(ExportError.WriteFailed)
            } finally {
                // RF-23, RF-26: si falla o se cancela, el destino no debe quedar con un archivo incompleto.
                if (!completed) deleteDocument(uri)
            }
        }

    override suspend fun discard(export: PreparedExport) {
        withContext(ioDispatcher) { archiveFile(export).delete() }
    }

    override suspend fun discardDestination(destination: String) {
        withContext(ioDispatcher) { deleteDocument(Uri.parse(destination)) }
    }

    /**
     * Temporal de [export]. Solo se acepta un nombre de archivo, nunca una ruta, para que un id manipulado no
     * pueda apuntar fuera de la carpeta de exportaciones.
     */
    private fun archiveFile(export: PreparedExport): File = File(directory, File(export.id).name)

    /**
     * Borra el documento [uri] si se puede. Es limpieza de mejor esfuerzo: si no se puede borrar, no hay nada
     * más que hacer y no debe ocultar el resultado (o la cancelación) que la provocó, así que solo se registra.
     * Son las excepciones que `deleteDocument` lanza o que un proveedor puede devolver a través del sistema.
     */
    private fun deleteDocument(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (error: FileNotFoundException) {
            // El documento ya no existe: no queda nada que limpiar.
            logDeleteFailure(error)
        } catch (error: SecurityException) {
            // Se perdió el permiso sobre el documento.
            logDeleteFailure(error)
        } catch (error: IllegalArgumentException) {
            // La URI no es un documento (por ejemplo, no viene del selector).
            logDeleteFailure(error)
        } catch (error: UnsupportedOperationException) {
            // El proveedor del destino no permite borrar documentos.
            logDeleteFailure(error)
        } catch (error: IllegalStateException) {
            // El proveedor no pudo borrar el archivo (p. ej. el almacenamiento externo lo rechazó).
            logDeleteFailure(error)
        }
    }

    private fun logDeleteFailure(error: Exception) {
        Log.w(LOG_TAG, "No se pudo borrar el destino de la exportación", error)
    }

    /** Como [InputStream.copyTo], pero comprueba la cancelación entre bloques para no seguir copiando en vano. */
    private suspend fun InputStream.copyCancellableTo(output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
        }
    }

    private fun labelOf(category: NoteCategory): String = context.getString(
        when (category) {
            NoteCategory.WORK -> R.string.export_pdf_category_work
            NoteCategory.PERSONAL -> R.string.export_pdf_category_personal
        },
    )

    private companion object {
        const val DIRECTORY = "exports"
        const val LOG_TAG = "PdfNotesExporter"
        const val ARCHIVE_EXTENSION = ".zip"

        /** Escribir truncando: si el documento ya tenía contenido, no quedan restos al final. */
        const val WRITE_TRUNCATE_MODE = "wt"
    }
}
