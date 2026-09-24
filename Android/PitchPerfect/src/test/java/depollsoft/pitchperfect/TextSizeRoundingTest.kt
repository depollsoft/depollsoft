package depollsoft.pitchperfect

import android.content.res.Configuration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ui.inWholePixels
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/** Text sizes rounded to whole pixels, as a TextView read them, at every font scale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "xxhdpi")
class TextSizeRoundingTest {
    private fun densityAt(fontScale: Float): Density {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val configuration = Configuration(context.resources.configuration).apply { this.fontScale = fontScale }
        return Density(context.createConfigurationContext(configuration))
    }

    @Test
    fun aRoundedSizeDrawsAtTheTextViewsWholePixelsUnderNonLinearFontScaling() {
        // Android 14 scales large text non-linearly: 16sp at 200% is not 2 x 16sp.
        val density = densityAt(2f)
        for (size in listOf(12.sp, 16.sp, 20.sp, 24.sp)) {
            val expected = with(density) { size.toPx() }.roundToInt().toFloat()
            val drawn = with(density) { size.inWholePixels(density).toPx() }
            assertEquals("$size at 200%", expected, drawn, 0.01f)
        }
    }

    @Test
    fun atTheDefaultScaleSizesAreUnchangedWherePixelsAreWhole() {
        val density = densityAt(1f)
        assertEquals(48f, with(density) { 16.sp.inWholePixels(density).toPx() }, 0.001f)
    }
}
