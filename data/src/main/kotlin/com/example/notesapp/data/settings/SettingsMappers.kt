package com.example.notesapp.data.settings

import com.example.notesapp.domain.settings.ThemeMode

private const val THEME_LIGHT = "light"
private const val THEME_DARK = "dark"

/** Valor guardado: estable aunque cambie el nombre del enum. */
internal fun ThemeMode.toStorageValue(): String = when (this) {
    ThemeMode.LIGHT -> THEME_LIGHT
    ThemeMode.DARK -> THEME_DARK
}

/** `null` si no hay valor o no es uno conocido. */
internal fun String?.toThemeModeOrNull(): ThemeMode? = when (this) {
    THEME_LIGHT -> ThemeMode.LIGHT
    THEME_DARK -> ThemeMode.DARK
    else -> null
}
