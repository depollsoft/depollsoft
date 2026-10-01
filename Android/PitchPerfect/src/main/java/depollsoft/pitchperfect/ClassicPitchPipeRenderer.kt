package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.content.ContextCompat
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateFonts
import kotlin.math.sin

/**
 * Paints the classic pitch pipe: the old grid of big buttons, in the Laboratory Instrument's
 * materials. Resting buttons are surface plates with a hairline rim; a sounding one lights as a
 * radial cell does, with its glow breathing round it. The middle carries the radial face's
 * readout above the range choices, radio buttons drawn as the Settings screen draws its own.
 */
class ClassicPitchPipeRenderer(
    private val context: Context,
) {
    private val surface = ContextCompat.getColor(context, R.color.plate_surface)
    private val ink = ContextCompat.getColor(context, R.color.plate_ink)
    private val inkSecondary = ContextCompat.getColor(context, R.color.plate_ink_secondary)
    private val hairline = ContextCompat.getColor(context, R.color.plate_hairline)
    private val lit = ContextCompat.getColor(context, R.color.plate_accent)
    private val onLit = ContextCompat.getColor(context, R.color.plate_on_accent)
    private val rangeLabels = rangeLabels(context)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val displayPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = runCatching { PlateFonts.oswaldTypeface(context) }.getOrDefault(Typeface.DEFAULT)
        }
    private val monoPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
    private val labelPaint = rangeLabelPaint(context)
    private val bounds = RectF()

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density

    private fun sp(value: Float): Float = sp(context, value)

    fun draw(
        canvas: Canvas,
        geometry: ClassicPitchPipeGeometry,
        notes: List<Note>,
        isFromFToF: Boolean,
        breathePhase: Float,
    ) {
        val breath = 0.82f + 0.18f * sin(breathePhase)
        val cells = minOf(notes.size, geometry.cellCenters.size)

        // Every glow first, so no neighbour's glow lies over a button.
        for (i in 0 until cells) {
            if (notes[i].isPlaying) drawGlow(canvas, geometry.button(i), breath)
        }
        // One size for every label, from a single-slot button, so the wide cell's matches.
        val single = geometry.singleButton
        val naturalSize = maxOf(sp(NATURAL_SP), minOf(single.width(), single.height()) * NATURAL_OF_BUTTON)
        for (i in 0 until cells) drawButton(canvas, geometry.button(i), notes[i], naturalSize, breathePhase)

        drawReadout(canvas, geometry.readout, notes.take(cells))
        drawRangeChoice(canvas, geometry.rangeRow(0), rangeLabels.first, selected = !isFromFToF)
        drawRangeChoice(canvas, geometry.rangeRow(1), rangeLabels.second, selected = isFromFToF)
    }

    /** Light leaking round a sounding button: stacked, widening plates, each a little fainter. */
    private fun drawGlow(
        canvas: Canvas,
        button: RectF,
        breath: Float,
    ) {
        val reach = dp(GLOW_DP)
        for (step in GLOW_STEPS downTo 1) {
            val spread = reach * step / GLOW_STEPS
            bounds.set(button)
            bounds.inset(-spread, -spread)
            fillPaint.color = withAlpha(lit, (GLOW_ALPHA * breath).toInt())
            canvas.drawRoundRect(bounds, dp(CORNER_DP) + spread, dp(CORNER_DP) + spread, fillPaint)
        }
    }

    private fun drawButton(
        canvas: Canvas,
        button: RectF,
        note: Note,
        naturalSize: Float,
        breathePhase: Float,
    ) {
        val playing = note.isPlaying
        val corner = dp(CORNER_DP)
        fillPaint.color = if (playing) withAlpha(lit, (255 * (0.9f + 0.1f * sin(breathePhase))).toInt()) else surface
        canvas.drawRoundRect(button, corner, corner, fillPaint)
        strokePaint.color = if (playing) lit else hairline
        strokePaint.strokeWidth = if (playing) 3f else 1.5f
        canvas.drawRoundRect(button, corner, corner, strokePaint)

        val natural = note.accidental == Accidental.Natural
        displayPaint.color =
            when {
                playing -> onLit
                natural -> ink
                else -> inkSecondary
            }
        val label = NoteNames.engraved(note)
        displayPaint.textSize = if (natural) naturalSize else naturalSize * ACCIDENTAL_OF_NATURAL
        fitWithin(displayPaint, label, button.width() * 0.8f, button.height() * 0.6f)
        canvas.drawText(label, button.centerX(), button.centerY() + displayPaint.textSize * 0.35f, displayPaint)
    }

    /**
     * The radial face's readout, as one block centred in [area]: the sounding notes' names, then
     * a frequency, an interval, a chord's name or a count. Idle, a dimmed "— Hz".
     */
    private fun drawReadout(
        canvas: Canvas,
        area: RectF,
        notes: List<Note>,
    ) {
        if (area.width() <= 0f || area.height() <= 0f) return
        if (area.height() < dp(TWO_LINE_MIN_DP)) {
            drawReadoutLine(canvas, area, notes)
            return
        }
        val nameSize = minOf(area.height() * 0.46f, area.width() * 0.22f)
        val detailSize = nameSize * 0.5f
        val maxWidth = area.width() * 0.92f
        val playing = notes.withIndex().filter { it.value.isPlaying }
        if (playing.isEmpty()) {
            monoPaint.color = withAlpha(inkSecondary, 140)
            monoPaint.textSize = detailSize
            fitWithin(monoPaint, IDLE, maxWidth, area.height())
            canvas.drawText(IDLE, area.centerX(), area.centerY() + centreOffset(monoPaint), monoPaint)
            return
        }

        displayPaint.color = ink
        displayPaint.textSize = nameSize
        val names = playing.joinToString(" ") { NoteNames.readout(it.value) }
        fitWithin(displayPaint, names, maxWidth, area.height())

        val chord = chordName(playing)
        // A chord has a name, not a measurement: it is engraved in the display face.
        val detailPaint = if (chord != null) displayPaint else monoPaint
        val detail = chord ?: measurement(playing)

        // Names and detail as one block, centred in the area.
        val nameHeight = displayPaint.textSize
        val gap = nameSize * 0.15f
        val detailHeight = if (chord != null) nameSize * CHORD_OF_NAME else detailSize
        val top = area.centerY() - (nameHeight + gap + detailHeight) / 2f
        canvas.drawText(names, area.centerX(), top + nameHeight * 0.85f, displayPaint)

        detailPaint.color = ink
        detailPaint.textSize = detailHeight
        if (chord != null) displayPaint.letterSpacing = 0.12f
        fitWithin(detailPaint, detail, maxWidth, detailHeight)
        canvas.drawText(detail, area.centerX(), top + nameHeight + gap + detailPaint.textSize * 0.85f, detailPaint)
        displayPaint.letterSpacing = 0f
    }

    /**
     * The readout on one line, for a well too short for two (a turned phone): the names, then
     * the detail beside them, centred in [area].
     */
    private fun drawReadoutLine(
        canvas: Canvas,
        area: RectF,
        notes: List<Note>,
    ) {
        val lineSize = area.height() * 0.6f
        val maxWidth = area.width() * 0.92f
        val playing = notes.withIndex().filter { it.value.isPlaying }
        if (playing.isEmpty()) {
            monoPaint.color = withAlpha(inkSecondary, 140)
            monoPaint.textSize = lineSize * 0.7f
            fitWithin(monoPaint, IDLE, maxWidth, area.height())
            canvas.drawText(IDLE, area.centerX(), area.centerY() + centreOffset(monoPaint), monoPaint)
            return
        }
        val names = playing.joinToString(" ") { NoteNames.readout(it.value) } + "  "
        val chord = chordName(playing)
        val detail = chord ?: measurement(playing)
        val detailPaint = if (chord != null) Paint(displayPaint).apply { letterSpacing = 0.12f } else monoPaint
        displayPaint.color = ink
        displayPaint.textSize = lineSize
        detailPaint.color = ink
        detailPaint.textSize = lineSize * if (chord != null) 0.8f else 0.7f
        // Both parts shrink together until the line fits.
        val width = displayPaint.measureText(names) + detailPaint.measureText(detail)
        if (width > maxWidth) {
            val scale = maxWidth / width
            displayPaint.textSize *= scale
            detailPaint.textSize *= scale
        }
        val namesWidth = displayPaint.measureText(names)
        val start = area.centerX() - (namesWidth + detailPaint.measureText(detail)) / 2f
        val baseline = area.centerY() + centreOffset(displayPaint)
        displayPaint.textAlign = Paint.Align.LEFT
        detailPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(names, start, baseline, displayPaint)
        canvas.drawText(detail, start + namesWidth, baseline, detailPaint)
        displayPaint.textAlign = Paint.Align.CENTER
        monoPaint.textAlign = Paint.Align.CENTER
    }

    /** The name of the chord [playing] sounds, when it has one. */
    private fun chordName(playing: List<IndexedValue<Note>>): String? =
        if (playing.size > 2) PitchChord.name(playing.map { it.index }) else null

    /** What the readout measures: a frequency, an interval or a count. */
    private fun measurement(playing: List<IndexedValue<Note>>): String =
        when (playing.size) {
            1 -> String.format("%.1f Hz", playing[0].value.tunedFrequency)
            2 -> PitchInterval.name(playing[0].index, playing[1].index)
            else -> "${playing.size} NOTES"
        }

    /** A radio button: its 20dp ring (with a dot when chosen) in a 32dp column, then the label. */
    private fun drawRangeChoice(
        canvas: Canvas,
        row: RectF,
        label: String,
        selected: Boolean,
    ) {
        val cx = row.left + dp(ClassicPitchPipeGeometry.RADIO_DP / 2f)
        val cy = row.centerY()
        val stroke = dp(2f)
        val ring = if (selected) lit else withAlpha(ink, (255 * UNSELECTED_RING_ALPHA).toInt())
        strokePaint.color = ring
        strokePaint.strokeWidth = stroke
        canvas.drawCircle(cx, cy, dp(10f) - stroke / 2f, strokePaint)
        if (selected) {
            fillPaint.color = ring
            canvas.drawCircle(cx, cy, dp(5f), fillPaint)
        }

        labelPaint.color = ink
        labelPaint.textSize = sp(LABEL_SP)
        val start = row.left + dp(ClassicPitchPipeGeometry.RADIO_DP)
        fitWithin(labelPaint, label, row.right - start, row.height())
        canvas.drawText(label, start, cy + centreOffset(labelPaint), labelPaint)
    }

    /** How far below a line's centre its baseline sits. */
    private fun centreOffset(paint: Paint): Float = paint.fontMetrics.let { -(it.ascent + it.descent) / 2f }

    /** Shrinks [paint]'s text until [text] fits [maxWidth] by [maxHeight]: very large font scales. */
    private fun fitWithin(
        paint: Paint,
        text: String,
        maxWidth: Float,
        maxHeight: Float,
    ) {
        if (maxWidth <= 0f || maxHeight <= 0f) return
        val width = paint.measureText(text)
        val scale = minOf(1f, maxWidth / width.coerceAtLeast(1f), maxHeight / paint.textSize.coerceAtLeast(1f))
        if (scale < 1f) paint.textSize *= scale
    }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    companion object {
        const val CORNER_DP = 2f
        const val NATURAL_SP = 28f
        const val LABEL_SP = 16f
        private const val NATURAL_OF_BUTTON = 0.3f
        private const val ACCIDENTAL_OF_NATURAL = 20f / 28f
        private const val CHORD_OF_NAME = 0.62f
        private const val GLOW_DP = 14f
        private const val GLOW_STEPS = 10
        private const val GLOW_ALPHA = 22
        private const val UNSELECTED_RING_ALPHA = 0.51f
        private const val IDLE = "— Hz"

        /** Below this height the readout goes on one line. */
        private const val TWO_LINE_MIN_DP = 56f

        private fun sp(
            context: Context,
            value: Float,
        ): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics)

        /** The range choices' labels: C to B, then F to E. */
        fun rangeLabels(context: Context): Pair<String, String> =
            context.getString(R.string.ClassicCtoB) to context.getString(R.string.ClassicFtoE)

        /** The paint the range labels are drawn with. */
        fun rangeLabelPaint(context: Context): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.LEFT
                textSize = sp(context, LABEL_SP)
            }

        /** The range labels' widths in pixels, which the geometry lays the choices out by. */
        fun rangeLabelWidths(context: Context): FloatArray {
            val paint = rangeLabelPaint(context)
            return rangeLabels(context).toList().map { paint.measureText(it) }.toFloatArray()
        }
    }
}
