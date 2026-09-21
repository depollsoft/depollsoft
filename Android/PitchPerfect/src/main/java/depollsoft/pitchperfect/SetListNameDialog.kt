package depollsoft.pitchperfect

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
 * Names a set list: New set list when [ARG_ID] is absent, Rename set list when it names one.
 *
 * A DialogFragment so the prompt and its half-typed name survive a rotation, and so the created or
 * renamed id reaches whoever asked — the Songs tab or the manage screen — through a fragment
 * result rather than a callback that recreation would drop. Validation is
 * [SongsModel.validateName]'s; a rejected name keeps the dialog open with the reason under the
 * field.
 */
class SetListNameDialog : DialogFragment() {
    private val listId: String?
        get() = arguments?.getString(ARG_ID)

    private val request: String
        get() = arguments?.getString(ARG_REQUEST) ?: REQUEST_DEFAULT

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val model = SongsModel.get()
        val id = listId
        val renaming = id != null
        val content = layoutInflater.inflate(R.layout.dialog_set_list_name, null)
        val field = content.findViewById<TextInputLayout>(R.id.setListNameLayout)
        val input = content.findViewById<TextInputEditText>(R.id.setListNameInput)
        if (renaming) {
            input.setText(model.displayName(id!!))
            field.hint = getString(R.string.SetListNameLabel)
        } else {
            field.helperText = getString(R.string.SetListNameHint)
        }
        input.setSelection(input.text?.length ?: 0)

        // Material's 80dp vertical insets leave too little room for a wrapped error above the
        // keyboard on a small screen; keep a 24dp margin instead.
        val verticalInset = (24 * resources.displayMetrics.density).toInt()
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setBackgroundInsetTop(verticalInset)
                .setBackgroundInsetBottom(verticalInset)
                .setTitle(if (renaming) R.string.SetListRenameTitle else R.string.SetListNewTitle)
                .setView(content)
                .setPositiveButton(if (renaming) R.string.SetListRename else R.string.SetListCreate, null)
                .setNegativeButton(R.string.Cancel, null)
                .create()

        fun submit(): Boolean {
            val name = input.text?.toString().orEmpty()
            // A list deleted on another device while this prompt was open has nothing to rename.
            if (renaming && !model.songLists.containsKey(id)) {
                dismissAllowingStateLoss()
                return true
            }
            val error = model.validateName(name, excludingId = id)
            if (error != null) {
                field.helperText = null
                field.error = getString(messageFor(error))
                return false
            }
            field.error = null
            val result =
                if (renaming) {
                    model.renameList(id!!, name)
                    id
                } else {
                    model.createList(name)
                }
            parentFragmentManager.setFragmentResult(
                request,
                Bundle().apply {
                    putString(RESULT_LIST_ID, result)
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
        const val REQUEST_DEFAULT = "depollsoft.pitchperfect.setListName"
        const val RESULT_LIST_ID = "listId"
        const val RESULT_CREATED = "created"
        const val FRAGMENT_TAG = "depollsoft.pitchperfect.setListNameDialog"
        private const val ARG_ID = "depollsoft.pitchperfect.setListName.id"
        private const val ARG_REQUEST = "depollsoft.pitchperfect.setListName.request"

        /** The reason a name was rejected, in the words the field shows under it. */
        fun messageFor(error: SongsModel.NameError): Int =
            when (error) {
                SongsModel.NameError.EMPTY -> R.string.SetListNameErrorEmpty
                SongsModel.NameError.TOO_LONG -> R.string.SetListNameErrorTooLong
                SongsModel.NameError.RESERVED -> R.string.SetListNameErrorReserved
                SongsModel.NameError.DUPLICATE -> R.string.SetListNameErrorDuplicate
            }

        private fun show(
            manager: FragmentManager,
            arguments: Bundle,
        ) {
            if (manager.isStateSaved || manager.findFragmentByTag(FRAGMENT_TAG) != null) return
            SetListNameDialog().apply { setArguments(arguments) }.show(manager, FRAGMENT_TAG)
        }

        /** Asks for a new set list's name; the result carries the created id. */
        fun create(
            manager: FragmentManager,
            request: String = REQUEST_DEFAULT,
        ) = show(manager, Bundle().apply { putString(ARG_REQUEST, request) })

        /** Asks for a new name for [id], prefilled with its display name. */
        fun rename(
            manager: FragmentManager,
            id: String,
            request: String = REQUEST_DEFAULT,
        ) = show(
            manager,
            Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_REQUEST, request)
            },
        )
    }
}
