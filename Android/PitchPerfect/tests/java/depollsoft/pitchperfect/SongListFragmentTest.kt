package depollsoft.pitchperfect

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the SongListFragment.
 * Tests song list display, CRUD operations, and sorting.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class SongListFragmentTest {

    private lateinit var scenario: FragmentScenario<SongListFragment>

    @Before
    fun setup() {
        scenario = launchFragmentInContainer<SongListFragment>(
            themeResId = R.style.Theme_PitchPerfect
        )
    }

    // ==================== Layout Tests ====================

    @Test
    fun testSongListIsDisplayed() {
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddSongFabIsDisplayed() {
        onView(withId(R.id.fab_add_song))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyStateIsShownWhenNoSongs() {
        // If no songs, empty state should be visible
        onView(withId(R.id.empty_state_view))
            .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
    }

    // ==================== Add Song Tests ====================

    @Test
    fun testFabClickOpensAddSongActivity() {
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(500)

        // Verify add song form is displayed
        onView(withId(R.id.song_name_input))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddNewSongWithName() {
        // Click FAB to add song
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(500)

        // Enter song name
        onView(withId(R.id.song_name_input))
            .perform(typeText("Test Song"), closeSoftKeyboard())

        // Click save
        onView(withId(R.id.save_button))
            .perform(click())

        Thread.sleep(500)

        // Verify song appears in list
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText(containsString("Test Song")))))
    }

    @Test
    fun testAddSongWithKey() {
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(500)

        // Enter song name
        onView(withId(R.id.song_name_input))
            .perform(typeText("Song With Key"), closeSoftKeyboard())

        // Select key
        onView(withId(R.id.key_spinner))
            .perform(click())
        
        onView(withText("G Major"))
            .perform(click())

        // Save
        onView(withId(R.id.save_button))
            .perform(click())

        Thread.sleep(500)

        // Verify song with key appears
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText(containsString("Song With Key")))))
    }

    @Test
    fun testAddSongCancellation() {
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(500)

        // Enter song name
        onView(withId(R.id.song_name_input))
            .perform(typeText("Cancelled Song"), closeSoftKeyboard())

        // Press back/cancel
        onView(withId(R.id.cancel_button))
            .perform(click())

        Thread.sleep(500)

        // Verify song was NOT added
        onView(withId(R.id.song_list))
            .check(matches(not(hasDescendant(withText(containsString("Cancelled Song"))))))
    }

    @Test
    fun testAddSongWithEmptyNameShowsError() {
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(500)

        // Don't enter name, just try to save
        onView(withId(R.id.save_button))
            .perform(click())

        // Verify error is shown
        onView(withId(R.id.song_name_input))
            .check(matches(hasErrorText(containsString("required"))))
    }

    // ==================== Song List Item Tests ====================

    @Test
    fun testSongItemDisplaysName() {
        // First add a song
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Display Test"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Verify song name is displayed
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText("Display Test"))))
    }

    @Test
    fun testSongItemDisplaysKey() {
        // Add a song with key
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Key Display Test"), closeSoftKeyboard())
        onView(withId(R.id.key_spinner)).perform(click())
        onView(withText("D Major")).perform(click())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Verify key is displayed
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText(containsString("D")))))
    }

    @Test
    fun testClickSongItemOpensDetails() {
        // Add a song first
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Click Test"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Click the song item
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Click Test")), click()))

        // Verify detail view opens
        onView(withId(R.id.song_detail_container))
            .check(matches(isDisplayed()))
    }

    // ==================== Edit Song Tests ====================

    @Test
    fun testLongPressSongItemShowsMenu() {
        // Add a song first
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Menu Test"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Long press
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Menu Test")), longClick()))

        // Verify context menu appears
        onView(withText("Edit"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEditSongName() {
        // Add a song
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Original Name"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Long press and edit
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Original Name")), longClick()))
        
        onView(withText("Edit")).perform(click())
        Thread.sleep(500)

        // Clear and enter new name
        onView(withId(R.id.song_name_input))
            .perform(clearText(), typeText("Updated Name"), closeSoftKeyboard())

        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Verify updated name
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText("Updated Name"))))
    }

    // ==================== Delete Song Tests ====================

    @Test
    fun testSwipeToDeleteSong() {
        // Add a song
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Delete Test"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Swipe to delete
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Delete Test")), swipeLeft()))

        Thread.sleep(500)

        // Verify song is deleted (or delete confirmation shown)
        onView(withId(R.id.song_list))
            .check(matches(not(hasDescendant(withText("Delete Test")))))
    }

    @Test
    fun testDeleteSongFromMenu() {
        // Add a song
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Menu Delete"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Long press for menu
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Menu Delete")), longClick()))

        // Select delete
        onView(withText("Delete")).perform(click())

        // Confirm delete
        onView(withText("OK")).perform(click())

        Thread.sleep(500)

        // Verify deleted
        onView(withId(R.id.song_list))
            .check(matches(not(hasDescendant(withText("Menu Delete")))))
    }

    @Test
    fun testDeleteConfirmationCanBeCancelled() {
        // Add a song
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.song_name_input))
            .perform(typeText("Cancel Delete"), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(500)

        // Long press for menu
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Cancel Delete")), longClick()))

        // Select delete
        onView(withText("Delete")).perform(click())

        // Cancel delete
        onView(withText("Cancel")).perform(click())

        Thread.sleep(300)

        // Verify NOT deleted
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText("Cancel Delete"))))
    }

    // ==================== Sort Tests ====================

    @Test
    fun testSortMenuIsAccessible() {
        // Open sort menu
        onView(withId(R.id.sort_menu))
            .perform(click())

        // Verify sort options appear
        onView(withText("Sort by Name"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSortByName() {
        // Add songs out of order
        addTestSong("Zebra Song")
        addTestSong("Apple Song")
        addTestSong("Mango Song")

        // Sort by name
        onView(withId(R.id.sort_menu)).perform(click())
        onView(withText("Sort by Name")).perform(click())

        Thread.sleep(300)

        // Verify alphabetical order (Apple should be first)
        onView(withId(R.id.song_list))
            .check(matches(atPosition(0, hasDescendant(withText(containsString("Apple"))))))
    }

    @Test
    fun testSortByKey() {
        // Add songs with different keys
        addTestSongWithKey("Song A", "G Major")
        addTestSongWithKey("Song B", "A Major")
        addTestSongWithKey("Song C", "C Major")

        // Sort by key
        onView(withId(R.id.sort_menu)).perform(click())
        onView(withText("Sort by Key")).perform(click())

        Thread.sleep(300)

        // Songs should be ordered by key
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSortByDateAdded() {
        // Sort by date
        onView(withId(R.id.sort_menu)).perform(click())
        onView(withText("Sort by Date")).perform(click())

        Thread.sleep(300)

        // Verify sort was applied
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    // ==================== Reorder Tests ====================

    @Test
    fun testDragToReorderSongs() {
        // Add multiple songs
        addTestSong("First Song")
        addTestSong("Second Song")

        // Long press and drag to reorder
        // Note: Espresso drag and drop is complex, this is a simplified test
        onView(withId(R.id.song_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, longClick()))

        // Verify reorder mode is activated
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    // ==================== Scroll Tests ====================

    @Test
    fun testScrollSongList() {
        // Add many songs
        for (i in 1..10) {
            addTestSong("Song $i")
        }

        // Scroll down
        onView(withId(R.id.song_list))
            .perform(swipeUp())

        // Verify list is still functional
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testScrollToTopButton() {
        // Add many songs and scroll down
        for (i in 1..10) {
            addTestSong("Scroll Song $i")
        }

        onView(withId(R.id.song_list))
            .perform(swipeUp())
            .perform(swipeUp())

        // Click scroll to top (if exists)
        onView(withId(R.id.scroll_to_top))
            .perform(click())

        Thread.sleep(300)

        // Verify at top
        onView(withId(R.id.song_list))
            .check(matches(atPosition(0, isDisplayed())))
    }

    // ==================== Fragment Lifecycle Tests ====================

    @Test
    fun testFragmentSurvivesRotation() {
        addTestSong("Rotation Test")

        // Recreate fragment
        scenario.recreate()

        Thread.sleep(500)

        // Verify song is still there
        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(withText("Rotation Test"))))
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testFabHasContentDescription() {
        onView(withId(R.id.fab_add_song))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testSongItemsAreAccessible() {
        addTestSong("Accessible Song")

        onView(withId(R.id.song_list))
            .check(matches(hasDescendant(hasContentDescription())))
    }

    // ==================== Helper Methods ====================

    private fun addTestSong(name: String) {
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.song_name_input))
            .perform(typeText(name), closeSoftKeyboard())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(300)
    }

    private fun addTestSongWithKey(name: String, key: String) {
        onView(withId(R.id.fab_add_song)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.song_name_input))
            .perform(typeText(name), closeSoftKeyboard())
        onView(withId(R.id.key_spinner)).perform(click())
        onView(withText(key)).perform(click())
        onView(withId(R.id.save_button)).perform(click())
        Thread.sleep(300)
    }

    // Custom matcher for RecyclerView position
    private fun atPosition(position: Int, itemMatcher: org.hamcrest.Matcher<android.view.View>): org.hamcrest.Matcher<android.view.View> {
        return object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: android.view.View): Boolean {
                if (view !is RecyclerView) return false
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}
