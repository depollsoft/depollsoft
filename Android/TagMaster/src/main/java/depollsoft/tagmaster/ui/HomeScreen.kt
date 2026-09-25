package depollsoft.tagmaster.ui

import depollsoft.compose.listItemMotion
import depollsoft.compose.shownOrder
import depollsoft.compose.reorderRow
import depollsoft.compose.reorderHandle
import depollsoft.compose.recyclerScrollbar
import depollsoft.compose.revealItem
import depollsoft.compose.rememberReorderState
import depollsoft.compose.ViewAlign
import depollsoft.lib.util.appVersionName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.R
import depollsoft.tagmaster.SettingsActivity
import depollsoft.tagmaster.SettingsModel
import depollsoft.tagmaster.TagBrowserActivity
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TagLists
import depollsoft.tagmaster.TagPaneState
import depollsoft.tagmaster.TagSearchActivity
import depollsoft.tagmaster.TeachableTagsActivity
import depollsoft.tagmaster.await
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random

/**
 * Home's two requests — a random tag, and a tag opened by its id — held by the activity so that
 * scrolling the header off screen never cancels one. Leaving the screen does.
 */
@Stable
class HomeActions(
    private val context: Context,
    private val pane: TagPaneState,
) {
    var isLoadingRandom by mutableStateOf(false)
        private set

    var openTagDialog by mutableStateOf(false)

    private var scope: CoroutineScope? = null

    /** Opens [id] beside the list on a wide window, full-screen otherwise. */
    fun openTag(id: Int) {
        if (pane.hasDetailPane) {
            pane.showTag(id)
        } else {
            context.startActivity(Intent(context, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, id))
        }
    }

    fun loadRandomTag(snackbars: Snackbars) {
        if (isLoadingRandom) return
        isLoadingRandom = true
        scope?.cancel()
        scope =
            CoroutineScope(Dispatchers.Main + Job()).also { launched ->
                launched.launch {
                    try {
                        val count =
                            Tag
                                .query(
                                    null,
                                    0,
                                    0,
                                    null,
                                    SettingsModel.randomLearningTracksFilter,
                                    SettingsModel.randomSheetMusicFilter,
                                    null,
                                    null,
                                    SettingsModel.minimumRandomTagRating,
                                    SettingsModel.minimumRandomDownloads,
                                    false,
                                    "id",
                                ).await()
                        if (count.available == 0) {
                            snackbars.show(
                                context.getString(R.string.home_random_no_matches),
                                context.getString(R.string.Settings),
                            ) { context.startActivity(Intent(context, SettingsActivity::class.java)) }
                            return@launch
                        }
                        val chosen = Random().nextInt(count.available)
                        val result =
                            Tag
                                .query(
                                    null,
                                    1,
                                    chosen,
                                    null,
                                    SettingsModel.randomLearningTracksFilter,
                                    SettingsModel.randomSheetMusicFilter,
                                    null,
                                    null,
                                    SettingsModel.minimumRandomTagRating,
                                    SettingsModel.minimumRandomDownloads,
                                    false,
                                    "id",
                                ).await()
                        openTag(result.tags[0].id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        snackbars.show(context.getString(R.string.home_random_error), context.getString(R.string.home_retry)) {
                            loadRandomTag(snackbars)
                        }
                    } finally {
                        isLoadingRandom = false
                    }
                }
            }
    }

    fun release() {
        scope?.cancel()
        scope = null
        isLoadingRandom = false
        openTagDialog = false
    }
}

/** Home's list: the actions, the lists, the favorites and the about footer. */
@Composable
fun HomeScreen(
    pane: TagPaneState,
    actions: HomeActions,
    editor: SavedListEditor,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val dialogs = rememberListDialogs()
    val listState = rememberLazyListState()
    val favorites = ListModel(TagLists.FAVORITE)
    val favoriteIds = favorites.ids.toList()
    TagLists.version
    val customKeys = TagLists.customKeys.toList()
    val changed = stringResource(R.string.saved_list_changed)
    val listsReorder =
        rememberReorderState<String>(listState, keyOf = { "list:$it" }) { baseline, order ->
            // A list deleted or created mid-drag changes the set; then this order is not one.
            baseline.toSet() == TagLists.customKeys.toSet() && TagLists.reorder(order)
        }
    val favoritesReorder =
        rememberReorderState<Int>(listState, keyOf = { "favorite:$it" }) { baseline, order ->
            val snapshot = favorites.snapshot()
            snapshot.ids == baseline && favorites.reorder(snapshot, order)
        }
    LaunchedEffect(favoriteIds, customKeys) {
        editor.contentChanged()
        val interrupted = favoritesReorder.sourceChanged(favoriteIds) or listsReorder.sourceChanged(customKeys)
        if (interrupted) view.announceForAccessibility(changed)
    }
    LaunchedEffect(editor.isEditing) {
        if (!editor.isEditing) {
            favoritesReorder.cancel()
            listsReorder.cancel()
        }
    }
    // Leaving the screen drops a drag in progress and the Remove confirmation, as the View
    // editor's pause() did.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        favoritesReorder.cancel()
        listsReorder.cancel()
        editor.dismissRemoval()
    }
    // The rows before the favorites: the header, the user's lists and the row closing them.
    val favoritesStart = 2 + customKeys.size
    val scope = rememberCoroutineScope()
    pane.reveal = { id ->
        val index = favoriteIds.indexOf(id)
        if (index >= 0) scope.launch { listState.revealItem(index + favoritesStart) }
    }
    val favoritesLabel = stringResource(R.string.Favorites)
    val bar =
        @Composable {
            TagMasterTopBar(
                title = stringResource(R.string.home_title),
                brandTitle = true,
                paneTitle = if (pane.hasDetailPane) stringResource(R.string.tag_pane_list_title) else null,
                actions =
                    listOf(
                        BarAction("settings", stringResource(R.string.Settings), R.drawable.ic_settings, ShowAs.Always) {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        },
                        BarAction(
                            "editSavedList",
                            stringResource(if (editor.isEditing) R.string.saved_list_done else R.string.saved_list_edit),
                            showAs = ShowAs.Always,
                            enabled = editor.canEdit,
                        ) { editor.toggle() },
                    ),
            )
        }
    ListDetailScaffold(pane, dialogs, Watermark.Window, bar) {
        val snackbars = LocalSnackbars.current
        val colors = TagMasterTheme.colors
        val lists = listsReorder.shownOrder(customKeys, listState)
        val shownFavorites = favoritesReorder.shownOrder(favoriteIds, listState)
        ReadingWidth(Modifier.fillMaxSize()) { inset ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .recyclerScrollbar(listState, top = 16.dp, bottom = 88.dp)
                    .testTag("homeList"),
                state = listState,
                contentPadding = PaddingValues(start = inset, top = 16.dp, end = inset, bottom = 88.dp),
            ) {
                item(key = "header") {
                    Column {
                        ActionRow(R.drawable.ic_library_music, Modifier.testTag("browseButton"), onClick = {
                            context.startActivity(Intent(context, TagBrowserActivity::class.java))
                        }) { ActionTitle(stringResource(R.string.BrowseTags)) }
                        ActionRow(
                            R.drawable.ic_shuffle,
                            Modifier.testTag("randomTagButton"),
                            enabled = !actions.isLoadingRandom,
                            onClick = { actions.loadRandomTag(snackbars) },
                            trailing = {
                                CompactBarberPole(
                                    actions.isLoadingRandom,
                                    stringResource(R.string.home_loading_random),
                                    Modifier.padding(start = 16.dp),
                                )
                            },
                        ) { ActionTitle(stringResource(R.string.RandomTag)) }
                        ActionRow(R.drawable.ic_tag, Modifier.testTag("openByIdButton"), onClick = { actions.openTagDialog = true }) {
                            ActionTitle(stringResource(R.string.open_tag))
                        }
                        SectionHeading(stringResource(R.string.lists_heading))
                        ActionRow(R.drawable.ic_people, Modifier.testTag("teachableButton"), onClick = {
                            context.startActivity(Intent(context, TeachableTagsActivity::class.java))
                        }) {
                            Column {
                                ActionTitle(stringResource(R.string.TeachableTags))
                                ActionDetail(listCountText(TagLists.TEACHABLE), Modifier.testTag("teachableCount"))
                            }
                        }
                    }
                }
                itemsIndexed(lists, key = { _, key -> "list:$key" }) { index, key ->
                    ManagedListRow(
                        key = key,
                        name = TagLists.name(key),
                        detail = listCountText(key),
                        editing = editor.isEditing,
                        position = index,
                        count = lists.size,
                        dialogs = dialogs,
                        onOpen = { context.startActivity(TagListActivity.intent(context, key)) },
                        handleGesture = { pressed -> Modifier.reorderHandle(listsReorder, key, { TagLists.customKeys.toList() }, editor.isEditing && lists.size > 1, pressed) },
                        modifier =
                            Modifier
                                .listItemMotion(this, animatePlacement = !listsReorder.isMoving(key))
                                .reorderRow(listsReorder, key, colors.surface),
                    )
                }
                item(key = "listsFooter") {
                    Column(Modifier.listItemMotion(this)) {
                        ActionRow(R.drawable.ic_add, Modifier.testTag("newListButton"), onClick = { dialogs.newList() }) {
                            ActionTitle(stringResource(R.string.list_new_row))
                        }
                        SectionHeading(favoritesLabel)
                        if (favoriteIds.isEmpty()) {
                            Text(
                                stringResource(R.string.home_favorites_empty),
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                    .testTag("favoritesEmptyText"),
                                style = TagMasterType.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                itemsIndexed(shownFavorites, key = { _, id -> "favorite:$id" }) { index, id ->
                    SavedTagRow(
                        id = id,
                        editing = editor.isEditing,
                        position = index,
                        count = shownFavorites.size,
                        listLabel = favoritesLabel,
                        selected = pane.selectedTagId == id,
                        onOpen = actions::openTag,
                        onRemove = editor::askToRemove,
                        onMove = editor::move,
                        handleModifier = { pressed ->
                            Modifier.reorderHandle(favoritesReorder, id, { favorites.ids.toList() }, editor.isEditing && shownFavorites.size > 1, pressed)
                        },
                        modifier =
                            Modifier
                                .listItemMotion(this, animatePlacement = !favoritesReorder.isMoving(id))
                                .reorderRow(favoritesReorder, id, colors.surface)
                                .topDivider(index > 0, colors.outlineVariant),
                    )
                }
                item(key = "footer") { Box(Modifier.listItemMotion(this)) { AboutFooter() } }
            }
        }
        SearchFab { context.startActivity(Intent(context, TagSearchActivity::class.java)) }
    }
    ListDialogsHost(dialogs)
    RemoveFromListDialog(editor, favoritesLabel)
    if (actions.openTagDialog) OpenTagDialog(actions)
}

/** The app name and version, attribution, links, copyright year and support links. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutFooter() {
    val colors = TagMasterTheme.colors
    val secondary = TagMasterType.bodySmall
    // The year is read whenever the footer comes back on screen, so it is right after New Year.
    val year = remember { GregorianCalendar().get(Calendar.YEAR) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            val context = LocalContext.current
            val version = stringResource(R.string.version_label, remember(context) { appVersionName(context) })
            for (text in listOf(stringResource(R.string.app_name), stringResource(R.string.home_version_separator), version)) {
                Text(text, Modifier.textViewWidth(text, secondary), style = secondary, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
        Hyperlink(
            stringResource(R.string.home_content_attribution),
            "http://www.barbershoptags.com",
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally),
        ) {
            Row(verticalAlignment = ViewAlign.CenterVertically) {
                Hyperlink(
                    stringResource(R.string.DepollSoft),
                    "http://apps.depoll.com",
                    Modifier
                        .heightIn(min = 48.dp)
                        .padding(start = 8.dp, end = 4.dp),
                )
                val copyright = stringResource(R.string.Copyright, year)
                Text(copyright, Modifier.textViewWidth(copyright, secondary), style = secondary, color = colors.onSurfaceVariant, maxLines = 1)
            }
            Hyperlink(stringResource(R.string.TermsOfUse), "http://apps.depoll.com/terms-of-use", Modifier.padding(horizontal = 8.dp))
            Hyperlink(stringResource(R.string.Donate), "http://www.davidpoll.com/applications/tag-master/donate", Modifier.padding(horizontal = 8.dp))
        }
    }
}

/** "Enter Tag ID": a number field that opens the tag, or says why it cannot. */
@Composable
private fun OpenTagDialog(actions: HomeActions) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var error by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    fun submit() {
        val id = text.text.toIntOrNull()
        if (id == null || id <= 0) {
            error = true
            return
        }
        error = false
        actions.openTagDialog = false
        actions.openTag(id)
    }
    TagMasterDialog(
        onDismissRequest = { actions.openTagDialog = false },
        wrapWidth = true,
        title = stringResource(R.string.home_enter_tag_id),
        dismiss = DialogButton(stringResource(R.string.home_cancel)) { actions.openTagDialog = false },
        confirm = DialogButton(stringResource(R.string.home_open), id = "openTagConfirm") { submit() },
    ) {
        OutlinedField(
            label = stringResource(R.string.TagId),
            value = text,
            onValueChange = { text = it },
            modifier =
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                    .fillMaxWidth(),
            error = if (error) stringResource(R.string.home_invalid_tag_id) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            fieldModifier = Modifier.focusRequester(focus).testTag("openTagIdInput"),
        )
        // Requested from inside the dialog, whose content is composed after the caller's.
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}
