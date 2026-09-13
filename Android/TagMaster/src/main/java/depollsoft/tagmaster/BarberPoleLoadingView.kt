package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
        private val logo = BarberPoleLogo(resources)
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
            val scale = minOf(width / BarberPoleLogo.WIDTH, height / BarberPoleLogo.HEIGHT)
            canvas.translate((width - BarberPoleLogo.WIDTH * scale) / 2f, (height - BarberPoleLogo.HEIGHT * scale) / 2f)
            canvas.scale(scale, scale)
            paint.style = Paint.Style.FILL
            paint.color = context.getColor(R.color.md_on_surface_variant)
            canvas.drawPath(logo.metal, paint)
            paint.color = Color.WHITE
            canvas.drawPath(logo.shaft, paint)
            val cylinder = canvas.save()
            canvas.clipPath(logo.shaft)
            // In the logo's own coordinate system only stripe phase moves. The frame never rotates.
            canvas.rotate(BarberPoleLogo.AXIS_ANGLE)
            val phase = stripePhase - kotlin.math.floor(stripePhase)
            for (index in -6..6) {
                val band = canvas.save()
                canvas.translate(0f, (index + phase * 2f) * BarberPoleLogo.STRIPE_STEP)
                paint.color = if (index % 2 == 0) Color.rgb(190, 42, 53) else Color.rgb(0, 99, 165)
                canvas.drawPath(logo.stripe, paint)
                canvas.restoreToCount(band)
            }
            canvas.restoreToCount(cylinder)
            canvas.restoreToCount(saved)
        }
    }
