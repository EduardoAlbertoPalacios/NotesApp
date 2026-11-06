package com.example.notesapp.domain.notes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanUpImageFilesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val cleanUpImageFiles = CleanUpImageFilesUseCase(repository, imageStorage)

    @Test
    fun `given files not used by any note when cleaning up then deletes only those`() = runTest {
        imageStorage.storedPaths += listOf(USED_A, ORPHAN_A, USED_B, ORPHAN_B)
        repository.imagePaths = setOf(USED_A, USED_B)

        cleanUpImageFiles()

        assertEquals(setOf(ORPHAN_A, ORPHAN_B), imageStorage.deletedPaths.toSet())
        assertEquals(setOf(USED_A, USED_B), imageStorage.listPaths())
    }

    @Test
    fun `given every file used when cleaning up then deletes nothing`() = runTest {
        imageStorage.storedPaths += listOf(USED_A, USED_B)
        repository.imagePaths = setOf(USED_A, USED_B)

        cleanUpImageFiles()

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given used path without file when cleaning up then deletes only the orphan files`() = runTest {
        imageStorage.storedPaths += listOf(USED_B, ORPHAN_A)
        repository.imagePaths = setOf(USED_A, USED_B)

        cleanUpImageFiles()

        assertEquals(listOf(ORPHAN_A), imageStorage.deletedPaths)
    }

    @Test
    fun `given no note uses images when cleaning up then deletes every file`() = runTest {
        imageStorage.storedPaths += listOf(ORPHAN_A, ORPHAN_B)
        repository.imagePaths = emptySet()

        cleanUpImageFiles()

        assertEquals(setOf(ORPHAN_A, ORPHAN_B), imageStorage.deletedPaths.toSet())
        assertTrue(imageStorage.listPaths().isEmpty())
    }

    @Test
    fun `given used paths saved with another prefix when cleaning up then keeps their files`() = runTest {
        imageStorage.storedPaths += listOf(CURRENT_USED_A, CURRENT_USED_B, CURRENT_ORPHAN)
        repository.imagePaths = setOf(LEGACY_USED_A, LEGACY_USED_B)

        cleanUpImageFiles()

        assertEquals(listOf(CURRENT_ORPHAN), imageStorage.deletedPaths)
        assertEquals(setOf(CURRENT_USED_A, CURRENT_USED_B), imageStorage.listPaths())
    }

    @Test
    fun `given used images and no file matching them when cleaning up then deletes nothing`() = runTest {
        imageStorage.storedPaths += listOf(ORPHAN_A, ORPHAN_B)
        repository.imagePaths = setOf(USED_A, USED_B)

        cleanUpImageFiles()

        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(setOf(ORPHAN_A, ORPHAN_B), imageStorage.listPaths())
    }

    @Test
    fun `given used images and no files at all when cleaning up then deletes nothing`() = runTest {
        repository.imagePaths = setOf(USED_A)

        cleanUpImageFiles()

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given used images cannot be read when cleaning up then deletes nothing`() = runTest {
        imageStorage.storedPaths += listOf(USED_A, ORPHAN_A)
        repository.getImagePathsFailure = NoteError.Storage

        cleanUpImageFiles()

        assertTrue(imageStorage.deletedPaths.isEmpty())
        assertEquals(setOf(USED_A, ORPHAN_A), imageStorage.listPaths())
    }

    private companion object {
        const val USED_A = "/files/note_images/used_a.jpg"
        const val USED_B = "/files/note_images/used_b.jpg"
        const val ORPHAN_A = "/files/note_images/orphan_a.jpg"
        const val ORPHAN_B = "/files/note_images/orphan_b.jpg"
        const val LEGACY_USED_A = "/data/data/com.example.notesapp/files/note_images/used_a.jpg"
        const val LEGACY_USED_B = "/data/data/com.example.notesapp/files/note_images/used_b.jpg"
        const val CURRENT_USED_A = "/data/user/0/com.example.notesapp/files/note_images/used_a.jpg"
        const val CURRENT_USED_B = "/data/user/0/com.example.notesapp/files/note_images/used_b.jpg"
        const val CURRENT_ORPHAN = "/data/user/0/com.example.notesapp/files/note_images/orphan_a.jpg"
    }
}
