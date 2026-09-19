package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.ui.ChangelogViewer

class MeActivity :
    AppCompatActivity(),
    TagPaneHost {
    val favoriteIds: TrackableCollection<Int>
        get() = FavoritesModel.favoriteIds

    internal lateinit var tagPane: TagPaneController
        private set

    /** Recycled favorites rows, keyed by tag id; exposed for tests. */
    lateinit var favoritesAdapter: SavedTagListAdapter
        private set

    lateinit var listEditor: SavedListEditor
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.meview)
        setUpToolbar(showUp = false)
        findViewById<View>(R.id.searchButton).applyBottomInsetsAsMargin()

        favoritesAdapter = SavedTagListAdapter({ FavoritesModel.favoriteIds }) { FavoriteTagItemView(it) }
        val header = layoutInflater.inflate(R.layout.meviewheader_item, null)
        val list = findViewById<RecyclerView>(R.id.homeList)
        list.applyContentInsets()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter =
            ConcatAdapter(
                StaticViewAdapter(view = header),
                favoritesAdapter,
                StaticViewAdapter(R.layout.meviewfooter),
            )
        list.addItemDecoration(SavedTagListAdapter.RowDivider(this))
        listEditor = SavedListEditor(this, ListModel("favorite"), favoritesAdapter, list, R.string.Favorites, savedInstanceState)

        this.supportActionBar?.title = getString(R.string.home_title).makeTitleString(this)

        if (depollsoft.lib.privacy.PrivacyChoices(this).hasChosen) {
            val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
            viewer.setTitle(getString(R.string.home_changelog_title))
            viewer.setIcon(R.mipmap.ic_launcher)
            viewer.showIfAppropriate()
        }

        findViewById<View>(R.id.searchButton).setOnClickListener {
            val i = Intent(this, TagSearchActivity::class.java)
            startActivity(i)
        }

        tagPane =
            TagPaneController(
                activity = this,
                listedIds = { FavoritesModel.favoriteIds.toList() },
                // The header occupies the first row of the ConcatAdapter.
                reveal = { id ->
                    val index = favoritesAdapter.currentList.indexOf(id)
                    if (index >= 0) list.smoothScrollToPosition(index + 1)
                },
            )
        tagPane.onCreate(savedInstanceState)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.memenu, menu)
        menuInflater.inflate(R.menu.savedlistmenu, menu)

        MenuItems
            .setShowAsAction(menu.findItem(R.id.settingsMenuItem), MenuItems.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (listEditor.selectMenu(item)) return true
        if (item.itemId == R.id.settingsMenuItem) {
            val i = Intent(this, SettingsActivity::class.java)
            this.startActivity(i)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        listEditor.prepareMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(event)
        if (::listEditor.isInitialized) listEditor.afterTouchEvent(event)
        return handled
    }

    override fun onSaveInstanceState(outState: Bundle) {
        listEditor.saveState(outState)
        tagPane.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        depollsoft.lib.privacy.TelemetryConsent.showIfNeeded(this)
        listEditor.resume()
    }

    override fun onPause() {
        listEditor.pause()
        super.onPause()
    }

    override fun onDestroy() {
        listEditor.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = tagPane.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
