package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableCollection
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Function
import com.bindroid.utils.Property
import com.bindroid.utils.ReflectedProperty

class TeachableTagsActivity : AppCompatActivity() {
    val teachableTags: TrackableCollection<Int>
        get() = TeachableTagsModel.teachableTagIds

    /** Recycled teachable-tag rows, keyed by tag id; exposed for tests. */
    lateinit var teachableAdapter: SavedTagListAdapter
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.teachabletagsview)
        setUpToolbar(true)

        teachableAdapter = SavedTagListAdapter({ TeachableTagsModel.teachableTagIds }) { TeachableTagItemView(it) }
        val list = findViewById<RecyclerView>(R.id.teachableTagsItemsControl)
        list.applyContentInsets()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = teachableAdapter
        list.addItemDecoration(SavedTagListAdapter.RowDivider(this))

        UiBinder.bind(
            ReflectedProperty(
                findViewById(R.id.teachableEmptyState),
                "Visibility",
            ),
            Property(Function { teachableTags.size == 0 }, null, Boolean::class.java),
            BindingMode.ONE_WAY,
            BoolConverter.get(),
        )

        supportActionBar?.title = getString(R.string.home_title).makeTitleString(this)
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
