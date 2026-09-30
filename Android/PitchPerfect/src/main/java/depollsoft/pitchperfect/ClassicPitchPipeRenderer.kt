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
 * radial cell does, with its glow breathing round it. The range choices are radio buttons, drawn
 * as the Settings screen draws its own.
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
    private val lowLabel = context.getString(R.string.ClassicCtoB)
    private val highLabel = context.getString(R.string.ClassicFtoE)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val engravingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = runCatching { PlateFonts.oswaldTypeface(context) }.getOrDefault(Typeface.DEFAULT)
        }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val bounds = RectF()

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics)

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
        for (i in 0 until cells) drawButton(canvas, geometry.button(i), notes[i], breathePhase)

        drawRangeChoice(canvas, geometry.rangeRow(0), lowLabel, selected = !isFromFToF)
        drawRangeChoice(canvas, geometry.rangeRow(1), highLabel, selected = isFromFToF)
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
        engravingPaint.color =
            when {
                playing -> onLit
                natural -> ink
                else -> inkSecondary
            }
        val label = NoteNames.engraved(note)
        // At least the phone's size, and in proportion on a tablet's bigger buttons.
        val size = maxOf(sp(NATURAL_SP), minOf(button.width(), button.height()) * NATURAL_OF_BUTTON)
        engravingPaint.textSize = if (natural) size else size * ACCIDENTAL_OF_NATURAL
        fitWithin(engravingPaint, label, button.width() * 0.8f, button.height() * 0.6f)
        canvas.drawText(label, button.centerX(), button.centerY() + engravingPaint.textSize * 0.35f, engravingPaint)
    }

    /** A radio button: its 20dp ring (with a dot when chosen) in a 32dp column, then the label. */
    private fun drawRangeChoice(
        canvas: Canvas,
        row: RectF,
        label: String,
        selected: Boolean,
    ) {
        val cx = row.left + dp(16f)
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
        val start = row.left + dp(32f)
        fitWithin(labelPaint, label, row.right - start - dp(8f), row.height())
        val metrics = labelPaint.fontMetrics
        canvas.drawText(label, start, cy - (metrics.ascent + metrics.descent) / 2f, labelPaint)
    }

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
        private const val NATURAL_OF_BUTTON = 0.3f
        private const val ACCIDENTAL_OF_NATURAL = 20f / 28f
        const val LABEL_SP = 16f
        private const val GLOW_DP = 14f
        private const val GLOW_STEPS = 10
        private const val GLOW_ALPHA = 22
        private const val UNSELECTED_RING_ALPHA = 0.51f
    }
}
