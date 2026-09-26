package depollsoft.pitchperfect

import android.os.Bundle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import depollsoft.pitchperfect.lib.PitchedSong

/** A request for the set list name dialog: a new list, or a new name for [listId]. */
data class NameRequest(
    val listId: String? = null,
)

/**
 * Something the Songs tab announces in a snackbar, with an optional action. [id] tells two
 * identical announcements apart.
 */
data class Announcement(
    val text: String,
    val long: Boolean,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val id: Long = System.nanoTime(),
)

/**
 * Everything the Songs tab does, apart from how it looks: which list shows, edit mode, the
 * prompts it has open and what it announces. The action bar and the tab share it.
 */
@Stable
class SongListState(
    val model: SongsModel,
) : SetListPrompts {
    /** Edit mode: songs show their edit buttons and drag handles, and the list actions appear. */
    var editing by mutableStateOf(false)
        private set

    override var nameRequest by mutableStateOf<NameRequest?>(null)
    override var pendingDelete by mutableStateOf<String?>(null)
    var menuFor by mutableStateOf<String?>(null)
    var announcement by mutableStateOf<Announcement?>(null)

    /** Each list keeps its own place; one list state would otherwise carry one scroll across all. */
    private val scrollStates = mutableMapOf<String, LazyListState>()

    fun scrollStateFor(listId: String): LazyListState = scrollStates.getOrPut(listId) { LazyListState() }

    /** Each list's place, for [restoreScrollPositions] after recreation, as a RecyclerView kept it. */
    fun saveScrollPositions(): Bundle =
        Bundle().apply {
            for ((listId, scroll) in scrollStates) {
                putIntArray(listId, intArrayOf(scroll.firstVisibleItemIndex, scroll.firstVisibleItemScrollOffset))
            }
        }

    fun restoreScrollPositions(saved: Bundle) {
        for (listId in saved.keySet()) {
            val (index, offset) = saved.getIntArray(listId)?.takeIf { it.size == 2 } ?: continue
            scrollStates[listId] = LazyListState(index, offset)
        }
    }

    val currentList: SongList get() = model.currentList

    fun toggleEditing() {
        editing = !editing
    }

    /** Sorts the current list; the list stays where it is on screen while the rows slide. */
    fun sortSongs() {
        model.currentList.sortSongs()
    }

    /** "Add songs from another set list…" is disabled when nothing is addable. */
    fun canAddSongsFromOtherLists(): Boolean = model.hasAddableSongs(model.currentListId)

    fun switchToList(listId: String) {
        if (model.currentListId == listId) return
        stopPlaying()
        model.currentListId = listId
    }

    fun promptNewList() {
        nameRequest = NameRequest()
    }

    fun promptRename(listId: String = model.currentListId) {
        nameRequest = NameRequest(listId)
    }

    /** The name dialog finished: a created list becomes the current one, out of edit mode. */
    fun nameChosen(
        listId: String,
        created: Boolean,
    ) {
        nameRequest = null
        if (created) {
            if (editing) toggleEditing()
            switchToList(listId)
        }
    }

    /** No prompt: the copy is made, shown and announced; edit mode stays on to prune it. */
    fun duplicateList(
        listId: String,
        announce: (String) -> String,
    ) {
        val newId = model.duplicateList(listId) ?: return
        stopPlaying()
        model.currentListId = newId
        announcement = Announcement(announce(model.displayName(newId)), long = false)
    }

    fun confirmDelete(listId: String) {
        val list = model.songLists[listId] ?: return
        if (list.id == SongsModel.DEFAULT_ID) return
        pendingDelete = listId
    }

    /**
     * Deletes [listId], offering to undo: the only irreversible action in the feature gets the
     * same slot Duplicate's announcement uses, and the list comes back with its songs and its id.
     */
    fun deleteList(
        listId: String,
        announce: (String) -> String,
        undoLabel: String,
    ) {
        pendingDelete = null
        val list = model.songLists[listId] ?: return
        if (listId == SongsModel.DEFAULT_ID) return
        val wasCurrent = listId == model.currentListId
        if (wasCurrent) stopPlaying()
        val name = model.displayName(list)
        model.deleteList(listId)
        if (wasCurrent && editing) toggleEditing()
        announcement =
            Announcement(
                announce(name),
                long = true,
                actionLabel = undoLabel,
                onAction = {
                    model.restoreList(list)
                    if (wasCurrent) model.currentListId = listId
                },
            )
    }

    /** Sounds [song], stopping whatever else in the list was sounding. */
    fun play(song: PitchedSong) {
        stopPlaying()
        song.play()
    }

    fun stopPlaying() {
        BriefNotes.cancelAll()
        model.currentList.songs.forEach { it.stop() }
    }
}
