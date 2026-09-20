package depollsoft.tagmaster

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
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
 */
class ListPickerDialog : DialogFragment() {
    private val tagId: Int
        get() = requireArguments().getInt(ARG_TAG_ID)

    private var rows: LinearLayout? = null

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

    private fun render() {
        val column = rows ?: return
        val activity = activity ?: return
        column.removeAllViews()
        val inflater = layoutInflater
        for (key in TagLists.allKeys()) {
            val row = inflater.inflate(R.layout.list_row, column, false)
            val name = TagLists.displayName(activity, key)
            row.findViewById<AppCompatImageView>(R.id.listRowIcon).setImageResource(listIconRes(key))
            row.findViewById<TextView>(R.id.listRowName).text = name
            row.findViewById<TextView>(R.id.listRowDetail).text = listCountText(activity, key)
            val checked = ListModel(key).contains(tagId)
            row.findViewById<View>(R.id.listRowCheck).visibility = if (checked) View.VISIBLE else View.GONE
            row.contentDescription =
                getString(if (checked) R.string.list_in_list else R.string.list_not_in_list, name)
            row.setOnClickListener {
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

    override fun onDestroyView() {
        rows = null
        super.onDestroyView()
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
