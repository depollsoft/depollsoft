package depollsoft.compose

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.draw.drawBehind
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

    private fun configuration(
        widthDp: Int,
        heightDp: Int,
        size: Int,
        smallestDp: Int = minOf(widthDp, heightDp),
    ) = android.content.res.Configuration().apply {
        screenWidthDp = widthDp
        screenHeightDp = heightDp
        smallestScreenWidthDp = smallestDp
        screenLayout = size
    }

    @Test
    fun aDialogsLeastWidthFollowsAppCompatForOrientationAndScreenSize() {
        val normal = android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL
        val large = android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        val xlarge = android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
        assertEquals(0.95f, dialogMinWidthFraction(configuration(411, 891, normal)), 0f)
        assertEquals(0.65f, dialogMinWidthFraction(configuration(891, 411, normal)), 0f)
        assertEquals(0.80f, dialogMinWidthFraction(configuration(600, 960, large)), 0f)
        assertEquals(0.55f, dialogMinWidthFraction(configuration(960, 600, large)), 0f)
        assertEquals(0.72f, dialogMinWidthFraction(configuration(800, 1280, xlarge)), 0f)
        assertEquals(0.45f, dialogMinWidthFraction(configuration(1280, 800, xlarge)), 0f)
    }

    @Test
    fun aDialogsFirstMeasureIsThePreferredWidthForTheScreen() {
        val normal = android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL
        val large = android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        assertEquals(320.dp, dialogFirstPassWidth(configuration(411, 891, normal)))
        assertEquals(440.dp, dialogFirstPassWidth(configuration(560, 900, large)))
        assertEquals(580.dp, dialogFirstPassWidth(configuration(800, 1280, large, smallestDp = 800)))
    }

    @Test
    fun aDrawableLearnsTheLayoutDirectionItIsDrawnIn() {
        val drawable = android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag("icon").width(24.dp).height(24.dp).drawBehind { drawFitCenter(drawable) })
            }
        }
        compose.onNodeWithTag("icon").captureToImage()
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, drawable.layoutDirection)
    }

    @Test
    fun listMotionEasesInAndOutAsValueAnimatorDid() {
        val interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        for (step in 0..10) {
            val t = step / 10f
            assertEquals(interpolator.getInterpolation(t), ListMotion.easing.transform(t), 0.0001f)
        }
    }
    @Test
    fun paragraphsTakeTheirDirectionFromTheTextAndSitAtTheLayoutsStart() {
        var rtl: TextStyle? = null
        var ltr: TextStyle? = null
        var centred: TextStyle? = null
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                rtl = TextStyle().viewParagraph()
                centred = TextStyle(textAlign = TextAlign.Center).viewParagraph()
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { ltr = TextStyle().viewParagraph() }
        }
        compose.waitForIdle()
        // English in a right-to-left locale reads left to right, from the layout's (right) edge.
        assertEquals(TextDirection.Content, rtl!!.textDirection)
        assertEquals(TextAlign.Right, rtl!!.textAlign)
        assertEquals(TextAlign.Left, ltr!!.textAlign)
        assertEquals(TextAlign.Center, centred!!.textAlign)
    }
}
