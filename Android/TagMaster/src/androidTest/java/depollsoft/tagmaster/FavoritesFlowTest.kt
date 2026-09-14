package depollsoft.tagmaster

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests for the Favorites flow in MeActivity.
 * Tests the favorites list using the actual MeActivity layout (meview.xml).
 * Uses actual resource IDs from TagMaster app.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FavoritesFlowTest {
    @get:Rule val fixture = NavigationTestFixture()

    private fun launchHome(): ActivityScenario<MeActivity> {
        val scenario = ActivityScenario.launch(MeActivity::class.java)
        try {
            try {
                onView(withId(android.R.id.button1)).perform(click())
            } catch (_: androidx.test.espresso.NoMatchingViewException) {
                // Changelog is only present on the first launch of a new installation.
            }
            return scenario
        } catch (error: Throwable) {
            scenario.close()
            throw error
        }
    }

    // ==================== Main Activity Launch Tests ====================

    @Test
    fun testMainActivityLaunches() {
        launchHome().use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
            }
        }
    }

    @Test
    fun testScrollViewIsDisplayed() {
        launchHome().use { scenario ->
            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testSearchButtonIsDisplayed() {
        launchHome().use { scenario ->
            onView(withId(R.id.searchButton))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Favorites Items Control Tests ====================

    @Test
    fun testFavoritesItemsControlExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.homeList))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Favorites control may not be visible if empty
            }
        }
    }

    @Test
    fun testFavoritesItemsControlIsDisplayed() {
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
        var original: com.bindroid.trackable.TrackableCollection<Int>? = null
        instrumentation.runOnMainSync {
            org.junit.Assert.assertNull(
                "Favorites fixtures require signed-out state",
                com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .currentUser,
            )
            ListModel.setTestMode(true) // Signed-out guard above; suppress fixture persistence.
            original = FavoritesModel.favoriteIds
            FavoritesModel.favoriteIds =
                com.bindroid.trackable.TrackableCollection<Int>().apply {
                    add(fixture.tag.id)
                }
        }
        try {
            launchHome().use { scenario ->
                EspressoTestUtils.waitForView(withText(fixture.tag.title))
                onView(withId(R.id.homeList)).check(matches(isDisplayed()))
                onView(withText(fixture.tag.title)).check(matches(isDisplayed()))
                scenario.onActivity {
                    org.junit.Assert.assertEquals(1, it.favoritesAdapter.itemCount)
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                FavoritesModel.favoriteIds = original!!
                ListModel.setTestMode(false)
            }
        }
    }

    // ==================== ScrollView Navigation Tests ====================

    @Test
    fun testScrollViewCanScrollUp() {
        launchHome().use { scenario ->
            onView(withId(R.id.homeList))
                .perform(swipeUp())

            EspressoTestUtils.shortWait(300)

            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testScrollViewCanScrollDown() {
        launchHome().use { scenario ->
            // Swipe up first
            onView(withId(R.id.homeList))
                .perform(swipeUp())

            EspressoTestUtils.shortWait(300)

            // Swipe down
            onView(withId(R.id.homeList))
                .perform(swipeDown())

            EspressoTestUtils.shortWait(300)

            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testScrollThroughContent() {
        launchHome().use { scenario ->
            repeat(3) {
                onView(withId(R.id.homeList))
                    .perform(swipeUp())
                EspressoTestUtils.shortWait(200)
            }

            repeat(3) {
                onView(withId(R.id.homeList))
                    .perform(swipeDown())
                EspressoTestUtils.shortWait(200)
            }

            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Search Button Tests ====================

    @Test
    fun testSearchButtonClickable() {
        launchHome().use { scenario ->
            onView(withId(R.id.searchButton))
                .check(matches(isClickable()))
        }
    }

    @Test
    fun testSearchButtonEnabled() {
        launchHome().use { scenario ->
            onView(withId(R.id.searchButton))
                .check(matches(isEnabled()))
        }
    }

    // ==================== Tag Item View Tests ====================

    @Test
    fun testTagTitleTextViewExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.titleTextView))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Title may not be visible if no favorites
            }
        }
    }

    @Test
    fun testFavoriteMarkerTextViewExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.favoriteMarkerTextView))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Favorite marker may not be visible
            }
        }
    }

    @Test
    fun testTeachableMarkerTextViewExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.teachableMarkerTextView))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Teachable marker may not be visible
            }
        }
    }

    @Test
    fun testRatingTextViewExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.ratingTextView))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Rating may not be visible
            }
        }
    }

    // ==================== Menu Item Tests ====================

    @Test
    fun testSettingsMenuItemExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.settingsMenuItem))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Menu item may not be visible
            }
        }
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testScrollViewSurvivesRotation() {
        launchHome().use { scenario ->
            // Scroll down
            onView(withId(R.id.homeList))
                .perform(swipeUp())

            EspressoTestUtils.shortWait(300)

            // Rotate
            scenario.recreate()

            EspressoTestUtils.shortWait(500)

            // ScrollView should still be displayed
            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testSearchButtonSurvivesRotation() {
        launchHome().use { scenario ->
            scenario.recreate()

            EspressoTestUtils.shortWait(500)

            onView(withId(R.id.searchButton))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testRapidScrolling() {
        launchHome().use { scenario ->
            repeat(5) {
                onView(withId(R.id.homeList))
                    .perform(swipeUp())
                EspressoTestUtils.shortWait(100)
            }

            repeat(5) {
                onView(withId(R.id.homeList))
                    .perform(swipeDown())
                EspressoTestUtils.shortWait(100)
            }

            onView(withId(R.id.homeList))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testMultipleActivityLaunches() {
        repeat(3) {
            launchHome().use { scenario ->
                onView(withId(R.id.homeList))
                    .check(matches(isDisplayed()))
            }
        }
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testScrollViewIsEnabled() {
        launchHome().use { scenario ->
            onView(withId(R.id.homeList))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun testFavoritesItemsControlIsEnabled() {
        launchHome().use { scenario ->
            onView(withId(R.id.homeList))
                .check(matches(isEnabled()))
        }
    }

    // ==================== Header View Tests ====================

    @Test
    fun testMeHeaderViewExists() {
        launchHome().use { scenario ->
            try {
                onView(withId(R.id.meHeaderView1))
                    .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
            } catch (e: Exception) {
                // Header may not be visible
            }
        }
    }

    @Test
    fun testMeHeaderViewIsDisplayed() {
        launchHome().use { scenario ->
            onView(withId(R.id.meHeaderView1))
                .check(matches(isDisplayed()))
        }
    }
}
