package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.Tracker
import com.bindroid.utils.Function
import com.bindroid.utils.bindTo

/**
 * One of the user's own lists: the same screen as [TeachableTagsActivity], over
 * `ListModel(listKey)` instead of a fixed key, plus the rename and delete actions that only a
 * user-defined list has.
 *
 * The list's name is the title, so a rename anywhere (including from another device) retitles the
 * screen; a delete anywhere closes it rather than leaving a screen onto nothing.
 */
class TagListActivity :
    AppCompatActivity(),
    TagPaneHost {
    val listKey: String
        get() = intent?.getStringExtra(EXTRA_LIST_KEY).orEmpty()

    /** The name this screen is currently titled with. */
    var listName: String = ""
        private set

    internal lateinit var tagPane: TagPaneController
        private set

    /** Recycled rows, keyed by tag id; exposed for tests. */
    lateinit var listAdapter: SavedTagListAdapter
        private set

    lateinit var listEditor: SavedListEditor
        private set

    private lateinit var model: ListModel
    private var released = false

    // Bindroid registrations are one-shot and every call to Trackable.track adds another, so the
    // screen holds exactly one and renews it only after it has fired.
    private var tracking = false
    private val registryTracker =
        object : Tracker {
            override fun update() {
                tracking = false
                if (released) return
                runOnUiThread { renderRegistry() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = listKey
        if (key.isEmpty() || !TagLists.isCustom(key) || !TagLists.customKeys.contains(key)) {
            // A stale shortcut or a list deleted before this screen opened: there is nothing to show.
            finish()
            return
        }
        setContentView(R.layout.taglistview)
        setUpToolbar(true)
        model = ListModel(key)

        listAdapter = SavedTagListAdapter({ model.ids }) { CustomListTagItemView(it) }
        val list = findViewById<RecyclerView>(R.id.tagListItemsControl)
        list.applyContentInsets()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = listAdapter
        list.addItemDecoration(SavedTagListAdapter.RowDivider(this))
        listEditor = SavedListEditor(this, model, listAdapter, list, TagLists.name(key), savedInstanceState)

        bindTo(R.id.tagListEmptyState, "Visibility", { model.ids.size == 0 }, BoolConverter.get())
        findViewById<View>(R.id.tagListBrowseButton).setOnClickListener {
            startActivity(Intent(this, TagBrowserActivity::class.java))
        }

        supportFragmentManager.setFragmentResultListener(REQUEST_RENAME, this) { _, _ -> renderRegistry() }

        tagPane =
            TagPaneController(
                activity = this,
                listedIds = { model.ids.toList() },
                reveal = { id ->
                    val index = listAdapter.currentList.indexOf(id)
                    if (index >= 0) list.smoothScrollToPosition(index)
                },
            )
        tagPane.onCreate(savedInstanceState)
        renderRegistry()
    }

    /**
     * Re-reads the list's name and whether it still exists, and re-registers for the next change.
     */
    private fun renderRegistry() {
        if (released || isFinishing || isDestroyed || !::model.isInitialized) return
        val key = listKey
        val read = Function<Pair<Boolean, String>> { TagLists.customKeys.contains(key) to TagLists.name(key) }
        val state =
            if (tracking) {
                read.evaluate()
            } else {
                Trackable.track(registryTracker, read).also { tracking = true }
            }
        if (!state.first) {
            finish()
            return
        }
        if (state.second == listName) return
        listName = state.second
        supportActionBar?.title = listName
        listEditor.listLabel = listName
        findViewById<TextView>(R.id.tagListEmptyText)?.text = getString(R.string.list_empty_title, listName)
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
        menuInflater.inflate(R.menu.taglistmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (::listEditor.isInitialized && listEditor.selectMenu(item)) return true
        return when (item.itemId) {
            R.id.renameListMenuItem -> {
                ListNameDialog.rename(supportFragmentManager, listKey, REQUEST_RENAME)
                true
            }

            R.id.deleteListMenuItem -> {
                ListRowMenu.confirmDelete(this, listKey)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (::listEditor.isInitialized) listEditor.prepareMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = (::tagPane.isInitialized && tagPane.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event)

    override fun onSupportNavigateUp() = navigateUpOrHome()

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val handled = super.dispatchTouchEvent(event)
        if (::listEditor.isInitialized) listEditor.afterTouchEvent(event)
        return handled
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::listEditor.isInitialized) listEditor.saveState(outState)
        if (::tagPane.isInitialized) tagPane.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::listEditor.isInitialized) {
            listEditor.resume()
            renderRegistry()
        }
    }

    override fun onPause() {
        if (::listEditor.isInitialized) listEditor.pause()
        super.onPause()
    }

    override fun onDestroy() {
        released = true
        if (::listEditor.isInitialized) listEditor.destroy()
        super.onDestroy()
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, TagSearchActivity::class.java))
        return true
    }

    companion object {
        const val EXTRA_LIST_KEY = "depollsoft.tagmaster.listKey"
        private const val REQUEST_RENAME = "depollsoft.tagmaster.tagList.rename"

        fun intent(
            context: Context,
            key: String,
        ): Intent = Intent(context, TagListActivity::class.java).putExtra(EXTRA_LIST_KEY, key)
    }
}
