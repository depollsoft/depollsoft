package depollsoft.pitchperfect

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The face on the oldest Wear OS the app supports (minSdk 26). Every other test runs on SDK 35,
 * where a call newer than minSdk can't fail; here a missing method throws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], qualifiers = "w192dp-h192dp-round-xhdpi")
class MinSdkSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val silent =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        SettingsModel.setToggleNotes(true)
        Note.setPlayer(silent)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    @Test
    fun aLatchedNoteBreathes() {
        // A sounding note starts the glow, which reads the system's animator duration scale.
        val model = PitchPipeModel()
        compose.setContent { WearPitchPipeScreen(model, rememberWearInstrumentState(model)) }
        compose.waitForIdle()
        val c = WearInstrumentGeometry(384, 384, 13).cellCenters[0]
        compose.onNodeWithTag("pitchInstrument").performTouchInput { click(Offset(c[0], c[1])) }
        compose.waitForIdle()
        assertTrue("a note is sounding", Note.getCommonNotes().any { it.isPlaying })
    }
}
