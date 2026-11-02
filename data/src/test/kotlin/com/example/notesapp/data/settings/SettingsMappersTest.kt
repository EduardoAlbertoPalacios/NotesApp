package com.example.notesapp.data.settings

import com.example.notesapp.domain.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsMappersTest {

    @Test
    fun `given light when mapping to storage and back then is unchanged`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.LIGHT.toStorageValue().toThemeModeOrNull())
    }

    @Test
    fun `given dark when mapping to storage and back then is unchanged`() {
        assertEquals(ThemeMode.DARK, ThemeMode.DARK.toStorageValue().toThemeModeOrNull())
    }

    @Test
    fun `given light when mapping to storage then value is light`() {
        assertEquals("light", ThemeMode.LIGHT.toStorageValue())
    }

    @Test
    fun `given dark when mapping to storage then value is dark`() {
        assertEquals("dark", ThemeMode.DARK.toStorageValue())
    }

    @Test
    fun `given no value when mapping to theme then is null`() {
        assertNull((null as String?).toThemeModeOrNull())
    }

    @Test
    fun `given unknown text when mapping to theme then is null`() {
        assertNull("sepia".toThemeModeOrNull())
    }

    @Test
    fun `given empty text when mapping to theme then is null`() {
        assertNull("".toThemeModeOrNull())
    }

    @Test
    fun `given enum name when mapping to theme then is null`() {
        assertNull("LIGHT".toThemeModeOrNull())
    }
}
