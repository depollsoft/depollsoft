package depollsoft.pitchperfect

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import depollsoft.pitchperfect.ui.PlateActionIcon
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.PlateTopBar

/**
 * Adds a song to a set list, or edits one ([ID_EXTRA]). Every song operation targets the list the
 * Songs tab is showing ([LIST_EXTRA]), not the default one.
 */
class AddSongActivity : AppCompatActivity() {
    internal lateinit var editor: SongEditorState
        private set

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = SongsModel.get().listOrCurrent(intent.getStringExtra(LIST_EXTRA))
        editor = SongEditorState(list, intent.getStringExtra(ID_EXTRA))
        // The title typed and the key chosen so far survive rotation, as the EditText's did.
        keepAcrossRecreation("depollsoft.pitchperfect.SongEditor", { android.os.Bundle().also(editor::save) }, editor::restore)
        setTitle(if (editor.editing) R.string.EditSong else R.string.AddSong)

        setContent {
            PlateTheme {
                val titleFocus = remember { FocusRequester() }
                Column(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    PlateTopBar(stringResource(if (editor.editing) R.string.EditSong else R.string.AddSong)) {
                        PlateActionIcon(R.drawable.ic_delete, stringResource(R.string.RemoveSong), ::remove, Modifier.testTag(TestTags.REMOVE_SONG))
                        PlateActionIcon(R.drawable.ic_undo, stringResource(R.string.Cancel), ::cancel, Modifier.testTag(TestTags.CANCEL_SONG))
                        PlateActionIcon(
                            R.drawable.ic_check,
                            stringResource(R.string.Ok),
                            {
                                if (editor.save()) done(RESULT_SAVED) else titleFocus.requestFocus()
                            },
                            Modifier.testTag(TestTags.OK_SONG),
                        )
                    }
                    AddSongScreen(editor, onSaved = { done(RESULT_SAVED) }, titleFocus = titleFocus)
                }
            }
        }
    }

    private fun remove() {
        editor.remove()
        done(RESULT_SAVED)
    }

    private fun cancel() = done(Activity.RESULT_CANCELED)

    private fun done(result: Int) {
        setResult(result)
        PitchPerfectActivity.handlingResult = true
        finish()
    }

    companion object {
        const val ID_EXTRA = "depollsoft.pitchperfect.AddSong.id"
        const val LIST_EXTRA = "depollsoft.pitchperfect.AddSong.listId"
        private const val RESULT_SAVED = 1
    }
}
