package com.example.notesapp.data.images

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.notesapp.common.dispatchers.IoDispatcher
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.ImageStorage
import com.example.notesapp.domain.notes.NoteError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Copia las imágenes a `filesDir/note_images`. El selector de fotos solo da acceso temporal a la
 * imagen original, así que la nota guarda su propia copia.
 */
internal class ImageStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ImageStorage {

    private val directory: File get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override suspend fun save(sourceUri: String): AppResult<String, NoteError> = withContext(ioDispatcher) {
        val uri = Uri.parse(sourceUri)
        val target = File(directory, "${UUID.randomUUID()}.${extensionOf(uri)}")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext AppResult.Error(NoteError.ImageUnavailable)
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            AppResult.Success(target.absolutePath)
        } catch (error: IOException) {
            target.delete()
            AppResult.Error(NoteError.ImageUnavailable)
        } catch (error: SecurityException) {
            target.delete()
            AppResult.Error(NoteError.ImageUnavailable)
        }
    }

    override suspend fun delete(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            // Solo borra copias propias, nunca archivos fuera de la carpeta de imágenes de la app.
            if (file.parentFile?.canonicalPath == directory.canonicalPath) file.delete()
        }
    }

    private fun extensionOf(uri: Uri): String =
        context.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: DEFAULT_EXTENSION

    private companion object {
        const val DIRECTORY = "note_images"
        const val DEFAULT_EXTENSION = "jpg"
    }
}
