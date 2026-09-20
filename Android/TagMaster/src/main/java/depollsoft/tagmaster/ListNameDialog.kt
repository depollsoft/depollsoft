package depollsoft.tagmaster

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Names a list: New list when [ARG_KEY] is absent, Rename list when it names an existing one.
 *
 * A DialogFragment so the dialog and its half-typed name come back after a rotation, and so the
 * created or renamed key reaches whoever asked for it — Home, the list screen, or the add-to-list
 * picker — through a fragment result rather than a callback that recreation would drop.
 * Validation is [TagLists.validateName]'s; a rejected name keeps the dialog open with the reason
 * under the field.
 */
class ListNameDialog : DialogFragment() {
    private val listKey: String?
        get() = arguments?.getString(ARG_KEY)

    private val request: String
        get() = arguments?.getString(ARG_REQUEST) ?: REQUEST_DEFAULT

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val key = listKey
        val renaming = key != null
        val content = layoutInflater.inflate(R.layout.dialog_list_name, null)
        val field = content.findViewById<TextInputLayout>(R.id.listNameLayout)
        val input = content.findViewById<TextInputEditText>(R.id.listNameInput)
        if (renaming) input.setText(TagLists.name(key!!))
        input.setSelection(input.text?.length ?: 0)

        // Material's 80dp vertical insets leave too little room for a wrapped error above the
        // keyboard on small screens, exactly as the Open Tag dialog found. Keep a 24dp margin.
        val verticalInset = (24 * resources.displayMetrics.density).toInt()
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setBackgroundInsetTop(verticalInset)
                .setBackgroundInsetBottom(verticalInset)
                .setTitle(if (renaming) R.string.list_rename_title else R.string.list_new_title)
                .setView(content)
                .setPositiveButton(if (renaming) R.string.list_rename else R.string.list_create, null)
                .setNegativeButton(R.string.home_cancel, null)
                .create()

        fun submit(): Boolean {
            val name = input.text?.toString().orEmpty()
            // A list deleted on another device while this dialog was open has nothing to rename.
            if (renaming && !TagLists.customKeys.contains(key)) {
                dismissAllowingStateLoss()
                return true
            }
            val error = TagLists.validateName(name, excludingKey = key)
            if (error != null) {
                field.error = getString(messageFor(error))
                return false
            }
            field.error = null
            val result =
                if (renaming) {
                    TagLists.rename(key!!, name)
                    key
                } else {
                    TagLists.create(name)
                }
            parentFragmentManager.setFragmentResult(
                request,
                Bundle().apply {
                    putString(RESULT_LIST_KEY, result)
                    putBoolean(RESULT_CREATED, !renaming)
                },
            )
            dismissAllowingStateLoss()
            return true
        }

        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
            ) {
                submit()
            } else {
                false
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
            input.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        return dialog
    }

    companion object {
        const val REQUEST_DEFAULT = "depollsoft.tagmaster.listName"
        const val RESULT_LIST_KEY = "listKey"
        const val RESULT_CREATED = "created"
        private const val ARG_KEY = "depollsoft.tagmaster.listName.key"
        private const val ARG_REQUEST = "depollsoft.tagmaster.listName.request"
        private const val FRAGMENT_TAG = "depollsoft.tagmaster.listNameDialog"

        /** The reason a name was rejected, in the words the field shows under it. */
        fun messageFor(error: TagLists.NameError): Int =
            when (error) {
                TagLists.NameError.EMPTY -> R.string.list_name_error_empty
                TagLists.NameError.TOO_LONG -> R.string.list_name_error_too_long
                TagLists.NameError.DUPLICATE -> R.string.list_name_error_duplicate
                TagLists.NameError.RESERVED -> R.string.list_name_error_reserved
            }

        private fun show(
            manager: FragmentManager,
            arguments: Bundle,
        ) {
            if (manager.isStateSaved || manager.findFragmentByTag(FRAGMENT_TAG) != null) return
            ListNameDialog().apply { setArguments(arguments) }.show(manager, FRAGMENT_TAG)
        }

        /** Asks for a new list's name; the result carries the created key. */
        fun create(
            manager: FragmentManager,
            request: String = REQUEST_DEFAULT,
        ) = show(manager, Bundle().apply { putString(ARG_REQUEST, request) })

        /** Asks for a new name for [key], prefilled with the current one. */
        fun rename(
            manager: FragmentManager,
            key: String,
            request: String = REQUEST_DEFAULT,
        ) = show(
            manager,
            Bundle().apply {
                putString(ARG_KEY, key)
                putString(ARG_REQUEST, request)
            },
        )
    }
}
