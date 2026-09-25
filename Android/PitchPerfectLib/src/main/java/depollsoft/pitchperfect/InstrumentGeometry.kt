package depollsoft.pitchperfect

import android.graphics.Rect

/** Where a pitch-pipe face's cells and range rows sit, in the face's pixels. */
interface InstrumentGeometry {
    val width: Int
    val height: Int

    /** Each cell's centre as `[x, y]`, clockwise from the top. */
    val cellCenters: List<FloatArray>

    /** The cell under a finger, or -1. */
    fun cellAt(
        x: Float,
        y: Float,
    ): Int

    /** The range row under a finger: 0 for C to C, 1 for F to F, -1 for neither. */
    fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int

    /** Where a screen reader finds cell [index]. */
    fun cellTarget(index: Int): Rect

    /** Where a screen reader finds range row [row]. */
    fun rangeTarget(row: Int): Rect
}
