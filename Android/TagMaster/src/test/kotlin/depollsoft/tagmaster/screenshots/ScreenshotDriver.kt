package depollsoft.tagmaster.screenshots

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.viewpager2.widget.ViewPager2
import depollsoft.tagmaster.ListDeleteDialog
import depollsoft.tagmaster.ListNameDialog
import depollsoft.tagmaster.ListPickerDialog
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.R
import depollsoft.tagmaster.RatingsPopup
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TeachableTagsActivity

/**
 * The few steps a screenshot needs that depend on how the screen is built.
 *
 * Everything else a screenshot sets up goes through the models, the intents, the disk cache and the
 * network transport, which the Compose port keeps; only these helpers change with the UI toolkit.
 */
internal object ScreenshotDriver {
    /** Turns on the list's edit mode, as tapping Edit in the toolbar does. */
    fun startEditing(activity: Activity) {
        val menu = MenuBuilder(activity)
        activity.menuInflater.inflate(R.menu.savedlistmenu, menu)
        val item = menu.findItem(R.id.editSavedList)
        when (activity) {
            is MeActivity -> activity.listEditor.selectMenu(item)
            is TeachableTagsActivity -> activity.listEditor.selectMenu(item)
            is TagListActivity -> activity.listEditor.selectMenu(item)
            else -> error("${activity.javaClass.simpleName} has no edit mode")
        }
    }

    /** Shows the tag detail page at [index] (0 Summary, 1 Details, 2 Tracks, 3 Videos). */
    fun selectDetailPage(
        activity: Activity,
        index: Int,
    ) {
        val root =
            when (activity) {
                is TagDetailActivity -> activity.detailFragment!!.requireView()
                else -> activity.window.decorView
            }
        root.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(index, false)
    }

    fun showNewListDialog(activity: AppCompatActivity) = ListNameDialog.create(activity.supportFragmentManager)

    fun showRenameListDialog(
        activity: AppCompatActivity,
        key: String,
    ) = ListNameDialog.rename(activity.supportFragmentManager, key)

    fun showDeleteListDialog(
        activity: AppCompatActivity,
        key: String,
    ) = ListDeleteDialog.show(activity.supportFragmentManager, key)

    fun showListPicker(
        activity: AppCompatActivity,
        tagId: Int,
    ) = ListPickerDialog.show(activity.supportFragmentManager, tagId)

    fun showRatingDialog(activity: AppCompatActivity) = RatingsPopup(activity).show()

    fun showOpenTagDialog(activity: MeActivity) {
        activity.findViewById<View>(R.id.openByIdButton).performClick()
    }
}
