package depollsoft.pitchperfect

import depollsoft.compose.viewDp
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.centerVerticallyLikeViews
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * The set list selector: one machined part with N positions, drawn exactly like the instrument's
 * range selector.
 *
 * A single round-rect frame with a 1.5dp hairline stroke is painted over the scrolling positions,
 * so it stays put while they scroll inside it. Each position has a 1dp hairline trailing edge; the
 * selected one carries a 10% ink wash and the range selector's lit indicator dot.
 */
@Composable
fun SetListSelector(
    lists: List<SongList>,
    currentId: String,
    displayName: (SongList) -> String,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    positionMenu: @Composable (listId: String) -> Unit = {},
) {
    val colors = plateColors
    val scroll = rememberScrollState()
    val view = LocalView.current
    val shape = RoundedCornerShape(FRAME_RADIUS)
    val description = stringResource(R.string.SetListSelectorDescription)
    Box(
        modifier
            .semantics { contentDescription = description }
            .testTag(TestTags.SET_LIST_SELECTOR)
            .drawWithContent {
                drawContent()
                val stroke = FRAME_STROKE.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    colors.hairline,
                    Offset(inset, inset),
                    Size(size.width - stroke, size.height - stroke),
                    CornerRadius(FRAME_RADIUS.toPx()),
                    style = Stroke(stroke),
                )
            }.clip(shape),
    ) {
        // Re-rendered, and re-centred, whenever what a position shows changes.
        val signature =
            lists.joinToString("|") { "${it.id}:${displayName(it)}:${it.songs.size}" } + "#$currentId"
        val selectedIndex = lists.indexOfFirst { it.id == currentId }
        PositionsRow(scroll, selectedIndex, signature, Modifier.fadingEdges(scroll)) {
            lists.forEach { list ->
                val name = displayName(list)
                val selected = list.id == currentId
                val count = list.songs.size
                val countText =
                    if (count == 0) stringResource(R.string.NoSongsAccessibility) else pluralStringResource(R.plurals.SongCount, count, count)
                val positionDescription = stringResource(R.string.SetListPositionDescription, name, countText)
                val actionsLabel = stringResource(R.string.SetListRowActions)
                Position(
                    text = name.uppercase(),
                    selected = selected,
                    menu = { positionMenu(list.id) },
                    modifier =
                        Modifier
                            .testTag(TestTags.setListPosition(list.id))
                            .combinedClickable(
                                onClick = {
                                    if (!selected) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onSelect(list.id)
                                    }
                                },
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onLongPress(list.id)
                                },
                            ).semantics(mergeDescendants = true) {
                                contentDescription = positionDescription
                                role = Role.Button
                                this.selected = selected
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction(actionsLabel) {
                                            onLongPress(list.id)
                                            true
                                        },
                                    )
                            },
                )
            }
            AddPosition(onCreate)
        }
    }
}

/**
 * Lays the positions out like the View's LinearLayout inside a filling HorizontalScrollView: at
 * their own widths when they overflow, and otherwise sharing the spare width equally (the last
 * child, the "+" position, has a fixed width and no share).
 */
@Composable
private fun PositionsRow(
    scroll: ScrollState,
    selectedIndex: Int,
    signature: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // Where the selected position ended up, recorded by the layout for the scroll below.
    val selectedBounds = remember { IntArray(3) }
    // A switch, a new list or a rename brings the current position to the middle of the part.
    LaunchedEffect(signature) {
        withFrameNanos { }
        val (left, width, viewport) = selectedBounds.toList()
        if (width > 0) scroll.scrollTo((left - (viewport - width) / 2).coerceAtLeast(0))
    }
    // Inside the scroll the row is offered unbounded width, so the viewport is measured outside it.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val viewport = constraints.maxWidth
        PositionsLayout(scroll, viewport, selectedBounds, selectedIndex, content)
    }
}

@Composable
private fun PositionsLayout(
    scroll: ScrollState,
    viewport: Int,
    selectedBounds: IntArray,
    selectedIndex: Int,
    content: @Composable () -> Unit,
) {
    Layout(content, Modifier.horizontalScroll(scroll)) { measurables, constraints ->
        val height = constraints.maxHeight
        // Each position's own width, from its intrinsics: a measurable is measured only once.
        val natural = measurables.map { it.maxIntrinsicWidth(height) }
        val total = natural.sum()
        val widths =
            if (total < viewport) {
                // LinearLayout hands out the excess in order, each share truncated, the
                // remainder carried to the next weighted child.
                var remaining = viewport - total
                var weights = (measurables.size - 1).toFloat()
                natural.mapIndexed { index, width ->
                    if (index == natural.lastIndex) {
                        width
                    } else {
                        val share = (remaining / weights).toInt()
                        remaining -= share
                        weights -= 1f
                        width + share
                    }
                }
            } else {
                natural
            }
        val placeables = measurables.mapIndexed { index, it -> it.measure(Constraints.fixed(widths[index], height)) }
        val width = placeables.sumOf { it.width }
        selectedBounds.fill(0)
        if (selectedIndex in placeables.indices) {
            selectedBounds[0] = placeables.take(selectedIndex).sumOf { it.width }
            selectedBounds[1] = placeables[selectedIndex].width
            selectedBounds[2] = viewport
        }
        layout(width, height) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, 0)
                x += it.width
            }
        }
    }
}

@Composable
private fun Position(
    text: String,
    selected: Boolean,
    menu: @Composable () -> Unit,
    modifier: Modifier,
) {
    val colors = plateColors
    Box(
        modifier
            .fillMaxHeight()
            .widthIn(max = MAX_POSITION_WIDTH)
            .drawBehind {
                // The dot sits before the label and the hairline after the position, mirrored in a
                // right-to-left layout along with the padding.
                val rtl = layoutDirection == LayoutDirection.Rtl
                if (selected) {
                    drawRect(colors.ink.copy(alpha = WASH_ALPHA / 255f))
                    val dot = DOT_SIZE.toPx()
                    val fromStart = DOT_INSET.toPx() + dot / 2f
                    drawCircle(colors.accent, dot / 2f, Offset(if (rtl) size.width - fromStart else fromStart, size.height / 2f))
                }
                val edge = 1.dp.toPx()
                val x = if (rtl) edge / 2f else size.width - edge / 2f
                drawLine(colors.hairline, Offset(x, 0f), Offset(x, size.height), edge)
            }
    ) {
        // The position's long-press menu hangs from the position itself, outside its padding.
        menu()
        Box(
            Modifier
                // Room for the dot before the label on every position, so the label never shifts
                // when the selection moves.
                .padding(start = LEADING_PADDING, end = SIDE_PADDING)
                .centerVerticallyLikeViews(),
        ) {
            PlateText(
            text,
            style =
                plateText(
                    13.sp,
                    // Secondary ink on the unselected label, full ink on the current list.
                    if (selected) colors.ink else colors.inkSecondary,
                    PlateFonts.oswald,
                    letterSpacing = 0.16f,
                    // The selector set its sizes with setTextSize, which keeps the fraction.
                    wholePixels = false,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            )
        }
    }
}

@Composable
private fun AddPosition(onCreate: () -> Unit) {
    val colors = plateColors
    val view = LocalView.current
    val description = stringResource(R.string.SetListNew)
    Box(
        Modifier
            .fillMaxHeight()
            .widthIn(min = ADD_WIDTH, max = ADD_WIDTH)
            .testTag(TestTags.SET_LIST_ADD_POSITION)
            .combinedClickable(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onCreate()
            }).semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        PlateText(
            stringResource(R.string.SetListAddGlyph),
            style = plateText(20.sp, colors.inkSecondary, PlateFonts.oswald, wholePixels = false),
        )
    }
}

/**
 * HorizontalScrollView's fading edges: content fades toward the plate over [FADE_LENGTH] on a
 * side with more to scroll, scaled by how much more there is. The scroll counts from the start
 * edge, which is the right one in a right-to-left layout.
 */
private fun Modifier.fadingEdges(scroll: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val length = FADE_LENGTH.toPx()
            val start = (scroll.value / length).coerceIn(0f, 1f) * length
            val end = ((scroll.maxValue - scroll.value) / length).coerceIn(0f, 1f) * length
            val rtl = layoutDirection == LayoutDirection.Rtl
            val left = if (rtl) end else start
            val right = if (rtl) start else end
            if (left > 0f) {
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), 0f, left),
                    Offset.Zero,
                    Size(left, size.height),
                    blendMode = BlendMode.DstOut,
                )
            }
            if (right > 0f) {
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), size.width - right, size.width),
                    Offset(size.width - right, 0f),
                    Size(right, size.height),
                    blendMode = BlendMode.DstOut,
                )
            }
        }

private val FRAME_RADIUS = 5.dp
private val FRAME_STROKE = 1.5.dp
// The selector View sized its paddings and widths with (dp * density).toInt().
private val SIDE_PADDING @Composable get() = viewDp(14f)
private val LEADING_PADDING @Composable get() = viewDp(20f)
private val FADE_LENGTH = 24.dp
private val DOT_INSET = 8.dp
private val DOT_SIZE = 6.dp
private val ADD_WIDTH @Composable get() = viewDp(44f)
private val MAX_POSITION_WIDTH @Composable get() = viewDp(180f)
private const val WASH_ALPHA = 26
