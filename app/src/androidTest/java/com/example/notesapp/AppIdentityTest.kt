package com.example.notesapp

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIdentityTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        packageManager = context.packageManager
    }

    @Test
    fun given_installed_app_when_reading_application_label_then_is_notelet() {
        val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)

        val label = packageManager.getApplicationLabel(applicationInfo).toString()

        assertEquals(APP_NAME, label)
    }

    @Test
    fun given_installed_app_when_reading_main_activity_label_then_is_notelet() {
        val activityInfo = packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        val label = activityInfo.loadLabel(packageManager).toString()

        assertEquals(APP_NAME, label)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun given_api_26_or_higher_when_loading_app_icon_then_is_adaptive() {
        val icon = packageManager.getApplicationIcon(context.packageName)

        assertTrue("Icon is ${icon.javaClass.name}", icon is AdaptiveIconDrawable)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun given_api_26_or_higher_when_loading_round_icon_then_is_adaptive() {
        val roundIcon = loadDrawable(R.mipmap.ic_launcher_round)

        assertTrue("Round icon is ${roundIcon.javaClass.name}", roundIcon is AdaptiveIconDrawable)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun given_api_26_or_higher_when_rendering_app_icon_background_then_is_brand_red() {
        val icon = packageManager.getApplicationIcon(context.packageName) as AdaptiveIconDrawable

        val color = centerPixelOf(icon.background)

        assertEquals(hex(BRAND_RED), hex(color))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun given_api_26_or_higher_when_rendering_round_icon_background_then_is_brand_red() {
        val roundIcon = loadDrawable(R.mipmap.ic_launcher_round) as AdaptiveIconDrawable

        val color = centerPixelOf(roundIcon.background)

        assertEquals(hex(BRAND_RED), hex(color))
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun given_api_33_or_higher_when_loading_icons_then_both_have_monochrome_layer() {
        val icon = packageManager.getApplicationIcon(context.packageName) as AdaptiveIconDrawable
        val roundIcon = loadDrawable(R.mipmap.ic_launcher_round) as AdaptiveIconDrawable

        assertNotNull(icon.monochrome)
        assertNotNull(roundIcon.monochrome)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    fun given_api_below_26_when_loading_icons_then_both_are_vectors() {
        val icon = packageManager.getApplicationIcon(context.packageName)
        val roundIcon = loadDrawable(R.mipmap.ic_launcher_round)

        assertTrue("Icon is ${icon.javaClass.name}", icon is VectorDrawable)
        assertTrue("Round icon is ${roundIcon.javaClass.name}", roundIcon is VectorDrawable)
    }

    private fun loadDrawable(resId: Int): Drawable =
        requireNotNull(context.getDrawable(resId)) { "Drawable $resId not found" }

    private fun centerPixelOf(drawable: Drawable): Int {
        val bitmap = Bitmap.createBitmap(RENDER_SIZE_PX, RENDER_SIZE_PX, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, RENDER_SIZE_PX, RENDER_SIZE_PX)
        drawable.draw(Canvas(bitmap))
        return bitmap.getPixel(RENDER_SIZE_PX / 2, RENDER_SIZE_PX / 2).also { bitmap.recycle() }
    }

    private fun hex(color: Int): String = String.format("#%08X", color)

    private companion object {
        const val APP_NAME = "Notelet"
        const val BRAND_RED = 0xFFE8484F.toInt()
        const val RENDER_SIZE_PX = 108
    }
}
