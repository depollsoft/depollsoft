package depollsoft.tagmaster

import android.app.Application
import android.view.ContextThemeWrapper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.OnAppSurface
import depollsoft.tagmaster.ui.OnChrome
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.asRipple
import depollsoft.tagmaster.ui.tagMasterColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The chrome's light highlight stays on the chrome: a popup opened from it (the overflow menu) highlights as the app does. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ChromeHighlightTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPopupFromTheChromeHighlightsInTheAppsColourAgain() {
        var chrome: RippleConfiguration? = null
        var popup: RippleConfiguration? = null
        val themed = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides themed) {
                TagMasterTheme {
                    OnChrome {
                        chrome = LocalRippleConfiguration.current
                        OnAppSurface { popup = LocalRippleConfiguration.current }
                    }
                }
            }
        }
        compose.waitForIdle()
        val colors = tagMasterColors(themed)
        assertEquals(colors.chromeHighlight.asRipple().color, chrome!!.color)
        assertEquals("the overflow menu's rows use the app's highlight", colors.controlHighlight.asRipple().color, popup!!.color)
    }
}
