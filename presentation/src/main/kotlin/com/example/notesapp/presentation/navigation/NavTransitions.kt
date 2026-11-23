package com.example.notesapp.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry

/** Duración de las transiciones entre pantallas, en milisegundos (spec 015, RF-6). */
internal const val NAV_TRANSITION_MILLIS = 300

/**
 * Transiciones del `NavHost` (spec 015, D1): al avanzar, la pantalla nueva entra desde la derecha y la anterior se
 * desplaza un tercio a la izquierda; al volver, al revés. Sin fundido.
 *
 * Tienen la forma de los parámetros de `NavHost`, así que se pasan tal cual, también en un `NavHost` de prueba.
 * Compose aplica por su cuenta la escala de duración de animación del sistema (RF-6 a RF-8).
 */
internal object NavTransitions {

    private val slideSpec: FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = NAV_TRANSITION_MILLIS, easing = FastOutSlowInEasing)

    /** Al avanzar: la pantalla nueva entra desde el borde derecho. */
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }
    }

    /** Al avanzar: la pantalla anterior se desplaza un tercio a la izquierda. */
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / 3 }
    }

    /** Al volver: la pantalla de debajo vuelve desde un tercio a la izquierda. */
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = slideSpec) { fullWidth -> -fullWidth / 3 }
    }

    /** Al volver: la pantalla actual sale por el borde derecho. */
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = slideSpec) { fullWidth -> fullWidth }
    }

    /**
     * Gesto de Atrás predictivo (RF-9 a RF-12): las mismas transiciones de volver, sea cual sea el borde.
     * Sin esto, `NavHost` usaría sus propias transiciones por defecto (fundido y escala).
     */
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        popEnter()
    }

    /** Ver [predictivePopEnter]. */
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        popExit()
    }
}

/**
 * Ignora los toques mientras la pantalla está saliendo (spec 015, RF-18). Consume los eventos de puntero en la
 * pasada inicial, antes de que lleguen a sus hijos, solo cuando el destino de [scope] es dejar de verse.
 */
internal fun Modifier.blockTouchesWhileExiting(scope: AnimatedVisibilityScope): Modifier =
    pointerInput(scope) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (scope.transition.targetState == EnterExitState.PostExit) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }

/**
 * Ejecuta [block] solo si esta entrada es la pantalla activa (spec 015, RF-19). Durante una transición, Navigation
 * limita las entradas a `STARTED`, así que un segundo toque no abre otra pantalla. No se usa para volver (RF-20).
 */
internal inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}
