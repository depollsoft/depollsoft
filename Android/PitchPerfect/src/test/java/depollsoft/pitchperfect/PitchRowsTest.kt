package depollsoft.pitchperfect

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The Notes and Keys tabs: rows that sound while held, the key lists and their switch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PitchRowsTest {
    @get:Rule
    val compose = createComposeRule()

    private val notes = Note.getPrunedNotes()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        Note.setPlayer(SilentPlayer)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.setTestMode(false)
    }

    private object SilentPlayer : Note.NotePlayer {
        override fun play(n: Note) = Unit

        override fun stop(n: Note) = Unit
    }

    private fun settle() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    // ==================== Notes ====================

    private fun showNotes() {
        compose.setContent { PlateTheme { NoteListScreen(isCurrentPage = true) } }
        settle()
    }

    @Test
    fun theNotesListCoversEveryPitchOnceStartingNearMiddleC() {
        showNotes()
        assertTrue("enharmonic twins share a row", notes.zipWithNext().none { (a, b) -> a.frequency == b.frequency })
        assertTrue("the rows climb in pitch", notes.zipWithNext().all { (a, b) -> a.frequency < b.frequency })
        val middle = notes.indexOfFirst { it.friendlyName == "C" && it.octave == 4 }
        val c4 = compose.onNodeWithTag(TestTags.noteRow(middle))
        c4.assertIsDisplayed()
        assertTrue(
            "the list opens with middle C in view",
            compose.onAllNodesWithTag(TestTags.noteRow(0)).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun aNoteRowNamesBothSpellingsAndItsFrequency() {
        showNotes()
        val index = notes.indexOfFirst { it.friendlyName == "C" && it.accidental == Accidental.Sharp && it.octave == 4 }
        val text =
            compose.onNodeWithTag(TestTags.noteRow(index)).fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Text)?.joinToString(" ")
        assertEquals("C${NoteText.SHARP}4 / D${NoteText.FLAT}4 277.18 Hz", text)
    }

    @Test
    fun aNoteSoundsWhileHeld() {
        showNotes()
        val index = notes.indexOfFirst { it.friendlyName == "C" && it.octave == 4 }
        val row = compose.onNodeWithTag(TestTags.noteRow(index))
        row.performTouchDown()
        settle()
        assertTrue(notes[index].isPlaying)
        assertEquals(true, row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
        row.performTouchUp()
        settle()
        assertFalse(notes[index].isPlaying)
    }

    @Test
    fun inToggleModeATapLatchesTheNote() {
        SettingsModel.toggleNotes = true
        showNotes()
        val index = notes.indexOfFirst { it.friendlyName == "E" && it.octave == 4 }
        compose.onNodeWithTag(TestTags.noteRow(index)).performTap()
        settle()
        assertTrue(notes[index].isPlaying)
        compose.onNodeWithTag(TestTags.noteRow(index)).performTap()
        settle()
        assertFalse(notes[index].isPlaying)
    }

    @Test
    fun aScreenReaderSoundsANoteForAMoment() {
        showNotes()
        val index = notes.indexOfFirst { it.friendlyName == "G" && it.octave == 4 }
        compose.onNodeWithTag(TestTags.noteRow(index)).performSemanticsAction(SemanticsActions.OnClick)
        settle()
        assertTrue(notes[index].isPlaying)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(InstrumentState.ACCESSIBILITY_NOTE_MS + 100))
        settle()
        assertFalse(notes[index].isPlaying)
    }

    @Test
    fun leavingTheNotesPageSilencesIt() {
        SettingsModel.toggleNotes = true
        var current by androidx.compose.runtime.mutableStateOf(true)
        compose.setContent { PlateTheme { NoteListScreen(isCurrentPage = current) } }
        settle()
        val index = notes.indexOfFirst { it.friendlyName == "A" && it.octave == 4 }
        notes[index].play()
        current = false
        settle()
        assertFalse(notes[index].isPlaying)
    }

    // ==================== Keys ====================

    private lateinit var keys: KeySignatureModel

    private fun showKeys() {
        keys = KeySignatureModel()
        compose.setContent { PlateTheme { KeySignatureScreen(isCurrentPage = true, model = keys) } }
        settle()
    }

    private fun keyRow(listTag: String, description: String) =
        compose.onNode(
            androidx.compose.ui.test.hasContentDescription(description) and hasAnyAncestor(hasTestTag(listTag)),
        )

    @Test
    fun majorKeysShowByDefaultWithTheSwitchOffered() {
        showKeys()
        assertTrue(keys.isMajor)
        keyRow(TestTags.MAJOR_KEYS, "C major, no sharps or flats").assertIsDisplayed()
        compose.onNodeWithContentDescription(RichApplication.getAppContext().getString(R.string.SwitchToMinorKeys)).assertIsDisplayed()
        assertEquals(13, keys.majorKeys.size)
    }

    @Test
    fun theSwitchShowsMinorKeysAndBack() {
        showKeys()
        compose.onNodeWithTag(TestTags.MAJOR_MINOR_FAB).performClick()
        settle()
        assertFalse(keys.isMajor)
        keyRow(TestTags.MINOR_KEYS, "A minor, no sharps or flats").assertIsDisplayed()
        compose.onNodeWithContentDescription(RichApplication.getAppContext().getString(R.string.SwitchToMajorKeys)).assertIsDisplayed()
        assertEquals("the hidden list is hidden from screen readers too", 0, compose.onAllNodesWithTag(TestTags.keyRow(6)).fetchSemanticsNodes().count { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() == "C major, no sharps or flats"
        })

        compose.onNodeWithTag(TestTags.MAJOR_MINOR_FAB).performClick()
        settle()
        assertTrue("two switches end where they started", keys.isMajor)
        keyRow(TestTags.MAJOR_KEYS, "C major, no sharps or flats").assertIsDisplayed()
    }

    @Test
    fun theOtherListOpensWhereThisOneWas() {
        showKeys()
        compose.onNodeWithTag(TestTags.MAJOR_KEYS).performScrollToIndex(0)
        settle()
        keyRow(TestTags.MAJOR_KEYS, "G flat major, 6 flats").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.MAJOR_MINOR_FAB).performClick()
        settle()
        keyRow(TestTags.MINOR_KEYS, "E flat minor, 6 flats").assertIsDisplayed()
    }

    @Test
    fun aKeySoundsItsTonicWhileHeldAndTheSwitchSilencesIt() {
        showKeys()
        val c = keys.majorKeys.first { it.friendlyName == "C" }
        val row = keyRow(TestTags.MAJOR_KEYS, "C major, no sharps or flats")
        row.performTouchDown()
        settle()
        assertTrue(c.note.isPlaying)
        row.performTouchUp()
        settle()
        assertFalse(c.note.isPlaying)

        SettingsModel.toggleNotes = true
        row.performTap()
        settle()
        assertTrue(c.note.isPlaying)
        compose.onNodeWithTag(TestTags.MAJOR_MINOR_FAB).performClick()
        settle()
        assertFalse("switching lists silences the keys", c.note.isPlaying)
    }

    @Test
    fun keyRowsShowTheSignatureAndTheName() {
        showKeys()
        val text =
            keyRow(TestTags.MAJOR_KEYS, "A flat major, 4 flats").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Text)?.joinToString("|")
        assertEquals("&«|A${NoteText.FLAT}", text)
    }
}
