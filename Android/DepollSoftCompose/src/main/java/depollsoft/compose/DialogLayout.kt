package depollsoft.compose

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether [title] fits on one line of [style] in AppCompat's DialogTitle rule. A wrap-content
 * dialog window is first measured at the platform's preferred dialog width, [firstPassWidth]
 * ([dialogFirstPassWidth]: 320dp on a phone), less the card's [cardInset] and the title's
 * [titlePadding] on each side. A title that would ellipsize in that pass switches for good to a
 * smaller size over two lines, however wide the dialog then opens. [finalWidth] also caps the
 * measure, for a dialog narrower than that first pass.
 */
fun TextMeasurer.dialogTitleFits(
    title: String,
    style: TextStyle,
    density: Density,
    cardInset: Dp,
    titlePadding: Dp = 24.dp,
    finalWidth: Int = Int.MAX_VALUE,
    firstPassWidth: Dp = 320.dp,
): Boolean {
    val firstPass = with(density) { firstPassWidth.roundToPx() - 2 * cardInset.roundToPx() - 2 * titlePadding.roundToPx() }
    return !measure(title, style, maxLines = 1, constraints = Constraints(maxWidth = minOf(firstPass, finalWidth))).hasVisualOverflow
}

/**
 * The width a wrap-content dialog window is first measured at: AppCompat's
 * `abc_config_prefDialogWidth`, 320dp, 440dp on a large screen and 580dp from sw600dp.
 */
@Composable
@ReadOnlyComposable
fun dialogFirstPassWidth(): Dp = dialogFirstPassWidth(LocalConfiguration.current)

internal fun dialogFirstPassWidth(configuration: Configuration): Dp =
    when {
        configuration.smallestScreenWidthDp >= 600 -> 580.dp
        configuration.screenSize() >= Configuration.SCREENLAYOUT_SIZE_LARGE -> 440.dp
        else -> 320.dp
    }

/**
 * The least share of the screen's width a wrap-content dialog window takes, as the platform
 * applied AppCompat's `windowMinWidthMinor` (the screen is portrait) or `windowMinWidthMajor`
 * (landscape): 95% or 65% on a phone, 80% or 55% on a large screen, 72% or 45% on an extra-large
 * one.
 */
@Composable
@ReadOnlyComposable
fun dialogMinWidthFraction(): Float = dialogMinWidthFraction(LocalConfiguration.current)

internal fun dialogMinWidthFraction(configuration: Configuration): Float {
    val portrait = configuration.screenWidthDp < configuration.screenHeightDp
    return when (configuration.screenSize()) {
        Configuration.SCREENLAYOUT_SIZE_XLARGE -> if (portrait) 0.72f else 0.45f
        Configuration.SCREENLAYOUT_SIZE_LARGE -> if (portrait) 0.80f else 0.55f
        else -> if (portrait) DIALOG_MIN_WIDTH_FRACTION else 0.65f
    }
}

private fun Configuration.screenSize() = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

/**
 * A dialog window's width as AppCompat sized a wrap-content one: its content's own width, at
 * least [dialogMinWidthFraction] of the screen, and never wider than the [screenWidth] (pixels).
 */
@Composable
fun Modifier.dialogWindowWidth(screenWidth: Int): Modifier {
    val fraction = dialogMinWidthFraction()
    return layout { measurable, constraints ->
        val minimum = (screenWidth * fraction).toInt()
        val width =
            measurable.maxIntrinsicWidth(constraints.maxHeight)
                .coerceIn(minimum.coerceAtMost(screenWidth), screenWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

/** AppCompat's `windowMinWidthMinor` on a phone: a dialog window is at least 95% of a portrait screen. */
const val DIALOG_MIN_WIDTH_FRACTION = 0.95f
