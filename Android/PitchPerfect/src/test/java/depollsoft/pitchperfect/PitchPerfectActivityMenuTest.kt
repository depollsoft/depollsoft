package depollsoft.pitchperfect

import android.os.Looper
import android.widget.PopupMenu
import androidx.viewpager2.widget.ViewPager2
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class PitchPerfectActivityMenuTest {
    @Before
    fun useInMemoryPreferences() {
        Preferences.setTestMode(true)
        // Another class may have emptied the in-memory store since SettingsModel's `preference`
        // delegates registered their defaults, which happens only once. Put them back so this
        // test reads real settings whatever order it runs in.
        ScreenTestSupport.seedSettingsDefaults()
    }

    @After
    fun resetPreferences() {
        Preferences.setTestMode(false)
    }

    @Test
    fun songActions_toggleWithoutRebuildingTheMenu() {
        val activity = Robolectric.buildActivity(PitchPerfectActivity::class.java).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val menu = PopupMenu(activity, pager).menu

        assertTrue(activity.onCreateOptionsMenu(menu))
        val edit = menu.findItem(R.id.editSongsMenuItem)
        val sort = menu.findItem(R.id.sortMenuItem)
        assertNotNull(menu.findItem(R.id.settingsMenuItem))
        assertFalse("song actions stay hidden off the Songs page", edit.isVisible)
        assertFalse(sort.isVisible)

        // An animated switch must reveal the actions once the pager settles.
        pager.setCurrentItem(3, true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))

        assertEquals(3, pager.currentItem)
        assertTrue("edit action appears after the animated switch", edit.isVisible)
        assertFalse(sort.isVisible)
        assertEquals(activity.getString(R.string.EditSongList), edit.title.toString())

        // Leaving the page hides them again; the menu is never re-inflated.
        pager.setCurrentItem(1, false)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(edit.isVisible)
        assertEquals(3, menu.size())
    }
}
