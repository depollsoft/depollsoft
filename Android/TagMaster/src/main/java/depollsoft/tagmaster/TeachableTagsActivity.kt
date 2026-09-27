package depollsoft.tagmaster

import android.os.Bundle
import androidx.compose.ui.res.stringResource
import depollsoft.tagmaster.ui.EmptyState
import depollsoft.tagmaster.ui.SavedListEditor
import depollsoft.tagmaster.ui.SavedListScreen
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.setTagMasterContent

/** The tags the user can teach, in their own order. */
class TeachableTagsActivity : SavedListActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { TeachableTagsModel.teachableTagIds.toList() },
            )
        listEditor = SavedListEditor(wasEditing(savedInstanceState), { ListModel(TagLists.TEACHABLE) })
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
}
