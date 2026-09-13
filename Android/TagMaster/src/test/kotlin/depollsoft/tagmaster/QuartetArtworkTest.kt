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
            assertEquals(0f, QuartetArtwork.still[voice], 0f)
            assertEquals(QuartetArtwork.translation(voice, 0f), QuartetArtwork.translation(voice, 1f), 0f)
            for (i in 0 until 120) {
                val midpoint = (QuartetArtwork.samples[voice][i] + QuartetArtwork.samples[voice][i + 1]) / 2
                assertEquals(midpoint, QuartetArtwork.translation(voice, (i + 0.5f) / 120), 0.00001f)
            }
        }
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
        assertTrue(start.sameAs(Bitmap.createBitmap(wide, 50, 0, 216, 96)))
        assertEquals(0xFF007AA3.toInt(), QuartetArtwork.lightNote)
        assertEquals(0xFF5AC8FA.toInt(), QuartetArtwork.darkNote)
    }
}
