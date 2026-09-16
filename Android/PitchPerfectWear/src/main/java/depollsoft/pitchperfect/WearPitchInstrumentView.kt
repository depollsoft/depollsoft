package depollsoft.pitchperfect

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import androidx.core.view.InputDeviceCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * The Laboratory Instrument: a custom-drawn radial pitch-pipe face.
 * Thirteen glass cells on a blackened-steel panel; the sounding cell is the
 * one luminous element on screen. Frequency digits and the octave-range
 * selector live in the ring's hole.
 */
class WearPitchInstrumentView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        var model: PitchPipeModel? = null
            set(value) {
                stopAll()
                field = value
                updateGeometry(width, height)
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
        internal var cellCenters = emptyArray<FloatArray?>()
            private set
        internal var cellRadius = 0f
            private set
        internal var ringRadius = 0f
            private set
        internal var edgeInset = 0f
            private set
        private var size = 0f
        private var panel: Bitmap? = null
        private val score = BitmapFactory.decodeResource(resources, R.drawable.panobackground)
        private var faceCx = 0f
        private var faceCy = 0f
        internal val rangeLowRect = RectF()
        internal val rangeHighRect = RectF()
        private val touchTracker =
            PitchMultiTouchTracker(
                onStart = { cell ->
                    notes().getOrNull(cell)?.play()
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onStop = { cell -> notes().getOrNull(cell)?.stop() },
            )
        private var breathePhase = 0f
        internal var breatheAnimator: ValueAnimator? = null
            private set

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
            updateGeometry(w, h)
            renderPanel(w, h)
        }

        private fun updateGeometry(w: Int, h: Int) {
            size = min(w, h).toFloat()
            faceCx = w / 2f
            faceCy = h / 2f
            edgeInset = 0.02f * size
            cellRadius = 0.085f * size
            ringRadius = 0.5f * size - edgeInset - cellRadius
            val count = notes().size
            cellCenters = arrayOfNulls(count)
            val step = 360.0 / count.coerceAtLeast(1)
            val start = -90.0 + step / 2.0
            for (i in 0 until count) {
                val angle = Math.toRadians(start + i * step)
                cellCenters[i] =
                    floatArrayOf(
                        faceCx + ringRadius * cos(angle).toFloat(),
                        faceCy + ringRadius * sin(angle).toFloat(),
                    )
            }
            // The range selector is one machined part seated in the ring's hole.
            val rangeWidth = 0.36f * size
            val rowHeight = 0.08f * size
            val rangeTop = faceCy + 0.055f * size
            rangeLowRect.set(
                faceCx - rangeWidth / 2f,
                rangeTop,
                faceCx + rangeWidth / 2f,
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

        private fun renderPanel(w: Int, h: Int) {
            panel?.recycle()
            panel = null
            if (w <= 0 || h <= 0) return
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(ground)
            val grainPaint = Paint().apply {
                color = hairline
                strokeWidth = 1f
            }
            for (y in 0 until h step 4) {
                grainPaint.alpha = 8 + ((y * 31) % 14)
                canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), grainPaint)
            }
            score?.let { tile ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = 28
                    colorFilter = PorterDuffColorFilter(inkSecondary, PorterDuff.Mode.SRC_IN)
                }
                val tileHeight = w * tile.height.toFloat() / tile.width
                val source = Rect(0, 0, tile.width, tile.height)
                var top = 0f
                while (top < h) {
                    canvas.drawBitmap(tile, source, RectF(0f, top, w.toFloat(), top + tileHeight), paint)
                    top += tileHeight
                }
            }
            panel = bitmap
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
                stopBreathing()
            }
        }

        private fun stopBreathing() {
            breatheAnimator?.cancel()
            breatheAnimator = null
            breathePhase = 0f
        }

        override fun onDetachedFromWindow() {
            stopBreathing()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            panel?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            val currentNotes = notes()
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
            manageBreathing()
        }

        private fun drawCenter(
            canvas: Canvas,
            currentNotes: List<Note>,
        ) {
            val playingNotes = currentNotes.withIndex().filter { it.value.isPlaying }
            if (playingNotes.isNotEmpty()) {
                textPaint.color = ink
                textPaint.textSize = if (playingNotes.size == 1) size * 0.15f else size * 0.08f
                val label =
                    playingNotes.joinToString(" ") { indexed ->
                        val note = indexed.value
                        when (note.accidental) {
                            Accidental.Natural -> note.friendlyName
                            else -> "${sharpNameOf(note)}\u266F"
                        } + note.octave
                    }
                val labelWidth = textPaint.measureText(label)
                if (labelWidth > size * 0.5f) textPaint.textSize *= size * 0.5f / labelWidth
                canvas.drawText(label, faceCx, faceCy - size * 0.075f, textPaint)
                monoPaint.color = ink
                monoPaint.textSize = size * 0.065f
                val chord =
                    if (playingNotes.size > 2) PitchChord.name(playingNotes.map { it.index }) else null
                if (chord != null) {
                    // A chord has a name, not a measurement: engrave it in the
                    // display face so the exclamation reads as one word.
                    textPaint.textSize = size * 0.09f
                    textPaint.letterSpacing = 0.12f
                    canvas.drawText(chord, faceCx, faceCy + size * 0.015f, textPaint)
                    textPaint.letterSpacing = 0f
                } else {
                    val readout =
                        if (playingNotes.size == 1) {
                            String.format("%.1f Hz", playingNotes[0].value.frequency)
                        } else if (playingNotes.size == 2) {
                            PitchInterval.name(playingNotes[0].index, playingNotes[1].index)
                        } else {
                            "${playingNotes.size} NOTES"
                        }
                    canvas.drawText(readout, faceCx, faceCy + size * 0.015f, monoPaint)
                }
            } else {
                monoPaint.color = withAlpha(inkSecondary, 140)
                monoPaint.textSize = size * 0.065f
                canvas.drawText("\u2014 Hz", faceCx, faceCy - size * 0.02f, monoPaint)
            }
        }

        private fun drawRangeControl(canvas: Canvas) {
            val low = model?.isFromFToF == false
            // One machined frame contains both range positions.
            fillPaint.color = withAlpha(surface, 235)
            canvas.drawRoundRect(
                rangeLowRect.left,
                rangeLowRect.top,
                rangeHighRect.right,
                rangeHighRect.bottom,
                size * 0.012f,
                size * 0.012f,
                fillPaint,
            )
            strokePaint.color = hairline
            strokePaint.strokeWidth = 1.5f
            canvas.drawRoundRect(
                rangeLowRect.left,
                rangeLowRect.top,
                rangeHighRect.right,
                rangeHighRect.bottom,
                size * 0.012f,
                size * 0.012f,
                strokePaint,
            )
            strokePaint.strokeWidth = 1f
            strokePaint.color = withAlpha(hairline, 160)
            canvas.drawLine(
                rangeLowRect.left,
                rangeLowRect.bottom,
                rangeLowRect.right,
                rangeLowRect.bottom,
                strokePaint,
            )
            drawRangeRow(canvas, rangeLowRect, context.getString(R.string.CtoC), low)
            drawRangeRow(canvas, rangeHighRect, context.getString(R.string.FtoF), !low)
        }

        private fun drawRangeRow(
            canvas: Canvas,
            rect: RectF,
            label: String,
            selected: Boolean,
        ) {
            if (selected) {
                fillPaint.color = withAlpha(ink, 26)
                canvas.drawRect(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    fillPaint,
                )
                fillPaint.color = lit
                canvas.drawCircle(
                    rect.left + rect.height() * 0.62f,
                    rect.centerY(),
                    rect.height() * 0.13f,
                    fillPaint,
                )
            }
            textPaint.color = if (selected) ink else withAlpha(inkSecondary, 190)
            textPaint.textSize = rect.height() * 0.52f
            textPaint.letterSpacing = 0.16f
            canvas.drawText(label.uppercase(), rect.centerX(), rect.centerY() + textPaint.textSize * 0.35f, textPaint)
            textPaint.letterSpacing = 0f
        }

        private fun withAlpha(
            color: Int,
            alpha: Int,
        ): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

        private fun inHole(x: Float, y: Float): Boolean =
            hypot(x - faceCx, y - faceCy) < ringRadius - 1.25f * cellRadius

        /**
         * The row of the range selector under a finger, or -1. The drawn frame is
         * only ~15dp tall on a small watch, so the hit zone is the whole strip of
         * the hole around it: padded at the sides, reaching halfway up to the
         * readout, and all the way down to the ring. Cells keep their sectors.
         */
        internal fun rangeRowAt(x: Float, y: Float): Int {
            if (size <= 0f || !inHole(x, y)) return -1
            val sidePad = 0.06f * size
            val rowHeight = rangeLowRect.height()
            if (x < rangeLowRect.left - sidePad || x > rangeLowRect.right + sidePad) return -1
            if (y < rangeLowRect.top - rowHeight * 0.5f) return -1
            return if (y < rangeLowRect.bottom) 0 else 1
        }

        internal fun cellAt(x: Float, y: Float): Int {
            if (size <= 0f || cellCenters.isEmpty()) return -1
            val dx = x - faceCx
            val dy = y - faceCy
            if (hypot(dx, dy) < ringRadius - 1.25f * cellRadius) return -1
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
            val step = 360.0 / cellCenters.size
            return floor(((angle + 90.0 + 360.0) % 360.0) / step).toInt()
        }

        private fun selectRange(high: Boolean) {
            // Stop the old range before switching; its non-overlapping notes
            // will no longer be reachable through notes() afterward.
            stopAll()
            model?.let {
                if (it.isFromFToF != high) {
                    it.isFromFToF = high
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
            stopAll()
            touchHelper.invalidateRoot()
        }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_SCROLL &&
                event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
            ) {
                val delta = event.getAxisValue(MotionEventCompat.AXIS_SCROLL)
                if (delta != 0f) selectRange(delta < 0f)
                return true
            }
            return super.onGenericMotionEvent(event)
        }

        // Individual virtual buttons expose clicks through ExploreByTouchHelper.
        @SuppressLint("ClickableViewAccessibility")
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
                        val row = rangeRowAt(x, y)
                        val onControl = row != -1 || cellAt(x, y) != -1
                        if (onControl) parent?.requestDisallowInterceptTouchEvent(true)
                        if (row != -1) {
                            selectRange(row == 1)
                            return true
                        }
                    }
                    val index = cellAt(x, y)
                    if (index != -1 && index < currentNotes.size) {
                        if (toggleMode) {
                            val note = currentNotes[index]
                            note.isPlaying = !note.isPlaying
                            if (note.isPlaying) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
            stopBreathing()
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
                when (rangeRowAt(x, y)) {
                    0 -> return rangeLowId
                    1 -> return rangeHighId
                }
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
                        node.setBoundsInParent(Rect().also { rangeLowRect.roundOut(it) })
                    }

                    rangeHighId -> {
                        node.contentDescription = context.getString(R.string.RangeHighDescription)
                        node.isSelected = model?.isFromFToF == true
                        node.setBoundsInParent(Rect().also { rangeHighRect.roundOut(it) })
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
                        val c = cellCenters.getOrNull(virtualViewId)
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
                        selectRange(false)
                    }

                    rangeHighId -> {
                        selectRange(true)
                    }

                    else -> {
                        val currentNotes = notes()
                        if (virtualViewId !in currentNotes.indices) return false
                        val note = currentNotes[virtualViewId]
                        if (toggleMode) {
                            note.isPlaying = !note.isPlaying
                            if (note.isPlaying) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } else {
                            note.play()
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
