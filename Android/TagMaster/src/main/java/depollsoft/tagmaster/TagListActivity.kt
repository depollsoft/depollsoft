package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.BarAction
import depollsoft.tagmaster.ui.EmptyState
import depollsoft.tagmaster.ui.SavedListEditor
import depollsoft.tagmaster.ui.SavedListScreen
import depollsoft.tagmaster.ui.ShowAs
import depollsoft.tagmaster.ui.TagMasterButton
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.rememberListDialogs
import depollsoft.tagmaster.ui.setTagMasterContent

/**
 * One of the user's own lists: the same screen as [TeachableTagsActivity], plus the rename and
 * delete actions only a user-defined list has.
 *
 * The list's name is the title, so a rename anywhere (including from another device) retitles the
 * screen; a delete anywhere closes it rather than leaving a screen onto nothing.
 */
class TagListActivity :
    TagPaneActivity() {
    val listKey: String
        get() = intent?.getStringExtra(EXTRA_LIST_KEY).orEmpty()

    /** The name this screen is currently titled with. */
    val listName: String
        get() = TagLists.name(listKey)

    lateinit var listEditor: SavedListEditor
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = listKey
        if (key.isEmpty() || !TagLists.isCustom(key) || !TagLists.customKeys.contains(key)) {
            // A stale shortcut or a list deleted before this screen opened: there is nothing to show.
            finish()
            return
        }
        val model = ListModel(key)
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { model.ids.toList() },
            )
        listEditor = SavedListEditor(savedInstanceState?.getBoolean(STATE_EDITING) == true, { model })
        setTagMasterContent {
            val dialogs = rememberListDialogs()
            val exists = TagLists.customKeys.contains(key)
            LaunchedEffect(exists) { if (!exists) finish() }
            val name = TagLists.name(key)
            SavedListScreen(
                model = model,
                editor = listEditor,
                pane = tagPane,
                title = name,
                brandTitle = false,
                listLabel = name,
                listTag = "tagList",
                onNavigateUp = { navigateUpOrHome() },
                onOpenTag = tagPane::showTag,
                dialogs = dialogs,
                extraActions =
                    listOf(
                        BarAction("renameList", stringResource(R.string.list_rename_action), showAs = ShowAs.Never) { dialogs.rename(key) },
                        BarAction("deleteList", stringResource(R.string.list_delete_action), showAs = ShowAs.Never) { dialogs.delete(key) },
                    ),
            ) {
                EmptyState(stringResource(R.string.list_empty_title, name), stringResource(R.string.list_empty_hint)) {
                    TagMasterButton(
                        stringResource(R.string.list_empty_browse),
                        onClick = { startActivity(Intent(this@TagListActivity, TagBrowserActivity::class.java)) },
                        modifier = Modifier.padding(top = 16.dp).testTag("tagListBrowseButton"),
                        icon = R.drawable.ic_library_music,
                    )
                }
            }
        }
        tagPane.restore(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::listEditor.isInitialized) outState.putBoolean(STATE_EDITING, listEditor.isEditing)
        super.onSaveInstanceState(outState)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, TagSearchActivity::class.java))
        return true
    }

    companion object {
        const val EXTRA_LIST_KEY = "depollsoft.tagmaster.listKey"
        private const val STATE_EDITING = "savedListEditing"

        fun intent(
            context: Context,
            key: String,
        ): Intent = Intent(context, TagListActivity::class.java).putExtra(EXTRA_LIST_KEY, key)
    }
}
