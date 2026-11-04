package com.example.notesapp.data.settings

import com.example.notesapp.domain.notes.NoteColor
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

    @Test
    fun `given each color when mapping to storage then value is its stable text`() {
        assertEquals(colorsByStorageValue, NoteColor.entries.associateBy { it.toStorageValue() })
    }

    @Test
    fun `given each stable text when mapping to color then is its color`() {
        colorsByStorageValue.forEach { (storageValue, color) ->
            assertEquals(color, storageValue.toNoteColorOrNull())
        }
    }

    @Test
    fun `given each color when mapping to storage and back then is unchanged`() {
        NoteColor.entries.forEach { color ->
            assertEquals(color, color.toStorageValue().toNoteColorOrNull())
        }
    }

    @Test
    fun `given no value when mapping to color then is null`() {
        assertNull((null as String?).toNoteColorOrNull())
    }

    @Test
    fun `given unknown text when mapping to color then is null`() {
        assertNull("turquoise".toNoteColorOrNull())
    }

    @Test
    fun `given empty text when mapping to color then is null`() {
        assertNull("".toNoteColorOrNull())
    }

    @Test
    fun `given enum name when mapping to color then is null`() {
        assertNull("AQUA".toNoteColorOrNull())
    }

    private companion object {
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
