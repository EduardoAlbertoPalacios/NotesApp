package com.example.notesapp.data.export

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException
import java.util.Collections

/**
 * Proveedor de documentos mínimo para las pruebas: hace de documento creado por el selector del sistema.
 *
 * Cada documento es un archivo de [directory]. Según el prefijo de su id simula un fallo:
 * - [READ_ONLY_PREFIX]: se abre, pero cualquier escritura falla (como un destino lleno o sin permiso).
 * - [MISSING_PREFIX]: no se puede abrir.
 *
 * Registra los documentos borrados en [deletedIds].
 */
class TestDocumentsProvider : DocumentsProvider() {

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID))

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID))

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        onOpen?.invoke()
        if (documentId.startsWith(MISSING_PREFIX)) throw FileNotFoundException(documentId)
        val file = fileOf(documentId)
        if (documentId.startsWith(READ_ONLY_PREFIX)) {
            file.createNewFile()
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun deleteDocument(documentId: String) {
        fileOf(documentId).delete()
        deletedIds += documentId
    }

    private fun fileOf(documentId: String): File {
        val context = context ?: throw FileNotFoundException(documentId)
        return File(directory(context).apply { mkdirs() }, documentId)
    }

    companion object {
        const val AUTHORITY = "com.example.notesapp.data.test.documents"
        const val READ_ONLY_PREFIX = "solo-lectura-"
        const val MISSING_PREFIX = "inexistente-"

        /** Ids de los documentos borrados desde el último [reset]. */
        val deletedIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

        /** Se llama al abrir cualquier documento, antes de devolverlo. */
        @Volatile
        var onOpen: (() -> Unit)? = null

        fun directory(context: Context): File = File(context.filesDir, "test_documents")

        fun uriOf(documentId: String): Uri = DocumentsContract.buildDocumentUri(AUTHORITY, documentId)

        fun reset(context: Context) {
            deletedIds.clear()
            onOpen = null
            directory(context).deleteRecursively()
        }
    }
}
