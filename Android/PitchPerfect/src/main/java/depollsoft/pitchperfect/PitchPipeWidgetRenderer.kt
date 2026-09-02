package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Draws the launcher widget as the same circular instrument used in the app. */
object PitchPipeWidgetRenderer {
    private class Palette(
        context: Context,
    ) {
        val ground = ContextCompat.getColor(context, R.color.plate_ground)
        val surface = ContextCompat.getColor(context, R.color.plate_surface)
        val ink = ContextCompat.getColor(context, R.color.plate_ink)
        val inkSecondary = ContextCompat.getColor(context, R.color.plate_ink_secondary)
        val hairline = ContextCompat.getColor(context, R.color.plate_hairline)
        val lit = ContextCompat.getColor(context, R.color.plate_accent)
        val onLit = ContextCompat.getColor(context, R.color.plate_on_accent)
        val display: Typeface =
            runCatching { ResourcesCompat.getFont(context, R.font.oswald_medium) }.getOrNull()
                ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    fun face(
        context: Context,
        notes: List<Note>,
        highRange: Boolean,
        width: Int,
        height: Int,
        drawControls: Boolean = true,
    ): Bitmap {
        val p = Palette(context)
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val display =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = p.display
            }
        val mono =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }

        canvas.drawColor(p.ground)
        drawGrain(canvas, p.hairline, width, height)
        drawScore(context, canvas, p.inkSecondary, width, height)

        val cx = width / 2f
        val cy = height * 0.43f
        val ring = min(width.toFloat(), height * 0.82f) * 0.365f
        val radius = ring * 0.225f

        val faceNotes = notes.take(13)
        val step = 360.0 / faceNotes.size.coerceAtLeast(1)
        val start = -90.0 + step / 2.0
        if (drawControls) {
            faceNotes.forEachIndexed { index, note ->
                val angle = Math.toRadians(start + index * step)
                val x = cx + (cos(angle) * ring).toFloat()
                val y = cy + (sin(angle) * ring).toFloat()
                drawCell(canvas, note, x, y, radius, p, fill, stroke, display)
            }
        }

        drawReadout(canvas, notes, cx, cy, ring, p, display, mono)
        if (drawControls) {
            drawRange(canvas, cx, cy + ring * 0.20f, ring * 0.78f, ring * 0.145f, highRange, p, fill, stroke, display)
        }

        display.color = withAlpha(p.inkSecondary, 165)
        display.textSize = ring * 0.08f
        display.letterSpacing = 0.28f
        canvas.drawText("DIGITAL PITCH PIPE", cx, height * 0.91f, display)
        display.letterSpacing = 0f
        return bmp
    }

    fun cell(context: Context, note: Note, size: Int): Bitmap {
        val p = Palette(context)
        val bmp = Bitmap.createBitmap(size.coerceAtLeast(1), size.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = p.display
        }
        drawCell(canvas, note, size / 2f, size / 2f, size * 0.44f, p, fill, stroke, text)
        return bmp
    }

    fun rangeSelector(context: Context, high: Boolean, width: Int, height: Int): Bitmap {
        val p = Palette(context)
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = p.display
        }
        drawRange(canvas, width / 2f, 0f, width * 0.96f, height / 2f, high, p, fill, stroke, text)
        return bmp
    }

    private fun drawCell(
        canvas: Canvas,
        note: Note,
        cx: Float,
        cy: Float,
        radius: Float,
        p: Palette,
        fill: Paint,
        stroke: Paint,
        text: Paint,
    ) {
        val playing = note.isPlaying
        if (playing) {
            fill.shader =
                RadialGradient(cx, cy, radius * 2.4f, intArrayOf(withAlpha(p.lit, 150), withAlpha(p.lit, 0)), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius * 2.4f, fill)
            fill.shader = null
        }
        fill.color = if (playing) p.lit else p.surface
        canvas.drawCircle(cx, cy, radius, fill)
        stroke.color = if (playing) p.lit else p.hairline
        stroke.strokeWidth = if (playing) 3f else 1.5f
        canvas.drawCircle(cx, cy, radius, stroke)
        stroke.color = if (playing) withAlpha(p.onLit, 110) else withAlpha(p.inkSecondary, 70)
        stroke.strokeWidth = 1f
        canvas.drawCircle(cx, cy, radius * 0.86f, stroke)
        val natural = note.accidental == Accidental.Natural
        text.color =
            if (playing) {
                p.onLit
            } else if (natural) {
                p.ink
            } else {
                p.inkSecondary
            }
        text.textSize = if (natural) radius * 0.9f else radius * 0.62f
        canvas.drawText(if (natural) note.friendlyName else "\u266F/\u266D", cx, cy + text.textSize * 0.35f, text)
    }

    private fun drawReadout(
        canvas: Canvas,
        notes: List<Note>,
        cx: Float,
        cy: Float,
        ring: Float,
        p: Palette,
        display: Paint,
        mono: Paint,
    ) {
        val playing = notes.withIndex().filter { it.value.isPlaying }
        if (playing.isEmpty()) {
            mono.color = withAlpha(p.inkSecondary, 140)
            mono.textSize = ring * 0.15f
            canvas.drawText("\u2014 Hz", cx, cy - ring * 0.04f, mono)
            return
        }
        display.color = p.ink
        display.textSize = if (playing.size == 1) ring * 0.28f else ring * 0.16f
        val names =
            playing.joinToString(" ") { (_, note) ->
                (if (note.accidental == Accidental.Natural) note.friendlyName else "${note.friendlyName}\u266F") + note.octave
            }
        canvas.drawText(names, cx, cy - ring * 0.18f, display)
        mono.color = p.ink
        mono.textSize = ring * 0.13f
        val line =
            when (playing.size) {
                1 -> String.format("%.1f Hz", playing[0].value.frequency)
                2 -> PitchInterval.name(playing[0].index, playing[1].index)
                else -> "${playing.size} NOTES"
            }
        canvas.drawText(line, cx, cy + ring * 0.02f, mono)
    }

    private fun drawRange(
        canvas: Canvas,
        cx: Float,
        top: Float,
        width: Float,
        rowHeight: Float,
        high: Boolean,
        p: Palette,
        fill: Paint,
        stroke: Paint,
        text: Paint,
    ) {
        val left = cx - width / 2f
        val right = cx + width / 2f
        val bottom = top + rowHeight * 2f
        stroke.color = p.hairline
        stroke.strokeWidth = 1.5f
        canvas.drawRoundRect(left, top, right, bottom, 5f, 5f, stroke)
        stroke.strokeWidth = 1f
        canvas.drawLine(left, top + rowHeight, right, top + rowHeight, stroke)
        drawRangeRow(canvas, left, right, top, rowHeight, "C TO C", !high, p, fill, text)
        drawRangeRow(canvas, left, right, top + rowHeight, rowHeight, "F TO F", high, p, fill, text)
    }

    private fun drawRangeRow(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        height: Float,
        label: String,
        selected: Boolean,
        p: Palette,
        fill: Paint,
        text: Paint,
    ) {
        if (selected) {
            fill.color = withAlpha(p.ink, 26)
            canvas.drawRect(left + 1f, top + 1f, right - 1f, top + height - 1f, fill)
            fill.color = p.lit
            canvas.drawCircle(left + height * 0.55f, top + height * 0.5f, height * 0.13f, fill)
        }
        text.color = if (selected) p.ink else withAlpha(p.inkSecondary, 190)
        text.textSize = height * 0.5f
        text.letterSpacing = 0.16f
        canvas.drawText(label, (left + right) / 2f + height * 0.15f, top + height * 0.67f, text)
        text.letterSpacing = 0f
    }

    private fun drawGrain(
        canvas: Canvas,
        color: Int,
        width: Int,
        height: Int,
    ) {
        val paint =
            Paint().apply {
                strokeWidth = 1f
                this.color = color
            }
        var y = 0
        while (y < height) {
            paint.alpha = 8 + ((y * 31) % 14)
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
            y += 4
        }
    }

    private fun drawScore(
        context: Context,
        canvas: Canvas,
        color: Int,
        width: Int,
        height: Int,
    ) {
        val tile = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.panobackground) }.getOrNull() ?: return
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 26
                shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}
