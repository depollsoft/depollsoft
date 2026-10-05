package depollsoft.pitchperfect

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Play's Wear review enforces guideline WO-V15: a 48x48dp launcher icon centred on a black
 * splash screen. It rejected the bare launcher icon on the plate colour twice, so these
 * tests pin the theme, the icon's size and what the compat splash actually paints.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class SplashScreenThemeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun launcherStartsOnSplashThemeWithAppIcon() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, PitchPipeActivity::class.java), PackageManager.GET_META_DATA)
        assertEquals(R.style.Theme_PitchPerfect_Wear_Starting, info.themeResource)

        val theme = context.resources.newTheme().apply { applyStyle(info.themeResource, true) }
        assertEquals(R.drawable.splash_screen,
            theme.resolve(androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon))
        assertEquals(android.R.color.black,
            theme.resolve(androidx.core.splashscreen.R.attr.windowSplashScreenBackground))
        assertEquals(R.style.Theme_PitchPerfect_Wear,
            theme.resolve(androidx.core.splashscreen.R.attr.postSplashScreenTheme))
    }

    @Test
    fun splashIconIsTheLauncherIconAt48dp() {
        val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.splash_screen, null)
        val layers = icon as LayerDrawable
        assertEquals(1, layers.numberOfLayers)
        val expected = (48 * context.resources.displayMetrics.density).roundToInt()
        assertEquals(expected, layers.getLayerWidth(0))
        assertEquals(expected, layers.getLayerHeight(0))
        assertTrue("splash icon must be the adaptive launcher icon",
            layers.getDrawable(0) is AdaptiveIconDrawable)
    }

    // Before Android 12 (Wear OS 3 and earlier) there is no system splash, so core-splashscreen
    // paints the window background itself; on a 384px round watch that must be a 96px (48dp)
    // icon centred on black. (Wear OS 4 draws the system splash at its own icon size.)
    @Test
    @Config(sdk = [28], qualifiers = "watch-xhdpi")
    fun compatSplashPaintsA48dpIconCentredOnBlack() {
        val theme = context.resources.newTheme().apply {
            applyStyle(R.style.Theme_PitchPerfect_Wear_Starting, true)
        }
        val background = TypedValue().also {
            theme.resolveAttribute(android.R.attr.windowBackground, it, true)
        }
        val drawable = ResourcesCompat.getDrawable(context.resources, background.resourceId, theme)!!
        val size = 384
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))

        assertEquals(Color.BLACK, bitmap.getPixel(4, size / 2))
        assertEquals(Color.BLACK, bitmap.getPixel(size / 2, 4))
        var left = size; var top = size; var right = -1; var bottom = -1
        for (y in 0 until size) for (x in 0 until size) {
            if (bitmap.getPixel(x, y) != Color.BLACK) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        val width = right - left + 1
        val height = bottom - top + 1
        assertTrue("icon is $width x $height px, expected 96", abs(width - 96) <= 2 && abs(height - 96) <= 2)
        assertTrue("icon spans $left..$right, not centred", abs((left + right) / 2 - size / 2) <= 2)
        assertTrue("icon spans $top..$bottom, not centred", abs((top + bottom) / 2 - size / 2) <= 2)
    }

    private fun android.content.res.Resources.Theme.resolve(attr: Int): Int =
        TypedValue().also { resolveAttribute(attr, it, true) }.resourceId
}
