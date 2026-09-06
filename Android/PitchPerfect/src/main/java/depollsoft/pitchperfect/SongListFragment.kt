package depollsoft.pitchperfect

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.TrackableField
import com.bindroid.trackable.track
import com.bindroid.utils.bindTo
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SongListFragment : Fragment() {
    val model: SongsModel by TrackableField(SongsModel.get())
    var editing: Boolean by TrackableField(false)
    private var fab: FloatingActionButton? = null
    private var adapter: SongListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.songlistview, container, false)

        fab = rootView.findViewById(R.id.addSongButton)

        val recycler = rootView.findViewById<RecyclerView>(R.id.songListView)
        val songAdapter = SongListAdapter(model.defaultSongList)
        songAdapter.also { adapter = it }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = songAdapter
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_hairline)?.let { divider.setDrawable(it) }
        recycler.addItemDecoration(divider)

        val touchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.Callback() {
                    override fun getMovementFlags(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                    ): Int =
                        if (songAdapter.editing) {
                            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                        } else {
                            makeMovementFlags(0, 0)
                        }

                    override fun isLongPressDragEnabled(): Boolean = false

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean {
                        songAdapter.moveSong(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                        return true
                    }

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int,
                    ) = Unit

                    override fun onSelectedChanged(
                        viewHolder: RecyclerView.ViewHolder?,
                        actionState: Int,
                    ) {
                        super.onSelectedChanged(viewHolder, actionState)
                        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                            songAdapter.dragging = true
                        }
                    }

                    override fun clearView(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                    ) {
                        super.clearView(recyclerView, viewHolder)
                        songAdapter.commitDrag()
                    }
                },
            )
        touchHelper.attachToRecyclerView(recycler)
        songAdapter.onStartDrag = { holder -> touchHelper.startDrag(holder) }

        rootView.bindTo(
            R.id.sorryText,
            "Visibility",
            { model.defaultSongList.songs[0] },
            BoolConverter.get(true, true),
        )

        fab = rootView.findViewById(R.id.addSongButton)
        fab?.setOnClickListener(
            OnClickListener {
                val i = Intent(this@SongListFragment.context, AddSongActivity::class.java)
                this@SongListFragment.startActivityForResult(i, 1)
            },
        )
        fab?.show()

        return rootView
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val songAdapter = adapter ?: return

        // onCreateView runs before Fragment.view is assigned. Subscribing there
        // caused the one-shot Bindroid tracker to stop before Firestore could
        // deliver its first change.
        track({ model.defaultSongList.songs.track() }) {
            if (this@SongListFragment.view === view && adapter === songAdapter) {
                if (!songAdapter.dragging) {
                    activity?.runOnUiThread {
                        if (this@SongListFragment.view === view &&
                            adapter === songAdapter &&
                            !songAdapter.dragging
                        ) {
                            songAdapter.notifyDataSetChanged()
                        }
                    }
                }
                keepTracking
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        fab = null
        super.onDestroyView()
    }

    fun isEditingSongs(): Boolean = editing

    fun toggleEditingSongs() {
        editing = !editing
        adapter?.editing = editing
        activity?.invalidateOptionsMenu()
    }

    fun sortSongs() {
        model.defaultSongList.sortSongs()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        PitchPerfectActivity.handlingResult = true
    }

    private fun stopPlaying() {
        for (song in this.model.defaultSongList.songs) {
            song.stop()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPlaying()
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        stopPlaying()
        if (isVisibleToUser) {
            fab?.show()
        } else {
            fab?.hide()
        }
    }
}
