package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bindroid.converters.AdapterConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.parse.ParseUser
import com.parse.RefreshCallback
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.ui.ChangelogViewer

class MeActivity : AppCompatActivity() {

    val favoriteIds: TrackableCollection<Int>
        get() = FavoritesModel.getFavoriteIds()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.meview)

        UiBinder.bind(this, R.id.favoritesItemsControl, "Adapter", "FavoriteIds", AdapterConverter(
                FavoriteTagItemView::class.java, false, true))

        this.supportActionBar?.title = "Tag Master".makeTitleString(this)

        if (ParseUser.getCurrentUser() != null) {
            try {
                ParseUser.getCurrentUser().refreshInBackground(RefreshCallback { _, err ->
                    if (err != null) {
                        return@RefreshCallback
                    }
                    FavoritesModel.restoreFromUser()
                    TeachableTagsModel.restoreFromUser()
                })
            } catch (e: Exception) {
            }

        }

        val viewer = ChangelogViewer(this, this.getString(R.string.Changelog))
        viewer.setTitle("Tag Master Changelog")
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
