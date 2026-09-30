package depollsoft.pitchperfect.screenshots

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.PitchPipeModel
import depollsoft.pitchperfect.SettingsModel
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.capture
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The classic pitch pipe on the main screen: at rest, sounding, in both themes, turned, on a tablet and in large text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class ClassicScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() {
        ScreenshotSupport.setUp(compose)
        SettingsModel.classicPitchPipe = true
    }

    @After
    fun tearDown() {
        SettingsModel.classicPitchPipe = false
        ScreenshotSupport.tearDown()
    }

    /** The glow breathes; with animations off it rests at its brightest phase. */
    private fun reduceMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun play(
        name: String,
        accidental: Accidental,
        octave: Int,
    ) = Note.findNote(name, accidental, octave).play()

    private fun chord() {
        reduceMotion()
        play("C", Accidental.Natural, 4)
        play("E", Accidental.Natural, 4)
        play("G", Accidental.Natural, 4)
    }

    @Test
    fun classic() = launchMain().capture("main_pitch_pipe_classic")

    @Test
    fun classicChord() {
        chord()
        launchMain().capture("main_pitch_pipe_classic_chord")
    }

    @Test
    fun classicFromFToE() {
        PitchPipeModel().isFromFToF = true
        reduceMotion()
        play("A", Accidental.Sharp, 4)
        launchMain().capture("main_pitch_pipe_classic_f_to_e")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun classicChordNight() {
        chord()
        launchMain().capture("night_main_pitch_pipe_classic_chord")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_420)
    fun classic420() = launchMain().capture("dpi420_main_pitch_pipe_classic")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun classicLandscape() = launchMain().capture("land_main_pitch_pipe_classic")

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun classicTablet() = launchMain().capture("tablet_main_pitch_pipe_classic")

    @Test
    @Config(fontScale = 2f)
    fun classicLargeFont() = launchMain().capture("font200_main_pitch_pipe_classic")
}
