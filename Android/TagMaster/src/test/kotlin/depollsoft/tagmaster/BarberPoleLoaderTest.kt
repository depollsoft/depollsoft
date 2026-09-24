package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import depollsoft.tagmaster.ui.BarberPoleLoader
import depollsoft.tagmaster.ui.BarberPoleRenderer
import depollsoft.tagmaster.ui.CompactBarberPole
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

/**
 * The barber-pole loader's artwork: the canonical stripe and silhouette of the logo, stripes that
 * move while the frame stays still, and a seamless loop.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BarberPoleLoaderTest {
    @get:Rule
    val compose = createComposeRule()

    /** The loader's drawing surface: a size, and the resources the logo is built from. */
    private class Pole(
        var width: Int = 68,
        var height: Int = 116,
    ) {
        val context: android.content.Context = RuntimeEnvironment.getApplication()
        val resources: android.content.res.Resources = context.resources

        fun layout(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            width = right - left
            height = bottom - top
        }
    }

    private fun withPole(block: (Pole) -> Unit) = block(Pole())

    private fun render(
        pole: Pole,
        phase: Float,
    ): Bitmap =
        Bitmap.createBitmap(pole.width, pole.height, Bitmap.Config.ARGB_8888).also {
            BarberPoleRenderer.draw(Canvas(it), pole.width, pole.height, phase, dark = false, compactHeightPx = null)
        }

    private fun shaftMask(pole: Pole): Bitmap =
        Bitmap.createBitmap(pole.width, pole.height, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            val scale = minOf(pole.width / BarberPoleLogo.WIDTH, pole.height / BarberPoleLogo.HEIGHT)
            canvas.translate((pole.width - BarberPoleLogo.WIDTH * scale) / 2f, (pole.height - BarberPoleLogo.HEIGHT * scale) / 2f)
            canvas.scale(scale, scale)
            canvas.drawPath(
                BarberPoleLogo(pole.resources).shaft,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                    strokeWidth = 2f / scale // One raster-pixel tolerance at the clip edge.
                },
            )
        }

    /** The loaders say what is loading, and the compact one keeps its space while idle. */
    @Test
    fun loadersAnnounceThemselvesAndTheCompactOneKeepsItsSpace() {
        var loading by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            val themed = android.view.ContextThemeWrapper(androidx.compose.ui.platform.LocalContext.current, R.style.AppTheme)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalContext provides themed) {
                depollsoft.tagmaster.ui.TagMasterTheme {
                    androidx.compose.foundation.layout.Column {
                        BarberPoleLoader(true, "Loading tags", Modifier.testTag("pole"))
                        CompactBarberPole(loading, "Loading track", Modifier.testTag("compact"))
                    }
                }
            }
        }
        compose.onNodeWithTag("pole").assertContentDescriptionContains("Loading tags")
        val idle = compose.onNodeWithTag("compact", useUnmergedTree = true).fetchSemanticsNode().size
        assertTrue("the idle compact pole still takes its space", idle.width > 0 && idle.height > 0)
        loading = true
        compose.waitForIdle()
        assertEquals(idle, compose.onNodeWithTag("compact", useUnmergedTree = true).fetchSemanticsNode().size)
        compose.onNodeWithTag("compact", useUnmergedTree = true).assertContentDescriptionContains("Loading track")
    }

    @Test fun generated_stripe_matches_original_android_control_point_transform() =
        withPole { pole ->
            val source =
                pole.resources.getXml(R.drawable.ic_barberpole).use { xml ->
                    while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (xml.name == "path") break
                    }
                    androidx.core.graphics.PathParser.createPathFromPathData(
                        xml.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData"),
                    )!!
                }
            val original = android.graphics.PathMeasure(source, false)
            repeat(5) { assertTrue(original.nextContour()) }
            val expected = android.graphics.Path()
            original.getSegment(0f, original.length, expected, true)
            expected.close()
            expected.transform(android.graphics.Matrix().apply { setRotate(-26f) })
            val bounds = android.graphics.RectF()
            expected.computeBounds(bounds, true)
            expected.transform(android.graphics.Matrix().apply { setScale(1.4f, 1f, bounds.centerX(), bounds.centerY()) })
            val actual = BarberPoleLogo(pole.resources).stripe
            val actualBounds = android.graphics.RectF()
            expected.computeBounds(bounds, true)
            actual.computeBounds(actualBounds, true)
            assertEquals(bounds.left, actualBounds.left, 0.001f)
            assertEquals(bounds.top, actualBounds.top, 0.001f)
            assertEquals(bounds.right, actualBounds.right, 0.001f)
            assertEquals(bounds.bottom, actualBounds.bottom, 0.001f)
            val a = android.graphics.PathMeasure(expected, false)
            val b = android.graphics.PathMeasure(actual, false)
            assertEquals(a.length, b.length, 0.001f)
            val pointA = FloatArray(2)
            val pointB = FloatArray(2)
            for (sample in 0..100) {
                a.getPosTan(a.length * sample / 100f, pointA, null)
                b.getPosTan(b.length * sample / 100f, pointB, null)
                assertEquals("Canonical stripe x at $sample", pointA[0], pointB[0], 0.001f)
                assertEquals("Canonical stripe y at $sample", pointA[1], pointB[1], 0.001f)
            }
        }

    @Test fun stripes_move_but_logo_finials_collars_and_alpha_do_not() =
        withPole { pole ->
            for (size in listOf(68 to 116, 340 to 580, 400 to 580)) {
                pole.layout(0, 0, size.first, size.second)
                val mask = shaftMask(pole)
                val first = render(pole, 0f)
                for (phase in listOf(0.001f, 0.25f, 0.5f, 0.999f, 1f)) {
                    val next = render(pole, phase)
                    var changes = 0
                    val colors = mutableSetOf<Int>()
                    for (y in 0 until first.height) {
                        for (x in 0 until first.width) {
                            val a = first.getPixel(x, y)
                            val b = next.getPixel(x, y)
                            assertEquals("Silhouette alpha at $phase", android.graphics.Color.alpha(a), android.graphics.Color.alpha(b))
                            if (mask.getPixel(x, y) == 0) assertEquals("Stationary metal at $x,$y phase $phase", a, b)
                            if (a != b) changes++
                            colors.add(b)
                        }
                    }
                    assertTrue(colors.contains(android.graphics.Color.WHITE))
                    assertTrue(colors.contains(android.graphics.Color.rgb(190, 42, 53)))
                    assertTrue(colors.contains(android.graphics.Color.rgb(0, 99, 165)))
                    when (phase) {
                        1f -> assertEquals("Exact seamless loop endpoint", 0, changes)
                        0.25f, 0.5f -> assertTrue("Axial stripe movement", changes > first.width * first.height / 20)
                        else -> assertTrue("Continuous wrap, not a jump", changes < first.width * first.height / 20)
                    }
                    next.recycle()
                }
                first.recycle()
                mask.recycle()
            }
        }

    @Test fun canonical_diagonal_silhouette_round_balls_and_highlights_are_preserved() =
        withPole { pole ->
            pole.layout(0, 0, 300, 514)
            val source =
                pole.resources.getXml(R.drawable.ic_barberpole).use { xml ->
                    while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (xml.name == "path") break
                    }
                    androidx.core.graphics.PathParser.createPathFromPathData(
                        xml.getAttributeValue("http://schemas.android.com/apk/res/android", "pathData"),
                    )!!
                }
            val reference = Bitmap.createBitmap(300, 514, Bitmap.Config.ARGB_8888)
            val paint =
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = pole.context.getColor(R.color.md_on_surface_variant)
                }
            val referenceCanvas = Canvas(reference)
            val scale = minOf(300f / BarberPoleLogo.WIDTH, 514f / BarberPoleLogo.HEIGHT)
            referenceCanvas.translate((300 - BarberPoleLogo.WIDTH * scale) / 2f, (514 - BarberPoleLogo.HEIGHT * scale) / 2f)
            referenceCanvas.scale(scale, scale)
            referenceCanvas.drawPath(source, paint)
            val actual = render(pole, 0f)
            // Compare canonical balls AND flared collars, including their curved cutouts.
            var compared = 0
            for (y in 0 until 514) {
                for (x in 0 until 300) {
                    if (y in 129..416) continue
                    val expectedAlpha = android.graphics.Color.alpha(reference.getPixel(x, y))
                    val actualAlpha = android.graphics.Color.alpha(actual.getPixel(x, y))
                    // Path boolean operations can rasterize a curve edge differently from
                    // a compound vector. Allow one pixel only; interiors and holes must match.
                    val neighbors =
                        (-1..1).flatMap { dy ->
                            (-1..1).map { dx ->
                                android.graphics.Color.alpha(reference.getPixel((x + dx).coerceIn(0, 299), (y + dy).coerceIn(0, 513)))
                            }
                        }
                    assertTrue(
                        "Canonical finial/collar at $x,$y expected=$expectedAlpha actual=$actualAlpha",
                        actualAlpha in (neighbors.min() - 8)..(neighbors.max() + 8),
                    )
                    if (expectedAlpha > 240) compared++
                }
            }
            assertTrue("Large ball finials, not rectangular caps", compared > 14000)
            // Canonical ball-center line leans right by 25.6 degrees; whole viewport is 0.584.
            assertEquals(0.583715f, BarberPoleLogo.WIDTH / BarberPoleLogo.HEIGHT, 0.0001f)

            fun center(rows: IntRange): Pair<Double, Double> {
                var sumX = 0.0
                var sumY = 0.0
                var count = 0
                for (y in rows) {
                    for (x in 0 until actual.width) {
                        if (android.graphics.Color.alpha(actual.getPixel(x, y)) > 240) {
                            sumX += x
                            sumY += y
                            count++
                        }
                    }
                }
                return sumX / count to sumY / count
            }
            val top = center(0..99)
            val bottom = center(448..513)
            // Lower collar overlaps the ball's upper-left quadrant, so use the lower ball's
            // canonical center row to measure its horizontal center independently.
            val bottomXs = (0 until 110).filter { android.graphics.Color.alpha(actual.getPixel(it, 464)) > 240 }
            val bottomX = (bottomXs.first() + bottomXs.last()) / 2.0
            val angle = Math.toDegrees(kotlin.math.atan2(top.first - bottomX, 464.0 - top.second))
            assertEquals("Measured ball-center axis", 26.0, angle, 1.0)
            val pixels = mutableListOf<Pair<Int, Int>>()
            for (y in 0 until 514) {
                for (x in 0 until 300) {
                    if (android.graphics.Color.alpha(actual.getPixel(x, y)) > 0) pixels.add(x to y)
                }
            }
            val boundsWidth = pixels.maxOf { it.first } - pixels.minOf { it.first } + 1
            val boundsHeight = pixels.maxOf { it.second } - pixels.minOf { it.second } + 1
            assertEquals("Measured outer aspect", 0.584, boundsWidth.toDouble() / boundsHeight, 0.003)
            assertTrue("Round lower finial diameter", bottomXs.last() - bottomXs.first() in 95..103)
            assertTrue(bottom.second > 470)
            println("Logo geometry: bounds=${boundsWidth}x$boundsHeight angle=$angle canonical-cap-pixels=$compared")

            fun opaque(
                x: Int,
                y: Int,
            ) = android.graphics.Color.alpha(actual.getPixel(x, y)) > 240
            assertTrue(opaque(249, 51) && opaque(51, 464))
            assertFalse(opaque(200, 1) || opaque(299, 1) || opaque(0, 513) || opaque(99, 513))
            assertTrue("Curved upper ball highlight", !opaque(245, 19))
            assertTrue("Curved lower ball highlight", !opaque(25, 443))
            assertTrue("Upper collar highlight", !opaque(195, 88))
            assertTrue("Lower collar highlight", !opaque(80, 420))
            actual.recycle()
            reference.recycle()
        }
}
