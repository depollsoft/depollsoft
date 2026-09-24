package depollsoft.pitchperfect

import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The banner slot hosts whichever ad view the activity currently holds. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class AdAreaTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aReloadedBannerReplacesTheOldOneOnScreen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = View(context)
        val second = View(context)
        val slot = mutableStateOf(AdSlot(visible = true, heightPx = 50, banner = first, onRemoveAds = {}))
        compose.setContent { PlateTheme { AdArea(slot.value, 8.dp) } }
        compose.waitForIdle()
        assertTrue(first.isAttachedToWindow)

        // A configuration change the activity handles itself destroys the AdView and builds another.
        slot.value = AdSlot(visible = true, heightPx = 50, banner = second, onRemoveAds = {})
        compose.waitForIdle()
        assertTrue("the new banner is shown", second.isAttachedToWindow)
        assertFalse("the destroyed banner is gone", first.isAttachedToWindow)
    }
}
