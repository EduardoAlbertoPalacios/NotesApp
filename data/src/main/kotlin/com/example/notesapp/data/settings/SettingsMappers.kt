package com.example.notesapp.data.settings

import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.ThemeMode

private const val THEME_LIGHT = "light"
private const val THEME_DARK = "dark"
private const val COLOR_NONE = "none"
private const val COLOR_AQUA = "aqua"
private const val COLOR_ROSE = "rose"
private const val COLOR_VANILLA = "vanilla"
private const val COLOR_LAVENDER = "lavender"
private const val COLOR_MINT = "mint"
private const val COLOR_PEACH = "peach"
private const val COLOR_SAND = "sand"
private const val VIEW_GRID = "grid"
private const val VIEW_LIST = "list"

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

/** Valor guardado: estable aunque cambie el nombre del enum. */
internal fun NoteColor.toStorageValue(): String = when (this) {
    NoteColor.NONE -> COLOR_NONE
    NoteColor.AQUA -> COLOR_AQUA
    NoteColor.ROSE -> COLOR_ROSE
    NoteColor.VANILLA -> COLOR_VANILLA
    NoteColor.LAVENDER -> COLOR_LAVENDER
    NoteColor.MINT -> COLOR_MINT
    NoteColor.PEACH -> COLOR_PEACH
    NoteColor.SAND -> COLOR_SAND
}

/** `null` si no hay valor o no es uno conocido. */
internal fun String?.toNoteColorOrNull(): NoteColor? = when (this) {
    COLOR_NONE -> NoteColor.NONE
    COLOR_AQUA -> NoteColor.AQUA
    COLOR_ROSE -> NoteColor.ROSE
    COLOR_VANILLA -> NoteColor.VANILLA
    COLOR_LAVENDER -> NoteColor.LAVENDER
    COLOR_MINT -> NoteColor.MINT
    COLOR_PEACH -> NoteColor.PEACH
    COLOR_SAND -> NoteColor.SAND
    else -> null
}

/** Valor guardado: estable aunque cambie el nombre del enum. */
internal fun NotesView.toStorageValue(): String = when (this) {
    NotesView.GRID -> VIEW_GRID
    NotesView.LIST -> VIEW_LIST
}

/** `null` si no hay valor o no es uno conocido. */
internal fun String?.toNotesViewOrNull(): NotesView? = when (this) {
    VIEW_GRID -> NotesView.GRID
    VIEW_LIST -> NotesView.LIST
    else -> null
}
