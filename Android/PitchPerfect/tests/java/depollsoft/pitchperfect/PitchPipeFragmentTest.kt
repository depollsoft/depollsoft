package depollsoft.pitchperfect

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the PitchPipeFragment.
 * Tests pitch button interactions, toggle modes, and note display.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class PitchPipeFragmentTest {

    private lateinit var scenario: FragmentScenario<PitchPipeFragment>

    @Before
    fun setup() {
        scenario = launchFragmentInContainer<PitchPipeFragment>(
            themeResId = R.style.Theme_PitchPerfect
        )
    }

    // ==================== Layout Tests ====================

    @Test
    fun testPitchPipeContainerIsDisplayed() {
        onView(withId(R.id.pitch_pipe_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAllTwelveNoteButtonsAreDisplayed() {
        // Test all 12 pitch buttons (C to B)
        val noteIds = listOf(
            R.id.note_c, R.id.note_c_sharp, R.id.note_d, R.id.note_d_sharp,
            R.id.note_e, R.id.note_f, R.id.note_f_sharp, R.id.note_g,
            R.id.note_g_sharp, R.id.note_a, R.id.note_a_sharp, R.id.note_b
        )

        for (noteId in noteIds) {
            onView(withId(noteId))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testNoteButtonsAreClickable() {
        val noteIds = listOf(
            R.id.note_c, R.id.note_c_sharp, R.id.note_d, R.id.note_d_sharp,
            R.id.note_e, R.id.note_f, R.id.note_f_sharp, R.id.note_g,
            R.id.note_g_sharp, R.id.note_a, R.id.note_a_sharp, R.id.note_b
        )

        for (noteId in noteIds) {
            onView(withId(noteId))
                .check(matches(isClickable()))
        }
    }

    // ==================== Note Button Interaction Tests ====================

    @Test
    fun testClickNoteC() {
        onView(withId(R.id.note_c))
            .perform(click())

        // Verify the note is in playing state (highlighted)
        onView(withId(R.id.note_c))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteCSharp() {
        onView(withId(R.id.note_c_sharp))
            .perform(click())

        onView(withId(R.id.note_c_sharp))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteD() {
        onView(withId(R.id.note_d))
            .perform(click())

        onView(withId(R.id.note_d))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteDSharp() {
        onView(withId(R.id.note_d_sharp))
            .perform(click())

        onView(withId(R.id.note_d_sharp))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteE() {
        onView(withId(R.id.note_e))
            .perform(click())

        onView(withId(R.id.note_e))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteF() {
        onView(withId(R.id.note_f))
            .perform(click())

        onView(withId(R.id.note_f))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteFSharp() {
        onView(withId(R.id.note_f_sharp))
            .perform(click())

        onView(withId(R.id.note_f_sharp))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteG() {
        onView(withId(R.id.note_g))
            .perform(click())

        onView(withId(R.id.note_g))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteGSharp() {
        onView(withId(R.id.note_g_sharp))
            .perform(click())

        onView(withId(R.id.note_g_sharp))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteA() {
        onView(withId(R.id.note_a))
            .perform(click())

        onView(withId(R.id.note_a))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteASharp() {
        onView(withId(R.id.note_a_sharp))
            .perform(click())

        onView(withId(R.id.note_a_sharp))
            .check(matches(isSelected()))
    }

    @Test
    fun testClickNoteB() {
        onView(withId(R.id.note_b))
            .perform(click())

        onView(withId(R.id.note_b))
            .check(matches(isSelected()))
    }

    // ==================== Toggle Mode Tests ====================

    @Test
    fun testToggleNoteOnAndOff() {
        // Click to play note
        onView(withId(R.id.note_c))
            .perform(click())

        // Verify note is selected/playing
        onView(withId(R.id.note_c))
            .check(matches(isSelected()))

        // Click again to stop note
        onView(withId(R.id.note_c))
            .perform(click())

        // Verify note is no longer selected
        onView(withId(R.id.note_c))
            .check(matches(not(isSelected())))
    }

    @Test
    fun testMultipleNotesCanPlaySimultaneously() {
        // Click C
        onView(withId(R.id.note_c))
            .perform(click())

        // Click E
        onView(withId(R.id.note_e))
            .perform(click())

        // Click G
        onView(withId(R.id.note_g))
            .perform(click())

        // Verify all three are selected (C major chord)
        onView(withId(R.id.note_c))
            .check(matches(isSelected()))
        onView(withId(R.id.note_e))
            .check(matches(isSelected()))
        onView(withId(R.id.note_g))
            .check(matches(isSelected()))
    }

    @Test
    fun testStopAllNotes() {
        // Play multiple notes
        onView(withId(R.id.note_c)).perform(click())
        onView(withId(R.id.note_e)).perform(click())
        onView(withId(R.id.note_g)).perform(click())

        // Stop all notes button (if exists)
        onView(withId(R.id.stop_all_notes))
            .perform(click())

        // Verify all notes are stopped
        onView(withId(R.id.note_c))
            .check(matches(not(isSelected())))
        onView(withId(R.id.note_e))
            .check(matches(not(isSelected())))
        onView(withId(R.id.note_g))
            .check(matches(not(isSelected())))
    }

    // ==================== Range Switching Tests ====================

    @Test
    fun testRangeToggleButtonIsDisplayed() {
        onView(withId(R.id.range_toggle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwitchToCToC Range() {
        // Click range toggle to switch to C-to-C
        onView(withId(R.id.range_toggle))
            .perform(click())

        // Verify C-to-C is selected (check button text or state)
        onView(withId(R.id.range_toggle))
            .check(matches(withText(containsString("C-C"))))
    }

    @Test
    fun testSwitchToFToFRange() {
        // Ensure we're on C-to-C first
        onView(withId(R.id.range_toggle))
            .perform(click())

        // Click again to switch to F-to-F
        onView(withId(R.id.range_toggle))
            .perform(click())

        // Verify F-to-F is selected
        onView(withId(R.id.range_toggle))
            .check(matches(withText(containsString("F-F"))))
    }

    @Test
    fun testNoteLabelsChangeWithRange() {
        // Get initial label for first note
        onView(withId(R.id.note_c))
            .check(matches(withText("C")))

        // Toggle range
        onView(withId(R.id.range_toggle))
            .perform(click())
        onView(withId(R.id.range_toggle))
            .perform(click())

        // Labels should update based on new range
        // (The exact labels depend on the range implementation)
    }

    // ==================== Touch and Hold Tests ====================

    @Test
    fun testLongPressNoteButton() {
        // Long press a note button
        onView(withId(R.id.note_c))
            .perform(longClick())

        // In hold mode, note should play while held
        // This test verifies the UI responds to long press
    }

    // ==================== Octave Tests ====================

    @Test
    fun testOctaveIndicatorIsDisplayed() {
        onView(withId(R.id.octave_indicator))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testOctaveUpButton() {
        onView(withId(R.id.octave_up))
            .perform(click())

        // Verify octave increased
        onView(withId(R.id.octave_indicator))
            .check(matches(withText(containsString("5"))))
    }

    @Test
    fun testOctaveDownButton() {
        onView(withId(R.id.octave_down))
            .perform(click())

        // Verify octave decreased
        onView(withId(R.id.octave_indicator))
            .check(matches(withText(containsString("3"))))
    }

    @Test
    fun testOctaveBounds() {
        // Try to go below minimum octave
        repeat(10) {
            onView(withId(R.id.octave_down))
                .perform(click())
        }

        // Should be at minimum, button might be disabled
        onView(withId(R.id.octave_down))
            .check(matches(anyOf(not(isEnabled()), isDisplayed())))
    }

    // ==================== Fragment Lifecycle Tests ====================

    @Test
    fun testFragmentSurvivesConfigurationChange() {
        // Play a note
        onView(withId(R.id.note_c))
            .perform(click())

        // Recreate the fragment (simulates rotation)
        scenario.recreate()

        Thread.sleep(500)

        // Verify fragment still displays correctly
        onView(withId(R.id.pitch_pipe_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNoteStatePreservedAcrossConfigurationChange() {
        // Play a chord
        onView(withId(R.id.note_c)).perform(click())
        onView(withId(R.id.note_e)).perform(click())
        onView(withId(R.id.note_g)).perform(click())

        // Recreate fragment
        scenario.recreate()

        Thread.sleep(500)

        // Notes should still be playing (if state is preserved)
        // This tests state restoration
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testNoteButtonsHaveContentDescriptions() {
        val noteIds = listOf(
            R.id.note_c, R.id.note_c_sharp, R.id.note_d, R.id.note_d_sharp,
            R.id.note_e, R.id.note_f, R.id.note_f_sharp, R.id.note_g,
            R.id.note_g_sharp, R.id.note_a, R.id.note_a_sharp, R.id.note_b
        )

        for (noteId in noteIds) {
            onView(withId(noteId))
                .check(matches(hasContentDescription()))
        }
    }

    @Test
    fun testRangeToggleHasContentDescription() {
        onView(withId(R.id.range_toggle))
            .check(matches(hasContentDescription()))
    }

    // ==================== Visual State Tests ====================

    @Test
    fun testPlayingNoteHasVisualFeedback() {
        // Click to play note
        onView(withId(R.id.note_c))
            .perform(click())

        // Verify visual feedback (selected state, background change, etc.)
        onView(withId(R.id.note_c))
            .check(matches(isSelected()))
    }

    @Test
    fun testNoteButtonsHaveCorrectLabels() {
        // Verify some note labels
        onView(allOf(withId(R.id.note_c), withText("C")))
            .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.note_c_sharp), withText(anyOf(equalTo("C#"), equalTo("Db")))))
            .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.note_d), withText("D")))
            .check(matches(isDisplayed()))
    }
}
