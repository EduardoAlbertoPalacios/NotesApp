package com.example.notesapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notesapp.common.result.AppResult
import com.example.notesapp.domain.notes.NoteColor
import com.example.notesapp.domain.settings.NotesView
import com.example.notesapp.domain.settings.SettingsError
import com.example.notesapp.domain.settings.SettingsRepository
import com.example.notesapp.domain.settings.ThemeMode
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Singleton porque lo elegido en la sesión ([chosenThemeMode] y los demás) debe ser lo mismo para toda ella. */
@Singleton
internal class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    /**
     * Último tema elegido en esta sesión; tiene prioridad sobre lo leído. Así se aplica hasta cerrar la app
     * aunque no se pueda guardar, y también si una lectura fallida ya terminó de observar el archivo.
     */
    private val chosenThemeMode = MutableStateFlow<ThemeMode?>(null)

    /** Último color predeterminado elegido en esta sesión; tiene prioridad sobre lo leído, como [chosenThemeMode]. */
    private val chosenDefaultNoteColor = MutableStateFlow<NoteColor?>(null)

    /** Última vista elegida en esta sesión; tiene prioridad sobre lo leído, como [chosenThemeMode]. */
    private val chosenNotesView = MutableStateFlow<NotesView?>(null)

    /** Último estado del guardado automático elegido en esta sesión; tiene prioridad sobre lo leído. */
    private val chosenAutoSaveEnabled = MutableStateFlow<Boolean?>(null)

    override fun observeThemeMode(): Flow<ThemeMode?> =
        combine(storedThemeMode(), chosenThemeMode) { stored, chosen -> chosen ?: stored }
            .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode): AppResult<Unit, SettingsError> {
        chosenThemeMode.value = mode
        return try {
            dataStore.edit { it[THEME_MODE] = mode.toStorageValue() }
            AppResult.Success(Unit)
        } catch (_: IOException) {
            AppResult.Error(SettingsError.Storage)
        }
    }

    override fun observeDefaultNoteColor(): Flow<NoteColor?> =
        combine(storedDefaultNoteColor(), chosenDefaultNoteColor) { stored, chosen -> chosen ?: stored }
            .distinctUntilChanged()

    override suspend fun setDefaultNoteColor(color: NoteColor): AppResult<Unit, SettingsError> {
        chosenDefaultNoteColor.value = color
        return try {
            dataStore.edit { it[DEFAULT_NOTE_COLOR] = color.toStorageValue() }
            AppResult.Success(Unit)
        } catch (_: IOException) {
            AppResult.Error(SettingsError.Storage)
        }
    }

    override fun observeNotesView(): Flow<NotesView?> =
        combine(storedNotesView(), chosenNotesView) { stored, chosen -> chosen ?: stored }
            .distinctUntilChanged()

    override suspend fun setNotesView(view: NotesView): AppResult<Unit, SettingsError> {
        chosenNotesView.value = view
        return try {
            dataStore.edit { it[NOTES_VIEW] = view.toStorageValue() }
            AppResult.Success(Unit)
        } catch (_: IOException) {
            AppResult.Error(SettingsError.Storage)
        }
    }

    override fun observeAutoSaveEnabled(): Flow<Boolean?> =
        combine(storedAutoSaveEnabled(), chosenAutoSaveEnabled) { stored, chosen -> chosen ?: stored }
            .distinctUntilChanged()

    override suspend fun setAutoSaveEnabled(enabled: Boolean): AppResult<Unit, SettingsError> {
        chosenAutoSaveEnabled.value = enabled
        return try {
            dataStore.edit { it[AUTO_SAVE_ENABLED] = enabled }
            AppResult.Success(Unit)
        } catch (_: IOException) {
            AppResult.Error(SettingsError.Storage)
        }
    }

    private fun storedThemeMode(): Flow<ThemeMode?> =
        dataStore.data
            .map { it[THEME_MODE].toThemeModeOrNull() }
            .catch { error -> if (error is IOException) emit(null) else throw error }

    private fun storedDefaultNoteColor(): Flow<NoteColor?> =
        dataStore.data
            .map { it[DEFAULT_NOTE_COLOR].toNoteColorOrNull() }
            .catch { error -> if (error is IOException) emit(null) else throw error }

    private fun storedNotesView(): Flow<NotesView?> =
        dataStore.data
            .map { it[NOTES_VIEW].toNotesViewOrNull() }
            .catch { error -> if (error is IOException) emit(null) else throw error }

    // Un booleano se guarda tal cual: no hace falta un valor estable propio, como con los enums.
    private fun storedAutoSaveEnabled(): Flow<Boolean?> =
        dataStore.data
            .map { it[AUTO_SAVE_ENABLED] }
            .catch { error -> if (error is IOException) emit(null) else throw error }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_NOTE_COLOR = stringPreferencesKey("default_note_color")
        val NOTES_VIEW = stringPreferencesKey("notes_view")
        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
    }
}
