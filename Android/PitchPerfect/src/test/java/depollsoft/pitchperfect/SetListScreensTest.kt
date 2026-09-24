package depollsoft.pitchperfect

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ComposeScreens.Companion.song
import depollsoft.pitchperfect.lib.Key
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Set Lists screen and the Add Songs picker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SetListScreensTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        resetLists()
    }

    @After
    fun tearDown() {
        resetLists()
        screens.finish()
    }

    private fun resetLists() {
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    // ==================== Set Lists ====================

    private fun manage(): ManageSetListsActivity = screens.launch(ManageSetListsActivity::class.java).get()

    private fun rowText(listId: String): List<String> =
        compose.onNodeWithTag(TestTags.manageRow(listId)).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()

    private fun handleOf(listId: String) =
        compose.onNode(hasTestTag(TestTags.DRAG_HANDLE) and hasAnyAncestor(hasTestTag(TestTags.manageRow(listId))), useUnmergedTree = true)

    private fun rowActions(listId: String): List<String> =
        compose.onNodeWithTag(TestTags.manageRow(listId)).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.CustomActions)?.map { it.label }.orEmpty()

    @Test
    fun mySongsIsTheFirstRowAndCarriesItsCount() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val other = model.createList("Saturday show")
        val activity = manage()
        assertEquals(listOf(SongsModel.DEFAULT_ID, other), activity.state.rows.map { it.id })
        assertEquals(listOf("My Songs", "1 song"), rowText(SongsModel.DEFAULT_ID))
        handleOf(SongsModel.DEFAULT_ID).assertDoesNotExist()
        assertEquals("the current list says so", "My Songs, current set list", screens.description(TestTags.manageRow(SongsModel.DEFAULT_ID)))
    }

    @Test
    fun anEmptyCustomListReadsAsNoSongsAndCanBeDragged() {
        val id = model.createList("Saturday show")
        manage()
        assertEquals(listOf("Saturday show", "No songs"), rowText(id))
        handleOf(id).assertExists()
    }

    @Test
    fun aDropPersistsTheNewOrderOnce() {
        val a = model.createList("Alpha")
        val b = model.createList("Bravo")
        val c = model.createList("Charlie")
        val activity = manage()

        // Drag Charlie by its handle up past Bravo and Alpha, then drop.
        val rowHeight = compose.onNodeWithTag(TestTags.manageRow(c)).fetchSemanticsNode().size.height.toFloat()
        handleOf(c).performTouchInput {
            down(center)
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(0f, -rowHeight / 4f)) }
        }
        screens.settle()
        assertEquals("nothing is stored mid-drag", 2L, model.songLists[c]!!.order)
        handleOf(c).performTouchInput { up() }
        screens.settle()

        assertEquals(0L, model.songLists[c]!!.order)
        assertEquals(1L, model.songLists[a]!!.order)
        assertEquals(2L, model.songLists[b]!!.order)
        assertEquals(listOf(SongsModel.DEFAULT_ID, c, a, b), model.orderedLists.map { it.id })
        assertEquals(listOf(SongsModel.DEFAULT_ID, c, a, b), activity.state.rows.map { it.id })
    }

    @Test
    fun aDragNeverMovesAListAboveMySongs() {
        val a = model.createList("Alpha")
        manage()
        val rowHeight = compose.onNodeWithTag(TestTags.manageRow(a)).fetchSemanticsNode().size.height.toFloat()
        handleOf(a).performTouchInput {
            down(center)
            repeat(8) { moveBy(androidx.compose.ui.geometry.Offset(0f, -rowHeight / 4f)) }
            up()
        }
        screens.settle()
        assertEquals(listOf(SongsModel.DEFAULT_ID, a), model.orderedLists.map { it.id })
    }

    @Test
    fun customRowsOfferMoveUpAndMoveDownToScreenReaders() {
        val first = model.createList("Saturday show")
        val second = model.createList("Afterglow")
        manage()

        assertTrue("My Songs never moves", rowActions(SongsModel.DEFAULT_ID).isEmpty())
        assertEquals(listOf("Move Down"), rowActions(first))
        assertEquals(listOf("Move Up"), rowActions(second))

        compose.onNodeWithTag(TestTags.manageRow(second)).fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single().action()
        screens.settle()
        assertEquals(listOf(SongsModel.DEFAULT_ID, second, first), model.orderedLists.map { it.id })
        assertEquals(0L, model.songLists[second]!!.order)
        assertEquals(1L, model.songLists[first]!!.order)
        assertEquals(listOf("Move Down"), rowActions(second))
    }

    @Test
    fun tappingARowSwitchesToItAndReturnsToSongs() {
        val other = model.createList("Saturday show")
        val activity = manage()
        screens.click(TestTags.manageRow(other))
        assertEquals("a row tap makes the list current", other, model.currentListId)
        assertTrue("and returns to the Songs tab", activity.isFinishing)
        assertFalse("renaming is the row menu's job", screens.exists(TestTags.NAME_DIALOG_FIELD))
    }

    @Test
    fun theRowMenuRenamesDuplicatesAndDeletes() {
        val id = model.createList("Saturday show")
        model.songLists[id]!!.addSong(song("Blue Skies"))
        val activity = manage()
        fun openMenu(listId: String) {
            compose.onNode(hasTestTag(TestTags.ROW_OVERFLOW) and hasAnyAncestor(hasTestTag(TestTags.manageRow(listId))), useUnmergedTree = true)
                .performClick()
            screens.settle()
        }

        openMenu(id)
        compose.onNodeWithText("Duplicate set list").performClick()
        screens.settle()
        val copy = model.songLists.values.single { model.displayName(it) == "Saturday show copy" }
        assertEquals(1, copy.songs.size)
        assertEquals(3, activity.state.rows.size)

        openMenu(id)
        compose.onNodeWithText("Rename").performClick()
        screens.settle()
        assertTrue(screens.exists(TestTags.NAME_DIALOG_FIELD))
        compose.onNode(hasTestTag(TestTags.NAME_DIALOG_FIELD) and hasText("Saturday show")).assertExists()
        compose.onNodeWithText("CANCEL").performClick()
        screens.settle()

        openMenu(id)
        compose.onNodeWithText("Delete").performClick()
        screens.settle()
        compose.onNodeWithText("This removes the set list and its 1 song. My Songs is not affected.").assertExists()
        compose.onNodeWithText("DELETE").performClick()
        screens.settle()
        assertFalse(id in model.songLists.keys)
        assertEquals(2, activity.state.rows.size)
    }

    @Test
    fun theDefaultRowMenuOffersNoDelete() {
        model.createList("Saturday show")
        manage()
        compose.onNode(hasTestTag(TestTags.ROW_OVERFLOW) and hasAnyAncestor(hasTestTag(TestTags.manageRow(SongsModel.DEFAULT_ID))), useUnmergedTree = true)
            .performClick()
        screens.settle()
        compose.onNodeWithText("Rename").assertExists()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        model.deleteList(SongsModel.DEFAULT_ID)
        assertTrue("the model refuses to delete My Songs whatever the menu shows", model.songLists.containsKey(SongsModel.DEFAULT_ID))
    }

    @Test
    fun theNewSetListButtonCreatesAList() {
        val activity = manage()
        screens.click(TestTags.NEW_SET_LIST)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("Chapter show")
        screens.click(TestTags.NAME_DIALOG_CONFIRM)
        assertEquals(listOf("My Songs", "Chapter show"), activity.state.rows.map { model.displayName(it) })
        assertEquals("creating from here does not switch lists", SongsModel.DEFAULT_ID, model.currentListId)
    }

    @Test
    fun anOpenPromptAndItsHalfTypedNameSurviveRecreation() {
        val controller = screens.launch(ManageSetListsActivity::class.java)
        screens.click(TestTags.NEW_SET_LIST)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("Chapt")
        controller.recreate()
        screens.settle()
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).assert(hasText("Chapt"))
        screens.click(TestTags.NAME_DIALOG_CONFIRM)
        assertEquals(listOf("My Songs", "Chapt"), controller.get().state.rows.map { model.displayName(it) })
    }

    @Test
    fun aPendingDeleteSurvivesRecreation() {
        val other = model.createList("Saturday show")
        val controller = screens.launch(ManageSetListsActivity::class.java)
        controller.get().state.pendingDelete = other
        screens.settle()
        controller.recreate()
        screens.settle()
        assertEquals(other, controller.get().state.pendingDelete)
    }

    @Test
    fun aRemoteChangeReRendersTheRows() {
        val activity = manage()
        assertEquals(1, activity.state.rows.size)
        val id = model.createList("Arrived from elsewhere")
        screens.settle()
        assertEquals(listOf("Arrived from elsewhere", "No songs"), rowText(id))
    }

    // ==================== Add Songs ====================

    private lateinit var targetId: String

    private fun picker(): AddSongsFromListActivity {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), AddSongsFromListActivity::class.java)
                .putExtra(AddSongsFromListActivity.LIST_EXTRA, targetId)
        return screens.launch(AddSongsFromListActivity::class.java, intent).get()
    }

    private fun seedTarget() {
        targetId = model.createList("Saturday show")
        model.currentListId = targetId
    }

    private fun ticked(song: depollsoft.pitchperfect.lib.PitchedSong): Boolean =
        compose.onNodeWithTag(TestTags.addableRow(song.id)).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On

    @Test
    fun oneSectionPerOtherListWithSongsToOffer() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        val afterglow = model.createList("Afterglow")
        model.songLists[afterglow]!!.addSong(song("Shenandoah"))
        model.createList("Empty one")
        val activity = picker()

        assertEquals(listOf("My Songs", "Afterglow"), activity.addable.sections.map { it.first })
        compose.onNodeWithText("MY SONGS").assertExists()
        compose.onNodeWithText("AFTERGLOW").assertExists()
        compose.onNodeWithText("EMPTY ONE").assertDoesNotExist()
        compose.onNodeWithText("Blue Skies").assertExists()
        compose.onNodeWithText("Shenandoah").assertExists()
    }

    @Test
    fun aSongTheTargetAlreadyHasIsOmitted() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", Key.getMajorKeys()[3]))
        // Same title in another case, same key.
        model.songLists[targetId]!!.addSong(song("BLUE SKIES"))
        picker()
        compose.onNode(hasText("Blue Skies") and hasAnyAncestor(hasTestTag(TestTags.ADDABLE_LIST))).assertDoesNotExist()
        compose.onNodeWithText("Shenandoah").assertExists()
    }

    @Test
    fun nothingToAddShowsTheEmptyStateInsteadOfTheList() {
        seedTarget()
        picker()
        assertFalse(screens.exists(TestTags.ADDABLE_LIST))
        assertTrue(screens.exists(TestTags.NOTHING_TO_ADD))
        assertFalse("select all needs something to select", screens.exists(TestTags.OVERFLOW))
    }

    @Test
    fun selectAllTicksEveryOfferedSongAndThenClears() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", Key.getMajorKeys()[3]))
        val activity = picker()

        screens.click(TestTags.OVERFLOW)
        compose.onNodeWithText("Select all").performClick()
        screens.settle()
        assertEquals(2, activity.addable.count)
        compose.onNodeWithText("ADD 2 SONGS").assertExists()

        screens.click(TestTags.OVERFLOW)
        compose.onNodeWithText("Clear selection").performClick()
        screens.settle()
        assertEquals(0, activity.addable.count)
        screens.click(TestTags.OVERFLOW)
        compose.onNodeWithText("Select all").assertExists()
    }

    @Test
    fun theConfirmActionsCountTheSelection() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", Key.getMajorKeys()[3]))
        val activity = picker()
        val (blue, shenandoah) = model.defaultSongList.songs.toList()

        compose.onNodeWithText("ADD").assertExists()
        compose.onNodeWithTag(TestTags.CONFIRM_ADD_BUTTON).assertIsNotEnabled()
        compose.onNodeWithTag(TestTags.CONFIRM_ADD).assertIsNotEnabled()

        screens.click(TestTags.addableRow(blue.id))
        assertTrue(ticked(blue))
        compose.onNodeWithText("ADD 1 SONG").assertExists()
        compose.onNodeWithTag(TestTags.CONFIRM_ADD_BUTTON).assertIsEnabled()
        compose.onNodeWithContentDescription("Add 1 song").assertIsEnabled()

        screens.click(TestTags.addableRow(shenandoah.id))
        compose.onNodeWithText("ADD 2 SONGS").assertExists()

        // Tapping again un-ticks.
        screens.click(TestTags.addableRow(shenandoah.id))
        assertFalse(ticked(shenandoah))
        compose.onNodeWithText("ADD 1 SONG").assertExists()
        assertEquals(1, activity.addable.count)
    }

    @Test
    fun confirmingAppendsDeepCopiesWithFreshIdsInSourceOrder() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", Key.getMajorKeys()[3]))
        val activity = picker()
        val (blue, shenandoah) = model.defaultSongList.songs.toList()

        // Tick the second song first: the copies must still land in source order.
        screens.click(TestTags.addableRow(shenandoah.id))
        screens.click(TestTags.addableRow(blue.id))
        screens.click(TestTags.CONFIRM_ADD_BUTTON)

        val target = model.songLists[targetId]!!.songs
        assertEquals(listOf("Blue Skies", "Shenandoah"), target.map { it.name })
        model.defaultSongList.songs.zip(target).forEach { (left, right) ->
            assertNotEquals("a copy is its own song", left.id, right.id)
            assertEquals(left.key, right.key)
        }
        assertTrue("confirming closes the picker", activity.isFinishing)
    }

    @Test
    fun confirmingWithNothingSelectedDoesNothing() {
        seedTarget()
        model.defaultSongList.addSong(song("Blue Skies"))
        val activity = picker()
        activity.confirm()
        screens.settle()
        assertEquals(0, model.songLists[targetId]!!.songs.size)
        assertFalse(activity.isFinishing)
    }
}
