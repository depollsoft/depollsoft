package depollsoft.tagmaster.ui

import depollsoft.compose.ListMotion
import depollsoft.compose.PlatformIcon
import depollsoft.compose.viewPx
import depollsoft.compose.ViewAlign
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.R
import depollsoft.tagmaster.TagLists
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight

/**
 * One list, as Home and the Add to list picker show it: its icon, name and tag count, a check
 * when [checked], and — while [editing] — the leading remove control and trailing drag handle.
 */
@Composable
fun ListRow(
    key: String?,
    name: String,
    detail: String?,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = key?.let(::listIconRes) ?: R.drawable.ic_list,
    checked: Boolean = false,
    editing: Boolean = false,
    removeLabel: String? = null,
    onRemove: () -> Unit = {},
    handle: (@Composable () -> Unit)? = null,
) {
    val colors = TagMasterTheme.colors
    // The adapter set the gutter as (dp * density).toInt(), truncating.
    val gutter = with(LocalDensity.current) { viewPx(if (editing) 4 else 16).toDp() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = gutter, end = gutter, top = 12.dp, bottom = 12.dp),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        if (editing) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .clickable(interactionSource = null, indication = ripple(bounded = false, radius = 24.dp), role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = removeLabel ?: "" }
                    .testTag("listRemove:$key"),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(R.drawable.ic_remove_circle_outline, tint = colors.primary)
            }
        }
        PlatformIcon(icon, Modifier.padding(end = 16.dp), tint = colors.primary)
        Column(Modifier.weight(1f)) {
            // A rename or a new count crossfades, as RecyclerView animated a changed row.
            Crossfade(name, animationSpec = tween(ListMotion.CHANGE_MILLIS), label = "name") { ActionTitle(it, maxLines = 2) }
            if (detail != null) Crossfade(detail, animationSpec = tween(ListMotion.CHANGE_MILLIS), label = "detail") { ActionDetail(it) }
        }
        if (checked) PlatformIcon(R.drawable.ic_check, Modifier.padding(start = 16.dp), tint = colors.primary)
        handle?.invoke()
    }
}

/** The drag handle of a list row in edit mode, dimmed when there is nothing to reorder against. */
@Composable
fun DragHandle(
    label: String,
    enabled: Boolean,
    onMove: (Int) -> Boolean,
    gesture: (MutableInteractionSource) -> Modifier,
    tag: String,
    startMargin: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(start = if (startMargin) 8.dp else 0.dp)
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .indication(interactions, ripple(bounded = false, radius = 24.dp))
            .then(gesture(interactions))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> onMove(-1)
                    Key.DirectionDown -> onMove(1)
                    else -> false
                }
            }.focusable(enabled, interactions)
            .semantics {
                contentDescription = label
                role = Role.Button
            }.testTag(tag),
        contentAlignment = ViewAlign.Center,
    ) {
        PlatformIcon(R.drawable.ic_drag_handle, tint = TagMasterTheme.colors.onSurfaceVariant)
    }
}

/**
 * A custom list's row on Home: opens the list, and offers Rename…, Move up, Move down and
 * Delete… from a long press, a keyboard context click and accessibility actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManagedListRow(
    key: String,
    name: String,
    detail: String,
    editing: Boolean,
    position: Int,
    count: Int,
    dialogs: ListDialogs,
    onOpen: () -> Unit,
    handleGesture: (MutableInteractionSource) -> Modifier,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val rename = stringResource(R.string.list_row_rename)
    val moveUp = stringResource(R.string.MoveUp)
    val moveDown = stringResource(R.string.MoveDown)
    val delete = stringResource(R.string.list_row_delete)
    val positionLabel = stringResource(R.string.list_row_position, name, detail, position + 1, count)
    Box(modifier) {
        ListRow(
            key = key,
            name = name,
            detail = detail,
            editing = editing,
            removeLabel = stringResource(R.string.list_row_remove_named, name),
            onRemove = { if (editing) dialogs.delete(key) },
            handle =
                if (editing) {
                    {
                        DragHandle(
                            label = stringResource(R.string.list_row_drag_named, name, position + 1, count),
                            enabled = count > 1,
                            onMove = { delta -> TagLists.move(key, TagLists.customKeys.indexOf(key) + delta) },
                            gesture = handleGesture,
                            tag = "listDrag:$key",
                        )
                    }
                } else {
                    null
                },
            modifier =
                Modifier
                    .then(
                        if (editing) {
                            Modifier
                        } else {
                            Modifier.combinedClickable(onLongClick = { menuOpen = true }, onClick = onOpen)
                        },
                    ).semantics {
                        if (editing) contentDescription = positionLabel
                        customActions =
                            buildList {
                                add(CustomAccessibilityAction(rename) { dialogs.rename(key); true })
                                if (TagLists.canMoveUp(key)) add(CustomAccessibilityAction(moveUp) { TagLists.moveUp(key) })
                                if (TagLists.canMoveDown(key)) add(CustomAccessibilityAction(moveDown) { TagLists.moveDown(key) })
                                add(CustomAccessibilityAction(delete) { dialogs.delete(key); true })
                            }
                    }.testTag("listRow:$key"),
        )
        // Anchored to the row's end, where the thumb that opened it already is.
        Box(Modifier.align(androidx.compose.ui.Alignment.TopEnd)) {
            ListRowMenu(menuOpen, key, dialogs) { menuOpen = false }
        }
    }
}

@Composable
private fun ListRowMenu(
    expanded: Boolean,
    key: String,
    dialogs: ListDialogs,
    onDismiss: () -> Unit,
) {
    val colors = TagMasterTheme.colors
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, offset = DpOffset(0.dp, 0.dp), containerColor = colors.surfaceContainerHigh) {
        @Composable
        fun item(
            text: String,
            tag: String,
            action: () -> Unit,
        ) = DropdownMenuItem(
            text = { Text(text, style = TagMasterType.bodyLarge.withoutLineHeight(), color = colors.onSurface) },
            modifier = Modifier.testTag(tag),
            onClick = {
                onDismiss()
                action()
            },
        )
        item(stringResource(R.string.list_row_rename), "menuRename") { dialogs.rename(key) }
        if (TagLists.canMoveUp(key)) item(stringResource(R.string.MoveUp), "menuMoveUp") { TagLists.moveUp(key) }
        if (TagLists.canMoveDown(key)) item(stringResource(R.string.MoveDown), "menuMoveDown") { TagLists.moveDown(key) }
        item(stringResource(R.string.list_row_delete), "menuDelete") { dialogs.delete(key) }
    }
}
