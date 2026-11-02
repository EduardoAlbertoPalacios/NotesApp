package com.example.notesapp.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.data.di.createSettingsDataStore
import com.example.notesapp.domain.settings.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    /** DataStore solo admite archivos con la extensión `preferences_pb`. */
    private val settingsFile: File
        get() = File(temporaryFolder.root, "settings.preferences_pb")

    @Test
    fun given_saved_theme_when_reopening_the_file_then_reads_the_same_theme() = runTest {
        val result = withRepository { it.setThemeMode(ThemeMode.LIGHT) }

        assertEquals(AppResult.Success(Unit), result)
        withRepository { repository ->
            repository.observeThemeMode().test {
                assertEquals(ThemeMode.LIGHT, awaitItem())
            }
        }
    }

    @Test
    fun given_file_with_invalid_bytes_when_observing_then_emits_null() = runTest {
        writeInvalidBytes()

        withRepository { repository ->
            repository.observeThemeMode().test {
                assertEquals(null, awaitItem())
            }
        }
    }

    @Test
    fun given_file_with_invalid_bytes_when_saving_then_stores_the_theme() = runTest {
        writeInvalidBytes()

        val result = withRepository { it.setThemeMode(ThemeMode.LIGHT) }

        assertEquals(AppResult.Success(Unit), result)
        withRepository { repository ->
            repository.observeThemeMode().test {
                assertEquals(ThemeMode.LIGHT, awaitItem())
            }
        }
    }

    private fun writeInvalidBytes() {
        settingsFile.writeBytes(ByteArray(16) { 0xFF.toByte() })
    }

    /** Cierra el scope al terminar: no puede haber dos DataStore activos sobre el mismo archivo. */
    private suspend fun <T> TestScope.withRepository(block: suspend (SettingsRepositoryImpl) -> T): T {
        val job = Job()
        val dataStore = createSettingsDataStore(
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + job),
            produceFile = { settingsFile },
        )
        return try {
            block(SettingsRepositoryImpl(dataStore))
        } finally {
            job.cancelAndJoin()
        }
    }
}
