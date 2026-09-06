package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SongKeyListAdapterTest {
    @Test
    fun spokenNamesSpellTheAccidentalModeAndCount() {
        assertEquals("F sharp major, 6 sharps", SongKeyListAdapter.spokenName(Key.getMajorKeys().last()))
        assertEquals("C major, no sharps or flats", SongKeyListAdapter.spokenName(Key.getMajorKeys()[6]))
        assertEquals("E flat minor, 6 flats", SongKeyListAdapter.spokenName(Key.getMinorKeys().first()))
        assertEquals("D minor, 1 flat", SongKeyListAdapter.spokenName(Key.getMinorKeys()[5]))
    }

    @Test
    fun selectingAKeyFollowsItsMode() {
        var changed: Key? = null
        val adapter = SongKeyListAdapter(Key.getMajorKeys()[6]) { changed = it }
        assertFalse(adapter.isMinor)
        assertEquals(6, adapter.selectedIndex)

        adapter.select(Key.getMinorKeys()[3])
        assertTrue(adapter.isMinor)
        assertEquals(3, adapter.selectedIndex)
        assertEquals(null, changed)
    }

    @Test
    fun flippingTheModeKeepsTheSignature() {
        var changed: Key? = null
        val adapter = SongKeyListAdapter(Key.getMajorKeys()[9]) { changed = it }
        adapter.setMinor(true)
        assertTrue(adapter.isMinor)
        assertEquals(3, adapter.selectedKey.numAccidentals)
        assertEquals("f", adapter.selectedKey.friendlyName)
        assertEquals(adapter.selectedKey, changed)

        adapter.setMinor(false)
        assertEquals("A", adapter.selectedKey.friendlyName)
    }
}
