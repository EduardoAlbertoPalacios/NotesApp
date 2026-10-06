package com.example.notesapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun inserted_notes_are_observed_with_generated_ids() = runTest {
        val note = NoteEntity(title = "Ideas", content = "Libreta", color = "SAND", isPinned = false, updatedAt = 5)

        dao.insertAll(listOf(note))

        dao.observeAll().test {
            assertEquals(listOf(note.copy(id = 1)), awaitItem())
        }
    }

    @Test
    fun seed_callback_inserts_sample_notes_on_create() = runTest {
        val seeded = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
        ).addCallback(SeedNotesCallback(now = { 1_000_000_000L })).build()

        seeded.noteDao().observeAll().test {
            assertEquals(SeedNotesCallback.seedNotes(1_000_000_000L).size, awaitItem().size)
        }
        seeded.close()
    }
}
