package depollsoft.pitchperfect

import android.app.Activity
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A user's path through Pitch Perfect's set lists, each screen started from the intent the one
 * before sent: make a set list, fill it from My Songs, reorder it, turn the phone, delete it, and
 * undo the delete. Each screen has its own tests; this one catches a break in the hand-offs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class JourneyScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        model.clearAll()
        Note.setPlayer(ScreenTestSupport.silentPlayer)
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        model.clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    private fun string(id: Int) = org.robolectric.RuntimeEnvironment.getApplication().getString(id)

    private fun started(from: Activity): Intent = shadowOf(from).nextStartedActivity ?: error("${from.javaClass.simpleName} started nothing")

    private fun customAction(
        tag: String,
        label: String,
    ) {
        val actions = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val action = actions.first { it.label == label }
        compose.runOnIdle { action.action() }
        screens.settle()
    }

    private fun titles() = model.currentList.songs.map { it.name }

    @Test
    fun makeFillReorderRotateDeleteAndUndoASetList() {
        val library: List<PitchedSong> = listOf("Blue Skies", "Heart of My Heart", "Shenandoah").map { ComposeScreens.song(it) }
        library.forEach(model.defaultSongList::addSong)
        val main = screens.launch(PitchPerfectActivity::class.java)
        with(screens) { main.get().show(MainTab.SONGS) }

        // A new set list, named in the prompt; it becomes the current list.
        screens.click(TestTags.SET_LIST_ADD_POSITION)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("Contest Set")
        screens.click(TestTags.NAME_DIALOG_CONFIRM)
        assertEquals("Contest Set", model.currentList.name)
        val setList = model.currentListId

        // Filled from My Songs on the Add Songs screen the overflow opens.
        screens.click(TestTags.EDIT_SONGS)
        screens.click(TestTags.OVERFLOW)
        compose.onNodeWithTag(TestTags.ADD_FROM_LIST).performClick()
        screens.settle()
        val adding = screens.launch(AddSongsFromListActivity::class.java, started(main.get()))
        screens.click(TestTags.addableRow(library[2].id))
        screens.click(TestTags.addableRow(library[0].id))
        screens.click(TestTags.CONFIRM_ADD_BUTTON)
        assertTrue("Add Songs closes once it has added", adding.get().isFinishing)
        adding.pause().stop().destroy()
        screens.settle()
        assertEquals(setList, model.currentListId)
        assertEquals("added in list order", listOf("Blue Skies", "Shenandoah"), titles())

        // Reordered with the screen reader's move action, still editing.
        val shenandoah = model.currentList.songs.first { it.name == "Shenandoah" }
        customAction(TestTags.songRow(shenandoah.id), string(R.string.MoveUp))
        assertEquals(listOf("Shenandoah", "Blue Skies"), titles())

        // Turning the phone keeps the list and its order.
        main.recreate()
        screens.settle()
        assertEquals(setList, model.currentListId)
        assertEquals(listOf("Shenandoah", "Blue Skies"), titles())
        assertTrue(screens.exists(TestTags.songRow(shenandoah.id)))

        // Deleted from its menu, then brought back with Undo.
        compose.onNodeWithTag(TestTags.setListPosition(setList)).performSemanticsAction(SemanticsActions.OnLongClick)
        screens.settle()
        compose.onNodeWithText(string(R.string.SetListDeleteAction)).performClick()
        screens.settle()
        compose.onNode(hasText(string(R.string.SetListDelete), ignoreCase = true)).performClick()
        screens.settle()
        assertTrue("the list is gone", setList !in model.songLists.keys)
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        compose.onNode(hasText(string(R.string.Undo), ignoreCase = true)).performClick()
        screens.settle()
        assertEquals("Undo brings it back as the current list", setList, model.currentListId)
        assertEquals(listOf("Shenandoah", "Blue Skies"), titles())
    }
}
