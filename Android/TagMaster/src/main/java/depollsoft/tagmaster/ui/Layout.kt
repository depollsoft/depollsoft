package depollsoft.tagmaster.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import kotlin.math.ceil

/**
 * View-system centering. LinearLayout and FrameLayout center with integer division, so an odd
 * leftover pixel goes below (or after) the child; Compose's own alignments round it the other way.
 * Every layout that stands in for a View one uses these, so centered content lands on the same
 * pixel.
 */
object ViewAlign {
    val CenterVertically = Alignment.Vertical { size, space -> (space - size) / 2 }
    val CenterHorizontally = Alignment.Horizontal { size, space, _ -> (space - size) / 2 }
    val Center =
        Alignment { size, space, _ ->
            IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2)
        }
}

/**
 * The width a single-line TextView gives [text]: the platform's desired width, rounded up. Compose
 * can come out a pixel wider, which moves whatever sits after the text.
 */
@Composable
fun rememberTextViewWidth(
    text: String,
    style: TextStyle,
): Int {
    val density = LocalDensity.current
    val fonts = LocalFontFamilyResolver.current
    return remember(text, style, density, fonts) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = with(density) { style.fontSize.toPx() }
        paint.typeface =
            fonts
                .resolve(
                    style.fontFamily,
                    style.fontWeight ?: FontWeight.Normal,
                    style.fontStyle ?: FontStyle.Normal,
                    style.fontSynthesis ?: FontSynthesis.All,
                ).value as Typeface
        val spacing = style.letterSpacing
        if (spacing.isEm) {
            paint.letterSpacing = spacing.value
        } else if (spacing.isSp) {
            paint.letterSpacing = with(density) { spacing.toPx() } / paint.textSize
        }
        ceil(android.text.Layout.getDesiredWidth(text, paint)).toInt()
    }
}

/** Sizes the element to exactly [width] pixels (within the incoming constraints). */
fun Modifier.widthPx(width: Int): Modifier =
    layout { measurable, constraints ->
        val exact = width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = exact, maxWidth = exact))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
