package com.example.notesapp.domain.settings

import com.example.notesapp.common.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetThemeModeUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val setThemeMode = SetThemeModeUseCase(repository)

    @Test
    fun `given chosen themes when setting then saves each one in the repository`() = runTest {
        val first = setThemeMode(ThemeMode.LIGHT)
        val second = setThemeMode(ThemeMode.DARK)

        assertEquals(AppResult.Success(Unit), first)
        assertEquals(AppResult.Success(Unit), second)
        assertEquals(listOf(ThemeMode.LIGHT, ThemeMode.DARK), repository.savedModes)
    }

    @Test
    fun `given storage failure when setting then returns the error`() = runTest {
        repository.failure = SettingsError.Storage

        assertEquals(AppResult.Error(SettingsError.Storage), setThemeMode(ThemeMode.LIGHT))
    }
}
