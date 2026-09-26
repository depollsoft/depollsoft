package depollsoft.pitchperfect

import android.content.Intent
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import depollsoft.compose.ReorderState
import depollsoft.compose.SlidingSnackbarHost
import depollsoft.compose.SnackbarTiming
import depollsoft.compose.ViewAlign
import depollsoft.compose.listItemMotion
import depollsoft.compose.rememberReorderState
import depollsoft.compose.rememberSnackbarState
import depollsoft.compose.reorderHandle
import depollsoft.compose.reorderRow
import depollsoft.compose.shownOrder
import depollsoft.pitchperfect.lib.PitchedSong
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.DrawableIcon
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateActionIcon
import depollsoft.pitchperfect.ui.PlateAlertDialog
import depollsoft.pitchperfect.ui.PlateFab
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateMenuItem
import depollsoft.pitchperfect.ui.PlateOverflowMenu
import depollsoft.pitchperfect.ui.PlatePopupMenu
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.PopupMenuItem
import depollsoft.pitchperfect.ui.isTablet
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
            description = stringResource(R.string.AddSong),
            onClick = {
                context.startActivity(
                    Intent(context, AddSongActivity::class.java).putExtra(AddSongActivity.LIST_EXTRA, model.currentListId),
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TestTags.ADD_SONG_FAB),
        )
    }
    SongListDialogs(state)
}

/**
 * The Songs tab's snackbar: Duplicate's and Delete's announcements, the latter with Undo. The main
 * screen hosts it at the bottom of the window, over the navigation, where Snackbar.make put it, so
 * it stays up across a tab switch.
 */
@Composable
fun SongAnnouncements(
    state: SongListState,
    modifier: Modifier = Modifier,
) {
    val snackbars = rememberSnackbarState()
    val announcement = state.announcement
    LaunchedEffect(announcement) {
        if (announcement == null) return@LaunchedEffect
        val duration = if (announcement.long) SnackbarTiming.LONG_MILLIS else SnackbarTiming.SHORT_MILLIS
        if (snackbars.show(announcement.text, announcement.actionLabel, duration)) announcement.onAction?.invoke()
        if (state.announcement == announcement) state.announcement = null
    }
    val colors = plateColors
    // MaterialComponents' snackbar: 8dp in from the window's edges, between 320dp and 576dp wide
    // on a tablet, and its action in the theme's colorPrimary, the plate's surface.
    val size = if (isTablet) Modifier.widthIn(min = 320.dp, max = 576.dp) else Modifier
    SlidingSnackbarHost(snackbars, modifier) { shown ->
        Snackbar(
            Modifier.padding(8.dp).then(size),
            action =
                shown.actionLabel?.let { label ->
                    {
                        TextButton(onClick = shown::performAction, colors = ButtonDefaults.textButtonColors(contentColor = colors.surface)) {
                            Text(label)
                        }
                    }
                },
        ) { Text(shown.message) }
    }
}

@Composable
private fun SongRows(
    state: SongListState,
    list: SongList,
    listState: LazyListState,
) {
    // Songs trade places by id; a synced change waits until the drop, as the View list, which
    // skipped refreshing mid-drag, did.
    val reorder =
        rememberReorderState<String>(listState, list, keyOf = { it }) { _, order ->
            list.reorder(order)
        }
    val byId = list.songs.associateBy { it.id }
    val songs = reorder.shownOrder(list.songs.map { it.id }, listState).mapNotNull { byId[it] }
    val surface = plateColors.surface
    LazyColumn(
        Modifier.fillMaxSize().testTag(TestTags.SONG_LIST),
        state = listState,
        contentPadding = PaddingValues(bottom = 90.dp),
    ) {
        items(songs, key = { it.id }) { song ->
            Column(
                Modifier
                    .listItemMotion(this, animatePlacement = !reorder.isMoving(song.id))
                    .reorderRow(reorder, song.id, surface),
            ) {
                SongRow(state, list, song, reorder, listState)
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
    reorder: ReorderState<String>,
    listState: LazyListState,
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
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        if (editing) {
            Box {
                Box(
                    Modifier
                        .width(48.dp)
                        // The borderless ImageButton wrapped its 24dp icon in 16dp of padding: 56dp tall.
                        .height(56.dp)
                        .testTag(TestTags.EDIT_SONG_BUTTON)
                        .combinedClickable(
                            role = Role.Button,
                            indication = ripple(bounded = false),
                            interactionSource = null,
                            onLongClick = { menuOpen = true },
                            onClick = openEditor,
                        ).semantics { contentDescription = context.getString(R.string.EditSong) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DrawableIcon(R.drawable.ic_edit_button, colors.ink, size = 16.dp)
                }
                SongContextMenu(menuOpen, { menuOpen = false }, list, song, openEditor, listState)
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
            verticalAlignment = ViewAlign.CenterVertically,
        ) {
            PlateText(
                song.name.orEmpty(),
                style = plateText(20.sp, if (lit) colors.onAccent else colors.ink, PlateFonts.condensed),
                modifier = Modifier.weight(1f).padding(start = 20.dp, top = 14.dp, bottom = 14.dp),
            )
            LegacyText(
                song.key?.let { NoteText.keyName(it) } ?: "",
                18.sp,
                if (lit) colors.onAccent else colors.inkSecondary,
                Typeface.MONOSPACE,
                Modifier.padding(end = 20.dp),
                letterSpacing = 0.06f,
                wrapWidth = true,
            )
        }
        if (editing) {
            Box(
                Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .testTag(TestTags.DRAG_HANDLE)
                    .semantics { contentDescription = context.getString(R.string.ReorderSong) }
                    .reorderHandle(reorder, song.id, { list.songs.map { it.id } }, enabled = true)
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
    listState: LazyListState,
) {
    PlatePopupMenu(
        expanded,
        onDismiss,
        listOfNotNull(
            PopupMenuItem(stringResource(R.string.EditSong), onClick = openEditor),
            PopupMenuItem(stringResource(R.string.RemoveSong)) { list.removeSong(song) },
            PopupMenuItem(stringResource(R.string.SortAll)) {
                list.sortSongs()
            },
            if (list.canMoveUp(song)) {
                PopupMenuItem(stringResource(R.string.MoveUp)) {
                        list.moveUp(song)
                }
            } else {
                null
            },
            if (list.canMoveDown(song)) {
                PopupMenuItem(stringResource(R.string.MoveDown)) {
                        list.moveDown(song)
                }
            } else {
                null
            },
        ),
    )
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
    PlatePopupMenu(
        state.menuFor == listId,
        close,
        listOfNotNull(
            PopupMenuItem(model.displayName(listId), enabled = false),
            PopupMenuItem(stringResource(R.string.SetListRenameAction)) { state.promptRename(listId) },
            PopupMenuItem(stringResource(R.string.SetListDuplicateAction)) {
                state.duplicateList(listId) { context.getString(R.string.SetListDuplicatedAnnouncement, it) }
            },
            if (listId != SongsModel.DEFAULT_ID) {
                PopupMenuItem(stringResource(R.string.SetListDeleteAction)) { state.confirmDelete(listId) }
            } else {
                null
            },
            PopupMenuItem(stringResource(R.string.SetListManageAction)) {
                context.startActivity(Intent(context, ManageSetListsActivity::class.java))
            },
        ),
    )
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
