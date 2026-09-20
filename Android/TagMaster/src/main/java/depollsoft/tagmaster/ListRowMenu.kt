package depollsoft.tagmaster

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The manage-this-list actions a Home row offers: Rename…, Move up, Move down, Delete….
 *
 * They are reachable three ways — a long press, the same popup from a keyboard context click, and
 * accessibility custom actions — because a long press is not available to a screen reader or a
 * keyboard, and a row of small buttons is not worth the width.
 */
object ListRowMenu {
    private const val ITEM_RENAME = 1
    private const val ITEM_MOVE_UP = 2
    private const val ITEM_MOVE_DOWN = 3
    private const val ITEM_DELETE = 4

    /** Wires [row] (a list row for [key]) to the popup and the equivalent custom actions. */
    fun install(
        activity: FragmentActivity,
        row: View,
        key: String,
        request: String = ListNameDialog.REQUEST_DEFAULT,
    ) {
        row.isLongClickable = true
        row.setOnLongClickListener {
            show(activity, row, key, request)
            true
        }
        ViewCompat.addAccessibilityAction(row, activity.getString(R.string.list_row_rename)) { _, _ ->
            rename(activity, key, request)
            true
        }
        if (TagLists.canMoveUp(key)) {
            ViewCompat.addAccessibilityAction(row, activity.getString(R.string.MoveUp)) { _, _ -> TagLists.moveUp(key) }
        }
        if (TagLists.canMoveDown(key)) {
            ViewCompat.addAccessibilityAction(row, activity.getString(R.string.MoveDown)) { _, _ -> TagLists.moveDown(key) }
        }
        ViewCompat.addAccessibilityAction(row, activity.getString(R.string.list_row_delete)) { _, _ ->
            confirmDelete(activity, key)
            true
        }
    }

    /** Opens the actions for [key] anchored to [anchor]. */
    fun show(
        activity: FragmentActivity,
        anchor: View,
        key: String,
        request: String = ListNameDialog.REQUEST_DEFAULT,
    ) {
        if (!TagLists.customKeys.contains(key)) return
        val popup = PopupMenu(anchor.context, anchor)
        popup.menu.add(Menu.NONE, ITEM_RENAME, 0, R.string.list_row_rename)
        if (TagLists.canMoveUp(key)) popup.menu.add(Menu.NONE, ITEM_MOVE_UP, 1, R.string.MoveUp)
        if (TagLists.canMoveDown(key)) popup.menu.add(Menu.NONE, ITEM_MOVE_DOWN, 2, R.string.MoveDown)
        popup.menu.add(Menu.NONE, ITEM_DELETE, 3, R.string.list_row_delete)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ITEM_RENAME -> rename(activity, key, request)
                ITEM_MOVE_UP -> TagLists.moveUp(key)
                ITEM_MOVE_DOWN -> TagLists.moveDown(key)
                ITEM_DELETE -> confirmDelete(activity, key)
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.show()
    }

    fun rename(
        activity: FragmentActivity,
        key: String,
        request: String = ListNameDialog.REQUEST_DEFAULT,
    ) = ListNameDialog.rename(activity.supportFragmentManager, key, request)

    fun confirmDelete(
        activity: FragmentActivity,
        key: String,
    ) = ListDeleteDialog.show(activity.supportFragmentManager, key)
}

/**
 * "Delete “Afterglow set”?" — with the number of tags that go with it, so nobody deletes a
 * forty-tag set thinking it was the empty one. A DialogFragment, so it survives a rotation.
 */
class ListDeleteDialog : DialogFragment() {
    private val listKey: String
        get() = requireArguments().getString(ARG_KEY).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val key = listKey
        val count = ListModel(key).ids.size
        val message =
            if (count == 0) {
                getString(R.string.list_delete_message_empty)
            } else {
                resources.getQuantityString(R.plurals.list_delete_message, count, count)
            }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.list_delete_title, TagLists.name(key)))
            .setMessage(message)
            .setNegativeButton(R.string.home_cancel, null)
            .setPositiveButton(R.string.list_delete) { _, _ ->
                if (TagLists.isCustom(key)) TagLists.delete(key)
            }.create()
    }

    override fun onStart() {
        super.onStart()
        // Deleting a list is the destructive choice, and reads as one.
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorError, Color.RED),
        )
    }

    companion object {
        private const val ARG_KEY = "depollsoft.tagmaster.listDelete.key"
        private const val FRAGMENT_TAG = "depollsoft.tagmaster.listDeleteDialog"

        fun show(
            manager: FragmentManager,
            key: String,
        ) {
            if (manager.isStateSaved || manager.findFragmentByTag(FRAGMENT_TAG) != null) return
            ListDeleteDialog()
                .apply { arguments = Bundle().apply { putString(ARG_KEY, key) } }
                .show(manager, FRAGMENT_TAG)
        }
    }
}
