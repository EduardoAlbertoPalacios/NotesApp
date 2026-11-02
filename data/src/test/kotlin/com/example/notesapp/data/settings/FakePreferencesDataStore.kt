package com.example.notesapp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Igual que el DataStore real, un fallo de lectura termina la recolección de [data]. */
class FakePreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {

    val preferences = MutableStateFlow(initial)
    var readFailure: Exception? = null
    var writeFailure: Exception? = null

    override val data: Flow<Preferences> = flow {
        readFailure?.let { throw it }
        emitAll(preferences)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        writeFailure?.let { throw it }
        return transform(preferences.value).also { preferences.value = it }
    }
}
