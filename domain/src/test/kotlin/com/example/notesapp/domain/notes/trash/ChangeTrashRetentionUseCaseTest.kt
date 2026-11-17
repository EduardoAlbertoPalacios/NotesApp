package com.example.notesapp.domain.notes.trash

import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeImageStorage
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.notes.NoteImage
import com.example.notesapp.domain.settings.FakeSettingsRepository
import com.example.notesapp.domain.settings.SettingsError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeTrashRetentionUseCaseTest {

    private val repository = FakeNoteRepository()
    private val settings = FakeSettingsRepository()
    private val imageStorage = FakeImageStorage()

    private fun createChangeTrashRetention() = ChangeTrashRetentionUseCase(
        noteRepository = repository,
        settingsRepository = settings,
        getTrashRetention = GetTrashRetentionUseCase(settings),
        deleteTrashedNotes = DeleteTrashedNotesUseCase(repository, imageStorage),
        timeProvider = TimeProvider { NOW },
    )

    private fun trashWithOldNote() {
        repository.trash.value = AppResult.Success(
            listOf(
                trashedNote(id = 1, trashedAt = NOW - 10 * DAY, images = listOf(NoteImage(id = 1, path = "/a.jpg"))),
                trashedNote(id = 2, trashedAt = NOW - 8 * DAY),
                trashedNote(id = 3, trashedAt = NOW - DAY),
            ),
        )
    }

    @Test
    fun `given longer retention when changing then applies it without reading the trash`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        trashWithOldNote()

        val result = createChangeTrashRetention()(TrashRetention.DAYS_90)

        assertEquals(TrashRetentionChange.Applied, result)
        assertEquals(listOf(TrashRetention.DAYS_90), settings.savedTrashRetentions)
        assertEquals(0, repository.observeTrashCalls)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given never when changing from days then applies it without reading the trash`() = runTest {
        trashWithOldNote()

        val result = createChangeTrashRetention()(TrashRetention.NEVER)

        assertEquals(TrashRetentionChange.Applied, result)
        assertEquals(listOf(TrashRetention.NEVER), settings.savedTrashRetentions)
        assertEquals(0, repository.observeTrashCalls)
    }

    @Test
    fun `given same retention when changing then applies it without deleting`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        trashWithOldNote()

        val result = createChangeTrashRetention()(TrashRetention.DAYS_7)

        assertEquals(TrashRetentionChange.Applied, result)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given shorter retention without affected notes when changing then applies it`() = runTest {
        repository.trash.value = AppResult.Success(listOf(trashedNote(id = 3, trashedAt = NOW - DAY)))

        val result = createChangeTrashRetention()(TrashRetention.DAYS_7)

        assertEquals(TrashRetentionChange.Applied, result)
        assertEquals(listOf(TrashRetention.DAYS_7), settings.savedTrashRetentions)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given shorter retention with affected notes when not confirmed then asks for confirmation without changes`() =
        runTest {
            trashWithOldNote()

            val result = createChangeTrashRetention()(TrashRetention.DAYS_7)

            assertEquals(TrashRetentionChange.NeedsConfirmation(count = 2), result)
            assertTrue(settings.savedTrashRetentions.isEmpty())
            assertEquals(null, settings.trashRetention.value)
            assertTrue(repository.deletedNoteIdBatches.isEmpty())
            assertTrue(imageStorage.deletedPaths.isEmpty())
        }

    @Test
    fun `given current never and days retention with affected notes when not confirmed then asks for confirmation`() =
        runTest {
            settings.trashRetention.value = TrashRetention.NEVER
            trashWithOldNote()

            val result = createChangeTrashRetention()(TrashRetention.DAYS_7)

            assertEquals(TrashRetentionChange.NeedsConfirmation(count = 2), result)
            assertEquals(TrashRetention.NEVER, settings.trashRetention.value)
            assertTrue(repository.deletedNoteIdBatches.isEmpty())
        }

    @Test
    fun `given shorter retention with affected notes when confirmed then saves it and deletes them with their images`() =
        runTest {
            trashWithOldNote()

            val result = createChangeTrashRetention()(TrashRetention.DAYS_7, confirmed = true)

            assertEquals(TrashRetentionChange.Applied, result)
            assertEquals(TrashRetention.DAYS_7, settings.trashRetention.value)
            assertEquals(listOf(listOf(1L, 2L)), repository.deletedNoteIdBatches)
            assertEquals(listOf("/a.jpg"), imageStorage.deletedPaths)
        }

    @Test
    fun `given save failure when confirmed then fails without deleting anything`() = runTest {
        trashWithOldNote()
        settings.trashRetentionFailure = SettingsError.Storage

        val result = createChangeTrashRetention()(TrashRetention.DAYS_7, confirmed = true)

        assertEquals(TrashRetentionChange.Failed, result)
        assertEquals(null, settings.trashRetention.value)
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun `given save failure with longer retention when changing then fails`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        settings.trashRetentionFailure = SettingsError.Storage

        val result = createChangeTrashRetention()(TrashRetention.DAYS_30)

        assertEquals(TrashRetentionChange.Failed, result)
        assertEquals(TrashRetention.DAYS_7, settings.trashRetention.value)
    }

    @Test
    fun `given trash cannot be read with shorter retention when changing then fails without saving`() = runTest {
        repository.trash.value = AppResult.Error(NoteError.Storage)

        val result = createChangeTrashRetention()(TrashRetention.DAYS_7, confirmed = true)

        assertEquals(TrashRetentionChange.Failed, result)
        assertTrue(settings.savedTrashRetentions.isEmpty())
        assertTrue(repository.deletedNoteIdBatches.isEmpty())
    }

    @Test
    fun `given delete failure after saving when confirmed then keeps the new retention applied`() = runTest {
        trashWithOldNote()
        repository.deleteNotesFailure = NoteError.Storage

        val result = createChangeTrashRetention()(TrashRetention.DAYS_7, confirmed = true)

        assertEquals(TrashRetentionChange.Applied, result)
        assertEquals(TrashRetention.DAYS_7, settings.trashRetention.value)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }
}
