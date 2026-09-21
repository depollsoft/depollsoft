package depollsoft.pitchperfect

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.TrackableField
import com.bindroid.trackable.track
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class SongListFragment : Fragment() {
    val model: SongsModel by TrackableField(SongsModel.get())
    var editing: Boolean by TrackableField(false)
    private var fab: FloatingActionButton? = null
    private var adapter: SongListAdapter? = null
    private var selector: SetListSelectorView? = null
    private var sorryText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        parentFragmentManager.setFragmentResultListener(NAME_REQUEST, this) { _, result ->
            val listId = result.getString(SetListNameDialog.RESULT_LIST_ID) ?: return@setFragmentResultListener
            if (result.getBoolean(SetListNameDialog.RESULT_CREATED)) {
                // Creating switches the tab to the new (empty) list and leaves edit mode.
                if (editing) toggleEditingSongs()
                switchToList(listId)
            } else {
                renderLists()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.songlistview, container, false)

        fab = rootView.findViewById(R.id.addSongButton)
        sorryText = rootView.findViewById(R.id.sorryText)

        val recycler = rootView.findViewById<RecyclerView>(R.id.songListView)
        val songAdapter = SongListAdapter(model.currentList)
        songAdapter.also { adapter = it }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = songAdapter
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(requireContext(), R.drawable.divider_hairline)?.let { divider.setDrawable(it) }
        recycler.addItemDecoration(divider)

        selector =
            rootView.findViewById<SetListSelectorView>(R.id.setListSelector)?.also { part ->
                part.onSelect = { listId -> switchToList(listId) }
                part.onCreate = { promptNewSetList() }
                part.onLongPress = { anchor, listId -> showListMenu(anchor, listId) }
                part.render()
            }

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

        fab = rootView.findViewById(R.id.addSongButton)
        fab?.setOnClickListener(
            OnClickListener {
                val i = Intent(this@SongListFragment.context, AddSongActivity::class.java)
                i.putExtra(AddSongActivity.LIST_EXTRA, model.currentListId)
                this@SongListFragment.startActivityForResult(i, 1)
            },
        )
        fab?.show()

        applyEmptyStateCopy()
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
        track({ model.currentList.songs.track() }) {
            if (this@SongListFragment.view === view && adapter === songAdapter) {
                if (!songAdapter.dragging) {
                    activity?.runOnUiThread {
                        if (this@SongListFragment.view === view &&
                            adapter === songAdapter &&
                            !songAdapter.dragging
                        ) {
                            songAdapter.notifyDataSetChanged()
                            applyEmptyStateCopy()
                        }
                    }
                }
                keepTracking
            }
        }

        // The set of lists, their names, their orders and the current list can all change under
        // the tab — a rename on another device, a list deleted, a position tapped here.
        track({ model.trackLists() }) {
            if (this@SongListFragment.view === view) {
                activity?.runOnUiThread {
                    if (this@SongListFragment.view === view) renderLists()
                }
                keepTracking
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        fab = null
        selector = null
        sorryText = null
        super.onDestroyView()
    }

    // MARK: - The current list

    val currentListId: String
        get() = model.currentListId

    private fun switchToList(listId: String) {
        if (model.currentListId == listId) return
        stopPlaying()
        model.currentListId = listId
        renderLists()
    }

    private fun renderLists() {
        adapter?.songList = model.currentList
        selector?.render()
        applyEmptyStateCopy()
        activity?.invalidateOptionsMenu()
    }

    /**
     * The empty state is set here, not through a binding: the current list is a plain
     * preference, so a one-time binding on its songs would never notice a switch to another list.
     */
    private fun applyEmptyStateCopy() {
        val list = model.currentList
        sorryText?.setText(
            if (list.id == SongsModel.DEFAULT_ID) {
                R.string.NoSongsInList
            } else {
                R.string.NoSongsInSetList
            },
        )
        sorryText?.visibility = if (list.songs.isEmpty()) View.VISIBLE else View.GONE
    }

    // MARK: - Edit mode

    fun isEditingSongs(): Boolean = editing

    fun toggleEditingSongs() {
        editing = !editing
        adapter?.editing = editing
        activity?.invalidateOptionsMenu()
    }

    fun sortSongs() {
        model.currentList.sortSongs()
    }

    /** "Add songs from another set list…" is disabled when nothing is addable. */
    fun canAddSongsFromOtherLists(): Boolean = model.hasAddableSongs(model.currentListId)

    /** The default list is never deletable. */
    fun canDeleteCurrentList(): Boolean = model.currentListId != SongsModel.DEFAULT_ID

    fun promptNewSetList() {
        SetListNameDialog.create(parentFragmentManager, NAME_REQUEST)
    }

    fun promptRenameSetList(listId: String = model.currentListId) {
        SetListNameDialog.rename(parentFragmentManager, listId, NAME_REQUEST)
    }

    /** No prompt: the copy is made, shown, and announced; edit mode stays on to prune it. */
    fun duplicateCurrentList() = duplicateList(model.currentListId)

    fun duplicateList(listId: String) {
        val newId = model.duplicateList(listId) ?: return
        stopPlaying()
        model.currentListId = newId
        renderLists()
        val root = view ?: return
        Snackbar
            .make(
                root,
                getString(R.string.SetListDuplicatedAnnouncement, model.displayName(newId)),
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    fun confirmDeleteCurrentList() = confirmDeleteList(model.currentListId)

    /**
     * Long-pressing a selector position offers the list's own actions — rename, duplicate, delete
     * and the manage screen — without first entering edit mode. Visible for the screen tests.
     */
    internal fun showListMenu(
        anchor: View,
        listId: String,
    ) {
        val popup = PopupMenu(requireContext(), anchor)
        // The menu names the list it acts on: a long press on one position while another is
        // current would otherwise look exactly like the current list's menu.
        popup.menu.add(Menu.NONE, HEADER_ITEM_ID, Menu.NONE, model.displayName(listId)).isEnabled = false
        popup.menuInflater.inflate(R.menu.setlistselectormenu, popup.menu)
        popup.menu.findItem(R.id.deleteSetListMenuItem).isVisible = listId != SongsModel.DEFAULT_ID
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.renameSetListMenuItem -> promptRenameSetList(listId)
                R.id.duplicateSetListMenuItem -> duplicateList(listId)
                R.id.deleteSetListMenuItem -> confirmDeleteList(listId)
                R.id.manageSetListsMenuItem -> openManageSetLists()
            }
            true
        }
        popup.show()
    }

    fun confirmDeleteList(listId: String) {
        val list = model.songLists[listId] ?: return
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.SetListDeleteTitle, model.displayName(list)))
            .setMessage(message)
            .setPositiveButton(R.string.SetListDelete) { _, _ -> deleteList(listId) }
            .setNegativeButton(R.string.Cancel, null)
            .show()
    }

    /** Exposed for the screen tests, which cannot press a platform dialog's button. */
    internal fun deleteCurrentList() = deleteList(model.currentListId)

    internal fun deleteList(listId: String) {
        val list = model.songLists[listId] ?: return
        if (listId == SongsModel.DEFAULT_ID) return
        val wasCurrent = listId == model.currentListId
        if (wasCurrent) stopPlaying()
        val name = model.displayName(list)
        model.deleteList(listId)
        if (wasCurrent && editing) toggleEditingSongs()
        renderLists()
        // The only irreversible action in the feature gets the same undo Duplicate's Snackbar
        // slot already occupies; the list comes back with its songs and its id.
        val root = view ?: return
        Snackbar
            .make(root, getString(R.string.SetListDeletedAnnouncement, name), Snackbar.LENGTH_LONG)
            .setAction(R.string.Undo) {
                model.restoreList(list)
                if (wasCurrent) model.currentListId = listId
                renderLists()
            }.show()
    }

    fun openAddSongsFromList() {
        val intent = Intent(requireContext(), AddSongsFromListActivity::class.java)
        intent.putExtra(AddSongsFromListActivity.LIST_EXTRA, model.currentListId)
        startActivity(intent)
    }

    fun openManageSetLists() {
        startActivity(Intent(requireContext(), ManageSetListsActivity::class.java))
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
        for (song in this.model.currentList.songs) {
            song.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        // A list may have been renamed, created or deleted on the screens this tab opens.
        renderLists()
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

    companion object {
        private const val NAME_REQUEST = "depollsoft.pitchperfect.songs.setListName"
        internal const val HEADER_ITEM_ID = 1
    }
}
