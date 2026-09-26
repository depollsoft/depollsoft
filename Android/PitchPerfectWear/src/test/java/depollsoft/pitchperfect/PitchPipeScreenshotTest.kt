package depollsoft.pitchperfect

import android.app.Application
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import depollsoft.lib.testing.GOLDEN_TOLERANCE
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel goldens for the watch face, on a small round watch and a square one.
 *
 * Reduced motion pins the breathing glow at its resting phase so every capture is deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class PitchPipeScreenshotTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val silent =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        // SettingsModel registers its defaults once per process; reseed them after the clear.
        SettingsModel.setToggleNotes(false)
        SettingsModel.setWakeLock(false)
        Note.setPlayer(silent)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    private fun capture(
        name: String,
        fromFToF: Boolean = false,
        playing: List<Note> = emptyList(),
    ) {
        Preferences.set("depollsoft.pitchperfect.PitchPipeModel.IsFromFToF", fromFToF)
        val controller = Robolectric.buildActivity(PitchPipeActivity::class.java).setup()
        playing.forEach { it.play() }
        shadowOf(Looper.getMainLooper()).idle()
        controller.get().window.decorView.captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = GOLDEN_TOLERANCE)
        playing.forEach { it.stop() }
        controller.pause().stop().destroy()
    }

    private fun note(
        name: String,
        accidental: Accidental = Accidental.Natural,
        octave: Int = 4,
    ) = Note.findNote(name, accidental, octave)

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundIdle() = capture("round_idle")

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundOneNote() = capture("round_one_note", playing = listOf(note("A")))

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundSharpNote() = capture("round_sharp_note", playing = listOf(note("C", Accidental.Sharp)))

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundInterval() = capture("round_interval", playing = listOf(note("C"), note("G")))

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundChord() = capture("round_chord", playing = listOf(note("C"), note("E"), note("G")))

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundBarbershopSeventh() =
        capture(
            "round_barbershop_seventh",
            playing = listOf(note("C"), note("E"), note("G"), note("A", Accidental.Sharp)),
        )

    @Test
    @Config(qualifiers = "w192dp-h192dp-round-xhdpi")
    fun roundHighRange() = capture("round_high_range", fromFToF = true, playing = listOf(note("F", octave = 5)))

    @Test
    @Config(qualifiers = "w200dp-h200dp-notround-xhdpi")
    fun squareIdle() = capture("square_idle")

    @Test
    @Config(qualifiers = "w200dp-h200dp-notround-xhdpi")
    fun squareOneNote() = capture("square_one_note", playing = listOf(note("D")))
}
