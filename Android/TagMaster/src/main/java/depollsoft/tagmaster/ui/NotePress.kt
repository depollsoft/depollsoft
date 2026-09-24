package depollsoft.tagmaster.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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

/** Wires [player] to presses, keyboard Enter/Space and accessibility clicks of this element. */
fun Modifier.notePress(
    player: NotePlayer,
    note: () -> Note?,
    enabled: Boolean = true,
    description: String? = null,
): Modifier {
    if (!enabled) return this
    return this
        .pointerInput(player) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                player.press(note())
                // The finally also covers the button leaving the screen mid-press.
                try {
                    waitForUpOrCancellation()?.consume()
                } finally {
                    player.release()
                }
            }
        }.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.DirectionCenter)) {
                player.click(note())
                true
            } else {
                false
            }
        }.focusable()
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
