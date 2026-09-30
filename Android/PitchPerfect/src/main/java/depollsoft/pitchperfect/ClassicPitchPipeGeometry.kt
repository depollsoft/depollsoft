package depollsoft.pitchperfect

import android.graphics.Rect
import android.graphics.RectF

/**
 * Where the classic pitch pipe's parts sit, for a face of [width] x [height] pixels at [density]
 * pixels per dp. This is the grid the app had before the Laboratory Instrument: four rows of four
 * slots, the cells round the edge running clockwise from the top left through one octave, and the
 * two range choices in the middle, one in each inner row.
 *
 * Like the radial face, it works in physical pixels from the left and does not mirror.
 */
class ClassicPitchPipeGeometry(
    override val width: Int,
    override val height: Int,
    cellCount: Int,
    private val density: Float,
) : InstrumentGeometry {
    private val columnWidth = width / COLUMNS.toFloat()
    private val rowHeight = height / ROWS.toFloat()
    private val slots = SLOTS.take(cellCount.coerceIn(0, CELLS))

    /**
     * Cell [index]'s whole slot. The old buttons took touches anywhere in their slot, the margin
     * round the drawn button included, so no finger lands between two notes.
     */
    fun slot(index: Int): RectF {
        val (column, row) = slots[index]
        return RectF(column * columnWidth, row * rowHeight, (column + 1) * columnWidth, (row + 1) * rowHeight)
    }

    /** The button drawn in cell [index]'s slot, inset as a Material button's background was. */
    fun button(index: Int): RectF =
        slot(index).apply { inset(BUTTON_INSET_X_DP * density, BUTTON_INSET_Y_DP * density) }

    override val cellCenters: List<FloatArray> =
        slots.indices.map { i -> slot(i).let { floatArrayOf(it.centerX(), it.centerY()) } }

    override fun cellAt(
        x: Float,
        y: Float,
    ): Int {
        if (x < 0f || y < 0f || x >= width || y >= height) return -1
        val column = (x / columnWidth).toInt().coerceAtMost(COLUMNS - 1)
        val row = (y / rowHeight).toInt().coerceAtMost(ROWS - 1)
        return slots.indexOf(column to row)
    }

    /**
     * Range choice [row] (0 for C to B, 1 for F to E): the two middle columns, one choice's height,
     * level with the buttons' labels in its row. The rest of the middle is bare panel.
     */
    fun rangeRow(row: Int): RectF {
        val center = (row + 1.5f) * rowHeight
        val half = RANGE_ROW_DP * density / 2f
        return RectF(columnWidth, center - half, columnWidth * 3, center + half)
    }

    override fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int =
        when {
            rangeRow(0).contains(x, y) -> 0
            rangeRow(1).contains(x, y) -> 1
            else -> -1
        }

    override fun cellTarget(index: Int): Rect = slot(index).toRect()

    override fun rangeTarget(row: Int): Rect = rangeRow(row).toRect()

    private fun RectF.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    companion object {
        /** The old grid showed one octave: C to B, or F to E. */
        const val CELLS = 12
        private const val COLUMNS = 4
        private const val ROWS = 4

        /** A Material button's background inset (`abc_button_inset_*_material`). */
        const val BUTTON_INSET_X_DP = 4f
        const val BUTTON_INSET_Y_DP = 6f

        /** A radio button's minimum height. */
        const val RANGE_ROW_DP = 48f

        /** (column, row) of each cell: along the top, down the right, back along the bottom, up the left. */
        private val SLOTS =
            listOf(
                0 to 0, 1 to 0, 2 to 0, 3 to 0,
                3 to 1, 3 to 2,
                3 to 3, 2 to 3, 1 to 3, 0 to 3,
                0 to 2, 0 to 1,
            )
    }
}
