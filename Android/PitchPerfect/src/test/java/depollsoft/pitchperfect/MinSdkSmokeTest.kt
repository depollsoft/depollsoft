package depollsoft.pitchperfect

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * The main screens on the oldest Android the app supports (minSdk 24) and on the first release
 * that has the platform calls other code guards (26). Every other test runs on SDK 35, where a call
 * newer than minSdk can't fail; here a missing method throws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 26], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class MinSdkSmokeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        SongsModel.get().clearAll()
        Note.setPlayer(ScreenTestSupport.silentPlayer)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        SettingsModel.toggleNotes = false
        SongsModel.get().clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    @Test
    fun theEmptySongListLaysOutItsWrappedMessage() {
        // The message wraps, so LegacyText builds a StaticLayout rather than a BoringLayout.
        val activity = screens.launchMain()
        with(screens) { activity.show(MainTab.SONGS) }
        assertTrue("the empty Songs message is shown", screens.exists(TestTags.SONGS_EMPTY))
    }

    @Test
    fun everyTabShows() {
        val activity = screens.launchMain()
        for (tab in MainTab.entries) {
            with(screens) { activity.show(tab) }
            assertTrue("$tab is showing", screens.isSelected(tab.testTag))
        }
        // Songs in edit mode, and the new set list prompt.
        SongsModel.get().defaultSongList.addSong(ComposeScreens.song("Shenandoah"))
        screens.click(TestTags.EDIT_SONGS)
        assertTrue("editing", activity.songs.editing)
        screens.click(TestTags.SET_LIST_ADD_POSITION)
        assertTrue("the name prompt opens", screens.exists(TestTags.NAME_DIALOG_FIELD))
    }

    @Test
    fun everyOtherScreenOpens() {
        val song = ComposeScreens.song("Shenandoah")
        SongsModel.get().defaultSongList.addSong(song)
        val setList = SongsModel.get().createList("Contest Set")
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val screensToOpen =
            listOf(
                android.content.Intent(app, AddSongActivity::class.java).putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID),
                android.content.Intent(app, AddSongActivity::class.java)
                    .putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID)
                    .putExtra(AddSongActivity.ID_EXTRA, song.id),
                android.content.Intent(app, AddSongsFromListActivity::class.java).putExtra(AddSongsFromListActivity.LIST_EXTRA, setList),
                android.content.Intent(app, ManageSetListsActivity::class.java),
                android.content.Intent(app, SettingsActivity::class.java),
            )
        for (intent in screensToOpen) {
            @Suppress("UNCHECKED_CAST")
            val type = Class.forName(intent.component!!.className) as Class<android.app.Activity>
            val controller = screens.launch(type, intent)
            assertTrue("${type.simpleName} is composed", compose.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty())
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun aLatchedPitchPipeNoteBreathes() {
        // A sounding note starts the glow, which reads the system's animator duration scale.
        SettingsModel.toggleNotes = true
        screens.launchMain()
        compose.onNodeWithContentDescription("A, octave 4").performTouchInput { click() }
        screens.settle()
        assertTrue("A4 is sounding", Note.getCommonNotes().any { it.isPlaying })
    }
}
