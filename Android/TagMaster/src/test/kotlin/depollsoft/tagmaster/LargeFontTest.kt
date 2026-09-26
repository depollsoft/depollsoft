package depollsoft.tagmaster

import android.app.Application
import android.util.TypedValue
import org.junit.Assert.assertFalse
import depollsoft.tagmaster.ui.ViewCenteredText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.BrandTitle
import depollsoft.tagmaster.ui.TagMasterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
            assertEquals("a ${textView}px TextView size", (textView + 0.5f).toInt().toFloat(), shown, 0.01f)
        }
    }

    @Test
    @Config(fontScale = 1.3f)
    fun theBrandTitleScalesAsTheToolbarsTextViewDid() {
        val context = RuntimeEnvironment.getApplication()
        val metrics = context.resources.displayMetrics
        val curved = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22f, metrics)
        assertTrue("the curve differs from linear scaling here", curved < 22f * metrics.density * 1.3f - 1f)
        // Below the 40dp cap the title follows the curve, not linear scaling.
        assertEquals(curved, BrandTitle(context).sizePx(context), 0.01f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // text measures at its real width only with native graphics
    fun centredTextTooLongForItsSpaceWrapsInsteadOfRunningOffBothEdges() {
        val label = "Clear cached sheet music and tracks"
        compose.setContent {
            // A button label at a large font size: far wider than the 200dp it gets.
            Box(Modifier.width(200.dp)) {
                ViewCenteredText(label, androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), androidx.compose.ui.graphics.Color.Black)
            }
        }
        compose.waitForIdle()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue("wraps: ${layout.lineCount} lines", layout.lineCount > 1)
        assertFalse("nothing cut off", layout.didOverflowWidth)
    }
}
