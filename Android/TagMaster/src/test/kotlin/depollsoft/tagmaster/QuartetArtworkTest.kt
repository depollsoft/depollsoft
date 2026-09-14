package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuartetArtworkTest {
    @Test fun sampled_motion_is_linear_periodic_and_settled() {
        assertEquals(2.8f, QuartetArtwork.PERIOD, 0f)
        for (voice in 0..3) {
            assertEquals(121, QuartetArtwork.samples[voice].size)
            assertEquals(1f, QuartetArtwork.still[voice], 0f)
            assertTrue(QuartetArtwork.samples[voice].all { it in .65f..1f })
            assertEquals(QuartetArtwork.opacity(voice, 0f), QuartetArtwork.opacity(voice, 1f), 0f)
            for (i in 0 until 120) {
                val midpoint = (QuartetArtwork.samples[voice][i] + QuartetArtwork.samples[voice][i + 1]) / 2
                assertEquals(midpoint, QuartetArtwork.opacity(voice, (i + 0.5f) / 120), 0.00001f)
            }
        }
    }

    @Test fun c7_has_four_fixed_heads_a_common_stem_flat_and_ledger() {
        assertArrayEquals(floatArrayOf(60f, 64f, 67f, 70f), QuartetArtwork.midi, 0f)
        assertArrayEquals(floatArrayOf(108f, 108f, 108f, 108f), QuartetArtwork.x, 0f)
        assertArrayEquals(floatArrayOf(77f, 65f, 53f, 41f), QuartetArtwork.y, 0f)
        val bounds = android.graphics.RectF()
        QuartetArtwork.stem.computeBounds(bounds, true)
        assertEquals(16f, bounds.top, .001f)
        assertEquals(77f, bounds.bottom, .001f)
        QuartetArtwork.ledger.computeBounds(bounds, true)
        assertEquals(96f, bounds.left, .001f)
        assertEquals(120f, bounds.right, .001f)
        QuartetArtwork.flat.computeBounds(bounds, true)
        assertTrue(bounds.right < 108f)
        assertTrue(bounds.top < 41f && bounds.bottom > 41f)
        val measure = android.graphics.PathMeasure(QuartetArtwork.note, false)
        assertFalse("One head contour, no individual stem", measure.nextContour())
    }

    @Test fun artwork_fits_uniformly_and_still_matches_endpoints() {
        val view = TagLoadingView(RuntimeEnvironment.getApplication())

        fun render(
            width: Int,
            phase: Float,
            moving: Boolean,
        ): Bitmap {
            view.layout(0, 0, width, 96)
            return Bitmap.createBitmap(width, 96, Bitmap.Config.ARGB_8888).also { view.drawQuartet(Canvas(it), phase, moving) }
        }
        val start = render(216, 0f, true)
        val end = render(216, 1f, true)
        val still = render(216, 0.5f, false)
        val wide = render(316, 0f, true)
        assertTrue(start.sameAs(end))
        assertTrue(start.sameAs(still))
        val centered = Bitmap.createBitmap(wide, 50, 0, 216, 96)

        fun boundary(
            image: Bitmap,
            x: Int,
            y: Int,
        ): Boolean {
            val pixel = image.getPixel(x, y)
            return (-1..1).any { dy ->
                (-1..1).any { dx ->
                    val xx = (x + dx).coerceIn(0, 215)
                    val yy = (y + dy).coerceIn(0, 95)
                    image.getPixel(xx, yy) != pixel
                }
            }
        }
        for (y in 0 until 96) {
            for (x in 0 until 216) {
                val a = start.getPixel(x, y)
                val b = centered.getPixel(x, y)
                if (a != b) {
                    assertTrue("Only one-pixel AA boundaries may differ at $x,$y", boundary(start, x, y) && boundary(centered, x, y))
                    for (image in listOf(start, centered)) {
                        assertTrue(
                            "Geometry remains within one pixel",
                            (-1..1).any { dy ->
                                (-1..1).any { dx ->
                                    android.graphics.Color.alpha(image.getPixel((x + dx).coerceIn(0, 215), (y + dy).coerceIn(0, 95))) > 10
                                }
                            },
                        )
                    }
                }
            }
        }
        assertEquals(0xFF007AA3.toInt(), QuartetArtwork.lightNote)
        assertEquals(0xFF5AC8FA.toInt(), QuartetArtwork.darkNote)
    }
}
