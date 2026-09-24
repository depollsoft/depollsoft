package depollsoft.pitchperfect

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import depollsoft.pitchperfect.lib.PitchedSong
import depollsoft.pitchperfect.ui.DrawableIcon
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateActionIcon
import depollsoft.pitchperfect.ui.PlateAlertDialog
import depollsoft.pitchperfect.ui.PlateFab
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateMenuItem
import depollsoft.pitchperfect.ui.PlateMenuRow
import depollsoft.pitchperfect.ui.PlateOverflowMenu
import depollsoft.pitchperfect.ui.hairlineWidth
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * The Songs tab: the set list selector, the current list's songs (sounding their keys while held),
 * and in edit mode each song's edit button and drag handle.
 */
@Composable
fun SongListScreen(
    state: SongListState,
    isCurrentPage: Boolean,
) {
    val context = LocalContext.current
    val model = state.model
    LifecycleResumeEffect(state) { onPauseOrDispose { state.stopPlaying() } }
    OnPageVisibilityChange(isCurrentPage) { current -> if (!current) state.stopPlaying() }

    model.trackLists()
    val list = model.currentList
    val songs = list.songs
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SetListSelector(
                lists = model.orderedLists,
                currentId = model.currentListId,
                displayName = model::displayName,
                onSelect = state::switchToList,
                onCreate = state::promptNewList,
                onLongPress = { state.menuFor = it },
                positionMenu = { listId -> SetListMenu(state, listId) },
                modifier =
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .height(48.dp),
            )
            if (songs.isEmpty()) {
                // The empty state names the list's kind: the home list, or a set list.
                val colors = plateColors
                LegacyText(
                    stringResource(if (list.id == SongsModel.DEFAULT_ID) R.string.NoSongsInList else R.string.NoSongsInSetList),
                    size = 15.sp,
                    color = colors.inkSecondary,
                    typeface = remember(context) { PlateFonts.oswaldTypeface(context) },
                    letterSpacing = 0.12f,
                    lineSpacingExtra = 6.dp,
                    align = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(start = 32.dp, end = 32.dp, top = 36.dp)
                            .fillMaxWidth()
                            .testTag(TestTags.SONGS_EMPTY),
                )
            }
            SongRows(state, list, state.scrollStateFor(list.id))
        }
        PlateFab(
            R.drawable.ic_add_button,
            description = null,
            onClick = {
                context.startActivity(
                    Intent(context, AddSongActivity::class.java).putExtra(AddSongActivity.LIST_EXTRA, model.currentListId),
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TestTags.ADD_SONG_FAB),
        )
        Announcements(state, Modifier.align(Alignment.BottomCenter))
    }
    SongListDialogs(state)
}

/**
 * The tab's snackbar: Duplicate's and Delete's announcements, the latter with Undo. They stay up
 * as long as MaterialComponents' short and long snackbars did.
 */
@Composable
private fun Announcements(
    state: SongListState,
    modifier: Modifier,
) {
    val host = remember { SnackbarHostState() }
    val announcement = state.announcement
    LaunchedEffect(announcement) {
        if (announcement == null) return@LaunchedEffect
        coroutineScope {
            val dismiss =
                launch {
                    delay(if (announcement.long) LONG_SNACKBAR_MS else SHORT_SNACKBAR_MS)
                    host.currentSnackbarData?.dismiss()
                }
            val result = host.showSnackbar(announcement.text, announcement.actionLabel, SnackbarDuration.Indefinite)
            dismiss.cancel()
            if (result == SnackbarResult.ActionPerformed) announcement.onAction?.invoke()
        }
        if (state.announcement == announcement) state.announcement = null
    }
    SnackbarHost(host, modifier)
}

private const val SHORT_SNACKBAR_MS = 1_500L
private const val LONG_SNACKBAR_MS = 2_750L

@Composable
private fun SongRows(
    state: SongListState,
    list: SongList,
    listState: LazyListState,
) {
    val drag = remember(list) { SongDrag(list, listState) }
    LazyColumn(
        Modifier.fillMaxSize().testTag(TestTags.SONG_LIST),
        state = listState,
        contentPadding = PaddingValues(bottom = 90.dp),
    ) {
        items(list.songs, key = { it.id }) { song ->
            val dragging = drag.songId == song.id
            Column(
                Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationY = drag.offset
                            shadowElevation = 1.dp.toPx()
                        }
                    },
            ) {
                SongRow(state, list, song, drag)
                Hairline()
            }
        }
    }
}

@Composable
private fun SongRow(
    state: SongListState,
    list: SongList,
    song: PitchedSong,
    drag: SongDrag,
) {
    val colors = plateColors
    val context = LocalContext.current
    val editing = state.editing
    val lit = song.isPlaying
    var menuOpen by remember { mutableStateOf(false) }
    val openEditor = {
        context.startActivity(
            Intent(context, AddSongActivity::class.java)
                .putExtra(AddSongActivity.ID_EXTRA, song.id)
                .putExtra(AddSongActivity.LIST_EXTRA, list.id),
        )
    }
    val moveUp = stringResource(R.string.MoveUp)
    val moveDown = stringResource(R.string.MoveDown)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.songRow(song.id))
            .background(if (lit) colors.accent else Color.Transparent)
            .semantics {
                if (editing) {
                    customActions =
                        listOfNotNull(
                            CustomAccessibilityAction(moveUp) {
                                list.moveUp(song)
                                true
                            }.takeIf { list.canMoveUp(song) },
                            CustomAccessibilityAction(moveDown) {
                                list.moveDown(song)
                                true
                            }.takeIf { list.canMoveDown(song) },
                        )
                }
            },
        verticalAlignment = ViewCenterVertically,
    ) {
        if (editing) {
            Box {
                Box(
                    Modifier
                        .width(48.dp)
                        .testTag(TestTags.EDIT_SONG_BUTTON)
                        .combinedClickable(
                            role = Role.Button,
                            indication = ripple(bounded = false),
                            interactionSource = null,
                            onLongClick = { menuOpen = true },
                            onClick = openEditor,
                        ).semantics { contentDescription = context.getString(R.string.EditSong) }
                        .padding(16.dp),
                ) {
                    DrawableIcon(R.drawable.ic_edit_button, colors.ink, size = 16.dp)
                }
                SongContextMenu(menuOpen, { menuOpen = false }, list, song, openEditor)
            }
        }
        Row(
            Modifier
                .weight(1f)
                .soundsWhileHeld(
                    toggleMode = { SettingsModel.toggleNotes },
                    isPlaying = { song.isPlaying },
                    play = { state.play(song) },
                    stop = { song.stop() },
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    selected = lit
                    onClick {
                        BriefNotes.activate(
                            song,
                            isPlaying = { song.isPlaying },
                            play = { state.play(song) },
                            stop = { song.stop() },
                        )
                    }
                },
            verticalAlignment = ViewCenterVertically,
        ) {
            PlateText(
                song.name.orEmpty(),
                style = plateText(20.sp, if (lit) colors.onAccent else colors.ink, PlateFonts.condensed),
                modifier = Modifier.weight(1f).padding(start = 20.dp, top = 14.dp, bottom = 14.dp),
            )
            PlateText(
                song.key?.let { NoteText.keyName(it) } ?: androidx.compose.ui.text.AnnotatedString(""),
                style = plateText(18.sp, if (lit) colors.onAccent else colors.inkSecondary, PlateFonts.mono, letterSpacing = 0.06f),
                modifier = Modifier.padding(end = 20.dp),
            )
        }
        if (editing) {
            Box(
                Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .testTag(TestTags.DRAG_HANDLE)
                    .semantics { contentDescription = context.getString(R.string.ReorderSong) }
                    .pointerInput(song.id) { drag.track(this, song.id) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                DrawableIcon(R.drawable.ic_drag_handle, colors.inkSecondary)
            }
        }
    }
}

/** The edit button's long-press menu: the song's own actions. */
@Composable
private fun SongContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    list: SongList,
    song: PitchedSong,
    openEditor: () -> Unit,
) {
    DropdownMenu(expanded, onDismiss) {
        PlateMenuRow(stringResource(R.string.EditSong)) {
            onDismiss()
            openEditor()
        }
        PlateMenuRow(stringResource(R.string.RemoveSong)) {
            onDismiss()
            list.removeSong(song)
        }
        PlateMenuRow(stringResource(R.string.SortAll)) {
            onDismiss()
            list.sortSongs()
        }
        if (list.canMoveUp(song)) {
            PlateMenuRow(stringResource(R.string.MoveUp)) {
                onDismiss()
                list.moveUp(song)
            }
        }
        if (list.canMoveDown(song)) {
            PlateMenuRow(stringResource(R.string.MoveDown)) {
                onDismiss()
                list.moveDown(song)
            }
        }
    }
}

/**
 * Long-pressing a selector position offers the list's own actions — rename, duplicate, delete and
 * the manage screen — without first entering edit mode. The menu names the list it acts on: a long
 * press on one position while another is current would otherwise look like the current list's.
 */
@Composable
private fun SetListMenu(
    state: SongListState,
    listId: String,
) {
    val context = LocalContext.current
    val model = state.model
    val close = { state.menuFor = null }
    DropdownMenu(state.menuFor == listId, close) {
        PlateMenuRow(model.displayName(listId), enabled = false) {}
        PlateMenuRow(stringResource(R.string.SetListRenameAction)) {
            close()
            state.promptRename(listId)
        }
        PlateMenuRow(stringResource(R.string.SetListDuplicateAction)) {
            close()
            state.duplicateList(listId) { context.getString(R.string.SetListDuplicatedAnnouncement, it) }
        }
        if (listId != SongsModel.DEFAULT_ID) {
            PlateMenuRow(stringResource(R.string.SetListDeleteAction)) {
                close()
                state.confirmDelete(listId)
            }
        }
        PlateMenuRow(stringResource(R.string.SetListManageAction)) {
            close()
            context.startActivity(Intent(context, ManageSetListsActivity::class.java))
        }
    }
}

/** The prompts the tab can have open: naming a list and confirming a delete. */
@Composable
private fun SongListDialogs(state: SongListState) {
    val context = LocalContext.current
    state.nameRequest?.let { request ->
        SetListNameDialog(
            listId = request.listId,
            onDismiss = { state.nameRequest = null },
            onDone = { id, created -> state.nameChosen(id, created) },
        )
    }
    state.pendingDelete?.let { listId ->
        DeleteSetListDialog(
            state.model,
            listId,
            onDismiss = { state.pendingDelete = null },
            onDelete = {
                state.deleteList(
                    listId,
                    announce = { context.getString(R.string.SetListDeletedAnnouncement, it) },
                    undoLabel = context.getString(R.string.Undo),
                )
            },
        )
    }
}

/** Confirms deleting a set list, saying how many songs go with it. My Songs is untouched. */
@Composable
fun DeleteSetListDialog(
    model: SongsModel,
    listId: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val list = model.songLists[listId] ?: return
    val count = list.songs.size
    val message =
        if (count == 0) {
            stringResource(R.string.SetListDeleteMessageEmpty)
        } else {
            stringResource(R.string.SetListDeleteMessage, pluralStringResource(R.plurals.SetListDeleteSongs, count, count))
        }
    PlateAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.SetListDeleteTitle, model.displayName(list)),
        message = message,
        buttons =
            listOf(
                DialogButton(stringResource(R.string.Cancel), onDismiss),
                DialogButton(stringResource(R.string.SetListDelete), onDelete),
            ),
    )
}

/** The action bar's actions: the Songs tab's edit mode and list actions, then Settings. */
@Composable
fun MainActions(
    onSongs: Boolean,
    songs: SongListState,
    openSettings: () -> Unit,
) {
    val context = LocalContext.current
    val editing = onSongs && songs.editing
    if (onSongs) {
        PlateActionIcon(
            if (editing) R.drawable.ic_check else R.drawable.ic_edit_button,
            stringResource(if (editing) R.string.StopEditing else R.string.EditSongList),
            songs::toggleEditing,
            Modifier.testTag(TestTags.EDIT_SONGS),
        )
    }
    if (editing) {
        PlateActionIcon(R.drawable.ic_sort_button, stringResource(R.string.SortAll), songs::sortSongs, Modifier.testTag(TestTags.SORT_SONGS))
    }
    PlateActionIcon(R.drawable.ic_settings, stringResource(R.string.Settings), openSettings, Modifier.testTag(TestTags.SETTINGS))
    if (editing) {
        // Edit mode carries the list's contents actions; the list itself is managed from a long
        // press on its selector position and from the Set Lists screen.
        val canAdd = songs.canAddSongsFromOtherLists()
        PlateOverflowMenu(
            listOf(
                PlateMenuItem(
                    // A disabled item says why, instead of leaving the person to guess.
                    stringResource(if (canAdd) R.string.SetListAddFrom else R.string.SetListAddFromNothing),
                    TestTags.ADD_FROM_LIST,
                    enabled = canAdd,
                ) {
                    context.startActivity(
                        Intent(context, AddSongsFromListActivity::class.java)
                            .putExtra(AddSongsFromListActivity.LIST_EXTRA, songs.model.currentListId),
                    )
                },
                PlateMenuItem(stringResource(R.string.SetListManageAction), TestTags.MANAGE_LISTS) {
                    context.startActivity(Intent(context, ManageSetListsActivity::class.java))
                },
            ),
        )
    }
}

/**
 * A drag of one song by its handle: the row follows the finger, trading places with a neighbour
 * once it passes that neighbour's middle. The steps are not stored; the drop commits once.
 */
private class SongDrag(
    private val list: SongList,
    private val listState: LazyListState,
) {
    var songId by mutableStateOf<String?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set

    suspend fun track(
        scope: androidx.compose.ui.input.pointer.PointerInputScope,
        id: String,
    ) = scope.awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        songId = id
        offset = 0f
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val delta = change.position.y - change.previousPosition.y
                change.consume()
                if (delta != 0f) moveBy(delta)
            }
        } finally {
            songId = null
            offset = 0f
            list.notifyOfChange()
        }
    }

    private fun moveBy(delta: Float) {
        offset += delta
        val items = listState.layoutInfo.visibleItemsInfo
        val index = list.songs.indexOfFirst { it.id == songId }
        val current = items.firstOrNull { it.index == index } ?: return
        val middle = current.offset + offset + current.size / 2f
        val next = items.firstOrNull { it.index == index + 1 }
        val previous = items.firstOrNull { it.index == index - 1 }
        if (offset > 0 && next != null && middle > next.offset + next.size / 2f) {
            list.moveWithoutStoring(index, index + 1)
            offset -= next.size
        } else if (offset < 0 && previous != null && middle < previous.offset + previous.size / 2f) {
            list.moveWithoutStoring(index, index - 1)
            offset += previous.size
        }
    }
}
