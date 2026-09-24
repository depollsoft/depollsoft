package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringResource
import depollsoft.tagmaster.ui.EmptyState
import depollsoft.tagmaster.ui.SavedListEditor
import depollsoft.tagmaster.ui.SavedListScreen
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.setTagMasterContent

/** The tags the user can teach, in their own order. */
class TeachableTagsActivity :
    AppCompatActivity(),
    TagPaneHost {
    val teachableTags: List<Int>
        get() = TeachableTagsModel.teachableTagIds

    internal lateinit var tagPane: TagPaneState
        private set

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

    override val hasDetailPane: Boolean
        get() = tagPane.hasDetailPane

    override var selectedTagId: Int?
        get() = tagPane.selectedTagId
        set(value) {
            tagPane.selectedTagId = value
        }

    override fun showTag(id: Int) = tagPane.showTag(id)

    override fun listedTagIds(): List<Int> = tagPane.listedTagIds()

    override fun revealTag(id: Int) = tagPane.revealTag(id)

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = tagPane.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EDITING, listEditor.isEditing)
        tagPane.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        tagPane.stop()
        super.onDestroy()
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, TagSearchActivity::class.java))
        return true
    }

    private companion object {
        const val STATE_EDITING = "savedListEditing"
    }
}
