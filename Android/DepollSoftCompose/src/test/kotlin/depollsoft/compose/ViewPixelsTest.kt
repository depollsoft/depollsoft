package depollsoft.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The View rules the Compose screens follow, including in right-to-left layouts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ViewPixelsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun startAndEndGravityMirrorInARightToLeftLayout() {
        val child = IntSize(10, 10)
        val space = IntSize(100, 20)
        assertEquals(IntOffset(0, 5), ViewAlign.CenterStart.align(child, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(90, 5), ViewAlign.CenterStart.align(child, space, LayoutDirection.Rtl))
        assertEquals(IntOffset(90, 5), ViewAlign.CenterEnd.align(child, space, LayoutDirection.Ltr))
        assertEquals(IntOffset(0, 5), ViewAlign.CenterEnd.align(child, space, LayoutDirection.Rtl))
        // An odd leftover pixel goes below and after, as integer division puts it.
        assertEquals(IntOffset(45, 5), ViewAlign.Center.align(IntSize(9, 9), IntSize(100, 20), LayoutDirection.Rtl))
    }

    @Test
    fun textSizesRoundToWholePixelsAndStayFullSizeUnderNonLinearScaling() {
        // 14sp at 2.625x is 36.75px; a TextView reads 37.
        val density = Density(2.625f)
        assertEquals(37f, with(density) { 14.sp.inWholePixels(density).toPx() }, 0.001f)
        assertEquals(TextStyle(fontSize = 37f.let { with(density) { it.toSp() } }).fontSize, TextStyle(fontSize = 14.sp).inWholePixels(density).fontSize)
    }

    @Test
    fun viewPixelsTruncateWhereDpRounds() {
        val density = Density(2.625f)
        assertEquals(10, density.viewPx(4))
        assertEquals(36, density.viewPx(14f))
    }

    @Test
    fun aTitleThatFitsTheFirstPassKeepsOneLine() {
        val context = RuntimeEnvironment.getApplication()
        val density = Density(context)
        val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)
        val style = TextStyle(fontSize = 16.sp)
        assertTrue(measurer.dialogTitleFits("Delete?", style, density, cardInset = 24.dp))
        assertFalse(measurer.dialogTitleFits("Delete \"The Saturday afternoon chapter show\"?", style, density, cardInset = 24.dp))
        // A dialog narrower than the first pass caps it.
        assertFalse(measurer.dialogTitleFits("Delete the set list?", style, density, cardInset = 24.dp, finalWidth = 40))
    }

    @Test
    fun theScrollbarThumbSitsAtTheRightInALeftToRightLayout() {
        assertEquals("right", thumbSide(LayoutDirection.Ltr))
    }

    @Test
    fun theScrollbarThumbSitsAtTheLeftInARightToLeftLayout() {
        assertEquals("left", thumbSide(LayoutDirection.Rtl))
    }

    private fun thumbSide(direction: LayoutDirection): String {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                val state = rememberLazyListState()
                Box(Modifier.width(200.dp).height(300.dp).testTag("list")) {
                    LazyColumn(Modifier.fillMaxSize().listViewScrollbar(state), state = state) {
                        items(100) { BasicText("Row $it", Modifier.height(48.dp).padding(horizontal = 20.dp)) }
                    }
                }
            }
        }
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag("list").captureToImage().toPixelMap()
        // Column 20 lies inside the rows' padding on both sides: nothing but background there.
        fun inked(x: Int) = (0 until pixels.height).any { pixels[x, it] != pixels[20, it] }
        val left = (0 until 6).any { inked(it) }
        val right = (pixels.width - 6 until pixels.width).any { inked(it) }
        return when {
            left && !right -> "left"
            right && !left -> "right"
            else -> "left=$left right=$right"
        }
    }
}
