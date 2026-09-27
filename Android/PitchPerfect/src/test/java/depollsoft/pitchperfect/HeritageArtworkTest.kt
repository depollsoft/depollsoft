package depollsoft.pitchperfect

import android.app.Application
import android.graphics.Bitmap
import depollsoft.pitchperfect.ui.HeritageArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The score artwork is held as an alpha mask. Decoded in full colour at 420dpi it took about 22MB
 * of a 48MB Android 7 heap, and random use of the app ran it out of memory within a few hundred
 * events on an Android 7 emulator; the old View app did the same.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = "w411dp-h731dp-420dpi")
class HeritageArtworkTest {
    @Test
    fun theArtworkIsKeptAsAnAlphaMaskAQuarterOfItsColourSize() {
        val resources = RuntimeEnvironment.getApplication().resources
        val mask = assertNotNull(HeritageArtwork.get(resources))
        assertEquals(Bitmap.Config.ALPHA_8, mask.config)
        // One byte a pixel: the 420dpi raster is about 2835x1910, 5.4MB rather than 21.7MB.
        val colourBytes = mask.width.toLong() * mask.height * 4
        assertTrue("mask is ${mask.byteCount} of $colourBytes colour bytes", mask.byteCount * 3L < colourBytes)
    }

    @Test
    fun theMaskIsDecodedOncePerDensity() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertTrue(HeritageArtwork.get(resources) === HeritageArtwork.get(resources))
    }

    private fun <T> assertNotNull(value: T?): T {
        assertNotNull("no artwork decoded", value as Any?)
        return value!!
    }
}
