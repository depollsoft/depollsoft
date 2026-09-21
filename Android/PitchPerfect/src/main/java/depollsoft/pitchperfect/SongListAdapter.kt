package depollsoft.pitchperfect

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.transaction

class SongListAdapter(
    songList: SongList,
) : RecyclerView.Adapter<SongListAdapter.SongHolder>() {
    /** The list being shown. Swapping it is how the set list selector changes the rows. */
    var songList: SongList = songList
        set(value) {
            if (field === value) return
            field = value
            dragging = false
            notifyDataSetChanged()
        }

    class SongHolder(
        val songView: SongListItemView,
    ) : RecyclerView.ViewHolder(songView)

    var dragging: Boolean = false
    var editing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    override fun getItemCount(): Int = songList.songs.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SongHolder {
        val view = SongListItemView(parent.context)
        view.layoutParams =
            RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
        return SongHolder(view)
    }

    override fun onBindViewHolder(
        holder: SongHolder,
        position: Int,
    ) {
        holder.songView.bind(songList.songs[position])
        holder.songView.setEditing(editing)
        holder.songView.setDragHandleTouchListener {
            if (editing) onStartDrag?.invoke(holder)
        }
    }

    fun moveSong(
        fromPosition: Int,
        toPosition: Int,
    ) {
        if (fromPosition == toPosition || fromPosition < 0 || toPosition < 0) return
        // Suppress per-step persistence; commitDrag stores and syncs once on drop.
        songList.isRestoring.set(true)
        try {
            songList.songs.transaction {
                val song = removeAt(fromPosition)
                add(toPosition, song)
            }
        } finally {
            songList.isRestoring.set(false)
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun commitDrag() {
        if (!dragging) return
        dragging = false
        songList.notifyOfChange()
    }
}
