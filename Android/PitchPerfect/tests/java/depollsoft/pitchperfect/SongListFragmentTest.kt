package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onData
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
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
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
 * - songKeySpinner: Spinner for key selection in add dialog
 * - songTitleTextView: Song title in list item
 * - songKeyTextView: Key signature in list item
 * - songs_item: Bottom nav menu item for songs tab
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SongListFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    /**
     * Navigate to the songs tab using the bottom navigation.
     */
    private fun navigateToSongsTab() {
        onView(withId(R.id.songs_item))
            .perform(click())
        Thread.sleep(500)
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testNavigateToSongsTab() {
        // Click on songs tab in bottom navigation
        onView(withId(R.id.songs_item))
            .perform(click())

        Thread.sleep(500)

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

        Thread.sleep(500)

        // Verify the add song dialog is displayed with title input
        onView(withId(R.id.songTitleEditText))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddSongDialogHasKeySpinner() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        Thread.sleep(500)

        // Verify the key spinner is displayed
        onView(withId(R.id.songKeySpinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCanEnterSongTitle() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        Thread.sleep(500)

        // Enter a song title
        onView(withId(R.id.songTitleEditText))
            .perform(typeText("Test Song Title"), closeSoftKeyboard())

        // Verify text was entered
        onView(withId(R.id.songTitleEditText))
            .check(matches(withText("Test Song Title")))
    }

    @Test
    fun testCanSelectKeyFromSpinner() {
        navigateToSongsTab()

        // Click the FAB to open add song dialog
        onView(withId(R.id.addSongButton))
            .perform(click())

        Thread.sleep(500)

        // Click on the key spinner to open it
        onView(withId(R.id.songKeySpinner))
            .perform(click())

        Thread.sleep(300)

        // Select an item from the spinner (first item after "Select Key")
        onData(anything())
            .atPosition(1)
            .perform(click())

        // Verify spinner is still displayed (selection completed)
        onView(withId(R.id.songKeySpinner))
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
        Thread.sleep(500)

        // Verify we're on songs tab
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))

        // Navigate to pitch pipe tab
        onView(withId(R.id.pitchpipe_item))
            .perform(click())
        Thread.sleep(500)

        // Navigate back to songs tab
        onView(withId(R.id.songs_item))
            .perform(click())
        Thread.sleep(500)

        // Verify we're back on songs tab
        onView(withId(R.id.addSongButton))
            .check(matches(isDisplayed()))
    }
}
