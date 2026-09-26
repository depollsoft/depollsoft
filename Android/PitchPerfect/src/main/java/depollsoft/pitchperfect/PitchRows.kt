package depollsoft.pitchperfect

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.hairlineWidth
import depollsoft.pitchperfect.ui.plateColors
import kotlinx.coroutines.flow.first

/**
 * A row that sounds while it is held, the Notes and Keys rows' touch contract:
 *
 * - A press starts [play]'s sound, or toggles it in "notes play until pressed again" mode.
 * - Releasing, or a scroll taking the finger, stops a held sound; a toggled one keeps sounding.
 * - In toggle mode a finger that keeps moving after [TOGGLE_SLOP_MS] stops the sound: that is a
 *   drag, not a tap.
 *
 * Pointer events are observed, never consumed, so the list still scrolls. The callbacks may
 * change from one composition to the next (a row's song replaced by a synced copy, say): each
 * press uses the callbacks current when it lands, and a gesture in progress is not restarted.
 */
fun Modifier.soundsWhileHeld(
    toggleMode: () -> Boolean,
    isPlaying: () -> Boolean,
    play: () -> Unit,
    stop: () -> Unit,
): Modifier = this then SoundsWhileHeldElement(HeldSound(toggleMode, isPlaying, play, stop))

private class HeldSound(
    val toggleMode: () -> Boolean,
    val isPlaying: () -> Boolean,
    val play: () -> Unit,
    val stop: () -> Unit,
)

private data class SoundsWhileHeldElement(
    val sound: HeldSound,
) : ModifierNodeElement<SoundsWhileHeldNode>() {
    override fun create() = SoundsWhileHeldNode(sound)

    override fun update(node: SoundsWhileHeldNode) {
        node.sound = sound
    }
}

private class SoundsWhileHeldNode(
    var sound: HeldSound,
) : DelegatingNode(),
    PointerInputModifierNode {
    private val input =
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // This press owns the song it started, even if the row is recomposed with
                    // another before the finger lifts.
                    val held = sound
                    val toggle = held.toggleMode()
                    if (toggle) {
                        if (held.isPlaying()) held.stop() else held.play()
                    } else {
                        held.play()
                    }
                    try {
                        while (true) {
                            // The final pass, after the list has had its chance to claim the finger for a scroll.
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            // Lifting, or a scroll taking the finger.
                            if (!change.pressed || change.isConsumed) break
                            if (toggle && change.positionChanged() && change.uptimeMillis - down.uptimeMillis > TOGGLE_SLOP_MS) {
                                held.stop()
                            }
                        }
                    } finally {
                        // Also when the row leaves the list mid-press (its list deleted by a
                        // sync, say): the gesture is cancelled with no further event.
                        if (!toggle) held.stop()
                    }
                }
            },
        )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) = input.onPointerEvent(pointerEvent, pass, bounds)

    override fun onCancelPointerInput() = input.onCancelPointerInput()
}

private const val TOGGLE_SLOP_MS = 100L

/** The ListView divider: a one-pixel hairline between rows, none after the last. */
fun LazyListScope.hairlineDivided(
    count: Int,
    key: ((Int) -> Any)? = null,
    row: @Composable (Int) -> Unit,
) {
    items(count, key = key) { index ->
        row(index)
        if (index < count - 1) Hairline()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val color = plateColors.hairline
    Box(modifier.fillMaxWidth().height(hairlineWidth).drawBehind { drawRect(color) })
}

/**
 * Scrolls a list once, after its first layout, so the middle row is centred: ListView's
 * `setSelection(count / 2 - visible / 2)`, where `visible` is the index span it had laid out.
 */
@Composable
fun CenterOnFirstLayout(
    state: LazyListState,
    count: Int,
    key: Any? = Unit,
) {
    LaunchedEffect(key) {
        val visible = snapshotFlow { state.layoutInfo.visibleItemsInfo }.first { it.isNotEmpty() }
        val span = visible.last().index - visible.first().index
        // The first layout may still be running; scroll once it has finished, as ListView's post did.
        withFrameNanos { }
        state.scrollToItem((count / 2 - span / 2).coerceAtLeast(0))
    }
}

/**
 * A screen reader's activation of a pitch row. A double tap would otherwise sound the note for
 * the few milliseconds between its synthetic down and up; instead it sounds for as long as the
 * pitch pipe's cells do, or toggles in "notes play until pressed again" mode.
 */
object BriefNotes {
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val pending = mutableMapOf<Any, Runnable>()

    fun activate(note: Note): Boolean = activate(note, { note.isPlaying }, { note.play() }, { note.stop() })

    /** [activate] for anything that sounds: a song sounds its key's tonic the same way. */
    fun activate(
        target: Any,
        isPlaying: () -> Boolean,
        play: () -> Unit,
        stop: () -> Unit,
    ): Boolean {
        pending.remove(target)?.let { main.removeCallbacks(it) }
        if (SettingsModel.toggleNotes) {
            if (isPlaying()) stop() else play()
        } else {
            play()
            val later =
                Runnable {
                    pending.remove(target)
                    stop()
                }
            pending[target] = later
            main.postDelayed(later, InstrumentState.ACCESSIBILITY_NOTE_MS)
        }
        return true
    }

    /**
     * Drops every pending stop. A screen that silences its notes calls this too: a stop still
     * pending from here would otherwise cut short the same note sounded later somewhere else
     * (every screen shares one [Note] per pitch).
     */
    fun cancelAll() {
        pending.values.forEach { main.removeCallbacks(it) }
        pending.clear()
    }
}

/**
 * Runs [onChange] each time a pager page stops or starts being the settled page, never for the
 * page's first composition: a ViewPager2 page that was never shown was never paused either.
 */
@Composable
fun OnPageVisibilityChange(
    isCurrentPage: Boolean,
    onChange: (current: Boolean) -> Unit,
) {
    val previous = remember { booleanArrayOf(isCurrentPage) }
    LaunchedEffect(isCurrentPage) {
        if (previous[0] != isCurrentPage) {
            previous[0] = isCurrentPage
            onChange(isCurrentPage)
        }
    }
}
