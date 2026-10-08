package com.example.notesapp.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notesapp.data.local.NotesDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotesDatabase::class.java,
    )

    @Test
    fun given_v1_database_with_note_when_migrating_to_v2_then_keeps_note_with_null_category() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT id, title, content, color, is_pinned, updated_at, category FROM notes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Ideas", cursor.getString(1))
            assertEquals("Libreta", cursor.getString(2))
            assertEquals("SAND", cursor.getString(3))
            assertEquals(1, cursor.getInt(4))
            assertEquals(5L, cursor.getLong(5))
            assertTrue(cursor.isNull(6))
        }
        db.close()
    }

    @Test
    fun given_v1_database_when_opened_with_room_then_reads_migrated_note() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(INSERT_V1_NOTE)
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotesDatabase::class.java,
            TEST_DB,
        ).build()
        val note = database.noteDao().getById(1)
        database.close()

        assertEquals("Ideas", note?.title)
        assertEquals(null, note?.category)
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val INSERT_V1_NOTE =
            "INSERT INTO notes (id, title, content, color, is_pinned, updated_at) " +
                "VALUES (1, 'Ideas', 'Libreta', 'SAND', 1, 5)"
    }
}
