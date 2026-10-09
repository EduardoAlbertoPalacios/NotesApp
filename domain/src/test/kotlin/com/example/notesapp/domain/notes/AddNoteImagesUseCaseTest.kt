package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeImageStorage.Companion.copyOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddNoteImagesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val addNoteImages = AddNoteImagesUseCase(repository, imageStorage, TimeProvider { NOW })

    @Test
    fun `given no uris when adding then returns the note unchanged without touching anything`() = runTest {
        val note = note()

        val result = addNoteImages(note, emptyList())

        assertEquals(AppResult.Success(AddedImages(note, failedCount = 0)), result)
        assertTrue(imageStorage.savedUris.isEmpty())
        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.addedImages.isEmpty())
    }

    @Test
    fun `given existing note when adding images then saves it with current time and attaches the copies`() = runTest {
        val previous = NoteImage(id = 1, path = "/previa.jpg")
        val note = note(id = 7, images = listOf(previous))

        val result = addNoteImages(note, listOf(URI_A, URI_B))

        assertEquals(listOf(note.copy(updatedAt = NOW)), repository.saved)
        assertEquals(listOf(7L to listOf(copyOf(URI_A), copyOf(URI_B))), repository.addedImages)
        assertEquals(
            AppResult.Success(
                AddedImages(
                    note = note.copy(
                        updatedAt = NOW,
                        images = listOf(previous, NoteImage(500, copyOf(URI_A)), NoteImage(501, copyOf(URI_B))),
                    ),
                    failedCount = 0,
                ),
            ),
            result,
        )
    }

    @Test
    fun `given new note without text when adding image then creates it and returns its id`() = runTest {
        val note = note(id = Note.NEW_ID, title = "", content = "")

        val result = addNoteImages(note, listOf(URI_A))

        assertEquals(listOf(note.copy(updatedAt = NOW)), repository.saved)
        assertEquals(listOf(100L to listOf(copyOf(URI_A))), repository.addedImages)
        assertEquals(
            AppResult.Success(
                AddedImages(
                    note = note.copy(id = 100, updatedAt = NOW, images = listOf(NoteImage(500, copyOf(URI_A)))),
                    failedCount = 0,
                ),
            ),
            result,
        )
    }

    @Test
    fun `given some uris unavailable when adding then attaches only the copied ones`() = runTest {
        imageStorage.unavailableUris += URI_A

        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B))

        assertEquals(listOf(7L to listOf(copyOf(URI_B))), repository.addedImages)
        assertEquals(listOf(NoteImage(500, copyOf(URI_B))), (result as AppResult.Success).data.note.images)
    }

    @Test
    fun `given some uris unavailable when adding then reports how many failed`() = runTest {
        imageStorage.unavailableUris += listOf(URI_A, URI_C)

        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B, URI_C))

        assertEquals(2, (result as AppResult.Success).data.failedCount)
    }

    @Test
    fun `given every uri copied when adding then failed count is zero`() = runTest {
        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B))

        assertEquals(0, (result as AppResult.Success).data.failedCount)
    }

    @Test
    fun `given no uri can be copied when adding then returns image unavailable without saving`() = runTest {
        imageStorage.unavailableUris += listOf(URI_A, URI_B)

        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B))

        assertEquals(AppResult.Error(NoteError.ImageUnavailable), result)
        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.addedImages.isEmpty())
    }

    @Test
    fun `given save failure when adding then returns the error and deletes the copies`() = runTest {
        repository.failure = NoteError.Storage

        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B))

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(repository.addedImages.isEmpty())
        assertEquals(listOf(copyOf(URI_A), copyOf(URI_B)), imageStorage.deletedPaths)
    }

    @Test
    fun `given deleted note when adding then returns not found and deletes the copies`() = runTest {
        repository.failure = NoteError.NotFound

        val result = addNoteImages(note(id = 7), listOf(URI_A))

        assertEquals(AppResult.Error(NoteError.NotFound), result)
        assertEquals(listOf(copyOf(URI_A)), imageStorage.deletedPaths)
    }

    @Test
    fun `given add images failure when adding then returns the error and deletes the copies`() = runTest {
        repository.addImagesFailure = NoteError.Storage

        val result = addNoteImages(note(id = 7), listOf(URI_A, URI_B))

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertEquals(listOf(copyOf(URI_A), copyOf(URI_B)), imageStorage.deletedPaths)
    }

    @Test
    fun `given images added successfully when adding then deletes no copies`() = runTest {
        addNoteImages(note(id = 7), listOf(URI_A))

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    private fun note(
        id: Long = 7,
        title: String = "Escapada",
        content: String = "La sierra",
        images: List<NoteImage> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = NoteColor.LAVENDER,
        isPinned = true,
        category = NoteCategory.PERSONAL,
        updatedAt = 0,
        images = images,
    )

    private companion object {
        const val NOW = 1_000L
        const val URI_A = "content://media/picker/a"
        const val URI_B = "content://media/picker/b"
        const val URI_C = "content://media/picker/c"
    }
}
