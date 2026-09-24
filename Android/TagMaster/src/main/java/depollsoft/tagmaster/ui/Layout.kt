package depollsoft.tagmaster.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset

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
