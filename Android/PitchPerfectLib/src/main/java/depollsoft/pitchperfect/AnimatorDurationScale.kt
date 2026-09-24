package depollsoft.pitchperfect

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * The system's "Animator duration scale", which a ValueAnimator stretched its duration by. Compose
 * only honours it when it is 0 (animations off), so a hand-timed loop that stood in for a
 * ValueAnimator multiplies its period by this.
 */
fun animatorDurationScale(context: Context): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.getDurationScale()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
