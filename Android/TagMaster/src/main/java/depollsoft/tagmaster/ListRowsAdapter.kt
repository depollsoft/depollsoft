package depollsoft.tagmaster

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.Tracker
import com.bindroid.utils.Function

/**
 * The user's own lists on Home, as their own section of the home list.
 *
 * They were plain views inside the header until they had to join Home's edit mode: reordering by
 * drag needs real adapter positions, so the lists are an adapter of their own sitting between the
 * header (the actions, the Lists heading and Teachable Tags) and the static row that closes the
 * group. In edit mode each row grows the same leading remove circle and trailing drag handle as a
 * saved tag row, and stops navigating.
 *
 * One Bindroid registration covers the whole section: the keys, every name and every tag count are
 * read inside a single [Trackable.track], so a rename, a created list, a tag added on another
 * device and a reorder all arrive as the same "rebuild the rows" signal.
 */
class ListRowsAdapter(
    private val activity: FragmentActivity,
    private val list: RecyclerView,
    private val onSourceChanged: () -> Unit,
) : RecyclerView.Adapter<ListRowsAdapter.Holder>() {
    /** One list, as the row shows it: the name and the tag count it is currently bound to. */
    data class Row(
        val key: String,
        val name: String,
        val detail: String,
    )

    class Holder(
        val row: View,
    ) : RecyclerView.ViewHolder(row) {
        val icon: AppCompatImageView = row.findViewById(R.id.listRowIcon)
        val name: TextView = row.findViewById(R.id.listRowName)
        val detail: TextView = row.findViewById(R.id.listRowDetail)
        val remove: AppCompatImageButton = row.findViewById(R.id.listRowRemove)
        val handle: AppCompatImageButton = row.findViewById(R.id.listRowDragHandle)

        /** The list this holder is showing; the identity every callback re-checks. */
        var key: String = ""

        /** The custom accessibility actions currently on this row, so a rebind can retire them. */
        val actionIds = mutableListOf<Int>()
    }

    private val items = mutableListOf<Row>()
    private val trackedLists = mutableListOf<ListModel>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val density = activity.resources.displayMetrics.density
    private var recycler: RecyclerView? = null
    private var disposed = false
    private var active = true
    private var drag: List<String>? = null

    /** The keys currently on screen, in the order they are shown. */
    val keys: List<String> get() = items.map { it.key }

    var isEditing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateVisibleRows()
        }

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

    private val touchHelper =
        ItemTouchHelper(
            object : ItemTouchHelper.Callback() {
                override fun isLongPressDragEnabled() = false

                override fun isItemViewSwipeEnabled() = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ): Int =
                    if (isEditing && active && valid(viewHolder) && itemCount > 1) {
                        makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                    } else {
                        0
                    }

                override fun canDropOver(
                    recyclerView: RecyclerView,
                    current: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ) = valid(current) && valid(target)

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    if (drag == null || !isEditing || !valid(viewHolder) || !valid(target)) return false
                    // Positions within this adapter, never ConcatAdapter absolute positions.
                    return previewMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int,
                ) = Unit

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ) {
                    super.clearView(recyclerView, viewHolder)
                    // Recovery animations can finish after cancel, pause or Done. Never commit here.
                    updateVisibleRows()
                }
            },
        )

    init {
        setHasStableIds(true)
        refresh()
        touchHelper.attachToRecyclerView(list)
    }

    private fun currentRows(): List<Row> {
        // The models whose counts these rows read are kept for as long as the rows are on screen,
        // so the tracked collections are exactly the ones the next read registers on.
        trackedLists.clear()
        return TagLists.customKeys.toList().map { key ->
            trackedLists.add(ListModel(key))
            Row(key, TagLists.name(key), listCountText(activity, key))
        }
    }

    private fun refresh() {
        if (recycler?.isComputingLayout == true) {
            mainHandler.post { if (!disposed) refresh() }
            return
        }
        val snapshot = Trackable.track(tracker, Function<List<Row>> { currentRows() })
        onSourceChanged()
        showSnapshot(snapshot)
    }

    private fun showSnapshot(snapshot: List<Row>) {
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
                    ) = old[oldItemPosition].key == snapshot[newItemPosition].key

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int,
                    ) = old[oldItemPosition] == snapshot[newItemPosition]
                },
            )
        items.clear()
        items.addAll(snapshot)
        diff.dispatchUpdatesTo(this)
        // A moved row is not rebound, so its spoken position and its Move up/down actions are
        // refreshed once the change has been dispatched.
        list.post { if (!disposed) updateVisibleRows() }
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

    private fun valid(holder: RecyclerView.ViewHolder): Boolean =
        holder is Holder && holder.bindingAdapter === this &&
            holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
            holder.bindingAdapterPosition in items.indices &&
            items[holder.bindingAdapterPosition].key == holder.key

    override fun getItemCount() = items.size

    override fun getItemId(position: Int) = items[position].key.hashCode().toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val row = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
        row.layoutParams =
            RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return Holder(row)
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        super.onViewAttachedToWindow(holder)
        // A cached holder can come back without rebinding; its editing state still has to be current.
        bindRow(holder)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        val row = items[position]
        holder.key = row.key
        holder.icon.setImageResource(listIconRes(row.key))
        holder.name.text = row.name
        holder.detail.text = row.detail
        bindRow(holder)
    }

    private fun bindRow(holder: Holder) {
        val position = items.indexOfFirst { it.key == holder.key }
        if (position < 0) return
        val row = items[position]
        val editing = isEditing
        val gutter = ((if (editing) 4 else 16) * density).toInt()
        holder.row.setPaddingRelative(gutter, holder.row.paddingTop, gutter, holder.row.paddingBottom)
        holder.remove.visibility = if (editing) View.VISIBLE else View.GONE
        holder.handle.visibility = if (editing) View.VISIBLE else View.GONE
        holder.handle.isEnabled = items.size > 1
        holder.handle.alpha = if (items.size > 1) 1f else 0.38f
        holder.row.setOnClickListener {
            if (!isEditing) activity.startActivity(TagListActivity.intent(activity, holder.key))
        }
        // setOnClickListener makes a view clickable; an editing row navigates nowhere.
        holder.row.isClickable = !editing
        holder.remove.contentDescription = activity.getString(R.string.list_row_remove_named, row.name)
        holder.remove.setOnClickListener {
            if (isEditing && valid(holder)) ListRowMenu.confirmDelete(activity, holder.key)
        }
        holder.handle.contentDescription =
            activity.getString(R.string.list_row_drag_named, row.name, position + 1, items.size)
        holder.handle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && isEditing && holder.handle.isEnabled) startDrag(holder)
            // Interception sends CANCEL to this child. It is NOT cancellation of the drag.
            false
        }
        holder.handle.setOnKeyListener { _, key, event ->
            if (isEditing && event.action == KeyEvent.ACTION_DOWN && event.isAltPressed) {
                when (key) {
                    KeyEvent.KEYCODE_DPAD_UP -> move(holder, -1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> move(holder, 1)
                    else -> false
                }
            } else {
                false
            }
        }
        holder.row.contentDescription =
            if (editing) {
                activity.getString(R.string.list_row_position, row.name, row.detail, position + 1, items.size)
            } else {
                null
            }
        // Long press is a shortcut to the same actions; in edit mode the controls are on the row.
        ListRowMenu.install(activity, holder.row, holder.key, holder.actionIds, allowLongPress = !editing)
    }

    private fun move(
        holder: Holder,
        delta: Int,
    ): Boolean {
        if (!isEditing || !active || !valid(holder) || drag != null) return false
        return TagLists.move(holder.key, TagLists.customKeys.indexOf(holder.key) + delta)
    }

    private fun startDrag(holder: Holder) {
        if (!isEditing || !active || !valid(holder) || items.size < 2 || drag != null) return
        val baseline = TagLists.customKeys.toList()
        if (baseline != keys) return
        drag = baseline
        touchHelper.startDrag(holder)
    }

    private fun updateVisibleRows() {
        for (index in 0 until list.childCount) {
            val holder = list.getChildViewHolder(list.getChildAt(index))
            if (holder is Holder && holder.bindingAdapter === this) bindRow(holder)
        }
    }

    /** Called from Activity.dispatchTouchEvent AFTER dispatch, exactly as [SavedListEditor] does. */
    fun afterTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val baseline = drag ?: return
                drag = null
                // A list deleted or created mid-drag changes the set; then this order is not one.
                if (active && isEditing && baseline.toSet() == keys.toSet()) TagLists.reorder(keys)
                showSnapshot(currentRows())
                updateVisibleRows()
            }

            MotionEvent.ACTION_CANCEL -> cancelDrag()
        }
    }

    fun cancelDrag() {
        if (drag == null) return
        drag = null // Invalidate before dispatch: clearView can run synchronously or much later.
        // Detaching alone leaves ItemTouchHelper's selected holder/autoscroll runnable alive.
        val now = android.os.SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        try {
            list.dispatchTouchEvent(cancel)
        } finally {
            cancel.recycle()
        }
        touchHelper.attachToRecyclerView(null)
        showSnapshot(currentRows())
        if (active) touchHelper.attachToRecyclerView(list)
        updateVisibleRows()
    }

    fun resume() {
        active = true
        touchHelper.attachToRecyclerView(list)
    }

    fun pause() {
        active = false
        cancelDrag()
        touchHelper.attachToRecyclerView(null)
    }

    fun dispose() {
        pause()
        disposed = true
        mainHandler.removeCallbacksAndMessages(null)
        recycler = null // A one-shot Bindroid registration may outlive the destroyed screen.
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recycler = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recycler = null
    }
}
