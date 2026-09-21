package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.utils.bindTo
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

    /** The user's own lists, between the header and the New list… row; exposed for tests. */
    lateinit var listsAdapter: ListRowsAdapter
        private set

    lateinit var listEditor: SavedListEditor
        private set

    /**
     * Where the favorites rows start in the home list: the header, the user's lists, then the row
     * that closes the Lists group.
     */
    val favoritesStartPosition: Int
        get() = 2 + listsAdapter.itemCount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.meview)
        setUpToolbar(showUp = false)
        findViewById<View>(R.id.searchButton).applyBottomInsetsAsMargin()

        favoritesAdapter = SavedTagListAdapter({ FavoritesModel.favoriteIds }) { FavoriteTagItemView(it) }
        val header = layoutInflater.inflate(R.layout.meviewheader_item, null)
        val listsFooter = layoutInflater.inflate(R.layout.meviewlists_footer, null)
        val list = findViewById<RecyclerView>(R.id.homeList)
        list.applyContentInsets()
        list.layoutManager = LinearLayoutManager(this)
        // The user's lists are their own adapter so edit mode can drag them; the header ends at the
        // Teachable Tags row and this footer picks the group back up at New list….
        listsAdapter = ListRowsAdapter(this, list) { listsChanged() }
        list.adapter =
            ConcatAdapter(
                StaticViewAdapter(view = header),
                listsAdapter,
                StaticViewAdapter(view = listsFooter),
                favoritesAdapter,
                StaticViewAdapter(R.layout.meviewfooter),
            )
        list.addItemDecoration(SavedTagListAdapter.RowDivider(this))
        setUpListsFooter(listsFooter)
        listEditor =
            SavedListEditor(
                this,
                ListModel("favorite"),
                favoritesAdapter,
                list,
                R.string.Favorites,
                savedInstanceState,
                companion = { TagLists.customKeys.isNotEmpty() },
            )
        listEditor.onEditingChanged = { editing -> listsAdapter.isEditing = editing }
        listsAdapter.isEditing = listEditor.isEditing

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
                reveal = { id ->
                    val index = favoritesAdapter.currentList.indexOf(id)
                    if (index >= 0) list.smoothScrollToPosition(index + favoritesStartPosition)
                },
            )
        tagPane.onCreate(savedInstanceState)
    }

    /** The New list… row and the Favorites heading that close the Lists group. */
    private fun setUpListsFooter(footer: View) {
        footer.bindTo(R.id.favoritesEmptyText, "Visibility", { FavoritesModel.favoriteIds.size == 0 }, BoolConverter.get())
        val newList = footer.findViewById<View>(R.id.newListButton)
        ViewCompat.setAccessibilityDelegate(
            newList,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = android.widget.Button::class.java.name
                }
            },
        )
        newList.setOnClickListener { ListNameDialog.create(supportFragmentManager) }
    }

    /** A list created, deleted or renamed changes what Edit applies to. */
    private fun listsChanged() {
        if (::listEditor.isInitialized) listEditor.contentChanged()
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
        if (::listsAdapter.isInitialized) listsAdapter.afterTouchEvent(event)
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
        listsAdapter.resume()
    }

    override fun onPause() {
        listEditor.pause()
        listsAdapter.pause()
        super.onPause()
    }

    override fun onDestroy() {
        listEditor.destroy()
        listsAdapter.dispose()
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
