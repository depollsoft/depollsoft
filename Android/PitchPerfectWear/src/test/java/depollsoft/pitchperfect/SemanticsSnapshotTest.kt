package depollsoft.pitchperfect

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import depollsoft.lib.testing.SemanticsSnapshot
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** What a screen reader hears and can do on the watch face (`src/test/semantics`). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w192dp-h192dp-round-xhdpi")
class SemanticsSnapshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val silent =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        SettingsModel.setToggleNotes(false)
        SettingsModel.setWakeLock(false)
        Note.setPlayer(silent)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    private fun verify(
        name: String,
        fromFToF: Boolean = false,
        playing: List<Note> = emptyList(),
    ) {
        Preferences.set("depollsoft.pitchperfect.PitchPipeModel.IsFromFToF", fromFToF)
        val controller = Robolectric.buildActivity(PitchPipeActivity::class.java).setup()
        playing.forEach { it.play() }
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        try {
            SemanticsSnapshot.verify(compose, name)
        } finally {
            playing.forEach { it.stop() }
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun idle() = verify("round_idle")

    @Test
    fun oneNoteSounding() = verify("round_one_note", playing = listOf(Note.findNote("A", depollsoft.pitchperfect.lib.Accidental.Natural, 4)))

    @Test
    fun highRange() = verify("round_high_range", fromFToF = true)
}
