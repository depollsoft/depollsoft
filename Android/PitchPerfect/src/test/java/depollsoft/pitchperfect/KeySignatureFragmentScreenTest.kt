package depollsoft.pitchperfect

import android.view.View
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
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
import org.robolectric.RobolectricTestRunner as Runner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The key signature screen's lists and major/minor toggle, migrated from the instrumented
 * `KeySignatureFragmentTest`.
 *
 * The fragment is hosted directly rather than reached through the pager, because every assertion
 * here is about the fragment's own views. The bottom-navigation route into it is covered by
 * [PitchPerfectActivityScreenTest].
 *
 * Espresso's `onData(...).inAdapterView(...).atPosition(n)` becomes a direct read of the
 * `ListView`'s adapter and its bound child, which is what the matcher resolved to anyway.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class KeySignatureFragmentScreenTest {
    private var controller: ActivityController<FragmentActivity>? = null
    private lateinit var fragment: KeySignatureFragment

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun host(): FragmentActivity {
        val created = Robolectric.buildActivity(FragmentActivity::class.java)
        controller = created
        created.get().setTheme(R.style.AppTheme)
        created.setup()
        val activity = created.get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        fragment = KeySignatureFragment()
        activity.supportFragmentManager
            .beginTransaction()
            .add(container.id, fragment)
            .commitNow()
        idle()
        return activity
    }

    private fun majorList(): ListView = fragment.requireView().findViewById(R.id.majorKeySignatureListView)

    private fun minorList(): ListView = fragment.requireView().findViewById(R.id.minorKeySignatureListView)

    private fun fab(): ExtendedFloatingActionButton = fragment.requireView().findViewById(R.id.majorMinorFab)

    /**
     * The row the list has actually laid out at [position].
     *
     * `KeySignatureListItemView` binds its key in `onAttachedToWindow`, so an adapter row built
     * detached would never show its text. Scrolling the list to the position and reading the real
     * child is what Espresso's `onData(...).atPosition(n)` resolved to on a device.
     */
    private fun rowAt(
        list: ListView,
        position: Int,
    ): View {
        val adapter = requireNotNull(list.adapter) { "the list should have an adapter" }
        assertTrue("position $position should exist", position < adapter.count)
        list.setSelection(position)
        idle()
        val child = list.getChildAt(position - list.firstVisiblePosition)
        return requireNotNull(child) { "the list should have laid out row $position" }
    }

    private fun toggle() {
        fab().performClick()
        idle()
    }

    // ==================== Layout ====================

    @Test
    fun majorKeyListIsDisplayedByDefault() {
        host()
        assertTrue("the fragment starts in major mode", fragment.model.isMajor)
        assertDisplayed("majorKeySignatureListView", majorList())
        assertEquals("the minor list is hidden in major mode", View.GONE, minorList().visibility)
    }

    @Test
    fun majorMinorFabIsDisplayed() {
        host()
        assertDisplayed("majorMinorFab", fab())
    }

    @Test
    fun majorKeyListScrollsAndStaysDisplayed() {
        host()
        val list = majorList()
        list.smoothScrollToPosition(list.adapter.count - 1)
        list.setSelection(list.adapter.count - 1)
        idle()
        assertDisplayed("majorKeySignatureListView after scrolling", list)
    }

    // ==================== Major keys ====================

    @Test
    fun majorKeyListHasItems() {
        host()
        assertEquals("the twelve major keys plus the enharmonic spelling", 13, majorList().adapter.count)
        assertDisplayed("the first major row", rowAt(majorList(), 0))
    }

    @Test
    fun selectingMajorKeysSoundsThemAndLeavesTheListDisplayed() {
        host()
        for (position in 0..2) {
            val key = majorList().adapter.getItem(position) as depollsoft.pitchperfect.lib.Key
            val row = rowAt(majorList(), position)
            row.onTouchEvent(motion(android.view.MotionEvent.ACTION_DOWN, row))
            assertTrue("major key at $position should sound while held", key.note.isPlaying)
            row.onTouchEvent(motion(android.view.MotionEvent.ACTION_UP, row))
            assertFalse("major key at $position should fall silent", key.note.isPlaying)
        }
        assertDisplayed("majorKeySignatureListView", majorList())
    }

    @Test
    fun majorKeyItemsShowANameAndASignature() {
        host()
        val row = rowAt(majorList(), 0)
        assertNotNull("keyNameTextView", row.findViewById<TextView>(R.id.keyNameTextView))
        assertNotNull("keySignatureTextView", row.findViewById<TextView>(R.id.keySignatureTextView))
        assertTrue(
            "the row names its key",
            row.findViewById<TextView>(R.id.keyNameTextView).text.isNotEmpty(),
        )
    }

    // ==================== Major/minor toggle ====================

    @Test
    fun togglingShowsMinorKeys() {
        host()
        toggle()
        assertFalse(fragment.model.isMajor)
        assertDisplayed("minorKeySignatureListView", minorList())
        assertEquals("the major list is hidden in minor mode", View.GONE, majorList().visibility)
    }

    @Test
    fun togglingBackShowsMajorKeys() {
        host()
        toggle()
        toggle()
        assertTrue(fragment.model.isMajor)
        assertDisplayed("majorKeySignatureListView", majorList())
        assertEquals(View.GONE, minorList().visibility)
    }

    @Test
    fun minorKeyListHasItems() {
        host()
        toggle()
        assertEquals(13, minorList().adapter.count)
        assertDisplayed("the first minor row", rowAt(minorList(), 0))
    }

    @Test
    fun selectingMinorKeysSoundsThemAndLeavesTheListDisplayed() {
        host()
        toggle()
        for (position in 0..2) {
            val key = minorList().adapter.getItem(position) as depollsoft.pitchperfect.lib.Key
            val row = rowAt(minorList(), position)
            row.onTouchEvent(motion(android.view.MotionEvent.ACTION_DOWN, row))
            assertTrue("minor key at $position should sound while held", key.note.isPlaying)
            row.onTouchEvent(motion(android.view.MotionEvent.ACTION_UP, row))
            assertFalse("minor key at $position should fall silent", key.note.isPlaying)
        }
        assertDisplayed("minorKeySignatureListView", minorList())
    }

    @Test
    fun minorKeyListScrollsAndStaysDisplayed() {
        host()
        toggle()
        val list = minorList()
        list.setSelection(list.adapter.count - 1)
        idle()
        assertDisplayed("minorKeySignatureListView after scrolling", list)
    }

    @Test
    fun minorKeyItemsShowANameAndASignature() {
        host()
        toggle()
        val row = rowAt(minorList(), 0)
        assertTrue(row.findViewById<TextView>(R.id.keyNameTextView).text.isNotEmpty())
        assertNotNull(row.findViewById<TextView>(R.id.keySignatureTextView))
    }

    // ==================== FAB state ====================

    @Test
    fun fabRemainsVisibleAfterToggling() {
        host()
        toggle()
        assertDisplayed("majorMinorFab after toggling", fab())
    }

    @Test
    fun multipleTogglesEndWhereTheyStarted() {
        host()
        repeat(4) { toggle() }
        assertTrue("an even number of toggles returns to major", fragment.model.isMajor)
        assertDisplayed("majorMinorFab", fab())
        assertDisplayed("majorKeySignatureListView", majorList())
    }

    private fun motion(
        action: Int,
        view: View,
    ) = android.view.MotionEvent.obtain(
        android.os.SystemClock.uptimeMillis(),
        android.os.SystemClock.uptimeMillis(),
        action,
        view.width / 2f,
        view.height / 2f,
        0,
    )
}
