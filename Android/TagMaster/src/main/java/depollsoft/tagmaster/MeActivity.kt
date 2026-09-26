package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import depollsoft.lib.ui.ChangelogViewer
import depollsoft.tagmaster.ui.HomeActions
import depollsoft.tagmaster.ui.HomeScreen
import depollsoft.tagmaster.ui.SavedListEditor
import depollsoft.tagmaster.ui.setTagMasterContent

/**
 * Home: browsing, a random tag and opening a tag by id; the Teachable Tags list and the user's own
 * lists; and the favorites. On a wide window a chosen tag opens beside the list.
 */
class MeActivity :
    TagPaneActivity() {
    val favoriteIds: List<Int>
        get() = FavoritesModel.favoriteIds

    internal lateinit var home: HomeActions
        private set

    /** Edit mode for the favorites and, sharing its Edit action, the user's own lists. */
    lateinit var listEditor: SavedListEditor
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { FavoritesModel.favoriteIds.toList() },
            )
        home = HomeActions(this, tagPane)
        listEditor =
            SavedListEditor(
                initiallyEditing = savedInstanceState?.getBoolean(STATE_EDITING) == true,
                model = { ListModel(TagLists.FAVORITE) },
                companion = { TagLists.customKeys.isNotEmpty() },
            )
        setTagMasterContent { HomeScreen(tagPane, home, listEditor) }
        tagPane.restore(savedInstanceState)

        if (PrivacyChoices(this).hasChosen) {
            val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
            viewer.setTitle(getString(R.string.home_changelog_title))
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.showIfAppropriate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EDITING, listEditor.isEditing)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        TelemetryConsent.showIfNeeded(this)
    }

    override fun onDestroy() {
        home.release()
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
