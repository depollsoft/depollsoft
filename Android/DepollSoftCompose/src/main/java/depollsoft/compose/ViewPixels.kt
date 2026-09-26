package depollsoft.compose

import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit

// The rules that put Compose on the pixels the View screens used. Whole-number densities (xhdpi,
// xxhdpi) hide every one of them; 420, 440 and 560dpi, most phones, show them.

/**
 * [this] size as the whole number of pixels a TextView reads a text appearance's size in
 * (`getDimensionPixelSize` rounds 14sp at 2.625x, 36.75px, to 37px), back in sp. Compose would
 * keep the fraction and draw every line a little shorter than the View's.
 */
fun TextUnit.inWholePixels(density: Density): TextUnit {
    if (!isSp) return this
    val px = with(density) { toPx() }
    val whole = if (px == 0f) 0f else maxOf(1f, (px + 0.5f).toInt().toFloat())
    // Back through the density's own conversion: with Android 14's non-linear font scaling, a
    // plain division by fontScale would undo a different curve than toPx applied.
    return with(density) { whole.toSp() }
}

/** [this] style with its size and line height in whole pixels; see [TextUnit.inWholePixels]. */
fun TextStyle.inWholePixels(density: Density): TextStyle =
    copy(fontSize = fontSize.inWholePixels(density), lineHeight = lineHeight.inWholePixels(density))

@Composable
@ReadOnlyComposable
fun TextStyle.inWholePixels(): TextStyle = inWholePixels(LocalDensity.current)

/**
 * [this] style laid out as a TextView lays out a paragraph. Its direction comes from its first
 * strong character, where Compose would use the layout direction and read untranslated English
 * right to left in a right-to-left locale (".here", "tags 2"). Unless the style already aligns
 * its text, it sits at the layout's start edge as `gravity="start"` put it: Compose's
 * [TextAlign.Start] would follow the text's own direction instead.
 */
@Composable
@ReadOnlyComposable
fun TextStyle.viewParagraph(): TextStyle =
    copy(
        textDirection = TextDirection.Content,
        textAlign =
            if (textAlign != TextAlign.Unspecified) {
                textAlign
            } else if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                TextAlign.Right
            } else {
                TextAlign.Left
            },
    )

/**
 * [dp] in pixels as the View code computed it, `(dp * displayMetrics.density).toInt()`: truncated
 * where [Density.roundToPx] rounds (4dp is 10.5px at 420dpi: the View took 10).
 */
fun Density.viewPx(dp: Float): Int = (dp * density).toInt()

fun Density.viewPx(dp: Int): Int = viewPx(dp.toFloat())

/** [dp] truncated to whole pixels as the View code did; see [viewPx]. */
@Composable
@ReadOnlyComposable
fun viewDp(dp: Float): Dp = with(LocalDensity.current) { viewPx(dp).toDp() }

/**
 * View-system centring. LinearLayout and FrameLayout centre with integer division, so an odd
 * leftover pixel goes below (or after) the child; Compose's own alignments round it the other
 * way. Layouts that stand in for View ones use these, so centred content lands on the same pixel.
 */
object ViewAlign {
    val CenterVertically = Alignment.Vertical { size, space -> (space - size) / 2 }

    /** Gravity center_horizontal, which does not mirror: the odd pixel goes to the right. */
    val CenterHorizontally = Alignment.Horizontal { size, space, _ -> (space - size) / 2 }
    val Center =
        Alignment { size, space, _ ->
            IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2)
        }

    /** Gravity center_vertical|start: at the start edge, the right in a right-to-left layout. */
    val CenterStart =
        Alignment { size, space, direction ->
            IntOffset(if (direction == LayoutDirection.Rtl) space.width - size.width else 0, (space.height - size.height) / 2)
        }

    /** Gravity center_vertical|end. */
    val CenterEnd =
        Alignment { size, space, direction ->
            IntOffset(if (direction == LayoutDirection.Rtl) 0 else space.width - size.width, (space.height - size.height) / 2)
        }

    /** A row's children packed together and centred with integer division. */
    val CenterArrangement =
        object : Arrangement.Horizontal {
            override fun Density.arrange(
                totalSize: Int,
                sizes: IntArray,
                layoutDirection: LayoutDirection,
                outPositions: IntArray,
            ) {
                var x = (totalSize - sizes.sum()) / 2
                val order = if (layoutDirection == LayoutDirection.Ltr) sizes.indices else sizes.indices.reversed()
                for (i in order) {
                    outPositions[i] = x
                    x += sizes[i]
                }
            }
        }
}
