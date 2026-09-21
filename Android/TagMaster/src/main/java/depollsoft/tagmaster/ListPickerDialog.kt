package depollsoft.tagmaster

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.Tracker
import com.bindroid.utils.Function
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** The icon that stands for a list everywhere it is named: Home, the chips, and the picker. */
internal fun listIconRes(key: String): Int =
    when (key) {
        TagLists.FAVORITE -> R.drawable.ic_favorite
        TagLists.TEACHABLE -> R.drawable.ic_people
        else -> R.drawable.ic_list
    }

/** The trailing count a list row shows: "1 tag", "12 tags". */
internal fun listCountText(
    context: Context,
    key: String,
): String {
    val count = ListModel(key).ids.size
    return context.resources.getQuantityString(R.plurals.list_tag_count, count, count)
}

/**
 * "Add to list": every list the tag could be in, with a checkmark on the ones it is in, and a
 * New list… row at the bottom.
 *
 * Rows toggle immediately rather than committing on Done, so the chips behind the dialog update
 * as the user works and Done never has to mean anything but "I'm finished".
 *
 * The dialog can stay open for a while, and the lists it is showing are shared state: another
 * device (or another screen of this one) can create, rename, delete or fill a list underneath it.
 * So the rows are rendered from a single Bindroid registration over the registry and every list's
 * ids, and a row only writes once it has re-checked that its list is still there.
 */
class ListPickerDialog : DialogFragment() {
    /** One row, as it is shown: everything a render reads about a list. */
    private data class Row(
        val key: String,
        val name: String,
        val detail: String,
        val checked: Boolean,
    )

    private val tagId: Int
        get() = requireArguments().getInt(ARG_TAG_ID)

    private var rows: LinearLayout? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // The models whose ids the rows read are held for as long as the dialog is up, so the tracked
    // collections are exactly the ones the next render registers on.
    private val trackedLists = mutableListOf<ListModel>()

    // Bindroid registrations are one-shot and each Trackable.track adds another, so the dialog
    // holds exactly one and renews it only once it has fired.
    private var tracking = false
    private var gone = false

    private val tracker =
        object : Tracker {
            override fun update() {
                tracking = false
                if (gone) return
                mainHandler.post { if (!gone) render() }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val column =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        rows = column
        gone = false
        tracking = false
        val scroll = NestedScrollView(context).apply { addView(column) }

        // The name dialog opens on top of this one; its result comes back here, so a rotation in
        // between still lands the new list (and the tag in it) instead of dropping the work.
        parentFragmentManager.setFragmentResultListener(REQUEST_NEW_LIST, this) { _, bundle ->
            val key = bundle.getString(ListNameDialog.RESULT_LIST_KEY) ?: return@setFragmentResultListener
            ListModel(key).add(tagId)
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.list_added_to, TagLists.name(key)),
                    Toast.LENGTH_SHORT,
                ).show()
            render()
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.list_add_to_list)
            .setView(scroll)
            .setPositiveButton(R.string.list_picker_done, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    /**
     * What the rows should say right now.
     *
     * Every read a row depends on happens here, inside the one tracked function: the registry
     * version (a create, rename, delete or reorder moves only that), the ordered keys, and each
     * list's ids, which carry both the count and whether this tag is in it.
     */
    private fun currentRows(context: Context): List<Row> {
        trackedLists.clear()
        TagLists.version
        return TagLists.allKeys().map { key ->
            val model = ListModel(key)
            trackedLists.add(model)
            Row(key, TagLists.displayName(context, key), listCountText(context, key), model.contains(tagId))
        }
    }

    private fun render() {
        val column = rows ?: return
        val activity = activity ?: return
        val snapshot =
            if (tracking) {
                currentRows(activity)
            } else {
                Trackable.track(tracker, Function<List<Row>> { currentRows(activity) }).also { tracking = true }
            }
        column.removeAllViews()
        val inflater = layoutInflater
        for ((key, name, count, checked) in snapshot) {
            val row = inflater.inflate(R.layout.list_row, column, false)
            row.findViewById<AppCompatImageView>(R.id.listRowIcon).setImageResource(listIconRes(key))
            row.findViewById<TextView>(R.id.listRowName).text = name
            row.findViewById<TextView>(R.id.listRowDetail).text = count
            row.findViewById<View>(R.id.listRowCheck).visibility = if (checked) View.VISIBLE else View.GONE
            // The name and the count are what the row is; being in the list is a state, and a
            // checked one at that, so a screen reader can announce the change on its own.
            row.contentDescription = getString(R.string.list_row_label, name, count)
            ViewCompat.setStateDescription(
                row,
                getString(if (checked) R.string.list_state_in else R.string.list_state_not_in),
            )
            ViewCompat.setAccessibilityDelegate(
                row,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat,
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.isCheckable = true
                        info.isChecked = checked
                    }
                },
            )
            row.setOnClickListener {
                // The list may have been deleted since this row was drawn; adding to it here would
                // write `lists.<key>` back with no metadata and resurrect it under its raw slug.
                if (key !in TagLists.allKeys()) {
                    render()
                    return@setOnClickListener
                }
                val model = ListModel(key)
                if (model.contains(tagId)) model.remove(tagId) else model.add(tagId)
                render()
            }
            column.addView(row)
        }

        val newRow = inflater.inflate(R.layout.list_row, column, false)
        newRow.findViewById<AppCompatImageView>(R.id.listRowIcon).setImageResource(R.drawable.ic_add)
        newRow.findViewById<TextView>(R.id.listRowName).setText(R.string.list_new_row)
        newRow.findViewById<TextView>(R.id.listRowDetail).visibility = View.GONE
        newRow.setOnClickListener { ListNameDialog.create(parentFragmentManager, REQUEST_NEW_LIST) }
        column.addView(newRow)
    }

    override fun onDismiss(dialog: DialogInterface) {
        stopTracking()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        rows = null
        stopTracking()
        super.onDestroyView()
    }

    /**
     * Bindroid registrations are one-shot, so nothing has to be unsubscribed; what matters is that
     * the last one is never renewed and never touches a dialog that has gone away.
     */
    private fun stopTracking() {
        gone = true
        tracking = false
        mainHandler.removeCallbacksAndMessages(null)
        trackedLists.clear()
    }

    companion object {
        private const val ARG_TAG_ID = "depollsoft.tagmaster.listPicker.tagId"
        private const val FRAGMENT_TAG = "depollsoft.tagmaster.listPickerDialog"
        private const val REQUEST_NEW_LIST = "depollsoft.tagmaster.listPicker.newList"

        fun show(
            manager: FragmentManager,
            tagId: Int,
        ) {
            if (manager.isStateSaved || manager.findFragmentByTag(FRAGMENT_TAG) != null) return
            ListPickerDialog()
                .apply { arguments = Bundle().apply { putInt(ARG_TAG_ID, tagId) } }
                .show(manager, FRAGMENT_TAG)
        }
    }
}
