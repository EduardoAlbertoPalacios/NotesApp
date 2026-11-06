package com.example.notesapp.data.images

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.notesapp.common.result.AppResult
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageStorageImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** La carpeta que usa [ImageStorageImpl]; vive en el almacenamiento de la app de pruebas. */
    private val directory = File(context.filesDir, "note_images")

    @Before
    fun setUp() {
        directory.deleteRecursively()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun given_no_copies_when_listing_then_returns_empty() = runTest {
        assertTrue(createStorage().listPaths().isEmpty())
    }

    @Test
    fun given_files_in_directory_when_listing_then_returns_their_absolute_paths() = runTest {
        val first = copy("a.jpg")
        val second = copy("b.png")

        val paths = createStorage().listPaths()

        assertEquals(setOf(first.absolutePath, second.absolutePath), paths)
    }

    @Test
    fun given_subdirectory_when_listing_then_ignores_it() = runTest {
        val file = copy("a.jpg")
        File(directory, "carpeta").mkdirs()

        assertEquals(setOf(file.absolutePath), createStorage().listPaths())
    }

    @Test
    fun given_file_outside_directory_when_listing_then_ignores_it() = runTest {
        val file = copy("a.jpg")
        val outside = File(context.filesDir, "fuera.jpg").apply { writeBytes(byteArrayOf(0)) }

        val paths = createStorage().listPaths()

        outside.delete()
        assertEquals(setOf(file.absolutePath), paths)
    }

    @Test
    fun given_saved_image_when_listing_then_includes_its_copy() = runTest {
        val source = File(context.cacheDir, "origen.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val storage = createStorage()

        val saved = storage.save(Uri.fromFile(source).toString())

        source.delete()
        assertEquals(setOf((saved as AppResult.Success).data), storage.listPaths())
    }

    @Test
    fun given_deleted_copy_when_listing_then_omits_it() = runTest {
        val deleted = copy("a.jpg")
        val kept = copy("b.jpg")
        val storage = createStorage()

        storage.delete(deleted.absolutePath)

        assertEquals(setOf(kept.absolutePath), storage.listPaths())
    }

    private fun TestScope.createStorage() = ImageStorageImpl(context, StandardTestDispatcher(testScheduler))

    private fun copy(name: String): File =
        File(directory.apply { mkdirs() }, name).apply { writeBytes(byteArrayOf(0)) }
}
