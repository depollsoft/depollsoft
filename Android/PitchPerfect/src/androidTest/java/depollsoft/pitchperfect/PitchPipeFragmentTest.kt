package depollsoft.pitchperfect

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Instrumented interaction tests for the custom-drawn pitch instrument. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PitchPipeFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    @Before
    fun dismissDialogs() {
        EspressoTestUtils.dismissStartupDialogs()
    }

    @Test
    fun instrumentAndNavigation_areDisplayed() {
        onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchInstrument)).check(matches(isDisplayed()))
    }

    @Test
    fun everyPitchCell_acceptsATouch() {
        repeat(PITCH_COUNT) { index ->
            onView(withId(R.id.pitchInstrument)).perform(tapPitch(index))
        }
        // A range change follows production behavior and stops sounding notes.
        onView(withId(R.id.pitchInstrument)).perform(tapRange(high = false))
    }

    @Test
    fun rangeControl_switchesFromCToCToFToFAndBack() {
        onView(withId(R.id.pitchInstrument)).perform(tapRange(high = false))
        assertFalse(PitchPipeModel().isFromFToF)

        onView(withId(R.id.pitchInstrument)).perform(tapRange(high = true))
        assertTrue(PitchPipeModel().isFromFToF)

        onView(withId(R.id.pitchInstrument)).perform(tapRange(high = false))
        assertFalse(PitchPipeModel().isFromFToF)
    }

    @Test
    fun pitchCellsRemainInteractiveAfterRangeChange() {
        onView(withId(R.id.pitchInstrument))
            .perform(tapRange(high = true))
            .perform(tapPitch(0))
            .perform(tapPitch(6))
            .perform(tapRange(high = false))
    }

    private fun tapPitch(index: Int): ViewAction =
        geometryTap("pitch cell $index") { view ->
            val faceCx = view.width / 2f
            val faceCy = view.height * FACE_CENTER_Y
            val ring = min(view.width.toFloat(), view.height * FACE_HEIGHT_FRACTION) * RING_FRACTION
            val step = 360.0 / PITCH_COUNT
            val angle = Math.toRadians(PITCH_START_DEGREES + index * step)
            floatArrayOf(
                faceCx + (cos(angle) * ring).toFloat(),
                faceCy + (sin(angle) * ring).toFloat(),
            )
        }

    private fun tapRange(high: Boolean): ViewAction =
        geometryTap(if (high) "F-to-F range" else "C-to-C range") { view ->
            val faceCy = view.height * FACE_CENTER_Y
            val ring = min(view.width.toFloat(), view.height * FACE_HEIGHT_FRACTION) * RING_FRACTION
            val rangeTop = faceCy + ring * RANGE_TOP_FRACTION
            val rowHeight = ring * RANGE_ROW_FRACTION
            floatArrayOf(view.width / 2f, rangeTop + rowHeight * if (high) 1.5f else 0.5f)
        }

    private fun geometryTap(
        description: String,
        coordinates: (View) -> FloatArray,
    ): ViewAction =
        object : ViewAction {
            override fun getDescription() = "tap $description"

            override fun getConstraints(): Matcher<View> = isDisplayed()

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                val point = coordinates(view)
                val downTime = SystemClock.uptimeMillis()
                view.dispatchTouchEvent(
                    MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, point[0], point[1], 0),
                )
                view.dispatchTouchEvent(
                    MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, point[0], point[1], 0),
                )
                uiController.loopMainThreadUntilIdle()
            }
        }

    private companion object {
        const val PITCH_COUNT = 13
        const val FACE_CENTER_Y = 0.44f
        const val FACE_HEIGHT_FRACTION = 0.82f
        const val RING_FRACTION = 0.365f
        const val RANGE_TOP_FRACTION = 0.20f
        const val RANGE_ROW_FRACTION = 0.145f
        const val PITCH_START_DEGREES = -90.0 + 360.0 / PITCH_COUNT / 2.0
    }
}
