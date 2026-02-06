package depollsoft.tagmaster

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import com.google.android.material.bottomnavigation.BottomNavigationView
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.kotlin.ui.attachToViewPager
import depollsoft.lib.kotlin.ui.safeDismiss
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class TagDetailActivity : AppCompatActivity() {
    var tag: Tag? by trackable()
    private var progress: ProgressDialog? = null

    private val shareIntent: Intent
        get() {
            return ShareCompat.IntentBuilder(this)
                .setChooserTitle("Share a Tag")
                .setType("text/plain")
                .setSubject("${this.tag!!.title} - Tag Master")
                .setText(
                    """
                        Tag Title: ${tag!!.title}
                        ${tag!!.tagUri}
                        
                        Sent from Tag Master
                        http://www.davidpoll.com/applications/tag-master
                    """.trimIndent()
                )
                .createChooserIntent()
        }

    val tagId: Int
        get() = this.intent.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1)


    private fun loadQueryItem(refresh: Boolean) {
        val original = this.tag
        if (original != null) {
            val cache = ContentCache(this)
            val contentToDelete = ArrayList<RemoteLocation?>()
            contentToDelete.add(original.allPartsTrackUri)
            contentToDelete.add(original.baritoneTrackUri)
            contentToDelete.add(original.bassTrackUri)
            contentToDelete.add(original.leadTrackUri)
            contentToDelete.add(original.notationUri)
            contentToDelete.add(original.other1TrackUri)
            contentToDelete.add(original.other2TrackUri)
            contentToDelete.add(original.other3TrackUri)
            contentToDelete.add(original.other4TrackUri)
            contentToDelete.add(original.tenorTrackUri)
            contentToDelete.add(original.sheetMusicUri)
            for (loc in contentToDelete) {
                if (loc != null) {
                    cache.deletePrivateContent(loc.uri, loc.type)
                    cache.deletePublicContent(loc.uri, loc.type)
                }
            }
        }
        if (this.progress!!.isShowing)
            return
        this.progress!!.isIndeterminate = true
        this.progress!!.setMessage("Loading...")
        this.progress!!.show()

        val tagId = this.intent.extras!!.getInt(TagDetailActivity.TAG_ID_EXTRA)

        Tag.loadTagById(tagId, refresh).continueWith { task ->
            try {
                progress!!.safeDismiss()
            } catch (e: Exception) {
                // Sometimes this throws.
            }

            if (!task.isFaulted) {
                this@TagDetailActivity.tag = task.result
                Activities.invalidateOptionsMenu(this@TagDetailActivity)
            } else {
                this.tag = null
                CoroutineScope(Dispatchers.Main + Job()).launch {
                    try {
                        Toast.makeText(
                            this@TagDetailActivity.applicationContext,
                            "Unable to load tag or tag not found",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } catch (e: java.lang.Exception) {
                        Log.e("depollsoft.tagmaster", "Showing toast failed", e)
                    }
                }
            }
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagdetailview)

        this.progress = ProgressDialog(this)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        viewPager.adapter = object : FragmentStateAdapter(this.supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int {
                return 4
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> TagSummaryFragment()
                    1 -> TagMiscFragment()
                    2 -> TagTracksFragment()
                    3 -> TagVideosFragment()
                    else -> Fragment()
                }
            }
        }

        bottomNavigation.attachToViewPager(viewPager)

        UiBinder.bind(
            ReflectedProperty(this, "Title"), ReflectedProperty(this, "Tag.Title"),
            BindingMode.ONE_WAY
        )

        UiBinder.bind(this, R.id.bottomNavigation, "Visibility", "Tag", BoolConverter.get())

        this.loadQueryItem(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (this.tag == null)
            return false
        this.menuInflater.inflate(R.menu.tagdetailmenu, menu)
        MenuItems.setShowAsAction(
            menu.findItem(R.id.addFavoriteMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM
        )
        MenuItems.setShowAsAction(
            menu.findItem(R.id.removeFavoriteMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM
        )
        MenuItems.setShowAsAction(
            menu.findItem(R.id.shareMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val tag = this.tag
        try {
            if (tag != null) {
                when (item.itemId) {
                    R.id.addFavoriteMenuItem -> FavoritesModel.addFavorite(tag.id)
                    R.id.removeFavoriteMenuItem -> FavoritesModel.removeFavorite(tag.id)
                    R.id.addTeachableTagMenuItem -> TeachableTagsModel.addTeachableTag(tag.id)
                    R.id.removeTeachableTagMenuItem -> TeachableTagsModel.removeTeachableTag(tag.id)
                    R.id.addToListMenuItem -> {
                        showAddToListDialog(tag.id)
                        return true
                    }
                    R.id.shareMenuItem -> {
                        val i = this.shareIntent
                        this.startActivity(i)
                    }
                    R.id.refreshMenuItem -> {
                        this.loadQueryItem(true)
                        return true
                    }
                }
            }
        } finally {
            Activities.invalidateOptionsMenu(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val tag = this.tag
        if (tag != null) {
            menu.findItem(R.id.addFavoriteMenuItem).isVisible =
                !FavoritesModel.getIsFavorite(tag.id)
            menu.findItem(R.id.removeFavoriteMenuItem).isVisible =
                FavoritesModel.getIsFavorite(tag.id)
            menu.findItem(R.id.addTeachableTagMenuItem).isVisible =
                !TeachableTagsModel.getIsTeachableTag(tag.id)
            menu.findItem(R.id.removeTeachableTagMenuItem).isVisible =
                TeachableTagsModel.getIsTeachableTag(tag.id)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun showAddToListDialog(tagId: Int) {
        val allLists = CustomListsModel.allLists()
        val names = allLists.map { it.name }.toTypedArray()
        val checked = allLists.map { ListModel(it.key).ids.contains(tagId) }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.AddToList)
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                val list = allLists[which]
                val model = ListModel(list.key)
                if (isChecked) {
                    model.add(tagId)
                } else {
                    model.remove(tagId)
                }
            }
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.CreateNewList) { _, _ ->
                showCreateAndAddDialog(tagId)
            }
            .show()
    }

    private fun showCreateAndAddDialog(tagId: Int) {
        val editText = EditText(this)
        editText.hint = getString(R.string.EnterListName)
        AlertDialog.Builder(this)
            .setTitle(R.string.CreateNewList)
            .setView(editText)
            .setPositiveButton(R.string.Create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val key = CustomListsModel.createList(name)
                    ListModel(key).add(tagId)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        editText.requestFocus()
    }

    companion object {
        @JvmField
        val TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"
    }
}
