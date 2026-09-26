package depollsoft.tagmaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import bolts.Task
import depollsoft.tagmaster.barbershop.Tag

/**
 * A tag list screen that can open a tag beside itself in its [tagPane], which it creates in
 * onCreate. On phones (and any window narrower than the two-pane layout) the pane has no detail
 * and rows keep opening [TagDetailActivity] full-screen. The pane's selection and page are saved
 * with the activity, and it stops with it.
 */
abstract class TagPaneActivity : AppCompatActivity() {
    internal lateinit var tagPane: TagPaneState

    private val hasTagPane get() = ::tagPane.isInitialized

    val hasDetailPane: Boolean get() = tagPane.hasDetailPane

    val selectedTagId: Int? get() = tagPane.selectedTagId

    /** Opens [id] in the detail pane, keeping the page (Summary/Details/Tracks/Videos) in view. */
    fun showTag(id: Int) = tagPane.showTag(id)

    /**
     * Ctrl+Up / Ctrl+Down step the pane's tag before the view tree sees the keys: Compose moves
     * focus on an arrow key while dispatching it, so onKeyDown would never be reached.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (event.action == KeyEvent.ACTION_DOWN && hasTagPane && tagPane.onKeyDown(event.keyCode, event)) ||
            super.dispatchKeyEvent(event)

    override fun onSaveInstanceState(outState: Bundle) {
        if (hasTagPane) tagPane.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (hasTagPane) tagPane.stop()
        super.onDestroy()
    }
}

/** Whether a window is wide enough to show a tag beside its list (the w720dp-h480dp layout). */
fun hasTwoPanes(
    widthDp: Int,
    heightDp: Int,
): Boolean = widthDp >= 720 && heightDp >= 480

/**
 * A [TagPaneActivity]'s pane: which tag it shows, stepping to the previous or next tag of the
 * list, and the one [TagDetailState] every selection reuses so the open page survives a tag change.
 */
@Stable
class TagPaneState(
    private val context: Context,
    val hasDetailPane: Boolean,
    private val listedIds: () -> List<Int>,
    var reveal: (Int) -> Unit = {},
    private val hasMoreResults: () -> Boolean = { false },
    private val fetchMore: () -> Unit = {},
) {
    var selectedTagId: Int? by mutableStateOf(null)

    /** The pane's detail; null until a tag is chosen. */
    var detail: TagDetailState? by mutableStateOf(null)
        private set

    /** Test seam: the loader handed to the detail instead of the live tag cache. */
    var tagLoader: ((Int, Boolean) -> Task<Tag>)? = null
        set(value) {
            field = value
            if (value != null) detail?.loader = value
        }

    /** Opens [id] in the detail pane, keeping the page (Summary/Details/Tracks/Videos) in view. */
    fun showTag(id: Int) {
        if (!hasDetailPane) {
            context.startActivity(
                Intent(context, TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, id)
                    .addFlags(if (context is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        selectedTagId = id
        val existing = detail
        if (existing == null) {
            detail =
                TagDetailState(id, context).also { state ->
                    tagLoader?.let { state.loader = it }
                    state.start()
                }
        } else {
            existing.showTag(id)
        }
        reveal(id)
    }

    fun canStep(delta: Int): Boolean {
        val ids = listedIds()
        val index = ids.indexOf(selectedTagId ?: return false)
        if (index < 0) return false
        val target = index + delta
        if (target in ids.indices) return true
        return delta > 0 && target == ids.size && hasMoreResults()
    }

    fun step(delta: Int) {
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
     * a tag is open beside it. Plain arrows stay with the focused list. Returns true when the
     * event was consumed.
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

    fun save(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAG_ID, selectedTagId ?: 0)
        detail?.let { outState.putInt(STATE_PAGE, it.page) }
    }

    fun restore(savedState: Bundle?) {
        val restored = savedState?.getInt(STATE_SELECTED_TAG_ID, 0) ?: 0
        if (restored > 0 && hasDetailPane) {
            showTag(restored)
            detail?.page = savedState?.getInt(STATE_PAGE, 0) ?: 0
            detail?.revealPending = false
        }
    }

    fun stop() {
        detail?.stop()
    }

    companion object {
        private const val STATE_SELECTED_TAG_ID = "depollsoft.tagmaster.tagPaneSelection"
        private const val STATE_PAGE = "depollsoft.tagmaster.tagPanePage"
    }
}
