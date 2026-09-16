package depollsoft.pitchperfect

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.provider.Settings
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.util.Preferences
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WearPitchInstrumentViewTest {
    private lateinit var view: WearPitchInstrumentView
    private lateinit var model: PitchPipeModel
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        model = PitchPipeModel()
        view = WearPitchInstrumentView(context)
        view.model = model
        view.layout(0, 0, 384, 384)
    }

    @After
    fun tearDown() {
        view.stopAll()
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    @Test
    fun geometry_placesThirteenCellsInsideTheBezel() {
        assertEquals(13, view.cellCenters.size)
        assertEquals(384 * 0.02f, view.edgeInset, 0.001f)
        assertEquals(384 * 0.085f, view.cellRadius, 0.001f)
        assertEquals(384 * 0.395f, view.ringRadius, 0.001f)
        for (center in view.cellCenters) {
            val c = requireNotNull(center)
            val distance = hypot(c[0] - 192f, c[1] - 192f)
            assertEquals(view.ringRadius, distance, 0.001f)
            // The farthest point of each entire cell circle must fit, not just its center.
            assertTrue(distance + view.cellRadius <= 192f - view.edgeInset + 0.001f)
        }
        assertEquals(384 * 0.36f, view.rangeLowRect.width(), 0.001f)
        assertEquals(384 * 0.08f, view.rangeLowRect.height(), 0.001f)
        assertEquals(192f + 384 * 0.055f, view.rangeLowRect.top, 0.001f)
    }

    @Test
    fun sectors_coverCellsAndGapsButExcludeTheHole() {
        view.cellCenters.forEachIndexed { index, center ->
            val c = requireNotNull(center)
            assertEquals(index, view.cellAt(c[0], c[1]))
            val angle = Math.toRadians(-90.0 + (index + 1) * 360.0 / 13)
            val gapX = 192f + view.ringRadius * cos(angle).toFloat()
            val gapY = 192f + view.ringRadius * sin(angle).toFloat()
            assertTrue(view.cellAt(gapX, gapY) in listOf(index, (index + 1) % 13))
        }
        assertEquals(-1, view.cellAt(192f, 192f))
        assertEquals(-1, view.cellAt(192f, 192f - view.ringRadius + 1.25f * view.cellRadius + 1f))
        assertEquals(0, view.cellAt(200f, 0f))
    }

    @Test
    fun sectors_stopAtTheBezelSoSquareCornersAreNotPlayable() {
        // A round face is playable right up to its edge...
        assertEquals(12, view.cellAt(192f - 4f, 1f))
        // ...but the corners of a square watch, beyond the cells' outer rims, are score only.
        assertEquals(-1, view.cellAt(0f, 0f))
        assertEquals(-1, view.cellAt(383f, 383f))
        assertEquals(-1, view.cellAt(192f, 192f - view.ringRadius - 1.25f * view.cellRadius - 1f))

        // A finger that slides off the face into a corner releases its note.
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        assertTrue(model.notes[0].isPlaying)
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 2f, 2f, 0)
        try {
            view.onTouchEvent(event)
        } finally {
            event.recycle()
        }
        assertFalse(model.notes[0].isPlaying)
    }

    @Test
    fun accessibilityClick_pendingStopNeverCutsANewerActivationShort() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        activity.get().setContentView(view)
        val provider = requireNotNull(view.accessibilityNodeProvider)
        val looper = shadowOf(Looper.getMainLooper())

        // Re-activating the same note restarts its 1.5 s window.
        provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null)
        looper.idleFor(Duration.ofMillis(1000))
        provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null)
        looper.idleFor(Duration.ofMillis(1000))
        assertTrue(model.notes[0].isPlaying)
        looper.idleFor(Duration.ofMillis(600))
        assertFalse(model.notes[0].isPlaying)

        // A finger that takes over the note holds it past the click's window.
        provider.performAction(1, AccessibilityNodeInfo.ACTION_CLICK, null)
        looper.idleFor(Duration.ofMillis(1000))
        pointers(MotionEvent.ACTION_DOWN, 0 to 1)
        looper.idleFor(Duration.ofMillis(2000))
        assertTrue(model.notes[1].isPlaying)
        pointers(MotionEvent.ACTION_UP, 0 to 1)
        assertFalse(model.notes[1].isPlaying)

        // stopAll clears pending stops so they cannot fire on a later activation.
        provider.performAction(2, AccessibilityNodeInfo.ACTION_CLICK, null)
        view.stopAll()
        pointers(MotionEvent.ACTION_DOWN, 0 to 2)
        looper.idleFor(Duration.ofMillis(2000))
        assertTrue(model.notes[2].isPlaying)
        pointers(MotionEvent.ACTION_UP, 0 to 2)
        activity.pause().stop().destroy()
    }

    @Test
    fun lowRangeRow_stopsOldRangeNotesAndRepeatedSelectionKeepsTheRange() {
        model.isFromFToF = true
        val oldNote = model.notes[12] // F5 does not belong to C4-C5.
        oldNote.play()
        down(view.rangeLowRect.centerX(), view.rangeLowRect.centerY())
        assertFalse(model.isFromFToF)
        assertFalse(oldNote.isPlaying)
        assertTrue(model.notes.none { it.isPlaying })

        model.notes[0].play()
        down(view.rangeLowRect.centerX(), view.rangeLowRect.centerY())
        assertFalse(model.isFromFToF)
        assertTrue(model.notes.none { it.isPlaying })
        down(view.rangeHighRect.centerX(), view.rangeHighRect.centerY())
        assertTrue(model.isFromFToF)
    }

    @Test
    fun rangeRows_haveAGenerousHitZoneInsideTheHole() {
        val low = view.rangeLowRect
        val high = view.rangeHighRect
        val rowHeight = low.height()
        // Just above the frame still belongs to the top row; below it, the bottom row runs to the ring.
        assertEquals(0, view.rangeRowAt(low.centerX(), low.top - rowHeight * 0.4f))
        assertEquals(1, view.rangeRowAt(high.centerX(), high.bottom + rowHeight * 0.8f))
        // Sideways padding, but not the whole hole.
        assertEquals(0, view.rangeRowAt(low.left - 384 * 0.04f, low.centerY()))
        assertEquals(-1, view.rangeRowAt(low.left - 384 * 0.1f, low.centerY()))
        // The readout above the frame and the ring below it stay clear of the selector.
        assertEquals(-1, view.rangeRowAt(192f, low.top - rowHeight))
        assertEquals(-1, view.rangeRowAt(192f, 192f + view.ringRadius))
        // Cells are never stolen: every cell center resolves to no row.
        view.cellCenters.forEach { c -> assertEquals(-1, view.rangeRowAt(c!![0], c[1])) }

        down(high.centerX(), high.bottom + rowHeight * 0.8f)
        assertTrue(model.isFromFToF)
        down(low.centerX(), low.top - rowHeight * 0.4f)
        assertFalse(model.isFromFToF)
    }

    @Test
    fun rotary_selectsRangesAndStopsNotesEvenForRepeatedSelection() {
        val oldNote = model.notes[0]
        oldNote.play()
        scroll(-1f)
        assertTrue(model.isFromFToF)
        assertFalse(oldNote.isPlaying)
        model.notes[12].play()
        scroll(-1f)
        assertTrue(model.notes.none { it.isPlaying })
        scroll(1f)
        assertFalse(model.isFromFToF)
    }

    @Test
    fun drawingASoundingNoteStartsBreathingAndStopAllCancelsIt() {
        model.notes[0].play()
        draw()
        val animator = requireNotNull(view.breatheAnimator)
        assertTrue(animator.isStarted)
        assertEquals(4000L, animator.duration)
        view.stopAll()
        assertNull(view.breatheAnimator)
        assertFalse(animator.isStarted)
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun reducedMotionDisablesBreathing() {
        model.notes[0].play()
        draw()
        assertNotNull(view.breatheAnimator)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        draw()
        assertNull(view.breatheAnimator)
        assertTrue(model.notes[0].isPlaying)
    }

    @Test
    fun accessibility_exposesThirteenNotesAndTwoRanges() {
        val provider = requireNotNull(view.accessibilityNodeProvider)
        val host = requireNotNull(provider.createAccessibilityNodeInfo(View.NO_ID))
        assertEquals(15, host.childCount)
        val descriptions = listOf(
            "C, octave 4", "C sharp, D flat, octave 4", "D, octave 4",
            "D sharp, E flat, octave 4", "E, octave 4", "F, octave 4",
            "F sharp, G flat, octave 4", "G, octave 4", "G sharp, A flat, octave 4",
            "A, octave 4", "A sharp, B flat, octave 4", "B, octave 4", "C, octave 5",
        )
        descriptions.forEachIndexed { index, description ->
            val node = requireNotNull(provider.createAccessibilityNodeInfo(index))
            assertEquals(description, node.contentDescription.toString())
            assertEquals("android.widget.Button", node.className.toString())
            assertFalse(node.isSelected)
        }
        model.notes[0].play()
        assertTrue(provider.createAccessibilityNodeInfo(0)!!.isSelected)
        assertEquals("Octave range C to C", provider.createAccessibilityNodeInfo(100)!!.contentDescription)
        assertTrue(provider.createAccessibilityNodeInfo(100)!!.isSelected)
        assertEquals("Octave range F to F", provider.createAccessibilityNodeInfo(101)!!.contentDescription)
        assertFalse(provider.createAccessibilityNodeInfo(101)!!.isSelected)
    }

    @Test
    fun accessibility_clickPlaysForOneAndAHalfSecondsOrToggles() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        activity.get().setContentView(view)
        val provider = requireNotNull(view.accessibilityNodeProvider)
        assertTrue(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(model.notes[0].isPlaying)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1500))
        assertFalse(model.notes[0].isPlaying)
        view.toggleMode = true
        provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null)
        assertTrue(model.notes[0].isPlaying)
        provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null)
        assertFalse(model.notes[0].isPlaying)
        provider.performAction(101, AccessibilityNodeInfo.ACTION_CLICK, null)
        assertTrue(model.isFromFToF)
        activity.pause().stop().destroy()
    }

    @Test
    fun multiTouch_releasingOnePointerOnlyStopsItsNote() {
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        pointers(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 0 to 0, 1 to 4)
        assertTrue(model.notes[0].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        pointers(MotionEvent.ACTION_POINTER_UP, 0 to 0, 1 to 4)
        assertFalse(model.notes[0].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        pointers(MotionEvent.ACTION_UP, 1 to 4)
        assertFalse(model.notes[4].isPlaying)
    }

    @Test
    fun slidingBetweenCellsTransfersOnlyThatPointersNote() {
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        pointers(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 0 to 0, 1 to 4)
        pointers(MotionEvent.ACTION_MOVE, 0 to 1, 1 to 4)
        assertFalse(model.notes[0].isPlaying)
        assertTrue(model.notes[1].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        pointers(MotionEvent.ACTION_CANCEL, 0 to 1, 1 to 4)
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun toggleMode_keepsPlayingAfterReleaseAndStopsOnTheNextTap() {
        view.toggleMode = true
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        pointers(MotionEvent.ACTION_UP, 0 to 0)
        assertTrue(model.notes[0].isPlaying)
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        assertFalse(model.notes[0].isPlaying)
    }

    @Test
    fun stopAll_clearsPointerOwnership() {
        pointers(MotionEvent.ACTION_DOWN, 0 to 0)
        view.stopAll()
        pointers(MotionEvent.ACTION_MOVE, 0 to 1)
        assertTrue(model.notes.none { it.isPlaying })
    }

    private fun down(x: Float, y: Float) {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0)
        try {
            assertTrue(view.onTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun pointers(action: Int, vararg cells: Pair<Int, Int>) {
        val properties = cells.map { (id, _) ->
            MotionEvent.PointerProperties().apply {
                this.id = id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()
        val coordinates = cells.map { (_, cell) ->
            val center = requireNotNull(view.cellCenters[cell])
            MotionEvent.PointerCoords().apply {
                x = center[0]
                y = center[1]
            }
        }.toTypedArray()
        val event = MotionEvent.obtain(0, 0, action, cells.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        try {
            assertTrue(view.onTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun scroll(delta: Float) {
        val event = MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_SCROLL, 1,
            arrayOf(MotionEvent.PointerProperties().apply { id = 0 }),
            arrayOf(MotionEvent.PointerCoords().apply { setAxisValue(MotionEventCompat.AXIS_SCROLL, delta) }),
            0, 0, 1f, 1f, 0, 0, InputDeviceCompat.SOURCE_ROTARY_ENCODER, 0,
        )
        try {
            assertTrue(view.onGenericMotionEvent(event))
        } finally {
            event.recycle()
        }
    }

    private fun draw() {
        val bitmap = Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        bitmap.recycle()
    }
}
