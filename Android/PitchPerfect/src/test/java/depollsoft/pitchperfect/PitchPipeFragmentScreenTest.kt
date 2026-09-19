package depollsoft.pitchperfect

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Touch interaction with the custom-drawn pitch instrument, migrated from the instrumented
 * `PitchPipeFragmentTest`.
 *
 * `PitchInstrumentView` computes its cell and range geometry in `onSizeChanged`, so the same
 * coordinates the instrumented test derived are reproducible once Robolectric has laid the view
 * out. The taps go through `dispatchTouchEvent` exactly as the Espresso `ViewAction` did.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class)
class PitchPipeFragmentScreenTest {
    private var controller: ActivityController<PitchPerfectActivity>? = null
    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        // Switching range updates the home-screen widget in production; that is a separate
        // surface with its own coverage and needs no broadcast here.
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
    }

    @After
    fun tearDown() {
        PurchaseService.areAdsRemoved = false
        ScreenTestSupport.finishScreenTest(controller)
        widgets.close()
    }

    private fun launch(): PitchPerfectActivity {
        val created = Robolectric.buildActivity(PitchPerfectActivity::class.java)
        controller = created
        created.setup()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        return created.get()
    }

    private fun PitchPerfectActivity.instrument(): PitchInstrumentView = findViewById(R.id.pitchInstrument)

    private fun send(
        view: View,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, y, 0)
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
        idle()
    }

    /** The centre of pitch cell [index], from `PitchInstrumentView`'s own ring geometry. */
    private fun pitchPoint(
        view: View,
        index: Int,
    ): Pair<Float, Float> {
        val faceCx = view.width / 2f
        val faceCy = view.height * FACE_CENTER_Y
        val ring = min(view.width.toFloat(), view.height * FACE_HEIGHT_FRACTION) * RING_FRACTION
        val angle = Math.toRadians(PITCH_START_DEGREES + index * (360.0 / PITCH_COUNT))
        return faceCx + (cos(angle) * ring).toFloat() to faceCy + (sin(angle) * ring).toFloat()
    }

    /**
     * Press a cell, assert it sounds while held, release, assert it falls silent.
     *
     * The instrumented original only performed a down/up pair and asserted nothing, so a cell that
     * had stopped responding could not have failed it. A pitch pipe's contract is that the note
     * sounds for exactly as long as the finger is down, so that is what this checks.
     */
    private fun pressAndReleasePitch(
        view: View,
        index: Int,
        note: depollsoft.pitchperfect.lib.Note,
    ) {
        val (x, y) = pitchPoint(view, index)
        send(view, MotionEvent.ACTION_DOWN, x, y)
        assertTrue("cell $index should sound while held", note.isPlaying)
        send(view, MotionEvent.ACTION_UP, x, y)
        assertFalse("cell $index should fall silent on release", note.isPlaying)
    }

    private fun tapRange(
        view: View,
        high: Boolean,
    ) {
        val faceCy = view.height * FACE_CENTER_Y
        val ring = min(view.width.toFloat(), view.height * FACE_HEIGHT_FRACTION) * RING_FRACTION
        val rangeTop = faceCy + ring * RANGE_TOP_FRACTION
        val rowHeight = ring * RANGE_ROW_FRACTION
        val y = rangeTop + rowHeight * if (high) 1.5f else 0.5f
        send(view, MotionEvent.ACTION_DOWN, view.width / 2f, y)
        send(view, MotionEvent.ACTION_UP, view.width / 2f, y)
    }

    @Test
    fun instrumentAndNavigationAreDisplayed() {
        val activity = launch()
        assertDisplayed("bottomNavigation", activity.findViewById(R.id.bottomNavigation))
        assertDisplayed("viewPager", activity.findViewById<ViewPager2>(R.id.viewPager))
        assertDisplayed("pitchInstrument", activity.instrument())
    }

    @Test
    fun everyPitchCellAcceptsATouch() {
        val activity = launch()
        val instrument = activity.instrument()
        val model = requireNotNull(instrument.model) { "the fragment binds its model to the view" }
        for (index in 0 until PITCH_COUNT) {
            pressAndReleasePitch(instrument, index, model.notes[index])
        }
        // A range change follows production behaviour and stops sounding notes.
        tapRange(instrument, high = false)
        assertFalse(model.notes.any { it.isPlaying })
    }

    @Test
    fun rangeControlSwitchesFromCToCToFToFAndBack() {
        val activity = launch()
        val instrument = activity.instrument()

        tapRange(instrument, high = false)
        assertFalse("the low row selects C-to-C", PitchPipeModel().isFromFToF)

        tapRange(instrument, high = true)
        assertTrue("the high row selects F-to-F", PitchPipeModel().isFromFToF)

        tapRange(instrument, high = false)
        assertFalse("the low row selects C-to-C again", PitchPipeModel().isFromFToF)
    }

    @Test
    fun pitchCellsRemainInteractiveAfterRangeChange() {
        val activity = launch()
        val instrument = activity.instrument()
        tapRange(instrument, high = true)
        val model = requireNotNull(instrument.model)
        assertEquals("F-to-F starts on F4", "F", model.notes[0].friendlyName)
        assertEquals(4, model.notes[0].octave)

        pressAndReleasePitch(instrument, 0, model.notes[0])
        pressAndReleasePitch(instrument, 6, model.notes[6])

        tapRange(instrument, high = false)
        assertFalse(PitchPipeModel().isFromFToF)
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
