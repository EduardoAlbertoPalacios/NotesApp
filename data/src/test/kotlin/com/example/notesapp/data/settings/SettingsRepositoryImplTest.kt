package com.example.notesapp.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.ThemeMode
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryImplTest {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    private fun dataStore(storedValue: String? = null) = FakePreferencesDataStore(
        storedValue?.let { preferencesOf(themeModeKey to it) } ?: emptyPreferences(),
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
}
