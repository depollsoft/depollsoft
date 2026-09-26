package depollsoft.pitchperfect

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Where everything on the watch face sits, for a face of [width] x [height] pixels carrying
 * [cellCount] note cells. Cells ring the bezel; the range selector is seated in the ring's hole.
 */
class WearInstrumentGeometry(
    override val width: Int,
    override val height: Int,
    cellCount: Int,
) : InstrumentGeometry {
    val size = min(width, height).toFloat()
    val faceCx = width / 2f
    val faceCy = height / 2f
    val edgeInset = 0.02f * size
    val cellRadius = 0.085f * size
    val ringRadius = 0.5f * size - edgeInset - cellRadius

    override val cellCenters: List<FloatArray> =
        List(cellCount) { i ->
            val step = 360.0 / cellCount.coerceAtLeast(1)
            val angle = Math.toRadians(-90.0 + step / 2.0 + i * step)
            floatArrayOf(
                faceCx + ringRadius * cos(angle).toFloat(),
                faceCy + ringRadius * sin(angle).toFloat(),
            )
        }

    // The range selector is one machined part seated in the ring's hole.
    val rangeLowRect: RectF =
        run {
            val rangeWidth = 0.36f * size
            val rowHeight = 0.08f * size
            val rangeTop = faceCy + 0.055f * size
            RectF(faceCx - rangeWidth / 2f, rangeTop, faceCx + rangeWidth / 2f, rangeTop + rowHeight)
        }
    val rangeHighRect: RectF =
        RectF(rangeLowRect.left, rangeLowRect.bottom, rangeLowRect.right, rangeLowRect.bottom + rangeLowRect.height())

    private fun inHole(
        x: Float,
        y: Float,
    ): Boolean = hypot(x - faceCx, y - faceCy) < ringRadius - 1.25f * cellRadius

    /**
     * The row of the range selector under a finger, or -1. The drawn frame is
     * only ~15dp tall on a small watch, so the hit zone is the whole strip of
     * the hole around it: padded at the sides, reaching halfway up to the
     * readout, and all the way down to the ring. Cells keep their sectors.
     */
    override fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int {
        if (size <= 0f || !inHole(x, y)) return -1
        val sidePad = 0.06f * size
        val rowHeight = rangeLowRect.height()
        if (x < rangeLowRect.left - sidePad || x > rangeLowRect.right + sidePad) return -1
        if (y < rangeLowRect.top - rowHeight * 0.5f) return -1
        return if (y < rangeLowRect.bottom) 0 else 1
    }

    /**
     * The area a screen reader's explore-by-touch finds cell [index] in: the cell widened to half
     * the distance to its neighbours, so the gaps a finger would play from are not dead.
     */
    override fun cellTarget(index: Int): Rect {
        val c = cellCenters[index]
        val halfSpacing = ringRadius * sin(PI / cellCenters.size.coerceAtLeast(1)).toFloat()
        val half = maxOf(cellRadius, halfSpacing)
        return Rect((c[0] - half).toInt(), (c[1] - half).toInt(), (c[0] + half).toInt(), (c[1] + half).toInt())
    }

    /** The area explore-by-touch finds range row [row] in: the padded strip [rangeRowAt] uses. */
    override fun rangeTarget(row: Int): Rect {
        val sidePad = 0.06f * size
        val holeBottom = faceCy + ringRadius - 1.25f * cellRadius
        val top = if (row == 0) rangeLowRect.top - rangeLowRect.height() * 0.5f else rangeLowRect.bottom
        val bottom = if (row == 0) rangeLowRect.bottom else maxOf(rangeHighRect.bottom, holeBottom)
        return Rect(
            (rangeLowRect.left - sidePad).toInt(),
            top.toInt(),
            (rangeLowRect.right + sidePad).toInt(),
            bottom.toInt(),
        )
    }

    /** The cell whose sector is under a finger, or -1. */
    override fun cellAt(
        x: Float,
        y: Float,
    ): Int {
        if (size <= 0f || cellCenters.isEmpty()) return -1
        val dx = x - faceCx
        val dy = y - faceCy
        // The playable band reaches from the hole to just past the cells'
        // outer rims: the whole face on a round watch, but not the score-only
        // corners of a square one.
        val distance = hypot(dx, dy)
        if (distance < ringRadius - 1.25f * cellRadius || distance > ringRadius + 1.25f * cellRadius) return -1
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        val step = 360.0 / cellCenters.size
        return floor(((angle + 90.0 + 360.0) % 360.0) / step).toInt()
    }
}
