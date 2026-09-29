package depollsoft.pitchperfect.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import depollsoft.compose.scrollViewScrollbar

/** Whether a dialog's choice list has choices scrolled out of view above it (and its top hairline shows). */
val DialogListMoreAbove = SemanticsPropertyKey<Boolean>("DialogListMoreAbove")
var SemanticsPropertyReceiver.dialogListMoreAbove by DialogListMoreAbove

/** Whether a dialog's choice list has choices below the fold (and its bottom hairline shows). */
val DialogListMoreBelow = SemanticsPropertyKey<Boolean>("DialogListMoreBelow")
var SemanticsPropertyReceiver.dialogListMoreBelow by DialogListMoreBelow

/**
 * A dialog's scrolling list of choices that shows it scrolls: while there is more above or below,
 * a hairline marks that edge (under the title, above the buttons, as the platform's dialogs draw
 * them), the rows fade into it, and the scrollbar stays in view. When the list is taller than the
 * dialog, its height is trimmed so the last visible row is cut partway, never on a row's edge.
 * Each child (row or heading) should fill the width, so a drag or tap anywhere across it lands.
 */
@Composable
fun DialogChoiceList(
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = plateColors
    val scroll = rememberScrollState()
    val bottoms = remember { mutableStateListOf<Int>() }
    val moreAbove by remember { derivedStateOf { scroll.value > 0 } }
    val moreBelow by remember { derivedStateOf { scroll.value < scroll.maxValue } }
    // Read here so the semantics follow them.
    val above = moreAbove
    val below = moreBelow
    ChoiceColumn(
        bottoms,
        top = LIST_TOP,
        modifier =
            modifier
                .fillMaxWidth()
                .cutPartwayThroughARow(bottoms)
                .semantics {
                    dialogListMoreAbove = above
                    dialogListMoreBelow = below
                }.scrollViewScrollbar(scroll, alwaysShown = true)
                .drawWithContent {
                    drawContent()
                    if (scroll.value > 0) drawRect(colors.hairline, Offset.Zero, Size(size.width, 1f))
                    if (scroll.value < scroll.maxValue) drawRect(colors.hairline, Offset(0f, size.height - 1f), Size(size.width, 1f))
                }
                // The fade is a mask over the rows, so it works on any dialog background.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = EDGE_FADE.toPx()
                    // Ramp the fade in over the first bit of scrolling, so it never pops.
                    val topStrength = (scroll.value / fade).coerceIn(0f, 1f)
                    val bottomStrength = ((scroll.maxValue - scroll.value) / fade).coerceIn(0f, 1f)
                    if (topStrength > 0f) {
                        drawRect(
                            Brush.verticalGradient(0f to Color.Black.copy(alpha = 1f - topStrength * FADE_DEPTH), 1f to Color.Black, startY = 0f, endY = fade),
                            size = Size(size.width, fade),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    if (bottomStrength > 0f) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black,
                                1f to Color.Black.copy(alpha = 1f - bottomStrength * FADE_DEPTH),
                                startY = size.height - fade,
                                endY = size.height,
                            ),
                            topLeft = Offset(0f, size.height - fade),
                            size = Size(size.width, fade),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }.testTag(tag)
                .verticalScroll(scroll)
                .padding(start = 16.dp, end = 24.dp),
        content = content,
    )
}

/**
 * The rows, one under another from [top] down, each as wide as the list, noting where each one
 * ends so [cutPartwayThroughARow] can size the list.
 */
@Composable
private fun ChoiceColumn(
    bottoms: SnapshotStateList<Int>,
    top: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val topPx = top.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
        val ends = IntArray(placeables.size)
        var y = topPx
        placeables.forEachIndexed { index, placeable ->
            y += placeable.height
            ends[index] = y
        }
        layout(width, y) {
            if (!ends.contentEquals(bottoms.toIntArray())) {
                bottoms.clear()
                bottoms.addAll(ends.toList())
            }
            var at = topPx
            placeables.forEach {
                it.placeRelative(0, at)
                at += it.height
            }
        }
    }
}

/**
 * When the rows are taller than the room the dialog gives, ends the list halfway through the last
 * row that fits, so a row cut at the fold shows there is more.
 */
private fun Modifier.cutPartwayThroughARow(bottoms: List<Int>): Modifier =
    layout { measurable, constraints ->
        val contentHeight = bottoms.lastOrNull()
        var height = -1
        if (contentHeight != null && constraints.hasBoundedHeight && contentHeight > constraints.maxHeight) {
            bottoms.forEachIndexed { index, bottom ->
                val middle = ((if (index == 0) 0 else bottoms[index - 1]) + bottom) / 2
                if (middle <= constraints.maxHeight && middle >= constraints.minHeight) height = middle
            }
        }
        val placeable = measurable.measure(if (height > 0) constraints.copy(minHeight = height, maxHeight = height) else constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/** The space above the first row, as the lists had before. */
private val LIST_TOP = 8.dp

/** How far the rows fade into an edge with more past it. */
private val EDGE_FADE = 24.dp

/** How much of a row at that edge fades away: most of it, but it stays readable. */
private const val FADE_DEPTH = 0.75f
