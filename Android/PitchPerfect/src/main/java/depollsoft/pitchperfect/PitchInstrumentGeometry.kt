package depollsoft.pitchperfect

import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Where everything on the pitch-pipe face sits, for a face of [width] x [height] pixels carrying
 * [cellCount] note cells: a true ring of cells a little above centre, the range selector seated in
 * the ring's hole.
 */
class PitchInstrumentGeometry(
    override val width: Int,
    override val height: Int,
    cellCount: Int,
) : InstrumentGeometry {
    val faceCx = width / 2f
    val faceCy = height * FACE_CENTER_Y
    val ringRadius = min(width.toFloat(), height * FACE_HEIGHT_FRACTION) * RING_FRACTION
    val cellRadius = ringRadius * if (cellCount > 12) 0.225f else 0.245f

    override val cellCenters: List<FloatArray> =
        List(cellCount.coerceAtLeast(1)) { i ->
            val step = 360.0 / cellCount.coerceAtLeast(1)
            val angle = Math.toRadians(-90.0 + step / 2.0 + i * step)
            floatArrayOf(
                faceCx + ringRadius * cos(angle).toFloat(),
                faceCy + ringRadius * sin(angle).toFloat(),
            )
        }

    // The range selector is one machined part seated in the ring's hole.
    val rangeLowRect: Rect =
        run {
            val rangeWidth = (ringRadius * 0.72f).toInt()
            val rowHeight = (ringRadius * 0.145f).toInt()
            val rangeTop = (faceCy + ringRadius * 0.20f).toInt()
            Rect(
                (faceCx - rangeWidth / 2f).toInt(),
                rangeTop,
                (faceCx + rangeWidth / 2f).toInt(),
                rangeTop + rowHeight,
            )
        }
    val rangeHighRect: Rect =
        Rect(rangeLowRect.left, rangeLowRect.bottom, rangeLowRect.right, rangeLowRect.bottom + rangeLowRect.height())

    override fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int =
        when {
            rangeLowRect.contains(x.toInt(), y.toInt()) -> 0
            rangeHighRect.contains(x.toInt(), y.toInt()) -> 1
            else -> -1
        }

    /** The cell under a point, with a little slack around its rim, or -1. */
    override fun cellAt(
        x: Float,
        y: Float,
    ): Int {
        for (i in cellCenters.indices) {
            val c = cellCenters[i]
            if (hypot((x - c[0]).toDouble(), (y - c[1]).toDouble()) <= cellRadius * 1.15) return i
        }
        return -1
    }

    /** A screen reader finds a cell in the square around its disc. */
    override fun cellTarget(index: Int): Rect {
        val c = cellCenters[index]
        return Rect(
            (c[0] - cellRadius).toInt(),
            (c[1] - cellRadius).toInt(),
            (c[0] + cellRadius).toInt(),
            (c[1] + cellRadius).toInt(),
        )
    }

    override fun rangeTarget(row: Int): Rect = Rect(if (row == 0) rangeLowRect else rangeHighRect)

    companion object {
        const val FACE_CENTER_Y = 0.44f
        const val FACE_HEIGHT_FRACTION = 0.82f
        const val RING_FRACTION = 0.365f
    }
}
