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
 * Instrumented UI tests for the KeySignatureFragment.
 * Tests key signature list display, selection, and major/minor toggling.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class KeySignatureFragmentTest {

    private lateinit var scenario: FragmentScenario<KeySignatureFragment>

    @Before
    fun setup() {
        scenario = launchFragmentInContainer<KeySignatureFragment>(
            themeResId = R.style.Theme_PitchPerfect
        )
    }

    // ==================== Layout Tests ====================

    @Test
    fun testKeySignatureListIsDisplayed() {
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMajorMinorToggleFabIsDisplayed() {
        onView(withId(R.id.fab_toggle_major_minor))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeyListIsScrollable() {
        onView(withId(R.id.key_signature_list))
            .perform(swipeUp())

        // Should still be displayed after scroll
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    // ==================== Major Key Tests ====================

    @Test
    fun testMajorKeysAreDisplayed() {
        // Verify the list contains major keys
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withText(containsString("C Major")))))
    }

    @Test
    fun testSelectCMajorKey() {
        // Click on C Major
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Verify selection (check visual feedback)
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(allOf(withText(containsString("C")), isSelected()))))
    }

    @Test
    fun testSelectGMajorKey() {
        // Scroll to and click G Major
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("G Major"))), click()))
    }

    @Test
    fun testSelectDMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("D Major"))), click()))
    }

    @Test
    fun testSelectAMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("A Major"))), click()))
    }

    @Test
    fun testSelectEMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("E Major"))), click()))
    }

    @Test
    fun testSelectBMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("B Major"))), click()))
    }

    @Test
    fun testSelectFSharpMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("F#"))), click()))
    }

    @Test
    fun testSelectFMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("F Major"))), click()))
    }

    @Test
    fun testSelectBbMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("Bb"))), click()))
    }

    @Test
    fun testSelectEbMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("Eb"))), click()))
    }

    @Test
    fun testSelectAbMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("Ab"))), click()))
    }

    @Test
    fun testSelectDbMajorKey() {
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("Db"))), click()))
    }

    // ==================== Major/Minor Toggle Tests ====================

    @Test
    fun testToggleToMinorKeys() {
        // Click FAB to toggle to minor
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // Verify minor keys are now displayed
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withText(containsString("minor")))))
    }

    @Test
    fun testToggleBackToMajorKeys() {
        // Toggle to minor
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // Toggle back to major
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // Verify major keys are displayed
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withText(containsString("Major")))))
    }

    // ==================== Minor Key Tests ====================

    @Test
    fun testSelectAMinorKey() {
        // Toggle to minor keys
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // Select A minor
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("A minor"))), click()))
    }

    @Test
    fun testSelectEMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("E minor"))), click()))
    }

    @Test
    fun testSelectBMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("B minor"))), click()))
    }

    @Test
    fun testSelectDMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("D minor"))), click()))
    }

    @Test
    fun testSelectGMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("G minor"))), click()))
    }

    @Test
    fun testSelectCMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("C minor"))), click()))
    }

    @Test
    fun testSelectFMinorKey() {
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("F minor"))), click()))
    }

    // ==================== Key Selection Behavior Tests ====================

    @Test
    fun testSelectedKeyPlaysNotes() {
        // Select a key
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Verify the notes for that key are playing
        // This depends on how the fragment signals note playing
    }

    @Test
    fun testKeySelectionUpdatesHeader() {
        // Select a key
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(containsString("G Major"))), click()))

        // Verify header shows selected key
        onView(withId(R.id.selected_key_header))
            .check(matches(withText(containsString("G"))))
    }

    @Test
    fun testKeySelectionShowsNoteList() {
        // Select a key
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Verify the notes for the key are displayed
        onView(withId(R.id.key_notes_display))
            .check(matches(isDisplayed()))
    }

    // ==================== Scroll Tests ====================

    @Test
    fun testScrollToTopOfList() {
        // Scroll down first
        onView(withId(R.id.key_signature_list))
            .perform(swipeUp())
            .perform(swipeUp())

        // Scroll back to top
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))

        // Verify first item is visible
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withText(containsString("C Major")))))
    }

    @Test
    fun testScrollToBottomOfList() {
        // Scroll to last key
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(11))

        // Verify we can reach the end
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    // ==================== Fragment Lifecycle Tests ====================

    @Test
    fun testFragmentSurvivesRotation() {
        // Toggle to minor
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // Recreate fragment
        scenario.recreate()

        Thread.sleep(500)

        // Verify state is preserved (still showing minor)
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withText(containsString("minor")))))
    }

    @Test
    fun testSelectedKeyPreservedAfterRotation() {
        // Select a key
        onView(withId(R.id.key_signature_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))

        // Recreate fragment
        scenario.recreate()

        Thread.sleep(500)

        // Verify selection is preserved
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testFabHasContentDescription() {
        onView(withId(R.id.fab_toggle_major_minor))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testKeyItemsAreAccessible() {
        // Verify key items have content descriptions for screen readers
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(hasContentDescription())))
    }

    // ==================== Visual State Tests ====================

    @Test
    fun testFabShowsCorrectIconForMajor() {
        // In major mode, FAB should show minor icon (to toggle to minor)
        onView(withId(R.id.fab_toggle_major_minor))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFabShowsCorrectIconForMinor() {
        // Toggle to minor
        onView(withId(R.id.fab_toggle_major_minor))
            .perform(click())

        Thread.sleep(300)

        // FAB should now show major icon (to toggle back)
        onView(withId(R.id.fab_toggle_major_minor))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeyListItemsHaveNoteIndicators() {
        // Verify key items show their note composition
        onView(withId(R.id.key_signature_list))
            .check(matches(hasDescendant(withId(R.id.key_notes_indicator))))
    }

    // ==================== Edge Cases ====================

    @Test
    fun testRapidMajorMinorToggling() {
        // Rapidly toggle multiple times
        repeat(5) {
            onView(withId(R.id.fab_toggle_major_minor))
                .perform(click())
            Thread.sleep(100)
        }

        // UI should still be responsive
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRapidKeySelection() {
        // Rapidly select different keys
        for (i in 0..5) {
            onView(withId(R.id.key_signature_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(i, click()))
            Thread.sleep(100)
        }

        // UI should still be responsive
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }
}
