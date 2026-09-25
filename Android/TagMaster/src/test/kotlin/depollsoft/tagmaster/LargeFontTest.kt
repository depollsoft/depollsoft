package depollsoft.tagmaster

import android.app.Application
import android.util.TypedValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.TagMasterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * At a large font size, text is as big as a TextView made it: Android 14 scales sp non-linearly,
 * and rounding a size to whole pixels must go back through that same curve.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "xxhdpi", fontScale = 2f)
class LargeFontTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textAtTwiceTheFontSizeMatchesTheTextViewsWholePixels() {
        val metrics = RuntimeEnvironment.getApplication().resources.displayMetrics
        val sizes = mutableListOf<Pair<Float, Float>>()
        compose.setContent {
            val density = LocalDensity.current
            for ((style, sp) in listOf(TagMasterType.bodyLarge to 16f, TagMasterType.titleLarge to 22f, TagMasterType.headlineSmall to 24f)) {
                val shown = with(density) { style.fontSize.toPx() }
                val textView = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
                sizes += shown to textView
            }
        }
        compose.waitForIdle()
        // The curve flattens large sizes: 24sp at 200% is well under 48dp.
        assertTrue("non-linear scaling applies: ${sizes.last().second}px", sizes.last().second < metrics.density * 2f * 24f - 2f)
        for ((shown, textView) in sizes) {
            assertEquals("a ${textView}px TextView size", (textView + 0.5f).toInt().toFloat(), shown, 0.51f)
        }
    }
}
