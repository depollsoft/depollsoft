package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.KeyType
import depollsoft.pitchperfect.lib.Note
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * The key dial: thirteen key cells around the instrument ring in
 * circle-of-fifths order. C sits at twelve o'clock, sharps run clockwise,
 * flats counter-clockwise, and the enharmonic pair meets at six o'clock.
 * The chosen key's signature is engraved in the ring's hole beside a
 * Major/Minor selector, the same machined part the pitch pipe uses for its
 * octave range.
 */
class KeyDialView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        fun interface OnKeyChangeListener {
            fun onKeyChanged(key: Key)
        }

        var onKeyChange: OnKeyChangeListener? = null

        var selectedKey: Key? = null
            private set

        var isMinor: Boolean = false
            private set

        /** Selecting from code never sounds the tonic; only a tap does. */
        fun select(key: Key) {
            selectedKey = key
            isMinor = key.keyType == KeyType.Minor
            touchHelper.invalidateRoot()
            invalidate()
        }

        private val keys: List<Key>
            get() = if (isMinor) Key.getMinorKeys() else Key.getMajorKeys()

        private fun colorOrFallback(
            resourceId: Int,
            fallback: String,
        ): Int =
            runCatching { ContextCompat.getColor(context, resourceId) }
                .getOrElse { Color.parseColor(fallback) }

        private val surface = colorOrFallback(R.color.plate_surface, "#E7E8E9")
        private val ink = colorOrFallback(R.color.plate_ink, "#1C1E20")
        private val inkSecondary = colorOrFallback(R.color.plate_ink_secondary, "#55585C")
        private val hairline = colorOrFallback(R.color.plate_hairline, "#B7B9BC")
        private val lit = colorOrFallback(R.color.plate_accent, "#141618")
        private val onLit = colorOrFallback(R.color.plate_on_accent, "#F2F3F4")

        private val displayTypeface: Typeface =
            runCatching { ResourcesCompat.getFont(context, R.font.oswald_medium) }.getOrNull()
                ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        private val musicTypeface: Typeface? =
            runCatching { CommonModel.getMusiQwik() }.getOrNull()
                ?: runCatching { Typeface.createFromAsset(context.assets, "fonts/MusiQwik.ttf") }.getOrNull()
        private val accidentalTypeface: Typeface? =
            runCatching { Typeface.createFromAsset(context.assets, "fonts/NoteHedz170.ttf") }.getOrNull()

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = displayTypeface }
        private val monoPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
        private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var cellCenters = emptyArray<FloatArray>()
        private var cellRadius = 0f
        private var ringRadius = 0f
        private var faceCx = 0f
        private var faceCy = 0f
        private val majorRect = Rect()
        private val minorRect = Rect()
        private var previewNote: Note? = null
        private val stopPreviewRunnable = Runnable { stopPreview() }

        private val touchHelper = DialTouchHelper()

        init {
            ViewCompat.setAccessibilityDelegate(this, touchHelper)
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            faceCx = w / 2f
            faceCy = h / 2f
            val half = min(w, h) / 2f
            ringRadius = half * 0.80f
            cellRadius = ringRadius * 0.225f
            val count = keys.size.coerceAtLeast(1)
            val step = (2 * Math.PI / count)
            val top = (count - 1) / 2
            cellCenters =
                Array(count) { index ->
                    val angle = -Math.PI / 2 + (index - top) * step
                    floatArrayOf(
                        faceCx + ringRadius * cos(angle).toFloat(),
                        faceCy + ringRadius * sin(angle).toFloat(),
                    )
                }
            val selectorWidth = (ringRadius * 0.72f).toInt()
            val rowHeight = (ringRadius * 0.145f).toInt()
            val selectorTop = (faceCy + ringRadius * 0.30f).toInt()
            majorRect.set((faceCx - selectorWidth / 2f).toInt(), selectorTop, (faceCx + selectorWidth / 2f).toInt(), selectorTop + rowHeight)
            minorRect.set(majorRect.left, majorRect.bottom, majorRect.right, majorRect.bottom + rowHeight)
            touchHelper.invalidateRoot()
        }

        override fun onDetachedFromWindow() {
            stopPreview()
            super.onDetachedFromWindow()
        }

        private fun withAlpha(
            color: Int,
            alpha: Int,
        ): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

        private val selectedIndex: Int
            get() = selectedKey?.let { key -> keys.indexOfFirst { it == key } } ?: -1

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val currentKeys = keys
            val selected = selectedIndex

            // Bloom beneath the chosen key, as under a sounding pitch.
            cellCenters.getOrNull(selected)?.let { c ->
                val bloomRadius = cellRadius * 2.4f
                bloomPaint.shader =
                    RadialGradient(c[0], c[1], bloomRadius, intArrayOf(withAlpha(lit, 128), withAlpha(lit, 0)), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(c[0], c[1], bloomRadius, bloomPaint)
            }

            currentKeys.forEachIndexed { i, key ->
                val c = cellCenters.getOrNull(i) ?: return@forEachIndexed
                val active = i == selected
                fillPaint.color = if (active) lit else surface
                canvas.drawCircle(c[0], c[1], cellRadius, fillPaint)
                strokePaint.color = if (active) lit else hairline
                strokePaint.strokeWidth = if (active) 3f else 1.5f
                canvas.drawCircle(c[0], c[1], cellRadius, strokePaint)
                strokePaint.color = if (active) withAlpha(onLit, 110) else withAlpha(inkSecondary, 70)
                strokePaint.strokeWidth = 1f
                canvas.drawCircle(c[0], c[1], cellRadius * 0.86f, strokePaint)
                drawKeyName(canvas, key, c[0], c[1], cellRadius * 0.82f, if (active) onLit else ink)
            }

            drawHole(canvas)
        }

        /** Draws "F" plus its accidental glyph centred on (cx, cy). */
        private fun drawKeyName(
            canvas: Canvas,
            key: Key,
            cx: Float,
            cy: Float,
            size: Float,
            color: Int,
        ) {
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = size
            textPaint.color = color
            textPaint.typeface = displayTypeface
            val name = key.friendlyName
            val nameWidth = textPaint.measureText(name)
            val glyph =
                when (key.accidental) {
                    Accidental.Sharp -> if (accidentalTypeface != null) "\u00EC" else "\u266F"
                    Accidental.Flat -> if (accidentalTypeface != null) "\u00ED" else "\u266D"
                    else -> ""
                }
            val glyphSize = size * 0.9f
            var glyphWidth = 0f
            if (glyph.isNotEmpty()) {
                textPaint.typeface = accidentalTypeface ?: displayTypeface
                textPaint.textSize = glyphSize
                glyphWidth = textPaint.measureText(glyph)
            }
            val left = cx - (nameWidth + glyphWidth) / 2f
            val baseline = cy + size * 0.35f
            textPaint.typeface = displayTypeface
            textPaint.textSize = size
            canvas.drawText(name, left, baseline, textPaint)
            if (glyph.isNotEmpty()) {
                textPaint.typeface = accidentalTypeface ?: displayTypeface
                textPaint.textSize = glyphSize
                canvas.drawText(glyph, left + nameWidth, baseline, textPaint)
                textPaint.typeface = displayTypeface
            }
        }

        private fun drawHole(canvas: Canvas) {
            selectedKey?.let { key ->
                // The signature, engraved large; then the key's name and its count.
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = musicTypeface ?: displayTypeface
                textPaint.textSize = ringRadius * 0.58f
                textPaint.color = ink
                canvas.drawText(signatureGlyphs(key.numAccidentals), faceCx, faceCy - ringRadius * 0.22f, textPaint)

                val nameSize = ringRadius * 0.15f
                textPaint.typeface = displayTypeface
                textPaint.textSize = nameSize * 0.87f
                textPaint.letterSpacing = 0.12f
                val mode = if (isMinor) "  MINOR" else "  MAJOR"
                val modeWidth = textPaint.measureText(mode)
                textPaint.letterSpacing = 0f
                textPaint.textSize = nameSize
                val nameWidth = textPaint.measureText(key.friendlyName) + (if (key.accidental == Accidental.Natural) 0f else nameSize * 0.6f)
                val nameLeft = faceCx - (nameWidth + modeWidth) / 2f
                drawKeyName(canvas, key, nameLeft + nameWidth / 2f, faceCy + ringRadius * 0.04f, nameSize, ink)
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = nameSize * 0.87f
                textPaint.letterSpacing = 0.12f
                canvas.drawText(mode, nameLeft + nameWidth, faceCy + ringRadius * 0.04f + nameSize * 0.35f, textPaint)
                textPaint.letterSpacing = 0f

                monoPaint.color = inkSecondary
                monoPaint.textSize = ringRadius * 0.095f
                monoPaint.letterSpacing = 0.14f
                canvas.drawText(accidentalCount(key.numAccidentals), faceCx, faceCy + ringRadius * 0.24f, monoPaint)
                monoPaint.letterSpacing = 0f
            }

            // One machined frame contains both mode positions.
            fillPaint.color = withAlpha(surface, 235)
            canvas.drawRoundRect(majorRect.left.toFloat(), majorRect.top.toFloat(), minorRect.right.toFloat(), minorRect.bottom.toFloat(), 4f, 4f, fillPaint)
            strokePaint.color = hairline
            strokePaint.strokeWidth = 1.5f
            canvas.drawRoundRect(majorRect.left.toFloat(), majorRect.top.toFloat(), minorRect.right.toFloat(), minorRect.bottom.toFloat(), 4f, 4f, strokePaint)
            strokePaint.color = withAlpha(hairline, 150)
            strokePaint.strokeWidth = 1f
            canvas.drawLine(majorRect.left.toFloat(), majorRect.bottom.toFloat(), majorRect.right.toFloat(), majorRect.bottom.toFloat(), strokePaint)
            drawMode(canvas, majorRect, "MAJOR", !isMinor)
            drawMode(canvas, minorRect, "MINOR", isMinor)
        }

        private fun drawMode(
            canvas: Canvas,
            rect: Rect,
            label: String,
            selected: Boolean,
        ) {
            if (selected) {
                fillPaint.color = withAlpha(ink, 26)
                canvas.drawRect(rect, fillPaint)
                fillPaint.color = lit
                canvas.drawCircle(rect.left + rect.height() * 0.5f, rect.exactCenterY(), rect.height() * 0.13f, fillPaint)
            }
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = displayTypeface
            textPaint.textSize = rect.height() * 0.5f
            textPaint.letterSpacing = 0.14f
            textPaint.color = if (selected) ink else withAlpha(inkSecondary, 190)
            canvas.drawText(label, rect.exactCenterX(), rect.exactCenterY() + textPaint.textSize * 0.35f, textPaint)
            textPaint.letterSpacing = 0f
        }

        // Interaction

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> return true
                MotionEvent.ACTION_UP -> {
                    val x = event.x
                    val y = event.y
                    if (majorRect.contains(x.toInt(), y.toInt())) {
                        setMode(false)
                        return true
                    }
                    if (minorRect.contains(x.toInt(), y.toInt())) {
                        setMode(true)
                        return true
                    }
                    val index = cellAt(x, y)
                    if (index >= 0) choose(keys[index], sound = true)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun dispatchHoverEvent(event: MotionEvent): Boolean =
            touchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

        private fun cellAt(
            x: Float,
            y: Float,
        ): Int =
            cellCenters.indexOfFirst { c -> hypot(x - c[0], y - c[1]) <= cellRadius * 1.15f }

        private fun setMode(minor: Boolean) {
            if (minor == isMinor) return
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            // Keep the same signature when the mode flips: a relative key shares it.
            val accidentals = selectedKey?.numAccidentals ?: 0
            isMinor = minor
            keys.firstOrNull { it.numAccidentals == accidentals }?.let { choose(it, sound = false) }
            touchHelper.invalidateRoot()
            invalidate()
        }

        private fun choose(
            key: Key,
            sound: Boolean,
        ) {
            selectedKey = key
            if (sound) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                preview(key.note)
            }
            touchHelper.invalidateRoot()
            invalidate()
            onKeyChange?.onKeyChanged(key)
        }

        /** Sound the tonic briefly so a singer can confirm the key by ear. */
        private fun preview(note: Note) {
            stopPreview()
            note.play()
            previewNote = note
            postDelayed(stopPreviewRunnable, 700)
        }

        fun stopPreview() {
            removeCallbacks(stopPreviewRunnable)
            previewNote?.stop()
            previewNote = null
        }

        private inner class DialTouchHelper : ExploreByTouchHelper(this) {
            private val majorId = 100
            private val minorId = 101

            override fun getVirtualViewAt(
                x: Float,
                y: Float,
            ): Int {
                if (majorRect.contains(x.toInt(), y.toInt())) return majorId
                if (minorRect.contains(x.toInt(), y.toInt())) return minorId
                val cell = cellAt(x, y)
                return if (cell == -1) INVALID_ID else cell
            }

            override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
                for (i in 0 until min(cellCenters.size, keys.size)) virtualViewIds.add(i)
                virtualViewIds.add(majorId)
                virtualViewIds.add(minorId)
            }

            override fun onPopulateNodeForVirtualView(
                virtualViewId: Int,
                node: AccessibilityNodeInfoCompat,
            ) {
                node.className = "android.widget.Button"
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                when (virtualViewId) {
                    majorId -> {
                        node.contentDescription = context.getString(R.string.KeyModeMajor)
                        node.isSelected = !isMinor
                        node.setBoundsInParent(majorRect)
                    }

                    minorId -> {
                        node.contentDescription = context.getString(R.string.KeyModeMinor)
                        node.isSelected = isMinor
                        node.setBoundsInParent(minorRect)
                    }

                    else -> {
                        val key = keys.getOrNull(virtualViewId)
                        node.contentDescription = key?.let { spokenName(it) } ?: ""
                        node.isSelected = virtualViewId == selectedIndex
                        val c = cellCenters.getOrNull(virtualViewId)
                        if (c != null) {
                            node.setBoundsInParent(
                                Rect((c[0] - cellRadius).toInt(), (c[1] - cellRadius).toInt(), (c[0] + cellRadius).toInt(), (c[1] + cellRadius).toInt()),
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
                    majorId -> setMode(false)
                    minorId -> setMode(true)
                    else -> {
                        val key = keys.getOrNull(virtualViewId) ?: return false
                        choose(key, sound = true)
                    }
                }
                return true
            }
        }

        companion object {
            /** MusiQwik encodes a treble clef as "&" followed by one glyph per signature. */
            @JvmStatic
            fun signatureGlyphs(numAccidentals: Int): String {
                val count = min(abs(numAccidentals), 7)
                if (count == 0) return "&"
                return if (numAccidentals > 0) {
                    "&" + ('\u00A1' + (count - 1))
                } else {
                    "&" + if (count == 6) '\u20AC' else ('\u00A8' + (count - 1))
                }
            }

            @JvmStatic
            fun accidentalCount(numAccidentals: Int): String =
                when {
                    numAccidentals == 0 -> "NO SHARPS OR FLATS"
                    numAccidentals == 1 -> "1 SHARP"
                    numAccidentals == -1 -> "1 FLAT"
                    numAccidentals > 0 -> "$numAccidentals SHARPS"
                    else -> "${-numAccidentals} FLATS"
                }

            @JvmStatic
            fun spokenName(key: Key): String {
                val letter = key.note.friendlyName.uppercase()
                val spelled =
                    when (key.accidental) {
                        Accidental.Sharp -> "$letter sharp"
                        Accidental.Flat -> "$letter flat"
                        else -> letter
                    }
                val mode = if (key.keyType == KeyType.Minor) "minor" else "major"
                return "$spelled $mode, ${accidentalCount(key.numAccidentals).lowercase()}"
            }
        }
    }
