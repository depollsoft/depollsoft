package depollsoft.pitchperfect

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.PlateAlertDialog
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateOutlinedField
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * Names a set list: "New set list" when [listId] is null, "Rename set list" when it names one.
 *
 * Validation is [SongsModel.validateName]'s; a rejected name keeps the dialog open with the
 * reason under the field. The half-typed name survives a rotation. [onDone] gets the created or
 * renamed id.
 */
@Composable
fun SetListNameDialog(
    listId: String?,
    onDismiss: () -> Unit,
    onDone: (listId: String, created: Boolean) -> Unit,
    model: SongsModel = SongsModel.get(),
) {
    val context = LocalContext.current
    val renaming = listId != null
    val initial = remember(listId) { listId?.let { model.displayName(it) }.orEmpty() }
    var value by rememberSaveable(listId, stateSaver = TextFieldValue.Saver) {
        androidx.compose.runtime.mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var error by rememberSaveable(listId) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    fun submit() {
        // A list deleted on another device while this prompt was open has nothing to rename.
        if (listId != null && !model.songLists.containsKey(listId)) {
            onDismiss()
            return
        }
        val rejection = model.validateName(value.text, excludingId = listId)
        if (rejection != null) {
            error = context.getString(messageFor(rejection))
            return
        }
        val result =
            if (listId != null) {
                model.renameList(listId, value.text)
                listId
            } else {
                model.createList(value.text)
            }
        onDone(result, !renaming)
    }

    PlateAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(if (renaming) R.string.SetListRenameTitle else R.string.SetListNewTitle),
        verticalInset = 24.dp,
        buttons =
            listOf(
                DialogButton(stringResource(R.string.Cancel), onDismiss),
                DialogButton(
                    stringResource(if (renaming) R.string.SetListRename else R.string.SetListCreate),
                    ::submit,
                    testTag = TestTags.NAME_DIALOG_CONFIRM,
                ),
            ),
    ) {
        PlateOutlinedField(
            value = value,
            onValueChange = { value = it },
            label = stringResource(R.string.SetListNameLabel),
            textStyle = plateText(20.sp, plateColors.ink, PlateFonts.condensed),
            helper = if (renaming || error != null) null else stringResource(R.string.SetListNameHint),
            error = error,
            onDone = ::submit,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp).fillMaxWidth(),
            fieldModifier = Modifier.focusRequester(focus).testTag(TestTags.NAME_DIALOG_FIELD),
        )
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** The reason a name was rejected, in the words the field shows under it. */
fun messageFor(error: SongsModel.NameError): Int =
    when (error) {
        SongsModel.NameError.EMPTY -> R.string.SetListNameErrorEmpty
        SongsModel.NameError.TOO_LONG -> R.string.SetListNameErrorTooLong
        SongsModel.NameError.RESERVED -> R.string.SetListNameErrorReserved
        SongsModel.NameError.DUPLICATE -> R.string.SetListNameErrorDuplicate
    }
