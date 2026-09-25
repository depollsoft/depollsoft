package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tag detail screen: its title and pages, the list chips on the Summary page, and the list
 * picker behind "Add to list", each following changes made anywhere while it is open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class TagDetailActivityScreenTest : ComposeScreenTest() {
    private val fixture = ScreenTestSupport.fixtureTag()

    private fun detail(): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(fixture)
        val activity =
            launch(
                TagDetailActivity::class.java,
                Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, fixture.id),
            )
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        return activity
    }

    private fun left(tag: String) = node(tag).fetchSemanticsNode().boundsInRoot.let { it.top * 10000 + it.left }

    private fun toggle(tag: String) = node(tag).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]

    private fun page(index: Int) = click("detailTab:$index")

    @Test
    fun theScreenIsTitledByItsTagAndOpensOnTheSummary() {
        val activity = detail()
        assertEquals(fixture.title, text("toolbarTitle"))
        assertEquals(0, activity.detail.page)
        assertTrue(exists("detailPager"))
        assertTrue(exists("detailTabs"))
        assertTrue(exists("playKeyNoteButton"))
    }

    @Test
    fun everyPageIsReachedFromItsTab() {
        val activity = detail()
        page(1)
        assertEquals(1, activity.detail.page)
        assertTrue(compose.onAllNodes(hasText(fixture.arranger!!), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        page(2)
        assertTrue(exists("balance"))
        assertTrue(compose.onAllNodes(hasText(fixture.recordingMethod!!), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        page(3)
        assertTrue(exists("video:1"))
        page(0)
        assertEquals(0, activity.detail.page)
    }

    @Test
    fun theSelectedPageSurvivesRecreation() {
        detail()
        page(2)
        val recreated = recreate<TagDetailActivity>()
        ScreenTestSupport.awaitTagLoaded(recreated)
        idle()
        assertEquals(2, recreated.detail.page)
        assertTrue(exists("balance"))
    }

    @Test
    fun everyAvailablePartIsOfferedAndSelectsItself() {
        detail()
        page(2)
        for (index in 0..4) assertTrue("part $index", exists("part:$index"))
        assertFalse(exists("part:5"))
        click("part:3")
        assertTrue(node("part:3").fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertFalse(node("part:0").fetchSemanticsNode().config[SemanticsProperties.Selected])
    }

    @Test
    fun aTagInNoListStillOffersAddToList() {
        detail()
        assertTrue(exists("chip:add"))
        assertFalse(exists("chip:${TagLists.FAVORITE}"))
    }

    @Test
    fun everyListTheTagIsInGetsAChipInDisplayOrder() {
        val later = TagLists.create("Zebra")
        val earlier = TagLists.create("Afterglow set")
        FavoritesModel.addFavorite(fixture.id)
        ListModel(later).add(fixture.id)
        ListModel(earlier).add(fixture.id)
        detail()
        val keys = TagLists.allKeys().filter { ListModel(it).contains(fixture.id) }
        val chips = keys.map { "chip:$it" } + "chip:add"
        assertEquals(chips, chips.sortedBy(::left))
    }

    @Test
    fun aChipFollowsMembershipAndRenamesWhileOpen() {
        val key = TagLists.create("Afterglow set")
        detail()
        assertFalse(exists("chip:$key"))
        ListModel(key).add(fixture.id)
        idle()
        assertTrue(text("chip:$key").contains("Afterglow set"))
        TagLists.rename(key, "Pole cats")
        idle()
        assertTrue(text("chip:$key").contains("Pole cats"))
        ListModel(key).remove(fixture.id)
        idle()
        assertFalse(exists("chip:$key"))
    }

    @Test
    fun aChipOpensTheListItNames() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(fixture.id)
        val activity = detail()
        click("chip:$key")
        val started = nextStarted(activity)
        assertEquals(TagListActivity::class.java.name, started?.component?.className)
        assertEquals(TagListActivity.intent(app, key).extras.toString(), started!!.extras.toString())
    }

    @Test
    fun removingFromAChipCanBeUndoneToTheSamePlace() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(1)
        ListModel(key).add(fixture.id)
        ListModel(key).add(2)
        detail()
        customAction("chip:$key", string(R.string.list_chip_remove, "Afterglow set"))
        assertEquals(listOf(1, 2), ListModel(key).ids.toList())
        compose
            .onAllNodes(hasText(string(R.string.list_undo)))
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        idle()
        assertEquals(listOf(1, fixture.id, 2), ListModel(key).ids.toList())
    }

    @Test
    fun thePickerNamesEveryListAndMarksTheOnesTheTagIsIn() {
        val key = TagLists.create("Afterglow set")
        TeachableTagsModel.addTeachableTag(fixture.id)
        detail()
        click("chip:add")
        assertEquals(ToggleableState.Off, toggle("pickerRow:${TagLists.FAVORITE}"))
        assertEquals(ToggleableState.On, toggle("pickerRow:${TagLists.TEACHABLE}"))
        assertEquals(ToggleableState.Off, toggle("pickerRow:$key"))
        assertTrue(exists("pickerNewList"))
    }

    @Test
    fun aPickerRowTogglesMembershipAndTheToolbarFollows() {
        val key = TagLists.create("Afterglow set")
        detail()
        assertTrue(exists("addFavorite"))
        click("chip:add")
        click("pickerRow:$key")
        assertTrue(ListModel(key).contains(fixture.id))
        assertEquals(ToggleableState.On, toggle("pickerRow:$key"))
        click("pickerRow:${TagLists.FAVORITE}")
        assertTrue(FavoritesModel.favoriteIds.contains(fixture.id))
        assertTrue(exists("removeFavorite"))
        click("pickerRow:$key")
        assertFalse(ListModel(key).contains(fixture.id))
    }

    @Test
    fun thePickerFollowsListsCreatedRenamedAndDeletedWhileOpen() {
        detail()
        click("chip:add")
        val key = TagLists.create("Afterglow set")
        idle()
        assertTrue(text("pickerRow:$key").contains("Afterglow set"))
        TagLists.rename(key, "Pole cats")
        idle()
        assertTrue(text("pickerRow:$key").contains("Pole cats"))
        ListModel(key).add(fixture.id)
        idle()
        assertEquals(ToggleableState.On, toggle("pickerRow:$key"))
        TagLists.delete(key)
        idle()
        assertFalse(exists("pickerRow:$key"))
    }

    @Test
    fun theNewListRowMakesTheListAndPutsTheTagInIt() {
        detail()
        click("chip:add")
        click("pickerNewList")
        node("listNameInput").performTextInput("Tags for the car")
        click("listNameConfirm")
        val key = TagLists.customKeys.single { TagLists.name(it) == "Tags for the car" }
        assertTrue(ListModel(key).contains(fixture.id))
    }
}
