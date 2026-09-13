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
import com.bindroid.trackable.trackable
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.Tag

class TagDetailActivity : AppCompatActivity() {
    // Tag.equals compares IDs; a refreshed instance still needs to update every bound field.
    private val currentTag = com.bindroid.trackable.ComparingTrackableField<Tag?>(null) { left, right -> left === right }
    var tag: Tag?
        get() = currentTag.get()
        set(value) {
            currentTag.set(value)
        }
    var isLoading: Boolean by trackable(false)
    var loadFailed: Boolean by trackable(false)

    // Per-screen request dependency. Tests can control completion without changing the cache contract.
    internal var tagLoader: (Int, Boolean) -> Task<Tag> = { id, refresh -> Tag.loadTagById(id, refresh) }
    private var requestGeneration = 0
    private var retryRefresh = false
    private var refreshError: Snackbar? = null

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
        val requestedId = tagId
        val generation = ++requestGeneration
        val original = tag
        retryRefresh = refresh
        refreshError?.dismiss()
        isLoading = true
        loadFailed = false
        renderState()
        invalidateOptionsMenu()

        val request =
            if (refresh && original != null) {
                Task
                    .callInBackground<Void> {
                        val cache = ContentCache(this)
                        for (loc in listOfNotNull(
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
                        )) {
                            cache.deletePrivateContent(loc.uri, loc.type)
                            cache.deletePublicContent(loc.uri, loc.type)
                        }
                        null
                    }.continueWithTask { tagLoader(requestedId, refresh) }
            } else {
                try {
                    tagLoader(requestedId, refresh)
                } catch (error: Exception) {
                    Task.forError<Tag>(error)
                }
            }
        request.continueWith { task ->
            runOnUiThread {
                if (isFinishing || isDestroyed || generation != requestGeneration || tagId != requestedId) return@runOnUiThread
                isLoading = false
                if (!task.isFaulted && !task.isCancelled && task.result?.id == requestedId) {
                    tag = task.result
                } else {
                    // A refresh failure must not discard an already-visible tag.
                    loadFailed = true
                    findViewById<TextView>(R.id.detailErrorText).text = getString(R.string.detail_tag_load_failed, requestedId)
                    if (tag != null) {
                        refreshError =
                            Snackbar
                                .make(
                                    findViewById(R.id.frameLayout1),
                                    getString(R.string.detail_tag_refresh_failed, requestedId),
                                    Snackbar.LENGTH_INDEFINITE,
                                ).setAction(R.string.detail_retry) { loadQueryItem(true) }
                        refreshError?.show()
                    }
                }
                renderState()
                Activities.invalidateOptionsMenu(this)
            }
            null
        }
    }

    private fun renderState() {
        val loaded = tag != null
        val initialLoading = isLoading && !loaded

        fun show(
            id: Int,
            visible: Boolean,
        ) {
            findViewById<View>(id).visibility = if (visible) View.VISIBLE else View.GONE
        }
        show(R.id.detailLoadingState, initialLoading)
        findViewById<TagLoadingView>(R.id.quartetIllustration).loading = initialLoading
        val status = getString(R.string.detail_loading_tag, tagId)
        findViewById<TextView>(R.id.detailLoadingStatus).text = status
        findViewById<View>(R.id.detailLoadingComposition).contentDescription = getString(R.string.detail_gathering_quartet) + " " + status
        show(R.id.imageView1, loaded)
        show(R.id.viewPager, loaded)
        show(R.id.tabLayout, loaded)
        show(R.id.progress, isLoading && loaded)
        show(R.id.detailErrorState, loadFailed && !loaded && !isLoading)
        findViewById<View>(R.id.detailRetryButton).isEnabled = !isLoading
        if (loaded) setUpPagerIfNeeded()
    }

    private fun setUpPagerIfNeeded() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager.adapter != null) return
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        viewPager.adapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
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
            tab.setCustomView(R.layout.bottom_tab_content)
        }.attach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tagdetailview)
        setUpToolbar(true)
        UiBinder.bind(ReflectedProperty(this, "Title"), ReflectedProperty(this, "DisplayTitle"), BindingMode.ONE_WAY)
        findViewById<TabLayout>(R.id.tabLayout).applyHorizontalInsetsAsPadding()
        findViewById<View>(R.id.detailLoadingState).applyHorizontalInsetsAsPadding()
        findViewById<View>(R.id.detailRetryButton).setOnClickListener { loadQueryItem(retryRefresh) }
        loadQueryItem(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getIntExtra(TAG_ID_EXTRA, -1) == tagId) return
        ++requestGeneration
        setIntent(intent)
        tag = null
        isLoading = false
        loadQueryItem(false)
    }

    override fun onResume() {
        super.onResume()
        findViewById<TagLoadingView>(R.id.quartetIllustration).hostResumed = true
    }

    override fun onPause() {
        findViewById<TagLoadingView>(R.id.quartetIllustration).hostResumed = false
        super.onPause()
    }

    override fun onDestroy() {
        ++requestGeneration
        refreshError?.dismiss()
        findViewById<TagLoadingView>(R.id.quartetIllustration).loading = false
        super.onDestroy()
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
