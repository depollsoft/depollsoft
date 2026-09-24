package depollsoft.pitchperfect.screenshots

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.PurchaseService
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.capture
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.playKey
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.setEditingSongs
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showTab
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.toggleKeyMode
import org.junit.After
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The four tabs of the main screen, on a phone, in both themes and on a tablet. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class MainScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() = ScreenshotSupport.setUp(compose)

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    /** The breathing glow is animated; with animations off it rests at its brightest phase. */
    private fun reduceMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    private fun note(
        name: String,
        accidental: Accidental,
        octave: Int,
    ): Note = Note.findNote(name, accidental, octave)

    @Test
    fun pitchPipe() = launchMain().capture("main_pitch_pipe")

    @Test
    fun pitchPipeHighRange() {
        // The range is a stored preference, read when the pitch pipe's model is made.
        depollsoft.pitchperfect.PitchPipeModel().isFromFToF = true
        launchMain().capture("main_pitch_pipe_f_to_f")
    }

    @Test
    fun pitchPipeOneNote() {
        reduceMotion()
        note("A", Accidental.Natural, 4).play()
        launchMain().capture("main_pitch_pipe_one_note")
    }

    @Test
    fun pitchPipeChord() {
        reduceMotion()
        note("C", Accidental.Natural, 4).play()
        note("E", Accidental.Natural, 4).play()
        note("G", Accidental.Natural, 4).play()
        launchMain().capture("main_pitch_pipe_chord")
    }

    @Test
    fun pitchPipeInterval() {
        reduceMotion()
        note("C", Accidental.Natural, 4).play()
        note("F", Accidental.Sharp, 4).play()
        launchMain().capture("main_pitch_pipe_interval")
    }

    @Test
    fun pitchPipeWithAdSlot() {
        PurchaseService.areAdsRemoved = false
        launchMain().capture("main_pitch_pipe_ad_slot")
    }

    @Test
    fun notes() {
        val activity = launchMain()
        activity.showTab(1)
        activity.capture("main_notes")
    }

    @Test
    fun notesPlaying() {
        Note.getPrunedNotes().first { it.friendlyName == "A" && it.octave == 3 }.play()
        val activity = launchMain()
        activity.showTab(1)
        activity.capture("main_notes_playing")
    }

    @Test
    fun keysMajor() {
        val activity = launchMain()
        activity.showTab(2)
        activity.capture("main_keys_major")
    }

    @Test
    fun keysMinor() {
        val activity = launchMain()
        activity.showTab(2)
        activity.toggleKeyMode()
        activity.capture("main_keys_minor")
    }

    @Test
    fun keysPlaying() {
        val activity = launchMain()
        // The Keys tab silences its notes as it comes up, so the key sounds once it is showing.
        activity.showTab(2)
        activity.playKey(Key.getMajorKeys()[6])
        activity.capture("main_keys_playing")
    }

    @Test
    fun songsEmpty() {
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("main_songs_empty")
    }

    @Test
    fun songs() {
        ScreenshotSupport.seedMySongs()
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("main_songs")
    }

    @Test
    fun songPlaying() {
        ScreenshotSupport.seedMySongs()
        SongsModel.get().defaultSongList.songs[1].play()
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("main_songs_playing")
    }

    @Test
    fun songsEditing() {
        ScreenshotSupport.seedMySongs()
        val activity = launchMain()
        activity.showTab(3)
        activity.setEditingSongs(true)
        activity.capture("main_songs_editing")
    }

    @Test
    fun songsWithSetLists() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        SongsModel.get().currentListId = ids[0]
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("main_songs_set_lists")
    }

    @Test
    fun songsEmptySetList() {
        val ids = ScreenshotSupport.seedSetLists()
        SongsModel.get().currentListId = ids[1]
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("main_songs_empty_set_list")
    }

    @Test
    fun songsEditingWithSetLists() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.setEditingSongs(true)
        activity.capture("main_songs_set_lists_editing")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun pitchPipeNight() = launchMain().capture("night_main_pitch_pipe")

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun pitchPipeChordNight() {
        reduceMotion()
        note("D", Accidental.Natural, 4).play()
        note("F", Accidental.Sharp, 4).play()
        note("A", Accidental.Natural, 4).play()
        launchMain().capture("night_main_pitch_pipe_chord")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun notesNight() {
        Note.getPrunedNotes().first { it.friendlyName == "A" && it.octave == 3 }.play()
        val activity = launchMain()
        activity.showTab(1)
        activity.capture("night_main_notes_playing")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun keysNight() {
        val activity = launchMain()
        activity.showTab(2)
        activity.capture("night_main_keys_major")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun songsEditingNight() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.setEditingSongs(true)
        activity.capture("night_main_songs_editing")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun pitchPipeTablet() = launchMain().capture("tablet_main_pitch_pipe")

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun keysTablet() {
        val activity = launchMain()
        activity.showTab(2)
        activity.capture("tablet_main_keys_major")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun songsTablet() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("tablet_main_songs")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun songsTabletWithAdSlot() {
        PurchaseService.areAdsRemoved = false
        ScreenshotSupport.seedMySongs()
        val activity = launchMain()
        activity.showTab(3)
        activity.capture("tablet_main_songs_ad_slot")
    }
}
