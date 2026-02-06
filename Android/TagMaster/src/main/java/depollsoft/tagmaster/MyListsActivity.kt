package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import depollsoft.lib.compat.ui.ActionBars

class MyListsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var noListsTextView: TextView
    private lateinit var adapter: ArrayAdapter<ListMetadata>
    private val lists = mutableListOf<ListMetadata>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.mylistsview)

        listView = findViewById(R.id.listsListView)
        noListsTextView = findViewById(R.id.noCustomListsTextView)

        adapter = object : ArrayAdapter<ListMetadata>(this, R.layout.mylistsitemview, lists) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.mylistsitemview, parent, false)
                val item = getItem(position)!!
                view.findViewById<TextView>(R.id.listNameTextView).text = item.name
                val count = ListModel(item.key).ids.size
                view.findViewById<TextView>(R.id.tagCountTextView).text = "$count tags"
                return view
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = lists[position]
            val intent = Intent(this, CustomListActivity::class.java)
            intent.putExtra(CustomListActivity.CUSTOM_LIST_KEY_EXTRA, item.key)
            startActivity(intent)
        }

        registerForContextMenu(listView)

        findViewById<View>(R.id.createListFab).setOnClickListener {
            showCreateDialog()
        }

        supportActionBar?.title = "Tag Master".makeTitleString(this)

        refreshLists()
    }

    override fun onResume() {
        super.onResume()
        refreshLists()
    }

    private fun refreshLists() {
        lists.clear()
        lists.addAll(CustomListsModel.customLists())
        adapter.notifyDataSetChanged()
        noListsTextView.visibility = if (lists.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCreateDialog() {
        val editText = EditText(this)
        editText.hint = getString(R.string.EnterListName)
        AlertDialog.Builder(this)
            .setTitle(R.string.CreateNewList)
            .setView(editText)
            .setPositiveButton(R.string.Create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    CustomListsModel.createList(name)
                    refreshLists()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        editText.requestFocus()
    }

    private fun showRenameDialog(item: ListMetadata) {
        val editText = EditText(this)
        editText.setText(item.name)
        AlertDialog.Builder(this)
            .setTitle(R.string.RenameList)
            .setView(editText)
            .setPositiveButton(R.string.Rename) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    CustomListsModel.renameList(item.key, name)
                    refreshLists()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        editText.requestFocus()
    }

    private fun showDeleteDialog(item: ListMetadata) {
        AlertDialog.Builder(this)
            .setTitle(R.string.DeleteList)
            .setMessage(R.string.DeleteListConfirm)
            .setPositiveButton(R.string.Delete) { _, _ ->
                CustomListsModel.deleteList(item.key)
                refreshLists()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.mylistscontextmenu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return super.onContextItemSelected(item)
        val listItem = lists[info.position]
        return when (item.itemId) {
            R.id.renameListMenuItem -> {
                showRenameDialog(listItem)
                true
            }
            R.id.deleteListMenuItem -> {
                showDeleteDialog(listItem)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == ActionBars.HOME_MENU_ITEM_ID) {
            val intent = Intent(this, MeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            this.startActivity(intent)
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
