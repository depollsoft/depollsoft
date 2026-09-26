package depollsoft.tagmaster.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import depollsoft.compose.ListMotion
import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.R
import depollsoft.tagmaster.await
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.CancellationException

/** A tag loaded by id for a saved-list row: loading, loaded, or failed. */
@Stable
class TagLoad(
    val id: Int,
) {
    var tag: Tag? by mutableStateOf(null)
    var failed: Boolean by mutableStateOf(false)
    val isLoading: Boolean get() = tag == null && !failed
}

/**
 * A list screen's tag loads, kept while the screen is, so a row scrolled back into view shows the
 * tag it already loaded instead of starting empty and changing height a frame later.
 */
@Stable
class TagLoads {
    private val loads = mutableMapOf<Int, TagLoad>()

    fun of(id: Int): TagLoad = loads.getOrPut(id) { TagLoad(id) }
}

@Composable
fun rememberTagLoads(): TagLoads = remember { TagLoads() }

/** Loads [id] through the tag cache while the row is on screen, unless [loads] already has it. */
@Composable
fun rememberTagLoad(
    id: Int,
    loads: TagLoads? = null,
): TagLoad {
    val load = remember(id, loads) { loads?.of(id) ?: TagLoad(id) }
    LaunchedEffect(load) {
        if (load.tag != null) return@LaunchedEffect
        load.failed = false
        try {
            load.tag = Tag.loadTagById(id).await()
            if (load.tag == null) load.failed = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("depollsoft.tagmaster", "Failed to load tag", e)
            load.failed = true
        }
    }
    return load
}

/**
 * Edit mode for one saved list (favorites, Teachable Tags, a custom list): whether it is on, the
 * pending removal confirmation, and the drag that reorders the rows.
 *
 * Edit stays available while the list, or the rows that share its Edit action ([companion]),
 * have something to edit, and turns itself off when neither has.
 */
@Stable
class SavedListEditor(
    initiallyEditing: Boolean,
    private val model: () -> ListModel,
    private val companion: () -> Boolean = { false },
) {
    var isEditing by mutableStateOf(initiallyEditing && (model().ids.isNotEmpty() || companion()))
        private set

    /** The tag (id and shown name) the Remove confirmation is asking about. */
    var pendingRemoval: Pair<Int, String>? by mutableStateOf(null)
        private set

    val canEdit: Boolean get() = model().ids.isNotEmpty() || companion()

    fun toggle() {
        pendingRemoval = null
        isEditing = !isEditing && canEdit
    }

    /** Re-checks Edit after the list or its companion rows changed. */
    fun contentChanged() {
        if (!canEdit) isEditing = false
    }

    fun askToRemove(
        id: Int,
        name: String,
    ) {
        if (!isEditing || !model().contains(id)) return
        pendingRemoval = id to name
    }

    fun confirmRemoval() {
        val (id, _) = pendingRemoval ?: return
        pendingRemoval = null
        if (isEditing && model().contains(id)) model().remove(id)
    }

    fun dismissRemoval() {
        pendingRemoval = null
    }

    /** Moves [id] by [delta] positions (Alt+arrow or the Move up/down actions). */
    fun move(
        id: Int,
        delta: Int,
    ): Boolean {
        if (!isEditing) return false
        val current = model()
        return current.move(id, current.ids.indexOf(id) + delta)
    }
}

/** "Remove from Favorites?" — the confirmation a saved row's remove control asks. */
@Composable
fun RemoveFromListDialog(
    editor: SavedListEditor,
    listLabel: String,
) {
    val pending = editor.pendingRemoval ?: return
    TagMasterDialog(
        onDismissRequest = editor::dismissRemoval,
        title = stringResource(R.string.saved_list_remove_title, listLabel),
        message = stringResource(R.string.saved_list_remove_message, pending.second),
        dismiss = DialogButton(stringResource(R.string.home_cancel), onClick = editor::dismissRemoval),
        confirm = DialogButton(stringResource(R.string.saved_list_remove), onClick = editor::confirmRemoval),
    )
}

/** A hairline across the top of a row, the divider between saved rows. */
fun Modifier.topDivider(
    show: Boolean,
    color: Color,
) = if (!show) {
    this
} else {
    drawBehind {
        drawRect(color, Offset.Zero, Size(size.width, 1.dp.roundToPx().toFloat()))
    }
}

/**
 * One saved tag: the tag row once it has loaded (the compact loader in its place until then, or
 * "Failed to load tag 123" if it cannot), with the remove control and drag handle in edit mode.
 */
@Composable
fun SavedTagRow(
    id: Int,
    editing: Boolean,
    position: Int,
    count: Int,
    listLabel: String,
    selected: Boolean,
    onOpen: (Int) -> Unit,
    onRemove: (Int, String) -> Unit,
    onMove: (Int, Int) -> Boolean,
    handleModifier: (MutableInteractionSource) -> Modifier,
    modifier: Modifier = Modifier,
    loads: TagLoads? = null,
) {
    val context = LocalContext.current
    val handleInteractions = remember { MutableInteractionSource() }
    val rowInteractions = remember { MutableInteractionSource() }
    val colors = TagMasterTheme.colors
    val load = rememberTagLoad(id, loads)
    val tag = load.tag
    val displayName = tag?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.saved_list_tag_id, id)
    val removeLabel = stringResource(R.string.saved_list_remove_named, displayName, listLabel)
    val dragLabel = stringResource(R.string.saved_list_drag_named, displayName, position + 1, count, listLabel)
    val positionLabel = stringResource(R.string.saved_list_position, displayName, position + 1, count, listLabel)
    val removeAction = stringResource(R.string.saved_list_remove)
    val moveUp = stringResource(R.string.MoveUp)
    val moveDown = stringResource(R.string.MoveDown)
    val showing = stringResource(R.string.tag_row_showing)
    // The tablet's "showing" highlight fades in and out, as RecyclerView animated a changed row.
    val highlight by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f),
        tween(ListMotion.CHANGE_MILLIS),
        label = "selected",
    )
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (editing) {
                    Modifier
                        .semantics(mergeDescendants = true) {
                            contentDescription = positionLabel
                            customActions =
                                buildList {
                                    add(CustomAccessibilityAction(removeAction) { onRemove(id, displayName); true })
                                    if (position > 0) add(CustomAccessibilityAction(moveUp) { onMove(id, -1) })
                                    if (position < count - 1) add(CustomAccessibilityAction(moveDown) { onMove(id, 1) })
                                }
                        }
                        // Keyboard focus on the row shows, as the platform's default focus highlight did.
                        .indication(rowInteractions, ripple())
                        .focusable(interactionSource = rowInteractions)
                } else if (load.failed) {
                    // A tag that will not load can still be opened on the website.
                    Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Tag.getTagUri(id))))
                    }
                } else {
                    Modifier
                },
            ).testTag("savedTag:$id"),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        if (editing) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(interactionSource = null, indication = ripple(bounded = false, radius = 24.dp), role = Role.Button) {
                        onRemove(id, displayName)
                    }.semantics { contentDescription = removeLabel }
                    .testTag("remove:$id"),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(R.drawable.ic_remove_circle_outline, tint = colors.primary)
            }
        }
        Box(Modifier.weight(1f), contentAlignment = ViewAlign.Center) {
            when {
                tag != null ->
                    TagRowContent(
                        tag,
                        modifier =
                            Modifier
                                .drawBehind { if (highlight.alpha > 0f) drawRect(highlight) }
                                .then(
                                    if (editing) {
                                        Modifier.clearAndSetSemantics { hideFromAccessibility() }
                                    } else {
                                        Modifier
                                            .clickable { onOpen(id) }
                                            .semantics { if (selected) stateDescription = showing }
                                    },
                                ),
                        wrapWhenNarrow = editing,
                    )
                load.failed ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(stringResource(R.string.failed_to_load_tag), style = TagMasterType.bodyMedium, color = colors.text)
                        Text("$id", Modifier.padding(start = 4.dp), style = TagMasterType.bodyMedium, color = colors.text)
                    }
                else -> {
                    // Holds the row's height while the tag loads, as the invisible placeholder did.
                    Box(Modifier.alpha(0f).clearAndSetSemantics { }) { TagRowContent(null) }
                    CompactBarberPole(true, stringResource(R.string.home_loading_tag))
                }
            }
        }
        if (editing) {
            val enabled = count > 1
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .alpha(if (enabled) 1f else 0.38f)
                    .indication(handleInteractions, ripple(bounded = false, radius = 24.dp))
                    .then(handleModifier(handleInteractions))
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> onMove(id, -1)
                            Key.DirectionDown -> onMove(id, 1)
                            else -> false
                        }
                    }.focusable(enabled, handleInteractions)
                    .semantics {
                        contentDescription = dragLabel
                        role = Role.Button
                    }.testTag("drag:$id"),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(R.drawable.ic_drag_handle, tint = colors.onSurfaceVariant)
            }
        }
    }
}
