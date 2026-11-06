package com.example.notesapp.domain.notes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscardImageCopiesUseCaseTest {

    private val imageStorage = FakeImageStorage()
    private val discardImageCopies = DiscardImageCopiesUseCase(imageStorage)

    @Test
    fun `given pending copies when discarding then deletes each of them`() = runTest {
        imageStorage.storedPaths += listOf(PATH_A, PATH_B, PATH_KEPT)

        discardImageCopies(listOf(PATH_A, PATH_B))

        assertEquals(listOf(PATH_A, PATH_B), imageStorage.deletedPaths)
        assertEquals(setOf(PATH_KEPT), imageStorage.listPaths())
    }

    @Test
    fun `given no copies when discarding then deletes nothing`() = runTest {
        discardImageCopies(emptyList())

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    private companion object {
        const val PATH_A = "/files/note_images/a.jpg"
        const val PATH_B = "/files/note_images/b.jpg"
        const val PATH_KEPT = "/files/note_images/kept.jpg"
    }
}
