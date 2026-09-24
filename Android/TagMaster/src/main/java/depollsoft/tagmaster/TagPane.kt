package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import bolts.Task
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

/** Whether a window is wide enough to show a tag beside its list (the w720dp-h480dp layout). */
fun hasTwoPanes(
    widthDp: Int,
    heightDp: Int,
): Boolean = widthDp >= 720 && heightDp >= 480

/**
 * The shared half of [TagPaneHost]: which tag the pane shows, stepping to the previous or next
 * tag of the list, and the one [TagDetailState] every selection reuses so the open page survives
 * a tag change.
 */
@Stable
class TagPaneState(
    private val context: Context,
    override val hasDetailPane: Boolean,
    private val listedIds: () -> List<Int>,
    var reveal: (Int) -> Unit = {},
    private val hasMoreResults: () -> Boolean = { false },
    private val fetchMore: () -> Unit = {},
) : TagPaneHost {
    override var selectedTagId: Int? by mutableStateOf(null)

    /** The pane's detail; null until a tag is chosen. */
    var detail: TagDetailState? by mutableStateOf(null)
        private set

    /** Test seam: the loader handed to the detail instead of the live tag cache. */
    var tagLoader: ((Int, Boolean) -> Task<Tag>)? = null
        set(value) {
            field = value
            if (value != null) detail?.loader = value
        }

    override fun showTag(id: Int) {
        if (!hasDetailPane) {
            context.startActivity(
                Intent(context, TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, id)
                    .addFlags(if (context is android.app.Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK),
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

    override fun listedTagIds(): List<Int> = listedIds()

    override fun revealTag(id: Int) = reveal(id)

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
