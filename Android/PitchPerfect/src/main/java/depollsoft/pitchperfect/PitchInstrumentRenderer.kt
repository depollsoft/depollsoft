package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.sin

/**
 * The Laboratory Instrument: paints the radial pitch-pipe face onto a canvas.
 * Twelve glass cells on a blackened-steel panel; the sounding cell is the
 * one luminous element on screen. Frequency digits and the octave-range
 * selector live in the ring's hole.
 */
class PitchInstrumentRenderer(
    private val context: Context,
) {
    private fun colorOrFallback(
        resourceId: Int,
        fallback: String,
    ): Int =
        runCatching { ContextCompat.getColor(context, resourceId) }
            .getOrElse { Color.parseColor(fallback) }

    private val surface = colorOrFallback(R.color.plate_surface, "#16181A")
    private val ink = colorOrFallback(R.color.plate_ink, "#D9DBDD")
    private val inkSecondary = colorOrFallback(R.color.plate_ink_secondary, "#898D92")
    private val hairline = colorOrFallback(R.color.plate_hairline, "#2C2F33")
    private val lit = colorOrFallback(R.color.plate_accent, "#F2EFE6")
    private val onLit = colorOrFallback(R.color.plate_on_accent, "#101214")
    private val lowLabel = context.getString(R.string.CtoC).uppercase()
    private val highLabel = context.getString(R.string.FtoF).uppercase()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface =
                runCatching { ResourcesCompat.getFont(context, R.font.oswald_medium) }.getOrNull()
                    ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
    private val monoPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        canvas: Canvas,
        geometry: PitchInstrumentGeometry,
        notes: List<Note>,
        isFromFToF: Boolean,
        breathePhase: Float,
    ) {
        val cells = geometry.cellCenters
        val cellRadius = geometry.cellRadius
        val breath = 0.82f + 0.18f * sin(breathePhase)

        // Bloom pass under the glass so light appears to leak across the panel.
        notes.forEachIndexed { i, note ->
            if (note.isPlaying && i < cells.size) {
                val c = cells[i]
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

        notes.forEachIndexed { i, note ->
            if (i >= cells.size) return@forEachIndexed
            val c = cells[i]
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

            textPaint.color =
                when {
                    playing -> onLit
                    note.accidental == Accidental.Natural -> ink
                    else -> inkSecondary
                }
            textPaint.textSize = if (note.accidental == Accidental.Natural) cellRadius * 0.9f else cellRadius * 0.62f
            canvas.drawText(NoteNames.engraved(note), c[0], c[1] + textPaint.textSize * 0.35f, textPaint)
        }

        drawCenter(canvas, geometry, notes)
        drawRangeControl(canvas, geometry, isFromFToF)
        drawNameplate(canvas, geometry)
    }

    private fun drawCenter(
        canvas: Canvas,
        geometry: PitchInstrumentGeometry,
        notes: List<Note>,
    ) {
        val faceCx = geometry.faceCx
        val faceCy = geometry.faceCy
        val ringRadius = geometry.ringRadius
        val playingNotes = notes.withIndex().filter { it.value.isPlaying }
        if (playingNotes.isNotEmpty()) {
            textPaint.color = ink
            textPaint.textSize = if (playingNotes.size == 1) ringRadius * 0.30f else ringRadius * 0.17f
            val label = playingNotes.joinToString(" ") { NoteNames.readout(it.value) }
            canvas.drawText(label, faceCx, faceCy - ringRadius * 0.18f, textPaint)
            monoPaint.color = ink
            monoPaint.textSize = ringRadius * 0.15f
            val chord = if (playingNotes.size > 2) PitchChord.name(playingNotes.map { it.index }) else null
            if (chord != null) {
                // A chord has a name, not a measurement: engrave it in the
                // display face so the exclamation reads as one word.
                textPaint.textSize = ringRadius * 0.20f
                textPaint.letterSpacing = 0.12f
                canvas.drawText(chord, faceCx, faceCy + ringRadius * 0.04f, textPaint)
                textPaint.letterSpacing = 0f
            } else {
                val readout =
                    when (playingNotes.size) {
                        1 -> String.format("%.1f Hz", playingNotes[0].value.frequency)
                        2 -> PitchInterval.name(playingNotes[0].index, playingNotes[1].index)
                        else -> "${playingNotes.size} NOTES"
                    }
                canvas.drawText(readout, faceCx, faceCy + ringRadius * 0.02f, monoPaint)
            }
        } else {
            monoPaint.color = withAlpha(inkSecondary, 140)
            monoPaint.textSize = ringRadius * 0.15f
            canvas.drawText("— Hz", faceCx, faceCy - ringRadius * 0.04f, monoPaint)
        }
    }

    private fun drawRangeControl(
        canvas: Canvas,
        geometry: PitchInstrumentGeometry,
        isFromFToF: Boolean,
    ) {
        val low = geometry.rangeLowRect
        val high = geometry.rangeHighRect
        // One machined frame contains both range positions.
        fillPaint.color = withAlpha(surface, 235)
        canvas.drawRoundRect(low.left.toFloat(), low.top.toFloat(), high.right.toFloat(), high.bottom.toFloat(), 5f, 5f, fillPaint)
        strokePaint.color = hairline
        strokePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(low.left.toFloat(), low.top.toFloat(), high.right.toFloat(), high.bottom.toFloat(), 5f, 5f, strokePaint)
        strokePaint.strokeWidth = 1f
        strokePaint.color = withAlpha(hairline, 160)
        canvas.drawLine(low.left.toFloat(), low.bottom.toFloat(), low.right.toFloat(), low.bottom.toFloat(), strokePaint)
        drawRangeRow(canvas, low, lowLabel, !isFromFToF)
        drawRangeRow(canvas, high, highLabel, isFromFToF)
    }

    private fun drawRangeRow(
        canvas: Canvas,
        rect: Rect,
        label: String,
        selected: Boolean,
    ) {
        if (selected) {
            fillPaint.color = withAlpha(ink, 26)
            canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), fillPaint)
            fillPaint.color = lit
            canvas.drawCircle(rect.left + rect.height() * 0.62f, rect.exactCenterY(), rect.height() * 0.13f, fillPaint)
        }
        textPaint.color = if (selected) ink else withAlpha(inkSecondary, 190)
        textPaint.textSize = rect.height() * 0.52f
        textPaint.letterSpacing = 0.16f
        canvas.drawText(label, rect.exactCenterX(), rect.exactCenterY() + textPaint.textSize * 0.35f, textPaint)
        textPaint.letterSpacing = 0f
    }

    private fun drawNameplate(
        canvas: Canvas,
        geometry: PitchInstrumentGeometry,
    ) {
        textPaint.color = withAlpha(inkSecondary, 165)
        textPaint.textSize = geometry.ringRadius * 0.085f
        textPaint.letterSpacing = 0.34f
        canvas.drawText("DIGITAL PITCH PIPE", geometry.faceCx, geometry.height - textPaint.textSize * 1.6f, textPaint)
        textPaint.letterSpacing = 0f
    }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
