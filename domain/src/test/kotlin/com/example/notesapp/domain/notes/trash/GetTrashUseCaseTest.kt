package com.example.notesapp.domain.notes.trash

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.time.TimeProvider
import com.example.notesapp.domain.notes.FakeNoteRepository
import com.example.notesapp.domain.notes.NoteError
import com.example.notesapp.domain.settings.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTrashUseCaseTest {

    private val repository = FakeNoteRepository()
    private val settings = FakeSettingsRepository()
    private var now = NOW

    private fun createGetTrash() = GetTrashUseCase(
        repository = repository,
        getTrashRetention = GetTrashRetentionUseCase(settings),
        timeProvider = TimeProvider { now },
    )

    @Test
    fun `given trashed notes in any order when observing then sorts them by most recently trashed`() = runTest {
        val oldest = trashedNote(id = 1, trashedAt = NOW - 5 * DAY)
        val newest = trashedNote(id = 2, trashedAt = NOW - DAY)
        val middle = trashedNote(id = 3, trashedAt = NOW - 3 * DAY)
        repository.trash.value = AppResult.Success(listOf(oldest, newest, middle))

        createGetTrash()().test {
            val entries = (awaitItem() as AppResult.Success).data
            assertEquals(listOf(newest, middle, oldest), entries.map { it.trashed })
        }
    }

    @Test
    fun `given trashed notes when observing then includes days left with the current retention`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        val note = trashedNote(id = 1, trashedAt = NOW - 2 * DAY - 1)
        repository.trash.value = AppResult.Success(listOf(note))

        createGetTrash()().test {
            assertEquals(AppResult.Success(listOf(TrashEntry(note, daysLeft = 4))), awaitItem())
        }
    }

    @Test
    fun `given expired notes not yet purged when observing then hides them`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        val expired = trashedNote(id = 1, trashedAt = NOW - 7 * DAY)
        val alive = trashedNote(id = 2, trashedAt = NOW - 7 * DAY + 1)
        repository.trash.value = AppResult.Success(listOf(expired, alive))

        createGetTrash()().test {
            assertEquals(AppResult.Success(listOf(TrashEntry(alive, daysLeft = 0))), awaitItem())
        }
    }

    @Test
    fun `given never retention when observing then shows all notes without days left`() = runTest {
        settings.trashRetention.value = TrashRetention.NEVER
        val old = trashedNote(id = 1, trashedAt = NOW - 500 * DAY)
        val recent = trashedNote(id = 2, trashedAt = NOW - DAY)
        repository.trash.value = AppResult.Success(listOf(old, recent))

        createGetTrash()().test {
            assertEquals(
                AppResult.Success(listOf(TrashEntry(recent, daysLeft = null), TrashEntry(old, daysLeft = null))),
                awaitItem(),
            )
        }
    }

    @Test
    fun `given no stored retention when observing then uses thirty days`() = runTest {
        val note = trashedNote(id = 1, trashedAt = NOW)
        repository.trash.value = AppResult.Success(listOf(note))

        createGetTrash()().test {
            assertEquals(AppResult.Success(listOf(TrashEntry(note, daysLeft = 30))), awaitItem())
        }
    }

    @Test
    fun `given empty trash when observing then emits empty list`() = runTest {
        createGetTrash()().test {
            assertEquals(AppResult.Success(emptyList<TrashEntry>()), awaitItem())
        }
    }

    @Test
    fun `given repository error when observing then emits the error`() = runTest {
        repository.trash.value = AppResult.Error(NoteError.Storage)

        createGetTrash()().test {
            assertEquals(AppResult.Error(NoteError.Storage), awaitItem())
        }
    }

    @Test
    fun `given observed trash when retention gets shorter then hides newly expired notes`() = runTest {
        val old = trashedNote(id = 1, trashedAt = NOW - 10 * DAY)
        val recent = trashedNote(id = 2, trashedAt = NOW - DAY)
        repository.trash.value = AppResult.Success(listOf(old, recent))

        createGetTrash()().test {
            assertEquals(listOf(recent, old), (awaitItem() as AppResult.Success).data.map { it.trashed })

            settings.trashRetention.value = TrashRetention.DAYS_7
            assertEquals(AppResult.Success(listOf(TrashEntry(recent, daysLeft = 6))), awaitItem())
        }
    }

    @Test
    fun `given observed trash when it changes then recomputes days left with the time of the emission`() = runTest {
        settings.trashRetention.value = TrashRetention.DAYS_7
        val first = trashedNote(id = 1, trashedAt = NOW)
        repository.trash.value = AppResult.Success(listOf(first))

        createGetTrash()().test {
            assertEquals(AppResult.Success(listOf(TrashEntry(first, daysLeft = 7))), awaitItem())

            now = NOW + 3 * DAY
            val second = trashedNote(id = 2, trashedAt = NOW + 3 * DAY)
            repository.trash.value = AppResult.Success(listOf(first, second))
            assertEquals(
                AppResult.Success(listOf(TrashEntry(second, daysLeft = 7), TrashEntry(first, daysLeft = 4))),
                awaitItem(),
            )
        }
    }
}
