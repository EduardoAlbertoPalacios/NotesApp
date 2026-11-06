package com.example.notesapp.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.ThemeMode
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryImplTest {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val defaultNoteColorKey = stringPreferencesKey("default_note_color")
    private val notesViewKey = stringPreferencesKey("notes_view")

    private fun dataStore(storedValue: String? = null) = FakePreferencesDataStore(
        storedValue?.let { preferencesOf(themeModeKey to it) } ?: emptyPreferences(),
    )

    private fun colorDataStore(storedValue: String? = null) = FakePreferencesDataStore(
        storedValue?.let { preferencesOf(defaultNoteColorKey to it) } ?: emptyPreferences(),
    )

    private fun viewDataStore(storedValue: String? = null) = FakePreferencesDataStore(
        storedValue?.let { preferencesOf(notesViewKey to it) } ?: emptyPreferences(),
    )

    @Test
    fun `given no stored value when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(dataStore())

        repository.observeThemeMode().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given stored value when observing then emits its theme`() = runTest {
        val repository = SettingsRepositoryImpl(dataStore(storedValue = "light"))

        repository.observeThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `given observed theme when saving then stores its value and emits the new theme`() = runTest {
        val dataStore = dataStore(storedValue = "dark")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())

            val result = repository.setThemeMode(ThemeMode.LIGHT)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(ThemeMode.LIGHT, awaitItem())
            assertEquals("light", dataStore.preferences.value[themeModeKey])
        }
    }

    @Test
    fun `given unknown stored value when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(dataStore(storedValue = "sepia"))

        repository.observeThemeMode().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given io failure when reading then emits null`() = runTest {
        val dataStore = dataStore(storedValue = "light")
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given unexpected failure when reading then propagates it`() = runTest {
        val dataStore = dataStore(storedValue = "light")
        dataStore.readFailure = IllegalStateException("bug")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertTrue(awaitError() is IllegalStateException)
        }
    }

    @Test
    fun `given io failure when reading and a later save succeeds then emits the saved theme`() = runTest {
        val dataStore = dataStore()
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertEquals(null, awaitItem())
            dataStore.readFailure = null

            val result = repository.setThemeMode(ThemeMode.LIGHT)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `given io failure when saving then returns storage error and keeps emitting the chosen theme`() = runTest {
        val dataStore = dataStore(storedValue = "dark")
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())

            val result = repository.setThemeMode(ThemeMode.LIGHT)

            assertEquals(AppResult.Error(SettingsError.Storage), result)
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
        repository.observeThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
        assertEquals("dark", dataStore.preferences.value[themeModeKey])
    }

    @Test
    fun `given failed save when saving another theme succeeds then emits that theme`() = runTest {
        val dataStore = dataStore()
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeThemeMode().test {
            assertEquals(null, awaitItem())
            repository.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem())
            dataStore.writeFailure = null

            val result = repository.setThemeMode(ThemeMode.DARK)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(ThemeMode.DARK, awaitItem())
            assertEquals("dark", dataStore.preferences.value[themeModeKey])
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `given unexpected failure when saving then propagates it`() = runTest {
        val dataStore = dataStore()
        dataStore.writeFailure = IllegalStateException("bug")

        SettingsRepositoryImpl(dataStore).setThemeMode(ThemeMode.LIGHT)
    }

    @Test
    fun `given no stored color when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(colorDataStore())

        repository.observeDefaultNoteColor().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given each stored text when observing then emits its color`() = runTest {
        colorsByStorageValue.forEach { (storageValue, color) ->
            val repository = SettingsRepositoryImpl(colorDataStore(storedValue = storageValue))

            repository.observeDefaultNoteColor().test {
                assertEquals(color, awaitItem())
            }
        }
    }

    @Test
    fun `given each color when saving then stores its stable text`() = runTest {
        colorsByStorageValue.forEach { (storageValue, color) ->
            val dataStore = colorDataStore()

            val result = SettingsRepositoryImpl(dataStore).setDefaultNoteColor(color)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(storageValue, dataStore.preferences.value[defaultNoteColorKey])
        }
    }

    @Test
    fun `given observed color when saving then stores its value and emits the new color`() = runTest {
        val dataStore = colorDataStore(storedValue = "aqua")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertEquals(NoteColor.AQUA, awaitItem())

            val result = repository.setDefaultNoteColor(NoteColor.MINT)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NoteColor.MINT, awaitItem())
            assertEquals("mint", dataStore.preferences.value[defaultNoteColorKey])
        }
    }

    @Test
    fun `given saved color when the app reopens then emits that color`() = runTest {
        val dataStore = colorDataStore()
        SettingsRepositoryImpl(dataStore).setDefaultNoteColor(NoteColor.NONE)

        val reopened = SettingsRepositoryImpl(dataStore)

        reopened.observeDefaultNoteColor().test {
            assertEquals(NoteColor.NONE, awaitItem())
        }
    }

    @Test
    fun `given unknown stored color when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(colorDataStore(storedValue = "turquoise"))

        repository.observeDefaultNoteColor().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given io failure when reading color then emits null`() = runTest {
        val dataStore = colorDataStore(storedValue = "mint")
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given unexpected failure when reading color then propagates it`() = runTest {
        val dataStore = colorDataStore(storedValue = "mint")
        dataStore.readFailure = IllegalStateException("bug")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertTrue(awaitError() is IllegalStateException)
        }
    }

    @Test
    fun `given io failure when reading color and a later save succeeds then emits the saved color`() = runTest {
        val dataStore = colorDataStore()
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertEquals(null, awaitItem())
            dataStore.readFailure = null

            val result = repository.setDefaultNoteColor(NoteColor.MINT)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NoteColor.MINT, awaitItem())
        }
    }

    @Test
    fun `given io failure when saving color then returns storage error and keeps emitting the chosen color`() = runTest {
        val dataStore = colorDataStore(storedValue = "aqua")
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertEquals(NoteColor.AQUA, awaitItem())

            val result = repository.setDefaultNoteColor(NoteColor.MINT)

            assertEquals(AppResult.Error(SettingsError.Storage), result)
            assertEquals(NoteColor.MINT, awaitItem())
        }
        repository.observeDefaultNoteColor().test {
            assertEquals(NoteColor.MINT, awaitItem())
        }
        assertEquals("aqua", dataStore.preferences.value[defaultNoteColorKey])
    }

    @Test
    fun `given failed color save when the app reopens then emits the last stored color`() = runTest {
        val dataStore = colorDataStore(storedValue = "rose")
        dataStore.writeFailure = IOException("disk full")
        SettingsRepositoryImpl(dataStore).setDefaultNoteColor(NoteColor.MINT)

        val reopened = SettingsRepositoryImpl(dataStore)

        reopened.observeDefaultNoteColor().test {
            assertEquals(NoteColor.ROSE, awaitItem())
        }
    }

    @Test
    fun `given failed color save when saving another color succeeds then emits that color`() = runTest {
        val dataStore = colorDataStore()
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeDefaultNoteColor().test {
            assertEquals(null, awaitItem())
            repository.setDefaultNoteColor(NoteColor.MINT)
            assertEquals(NoteColor.MINT, awaitItem())
            dataStore.writeFailure = null

            val result = repository.setDefaultNoteColor(NoteColor.SAND)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NoteColor.SAND, awaitItem())
            assertEquals("sand", dataStore.preferences.value[defaultNoteColorKey])
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `given unexpected failure when saving color then propagates it`() = runTest {
        val dataStore = colorDataStore()
        dataStore.writeFailure = IllegalStateException("bug")

        SettingsRepositoryImpl(dataStore).setDefaultNoteColor(NoteColor.MINT)
    }

    @Test
    fun `given stored theme when saving color then theme is unchanged`() = runTest {
        val dataStore = dataStore(storedValue = "light")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.setDefaultNoteColor(NoteColor.MINT)

        repository.observeThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
        assertEquals("light", dataStore.preferences.value[themeModeKey])
    }

    @Test
    fun `given stored color when saving theme then color is unchanged`() = runTest {
        val dataStore = colorDataStore(storedValue = "mint")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.setThemeMode(ThemeMode.LIGHT)

        repository.observeDefaultNoteColor().test {
            assertEquals(NoteColor.MINT, awaitItem())
        }
        assertEquals("mint", dataStore.preferences.value[defaultNoteColorKey])
    }

    @Test
    fun `given no stored view when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(viewDataStore())

        repository.observeNotesView().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given each stored text when observing then emits its view`() = runTest {
        viewsByStorageValue.forEach { (storageValue, view) ->
            val repository = SettingsRepositoryImpl(viewDataStore(storedValue = storageValue))

            repository.observeNotesView().test {
                assertEquals(view, awaitItem())
            }
        }
    }

    @Test
    fun `given each view when saving then stores its stable text`() = runTest {
        viewsByStorageValue.forEach { (storageValue, view) ->
            val dataStore = viewDataStore()

            val result = SettingsRepositoryImpl(dataStore).setNotesView(view)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(storageValue, dataStore.preferences.value[notesViewKey])
        }
    }

    @Test
    fun `given observed view when saving then stores its value and emits the new view`() = runTest {
        val dataStore = viewDataStore(storedValue = "grid")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertEquals(NotesView.GRID, awaitItem())

            val result = repository.setNotesView(NotesView.LIST)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NotesView.LIST, awaitItem())
            assertEquals("list", dataStore.preferences.value[notesViewKey])
        }
    }

    @Test
    fun `given saved view when the app reopens then emits that view`() = runTest {
        val dataStore = viewDataStore()
        SettingsRepositoryImpl(dataStore).setNotesView(NotesView.LIST)

        val reopened = SettingsRepositoryImpl(dataStore)

        reopened.observeNotesView().test {
            assertEquals(NotesView.LIST, awaitItem())
        }
    }

    @Test
    fun `given unknown stored view when observing then emits null`() = runTest {
        val repository = SettingsRepositoryImpl(viewDataStore(storedValue = "mosaic"))

        repository.observeNotesView().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given io failure when reading view then emits null`() = runTest {
        val dataStore = viewDataStore(storedValue = "list")
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `given unexpected failure when reading view then propagates it`() = runTest {
        val dataStore = viewDataStore(storedValue = "list")
        dataStore.readFailure = IllegalStateException("bug")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertTrue(awaitError() is IllegalStateException)
        }
    }

    @Test
    fun `given io failure when reading view and a later save succeeds then emits the saved view`() = runTest {
        val dataStore = viewDataStore()
        dataStore.readFailure = IOException("disk failure")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertEquals(null, awaitItem())
            dataStore.readFailure = null

            val result = repository.setNotesView(NotesView.LIST)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NotesView.LIST, awaitItem())
        }
    }

    @Test
    fun `given io failure when saving view then returns storage error and keeps emitting the chosen view`() = runTest {
        val dataStore = viewDataStore(storedValue = "grid")
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertEquals(NotesView.GRID, awaitItem())

            val result = repository.setNotesView(NotesView.LIST)

            assertEquals(AppResult.Error(SettingsError.Storage), result)
            assertEquals(NotesView.LIST, awaitItem())
        }
        repository.observeNotesView().test {
            assertEquals(NotesView.LIST, awaitItem())
        }
        assertEquals("grid", dataStore.preferences.value[notesViewKey])
    }

    @Test
    fun `given failed view save when the app reopens then emits the last stored view`() = runTest {
        val dataStore = viewDataStore(storedValue = "grid")
        dataStore.writeFailure = IOException("disk full")
        SettingsRepositoryImpl(dataStore).setNotesView(NotesView.LIST)

        val reopened = SettingsRepositoryImpl(dataStore)

        reopened.observeNotesView().test {
            assertEquals(NotesView.GRID, awaitItem())
        }
    }

    @Test
    fun `given failed view save when saving another view succeeds then emits that view`() = runTest {
        val dataStore = viewDataStore()
        dataStore.writeFailure = IOException("disk full")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.observeNotesView().test {
            assertEquals(null, awaitItem())
            repository.setNotesView(NotesView.LIST)
            assertEquals(NotesView.LIST, awaitItem())
            dataStore.writeFailure = null

            val result = repository.setNotesView(NotesView.GRID)

            assertEquals(AppResult.Success(Unit), result)
            assertEquals(NotesView.GRID, awaitItem())
            assertEquals("grid", dataStore.preferences.value[notesViewKey])
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `given unexpected failure when saving view then propagates it`() = runTest {
        val dataStore = viewDataStore()
        dataStore.writeFailure = IllegalStateException("bug")

        SettingsRepositoryImpl(dataStore).setNotesView(NotesView.LIST)
    }

    @Test
    fun `given stored theme and color when saving view then theme and color are unchanged`() = runTest {
        val dataStore = FakePreferencesDataStore(
            preferencesOf(themeModeKey to "light", defaultNoteColorKey to "mint"),
        )
        val repository = SettingsRepositoryImpl(dataStore)

        repository.setNotesView(NotesView.LIST)

        repository.observeThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
        repository.observeDefaultNoteColor().test {
            assertEquals(NoteColor.MINT, awaitItem())
        }
        assertEquals("light", dataStore.preferences.value[themeModeKey])
        assertEquals("mint", dataStore.preferences.value[defaultNoteColorKey])
    }

    @Test
    fun `given stored view when saving theme and color then view is unchanged`() = runTest {
        val dataStore = viewDataStore(storedValue = "list")
        val repository = SettingsRepositoryImpl(dataStore)

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setDefaultNoteColor(NoteColor.MINT)

        repository.observeNotesView().test {
            assertEquals(NotesView.LIST, awaitItem())
        }
        assertEquals("list", dataStore.preferences.value[notesViewKey])
    }

    private companion object {
        val viewsByStorageValue = mapOf(
            "grid" to NotesView.GRID,
            "list" to NotesView.LIST,
        )

        val colorsByStorageValue = mapOf(
            "none" to NoteColor.NONE,
            "aqua" to NoteColor.AQUA,
            "rose" to NoteColor.ROSE,
            "vanilla" to NoteColor.VANILLA,
            "lavender" to NoteColor.LAVENDER,
            "mint" to NoteColor.MINT,
            "peach" to NoteColor.PEACH,
            "sand" to NoteColor.SAND,
        )
    }
}
