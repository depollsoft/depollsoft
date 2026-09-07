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
                    if (chooseInitialMaterial) {
                        chooseInitialMaterial = false
                        if (task.result.tracks.isNullOrEmpty()) openMaterial("details")
                    }
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

        selectedMaterial = savedInstanceState?.getString("material")
        wideWorkspace = resources.configuration.screenWidthDp >= 720 && resources.configuration.fontScale < 1.5f
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.summaryContainer, TagSummaryFragment(), "summary")
                .commitNow()
        }
        materialBack =
            object : androidx.activity.OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    selectedMaterial = null
                    updateWorkspace()
                }
            }
        onBackPressedDispatcher.addCallback(this, materialBack)
        if (wideWorkspace && savedInstanceState == null) {
            selectedMaterial = "tracks"
            chooseInitialMaterial = true
        }
        updateWorkspace()

        findViewById<View>(R.id.detailRoot).applyDeskInsets()

        findViewById<View>(R.id.detailRetryButton).setOnClickListener {
            loadQueryItem(true)
        }

        UiBinder.bind(
            ReflectedProperty(this, "Title"),
            ReflectedProperty(this, "Tag.Title"),
            BindingMode.ONE_WAY,
        )

        this.loadQueryItem(false)
    }

    private var selectedMaterial: String? = null
    private var wideWorkspace = false
    private var chooseInitialMaterial = false
    private lateinit var materialBack: androidx.activity.OnBackPressedCallback
    val hasOpenMaterial: Boolean get() = selectedMaterial != null

    fun openMaterial(material: String) {
        require(material in setOf("tracks", "details", "videos"))
        tag?.keyNote?.stop()
        selectedMaterial = material
        updateWorkspace()
    }

    private fun updateWorkspace() {
        val selected = selectedMaterial
        val summaryVisible = wideWorkspace || selected == null
        findViewById<View>(R.id.summaryContainer).visibility = if (summaryVisible) View.VISIBLE else View.GONE
        findViewById<View>(R.id.materialContainer).visibility = if (selected != null) View.VISIBLE else View.GONE
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.findFragmentByTag("summary")?.let {
            if (summaryVisible) transaction.show(it) else transaction.hide(it)
            transaction.setMaxLifecycle(
                it,
                if (summaryVisible) androidx.lifecycle.Lifecycle.State.RESUMED else androidx.lifecycle.Lifecycle.State.STARTED,
            )
        }
        for (name in listOf("tracks", "details", "videos")) {
            var fragment = supportFragmentManager.findFragmentByTag(name)
            if (name == selected && fragment == null) {
                fragment =
                    when (name) {
                        "tracks" -> TagTracksFragment()
                        "details" -> TagMiscFragment()
                        else -> TagVideosFragment()
                    }
                transaction.add(R.id.materialContainer, fragment, name)
            }
            fragment?.let {
                if (name == selected) transaction.show(it) else transaction.hide(it)
                transaction.setMaxLifecycle(
                    it,
                    if (name ==
                        selected
                    ) {
                        androidx.lifecycle.Lifecycle.State.RESUMED
                    } else {
                        androidx.lifecycle.Lifecycle.State.STARTED
                    },
                )
            }
        }
        transaction.commitNow()
        materialBack.isEnabled = selected != null
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        wideWorkspace = newConfig.screenWidthDp >= 720 && newConfig.fontScale < 1.5f
        updateWorkspace()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("material", selectedMaterial)
        super.onSaveInstanceState(outState)
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
