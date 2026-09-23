package depollsoft.pitchperfect

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Play's Wear review rejects builds whose splash screen omits the app icon.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SplashScreenThemeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun launcherStartsOnSplashThemeWithAppIcon() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, PitchPipeActivity::class.java), PackageManager.GET_META_DATA)
        assertEquals(R.style.Theme_PitchPerfect_Wear_Starting, info.themeResource)

        val theme = context.resources.newTheme().apply { applyStyle(info.themeResource, true) }
        assertEquals(R.mipmap.ic_launcher,
            theme.resolve(androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon))
        assertEquals(R.style.Theme_PitchPerfect_Wear,
            theme.resolve(androidx.core.splashscreen.R.attr.postSplashScreenTheme))
    }

    private fun android.content.res.Resources.Theme.resolve(attr: Int): Int =
        TypedValue().also { resolveAttribute(attr, it, true) }.resourceId
}
