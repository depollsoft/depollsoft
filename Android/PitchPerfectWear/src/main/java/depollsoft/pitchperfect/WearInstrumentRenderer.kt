package depollsoft.pitchperfect

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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.sin

/**
 * The Laboratory Instrument: paints the radial pitch-pipe face onto a canvas.
 * Thirteen glass cells on a blackened-steel panel; the sounding cell is the
 * one luminous element on screen. Frequency digits and the octave-range
 * selector live in the ring's hole.
 */
class WearInstrumentRenderer(
    private val context: Context,
) {
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
    private val score = BitmapFactory.decodeResource(context.resources, R.drawable.panobackground)
    private val lowLabel = context.getString(R.string.CtoC)
    private val highLabel = context.getString(R.string.FtoF)
    private var panel: Bitmap? = null

    fun draw(
        canvas: Canvas,
        geometry: WearInstrumentGeometry,
        notes: List<Note>,
        fromFToF: Boolean,
        breathePhase: Float,
    ) {
        panelFor(geometry.width, geometry.height)?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        val breath = 0.82f + 0.18f * sin(breathePhase)
        val centers = geometry.cellCenters
        val cellRadius = geometry.cellRadius

        // Bloom pass under the glass so light appears to leak across the panel.
        notes.forEachIndexed { i, note ->
            if (note.isPlaying && i < centers.size) {
                val c = centers[i]
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
            if (i >= centers.size) return@forEachIndexed
            val c = centers[i]
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
                if (playing) {
                    onLit
                } else if (note.accidental == Accidental.Natural) {
                    ink
                } else {
                    inkSecondary
                }
            textPaint.textSize = if (note.accidental == Accidental.Natural) cellRadius * 0.9f else cellRadius * 0.62f
            canvas.drawText(NoteNames.engraved(note), c[0], c[1] + textPaint.textSize * 0.35f, textPaint)
        }

        drawCenter(canvas, geometry, notes)
        drawRangeControl(canvas, geometry, fromFToF)
    }

    /** The grained panel and score engraving, drawn once per face size. */
    private fun panelFor(
        w: Int,
        h: Int,
    ): Bitmap? {
        panel?.let { if (it.width == w && it.height == h) return it }
        panel?.recycle()
        panel = null
        if (w <= 0 || h <= 0) return null
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ground)
        val grainPaint =
            Paint().apply {
                color = hairline
                strokeWidth = 1f
            }
        for (y in 0 until h step 4) {
            grainPaint.alpha = 8 + ((y * 31) % 14)
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), grainPaint)
        }
        score?.let { tile ->
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        return bitmap
    }

    private fun drawCenter(
        canvas: Canvas,
        geometry: WearInstrumentGeometry,
        notes: List<Note>,
    ) {
        val size = geometry.size
        val faceCx = geometry.faceCx
        val faceCy = geometry.faceCy
        val playingNotes = notes.withIndex().filter { it.value.isPlaying }
        if (playingNotes.isNotEmpty()) {
            textPaint.color = ink
            textPaint.textSize = if (playingNotes.size == 1) size * 0.15f else size * 0.08f
            val label = playingNotes.joinToString(" ") { NoteNames.readout(it.value) }
            val labelWidth = textPaint.measureText(label)
            if (labelWidth > size * 0.5f) textPaint.textSize *= size * 0.5f / labelWidth
            canvas.drawText(label, faceCx, faceCy - size * 0.075f, textPaint)
            monoPaint.color = ink
            monoPaint.textSize = size * 0.065f
            val chord = if (playingNotes.size > 2) PitchChord.name(playingNotes.map { it.index }) else null
            if (chord != null) {
                // A chord has a name, not a measurement: engrave it in the
                // display face so the exclamation reads as one word.
                textPaint.textSize = size * 0.09f
                textPaint.letterSpacing = 0.12f
                canvas.drawText(chord, faceCx, faceCy + size * 0.015f, textPaint)
                textPaint.letterSpacing = 0f
            } else {
                val readout =
                    when (playingNotes.size) {
                        1 -> String.format("%.1f Hz", playingNotes[0].value.frequency)
                        2 -> PitchInterval.name(playingNotes[0].index, playingNotes[1].index)
                        else -> "${playingNotes.size} NOTES"
                    }
                canvas.drawText(readout, faceCx, faceCy + size * 0.015f, monoPaint)
            }
        } else {
            monoPaint.color = withAlpha(inkSecondary, 140)
            monoPaint.textSize = size * 0.065f
            canvas.drawText("— Hz", faceCx, faceCy - size * 0.02f, monoPaint)
        }
    }

    private fun drawRangeControl(
        canvas: Canvas,
        geometry: WearInstrumentGeometry,
        fromFToF: Boolean,
    ) {
        val low = geometry.rangeLowRect
        val high = geometry.rangeHighRect
        val corner = geometry.size * 0.012f
        // One machined frame contains both range positions.
        fillPaint.color = withAlpha(surface, 235)
        canvas.drawRoundRect(low.left, low.top, high.right, high.bottom, corner, corner, fillPaint)
        strokePaint.color = hairline
        strokePaint.strokeWidth = 1.5f
        canvas.drawRoundRect(low.left, low.top, high.right, high.bottom, corner, corner, strokePaint)
        strokePaint.strokeWidth = 1f
        strokePaint.color = withAlpha(hairline, 160)
        canvas.drawLine(low.left, low.bottom, low.right, low.bottom, strokePaint)
        drawRangeRow(canvas, low, lowLabel, !fromFToF)
        drawRangeRow(canvas, high, highLabel, fromFToF)
    }

    private fun drawRangeRow(
        canvas: Canvas,
        rect: RectF,
        label: String,
        selected: Boolean,
    ) {
        if (selected) {
            fillPaint.color = withAlpha(ink, 26)
            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, fillPaint)
            fillPaint.color = lit
            canvas.drawCircle(rect.left + rect.height() * 0.62f, rect.centerY(), rect.height() * 0.13f, fillPaint)
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
}
