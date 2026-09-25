package depollsoft.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * How list rows move, taken from RecyclerView's DefaultItemAnimator so every list in both apps
 * moves the same way: a row fades in or out in 120ms, rows slide to new places in 250ms, and a
 * row whose contents change cross-fades in 250ms. A drag lifts its row in [LIFT_MILLIS] and
 * settles it into its slot in [SETTLE_MILLIS].
 */
object ListMotion {
    const val FADE_MILLIS = 120
    const val MOVE_MILLIS = 250
    const val CHANGE_MILLIS = 250
    const val LIFT_MILLIS = 150
    const val SETTLE_MILLIS = 200

    /** How high a dragged row floats above the list. */
    val liftedElevation = 6.dp

    val fade: FiniteAnimationSpec<Float> = tween(FADE_MILLIS)
    val placement: FiniteAnimationSpec<IntOffset> = tween(MOVE_MILLIS, easing = FastOutSlowInEasing)

    fun <T> change(): FiniteAnimationSpec<T> = tween(CHANGE_MILLIS)
}

/**
 * Fades this row in and out as it is added or removed and slides it when it moves.
 * [animatePlacement] is false for a row a finger (or its settle) positions, which must not also
 * be slid.
 */
fun Modifier.listItemMotion(
    scope: LazyItemScope,
    animatePlacement: Boolean = true,
): Modifier =
    with(scope) {
        this@listItemMotion.animateItem(
            fadeInSpec = ListMotion.fade,
            placementSpec = if (animatePlacement) ListMotion.placement else null,
            fadeOutSpec = ListMotion.fade,
        )
    }
