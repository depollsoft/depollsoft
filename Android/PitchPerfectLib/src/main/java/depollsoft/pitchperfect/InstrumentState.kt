package depollsoft.pitchperfect

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import depollsoft.pitchperfect.lib.Note
import kotlin.math.PI

/**
 * What a pitch-pipe face does with fingers and a screen reader, independent of how it is drawn.
 * Coordinates are pixels in the face's own space; [geometryFor] lays out a face of a given width,
 * height and cell count.
 *
 * @param haptic performs a `HapticFeedbackConstants` effect on the face.
 * @param reduceMotion whether the system asks for animations to be switched off.
 * @param tapEveryToggle whether toggling a note off taps too (the phone), or only toggling it on
 *   (the watch).
 * @param screenReaderFeedback whether a screen reader's activation taps and ticks as a finger
 *   does (the watch) or is silent (the phone).
 */
abstract class InstrumentState<G : InstrumentGeometry>(
    val model: PitchPipe,
    private val geometryFor: (width: Int, height: Int, cellCount: Int) -> G,
    private val haptic: (Int) -> Unit,
    private val reduceMotion: () -> Boolean,
    private val tapEveryToggle: Boolean,
    private val screenReaderFeedback: Boolean,
) {
    /** "Notes play until pressed again": a tap latches a note instead of holding it. */
    var toggleMode by mutableStateOf(false)

    var geometry by mutableStateOf(geometryFor(0, 0, model.notes.size))
        private set

    /** The breathing glow's phase, 0..2π; the face advances it while [breathing]. */
    var breathePhase by mutableFloatStateOf(0f)

    val notes: List<Note> get() = model.notes

    /** Whether a sounding note should make the glow breathe. */
    val breathing: Boolean get() = notes.any { it.isPlaying } && !reduceMotion()

    private val main = Handler(Looper.getMainLooper())
    private val touchTracker =
        PitchMultiTouchTracker(
            onStart = { cell ->
                cancelPendingStop(cell)
                notes.getOrNull(cell)?.play()
                haptic(HapticFeedbackConstants.KEYBOARD_TAP)
            },
            onStop = { cell -> notes.getOrNull(cell)?.stop() },
        )

    // A screen reader's activation sounds a note for a moment. Each cell owns at most one pending
    // stop, so activating it again, or a finger landing on it, is never cut short by an old one.
    private val pendingStops = mutableMapOf<Int, Runnable>()

    fun resize(
        width: Int,
        height: Int,
    ) {
        if (width != geometry.width || height != geometry.height || notes.size != geometry.cellCenters.size) {
            geometry = geometryFor(width, height, notes.size)
        }
    }

    /**
     * A finger landed. The first finger of a gesture may pick a range instead of a note; every
     * finger may take a cell. Returns whether the finger belongs to the face (a cell or the range
     * selector), so nothing around the face takes a slide between notes as a swipe.
     */
    fun down(
        pointerId: Long,
        x: Float,
        y: Float,
        firstFinger: Boolean,
    ): Boolean {
        if (firstFinger) {
            val row = geometry.rangeRowAt(x, y)
            if (row != -1) {
                selectRange(row == 1)
                return true
            }
        }
        val index = geometry.cellAt(x, y)
        if (index == -1 || index >= notes.size) return false
        if (toggleMode) {
            val note = notes[index]
            note.isPlaying = !note.isPlaying
            if (tapEveryToggle || note.isPlaying) haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            touchTracker.press(pointerId.toInt(), index)
        }
        return true
    }

    /** A finger slid; its note follows it from cell to cell, or stops off the ring. */
    fun move(
        pointerId: Long,
        x: Float,
        y: Float,
    ) {
        if (toggleMode) return
        val cell = geometry.cellAt(x, y).takeIf { it in notes.indices }
        touchTracker.move(pointerId.toInt(), cell)
    }

    /** One finger of several lifted. */
    fun up(pointerId: Long) {
        touchTracker.release(pointerId.toInt())
    }

    /**
     * The last finger lifted, or the gesture was cancelled. Toggled notes keep sounding; held ones
     * stop, even if toggle mode switched on while they were held.
     */
    fun endGesture() {
        touchTracker.clear()
    }

    /** Switches octave range. Every sounding note stops: the old range's cells are gone. */
    fun selectRange(high: Boolean) {
        // Stop the old range first: its notes above or below the new one are no longer among
        // [notes] once the range switches, and would otherwise sound on with no cell to stop them.
        stopAll()
        if (model.isFromFToF != high) {
            model.isFromFToF = high
            haptic(HapticFeedbackConstants.CLOCK_TICK)
        }
        stopAll()
    }

    /** A screen reader activated a range row. */
    fun accessibilitySelectRange(high: Boolean): Boolean {
        if (screenReaderFeedback) {
            selectRange(high)
        } else {
            stopAll()
            model.isFromFToF = high
            stopAll()
        }
        return true
    }

    /** A screen reader activated a cell: sound it briefly, or toggle it in toggle mode. */
    fun accessibilityClick(cell: Int): Boolean {
        if (cell !in notes.indices) return false
        val note = notes[cell]
        if (toggleMode) {
            note.isPlaying = !note.isPlaying
            if (screenReaderFeedback && note.isPlaying) haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            cancelPendingStop(cell)
            note.play()
            if (screenReaderFeedback) haptic(HapticFeedbackConstants.KEYBOARD_TAP)
            val stop =
                Runnable {
                    pendingStops.remove(cell)
                    note.stop()
                }
            pendingStops[cell] = stop
            main.postDelayed(stop, ACCESSIBILITY_NOTE_MS)
        }
        return true
    }

    /** Silences the face: on leaving it, pausing, or changing range. */
    fun stopAll() {
        pendingStops.values.forEach { main.removeCallbacks(it) }
        pendingStops.clear()
        notes.forEach {
            it.isPlaying = false
            it.stop()
        }
        touchTracker.clear()
        breathePhase = 0f
    }

    private fun cancelPendingStop(cell: Int) {
        pendingStops.remove(cell)?.let { main.removeCallbacks(it) }
    }

    companion object {
        const val ACCESSIBILITY_NOTE_MS = 1500L
        const val BREATH_PERIOD_MS = 4000L

        /**
         * The glow's phase [elapsedMs] into breathing: one full sine cycle every four seconds,
         * stretched by the system's [durationScale] as a ValueAnimator's duration is.
         */
        fun breathePhaseAt(
            elapsedMs: Long,
            durationScale: Float = 1f,
        ): Float {
            val period = (BREATH_PERIOD_MS * durationScale).toLong().coerceAtLeast(1L)
            return (elapsedMs % period) / period.toFloat() * (2 * PI).toFloat()
        }
    }
}
