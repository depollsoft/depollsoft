package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.res.stringResource
import depollsoft.tagmaster.ui.EmptyState
import depollsoft.tagmaster.ui.SavedListEditor
import depollsoft.tagmaster.ui.SavedListScreen
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.setTagMasterContent

/** The tags the user can teach, in their own order. */
class TeachableTagsActivity :
    TagPaneActivity() {
    val teachableTags: List<Int>
        get() = TeachableTagsModel.teachableTagIds

    lateinit var listEditor: SavedListEditor
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { TeachableTagsModel.teachableTagIds.toList() },
            )
        listEditor = SavedListEditor(savedInstanceState?.getBoolean(STATE_EDITING) == true, { ListModel(TagLists.TEACHABLE) })
        setTagMasterContent {
            SavedListScreen(
                model = ListModel(TagLists.TEACHABLE),
                editor = listEditor,
                pane = tagPane,
                title = stringResource(R.string.home_title),
                brandTitle = true,
                listLabel = stringResource(R.string.TeachableTags),
                listTag = "teachableList",
                onNavigateUp = { navigateUpOrHome() },
                onOpenTag = tagPane::showTag,
            ) {
                EmptyState(stringResource(R.string.NoTeachableTags), stringResource(R.string.home_teachable_hint))
            }
        }
        tagPane.restore(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EDITING, listEditor.isEditing)
        super.onSaveInstanceState(outState)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, TagSearchActivity::class.java))
        return true
    }

    private companion object {
        const val STATE_EDITING = "savedListEditing"
    }
}
