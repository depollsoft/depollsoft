package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.TrackableCollection
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.ui.ChangelogViewer

class MeActivity : AppCompatActivity() {
    val favoriteIds: TrackableCollection<Int>
        get() = FavoritesModel.favoriteIds

    /** Recycled favorites rows, keyed by tag id; exposed for tests. */
    lateinit var favoritesAdapter: SavedTagListAdapter
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

        this.supportActionBar?.title = getString(R.string.home_title).makeTitleString(this)

        val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
        viewer.setTitle(getString(R.string.home_changelog_title))
        viewer.setIcon(R.mipmap.ic_launcher)
        viewer.showIfAppropriate()

        findViewById<View>(R.id.searchButton).setOnClickListener {
            val i = Intent(this, TagSearchActivity::class.java)
            startActivity(i)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.memenu, menu)

        MenuItems
            .setShowAsAction(menu.findItem(R.id.settingsMenuItem), MenuItems.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsMenuItem) {
            val i = Intent(this, SettingsActivity::class.java)
            this.startActivity(i)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
