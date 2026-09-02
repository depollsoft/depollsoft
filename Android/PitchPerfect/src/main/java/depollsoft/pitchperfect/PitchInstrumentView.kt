package depollsoft.pitchperfect

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * The Laboratory Instrument: a custom-drawn radial pitch-pipe face.
 * Twelve glass cells on a blackened-steel panel; the sounding cell is the
 * one luminous element on screen. Frequency digits and the octave-range
 * selector live in the ring's hole.
 */
class PitchInstrumentView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        var model: PitchPipeModel? = null
            set(value) {
                field = value
                touchHelper.invalidateRoot()
                invalidate()
            }
        var toggleMode: Boolean = false

        private fun colorOrFallback(
            resourceId: Int,
            fallback: String,
        ): Int =
            runCatching { ContextCompat.getColor(context, resourceId) }
                .getOrElse { Color.parseColor(fallback) }

        private val ground = colorOrFallback(R.color.plate_ground, "#0E0F10")
        private val surface = colorOrFallback(R.color.plate_surface, "#16181A")
        private val ink = colorOrFallback(R.color.plate_ink, "#D9DBDD")
        private val inkSecondary = colorOrFallback(R.color.plate_ink_secondary, "#898D92")
        private val hairline = colorOrFallback(R.color.plate_hairline, "#2C2F33")
        private val lit = colorOrFallback(R.color.plate_accent, "#F2EFE6")
        private val onLit = colorOrFallback(R.color.plate_on_accent, "#101214")

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val displayTypeface: Typeface =
            runCatching {
                androidx.core.content.res.ResourcesCompat
                    .getFont(context, R.font.oswald_medium)
            }.getOrNull()
                ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        private val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
            }

        init {
            textPaint.typeface = displayTypeface
        }

        private val monoPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
        private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val heritageBitmap =
            runCatching { BitmapFactory.decodeResource(resources, R.drawable.panobackground) }.getOrNull()
        private val heritagePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 26
                colorFilter = PorterDuffColorFilter(inkSecondary, PorterDuff.Mode.SRC_IN)
            }

        private var cellCenters = emptyArray<FloatArray?>()
        private var cellRadius = 0f
        private var ringRadius = 0f
        private var faceCx = 0f
        private var faceCy = 0f
        private val rangeLowRect = Rect()
        private val rangeHighRect = Rect()
        private val touchTracker =
            PitchMultiTouchTracker(
                onStart = { cell ->
                    notes().getOrNull(cell)?.play()
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onStop = { cell -> notes().getOrNull(cell)?.stop() },
            )
        private var breathePhase = 0f
        private var breatheAnimator: ValueAnimator? = null

        private val touchHelper = InstrumentTouchHelper()

        init {
            ViewCompat.setAccessibilityDelegate(this, touchHelper)
        }

        private fun notes(): List<Note> = model?.notes ?: emptyList()

        private fun sharpNameOf(note: Note): String {
            if (note.accidental == Accidental.Sharp) return note.friendlyName
            val letters = "CDEFGAB"
            val index = letters.indexOf(note.friendlyName[0])
            return letters[(index + 6) % letters.length].toString()
        }

        private fun flatNameOf(note: Note): String {
            if (note.accidental == Accidental.Flat) return note.friendlyName
            val letters = "CDEFGAB"
            val index = letters.indexOf(note.friendlyName[0])
            return letters[(index + 1) % letters.length].toString()
        }

        private fun spokenName(note: Note): String =
            when (note.accidental) {
                Accidental.Natural -> "${note.friendlyName}, octave ${note.octave}"
                else -> "${sharpNameOf(note)} sharp, ${flatNameOf(note)} flat, octave ${note.octave}"
            }

        // A physical pitch pipe engraves the accidental cells with the glyphs alone;
        // the center readout and accessibility names carry the full note names.
        private fun engravedLines(note: Note): List<String> =
            when (note.accidental) {
                Accidental.Natural -> listOf(note.friendlyName)
                else -> listOf("\u266F/\u266D")
            }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            faceCx = w / 2f
            faceCy = h * 0.44f
            ringRadius = min(w.toFloat(), h * 0.82f) * 0.365f
            val count = notes().size.coerceAtLeast(1)
            cellRadius = ringRadius * if (count > 12) 0.225f else 0.245f
            cellCenters = arrayOfNulls(count)
            val step = 360.0 / count
            val start = -90.0 - step / 2.0
            for (i in 0 until count) {
                val angle = Math.toRadians(start + i * step)
                cellCenters[i] =
                    floatArrayOf(
                        faceCx + ringRadius * cos(angle).toFloat(),
                        faceCy + ringRadius * sin(angle).toFloat(),
                    )
            }
            // The range selector is one machined part seated in the ring's hole.
            val rangeWidth = (ringRadius * 0.72f).toInt()
            val rowHeight = (ringRadius * 0.145f).toInt()
            val rangeTop = (faceCy + ringRadius * 0.20f).toInt()
            rangeLowRect.set(
                (faceCx - rangeWidth / 2f).toInt(),
                rangeTop,
                (faceCx + rangeWidth / 2f).toInt(),
                rangeTop + rowHeight,
            )
            rangeHighRect.set(
                rangeLowRect.left,
                rangeLowRect.bottom,
                rangeLowRect.right,
                rangeLowRect.bottom + rowHeight,
            )
            touchHelper.invalidateRoot()
        }

        private fun anyPlaying(): Boolean = notes().any { it.isPlaying }

        private fun manageBreathing() {
            val reduceMotion =
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            if (anyPlaying() && !reduceMotion) {
                if (breatheAnimator == null) {
                    breatheAnimator =
                        ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
                            duration = 4000
                            repeatCount = ValueAnimator.INFINITE
                            interpolator = LinearInterpolator()
                            addUpdateListener {
                                breathePhase = it.animatedValue as Float
                                invalidate()
                            }
                            start()
                        }
                }
            } else {
                breatheAnimator?.cancel()
                breatheAnimator = null
                breathePhase = 0f
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            breatheAnimator?.cancel()
            breatheAnimator = null
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val currentNotes = notes()
            canvas.drawColor(ground)

            // Brushed-metal grain: fine directional noise across the whole panel.
            strokePaint.color = hairline
            strokePaint.strokeWidth = 1f
            var grainY = 0f
            while (grainY < height) {
                strokePaint.alpha = 8 + ((grainY.toInt() * 31) % 14)
                canvas.drawLine(0f, grainY, width.toFloat(), grainY, strokePaint)
                grainY += 4f
            }

            drawHeritageBackground(canvas)

            val breath = 0.82f + 0.18f * sin(breathePhase)

            // Bloom pass under the glass so light appears to leak across the panel.
            currentNotes.forEachIndexed { i, note ->
                if (note.isPlaying && i < cellCenters.size) {
                    val c = cellCenters[i] ?: return@forEachIndexed
                    val bloomRadius = cellRadius * 2.4f
                    bloomPaint.shader =
                        RadialGradient(
                            c[0],
                            c[1],
                            bloomRadius,
                            intArrayOf(withAlpha(lit, (150 * breath).toInt()), withAlpha(lit, 0)),
                            null,
                            Shader.TileMode.CLAMP,
                        )
                    canvas.drawCircle(c[0], c[1], bloomRadius, bloomPaint)
                }
            }

            currentNotes.forEachIndexed { i, note ->
                if (i >= cellCenters.size) return@forEachIndexed
                val c = cellCenters[i] ?: return@forEachIndexed
                val playing = note.isPlaying
                fillPaint.color = if (playing) withAlpha(lit, (255 * (0.9f + 0.1f * sin(breathePhase))).toInt()) else surface
                canvas.drawCircle(c[0], c[1], cellRadius, fillPaint)
                strokePaint.color = if (playing) lit else hairline
                strokePaint.strokeWidth = if (playing) 3f else 1.5f
                canvas.drawCircle(c[0], c[1], cellRadius, strokePaint)
                // Anode ring: the fine inner rim every cell carries.
                strokePaint.color = if (playing) withAlpha(onLit, 110) else withAlpha(inkSecondary, 70)
                strokePaint.strokeWidth = 1f
                canvas.drawCircle(c[0], c[1], cellRadius * 0.86f, strokePaint)

                val lines = engravedLines(note)
                textPaint.color =
                    if (playing) {
                        onLit
                    } else if (note.accidental == Accidental.Natural) {
                        ink
                    } else {
                        inkSecondary
                    }
                if (note.accidental == Accidental.Natural) {
                    textPaint.textSize = cellRadius * 0.9f
                } else {
                    textPaint.textSize = cellRadius * 0.62f
                }
                canvas.drawText(lines[0], c[0], c[1] + textPaint.textSize * 0.35f, textPaint)
            }

            drawCenter(canvas, currentNotes)
            drawRangeControl(canvas)
            drawNameplate(canvas)
            manageBreathing()
        }

        private fun drawHeritageBackground(canvas: Canvas) {
            val bitmap = heritageBitmap ?: return
            val source = Rect(0, 0, bitmap.width, bitmap.height)
            val tileHeight = width * (bitmap.height.toFloat() / bitmap.width.toFloat())
            if (tileHeight <= 0f) return

            var tileTop = 0f
            while (tileTop < height) {
                canvas.drawBitmap(
                    bitmap,
                    source,
                    RectF(0f, tileTop, width.toFloat(), tileTop + tileHeight),
                    heritagePaint,
                )
                tileTop += tileHeight
            }
        }

        private fun drawCenter(
            canvas: Canvas,
            currentNotes: List<Note>,
        ) {
            val playingNotes = currentNotes.withIndex().filter { it.value.isPlaying }
            if (playingNotes.isNotEmpty()) {
                textPaint.color = ink
                textPaint.textSize = if (playingNotes.size == 1) ringRadius * 0.30f else ringRadius * 0.17f
                val label =
                    playingNotes.joinToString(" ") { indexed ->
                        val note = indexed.value
                        when (note.accidental) {
                            Accidental.Natural -> note.friendlyName
                            else -> "${sharpNameOf(note)}\u266F"
                        } + note.octave
                    }
                canvas.drawText(label, faceCx, faceCy - ringRadius * 0.18f, textPaint)
                monoPaint.color = ink
                monoPaint.textSize = ringRadius * 0.15f
                val readout =
                    if (playingNotes.size == 1) {
                        String.format("%.1f Hz", playingNotes[0].value.frequency)
                    } else if (playingNotes.size == 2) {
                        PitchInterval.name(playingNotes[0].index, playingNotes[1].index)
                    } else {
                        "${playingNotes.size} NOTES"
                    }
                canvas.drawText(
                    readout,
                    faceCx,
                    faceCy + ringRadius * 0.02f,
                    monoPaint,
                )
            } else {
                monoPaint.color = withAlpha(inkSecondary, 140)
                monoPaint.textSize = ringRadius * 0.15f
                canvas.drawText("\u2014 Hz", faceCx, faceCy - ringRadius * 0.04f, monoPaint)
            }
        }

        private fun drawRangeControl(canvas: Canvas) {
            val low = model?.isFromFToF == false
            // One machined frame contains both range positions.
            fillPaint.color = withAlpha(surface, 235)
            canvas.drawRoundRect(
                rangeLowRect.left.toFloat(),
                rangeLowRect.top.toFloat(),
                rangeHighRect.right.toFloat(),
                rangeHighRect.bottom.toFloat(),
                5f,
                5f,
                fillPaint,
            )
            strokePaint.color = hairline
            strokePaint.strokeWidth = 1.5f
            canvas.drawRoundRect(
                rangeLowRect.left.toFloat(),
                rangeLowRect.top.toFloat(),
                rangeHighRect.right.toFloat(),
                rangeHighRect.bottom.toFloat(),
                5f,
                5f,
                strokePaint,
            )
            strokePaint.strokeWidth = 1f
            strokePaint.color = withAlpha(hairline, 160)
            canvas.drawLine(
                rangeLowRect.left.toFloat(),
                rangeLowRect.bottom.toFloat(),
                rangeLowRect.right.toFloat(),
                rangeLowRect.bottom.toFloat(),
                strokePaint,
            )
            drawRangeRow(canvas, rangeLowRect, context.getString(R.string.CtoC), low)
            drawRangeRow(canvas, rangeHighRect, context.getString(R.string.FtoF), !low)
        }

        private fun drawRangeRow(
            canvas: Canvas,
            rect: Rect,
            label: String,
            selected: Boolean,
        ) {
            if (selected) {
                fillPaint.color = withAlpha(ink, 26)
                canvas.drawRect(
                    rect.left.toFloat(),
                    rect.top.toFloat(),
                    rect.right.toFloat(),
                    rect.bottom.toFloat(),
                    fillPaint,
                )
                fillPaint.color = lit
                canvas.drawCircle(
                    rect.left + rect.height() * 0.62f,
                    rect.exactCenterY(),
                    rect.height() * 0.13f,
                    fillPaint,
                )
            }
            textPaint.color = if (selected) ink else withAlpha(inkSecondary, 190)
            textPaint.textSize = rect.height() * 0.52f
            textPaint.letterSpacing = 0.16f
            canvas.drawText(label.uppercase(), rect.exactCenterX(), rect.exactCenterY() + textPaint.textSize * 0.35f, textPaint)
            textPaint.letterSpacing = 0f
        }

        private fun drawNameplate(canvas: Canvas) {
            textPaint.color = withAlpha(inkSecondary, 165)
            textPaint.textSize = ringRadius * 0.085f
            textPaint.letterSpacing = 0.34f
            canvas.drawText(
                "DIGITAL PITCH PIPE",
                faceCx,
                height - textPaint.textSize * 1.6f,
                textPaint,
            )
            textPaint.letterSpacing = 0f
        }

        private fun withAlpha(
            color: Int,
            alpha: Int,
        ): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

        private fun cellAt(
            x: Float,
            y: Float,
        ): Int {
            for (i in cellCenters.indices) {
                val c = cellCenters[i] ?: continue
                if (hypot((x - c[0]).toDouble(), (y - c[1]).toDouble()) <= cellRadius * 1.15) return i
            }
            return -1
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val currentNotes = notes()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        // A finger on a cell or the range selector belongs to the
                        // instrument: sliding between notes must never page the tabs.
                        val onControl =
                            rangeLowRect.contains(x.toInt(), y.toInt()) ||
                                rangeHighRect.contains(x.toInt(), y.toInt()) ||
                                cellAt(x, y) != -1
                        if (onControl) parent?.requestDisallowInterceptTouchEvent(true)
                        if (rangeLowRect.contains(x.toInt(), y.toInt())) {
                            if (model?.isFromFToF != false) {
                                model?.isFromFToF = false
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            stopEverything()
                            invalidate()
                            return true
                        }
                        if (rangeHighRect.contains(x.toInt(), y.toInt())) {
                            if (model?.isFromFToF != true) {
                                model?.isFromFToF = true
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            stopEverything()
                            invalidate()
                            return true
                        }
                    }
                    val index = cellAt(x, y)
                    if (index != -1 && index < currentNotes.size) {
                        if (toggleMode) {
                            val note = currentNotes[index]
                            note.isPlaying = !note.isPlaying
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } else {
                            touchTracker.press(pointerId, index)
                        }
                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!toggleMode) {
                        var changed = false
                        for (pointerIndex in 0 until event.pointerCount) {
                            val pointerId = event.getPointerId(pointerIndex)
                            val newCell =
                                cellAt(event.getX(pointerIndex), event.getY(pointerIndex))
                                    .takeIf { it in currentNotes.indices }
                            changed = touchTracker.move(pointerId, newCell) || changed
                        }
                        if (changed) invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    touchTracker.release(event.getPointerId(event.actionIndex))
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!toggleMode) touchTracker.clear()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        private fun stopEverything() {
            notes().forEach {
                it.isPlaying = false
                it.stop()
            }
            touchTracker.clear()
        }

        fun stopAll() {
            stopEverything()
            invalidate()
        }

        override fun dispatchHoverEvent(event: MotionEvent): Boolean =
            touchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean =
            touchHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

        override fun onFocusChanged(
            gainFocus: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?,
        ) {
            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
            touchHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        }

        private inner class InstrumentTouchHelper : ExploreByTouchHelper(this) {
            private val rangeLowId = 100
            private val rangeHighId = 101

            override fun getVirtualViewAt(
                x: Float,
                y: Float,
            ): Int {
                if (rangeLowRect.contains(x.toInt(), y.toInt())) return rangeLowId
                if (rangeHighRect.contains(x.toInt(), y.toInt())) return rangeHighId
                val cell = cellAt(x, y)
                return if (cell == -1) INVALID_ID else cell
            }

            override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
                for (i in 0 until min(cellCenters.size, notes().size)) virtualViewIds.add(i)
                virtualViewIds.add(rangeLowId)
                virtualViewIds.add(rangeHighId)
            }

            override fun onPopulateNodeForVirtualView(
                virtualViewId: Int,
                node: AccessibilityNodeInfoCompat,
            ) {
                node.className = "android.widget.Button"
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                when (virtualViewId) {
                    rangeLowId -> {
                        node.contentDescription = context.getString(R.string.RangeLowDescription)
                        node.isSelected = model?.isFromFToF == false
                        node.setBoundsInParent(rangeLowRect)
                    }

                    rangeHighId -> {
                        node.contentDescription = context.getString(R.string.RangeHighDescription)
                        node.isSelected = model?.isFromFToF == true
                        node.setBoundsInParent(rangeHighRect)
                    }

                    else -> {
                        val currentNotes = notes()
                        if (virtualViewId in currentNotes.indices) {
                            val note = currentNotes[virtualViewId]
                            node.contentDescription = spokenName(note)
                            node.isSelected = note.isPlaying
                        } else {
                            node.contentDescription = ""
                        }
                        val c = cellCenters[virtualViewId]
                        if (c != null) {
                            node.setBoundsInParent(
                                Rect(
                                    (c[0] - cellRadius).toInt(),
                                    (c[1] - cellRadius).toInt(),
                                    (c[0] + cellRadius).toInt(),
                                    (c[1] + cellRadius).toInt(),
                                ),
                            )
                        } else {
                            node.setBoundsInParent(Rect(0, 0, 1, 1))
                        }
                    }
                }
            }

            override fun onPerformActionForVirtualView(
                virtualViewId: Int,
                action: Int,
                arguments: Bundle?,
            ): Boolean {
                if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false
                when (virtualViewId) {
                    rangeLowId -> {
                        model?.isFromFToF = false
                        stopEverything()
                    }

                    rangeHighId -> {
                        model?.isFromFToF = true
                        stopEverything()
                    }

                    else -> {
                        val currentNotes = notes()
                        if (virtualViewId !in currentNotes.indices) return false
                        val note = currentNotes[virtualViewId]
                        if (toggleMode) {
                            note.isPlaying = !note.isPlaying
                        } else {
                            note.play()
                            postDelayed({
                                note.stop()
                                invalidate()
                            }, 1500)
                        }
                    }
                }
                invalidate()
                return true
            }
        }
    }
