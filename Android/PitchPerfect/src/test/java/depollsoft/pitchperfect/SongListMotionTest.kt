package depollsoft.pitchperfect

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import depollsoft.compose.ListMotion
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Songs list on the real screen moves as RecyclerView's animator moved it: rows slide to their
 * new places when the list is sorted, and a removed song fades out rather than vanishing. Each
 * check looks part-way through the motion, so a list that jumps instead fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class SongListMotionTest {
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
        compose.mainClock.autoAdvance = true
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        SongsModel.get().clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    /** Three songs out of alphabetical order, on the Songs tab. */
    private fun showSongs(): List<PitchedSong> {
        val songs = listOf("Sweet Adeline", "Blue Skies", "Shenandoah").map { ComposeScreens.song(it) }
        val list = SongsModel.get().defaultSongList
        songs.forEach(list::addSong)
        val activity = screens.launchMain()
        with(screens) { activity.show(MainTab.SONGS) }
        return songs
    }

    private fun bounds(song: PitchedSong): Rect = compose.onNodeWithTag(TestTags.songRow(song.id), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun top(song: PitchedSong) = bounds(song).top

    /** How dark [area] of the screen is: 0 for blank ground, higher the more ink is drawn there. */
    private fun ink(area: Rect): Float {
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        var total = 0f
        var count = 0
        for (y in area.top.toInt() until area.bottom.toInt() step 2) {
            for (x in area.left.toInt() until area.right.toInt() step 2) {
                val c = pixels[x, y]
                total += 1f - (c.red + c.green + c.blue) / 3f
                count++
            }
        }
        return total / count
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    @Test
    fun sortingSlidesTheRowsToTheirNewPlaces() {
        val (sweet, blue, _) = showSongs()
        compose.onNodeWithTag(TestTags.EDIT_SONGS).performClick()
        screens.settle()
        val from = top(blue)
        val to = top(sweet)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(TestTags.SORT_SONGS).performClick()
        compose.waitForIdle()
        advance(ListMotion.MOVE_MILLIS / 2L)
        val midway = top(blue)
        assertTrue("Blue Skies is sliding up: $midway between $to and $from", midway < from - 1f && midway > to + 1f)
        advance(ListMotion.MOVE_MILLIS.toLong())
        assertEquals("and lands in the top row", to, top(blue), 1f)
    }

    @Test
    fun aRemovedSongFadesOutRatherThanVanishing() {
        // The last row, so no row slides up into its place while it goes.
        val (_, _, shenandoah) = showSongs()
        val slot = bounds(shenandoah)
        // Only the title's side of the row: the key letter sits apart at the far edge.
        val title = Rect(slot.left, slot.top, slot.left + slot.width / 2f, slot.bottom)
        val shown = ink(title)
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { SongsModel.get().defaultSongList.removeSong(shenandoah) }
        // A frame to apply the removal, and a little of the fade.
        advance(32)
        val fading = ink(title)
        advance(ListMotion.FADE_MILLIS.toLong() + ListMotion.MOVE_MILLIS)
        val gone = ink(title)
        assertTrue("the row's ink goes once it has faded: $gone < $shown", gone < shown)
        // Gone at once, the slot would already be the bare background.
        val left = (fading - gone) / (shown - gone)
        assertTrue("the removed row is still drawn as it fades: $left of its ink", left > 0.3f)
    }
}
