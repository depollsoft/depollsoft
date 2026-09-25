package depollsoft.tagmaster.screenshots

import android.app.Activity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.TagBrowserActivity
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TagPaneHost
import depollsoft.tagmaster.TeachableTagsActivity

/**
 * The few steps a screenshot needs that depend on how the screen is built.
 *
 * Everything else a screenshot sets up goes through the models, the intents, the disk cache and the
 * network transport; only these helpers know the screens are Compose.
 */
internal class ScreenshotDriver(
    private val compose: ComposeTestRule,
) {
    /** Turns on the list's edit mode, as tapping Edit in the toolbar does. */
    fun startEditing(activity: Activity) {
        val editor =
            when (activity) {
                is MeActivity -> activity.listEditor
                is TeachableTagsActivity -> activity.listEditor
                is TagListActivity -> activity.listEditor
                else -> error("${activity.javaClass.simpleName} has no edit mode")
            }
        if (!editor.isEditing) editor.toggle()
        compose.waitForIdle()
    }

    /** Shows the tag detail page at [index] (0 Summary, 1 Details, 2 Tracks, 3 Videos). */
    fun selectDetailPage(
        activity: Activity,
        index: Int,
    ) {
        val detail =
            when (activity) {
                is TagDetailActivity -> activity.detail
                is MeActivity -> activity.tagPane.detail
                is TagListActivity -> activity.tagPane.detail
                is TeachableTagsActivity -> activity.tagPane.detail
                is TagBrowserActivity -> activity.tagPane.detail
                else -> null
            }
        requireNotNull(detail) { "${activity.javaClass.simpleName} is not showing a tag" }.page = index
        compose.waitForIdle()
    }

    fun showNewListDialog() = click("newListButton")

    fun showRenameListDialog(key: String) {
        compose.onNodeWithTag("listRow:$key").performTouchInput { longClick() }
        click("menuRename")
    }

    fun showDeleteListDialog(key: String) {
        compose.onNodeWithTag("listRow:$key").performTouchInput { longClick() }
        click("menuDelete")
    }

    fun showListPicker() = click("chip:add")

    fun showRatingDialog() = click("rateButton")

    fun showOpenTagDialog() = click("openByIdButton")

    fun showTag(
        activity: Activity,
        id: Int,
    ) {
        (activity as TagPaneHost).showTag(id)
        compose.waitForIdle()
    }

    private fun click(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }
}
