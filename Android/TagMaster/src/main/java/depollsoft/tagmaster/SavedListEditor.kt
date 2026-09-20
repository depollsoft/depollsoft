package depollsoft.tagmaster

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Shared editing behavior for Home's favorites section, the teachable list and every user-defined
 * list.
 *
 * The label names the list in the spoken strings. A built-in list passes its string resource; a
 * user-defined one passes its current name, which [listLabel] follows when the user renames it.
 */
class SavedListEditor(
    private val activity: AppCompatActivity,
    private val model: ListModel,
    private val adapter: SavedTagListAdapter,
    private val list: RecyclerView,
    listLabel: CharSequence,
    savedState: Bundle?,
) {
    constructor(
        activity: AppCompatActivity,
        model: ListModel,
        adapter: SavedTagListAdapter,
        list: RecyclerView,
        @StringRes listLabel: Int,
        savedState: Bundle?,
    ) : this(activity, model, adapter, list, activity.getString(listLabel), savedState)

    /** The list's name, as the remove, drag and position announcements say it. */
    var listLabel: CharSequence = listLabel
        set(value) {
            if (field == value) return
            field = value
            updateVisibleRows()
        }

    var isEditing = savedState?.getBoolean(STATE_EDITING) == true && model.ids.isNotEmpty()
        private set
    private var drag: ListModel.Snapshot? = null
    private var confirmation: AlertDialog? = null
    private var active = true

    private val touchHelper =
        ItemTouchHelper(
            object : ItemTouchHelper.Callback() {
                override fun isLongPressDragEnabled() = false

                override fun isItemViewSwipeEnabled() = false

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ): Int =
                    if (isEditing && active && valid(viewHolder) && adapter.itemCount > 1) {
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
                    // These are positions within SavedTagListAdapter, never ConcatAdapter absolute positions.
                    val moved = adapter.previewMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                    // Refresh spoken positions after drop, not once per crossed row during pointer drag.
                    return moved
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
                    // Recovery animations can finish AFTER cancel, pause, Done or another drag. Never commit here.
                    updateVisibleRows()
                }
            },
        )

    init {
        adapter.bindEditing = ::bindRow
        adapter.sourceChanged = {
            val interrupted = drag != null
            cancelDrag()
            if (model.ids.isEmpty()) isEditing = false
            activity.invalidateOptionsMenu()
            list.post { updateVisibleRows() }
            if (interrupted) list.announceForAccessibility(activity.getString(R.string.saved_list_changed))
        }
        touchHelper.attachToRecyclerView(list)
    }

    private fun valid(holder: RecyclerView.ViewHolder): Boolean =
        holder is SavedTagListAdapter.Holder && holder.bindingAdapter === adapter &&
            holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
            holder.bindingAdapterPosition in adapter.currentList.indices &&
            adapter.currentList[holder.bindingAdapterPosition] == holder.row.tagId

    private fun bindRow(holder: SavedTagListAdapter.Holder) {
        val id = holder.row.tagId ?: return
        val position = adapter.currentList.indexOf(id)
        holder.row.setEditing(
            isEditing,
            position,
            adapter.itemCount,
            listLabel,
            remove = { if (valid(holder)) confirmRemove(id, holder.row.displayName) },
            move = { delta ->
                if (!isEditing || !active || !valid(holder) || drag != null) {
                    false
                } else {
                    model.move(id, model.ids.indexOf(id) + delta)
                }
            },
            startDrag = {
                if (isEditing && active && valid(holder) && adapter.itemCount > 1 && drag == null) {
                    val baseline = model.snapshot()
                    if (baseline.ids == adapter.currentList) {
                        drag = baseline
                        touchHelper.startDrag(holder)
                    }
                }
            },
        )
    }

    private fun updateVisibleRows() {
        for (index in 0 until list.childCount) {
            val holder = list.getChildViewHolder(list.getChildAt(index))
            if (holder is SavedTagListAdapter.Holder && holder.bindingAdapter === adapter) bindRow(holder)
        }
    }

    fun prepareMenu(menu: Menu) {
        menu.findItem(R.id.editSavedList)?.apply {
            setTitle(if (isEditing) R.string.saved_list_done else R.string.saved_list_edit)
            isEnabled = model.ids.isNotEmpty()
        }
    }

    fun selectMenu(item: MenuItem): Boolean {
        if (item.itemId != R.id.editSavedList) return false
        cancelDrag()
        confirmation?.dismiss()
        isEditing = !isEditing && model.ids.isNotEmpty()
        updateVisibleRows()
        activity.invalidateOptionsMenu()
        return true
    }

    private fun confirmRemove(
        id: Int,
        name: String,
    ) {
        if (!isEditing || !active || !model.contains(id)) return
        cancelDrag()
        confirmation?.dismiss()
        confirmation =
            MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.saved_list_remove_title, listLabel))
                .setMessage(activity.getString(R.string.saved_list_remove_message, name))
                .setNegativeButton(R.string.home_cancel, null)
                .setPositiveButton(R.string.saved_list_remove) { _, _ ->
                    // Source changes while the dialog is open cannot change the identity being removed.
                    if (active && isEditing && model.contains(id)) model.remove(id)
                }.create()
        confirmation?.setOnDismissListener { confirmation = null }
        confirmation?.show()
    }

    /** Called from Activity.dispatchTouchEvent AFTER dispatch, not the handle's CANCEL listener. */
    fun afterTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val baseline = drag ?: return
                drag = null
                if (active && isEditing) model.reorder(baseline, adapter.currentList)
                adapter.showSnapshot(model.ids.toList())
                updateVisibleRows()
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
            }
        }
    }

    fun cancelDrag() {
        if (drag == null) return
        drag = null // Invalidate before dispatch: clearView can run synchronously or much later.
        // Detaching alone leaves ItemTouchHelper's selected holder/autoscroll runnable alive.
        // End its actual gesture first, while its RecyclerView is still attached.
        val now = android.os.SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        try {
            list.dispatchTouchEvent(cancel)
        } finally {
            cancel.recycle()
        }
        touchHelper.attachToRecyclerView(null)
        adapter.showSnapshot(model.ids.toList())
        if (active) touchHelper.attachToRecyclerView(list)
        updateVisibleRows()
    }

    fun saveState(outState: Bundle) {
        outState.putBoolean(STATE_EDITING, isEditing)
    }

    fun resume() {
        active = true
        touchHelper.attachToRecyclerView(list)
    }

    fun pause() {
        active = false
        cancelDrag()
        touchHelper.attachToRecyclerView(null)
        confirmation?.dismiss()
    }

    fun destroy() {
        pause()
        adapter.dispose()
    }

    companion object {
        private const val STATE_EDITING = "savedListEditing"
    }
}
