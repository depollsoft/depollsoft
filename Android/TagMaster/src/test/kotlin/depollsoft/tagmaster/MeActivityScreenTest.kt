package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Home: its title and actions, the Lists group (Teachable Tags, the user's lists, New list…),
 * Favorites, the footer, list management from the rows, and edit mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class MeActivityScreenTest : ComposeScreenTest() {
    private val fixture = ScreenTestSupport.fixtureTag()

    private fun home(): MeActivity = launch(MeActivity::class.java)

    private fun top(tag: String) = node(tag).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun homeIsTitledAndOffersSearchSettingsAndEdit() {
        home()
        assertEquals(string(R.string.home_title), text("toolbarTitle"))
        node("searchButton").assertIsEnabled()
        node("settings").assertIsEnabled()
        assertTrue(exists("editSavedList"))
    }

    @Test
    fun theListIsAHeaderThenListsThenFavoritesThenAFooter() {
        home()
        assertTrue(top("browseButton") < top("teachableButton"))
        assertTrue(top("teachableButton") < top("newListButton"))
        assertTrue(top("newListButton") < top("favoritesEmptyText"))
    }

    @Test
    fun theHeaderActionsOpenTheirScreens() {
        val activity = home()
        click("searchButton")
        assertEquals(TagSearchActivity::class.java.name, nextStarted(activity)?.component?.className)
        click("browseButton")
        assertEquals(TagBrowserActivity::class.java.name, nextStarted(activity)?.component?.className)
        click("teachableButton")
        assertEquals(TeachableTagsActivity::class.java.name, nextStarted(activity)?.component?.className)
        click("settings")
        assertEquals(SettingsActivity::class.java.name, nextStarted(activity)?.component?.className)
    }

    @Test
    fun aFavoriteRendersItsTitleAndMarkers() {
        ScreenTestSupport.cacheOnDisk(fixture)
        FavoritesModel.addFavorite(fixture.id)
        home()
        val row = text("savedTag:${fixture.id}")
        assertTrue(row, row.contains(fixture.title!!))
        assertFalse(exists("favoritesEmptyText"))
    }

    @Test
    fun favoritesSurviveRecreation() {
        ScreenTestSupport.cacheOnDisk(fixture)
        FavoritesModel.addFavorite(fixture.id)
        home()
        recreate<MeActivity>()
        assertTrue(exists("savedTag:${fixture.id}"))
    }

    @Test
    fun theTeachableRowCountsItsTags() {
        home()
        assertEquals(pluralCount(0), text("teachableCount"))
        TeachableTagsModel.addTeachableTag(fixture.id)
        idle()
        assertEquals(pluralCount(1), text("teachableCount"))
    }

    private fun pluralCount(count: Int) = app.resources.getQuantityString(R.plurals.list_tag_count, count, count)

    @Test
    fun aListRowFollowsItsTagCountAndRenamesWithoutRestarting() {
        val key = TagLists.create("Afterglow set")
        home()
        assertTrue(text("listRow:$key").contains("Afterglow set"))
        assertTrue(text("listRow:$key").contains(pluralCount(0)))
        ListModel(key).add(fixture.id)
        idle()
        assertTrue(text("listRow:$key").contains(pluralCount(1)))
        TagLists.rename(key, "Pole cats")
        idle()
        assertTrue(text("listRow:$key").contains("Pole cats"))
    }

    @Test
    fun theListsGroupFollowsCreationsAndDeletions() {
        home()
        val key = TagLists.create("Easy tags")
        idle()
        assertTrue(exists("listRow:$key"))
        TagLists.delete(key)
        idle()
        assertFalse(exists("listRow:$key"))
    }

    @Test
    fun aListRowOpensThatList() {
        val key = TagLists.create("Afterglow set")
        val activity = home()
        click("listRow:$key")
        val started = nextStarted(activity)
        assertNotNull(started)
        val expected = TagListActivity.intent(app, key)
        assertEquals(expected.component, started!!.component)
        assertEquals(expected.extras.toString(), started.extras.toString())
    }

    @Test
    fun theNewListRowNamesAndCreatesAList() {
        home()
        click("newListButton")
        node("listNameInput").performTextInput("Tags for the car")
        click("listNameConfirm")
        val key = TagLists.customKeys.single { TagLists.name(it) == "Tags for the car" }
        assertTrue(exists("listRow:$key"))
        assertFalse(exists("dialog"))
    }

    @Test
    fun deletingAListFromItsRowConfirmsWithItsTagCount() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(1)
        ListModel(key).add(2)
        home()
        customAction("listRow:$key", string(R.string.list_row_delete))
        assertTrue(text("dialog").contains(app.resources.getQuantityString(R.plurals.list_delete_message, 2, 2)))
        click("listDeleteConfirm")
        assertFalse(TagLists.customKeys.contains(key))
        assertFalse(exists("listRow:$key"))
    }

    @Test
    fun anEmptyListStillSaysWhatDeletingItDoes() {
        val key = TagLists.create("Easy tags")
        home()
        customAction("listRow:$key", string(R.string.list_row_delete))
        assertTrue(text("dialog").contains(string(R.string.list_delete_message_empty, "Easy tags")))
    }

    @Test
    fun everyRowActionIsAlsoAnAccessibilityAction() {
        val first = TagLists.create("One")
        val second = TagLists.create("Two")
        home()
        val actions = customActions("listRow:$first")
        assertTrue(actions.toString(), actions.contains(string(R.string.list_row_rename)))
        assertTrue(actions.contains(string(R.string.list_row_delete)))
        assertTrue(actions.contains(string(R.string.MoveDown)))
        assertFalse("the first row cannot move up", actions.contains(string(R.string.MoveUp)))
        customAction("listRow:$first", string(R.string.MoveDown))
        assertEquals(listOf(second, first), TagLists.customKeys.toList())
    }

    @Test
    fun aMovedOrderIsStillThereOnTheNextVisit() {
        val first = TagLists.create("One")
        val second = TagLists.create("Two")
        home()
        customAction("listRow:$first", string(R.string.MoveDown))
        recreate<MeActivity>()
        assertTrue(top("listRow:$second") < top("listRow:$first"))
    }

    @Test
    fun editModeGivesListRowsARemoveControlAndSurvivesRecreation() {
        val key = TagLists.create("Afterglow set")
        val activity = home()
        assertFalse(exists("listRemove:$key"))
        click("editSavedList")
        assertTrue(activity.listEditor.isEditing)
        assertTrue(exists("listRemove:$key"))
        val recreated = recreate<MeActivity>()
        assertTrue(recreated.listEditor.isEditing)
        assertTrue(exists("listRemove:$key"))
    }

    @Test
    fun theRemoveControlOnAListRowAsksBeforeDeleting() {
        val key = TagLists.create("Afterglow set")
        home()
        click("editSavedList")
        click("listRemove:$key")
        assertTrue(exists("dialog"))
        assertTrue(TagLists.customKeys.contains(key))
    }

    @Test
    fun editIsOfferedForListsEvenWithNoFavorites() {
        home()
        node("editSavedList").assertIsNotEnabled()
        TagLists.create("Afterglow set")
        idle()
        node("editSavedList").assertIsEnabled()
    }

    @Test
    fun theFooterNamesTheApplicationAndItsInstalledVersion() {
        // Release builds set the version name from the release plan, not from resources.
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        org.robolectric.Shadows.shadowOf(app.packageManager).getInternalMutablePackageInfo(app.packageName).versionName = "9.8.7"
        home()
        val version = hasText("Version 9.8.7")
        node("homeList").performScrollToNode(version)
        idle()
        assertTrue(compose.onAllNodes(version, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText(string(R.string.app_name)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun homeIsAListNotADetailPaneOnAPhone() {
        val activity = home()
        assertFalse(activity.hasDetailPane)
    }

    @Test
    fun aTextFieldIsNamedByItsLabel() {
        home()
        click("openByIdButton")
        val field = node("openTagIdInput").fetchSemanticsNode().config
        assertEquals(listOf(string(R.string.TagId)), field[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription])
        // The floating label and the hint are drawing only; the field carries the name once.
        assertTrue(
            compose.onAllNodes(hasText(string(R.string.TagId)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    @Config(qualifiers = "w640dp-h200dp-land")
    fun onAShortLandscapeScreenWithLargeTextTheDialogKeepsItsButtons() {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        try {
            home()
            click("openByIdButton")
            node("openTagIdInput").performTextInput("0")
            click("openTagConfirm")
            // The error line makes the content taller still; Open must stay on screen.
            node("openTagConfirm").assertIsDisplayed()
            node("dialogButton:${string(R.string.home_cancel)}").assertIsDisplayed()
        } finally {
            org.robolectric.RuntimeEnvironment.setFontScale(1f)
        }
    }
}
