package depollsoft.tagmaster

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.bindroid.utils.Function

/**
 * Recycling list of saved tag ids (favorites or teachable tags), keyed by tag id so reorders and
 * removals animate as moves instead of rebinding every row. Rows are the existing
 * [SavedTagItemView] subclasses, which load and render their own tag. The list follows the
 * Bindroid collection returned by [ids]; both in-place edits and replacement of the collection
 * (sign-in sync, tests) resubmit a snapshot.
 */
class SavedTagListAdapter(
    private val ids: () -> TrackableCollection<Int>,
    private val createRow: (Context) -> SavedTagItemView,
) : ListAdapter<Int, SavedTagListAdapter.Holder>(DIFF) {
    class Holder(
        val row: SavedTagItemView,
    ) : RecyclerView.ViewHolder(row)

    private val mainHandler = Handler(Looper.getMainLooper())

    // Bindroid trackers fire once per registration, so refresh re-registers after each update.
    private val tracker =
        object : Tracker {
            override fun update() {
                if (Looper.myLooper() == Looper.getMainLooper()) refresh() else mainHandler.post { refresh() }
            }
        }

    init {
        setHasStableIds(true)
        refresh()
    }

    private fun refresh() {
        val snapshot = Trackable.track(tracker, Function<List<Int>> { ids().toList() })
        submitList(snapshot)
    }

    override fun getItemId(position: Int): Long = getItem(position).toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val row = createRow(parent.context)
        row.layoutParams =
            RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return Holder(row)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int,
    ) {
        holder.row.bind(getItem(position))
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

    companion object {
        private val DIFF =
            object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(
                    oldItem: Int,
                    newItem: Int,
                ) = oldItem == newItem

                override fun areContentsTheSame(
                    oldItem: Int,
                    newItem: Int,
                ) = true
            }
    }
}
