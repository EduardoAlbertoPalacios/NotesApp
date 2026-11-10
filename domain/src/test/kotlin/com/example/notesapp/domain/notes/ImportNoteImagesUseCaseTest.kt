package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.FakeImageStorage.Companion.copyOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportNoteImagesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val importNoteImages = ImportNoteImagesUseCase(imageStorage)

    @Test
    fun `given no uris when importing then returns nothing without copying`() = runTest {
        val result = importNoteImages(emptyList())

        assertEquals(AppResult.Success(ImportedImages(paths = emptyList(), failedCount = 0)), result)
        assertTrue(imageStorage.savedUris.isEmpty())
    }

    @Test
    fun `given uris when importing then returns the copies in order`() = runTest {
        val result = importNoteImages(listOf(URI_A, URI_B))

        assertEquals(
            AppResult.Success(ImportedImages(paths = listOf(copyOf(URI_A), copyOf(URI_B)), failedCount = 0)),
            result,
        )
        assertEquals(listOf(URI_A, URI_B), imageStorage.savedUris)
    }

    @Test
    fun `given uris when importing then does not touch the notes`() = runTest {
        importNoteImages(listOf(URI_A, URI_B))

        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.addedImages.isEmpty())
        assertTrue(repository.savedChanges.isEmpty())
    }

    @Test
    fun `given some uris unavailable when importing then returns the copied ones and how many failed`() = runTest {
        imageStorage.unavailableUris += listOf(URI_A, URI_C)

        val result = importNoteImages(listOf(URI_A, URI_B, URI_C))

        assertEquals(AppResult.Success(ImportedImages(paths = listOf(copyOf(URI_B)), failedCount = 2)), result)
    }

    @Test
    fun `given no uri can be copied when importing then returns image unavailable`() = runTest {
        imageStorage.unavailableUris += listOf(URI_A, URI_B)

        val result = importNoteImages(listOf(URI_A, URI_B))

        assertEquals(AppResult.Error(NoteError.ImageUnavailable), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    private companion object {
        const val URI_A = "content://media/picker/a"
        const val URI_B = "content://media/picker/b"
        const val URI_C = "content://media/picker/c"
    }
}
