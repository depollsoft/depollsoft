package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
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

/** Decorative quartet only. The enclosing native text provides the loading status. */
class TagLoadingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = QuartetArtwork.PHASESTART
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
                    ValueAnimator.ofFloat(QuartetArtwork.PHASESTART, QuartetArtwork.PHASEEND).apply {
                        duration = (QuartetArtwork.PERIOD * 1000).toLong()
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
            drawQuartet(canvas, phase, isAnimating)
        }

        // The production renderer also permits deterministic native-raster fixtures.
        internal fun drawQuartet(
            canvas: Canvas,
            phase: Float,
            moving: Boolean,
        ) {
            val save = canvas.save()
            val scale = minOf(width / QuartetArtwork.WIDTH, height / QuartetArtwork.HEIGHT)
            canvas.translate((width - QuartetArtwork.WIDTH * scale) / 2, (height - QuartetArtwork.HEIGHT * scale) / 2)
            canvas.scale(scale, scale)
            val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            paint.color = if (dark) QuartetArtwork.darkStaff else QuartetArtwork.lightStaff
            paint.alpha = (QuartetArtwork.STAFFALPHA * 255).toInt()
            paint.strokeWidth = QuartetArtwork.STAFFWIDTH
            paint.style = Paint.Style.STROKE
            canvas.drawPath(QuartetArtwork.staff, paint)
            paint.color = if (dark) QuartetArtwork.darkNote else QuartetArtwork.lightNote
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            for (voice in QuartetArtwork.x.indices) {
                val noteSave = canvas.save()
                val lift = if (moving) QuartetArtwork.translation(voice, phase) else QuartetArtwork.still[voice]
                canvas.translate(QuartetArtwork.x[voice], QuartetArtwork.y[voice] + lift)
                canvas.drawPath(QuartetArtwork.note, paint)
                canvas.restoreToCount(noteSave)
            }
            canvas.restoreToCount(save)
        }
    }
