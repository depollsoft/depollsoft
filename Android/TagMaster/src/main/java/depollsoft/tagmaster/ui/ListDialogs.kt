package depollsoft.tagmaster.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.R
import depollsoft.tagmaster.TagLists

/** The icon that stands for a list everywhere it is named: Home, the chips, and the picker. */
internal fun listIconRes(key: String): Int =
    when (key) {
        TagLists.FAVORITE -> R.drawable.ic_favorite
        TagLists.TEACHABLE -> R.drawable.ic_people
        else -> R.drawable.ic_list
    }

/** The trailing count a list row shows: "1 tag", "12 tags". */
@Composable
internal fun listCountText(key: String): String {
    val count = ListModel(key).ids.size
    return pluralStringResource(R.plurals.list_tag_count, count, count)
}

/** The reason a name was rejected, in the words the field shows under it. */
internal fun nameErrorMessage(error: TagLists.NameError): Int =
    when (error) {
        TagLists.NameError.EMPTY -> R.string.list_name_error_empty
        TagLists.NameError.TOO_LONG -> R.string.list_name_error_too_long
        TagLists.NameError.DUPLICATE -> R.string.list_name_error_duplicate
        TagLists.NameError.RESERVED -> R.string.list_name_error_reserved
    }

/**
 * Which list dialog a screen is showing, kept across recreation so a half-typed name or an open
 * picker comes back after a rotation.
 */
@Stable
class ListDialogs {
    /** New list when [NameRequest.key] is null, Rename list otherwise. */
    data class NameRequest(
        val key: String?,
        /** A tag to put in the list once it is created (the picker's New list… row). */
        val addTagId: Int? = null,
    )

    var naming: NameRequest? by mutableStateOf(null)
    var deleting: String? by mutableStateOf(null)
    var picking: Int? by mutableStateOf(null)

    fun newList(addTagId: Int? = null) {
        if (naming == null) naming = NameRequest(null, addTagId)
    }

    fun rename(key: String) {
        if (naming == null) naming = NameRequest(key)
    }

    fun delete(key: String) {
        if (deleting == null) deleting = key
    }

    fun pick(tagId: Int) {
        if (picking == null) picking = tagId
    }

    companion object {
        val saver: Saver<ListDialogs, ArrayList<Any?>> =
            Saver(
                save = { arrayListOf(it.naming?.key, it.naming != null, it.naming?.addTagId, it.deleting, it.picking) },
                restore = { values ->
                    ListDialogs().apply {
                        if (values[1] == true) naming = NameRequest(values[0] as String?, values[2] as Int?)
                        deleting = values[3] as String?
                        picking = values[4] as Int?
                    }
                },
            )
    }
}

@Composable
fun rememberListDialogs(): ListDialogs = rememberSaveable(saver = ListDialogs.saver) { ListDialogs() }

/** Shows whichever list dialog [dialogs] names. */
@Composable
fun ListDialogsHost(
    dialogs: ListDialogs,
    onRenamed: (String) -> Unit = {},
) {
    val context = LocalContext.current
    dialogs.picking?.let { tagId ->
        ListPickerDialog(tagId, onNewList = { dialogs.newList(addTagId = tagId) }, onDone = { dialogs.picking = null })
    }
    dialogs.naming?.let { request ->
        ListNameDialog(
            request.key,
            onDismiss = { dialogs.naming = null },
            onNamed = { key ->
                dialogs.naming = null
                if (request.key != null) onRenamed(key)
                request.addTagId?.let { tagId ->
                    ListModel(key).add(tagId)
                    Toast.makeText(context, context.getString(R.string.list_added_to, TagLists.name(key)), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
    dialogs.deleting?.let { key -> ListDeleteDialog(key) { dialogs.deleting = null } }
}

/**
 * Names a list: New list when [key] is null, Rename list for an existing one. A rejected name
 * keeps the dialog open with the reason under the field.
 */
@Composable
fun ListNameDialog(
    key: String?,
    onDismiss: () -> Unit,
    onNamed: (String) -> Unit,
) {
    val renaming = key != null
    val initial = remember(key) { key?.let(TagLists::name).orEmpty() }
    var field by rememberSaveable(key, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var error by rememberSaveable(key) { mutableStateOf<Int?>(null) }
    // A list deleted on another device while this dialog was open has nothing to rename.
    if (renaming && !TagLists.customKeys.contains(key)) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    fun submit() {
        val name = field.text
        if (renaming && !TagLists.customKeys.contains(key)) {
            onDismiss()
            return
        }
        val rejection = TagLists.validateName(name, excludingKey = key)
        if (rejection != null) {
            error = nameErrorMessage(rejection)
            return
        }
        error = null
        val result =
            if (renaming) {
                TagLists.rename(key!!, name)
                key
            } else {
                TagLists.create(name)
            }
        onNamed(result)
    }

    val focus = remember { FocusRequester() }
    TagMasterDialog(
        onDismissRequest = onDismiss,
        title = stringResource(if (renaming) R.string.list_rename_title else R.string.list_new_title),
        dismiss = DialogButton(stringResource(R.string.home_cancel), onClick = onDismiss),
        confirm = DialogButton(stringResource(if (renaming) R.string.list_rename else R.string.list_create), id = "listNameConfirm") { submit() },
    ) {
        OutlinedField(
            label = stringResource(R.string.list_name_hint),
            value = field,
            onValueChange = { field = it },
            modifier =
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .fillMaxWidth(),
            helper = stringResource(R.string.list_name_helper),
            error = error?.let { stringResource(it) },
            counter = "${field.text.length}/${TagLists.MAX_NAME_LENGTH}",
            maxLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            fieldModifier =
                Modifier
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        // The field wraps a long name onto a second line, but Enter still means
                        // "that's the name" and never inserts a newline.
                        if (event.key == Key.Enter && !event.isShiftPressed) {
                            if (event.type == KeyEventType.KeyDown) submit()
                            true
                        } else {
                            false
                        }
                    }.testTag("listNameInput"),
        )
        // Requested from inside the dialog, whose content is composed after the caller's.
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

/**
 * "Delete “Afterglow set”?" — with the number of tags that go with it, so nobody deletes a
 * forty-tag set thinking it was the empty one.
 */
@Composable
fun ListDeleteDialog(
    key: String,
    onDismiss: () -> Unit,
) {
    val count = ListModel(key).ids.size
    val name = TagLists.name(key)
    val message =
        if (count == 0) {
            stringResource(R.string.list_delete_message_empty, name)
        } else {
            pluralStringResource(R.plurals.list_delete_message, count, count)
        }
    TagMasterDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.list_delete_title, name),
        message = message,
        dismiss = DialogButton(stringResource(R.string.home_cancel), onClick = onDismiss),
        confirm =
            DialogButton(stringResource(R.string.list_delete), id = "listDeleteConfirm", destructive = true) {
                onDismiss()
                if (TagLists.isCustom(key)) TagLists.delete(key)
            },
    )
}

/**
 * "Add to list": every list the tag could be in, checked where it is, and New list… at the end.
 * Rows toggle immediately, so the chips behind the dialog follow along and Done only means
 * "finished". Every row re-reads the lists, which other devices can change while it is open.
 */
@Composable
fun ListPickerDialog(
    tagId: Int,
    onNewList: () -> Unit,
    onDone: () -> Unit,
) {
    TagMasterDialog(
        onDismissRequest = onDone,
        title = stringResource(R.string.list_add_to_list),
        confirm = DialogButton(stringResource(R.string.list_picker_done), id = "listPickerDone", onClick = onDone),
        wrapWidth = true,
    ) {
        TagLists.version
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            for (key in TagLists.allKeys()) {
                val model = ListModel(key)
                val checked = model.contains(tagId)
                val name = TagLists.displayName(LocalContext.current, key)
                val count = listCountText(key)
                val label = stringResource(R.string.list_row_label, name, count)
                val state = stringResource(if (checked) R.string.list_state_in else R.string.list_state_not_in)
                ListRow(
                    key = key,
                    name = name,
                    detail = count,
                    checked = checked,
                    modifier =
                        Modifier
                            .clickable(role = Role.Checkbox) {
                                // The list may have been deleted since this row was drawn; adding
                                // to it here would resurrect it under its raw slug.
                                if (key !in TagLists.allKeys()) return@clickable
                                if (model.contains(tagId)) model.remove(tagId) else model.add(tagId)
                            }.semantics(mergeDescendants = true) {
                                contentDescription = label
                                stateDescription = state
                                toggleableState = ToggleableState(checked)
                            }.testTag("pickerRow:$key"),
                )
            }
            ListRow(
                key = null,
                name = stringResource(R.string.list_new_row),
                detail = null,
                icon = R.drawable.ic_add,
                modifier = Modifier.clickable(role = Role.Button, onClick = onNewList).testTag("pickerNewList"),
            )
        }
    }
}
