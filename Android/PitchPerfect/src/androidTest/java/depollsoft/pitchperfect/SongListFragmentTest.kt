package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the Song List functionality.
 * Tests navigation to songs tab, song list display, and add song dialog.
 *
 * Uses actual resource IDs from the app:
 * - songListView: ListView showing songs
 * - addSongButton: FloatingActionButton to add songs
 * - sorryText: TextView shown when no songs exist
 * - songTitleEditText: EditText for song title in add dialog
 * - songKeyList: Key signature list for key selection in add dialog
 * - songTitleTextView: Song title in list item
 * - songKeyTextView: Key signature in list item
 * - songs_item: Bottom nav menu item for songs tab
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SongListFragmentTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    @Before
    fun dismissDialogs() {
        // Use efficient dialog dismissal instead of Thread.sleep()
        EspressoTestUtils.dismissStartupDialogs()
    }

    /**
     * Navigate to the songs tab using the bottom navigation.
     */
    private fun navigateToSongsTab() {
        onView(withId(R.id.songs_item))
            .perform(click())
        EspressoTestUtils.shortWait()
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testNavigateToSongsTab() {
        // Click on songs tab in bottom navigation
        onView(withId(R.id.songs_item))
            .perform(click())

        EspressoTestUtils.shortWait()

        // Verify we're on the songs tab by checking for the FAB
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))
    }

    // ==================== Layout Tests ====================

    @Test
    fun testSongListViewIsDisplayed() {
        navigateToSongsTab()

        // Verify the song list view is displayed
        onView(withId(R.id.songListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddSongFabIsDisplayed() {
        navigateToSongsTab()

        // Verify the FAB is displayed
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))
    }

    // ==================== Add Song Dialog Tests ====================

    @Test
    fun testFabClickOpensAddSongDialog() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        EspressoTestUtils.shortWait()

        // Verify the add song dialog is displayed with title input
        onView(withId(R.id.songTitleEditText))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddSongDialogHasKeyList() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        EspressoTestUtils.shortWait()

        // Verify the key list is displayed
        onView(withId(R.id.songKeyList))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCanEnterSongTitle() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        EspressoTestUtils.shortWait()

        // Enter a song title
        onView(withId(R.id.songTitleEditText))
            .perform(typeText("Test Song Title"), closeSoftKeyboard())

        // Verify text was entered
        onView(withId(R.id.songTitleEditText))
            .check(matches(withText("Test Song Title")))
    }

    @Test
    fun testCanTapKeyList() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        EspressoTestUtils.shortWait()

        // Tap the list; a row selects without leaving the editor
        onView(withId(R.id.songKeyList))
            .perform(click())

        EspressoTestUtils.shortWait()

        onView(withId(R.id.songKeyList))
            .check(matches(isDisplayed()))
    }

    // ==================== Integration Tests ====================

    @Test
    fun testBottomNavigationHasSongsItem() {
        // Verify the songs item exists in bottom navigation
        onView(withId(R.id.songs_item))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCanNavigateBetweenTabs() {
        // Navigate to songs tab
        onView(withId(R.id.songs_item))
            .perform(click())
        EspressoTestUtils.shortWait()

        // Verify we're on songs tab
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))

        // Navigate to pitch pipe tab
        onView(withId(R.id.pitchpipe_item))
            .perform(click())
        EspressoTestUtils.shortWait()

        // Navigate back to songs tab
        onView(withId(R.id.songs_item))
            .perform(click())
        EspressoTestUtils.shortWait()

        // Verify we're back on songs tab
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))
    }
}
