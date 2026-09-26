package depollsoft.tagmaster.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import depollsoft.compose.ViewAlign
import depollsoft.compose.listItemMotion
import depollsoft.compose.recyclerScrollbar
import depollsoft.compose.rememberReorderState
import depollsoft.compose.reorderHandle
import depollsoft.compose.reorderRow
import depollsoft.compose.revealItem
import depollsoft.compose.shownOrder
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.R
import depollsoft.tagmaster.TagPaneState
import kotlinx.coroutines.launch

/**
 * One saved list — Teachable Tags or one of the user's own — as a screen: its toolbar (Edit, and
 * [extraActions]), the tag rows with edit mode and drag reordering, and [empty] when it has none.
 */
@Composable
fun SavedListScreen(
    model: ListModel,
    editor: SavedListEditor,
    pane: TagPaneState,
    title: String,
    brandTitle: Boolean,
    listLabel: String,
    listTag: String,
    onNavigateUp: () -> Unit,
    onOpenTag: (Int) -> Unit,
    extraActions: List<BarAction> = emptyList(),
    dialogs: ListDialogs = rememberListDialogs(),
    empty: @Composable () -> Unit,
) {
    val view = LocalView.current
    val listState = rememberLazyListState()
    val ids = model.ids.toList()
    val changed = stringResource(R.string.saved_list_changed)
    val reorder =
        rememberReorderState<Int>(listState, model, keyOf = { "tag:$it" }) { baseline, order ->
            val snapshot = model.snapshot()
            snapshot.ids == baseline && model.reorder(snapshot, order)
        }
    LaunchedEffect(ids) {
        editor.contentChanged()
        if (reorder.sourceChanged(ids)) view.announceForAccessibility(changed)
    }
    LaunchedEffect(editor.isEditing) { if (!editor.isEditing) reorder.cancel() }
    // Leaving the screen drops a drag in progress and the Remove confirmation.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        reorder.cancel()
        editor.dismissRemoval()
    }
    val scope = rememberCoroutineScope()
    pane.reveal = { id ->
        val index = ids.indexOf(id)
        if (index >= 0) scope.launch { listState.revealItem(index) }
    }
    val bar =
        @Composable {
            TagMasterTopBar(
                title = title,
                brandTitle = brandTitle,
                onNavigateUp = onNavigateUp,
                paneTitle = if (pane.hasDetailPane) stringResource(R.string.tag_pane_list_title) else null,
                actions =
                    listOf(
                        BarAction(
                            "editSavedList",
                            stringResource(if (editor.isEditing) R.string.saved_list_done else R.string.saved_list_edit),
                            showAs = ShowAs.Always,
                            enabled = editor.canEdit,
                        ) { editor.toggle() },
                    ) + extraActions,
            )
        }
    ListDetailScaffold(pane, dialogs, Watermark.Window, bar) {
        val colors = TagMasterTheme.colors
        val shown = reorder.shownOrder(ids, listState)
        ReadingWidth(Modifier.fillMaxSize()) { inset ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .recyclerScrollbar(listState, top = 16.dp, bottom = 16.dp)
                    .testTag(listTag),
                state = listState,
                contentPadding = PaddingValues(horizontal = inset, vertical = 16.dp),
            ) {
                itemsIndexed(shown, key = { _, id -> "tag:$id" }) { index, id ->
                    SavedTagRow(
                        id = id,
                        editing = editor.isEditing,
                        position = index,
                        count = shown.size,
                        listLabel = listLabel,
                        selected = pane.selectedTagId == id,
                        onOpen = onOpenTag,
                        onRemove = editor::askToRemove,
                        onMove = editor::move,
                        handleModifier = { pressed -> Modifier.reorderHandle(reorder, id, { model.ids.toList() }, editor.isEditing && shown.size > 1, pressed) },
                        modifier =
                            Modifier
                                .listItemMotion(this, animatePlacement = !reorder.isMoving(id))
                                .reorderRow(reorder, id, colors.surface)
                                .topDivider(index > 0, colors.outlineVariant),
                    )
                }
            }
        }
        if (ids.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = ViewAlign.Center) { empty() }
        }
    }
    ListDialogsHost(dialogs)
    RemoveFromListDialog(editor, listLabel)
}
