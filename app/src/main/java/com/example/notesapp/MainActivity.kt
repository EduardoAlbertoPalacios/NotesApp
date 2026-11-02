package com.example.notesapp

import android.app.UiModeManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notesapp.domain.settings.ThemeMode
import com.example.notesapp.presentation.navigation.NotesNavHost
import com.example.notesapp.presentation.settings.AppThemeViewModel
import com.example.notesapp.presentation.theme.NotesAppTheme
import dagger.hilt.android.AndroidEntryPoint

private val LegacyNavigationBarScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appThemeViewModel: AppThemeViewModel by viewModels()
    private var isThemeReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holdFirstDrawUntilThemeReady()
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            val mode = themeMode ?: return@setContent
            LaunchedEffect(mode) { applySystemBars(mode) }
            NotesAppTheme(darkTheme = mode == ThemeMode.DARK) {
                val background = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    window.setBackgroundDrawable(ColorDrawable(background))
                    isThemeReady = true
                }
                NotesNavHost(appVersion = BuildConfig.VERSION_NAME)
            }
        }
    }

    /** Evita dibujar con un tema que todavía no se ha leído. */
    private fun holdFirstDrawUntilThemeReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!isThemeReady) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }

    /**
     * Lo guarda el sistema, que así pinta la siguiente ventana inicial con este tema. Se hace al salir porque,
     * con la app en pantalla, el cambio de configuración deja las barras del sistema con el tema anterior.
     */
    override fun onStop() {
        super.onStop()
        val mode = appThemeViewModel.themeMode.value ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(UiModeManager::class.java).setApplicationNightMode(
                if (mode == ThemeMode.DARK) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO,
            )
        }
    }

    private fun applySystemBars(mode: ThemeMode) {
        val isDark = mode == ThemeMode.DARK
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle(isDark),
            navigationBarStyle = systemBarStyle(isDark),
        )
    }

    private fun systemBarStyle(isDark: Boolean): SystemBarStyle =
        if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            // Antes de API 26 los íconos de navegación siempre son claros: necesitan un velo oscuro detrás.
            SystemBarStyle.light(Color.TRANSPARENT, LegacyNavigationBarScrim)
        }
}
