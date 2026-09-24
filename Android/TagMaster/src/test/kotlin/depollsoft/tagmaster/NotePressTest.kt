package depollsoft.tagmaster

import android.app.Application
import android.os.Looper
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.tagmaster.ui.notePress
import depollsoft.tagmaster.ui.rememberNotePlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * The key-note buttons: a touch sounds the note until the finger lifts, a click that is not a
 * touch (screen reader, keyboard) plays it for 1.5 seconds, and only a note the control started
 * is ever stopped. Ported from the pitch-pipe button cases that covered the View implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class NotePressTest {
    @get:Rule
    val compose = createComposeRule()

    private var shown by mutableStateOf(true)
    private lateinit var current: Note

    private fun note(): Note =
        Mockito.mock(Note::class.java).also {
            Mockito.`when`(it.accidental).thenReturn(Accidental.Natural)
            Mockito.`when`(it.friendlyName).thenReturn("C")
        }

    private var playerShown by mutableStateOf(true)

    private val view: View = Mockito.mock(View::class.java)

    private fun setUp(inAScrollingPage: Boolean = false): Note {
        current = note()
        compose.setContent {
            if (!playerShown) return@setContent
            val player = rememberNotePlayer()
            if (shown) {
                Column(
                    Modifier
                        .size(200.dp)
                        .then(if (inAScrollingPage) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .testTag("key")
                            .notePress(player, { current }, view = view),
                    )
                    if (inAScrollingPage) Box(Modifier.size(48.dp, 1000.dp))
                }
            }
        }
        compose.waitForIdle()
        return current
    }

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun accessibilityClickPlaysOnceAndStopsAfter1500ms() {
        val note = setUp()
        compose.onNodeWithTag("key").performSemanticsAction(SemanticsActions.OnClick)
        Mockito.verify(note).play()
        idleFor(1499)
        Mockito.verify(note, Mockito.never()).stop()
        idleFor(1)
        Mockito.verify(note).stop()
    }

    @Test
    fun touchPlaysWhileHeldAndStopsOnceOnRelease() {
        val note = setUp()
        compose.onNodeWithTag("key").performTouchInput { down(center) }
        compose.waitForIdle()
        Mockito.verify(note, Mockito.times(1)).play()
        Mockito.verify(note, Mockito.never()).stop()
        compose.onNodeWithTag("key").performTouchInput { up() }
        compose.waitForIdle()
        idleFor(2000)
        Mockito.verify(note, Mockito.times(1)).play()
        Mockito.verify(note, Mockito.times(1)).stop()
    }

    @Test
    fun cancelledTouchStopsTheHeldNote() {
        val note = setUp()
        compose.onNodeWithTag("key").performTouchInput {
            down(center)
            cancel()
        }
        compose.waitForIdle()
        Mockito.verify(note).play()
        Mockito.verify(note).stop()
    }

    @Test
    fun replacingTheNoteWhileHeldStopsOnlyTheNoteThatWasPlaying() {
        val note = setUp()
        compose.onNodeWithTag("key").performTouchInput { down(center) }
        compose.waitForIdle()
        val replacement = note()
        current = replacement
        compose.onNodeWithTag("key").performTouchInput { up() }
        compose.waitForIdle()
        Mockito.verify(note).stop()
        Mockito.verify(replacement, Mockito.never()).play()
        Mockito.verify(replacement, Mockito.never()).stop()
    }

    @Test
    fun removingTheButtonStopsAHeldNote() {
        val note = setUp()
        compose.onNodeWithTag("key").performTouchInput { down(center) }
        compose.waitForIdle()
        shown = false
        compose.waitForIdle()
        Mockito.verify(note).play()
        Mockito.verify(note).stop()
    }

    @Test
    fun leavingTheScreenStopsATimedClick() {
        val note = setUp()
        compose.onNodeWithTag("key").performSemanticsAction(SemanticsActions.OnClick)
        playerShown = false
        compose.waitForIdle()
        Mockito.verify(note).stop()
        idleFor(2000)
        Mockito.verify(note, Mockito.times(1)).stop()
    }

    @Test
    fun aFingerThatDriftsOffTheButtonKeepsTheNoteSounding() {
        val note = setUp()
        compose.onNodeWithTag("key").performTouchInput {
            down(center)
            moveBy(Offset(width * 2f, 0f))
        }
        compose.waitForIdle()
        Mockito.verify(note, Mockito.never()).stop()
        compose.onNodeWithTag("key").performTouchInput { up() }
        compose.waitForIdle()
        Mockito.verify(note).stop()
        Mockito.verify(view, Mockito.never()).playSoundEffect(SoundEffectConstants.CLICK)
    }

    @Test
    fun aScrollThatTakesTheTouchStopsTheNote() {
        val note = setUp(inAScrollingPage = true)
        compose.onNodeWithTag("key").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, -20f)) }
        }
        compose.waitForIdle()
        Mockito.verify(note).stop()
        compose.onNodeWithTag("key").performTouchInput { up() }
    }

    @Test
    fun aTapMakesTheClickSoundAndACancelledTouchDoesNot() {
        setUp()
        compose.onNodeWithTag("key").performTouchInput {
            down(center)
            cancel()
        }
        compose.waitForIdle()
        Mockito.verify(view, Mockito.never()).playSoundEffect(SoundEffectConstants.CLICK)
        compose.onNodeWithTag("key").performTouchInput { click() }
        compose.waitForIdle()
        Mockito.verify(view).playSoundEffect(SoundEffectConstants.CLICK)
    }
}
