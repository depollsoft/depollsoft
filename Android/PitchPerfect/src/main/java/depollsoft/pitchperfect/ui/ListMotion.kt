package depollsoft.pitchperfect.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * How rows move when a list changes, taken from RecyclerView's DefaultItemAnimator so every list
 * in the app moves the same way: rows fade in and out over 120ms, slide to new places over 250ms,
 * and a row whose contents change cross-fades over 250ms.
 */
object ListMotion {
    const val FADE_MS = 120
    const val MOVE_MS = 250
    const val CHANGE_MS = 250

    fun fade(): FiniteAnimationSpec<Float> = tween(FADE_MS)

    fun placement(): FiniteAnimationSpec<IntOffset> = tween(MOVE_MS, easing = FastOutSlowInEasing)

    fun <T> change(): FiniteAnimationSpec<T> = tween(CHANGE_MS)
}
