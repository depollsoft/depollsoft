package depollsoft.compose

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

/**
 * How list rows move, taken from RecyclerView's DefaultItemAnimator so every list in both apps
 * moves the same way: a row fades in or out in 120ms, rows slide to new places in 250ms, and a
 * row whose contents change cross-fades in 250ms. A drag lifts its row in [LIFT_MILLIS] and
 * settles it into its slot in [SETTLE_MILLIS]. Every one of them follows [easing], the curve
 * those animators and ItemTouchHelper's settle took from ValueAnimator.
 */
object ListMotion {
    const val FADE_MILLIS = 120
    const val MOVE_MILLIS = 250
    const val CHANGE_MILLIS = 250
    const val LIFT_MILLIS = 150
    const val SETTLE_MILLIS = 200

    /** How high a dragged row floats above the list. */
    val liftedElevation = 6.dp

    /** ValueAnimator's default interpolator, AccelerateDecelerateInterpolator. */
    val easing = Easing { fraction -> (cos((fraction + 1) * PI) / 2 + 0.5).toFloat() }

    val fade: FiniteAnimationSpec<Float> = tween(FADE_MILLIS, easing = easing)
    val placement: FiniteAnimationSpec<IntOffset> = tween(MOVE_MILLIS, easing = easing)

    fun <T> change(): FiniteAnimationSpec<T> = tween(CHANGE_MILLIS, easing = easing)
}

/**
 * Fades this row in and out as it is added or removed and slides it when it moves.
 *
 * Pass `animatePlacement = !listState.isScrollInProgress` (and false for a row a finger or its
 * settle positions). While a list scrolls, rows fill in as they come into view and change height,
 * pushing the rows after them along; RecyclerView's item animator ran only for adapter changes, so
 * those rows jumped, where a glide would lag behind a fast scroll. A list's own changes (a row
 * added, removed or moved, a sync) happen at rest and still glide.
 */
fun Modifier.listItemMotion(
    scope: LazyItemScope,
    animatePlacement: Boolean = true,
): Modifier =
    with(scope) {
        this@listItemMotion.animateItem(
            fadeInSpec = ListMotion.fade,
            // snap(), not null: a null spec stops the list tracking where the row was, so the frame
            // that turns gliding back on would have nothing to glide from.
            placementSpec = if (animatePlacement) ListMotion.placement else snap(),
            fadeOutSpec = ListMotion.fade,
        )
    }
