package depollsoft.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether [title] fits on one line of [style] in AppCompat's DialogTitle rule. A wrap-content
 * dialog window is first measured at the platform's preferred dialog width, 320dp, less the
 * card's [cardInset] and the title's [titlePadding] on each side. A title that would ellipsize in
 * that pass switches for good to a smaller size over two lines, however wide the dialog then
 * opens. [finalWidth] also caps the measure, for a dialog narrower than that first pass.
 */
fun TextMeasurer.dialogTitleFits(
    title: String,
    style: TextStyle,
    density: Density,
    cardInset: Dp,
    titlePadding: Dp = 24.dp,
    finalWidth: Int = Int.MAX_VALUE,
): Boolean {
    val firstPass = with(density) { 320.dp.roundToPx() - 2 * cardInset.roundToPx() - 2 * titlePadding.roundToPx() }
    return !measure(title, style, maxLines = 1, constraints = Constraints(maxWidth = minOf(firstPass, finalWidth))).hasVisualOverflow
}

/**
 * A dialog window's width as AppCompat sized a wrap-content one: its content's own width, at
 * least 95% of a portrait screen (`windowMinWidthMinor`), and never wider than the
 * [screenWidth] (pixels).
 */
fun Modifier.dialogWindowWidth(screenWidth: Int): Modifier =
    layout { measurable, constraints ->
        val minimum = (screenWidth * DIALOG_MIN_WIDTH_FRACTION).toInt()
        val width =
            measurable.maxIntrinsicWidth(constraints.maxHeight)
                .coerceIn(minimum.coerceAtMost(screenWidth), screenWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/** AppCompat's `windowMinWidthMinor`: a dialog window is at least 95% of a portrait screen. */
const val DIALOG_MIN_WIDTH_FRACTION = 0.95f
