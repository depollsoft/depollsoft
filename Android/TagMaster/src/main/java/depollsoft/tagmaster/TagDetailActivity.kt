package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import bolts.Task
import com.bindroid.BindingMode
import com.bindroid.ui.UiBinder
import com.bindroid.utils.ReflectedProperty
import depollsoft.tagmaster.barbershop.Tag

/**
 * Full-screen host for one tag. Everything the screen does lives in [TagDetailFragment]; this
 * activity owns only the chrome (toolbar, title, options menu) and the intent that names the tag.
 */
class TagDetailActivity :
    AppCompatActivity(),
    TagDetailHost {
    internal val detailFragment: TagDetailFragment?
        get() = supportFragmentManager.findFragmentById(R.id.tagDetailFragment) as? TagDetailFragment

    override var tag: Tag?
        get() = detailFragment?.tag
        set(value) {
            detailFragment?.tag = value
        }

    val isLoading: Boolean
        get() = detailFragment?.isLoading ?: false

    val loadFailed: Boolean
        get() = detailFragment?.loadFailed ?: false

    /**
     * Per-screen request dependency, read by the detail fragment when it starts its first load.
     * Tests set it before onCreate without changing the cache contract.
     */
    internal var tagLoader: (Int, Boolean) -> Task<Tag> = { id, refresh -> Tag.loadTagById(id, refresh) }

    /** Toolbar title: the tag once loaded, the app name while loading or after a failure. */
    val displayTitle: CharSequence
        get() = tag?.title ?: getString(R.string.detail_brand_title).makeTitleString(this)

    val tagId: Int
        get() = this.intent.getIntExtra(TAG_ID_EXTRA, -1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tagdetailview)
        setUpToolbar(true)
        UiBinder.bind(ReflectedProperty(this, "Title"), ReflectedProperty(this, "DisplayTitle"), BindingMode.ONE_WAY)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getIntExtra(TAG_ID_EXTRA, -1) == tagId) return
        setIntent(intent)
        detailFragment?.showTag(tagId)
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()

    override fun onCreateOptionsMenu(menu: Menu): Boolean = detailFragment?.buildMenu(menu, menuInflater) ?: false

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        detailFragment?.prepareMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (detailFragment?.handleMenuItem(item) == true) return true
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmField
        val TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"
    }
}
