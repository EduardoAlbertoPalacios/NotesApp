package com.example.notesapp.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.domain.notes.trash.PurgeExpiredTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Tareas de una vez al abrir la app. Lo crea `MainActivity` y sobrevive al giro, así que cada tarea se hace una
 * sola vez por apertura, se entre por la pantalla que se entre (decisión D4 del plan 012).
 */
@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val purgeExpiredTrash: PurgeExpiredTrashUseCase,
) : ViewModel() {

    init {
        // Borra las notas que cumplieron el plazo de la papelera (spec 012, RF-49). Si falla, se reintenta en la
        // próxima apertura; mientras tanto no se muestran (RF-48).
        viewModelScope.launch { purgeExpiredTrash() }
    }
}
