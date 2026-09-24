package depollsoft.tagmaster.ui

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import android.os.Handler
import android.os.Looper
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note

/**
 * Plays a note the way the pitch-pipe key buttons did: it sounds while a finger is down and stops
 * when it lifts; a click that is not a touch (keyboard, screen reader) plays it for 1.5 seconds.
 * Only a note this control started is ever stopped by it.
 */
@Stable
class NotePlayer {
    private val main = Handler(Looper.getMainLooper())
    private var active: Note? = null
    private val stopTimed = Runnable { stop() }

    fun press(note: Note?) {
        stop()
        active = note?.also { it.play() }
    }

    fun release() = stop()

    fun click(note: Note?) {
        stop()
        val playing = note ?: return
        active = playing
        playing.play()
        main.postDelayed(stopTimed, 1500)
    }

    fun stop() {
        main.removeCallbacks(stopTimed)
        active?.stop()
        active = null
    }
}

@Composable
fun rememberNotePlayer(): NotePlayer {
    val player = remember { NotePlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }
    return player
}

/**
 * Wires [player] to presses, keyboard Enter/Space and accessibility clicks of this element.
 *
 * A finger sounds the note until it lifts, even if it drifts off the button, as the View buttons
 * stopped only on UP or CANCEL; a scroll that takes the gesture is that CANCEL. A tap that lifts on
 * the button plays the system click sound through [view], as a View's performClick did. Presses,
 * hover and focus are reported to [interactionSource] for the caller's indication.
 */
fun Modifier.notePress(
    player: NotePlayer,
    note: () -> Note?,
    enabled: Boolean = true,
    description: String? = null,
    view: View? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    if (!enabled) return this
    return this
        .pointerInput(player) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                player.press(note())
                val press = PressInteraction.Press(down.position)
                interactionSource?.tryEmit(press)
                var tapped = false
                // The finally also covers the button leaving the screen mid-press.
                try {
                    while (true) {
                        // Final pass: a scroll above that took the gesture has consumed it by now.
                        val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id } ?: break
                        // The down itself arrives here too, consumed by this handler.
                        if (change.changedToDownIgnoreConsumed()) continue
                        if (change.isConsumed) break
                        if (!change.pressed) {
                            change.consume()
                            tapped = !change.isOutOfBounds(size, extendedTouchPadding)
                            break
                        }
                    }
                } finally {
                    player.release()
                    interactionSource?.tryEmit(if (tapped) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                    if (tapped) view?.playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
        }.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.DirectionCenter)) {
                player.click(note())
                true
            } else {
                false
            }
        }.then(if (interactionSource != null) Modifier.hoverable(interactionSource) else Modifier)
        .focusable(interactionSource = interactionSource)
        .semantics {
            role = Role.Button
            if (description != null) contentDescription = description
            onClick {
                player.click(note())
                true
            }
        }
}

/** How a screen reader names a note: "B flat, A sharp, octave 4". */
fun noteDescription(note: Note?): String? {
    note ?: return null
    val description = StringBuilder()
    when (note.accidental) {
        Accidental.Natural -> description.append(note.friendlyName)
        Accidental.Sharp -> {
            description.append(note.friendlyName).append(" sharp")
            note.alternate?.let { description.append(", ").append(it.friendlyName).append(" flat") }
        }
        else -> {
            description.append(note.friendlyName).append(" flat")
            note.alternate?.let { description.append(", ").append(it.friendlyName).append(" sharp") }
        }
    }
    description.append(", octave ").append(note.octave)
    return description.toString()
}
