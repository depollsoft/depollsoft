package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner

/** App-local pending indicator. Artwork is shared; hosts bind Loading, never visibility alone. */
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
        private val compact =
            context.obtainStyledAttributes(attrs, R.styleable.BarberPoleLoadingView).let {
                try {
                    it.getBoolean(R.styleable.BarberPoleLoadingView_barberPoleCompact, false)
                } finally {
                    it.recycle()
                }
            }
        private var observedLifecycle: Lifecycle? = null
        private val lifecycleObserver = LifecycleEventObserver { _, _ -> updateMotion() }
        private var resumedOverride: Boolean? = null
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
                if (field == value) return
                field = value
                visibility =
                    if (value) {
                        VISIBLE
                    } else if (compact) {
                        INVISIBLE
                    } else {
                        GONE
                    }
                // A next-page request can begin inside ListView layout; coalesce a new traversal.
                removeCallbacks(relayout)
                post(relayout)
                updateMotion()
            }

        // Query pagers may be retained/offscreen while their activity is resumed.
        // Their explicit host gate remains authoritative; other hosts use the view tree.
        var hostResumed: Boolean
            get() = resumedOverride ?: (observedLifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true)
            set(value) {
                resumedOverride = value
                updateMotion()
            }
        private val layoutObserver =
            ViewTreeObserver.OnPreDrawListener {
                observeLifecycle()
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

        private fun observeLifecycle() {
            val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
            if (observedLifecycle !== lifecycle) {
                observedLifecycle?.removeObserver(lifecycleObserver)
                observedLifecycle = lifecycle
                lifecycle?.addObserver(lifecycleObserver)
            }
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val density = resources.displayMetrics.density
            val w = if (compact) BarberPoleLogo.COMPACT_WIDTH else BarberPoleLogo.ARTWORK_WIDTH
            val h = if (compact) BarberPoleLogo.COMPACT_HEIGHT else BarberPoleLogo.ARTWORK_HEIGHT
            setMeasuredDimension(
                resolveSize(kotlin.math.ceil(w * density).toInt(), widthMeasureSpec),
                resolveSize(kotlin.math.ceil(h * density).toInt(), heightMeasureSpec),
            )
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
                        duration = (BarberPoleLogo.DURATION_SECONDS * 1000).toLong()
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
            observeLifecycle()
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
            observedLifecycle?.removeObserver(lifecycleObserver)
            observedLifecycle = null
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
            val fit = minOf(width / BarberPoleLogo.WIDTH, height / BarberPoleLogo.HEIGHT)
            val scale =
                if (compact) {
                    minOf(
                        fit,
                        BarberPoleLogo.COMPACT_HEIGHT * resources.displayMetrics.density / BarberPoleLogo.HEIGHT,
                    )
                } else {
                    fit
                }
            canvas.translate((width - BarberPoleLogo.WIDTH * scale) / 2f, (height - BarberPoleLogo.HEIGHT * scale) / 2f)
            canvas.scale(scale, scale)
            paint.style = Paint.Style.FILL
            paint.color =
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    BarberPoleLogo.METAL_DARK
                } else {
                    BarberPoleLogo.METAL_LIGHT
                }
            canvas.drawPath(logo.metal, paint)
            paint.color = BarberPoleLogo.WHITE
            canvas.drawPath(logo.shaft, paint)
            val cylinder = canvas.save()
            canvas.clipPath(logo.shaft)
            // In the logo's own coordinate system only stripe phase moves. The frame never rotates.
            canvas.rotate(BarberPoleLogo.AXIS_ANGLE)
            val phase = stripePhase - kotlin.math.floor(stripePhase)
            for (index in BarberPoleLogo.REPEAT_MIN..BarberPoleLogo.REPEAT_MAX) {
                val band = canvas.save()
                canvas.translate(0f, (index + phase * BarberPoleLogo.PHASE_MULTIPLIER) * BarberPoleLogo.STRIPE_STEP)
                paint.color = if (index % 2 == 0) BarberPoleLogo.RED else BarberPoleLogo.BLUE
                canvas.drawPath(logo.stripe, paint)
                canvas.restoreToCount(band)
            }
            canvas.restoreToCount(cylinder)
            canvas.restoreToCount(saved)
        }
    }
