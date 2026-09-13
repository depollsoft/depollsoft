package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator

/** A stationary barber pole with clipped stripes for a real pending tag query. */
class BarberPoleLoadingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stripe = Path()
        private val visibleBounds = Rect()
        private var initialized = false
        private var phase = 0f
        private var animator: ValueAnimator? = null
        internal val isAnimating: Boolean get() = animator?.isStarted == true
        private val relayout =
            Runnable {
                requestLayout()
                (parent as? View)?.requestLayout()
            }
        var loading = false
            set(value) {
                field = value
                visibility = if (value) VISIBLE else GONE
                // A next-page request can begin inside ListView layout; coalesce a new traversal.
                removeCallbacks(relayout)
                post(relayout)
                updateMotion()
            }
        var hostResumed = false
            set(value) {
                field = value
                updateMotion()
            }
        private val layoutObserver =
            ViewTreeObserver.OnPreDrawListener {
                updateMotion()
                true
            }
        private val motionObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = updateMotion()
            }

        init {
            initialized = true
        }

        private fun updateMotion() {
            // View can dispatch visibility callbacks before this subclass's fields are initialized.
            if (!initialized) return
            val run =
                loading && hostResumed && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
                    getGlobalVisibleRect(visibleBounds) &&
                    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
            if (run && animator?.isStarted != true) {
                animator =
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 2000
                        repeatCount = ValueAnimator.INFINITE
                        interpolator = LinearInterpolator()
                        addUpdateListener {
                            phase = it.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
            } else if (!run) {
                stopMotion()
            }
        }

        private fun stopMotion() {
            if (animator == null && phase == 0f) return
            animator?.cancel()
            animator?.removeAllUpdateListeners()
            animator = null
            phase = 0f
            invalidate()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            viewTreeObserver.addOnPreDrawListener(layoutObserver)
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                motionObserver,
            )
            updateMotion()
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(relayout)
            viewTreeObserver.removeOnPreDrawListener(layoutObserver)
            context.contentResolver.unregisterContentObserver(motionObserver)
            stopMotion()
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
            drawPole(canvas, phase)
        }

        internal fun drawPole(
            canvas: Canvas,
            stripePhase: Float,
        ) {
            val saved = canvas.save()
            canvas.scale(width / 28f, height / 52f)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRect(5f, 10f, 23f, 42f, paint)
            val cylinder = canvas.save()
            canvas.clipRect(5f, 10f, 23f, 42f)
            for (index in -2..3) {
                for (band in 0..1) {
                    // Traditional pole colors stay inside the cylinder, never in app chrome.
                    paint.color = if (band == 0) Color.rgb(190, 42, 53) else Color.rgb(0, 99, 165)
                    val y = index * 24f + band * 12f + stripePhase * 24f
                    stripe.reset()
                    stripe.moveTo(5f, y)
                    stripe.lineTo(23f, y - 12f)
                    stripe.lineTo(23f, y - 6f)
                    stripe.lineTo(5f, y + 6f)
                    stripe.close()
                    canvas.drawPath(stripe, paint)
                }
            }
            canvas.restoreToCount(cylinder)
            paint.color = context.getColor(R.color.md_on_surface_variant)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            canvas.drawRect(5f, 10f, 23f, 42f, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(3f, 6f, 25f, 10f, 2f, 2f, paint)
            canvas.drawRoundRect(3f, 42f, 25f, 46f, 2f, 2f, paint)
            canvas.drawRoundRect(8f, 2f, 20f, 6f, 2f, 2f, paint)
            canvas.drawRoundRect(8f, 46f, 20f, 50f, 2f, 2f, paint)
            canvas.restoreToCount(saved)
        }
    }
