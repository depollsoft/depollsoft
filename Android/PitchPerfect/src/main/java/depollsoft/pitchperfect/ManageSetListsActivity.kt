package depollsoft.pitchperfect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.track
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * "Set Lists": every list in order, with its song count, a drag handle and a row overflow.
 *
 * My Songs is pinned first, has no handle and cannot be deleted. A drop commits the new order
 * once, rewriting `order` on every custom list, rather than once per step. The rows re-render on
 * any change, including one arriving from another device.
 */
class ManageSetListsActivity : AppCompatActivity() {
    private val model get() = SongsModel.get()
    private lateinit var adapter: SetListRowAdapter
    private var rows: List<SongList> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.managesetlistsview)
        setTitle(R.string.ManageSetListsTitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rows = model.orderedLists
        adapter = SetListRowAdapter()
        val list = findViewById<RecyclerView>(R.id.setListManageList)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.divider_hairline)?.let { divider.setDrawable(it) }
        list.addItemDecoration(divider)

        val touchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.Callback() {
                    override fun getMovementFlags(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                    ): Int =
                        if (isCustomRow(viewHolder.bindingAdapterPosition)) {
                            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                        } else {
                            makeMovementFlags(0, 0)
                        }

                    override fun isLongPressDragEnabled(): Boolean = false

                    override fun canDropOver(
                        recyclerView: RecyclerView,
                        current: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean = isCustomRow(target.bindingAdapterPosition)

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean {
                        val from = viewHolder.bindingAdapterPosition
                        val to = target.bindingAdapterPosition
                        if (!isCustomRow(from) || !isCustomRow(to)) return false
                        val moved = rows.toMutableList()
                        moved.add(to, moved.removeAt(from))
                        rows = moved
                        adapter.notifyItemMoved(from, to)
                        dragged = true
                        return true
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
                        commitOrder()
                    }
                },
            )
        touchHelper.attachToRecyclerView(list)
        adapter.onStartDrag = { holder -> touchHelper.startDrag(holder) }

        findViewById<FloatingActionButton>(R.id.newSetListButton).setOnClickListener {
            SetListNameDialog.create(supportFragmentManager, NAME_REQUEST)
        }
        supportFragmentManager.setFragmentResultListener(NAME_REQUEST, this) { _, _ -> refresh() }

        track({ model.trackLists() }) {
            if (!isDestroyed) {
                runOnUiThread { if (!isDestroyed) refresh() }
                keepTracking
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean =
        if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    private var dragged = false

    private fun isCustomRow(position: Int): Boolean =
        position in rows.indices && rows[position].id != SongsModel.DEFAULT_ID

    /** Commits the dropped order once, as the contract requires. Visible for the screen tests. */
    internal fun commitOrder() {
        if (!dragged) return
        dragged = false
        model.reorderLists(rows.map { it.id })
    }

    /**
     * The drag callback's move, without a gesture: a Robolectric test cannot produce one, and the
     * order that gets committed is the thing worth asserting.
     */
    internal fun moveForTest(
        from: Int,
        to: Int,
    ) {
        if (!isCustomRow(from) || !isCustomRow(to)) return
        val moved = rows.toMutableList()
        moved.add(to, moved.removeAt(from))
        rows = moved
        adapter.notifyItemMoved(from, to)
        dragged = true
    }

    /** Whether the row overflow for [list] offers Delete. Visible for the screen tests. */
    internal fun rowMenuOffersDeleteForTest(list: SongList): Boolean {
        val popup = PopupMenu(this, findViewById(R.id.setListManageList))
        popup.menuInflater.inflate(R.menu.setlistrowmenu, popup.menu)
        return list.id != SongsModel.DEFAULT_ID && popup.menu.findItem(R.id.deleteSetListRowMenuItem) != null
    }

    /** Rebuilds the rows from the model. Visible for the screen tests. */
    internal fun refresh() {
        if (dragged) return
        rows = model.orderedLists
        adapter.notifyDataSetChanged()
    }

    /** Makes [list] current and returns to the Songs tab. Visible for the screen tests. */
    internal fun switchTo(list: SongList) {
        model.currentListId = list.id
        finish()
    }

    internal fun promptRename(list: SongList) {
        SetListNameDialog.rename(supportFragmentManager, list.id, NAME_REQUEST)
    }

    internal fun duplicate(list: SongList) {
        model.duplicateList(list.id)
        refresh()
    }

    internal fun confirmDelete(list: SongList) {
        if (list.id == SongsModel.DEFAULT_ID) return
        val count = list.songs.size
        val message =
            if (count == 0) {
                getString(R.string.SetListDeleteMessageEmpty)
            } else {
                getString(
                    R.string.SetListDeleteMessage,
                    resources.getQuantityString(R.plurals.SetListDeleteSongs, count, count),
                )
            }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.SetListDeleteTitle, model.displayName(list)))
            .setMessage(message)
            .setPositiveButton(R.string.SetListDelete) { _, _ -> delete(list) }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    internal fun delete(list: SongList) {
        model.deleteList(list.id)
        refresh()
    }

    private inner class SetListRowAdapter : RecyclerView.Adapter<RowHolder>() {
        var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

        override fun getItemCount(): Int = rows.size

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RowHolder =
            RowHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.setlistrowview, parent, false),
            )

        override fun onBindViewHolder(
            holder: RowHolder,
            position: Int,
        ) {
            val list = rows[position]
            val custom = list.id != SongsModel.DEFAULT_ID
            val current = list.id == model.currentListId
            val name = model.displayName(list)
            holder.itemView.tag = list.id
            holder.name.text = name
            holder.count.text = countLabel(list.songs.size)
            holder.currentDot.visibility = if (current) View.VISIBLE else View.INVISIBLE
            holder.itemView.contentDescription =
                if (current) getString(R.string.SetListCurrentDescription, name) else null
            holder.handle.visibility = if (custom) View.VISIBLE else View.INVISIBLE
            holder.handle.setOnTouchListener { _, event ->
                if (custom && event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag?.invoke(holder)
                    true
                } else {
                    false
                }
            }
            // A tap on a list switches to it, here as everywhere; Rename lives in the row menu.
            holder.itemView.setOnClickListener { switchTo(list) }
            holder.overflow.contentDescription = getString(R.string.SetListRowOverflow, name)
            holder.overflow.setOnClickListener { showRowMenu(it, list) }
        }
    }

    private fun showRowMenu(
        anchor: View,
        list: SongList,
    ) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.setlistrowmenu, popup.menu)
        popup.menu.findItem(R.id.deleteSetListRowMenuItem).isVisible =
            list.id != SongsModel.DEFAULT_ID
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.renameSetListRowMenuItem -> promptRename(list)
                R.id.duplicateSetListRowMenuItem -> duplicate(list)
                R.id.deleteSetListRowMenuItem -> confirmDelete(list)
            }
            true
        }
        popup.show()
    }

    private fun countLabel(count: Int): String =
        if (count == 0) {
            getString(R.string.NoSongsCount)
        } else {
            resources.getQuantityString(R.plurals.SongCount, count, count)
        }

    private class RowHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.setListRowName)
        val count: TextView = view.findViewById(R.id.setListRowCount)
        val overflow: ImageButton = view.findViewById(R.id.setListRowOverflow)
        val handle: ImageView = view.findViewById(R.id.setListRowDragHandle)
        val currentDot: View = view.findViewById(R.id.setListRowCurrentDot)
    }

    companion object {
        private const val NAME_REQUEST = "depollsoft.pitchperfect.manageSetLists.name"
    }
}
