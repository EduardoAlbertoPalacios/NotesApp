package com.example.notesapp.domain.notes

import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.common.result.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetNotesUseCaseTest {

    private val work = note(id = 10, updatedAt = 10, category = NoteCategory.WORK)
    private val personal = note(id = 11, updatedAt = 20, category = NoteCategory.PERSONAL)
    private val uncategorized = note(id = 12, updatedAt = 30, category = null)
    private val mixedNotes = listOf(work, personal, uncategorized)

    @Test
    fun `given unordered notes when invoked then emits pinned first and newest edit first`() = runTest {
        val oldPinned = note(id = 1, isPinned = true, updatedAt = 100)
        val newUnpinned = note(id = 2, isPinned = false, updatedAt = 300)
        val newPinned = note(id = 3, isPinned = true, updatedAt = 200)
        val oldUnpinned = note(id = 4, isPinned = false, updatedAt = 50)
        val repository = FakeNoteRepository(
            AppResult.Success(listOf(oldPinned, newUnpinned, newPinned, oldUnpinned)),
        )

        notesOf(repository).test {
            assertEquals(
                AppResult.Success(listOf(newPinned, oldPinned, newUnpinned, oldUnpinned)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `given repository updates when invoked then emits each new list`() = runTest {
        val repository = FakeNoteRepository()
        val note = note(id = 1)

        notesOf(repository).test {
            assertEquals(AppResult.Success(emptyList<Note>()), awaitItem())
            repository.notes.value = AppResult.Success(listOf(note))
            assertEquals(AppResult.Success(listOf(note)), awaitItem())
        }
    }

    @Test
    fun `given storage error when invoked then emits the error`() = runTest {
        val repository = FakeNoteRepository(AppResult.Error(NoteError.Storage))

        notesOf(repository).test {
            assertEquals(AppResult.Error(NoteError.Storage), awaitItem())
        }
    }

    @Test
    fun `given work category when invoked then emits only work notes`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        notesOf(repository, NoteCategory.WORK).test {
            assertEquals(AppResult.Success(listOf(work)), awaitItem())
        }
    }

    @Test
    fun `given personal category when invoked then emits only personal notes`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        notesOf(repository, NoteCategory.PERSONAL).test {
            assertEquals(AppResult.Success(listOf(personal)), awaitItem())
        }
    }

    @Test
    fun `given null category when invoked then emits every note including uncategorized`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        notesOf(repository, null).test {
            assertEquals(AppResult.Success(listOf(uncategorized, personal, work)), awaitItem())
        }
    }

    @Test
    fun `given only uncategorized notes when filtering by category then emits empty list`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(listOf(uncategorized)))

        notesOf(repository, NoteCategory.WORK).test {
            assertEquals(AppResult.Success(emptyList<Note>()), awaitItem())
        }
    }

    @Test
    fun `given category filter when invoked then keeps pinned first and newest edit first`() = runTest {
        val oldPinned = note(id = 1, isPinned = true, updatedAt = 100, category = NoteCategory.WORK)
        val newUnpinned = note(id = 2, isPinned = false, updatedAt = 300, category = NoteCategory.WORK)
        val otherCategory = note(id = 3, isPinned = true, updatedAt = 400, category = NoteCategory.PERSONAL)
        val newPinned = note(id = 4, isPinned = true, updatedAt = 200, category = NoteCategory.WORK)
        val repository = FakeNoteRepository(
            AppResult.Success(listOf(oldPinned, newUnpinned, otherCategory, newPinned)),
        )

        notesOf(repository, NoteCategory.WORK).test {
            assertEquals(AppResult.Success(listOf(newPinned, oldPinned, newUnpinned)), awaitItem())
        }
    }

    @Test
    fun `given empty query when invoked with category then filters by category`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        filteredOf(repository, NoteCategory.WORK, query = "").test {
            assertEquals(AppResult.Success(FilteredNotes(listOf(work), totalCount = 3)), awaitItem())
        }
    }

    @Test
    fun `given blank query when invoked with category then filters by category`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        filteredOf(repository, NoteCategory.PERSONAL, query = "   ").test {
            assertEquals(AppResult.Success(FilteredNotes(listOf(personal), totalCount = 3)), awaitItem())
        }
    }

    @Test
    fun `given query and work category when invoked then ignores category and returns matches from any category`() =
        runTest {
            val workMeeting = note(id = 1, updatedAt = 10, category = NoteCategory.WORK, title = "Compras")
            val personalMeeting = note(id = 2, updatedAt = 20, category = NoteCategory.PERSONAL, title = "Reunión familiar")
            val uncategorizedMeeting = note(id = 3, updatedAt = 30, category = null, content = "Preparar la reunión")
            val repository = FakeNoteRepository(
                AppResult.Success(listOf(workMeeting, personalMeeting, uncategorizedMeeting)),
            )

            notesOf(repository, NoteCategory.WORK, query = "reunion").test {
                assertEquals(AppResult.Success(listOf(uncategorizedMeeting, personalMeeting)), awaitItem())
            }
        }

    @Test
    fun `given query when invoked then returns only notes matching flexibly`() = runTest {
        val meeting = note(id = 1, updatedAt = 10, title = "Reunión de equipo")
        val shopping = note(id = 2, updatedAt = 20, title = "Compras", content = "Leche y pan")
        val repository = FakeNoteRepository(AppResult.Success(listOf(meeting, shopping)))

        notesOf(repository, query = "reunion").test {
            assertEquals(AppResult.Success(listOf(meeting)), awaitItem())
        }
    }

    @Test
    fun `given content with bold span over part of a word when searching then finds it with spans intact`() =
        runTest {
            val spans = listOf(FormatSpan(start = 0, end = 3, format = TextFormat.BOLD))
            val formatted = note(id = 1, updatedAt = 10, title = "Agenda", content = "reunión", contentSpans = spans)
            val other = note(id = 2, updatedAt = 20, title = "Compras", content = "Leche y pan")
            val repository = FakeNoteRepository(AppResult.Success(listOf(formatted, other)))

            filteredOf(repository, query = "reunion").test {
                val result = awaitItem()

                assertEquals(AppResult.Success(FilteredNotes(listOf(formatted), totalCount = 2)), result)
                assertEquals(spans, (result as AppResult.Success).data.notes.single().contentSpans)
            }
        }

    @Test
    fun `given query matching a task text when searching then finds the note with its tasks`() = runTest {
        val tasks = listOf(ChecklistItem(line = 0, isDone = false), ChecklistItem(line = 1, isDone = true))
        val shopping = note(id = 1, updatedAt = 10, title = "La compra", content = "Tomates\nCafé", contentTasks = tasks)
        val other = note(id = 2, updatedAt = 20, title = "Ideas", content = "Viaje")
        val repository = FakeNoteRepository(AppResult.Success(listOf(shopping, other)))

        filteredOf(repository, query = "cafe").test {
            assertEquals(AppResult.Success(FilteredNotes(listOf(shopping), totalCount = 2)), awaitItem())
        }
    }

    @Test
    fun `given query when invoked then keeps pinned first and newest edit first in results`() = runTest {
        val oldPinned = note(id = 1, isPinned = true, updatedAt = 100, title = "Viaje a Lima")
        val newUnpinned = note(id = 2, isPinned = false, updatedAt = 300, title = "Viaje a Cusco")
        val newPinned = note(id = 3, isPinned = true, updatedAt = 200, content = "Planear el viaje")
        val oldUnpinned = note(id = 4, isPinned = false, updatedAt = 50, title = "VIAJE corto")
        val newestNotMatching = note(id = 5, isPinned = true, updatedAt = 900, title = "Compras")
        val repository = FakeNoteRepository(
            AppResult.Success(listOf(oldUnpinned, oldPinned, newestNotMatching, newUnpinned, newPinned)),
        )

        notesOf(repository, query = "viaje").test {
            assertEquals(
                AppResult.Success(listOf(newPinned, oldPinned, newUnpinned, oldUnpinned)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `given notes when invoked without filter then total count is every note`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        totalCountOf(repository).test {
            assertEquals(AppResult.Success(3), awaitItem())
        }
    }

    @Test
    fun `given notes when invoked with category filter then total count is every note`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        totalCountOf(repository, NoteCategory.WORK).test {
            assertEquals(AppResult.Success(3), awaitItem())
        }
    }

    @Test
    fun `given notes when searching with results then total count is every note`() = runTest {
        val matching = note(id = 1, title = "Reunión")
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes + matching))

        filteredOf(repository, query = "reunion").test {
            assertEquals(AppResult.Success(FilteredNotes(listOf(matching), totalCount = 4)), awaitItem())
        }
    }

    @Test
    fun `given notes when searching without results then total count is every note`() = runTest {
        val repository = FakeNoteRepository(AppResult.Success(mixedNotes))

        filteredOf(repository, NoteCategory.WORK, query = "inexistente").test {
            assertEquals(AppResult.Success(FilteredNotes(emptyList(), totalCount = 3)), awaitItem())
        }
    }

    @Test
    fun `given empty repository when invoked then emits empty notes and zero total count`() = runTest {
        val repository = FakeNoteRepository()

        filteredOf(repository).test {
            assertEquals(AppResult.Success(FilteredNotes(emptyList(), totalCount = 0)), awaitItem())
        }
    }

    @Test
    fun `given empty repository when searching then emits empty notes and zero total count`() = runTest {
        val repository = FakeNoteRepository()

        filteredOf(repository, NoteCategory.WORK, query = "reunion").test {
            assertEquals(AppResult.Success(FilteredNotes(emptyList(), totalCount = 0)), awaitItem())
        }
    }

    @Test
    fun `given storage error when searching then emits the error`() = runTest {
        val repository = FakeNoteRepository(AppResult.Error(NoteError.Storage))

        filteredOf(repository, NoteCategory.WORK, query = "reunion").test {
            assertEquals(AppResult.Error(NoteError.Storage), awaitItem())
        }
    }

    @Test
    fun `given active search when a note is edited then emits recalculated results`() = runTest {
        val meeting = note(id = 1, updatedAt = 10, title = "Reunión")
        val shopping = note(id = 2, updatedAt = 20, title = "Compras")
        val repository = FakeNoteRepository(AppResult.Success(listOf(meeting, shopping)))

        filteredOf(repository, query = "reunion").test {
            assertEquals(AppResult.Success(FilteredNotes(listOf(meeting), totalCount = 2)), awaitItem())

            val editedMeeting = meeting.copy(title = "Llamada", updatedAt = 30)
            val editedShopping = shopping.copy(content = "Comprar café para la reunión", updatedAt = 40)
            repository.notes.value = AppResult.Success(listOf(editedMeeting, editedShopping))

            assertEquals(AppResult.Success(FilteredNotes(listOf(editedShopping), totalCount = 2)), awaitItem())
        }
    }

    @Test
    fun `given active search when a matching note is deleted then emits results without it and updated total`() =
        runTest {
            val meeting = note(id = 1, updatedAt = 10, title = "Reunión")
            val shopping = note(id = 2, updatedAt = 20, title = "Compras")
            val repository = FakeNoteRepository(AppResult.Success(listOf(meeting, shopping)))

            filteredOf(repository, query = "reunion").test {
                assertEquals(AppResult.Success(FilteredNotes(listOf(meeting), totalCount = 2)), awaitItem())

                repository.notes.value = AppResult.Success(listOf(shopping))

                assertEquals(AppResult.Success(FilteredNotes(emptyList(), totalCount = 1)), awaitItem())
            }
        }

    private fun TestScope.notesOf(
        repository: NoteRepository,
        category: NoteCategory? = null,
        query: String = "",
    ): Flow<AppResult<List<Note>, NoteError>> =
        filteredOf(repository, category, query).map { result -> result.map { it.notes } }

    private fun TestScope.totalCountOf(
        repository: NoteRepository,
        category: NoteCategory? = null,
        query: String = "",
    ): Flow<AppResult<Int, NoteError>> =
        filteredOf(repository, category, query).map { result -> result.map { it.totalCount } }

    private fun TestScope.filteredOf(
        repository: NoteRepository,
        category: NoteCategory? = null,
        query: String = "",
    ): Flow<AppResult<FilteredNotes, NoteError>> =
        GetNotesUseCase(repository, UnconfinedTestDispatcher(testScheduler))(category, query)

    private fun note(
        id: Long,
        isPinned: Boolean = false,
        updatedAt: Long = 0,
        category: NoteCategory? = null,
        title: String = "Nota $id",
        content: String = "Contenido",
        contentSpans: List<FormatSpan> = emptyList(),
        contentTasks: List<ChecklistItem> = emptyList(),
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = NoteColor.AQUA,
        isPinned = isPinned,
        category = category,
        updatedAt = updatedAt,
        contentSpans = contentSpans,
        contentTasks = contentTasks,
    )
}
