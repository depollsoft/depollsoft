package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/** Decorative quartet only. The enclosing native text provides the loading status. */
class TagLoadingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f
        private val visibleBounds = Rect()
        private val layoutObserver = ViewTreeObserver.OnGlobalLayoutListener { updateMotion() }
        private val scrollObserver = ViewTreeObserver.OnScrollChangedListener { updateMotion() }
        private var animator: ValueAnimator? = null
        internal val isAnimating: Boolean get() = animator?.isStarted == true
        var loading = false
            set(value) {
                field = value
                updateMotion()
            }
        var hostResumed = false
            set(value) {
                field = value
                updateMotion()
            }
        private val motionObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = updateMotion()
            }

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        private fun motionAllowed() =
            if (Build.VERSION.SDK_INT >= 26) {
                ValueAnimator.areAnimatorsEnabled()
            } else {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
            }

        private fun updateMotion() {
            val run =
                loading && hostResumed && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
                    getGlobalVisibleRect(visibleBounds) &&
                    motionAllowed()
            if (run && animator == null) {
                animator =
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 2800
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = LinearInterpolator()
                        addUpdateListener {
                            phase = it.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
            } else if (!run) {
                animator?.cancel()
                animator?.removeAllUpdateListeners()
                animator = null
                phase = 0f
                invalidate()
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            viewTreeObserver.addOnGlobalLayoutListener(layoutObserver)
            viewTreeObserver.addOnScrollChangedListener(scrollObserver)
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                motionObserver,
            )
            updateMotion()
        }

        override fun onDetachedFromWindow() {
            viewTreeObserver.removeOnGlobalLayoutListener(layoutObserver)
            viewTreeObserver.removeOnScrollChangedListener(scrollObserver)
            context.contentResolver.unregisterContentObserver(motionObserver)
            animator?.cancel()
            animator?.removeAllUpdateListeners()
            animator = null
            phase = 0f
            super.onDetachedFromWindow()
        }

        override fun onVisibilityChanged(
            changedView: View,
            visibility: Int,
        ) {
            super.onVisibilityChanged(changedView, visibility)
            updateMotion()
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            updateMotion()
        }

        override fun onVisibilityAggregated(isVisible: Boolean) {
            super.onVisibilityAggregated(isVisible)
            updateMotion()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val save = canvas.save()
            canvas.scale(width / 216f, height / 96f)
            paint.color = context.getColor(R.color.md_on_surface_variant)
            paint.alpha = 75
            paint.strokeWidth = 1f
            for (line in 0..4) canvas.drawLine(8f, 32f + line * 9f, 208f, 32f + line * 9f, paint)
            paint.color = context.getColor(R.color.md_primary)
            paint.alpha = 255
            for (voice in 0..3) {
                val local = (phase - voice * 0.12f + 1f) % 1f
                // A short gentle gathering gesture, then rest. Static mode is the settled quartet.
                val lift = if (isAnimating && local < 0.45f) sin(local / 0.45f * PI).toFloat() * 4f else 0f
                val x = 43f + voice * 43f
                val y = 59f - voice * 9f - lift
                val noteSave = canvas.save()
                canvas.rotate(-18f, x, y)
                canvas.drawOval(x - 7f, y - 4.5f, x + 7f, y + 4.5f, paint)
                canvas.restoreToCount(noteSave)
                paint.strokeWidth = 1.8f
                canvas.drawLine(x + 6f, y, x + 6f, y - 25f, paint)
            }
            canvas.restoreToCount(save)
        }
    }
