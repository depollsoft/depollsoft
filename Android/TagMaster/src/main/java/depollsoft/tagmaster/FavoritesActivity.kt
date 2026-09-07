package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.bindroid.utils.bindTo

/** The existing favorite order and long-press operations, in their own destination. */
class FavoritesActivity : AppCompatActivity() {
    val favoriteIds: TrackableCollection<Int>
        get() = FavoritesModel.favoriteIds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDeskBack()
        setContentView(R.layout.favoritesview)
        UiBinder.bind(
            this,
            R.id.favoritesItemsControl,
            "Adapter",
            "FavoriteIds",
            AdapterConverter(FavoriteTagItemView::class.java, true, true),
        )
        findViewById<View>(R.id.favoritesRoot).apply {
            applyDeskInsets()
            bindTo(R.id.noFavoritesTextView, "Visibility", { favoriteIds.isEmpty() }, BoolConverter.get())
        }
        supportActionBar?.title = getString(R.string.Favorites).makeTitleString(this)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, TagSearchActivity::class.java))
        return true
    }
}
