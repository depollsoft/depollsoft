package depollsoft.pitchperfect

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import depollsoft.pitchperfect.lib.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class KeyDialViewTest {
    @Test
    fun signatureGlyphsFollowTheMusiQwikEncoding() {
        assertEquals("&", KeyDialView.signatureGlyphs(0))
        assertEquals("&\u00a1", KeyDialView.signatureGlyphs(1))
        assertEquals("&\u00a3", KeyDialView.signatureGlyphs(3))
        assertEquals("&\u00a6", KeyDialView.signatureGlyphs(6))
        assertEquals("&\u00a8", KeyDialView.signatureGlyphs(-1))
        assertEquals("&\u00a9", KeyDialView.signatureGlyphs(-2))
        assertEquals("&\u20ac", KeyDialView.signatureGlyphs(-6))
    }

    @Test
    fun accidentalCountsReadAsEngravedLabels() {
        assertEquals("NO SHARPS OR FLATS", KeyDialView.accidentalCount(0))
        assertEquals("1 SHARP", KeyDialView.accidentalCount(1))
        assertEquals("1 FLAT", KeyDialView.accidentalCount(-1))
        assertEquals("4 SHARPS", KeyDialView.accidentalCount(4))
        assertEquals("5 FLATS", KeyDialView.accidentalCount(-5))
    }

    @Test
    fun spokenNamesSpellTheAccidentalAndMode() {
        assertEquals("F sharp major, 6 sharps", KeyDialView.spokenName(Key.getMajorKeys().last()))
        assertEquals("C major, no sharps or flats", KeyDialView.spokenName(Key.getMajorKeys()[6]))
        assertEquals("E flat minor, 6 flats", KeyDialView.spokenName(Key.getMinorKeys().first()))
    }

    @Test
    fun selectingAKeyFollowsItsMode() {
        val view = KeyDialView(ApplicationProvider.getApplicationContext<Application>())
        view.layout(0, 0, 600, 600)
        assertFalse(view.isMinor)
        val minor = Key.getMinorKeys()[3]
        view.select(minor)
        assertTrue(view.isMinor)
        assertEquals(minor, view.selectedKey)
        view.select(Key.getMajorKeys()[6])
        assertFalse(view.isMinor)
    }
}
