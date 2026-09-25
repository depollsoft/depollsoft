package depollsoft.tagmaster.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

/**
 * The width a single-line TextView gives [text]: the platform's desired width, rounded up. Compose
 * can come out a pixel wider, which moves whatever sits after the text.
 */
@Composable
fun rememberTextViewWidth(
    text: String,
    style: TextStyle,
): Int = ceil(rememberDesiredWidth(text, style)).toInt()

/** Sizes a single line of [text] to the width a wrap_content TextView gives it. */
@Composable
fun Modifier.textViewWidth(
    text: String,
    style: TextStyle,
): Modifier = widthPx(rememberTextViewWidth(text, style))

/** The platform's unrounded width for one line of [text] in [style]. */
@Composable
fun rememberDesiredWidth(
    text: String,
    style: TextStyle,
): Float {
    val paint = rememberTextViewPaint(style)
    return remember(text, paint) { android.text.Layout.getDesiredWidth(text, paint) }
}

/** The paint a TextView showing [style] measures with: size, typeface and letter spacing. */
@Composable
fun rememberTextViewPaint(style: TextStyle): TextPaint {
    val density = LocalDensity.current
    val fonts = LocalFontFamilyResolver.current
    return remember(style, density, fonts) {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = with(density) { style.fontSize.toPx() }
            typeface =
                fonts
                    .resolve(
                        style.fontFamily,
                        style.fontWeight ?: FontWeight.Normal,
                        style.fontStyle ?: FontStyle.Normal,
                        style.fontSynthesis ?: FontSynthesis.All,
                    ).value as Typeface
            val spacing = style.letterSpacing
            if (spacing.isEm) {
                letterSpacing = spacing.value
            } else if (spacing.isSp) {
                letterSpacing = with(density) { spacing.toPx() } / textSize
            }
        }
    }
}

/**
 * One line of text centered the way a gravity="center" TextView centers it. The view wraps to the
 * rounded-up line width (or takes whatever wider width its constraints ask for), and
 * `Layout.draw` starts the line at `(width - ((int) lineWidth & ~1)) >> 1`, so an odd line width
 * starts a pixel further in than Compose's own centering would.
 */
@Composable
fun ViewCenteredText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val line = rememberDesiredWidth(text, style)
    val wrapped = ceil(line).toInt()
    val even = line.toInt() and 1.inv()
    Layout({ Text(text, style = style, color = color, maxLines = 1, softWrap = false) }, modifier) { measurables, constraints ->
        val width = maxOf(wrapped, constraints.minWidth).coerceAtMost(constraints.maxWidth)
        val placeable =
            measurables.single().measure(
                Constraints(minWidth = 0, maxWidth = maxOf(width, wrapped), minHeight = 0, maxHeight = constraints.maxHeight),
            )
        val height = maxOf(placeable.height, constraints.minHeight)
        layout(width, height) {
            placeable.placeRelative((width - even) shr 1, (height - placeable.height) / 2)
        }
    }
}

/** Sizes the element to exactly [width] pixels (within the incoming constraints). */
fun Modifier.widthPx(width: Int): Modifier =
    layout { measurable, constraints ->
        val exact = width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = exact, maxWidth = exact))
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
