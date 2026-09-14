package depollsoft.tagmaster

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.bindroid.utils.Function

/**
 * One synchronous owner for both source snapshots and drag previews. An AsyncListDiffer cannot
 * race ItemTouchHelper's notifyItemMoved calls. Stable IDs belong only to this adapter; Home's
 * ConcatAdapter deliberately retains its default NO_STABLE_IDS for the static header/footer.
 */
class SavedTagListAdapter(
    private val ids: () -> TrackableCollection<Int>,
    private val createRow: (Context) -> SavedTagItemView,
) : RecyclerView.Adapter<SavedTagListAdapter.Holder>() {
    class Holder(
        val row: SavedTagItemView,
    ) : RecyclerView.ViewHolder(row)

    private val items = mutableListOf<Int>()
    val currentList: List<Int> get() = items.toList()
    internal var bindEditing: ((Holder) -> Unit)? = null
    internal var sourceChanged: (() -> Unit)? = null
    private var recycler: RecyclerView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var disposed = false

    // Bindroid registrations are one-shot. Re-register even when only the collection was replaced.
    private val tracker =
        object : Tracker {
            override fun update() {
                if (disposed) return
                if (Looper.myLooper() == Looper.getMainLooper() && recycler?.isComputingLayout != true) {
                    refresh()
                } else {
                    mainHandler.post { if (!disposed) refresh() }
                }
            }
        }

    init {
        setHasStableIds(true)
        refresh()
    }

    private fun refresh() {
        if (recycler?.isComputingLayout == true) {
            mainHandler.post { if (!disposed) refresh() }
            return
        }
        val snapshot = Trackable.track(tracker, Function<List<Int>> { ids().toList() })
        sourceChanged?.invoke()
        showSnapshot(snapshot)
    }

    internal fun showSnapshot(snapshot: List<Int>) {
        val old = items.toList()
        if (old == snapshot) return
        val diff =
            DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize() = old.size

                    override fun getNewListSize() = snapshot.size

                    override fun areItemsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int,
                    ) = old[oldItemPosition] == snapshot[newItemPosition]

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int,
                    ) = true
                },
            )
        items.clear()
        items.addAll(snapshot)
        diff.dispatchUpdatesTo(this)
    }

    internal fun previewMove(
        from: Int,
        to: Int,
    ): Boolean {
        if (from !in items.indices || to !in items.indices || from == to) return false
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
        return true
    }

    internal fun dispose() {
        disposed = true
        mainHandler.removeCallbacksAndMessages(null)
        sourceChanged = null
        bindEditing = null
        recycler = null // A one-shot Bindroid registration may outlive the destroyed screen.
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recycler = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recycler = null
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = items[position].toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val row = createRow(parent.context)
        row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return Holder(row)
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        super.onViewAttachedToWindow(holder)
        // RecyclerView may reuse an offscreen cached holder without rebinding its unchanged tag ID.
        // Editing state and position actions still need to reflect the current screen state.
        bindEditing?.invoke(holder)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        holder.row.bind(items[position])
        bindEditing?.invoke(holder)
    }

    /** Hairline between saved rows only (not around headers or footers sharing the list). */
    class RowDivider(
        context: Context,
    ) : RecyclerView.ItemDecoration() {
        private val divider = AppCompatResources.getDrawable(context, R.drawable.list_divider)!!

        override fun onDraw(
            canvas: Canvas,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                val holder = parent.getChildViewHolder(child) as? Holder ?: continue
                if (holder.bindingAdapterPosition <= 0) continue
                val top = child.top + child.translationY.toInt()
                divider.setBounds(
                    parent.paddingLeft,
                    top,
                    parent.width - parent.paddingRight,
                    top + divider.intrinsicHeight,
                )
                divider.alpha = (child.alpha * 255).toInt()
                divider.draw(canvas)
            }
        }
    }
}
