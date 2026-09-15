package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
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

class TeachableTagsActivity :
    AppCompatActivity(),
    TagPaneHost {
    val teachableTags: TrackableCollection<Int>
        get() = TeachableTagsModel.teachableTagIds

    internal lateinit var tagPane: TagPaneController
        private set

    /** Recycled teachable-tag rows, keyed by tag id; exposed for tests. */
    lateinit var teachableAdapter: SavedTagListAdapter
        private set

    lateinit var listEditor: SavedListEditor
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
        listEditor = SavedListEditor(this, ListModel("teachable"), teachableAdapter, list, R.string.TeachableTags, savedInstanceState)

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

        tagPane =
            TagPaneController(
                activity = this,
                listedIds = { TeachableTagsModel.teachableTagIds.toList() },
                reveal = { id ->
                    val index = teachableAdapter.currentList.indexOf(id)
                    if (index >= 0) list.smoothScrollToPosition(index)
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
        menuInflater.inflate(R.menu.savedlistmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = listEditor.selectMenu(item) || super.onOptionsItemSelected(item)

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = tagPane.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    override fun onSupportNavigateUp() = navigateUpOrHome()

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

    override fun onSearchRequested(): Boolean {
        val i = Intent(this, TagSearchActivity::class.java)
        this.startActivity(i)
        return true
    }
}
