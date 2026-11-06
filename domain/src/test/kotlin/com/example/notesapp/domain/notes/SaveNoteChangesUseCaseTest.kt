package com.example.notesapp.domain.notes

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeNoteRepository.SavedChanges
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveNoteChangesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val imageStorage = FakeImageStorage()
    private val saveNoteChanges = SaveNoteChangesUseCase(repository, imageStorage, TimeProvider { NOW })

    @Test
    fun `given added and removed images when saving then sends both lists with the kept images`() = runTest {
        val note = note(images = listOf(IMAGE_A, IMAGE_B))

        saveNoteChanges(note, addedPaths = listOf(NEW_PATH), removedImages = listOf(IMAGE_A))

        assertEquals(
            listOf(
                SavedChanges(
                    note = note.copy(images = listOf(IMAGE_B), updatedAt = NOW),
                    addedPaths = listOf(NEW_PATH),
                    removedImageIds = listOf(IMAGE_A.id),
                ),
            ),
            repository.savedChanges,
        )
    }

    @Test
    fun `given existing note when saving then returns the saved note with its images`() = runTest {
        val note = note(images = listOf(IMAGE_A, IMAGE_B))

        val result = saveNoteChanges(note, addedPaths = listOf(NEW_PATH), removedImages = listOf(IMAGE_A))

        assertEquals(
            AppResult.Success(note.copy(updatedAt = NOW, images = listOf(IMAGE_B, NoteImage(500, NEW_PATH)))),
            result,
        )
    }

    @Test
    fun `given new note when saving then returns it with its generated id`() = runTest {
        val note = note(id = Note.NEW_ID)

        val result = saveNoteChanges(note, addedPaths = listOf(NEW_PATH), removedImages = emptyList())

        assertEquals(
            AppResult.Success(note.copy(id = 100, updatedAt = NOW, images = listOf(NoteImage(500, NEW_PATH)))),
            result,
        )
    }

    @Test
    fun `given note with old edit time when saving then uses the current time`() = runTest {
        saveNoteChanges(note(updatedAt = 5), addedPaths = emptyList(), removedImages = emptyList())

        assertEquals(NOW, repository.savedChanges.single().note.updatedAt)
    }

    @Test
    fun `given removed images when save succeeds then deletes their files`() = runTest {
        val note = note(images = listOf(IMAGE_A, IMAGE_B))

        saveNoteChanges(note, addedPaths = emptyList(), removedImages = listOf(IMAGE_A, IMAGE_B))

        assertEquals(listOf(IMAGE_A.path, IMAGE_B.path), imageStorage.deletedPaths)
    }

    @Test
    fun `given no removed images when saving then deletes no files`() = runTest {
        saveNoteChanges(note(images = listOf(IMAGE_A)), addedPaths = listOf(NEW_PATH), removedImages = emptyList())

        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given repository failure when saving then returns the error and keeps every file`() = runTest {
        repository.saveNoteChangesFailure = NoteError.Storage

        val result = saveNoteChanges(
            note(images = listOf(IMAGE_A)),
            addedPaths = listOf(NEW_PATH),
            removedImages = listOf(IMAGE_A),
        )

        assertEquals(AppResult.Error(NoteError.Storage), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given deleted note when saving then returns not found and keeps every file`() = runTest {
        repository.saveNoteChangesFailure = NoteError.NotFound

        val result = saveNoteChanges(
            note(images = listOf(IMAGE_A)),
            addedPaths = emptyList(),
            removedImages = listOf(IMAGE_A),
        )

        assertEquals(AppResult.Error(NoteError.NotFound), result)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given blank text and no images when saving then returns empty note without saving`() = runTest {
        val result = saveNoteChanges(note(title = " ", content = "\n"), addedPaths = emptyList(), removedImages = emptyList())

        assertEquals(AppResult.Error(NoteError.EmptyNote), result)
        assertTrue(repository.savedChanges.isEmpty())
    }

    @Test
    fun `given blank text and every image removed when saving then returns empty note and keeps the files`() =
        runTest {
            val note = note(title = "", content = "", images = listOf(IMAGE_A))

            val result = saveNoteChanges(note, addedPaths = emptyList(), removedImages = listOf(IMAGE_A))

            assertEquals(AppResult.Error(NoteError.EmptyNote), result)
            assertTrue(repository.savedChanges.isEmpty())
            assertTrue(imageStorage.deletedPaths.isEmpty())
        }

    @Test
    fun `given blank text with a kept image when saving then saves it`() = runTest {
        val note = note(title = "", content = "", images = listOf(IMAGE_A, IMAGE_B))

        val result = saveNoteChanges(note, addedPaths = emptyList(), removedImages = listOf(IMAGE_A))

        assertEquals(AppResult.Success(note.copy(updatedAt = NOW, images = listOf(IMAGE_B))), result)
    }

    @Test
    fun `given blank text with only an added image when saving then saves it`() = runTest {
        val note = note(id = Note.NEW_ID, title = "", content = "")

        val result = saveNoteChanges(note, addedPaths = listOf(NEW_PATH), removedImages = emptyList())

        assertEquals(
            AppResult.Success(note.copy(id = 100, updatedAt = NOW, images = listOf(NoteImage(500, NEW_PATH)))),
            result,
        )
    }

    private fun note(
        id: Long = 7,
        title: String = "Escapada",
        content: String = "La sierra",
        updatedAt: Long = 0,
        images: List<NoteImage> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = NoteColor.LAVENDER,
        isPinned = true,
        category = NoteCategory.PERSONAL,
        updatedAt = updatedAt,
        images = images,
        contentTasks = listOf(ChecklistItem(line = 0, isDone = true)),
    )

    private companion object {
        const val NOW = 1_000L
        const val NEW_PATH = "/files/note_images/nueva.jpg"
        val IMAGE_A = NoteImage(id = 1, path = "/files/note_images/a.jpg")
        val IMAGE_B = NoteImage(id = 2, path = "/files/note_images/b.jpg")
    }
}
