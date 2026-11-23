package com.example.notesapp.presentation.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Transiciones de `NavTransitions` sobre un `NavHost` de prueba con dos destinos opacos, conectado igual que
 * `NotesNavHost`: mismas transiciones, `blockTouchesWhileExiting` en cada destino y `ifResumed` al avanzar.
 */
@RunWith(AndroidJUnit4::class)
class NavTransitionsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    @Volatile
    private var tapsOnStart = 0

    @Before
    fun setUp() {
        composeRule.setContent {
            val controller = rememberNavController()
            navController = controller
            NavHost(
                navController = controller,
                startDestination = START,
                modifier = Modifier.fillMaxSize(),
                enterTransition = NavTransitions.enter,
                exitTransition = NavTransitions.exit,
                popEnterTransition = NavTransitions.popEnter,
                popExitTransition = NavTransitions.popExit,
                predictivePopEnterTransition = NavTransitions.predictivePopEnter,
                predictivePopExitTransition = NavTransitions.predictivePopExit,
            ) {
                composable(START) { entry ->
                    Box(
                        Modifier
                            .blockTouchesWhileExiting(this)
                            .fillMaxSize()
                            .background(Color.White)
                            .testTag(START_SCREEN),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .size(TARGET_SIZE)
                                .background(Color.Red)
                                .testTag(START_TAP_TARGET)
                                .clickable { tapsOnStart++ },
                        )
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(TARGET_SIZE)
                                .background(Color.Blue)
                                .testTag(GO_FORWARD)
                                .clickable { entry.ifResumed { controller.navigate(NEXT) } },
                        )
                    }
                }
                composable(NEXT) {
                    Box(
                        Modifier
                            .blockTouchesWhileExiting(this)
                            .fillMaxSize()
                            .background(Color.Black)
                            .testTag(NEXT_SCREEN),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // A partir de aquí el reloj solo avanza cuando la prueba lo pide.
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun given_start_screen_when_navigating_forward_then_new_slides_in_from_right_and_previous_shifts_a_third_left() {
        val width = rootWidth()

        navigate()
        composeRule.mainClock.advanceTimeBy(HALF_TRANSITION_MILLIS)

        val newLeft = left(NEXT_SCREEN)
        val previousLeft = left(START_SCREEN)
        assertTrue("nueva a mitad: $newLeft de $width", newLeft > 0f && newLeft < width)
        assertTrue("anterior a mitad: $previousLeft de $width", previousLeft < 0f && previousLeft > -width / 3f)

        settle()

        assertEquals(0f, left(NEXT_SCREEN))
        composeRule.onNodeWithTag(START_SCREEN).assertDoesNotExist()
    }

    @Test
    fun given_next_screen_when_popping_back_then_current_slides_out_right_and_previous_returns_from_a_third_left() {
        navigate()
        settle()
        val width = rootWidth()

        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(HALF_TRANSITION_MILLIS)

        val currentLeft = left(NEXT_SCREEN)
        val previousLeft = left(START_SCREEN)
        assertTrue("actual a mitad: $currentLeft de $width", currentLeft > 0f && currentLeft < width)
        assertTrue("anterior a mitad: $previousLeft de $width", previousLeft < 0f && previousLeft > -width / 3f)

        settle()

        assertEquals(0f, left(START_SCREEN))
        composeRule.onNodeWithTag(NEXT_SCREEN).assertDoesNotExist()
    }

    @Test
    fun given_forward_animation_running_when_exiting_screen_is_tapped_then_tap_is_ignored() {
        // Control: sin animación el toque sí llega.
        composeRule.onNodeWithTag(START_TAP_TARGET).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, tapsOnStart)

        navigate()
        composeRule.mainClock.advanceTimeByFrame()
        // El objetivo sigue a la vista: la pantalla nueva aún no lo cubre.
        val targetRight = composeRule.onNodeWithTag(START_TAP_TARGET).fetchSemanticsNode().boundsInRoot.right
        assertTrue("objetivo cubierto: $targetRight", targetRight < left(NEXT_SCREEN))
        composeRule.onNodeWithTag(START_TAP_TARGET).performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(1, tapsOnStart)
    }

    @Test
    fun given_forward_element_when_tapped_twice_quickly_then_navigates_only_once() {
        composeRule.onNodeWithTag(GO_FORWARD).performClick()
        composeRule.onNodeWithTag(GO_FORWARD).performClick()

        settle()

        assertEquals(1, entriesOf(NEXT))
        assertEquals(1, entriesOf(START))
    }

    @Test
    fun given_forward_animation_running_when_back_pressed_then_returns_to_start_destination() {
        navigate()
        composeRule.mainClock.advanceTimeBy(HALF_TRANSITION_MILLIS)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()

        assertEquals(START, navController.currentDestination?.route)
        assertEquals(0, entriesOf(NEXT))
        assertEquals(0f, left(START_SCREEN))
        composeRule.onNodeWithTag(NEXT_SCREEN).assertDoesNotExist()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun given_predictive_back_at_half_progress_when_cancelled_then_current_returns_to_place_and_stays() {
        navigate()
        settle()
        val width = rootWidth()

        startPredictiveBack()
        progressPredictiveBack(0.5f)

        val currentLeft = left(NEXT_SCREEN)
        val previousLeft = left(START_SCREEN)
        assertTrue("actual a mitad del gesto: $currentLeft de $width", currentLeft > 0f && currentLeft < width)
        assertTrue("anterior a mitad del gesto: $previousLeft de $width", previousLeft < 0f && previousLeft > -width / 3f)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        settle()

        assertEquals(NEXT, navController.currentDestination?.route)
        assertEquals(1, entriesOf(NEXT))
        assertEquals(0f, left(NEXT_SCREEN))
        composeRule.onNodeWithTag(START_SCREEN).assertDoesNotExist()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun given_predictive_back_at_half_progress_when_completed_then_pops_to_previous() {
        navigate()
        settle()

        startPredictiveBack()
        progressPredictiveBack(0.5f)
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()

        assertEquals(START, navController.currentDestination?.route)
        assertEquals(0, entriesOf(NEXT))
        assertEquals(0f, left(START_SCREEN))
        composeRule.onNodeWithTag(NEXT_SCREEN).assertDoesNotExist()
    }

    private fun navigate() {
        composeRule.runOnUiThread { navController.navigate(NEXT) }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun startPredictiveBack() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun progressPredictiveBack(progress: Float) {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.dispatchOnBackProgressed(backEvent(progress))
        }
        // Varios fotogramas para que el NavHost lleve la transición al punto del gesto.
        repeat(SEEK_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
    }

    private fun backEvent(progress: Float) =
        BackEventCompat(touchX = 0f, touchY = 500f, progress = progress, swipeEdge = BackEventCompat.EDGE_LEFT)

    /** Lleva la animación en curso hasta su final y deja componer el resultado. */
    private fun settle() {
        composeRule.mainClock.advanceTimeBy(NAV_TRANSITION_MILLIS.toLong() + SETTLE_MARGIN_MILLIS)
        composeRule.waitForIdle()
    }

    private fun entriesOf(route: String): Int =
        navController.currentBackStack.value.count { it.destination.route == route }

    private fun left(tag: String): Float =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left

    private fun rootWidth(): Float = composeRule.onRoot().fetchSemanticsNode().size.width.toFloat()

    private companion object {
        const val START = "start"
        const val NEXT = "next"
        const val START_SCREEN = "nav_test_start_screen"
        const val NEXT_SCREEN = "nav_test_next_screen"
        const val START_TAP_TARGET = "nav_test_start_tap_target"
        const val GO_FORWARD = "nav_test_go_forward"
        const val HALF_TRANSITION_MILLIS = NAV_TRANSITION_MILLIS / 2L
        const val SETTLE_MARGIN_MILLIS = 100L
        const val SEEK_FRAMES = 5
        val TARGET_SIZE = 48.dp
    }
}
