package depollsoft.pitchperfect

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.compose.ReorderState
import depollsoft.compose.ViewAlign
import depollsoft.compose.listItemMotion
import depollsoft.compose.rememberReorderState
import depollsoft.compose.reorderHandle
import depollsoft.compose.reorderRow
import depollsoft.compose.shownOrder
import depollsoft.pitchperfect.ui.DrawableIcon
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateFab
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlatePopupMenu
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.PlateTopBar
import depollsoft.pitchperfect.ui.PopupMenuItem
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * The Set Lists screen's state. A drag previews a new order of the custom lists and commits it
 * once, on drop, rewriting `order` on every custom list, as the design contract requires.
 */
@Stable
class ManageSetListsState(
    val model: SongsModel,
) : SetListPrompts {
    /** The rows in the model's order: My Songs first, then the custom lists. */
    val rows: List<SongList> get() = model.orderedLists

    override var nameRequest by mutableStateOf<NameRequest?>(null)
    override var pendingDelete by mutableStateOf<String?>(null)

    fun isCustom(list: SongList): Boolean = list.id != SongsModel.DEFAULT_ID

    /** The custom lists' ids in order: what a drag reorders. My Songs stays pinned first. */
    val customIds: List<String> get() = rows.filter(::isCustom).map { it.id }

    /** Moves [list] one step among the custom lists and commits at once: the screen reader's reorder. */
    fun moveList(
        list: SongList,
        delta: Int,
    ): Boolean {
        val custom = rows.filter(::isCustom).toMutableList()
        val from = custom.indexOf(list)
        val to = from + delta
        if (from < 0 || to !in custom.indices) return false
        custom.add(to, custom.removeAt(from))
        model.reorderLists(custom.map { it.id })
        return true
    }

    fun duplicate(list: SongList) {
        model.duplicateList(list.id)
    }

    fun confirmDelete(list: SongList) {
        if (isCustom(list)) pendingDelete = list.id
    }

    fun delete(listId: String) {
        pendingDelete = null
        model.deleteList(listId)
    }
}

/**
 * "Set Lists": every list in order, with its song count, a drag handle and a row overflow. My
 * Songs is pinned first, has no handle and cannot be deleted. The rows re-render on any change,
 * including one arriving from another device.
 */
class ManageSetListsActivity : AppCompatActivity() {
    internal val state by lazy { ManageSetListsState(SongsModel.get()) }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.ManageSetListsTitle)
        keepSetListPromptsOpen(state)
        setContent {
            PlateTheme {
                Column(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    PlateTopBar(stringResource(R.string.ManageSetListsTitle), navigationUp = ::finish)
                    ManageSetListsScreen(state, onSwitch = ::switchTo)
                }
            }
        }
    }

    /** Makes [list] current and returns to the Songs tab: a tap on a list switches to it. */
    internal fun switchTo(list: SongList) {
        state.model.currentListId = list.id
        finish()
    }
}

@Composable
fun ManageSetListsScreen(
    state: ManageSetListsState,
    onSwitch: (SongList) -> Unit,
) {
    state.model.trackLists()
    val listState = rememberLazyListState()
    // Only the custom lists trade places; other devices' changes wait until the drop.
    val reorder =
        rememberReorderState<String>(listState, state, keyOf = { it }) { _, order ->
            state.model.reorderLists(order)
        }
    val byId = state.rows.associateBy { it.id }
    val rows = state.rows.filterNot(state::isCustom) + reorder.shownOrder(state.customIds, listState).mapNotNull { byId[it] }
    val surface = plateColors.surface
    PlateBackground {
        LazyColumn(
            Modifier.fillMaxSize().testTag(TestTags.MANAGE_LIST),
            state = listState,
            contentPadding = PaddingValues(bottom = 90.dp),
        ) {
            items(rows, key = { it.id }) { list ->
                Column(
                    Modifier
                        .listItemMotion(this, animatePlacement = !listState.isScrollInProgress && !reorder.isMoving(list.id))
                        .reorderRow(reorder, list.id, surface),
                ) {
                    SetListRow(state, list, reorder, onSwitch) { delta -> state.moveList(list, delta) }
                    Hairline()
                }
            }
        }
        PlateFab(
            R.drawable.ic_add_button,
            stringResource(R.string.SetListNew),
            onClick = { state.nameRequest = NameRequest() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TestTags.NEW_SET_LIST),
        )
    }
    state.nameRequest?.let { request ->
        SetListNameDialog(request.listId, onDismiss = { state.nameRequest = null }, onDone = { _, _ -> state.nameRequest = null })
    }
    state.pendingDelete?.let { listId ->
        DeleteSetListDialog(state.model, listId, onDismiss = { state.pendingDelete = null }, onDelete = { state.delete(listId) })
    }
}

@Composable
private fun SetListRow(
    state: ManageSetListsState,
    list: SongList,
    reorder: ReorderState<String>,
    onSwitch: (SongList) -> Unit,
    moveList: (delta: Int) -> Boolean,
) {
    val colors = plateColors
    val model = state.model
    val custom = state.isCustom(list)
    val current = list.id == model.currentListId
    val name = model.displayName(list)
    val count = list.songs.size
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var menuOpen by remember { mutableStateOf(false) }
    val currentDescription = stringResource(R.string.SetListCurrentDescription, name)
    val moveUp = stringResource(R.string.MoveUp)
    val moveDown = stringResource(R.string.MoveDown)
    val customRows = state.rows.filter(state::isCustom)
    val customIndex = customRows.indexOf(list)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.manageRow(list.id))
            .background(if (pressed) colors.accent else Color.Transparent)
            // A tap on a list switches to it, here as everywhere; Rename lives in the row menu.
            .clickable(interaction, indication = null) { onSwitch(list) }
            .semantics {
                if (current) contentDescription = currentDescription
                // The drag handle is a touch gesture; screen readers and switch access reorder
                // through named actions instead, one row at a time.
                if (custom) {
                    customActions =
                        listOfNotNull(
                            CustomAccessibilityAction(moveUp) { moveList(-1) }.takeIf { customIndex > 0 },
                            CustomAccessibilityAction(moveDown) { moveList(1) }
                                .takeIf { customIndex in 0 until customRows.size - 1 },
                        )
                }
            },
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(start = 8.dp)
                .size(6.dp)
                .alpha(if (current) 1f else 0f)
                .background(colors.accent, CircleShape),
        )
        PlateText(
            name,
            style = plateText(20.sp, if (pressed) colors.onAccent else colors.ink, PlateFonts.condensed),
            modifier = Modifier.weight(1f).padding(start = 6.dp, top = 14.dp, bottom = 14.dp),
        )
        PlateText(
            if (count == 0) stringResource(R.string.NoSongsCount) else pluralStringResource(R.plurals.SongCount, count, count),
            style = plateText(14.sp, if (pressed) colors.onAccent else colors.inkSecondary, PlateFonts.mono, letterSpacing = 0.06f),
            modifier = Modifier.padding(end = 8.dp),
        )
        Box {
            val overflowDescription = stringResource(R.string.SetListRowOverflow, name)
            Box(
                Modifier
                    .size(48.dp)
                    .testTag(TestTags.ROW_OVERFLOW)
                    .clickable(role = Role.Button, indication = ripple(bounded = false), interactionSource = null) { menuOpen = true }
                    .semantics { contentDescription = overflowDescription },
                contentAlignment = Alignment.Center,
            ) {
                DrawableIcon(R.drawable.ic_more_vert, colors.inkSecondary)
            }
            PlatePopupMenu(
                menuOpen,
                { menuOpen = false },
                listOfNotNull(
                    PopupMenuItem(stringResource(R.string.SetListRename)) { state.nameRequest = NameRequest(list.id) },
                    PopupMenuItem(stringResource(R.string.SetListDuplicateAction)) { state.duplicate(list) },
                    if (custom) PopupMenuItem(stringResource(R.string.SetListDelete)) { state.confirmDelete(list) } else null,
                ),
            )
        }
        val handleDescription = stringResource(R.string.SetListReorder)
        Box(
            Modifier
                .size(48.dp)
                .alpha(if (custom) 1f else 0f)
                .then(
                    if (custom) {
                        Modifier
                            .testTag(TestTags.DRAG_HANDLE)
                            .semantics { contentDescription = handleDescription }
                            .reorderHandle(reorder, list.id, { state.customIds }, enabled = true)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            DrawableIcon(R.drawable.ic_drag_handle, colors.inkSecondary)
        }
    }
}
