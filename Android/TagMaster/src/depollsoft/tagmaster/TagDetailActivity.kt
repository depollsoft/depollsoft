package depollsoft.tagmaster

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.kotlin.ui.attachToViewPager
import depollsoft.lib.util.ContentCache
import depollsoft.lib.util.IntentUtilities
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import java.util.*

class TagDetailActivity : AppCompatActivity() {
    var tag: Tag? by TrackableField<Tag?>()
    private var progress: ProgressDialog? = null

    private val emailIntent: Intent
        get() {
            val i = Intent(Intent.ACTION_SEND)
            i.putExtra(Intent.EXTRA_SUBJECT, this.tag!!.title + " - Tag Master for Android")
            i.putExtra(
                    Intent.EXTRA_TEXT,
                    String
                            .format(
                                    "Tag Title: %s\n%s\n\n\nSent from Tag Master for Android\nhttp://www.davidpoll.com/applications/tag-master",
                                    this.tag!!.title, this.tag!!.tagUri))
            i.type = "text/plain"
            return i
        }

    private val smsIntent: Intent
        get() {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            i.putExtra("sms_body", String.format("%s %s - Sent from Tag Master", this.tag!!.title,
                    this.tag!!.tagUri))
            return i
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
                if (this@TagDetailActivity.progress!!.isShowing) {
                    this@TagDetailActivity.progress!!.dismiss()
                }
            } catch (e: Exception) {
                // Sometimes this throws.
            }

            if (!task.isFaulted) {
                this@TagDetailActivity.tag = task.result
                Activities.invalidateOptionsMenu(this@TagDetailActivity)
            } else {
                this.tag = null
            }
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.tagdetailview)

        this.progress = ProgressDialog(this)

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        viewPager.adapter = object : FragmentPagerAdapter(this.supportFragmentManager) {
            override fun getCount(): Int {
                return 4
            }

            override fun getItem(position: Int): Fragment {
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

        UiBinder.bind(ReflectedProperty(this, "Title"), ReflectedProperty(this, "Tag.Title"),
                BindingMode.ONE_WAY)

        UiBinder.bind(this, R.id.bottomNavigation, "Visibility", "Tag", BoolConverter.get())

        this.loadQueryItem(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (this.tag == null)
            return false
        this.menuInflater.inflate(R.menu.tagdetailmenu, menu)
        MenuItems.setShowAsAction(menu.findItem(R.id.addFavoriteMenuItem),
                MenuItems.SHOW_AS_ACTION_IF_ROOM)
        MenuItems.setShowAsAction(menu.findItem(R.id.removeFavoriteMenuItem),
                MenuItems.SHOW_AS_ACTION_IF_ROOM)
        MenuItems.setShowAsAction(menu.findItem(R.id.smsMenuItem), MenuItems.SHOW_AS_ACTION_IF_ROOM)
        menu.findItem(R.id.smsMenuItem).isVisible = IntentUtilities.isIntentAvailable(this, this.smsIntent)
        menu.findItem(R.id.emailMenuItem).isVisible = IntentUtilities.isIntentAvailable(this, this.emailIntent)
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
                    R.id.emailMenuItem -> {
                        val i = this.emailIntent
                        this.startActivity(i)
                    }
                    R.id.smsMenuItem -> {
                        val i = this.smsIntent
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
            menu.findItem(R.id.addFavoriteMenuItem).isVisible = !FavoritesModel.getIsFavorite(tag.id)
            menu.findItem(R.id.removeFavoriteMenuItem).isVisible = FavoritesModel.getIsFavorite(tag.id)
            menu.findItem(R.id.addTeachableTagMenuItem).isVisible = !TeachableTagsModel.getIsTeachableTag(tag.id)
            menu.findItem(R.id.removeTeachableTagMenuItem).isVisible = TeachableTagsModel.getIsTeachableTag(tag.id)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    companion object {
        @JvmField
        val TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"
    }
}
