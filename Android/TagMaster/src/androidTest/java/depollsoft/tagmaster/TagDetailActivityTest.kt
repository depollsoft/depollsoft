package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import depollsoft.tagmaster.NavigationTestFixture.Companion.assertPage
import depollsoft.tagmaster.NavigationTestFixture.Companion.selectTab
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Every test launches real Detail with a valid cached tag and selects its owning fragment. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TagDetailActivityTest {
    private val fixture = NavigationTestFixture()
    private val activityRule = ActivityScenarioRule<TagDetailActivity>(fixture.detailIntent())
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(activityRule)

    @Before fun awaitCachedTag() {
        EspressoTestUtils.waitForView(withId(R.id.tabLayout))
        activityRule.scenario.onActivity {
            assertEquals(fixture.tag.id, it.tagId)
            assertEquals(fixture.tag.title, it.tag!!.title)
            assertFalse(it.isLoading)
        }
    }

    @Test fun testActivityLaunches() { activityRule.scenario.onActivity { assertEquals(fixture.tag.title, it.title.toString()) } }
    @Test fun testViewPagerIsDisplayed() { onView(withId(R.id.viewPager)).check(matches(isDisplayed())) }
    @Test fun testDetailTabsAreDisplayed() { onView(withId(R.id.tabLayout)).check(matches(isDisplayed())); assertPage(0) }
    @Test fun testKeyButtonExists() { onView(withId(R.id.playKeyNoteButton)).perform(scrollTo()).check(matches(withText("C"))) }
    @Test fun testKeyButtonIsClickable() { onView(withId(R.id.playKeyNoteButton)).perform(scrollTo()).check(matches(isClickable())) }
    @Test fun testTitleTextViewExists() { onView(allOf(withId(R.id.titleTextView), isDisplayed())).check(matches(withText(fixture.tag.title))) }
    @Test fun testArrangedByTextViewExists() { selectTab(R.string.details, 1); onView(withId(R.id.arrangedByTextView)).perform(scrollTo()).check(matches(withText("Fixture arranger"))) }
    @Test fun testYearArrangedTextViewExists() { selectTab(R.string.details, 1); onView(withId(R.id.yearArrangedTextView)).perform(scrollTo()).check(matches(withText("2026"))) }
    @Test fun testClassicTagTextViewExists() { onView(withId(R.id.classicTagTextView)).perform(scrollTo()).check(matches(withText("17"))) }
    @Test fun testVideoListExists() {
        selectTab(R.string.videos, 3)
        onView(withId(R.id.videoList)).check(matches(isDisplayed()))
        onView(withId(R.id.videoList)).check { view, error ->
            if (error != null) throw error
            assertEquals(1, (view as android.widget.ListView).adapter.count)
        }
    }
    @Test fun testVideoPreviewExists() {
        selectTab(R.string.videos, 3)
        onView(allOf(withId(R.id.videoPreview), isDescendantOfA(withId(R.id.videoList)))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.sungByTextView), isDescendantOfA(withId(R.id.videoList)))).check(matches(withText("Fixture quartet")))
    }
    @Test fun testBalanceSeekBarExists() { tracks(); onView(withId(R.id.balanceSeekBar)).perform(scrollTo()).check(matches(isDisplayed())) }
    @Test fun testTrackNotesTextViewExists() { tracks(); onView(withId(R.id.trackNotesTextView)).perform(scrollTo()).check(matches(withText(fixture.tag.recordingMethod))) }
    @Test fun testAllPartsButtonExists() { partVisible(R.id.allPartsButton) }
    @Test fun testAllPartsButtonIsClickable() { selectPart(R.id.allPartsButton, fixture.tag.allPartsTrackUri!!.uri) }
    @Test fun testLeadButtonExists() { partVisible(R.id.leadButton) }
    @Test fun testLeadButtonIsClickable() { selectPart(R.id.leadButton, fixture.tag.leadTrackUri!!.uri) }
    @Test fun testTenorButtonExists() { partVisible(R.id.tenorButton) }
    @Test fun testTenorButtonIsClickable() { selectPart(R.id.tenorButton, fixture.tag.tenorTrackUri!!.uri) }
    @Test fun testBariButtonExists() { partVisible(R.id.bariButton) }
    @Test fun testBariButtonIsClickable() { selectPart(R.id.bariButton, fixture.tag.baritoneTrackUri!!.uri) }
    @Test fun testBassButtonExists() { partVisible(R.id.bassButton) }
    @Test fun testBassButtonIsClickable() { selectPart(R.id.bassButton, fixture.tag.bassTrackUri!!.uri) }
    @Test fun testSwipeViewPagerLeft() {
        onView(withId(R.id.viewPager)).perform(swipeLeft())
        assertPage(1)
        onView(withId(R.id.arrangedByTextView)).check(matches(isDisplayed()))
    }
    @Test fun testSwipeViewPagerRight() {
        selectTab(R.string.details, 1)
        onView(withId(R.id.viewPager)).perform(swipeRight())
        assertPage(0)
        onView(withId(R.id.tagIdTextView)).check(matches(withText(fixture.tag.id.toString())))
    }
    @Test fun testMultipleViewPagerSwipes() {
        for (page in 1..3) { onView(withId(R.id.viewPager)).perform(swipeLeft()); assertPage(page) }
        for (page in 2 downTo 0) { onView(withId(R.id.viewPager)).perform(swipeRight()); assertPage(page) }
        onView(withId(R.id.tagIdTextView)).check(matches(withText(fixture.tag.id.toString())))
    }
    @Test fun testActivitySurvivesRecreation() {
        activityRule.scenario.recreate()
        awaitCachedTag()
        onView(withId(R.id.tagIdTextView)).check(matches(withText(fixture.tag.id.toString())))
    }
    @Test fun testSelectedTabPreservedAfterRecreation() {
        selectTab(R.string.details, 1)
        activityRule.scenario.recreate()
        awaitCachedTag()
        assertPage(1)
        onView(withId(R.id.arrangedByTextView)).check(matches(withText("Fixture arranger")))
    }
    @Test fun testViewPagerIsEnabled() { onView(withId(R.id.viewPager)).check(matches(isEnabled())) }
    @Test fun testDetailTabsAreEnabled() { onView(withId(R.id.tabLayout)).check(matches(isEnabled())); selectTab(R.string.tracks, 2) }
    @Test fun testRepeatedTabChangesReachRequestedPage() {
        repeat(5) { selectTab(R.string.videos, 3); selectTab(R.string.summary, 0) }
        onView(withId(R.id.tagIdTextView)).check(matches(withText(fixture.tag.id.toString())))
    }

    private fun tracks() { selectTab(R.string.tracks, 2) }
    private fun partVisible(id: Int) { tracks(); onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed())) }
    private fun selectPart(id: Int, uri: String) {
        tracks()
        onView(withId(id)).perform(scrollTo(), click()).check(matches(isChecked()))
        activityRule.scenario.onActivity { assertEquals(uri, it.findViewById<MediaPlayerView>(R.id.mediaPlayer).remoteLocation.uri) }
    }
}
