package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchMultiTouchTrackerTest {
    @Test
    fun twoPointers_startAndReleaseNotesIndependently() {
        val started = mutableListOf<Int>()
        val stopped = mutableListOf<Int>()
        val tracker = PitchMultiTouchTracker(started::add, stopped::add)

        tracker.press(pointerId = 10, cell = 0)
        tracker.press(pointerId = 11, cell = 1)

        assertEquals(listOf(0, 1), started)
        assertTrue(stopped.isEmpty())

        tracker.release(pointerId = 11)
        assertEquals(listOf(1), stopped)

        tracker.release(pointerId = 10)
        assertEquals(listOf(1, 0), stopped)
    }

    @Test
    fun sharedCell_stopsOnlyAfterLastPointerReleases() {
        val started = mutableListOf<Int>()
        val stopped = mutableListOf<Int>()
        val tracker = PitchMultiTouchTracker(started::add, stopped::add)

        tracker.press(pointerId = 1, cell = 4)
        tracker.press(pointerId = 2, cell = 4)
        tracker.release(pointerId = 1)

        assertEquals(listOf(4), started)
        assertTrue(stopped.isEmpty())

        tracker.release(pointerId = 2)
        assertEquals(listOf(4), stopped)
    }
}
