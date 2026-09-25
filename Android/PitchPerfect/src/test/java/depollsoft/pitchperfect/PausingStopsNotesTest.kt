package depollsoft.pitchperfect

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import depollsoft.lib.activity.RichApplication
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
 * Leaving the app silences every tab. Notes play until pressed again here, so each note would
 * otherwise keep sounding in the background.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PausingStopsNotesTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)

    /** The notes the app has started and not yet stopped. */
    private val sounding = mutableSetOf<Note>()
    private val recording =
        object : Note.NotePlayer {
            override fun play(n: Note) {
                sounding += n
            }

            override fun stop(n: Note) {
                sounding -= n
            }
        }

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        SongsModel.get().clearAll()
        SettingsModel.toggleNotes = true
        Note.setPlayer(recording)
    }

    @After
    fun tearDown() {
        SongsModel.get().clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    /** Starts a note on [tab] with [press], then pauses the activity. */
    private fun pausingSilences(
        tab: MainTab,
        press: () -> Unit,
    ) {
        val controller = screens.launch(PitchPerfectActivity::class.java)
        with(screens) { controller.get().show(tab) }
        press()
        screens.settle()
        assertTrue("a note is sounding on ${tab.name}", sounding.isNotEmpty())
        controller.pause()
        screens.settle()
        assertTrue("pausing stopped $sounding", sounding.isEmpty())
    }

    @Test
    fun pausingStopsThePitchPipe() =
        pausingSilences(MainTab.PITCH_PIPE) {
            compose.onNodeWithContentDescription("A, octave 4").performTouchInput { click() }
        }

    @Test
    fun pausingStopsANote() =
        pausingSilences(MainTab.NOTES) {
            val middle = Note.getPrunedNotes().indexOfFirst { it.friendlyName == "C" && it.octave == 4 }
            compose.onNodeWithTag(TestTags.noteRow(middle)).performTouchInput { click() }
        }

    @Test
    fun pausingStopsAKey() =
        pausingSilences(MainTab.KEYS) {
            val shownKey = SemanticsMatcher("a key row") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("key:") == true }
            compose.onAllNodes(shownKey)[0].performTouchInput { click() }
        }

    @Test
    fun pausingStopsASong() {
        val song = ComposeScreens.song("Blue Skies")
        SongsModel.get().defaultSongList.addSong(song)
        pausingSilences(MainTab.SONGS) {
            compose.onNodeWithTag(TestTags.songRow(song.id)).performTouchInput { click() }
        }
    }
}
