package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.progressindicator.LinearProgressIndicator
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import java.util.ArrayList

class TagDetailActivity : AppCompatActivity() {
    var tag: Tag? by trackable()

    private val shareIntent: Intent
        get() {
            return ShareCompat
                .IntentBuilder(this)
                .setChooserTitle("Share a Tag")
                .setType("text/plain")
                .setSubject("${this.tag!!.title} - Tag Master")
                .setText(
                    """
                    Tag Title: ${tag!!.title}
                    ${tag!!.tagUri}

                    Sent from Tag Master
                    http://www.davidpoll.com/applications/tag-master
                    """.trimIndent(),
                ).createChooserIntent()
        }

    val tagId: Int
        get() = this.intent.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1)

    private var loading = false

    private fun setLoading(value: Boolean) {
        loading = value
        findViewById<LinearProgressIndicator>(R.id.detailProgress).visibility =
            if (value) View.VISIBLE else View.GONE
        if (value) {
            findViewById<View>(R.id.detailError).visibility = View.GONE
        }
    }

    private fun loadQueryItem(refresh: Boolean) {
        if (tagId <= 0) {
            setLoading(false)
            findViewById<View>(R.id.detailError).visibility = View.VISIBLE
            findViewById<View>(R.id.detailRetryButton).visibility = View.GONE
            return
        }
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
        if (loading) return
        setLoading(true)

        val tagId = this.tagId

        Tag.loadTagById(tagId, refresh).continueWith { task ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)
                if (!task.isFaulted && !task.isCancelled && task.result != null) {
                    this@TagDetailActivity.tag = task.result
                    findViewById<View>(R.id.detailError).visibility = View.GONE
                    Activities.invalidateOptionsMenu(this@TagDetailActivity)
                } else {
                    // Stay on the screen and say what happened: a failed fetch in a
                    // rehearsal room is usually the room's wifi, and the singer
                    // wants to try again rather than start over.
                    this@TagDetailActivity.tag = null
                    findViewById<View>(R.id.detailError).visibility = View.VISIBLE
                    Activities.invalidateOptionsMenu(this@TagDetailActivity)
                }
            }
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagdetailview)
        enableDeskBack()

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val navigation = findViewById<NavigationBarView>(R.id.bottomNavigation)

        viewPager.adapter =
            object : FragmentStateAdapter(this.supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = 4

                override fun createFragment(position: Int): Fragment =
                    when (position) {
                        0 -> TagSummaryFragment()
                        1 -> TagMiscFragment()
                        2 -> TagTracksFragment()
                        3 -> TagVideosFragment()
                        else -> Fragment()
                    }
            }

        navigation.attachToPager(viewPager)

        findViewById<View>(R.id.detailRoot).applyDeskInsets()

        findViewById<View>(R.id.detailRetryButton).setOnClickListener {
            loadQueryItem(true)
        }

        UiBinder.bind(
            ReflectedProperty(this, "Title"),
            ReflectedProperty(this, "Tag.Title"),
            BindingMode.ONE_WAY,
        )

        UiBinder.bind(this, R.id.bottomNavigation, "Visibility", "Tag", BoolConverter.get())

        this.loadQueryItem(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (this.tag == null) {
            return false
        }
        this.menuInflater.inflate(R.menu.tagdetailmenu, menu)
        MenuItems.setShowAsAction(
            menu.findItem(R.id.addFavoriteMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM,
        )
        MenuItems.setShowAsAction(
            menu.findItem(R.id.removeFavoriteMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM,
        )
        MenuItems.setShowAsAction(
            menu.findItem(R.id.shareMenuItem),
            MenuItems.SHOW_AS_ACTION_IF_ROOM,
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val tag = this.tag
        try {
            if (tag != null) {
                when (item.itemId) {
                    // Adding to a list is a save the singer needs to trust while
                    // looking at the room rather than the screen.
                    R.id.addFavoriteMenuItem -> {
                        FavoritesModel.addFavorite(tag.id)
                        confirmSave()
                    }

                    R.id.removeFavoriteMenuItem -> {
                        FavoritesModel.removeFavorite(tag.id)
                        confirmSave()
                    }

                    R.id.addTeachableTagMenuItem -> {
                        TeachableTagsModel.addTeachableTag(tag.id)
                        confirmSave()
                    }

                    R.id.removeTeachableTagMenuItem -> {
                        TeachableTagsModel.removeTeachableTag(tag.id)
                        confirmSave()
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

    private fun confirmSave() {
        findViewById<View>(R.id.detailRoot)?.confirmHaptic()
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

    companion object {
        @JvmField
        val TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"
    }
}
