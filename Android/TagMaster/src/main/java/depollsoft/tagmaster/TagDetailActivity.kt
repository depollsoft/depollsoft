package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.Tag

class TagDetailActivity : AppCompatActivity() {
    var tag: Tag? by trackable()
    var isLoading: Boolean by trackable(false)
    var loadFailed: Boolean by trackable(false)

    /** Toolbar title: the tag once loaded, the app name while loading or after a failure. */
    val displayTitle: CharSequence
        get() = tag?.title ?: getString(R.string.detail_brand_title).makeTitleString(this)

    private val shareIntent: Intent
        get() {
            return ShareCompat
                .IntentBuilder(this)
                .setChooserTitle(R.string.detail_share_title)
                .setType("text/plain")
                .setSubject(getString(R.string.detail_share_subject, tag!!.title))
                .setText(getString(R.string.detail_share_text, tag!!.title, tag!!.tagUri))
                .createChooserIntent()
        }

    val tagId: Int
        get() = this.intent.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1)

    private fun loadQueryItem(refresh: Boolean) {
        if (isLoading || isFinishing || isDestroyed) return
        val original = this.tag
        val contentToDelete =
            if (original == null) {
                emptyList()
            } else {
                listOfNotNull(
                    original.allPartsTrackUri,
                    original.baritoneTrackUri,
                    original.bassTrackUri,
                    original.leadTrackUri,
                    original.notationUri,
                    original.other1TrackUri,
                    original.other2TrackUri,
                    original.other3TrackUri,
                    original.other4TrackUri,
                    original.tenorTrackUri,
                    original.sheetMusicUri,
                )
            }
        isLoading = true
        loadFailed = false
        invalidateOptionsMenu()

        val cache = ContentCache(this)
        Task
            .callInBackground<Void> {
                // Refresh drops the tag's cached media before reloading; keep the file I/O off the UI thread.
                for (loc in contentToDelete) {
                    cache.deletePrivateContent(loc.uri, loc.type)
                    cache.deletePublicContent(loc.uri, loc.type)
                }
                null
            }.continueWithTask { Tag.loadTagById(tagId, refresh) }
            .continueWith { task ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    isLoading = false
                    if (!task.isFaulted && !task.isCancelled && task.result != null) {
                        tag = task.result
                    } else {
                        tag = null
                        loadFailed = true
                        findViewById<TextView>(R.id.detailErrorText).text =
                            getString(R.string.detail_tag_load_failed, tagId)
                    }
                    Activities.invalidateOptionsMenu(this)
                }
                null
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagdetailview)

        setUpToolbar(true)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

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

        val tabs = PopupMenu(this, tabLayout).apply { inflate(R.menu.tagdetailnavigation) }.menu
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val item = tabs.getItem(position)
            tab.text = item.title
            tab.icon = item.icon
            tab.id = item.itemId
        }.attach()

        UiBinder.bind(
            ReflectedProperty(this, "Title"),
            ReflectedProperty(this, "DisplayTitle"),
            BindingMode.ONE_WAY,
        )

        tabLayout.applyContentInsets(bottom = false)
        UiBinder.bind(this, R.id.tabLayout, "Visibility", "Tag", BoolConverter.get())
        UiBinder.bind(this, R.id.progress, "Visibility", "IsLoading", BoolConverter.get())
        UiBinder.bind(this, R.id.viewPager, "Visibility", "LoadFailed", BoolConverter.get(true))
        UiBinder.bind(this, R.id.detailErrorState, "Visibility", "LoadFailed", BoolConverter.get())
        findViewById<View>(R.id.detailRetryButton).setOnClickListener { loadQueryItem(false) }

        this.loadQueryItem(false)
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()

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
                    R.id.addFavoriteMenuItem -> {
                        FavoritesModel.addFavorite(tag.id)
                    }

                    R.id.removeFavoriteMenuItem -> {
                        FavoritesModel.removeFavorite(tag.id)
                    }

                    R.id.addTeachableTagMenuItem -> {
                        TeachableTagsModel.addTeachableTag(tag.id)
                    }

                    R.id.removeTeachableTagMenuItem -> {
                        TeachableTagsModel.removeTeachableTag(tag.id)
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
            menu.findItem(R.id.refreshMenuItem)?.isEnabled = !isLoading
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
