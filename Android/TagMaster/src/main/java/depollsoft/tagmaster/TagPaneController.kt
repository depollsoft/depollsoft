package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import bolts.Task
import com.bindroid.trackable.TrackableField
import com.google.android.material.appbar.MaterialToolbar
import depollsoft.tagmaster.barbershop.Tag

/**
 * A tag list that can open a tag beside itself. Implemented by every list screen; on phones (and
 * any window narrower than the two-pane layout) [hasDetailPane] is false and rows keep opening
 * [TagDetailActivity] full-screen.
 */
interface TagPaneHost {
    val hasDetailPane: Boolean

    var selectedTagId: Int?

    /** Opens [id] in the detail pane, keeping the page (Summary/Details/Tracks/Videos) in view. */
    fun showTag(id: Int)

    /** The ids of the list the user is looking at, in the order they are shown. */
    fun listedTagIds(): List<Int>

    /** Scrolls the row for [id] into view. */
    fun revealTag(id: Int)
}

/**
 * The shared half of [TagPaneHost]: the detail pane's toolbar, its previous/next chevrons, the
 * "Pick a tag" invitation, and the one [TagDetailFragment] that every selection reuses so the open
 * page survives a tag change.
 */
class TagPaneController(
    private val activity: AppCompatActivity,
    private val listedIds: () -> List<Int>,
    private val reveal: (Int) -> Unit = {},
    private val hasMoreResults: () -> Boolean = { false },
    private val fetchMore: () -> Unit = {},
) {
    private val detailPane: View? = activity.findViewById(R.id.detailPane)
    private val detailToolbar: MaterialToolbar? = activity.findViewById(R.id.detailToolbar)
    private val detailContainer: ViewGroup? = activity.findViewById(R.id.detailContainer)
    private val emptyState: View? = activity.findViewById(R.id.tagPaneEmptyState)
    private val selected = TrackableField<Int?>(null)

    /** Test seam: the loader handed to the detail fragment instead of the live tag cache. */
    internal var tagLoader: ((Int, Boolean) -> Task<Tag>)? = null

    val hasDetailPane: Boolean
        get() = detailPane != null && detailContainer != null

    var selectedTagId: Int?
        get() = selected.get()
        set(value) {
            selected.set(value)
        }

    internal val detailFragment: TagDetailFragment?
        get() = activity.supportFragmentManager.findFragmentByTag(DETAIL_FRAGMENT_TAG) as? TagDetailFragment

    /** Call from the activity's onCreate, after setContentView. */
    fun onCreate(savedInstanceState: Bundle?) {
        if (!hasDetailPane) {
            // A window that lost its pane (a small tablet rotated to portrait) keeps only the list.
            detailFragment?.let {
                activity.supportFragmentManager
                    .beginTransaction()
                    .remove(it)
                    .commitNowAllowingStateLoss()
            }
            return
        }
        val toolbar = detailToolbar ?: return
        ViewCompat.setAccessibilityPaneTitle(toolbar, activity.getString(R.string.tag_pane_detail_title))
        activity.findViewById<MaterialToolbar>(R.id.toolbar)?.let {
            ViewCompat.setAccessibilityPaneTitle(it, activity.getString(R.string.tag_pane_list_title))
        }
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.tagpanemenu)
        toolbar.setOnMenuItemClickListener { item -> onMenuItem(item) }
        adopt(detailFragment)

        val restored = savedInstanceState?.getInt(STATE_SELECTED_TAG_ID, 0) ?: 0
        if (restored > 0) showTag(restored) else renderChrome()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAG_ID, selectedTagId ?: 0)
    }

    fun showTag(id: Int) {
        if (!hasDetailPane) {
            activity.startActivity(
                Intent(activity, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, id),
            )
            return
        }
        selected.set(id)
        emptyState?.visibility = View.GONE
        val existing = detailFragment
        if (existing == null) {
            val fragment = TagDetailFragment.newInstance(id)
            adopt(fragment)
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.detailContainer, fragment, DETAIL_FRAGMENT_TAG)
                .commitNow()
        } else {
            existing.showTag(id)
        }
        renderChrome()
        reveal(id)
    }

    fun listedTagIds(): List<Int> = listedIds()

    fun revealTag(id: Int) = reveal(id)

    /** Re-reads the pane's toolbar from the fragment's current state. */
    fun renderChrome() {
        val toolbar = detailToolbar ?: return
        val fragment = detailFragment
        val current = fragment?.tag
        toolbar.title = current?.title ?: ""
        markTitleAsHeading(toolbar)
        val menu: Menu = toolbar.menu
        for (id in TAG_ACTION_ITEMS) menu.findItem(id)?.isVisible = current != null
        if (current != null) fragment.prepareMenu(menu)
        // Nothing to step from until a tag is open: the empty pane's bar stays quiet.
        val stepping = selectedTagId != null
        menu.findItem(R.id.previousTagMenuItem)?.isVisible = stepping
        menu.findItem(R.id.nextTagMenuItem)?.isVisible = stepping
        setStepState(menu.findItem(R.id.previousTagMenuItem), canStep(-1))
        setStepState(menu.findItem(R.id.nextTagMenuItem), canStep(1))
    }

    private fun adopt(fragment: TagDetailFragment?) {
        fragment ?: return
        tagLoader?.let { fragment.tagLoader = it }
        fragment.onStateChanged = { renderChrome() }
    }

    private fun setStepState(
        item: MenuItem?,
        enabled: Boolean,
    ) {
        item ?: return
        item.isEnabled = enabled
        item.icon?.alpha = if (enabled) 255 else DISABLED_ICON_ALPHA
    }

    private fun canStep(delta: Int): Boolean {
        val ids = listedIds()
        val index = ids.indexOf(selectedTagId ?: return false)
        if (index < 0) return false
        val target = index + delta
        if (target in ids.indices) return true
        return delta > 0 && target == ids.size && hasMoreResults()
    }

    internal fun step(delta: Int) {
        val ids = listedIds()
        val index = ids.indexOf(selectedTagId ?: return)
        if (index < 0) return
        val target = index + delta
        when {
            target in ids.indices -> showTag(ids[target])
            // Walking off the end of a paged list asks for the next page instead of stopping.
            delta > 0 && hasMoreResults() -> fetchMore()
        }
    }

    /**
     * Hardware keyboards step the way the iPad's ⌘↑ / ⌘↓ do: Ctrl+↑ / Ctrl+↓ walk the list while
     * a tag is open beside it. Plain arrows stay with the focused list. Call from the host
     * activity's onKeyDown; returns true when the event was consumed.
     */
    fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (!hasDetailPane || selectedTagId == null || !event.isCtrlPressed || event.repeatCount > 0) return false
        val delta =
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> -1
                KeyEvent.KEYCODE_DPAD_DOWN -> 1
                else -> return false
            }
        if (!canStep(delta)) return true
        step(delta)
        return true
    }

    internal fun onMenuItem(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.previousTagMenuItem -> {
                step(-1)
                return true
            }

            R.id.nextTagMenuItem -> {
                step(1)
                return true
            }
        }
        detailFragment?.handleMenuItem(item)
        renderChrome()
        return true
    }

    private fun markTitleAsHeading(toolbar: MaterialToolbar) {
        val title = toolbar.title ?: return
        for (index in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(index)
            if (child is TextView && child.text == title) {
                ViewCompat.setAccessibilityHeading(child, true)
                return
            }
        }
    }

    companion object {
        internal const val DETAIL_FRAGMENT_TAG = "depollsoft.tagmaster.tagPaneDetail"
        private const val STATE_SELECTED_TAG_ID = "depollsoft.tagmaster.tagPaneSelection"
        private const val DISABLED_ICON_ALPHA = 97

        private val TAG_ACTION_ITEMS =
            intArrayOf(
                R.id.addFavoriteMenuItem,
                R.id.removeFavoriteMenuItem,
                R.id.addTeachableTagMenuItem,
                R.id.removeTeachableTagMenuItem,
                R.id.refreshMenuItem,
                R.id.shareMenuItem,
            )
    }
}
