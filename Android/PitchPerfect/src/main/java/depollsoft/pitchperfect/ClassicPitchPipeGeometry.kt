package depollsoft.pitchperfect

import android.graphics.Rect
import android.graphics.RectF

/**
 * Where the classic pitch pipe's parts sit, for a face of [width] x [height] pixels at [density]
 * pixels per dp. This is the grid the app had before the Laboratory Instrument, with its middle put
 * to use: four rows of four slots, twelve cells round the edge running clockwise from the top left,
 * the octave's upper note as one wide cell across the top of the middle, and below it a well holding
 * the readout and the range choices.
 *
 * [rangeLabelWidths] are the two range labels' widths in pixels, which decide whether the choices
 * fit side by side. Like the radial face, the grid works in physical pixels from the left and does
 * not mirror.
 */
class ClassicPitchPipeGeometry(
    override val width: Int,
    override val height: Int,
    cellCount: Int,
    private val density: Float,
    private val rangeLabelWidths: FloatArray,
) : InstrumentGeometry {
    private val columnWidth = width / COLUMNS.toFloat()
    private val rowHeight = height / ROWS.toFloat()
    private val slots = SLOTS.take(cellCount.coerceIn(0, SLOTS.size))

    private fun dp(value: Float) = value * density

    /**
     * Cell [index]'s whole slot. A finger anywhere in it, the margin round the drawn button
     * included, plays the cell, so none lands between two notes.
     */
    fun slot(index: Int): RectF = slots[index].toRect()

    /** The button drawn in cell [index]'s slot, inset as a Material button's background was. */
    fun button(index: Int): RectF = slot(index).insetAsButton()

    /** A button in a single slot: every label is sized from this, the wide cell's included. */
    val singleButton: RectF = Slot(0, 0).toRect().insetAsButton()

    override val cellCenters: List<FloatArray> =
        slots.indices.map { i -> slot(i).let { floatArrayOf(it.centerX(), it.centerY()) } }

    /** The bare panel under the wide cell: the readout and the range choices. */
    val well: RectF = Slot(1, 2, columns = 2).toRect().insetAsButton()

    /** Whether the choices sit under the readout (a tall well) or beside it (a short one). */
    val stacked: Boolean

    /** Whether the two choices sit side by side, rather than one above the other. */
    val choicesInARow: Boolean

    private val choices: List<RectF>

    /** Where the readout is centred. */
    val readout: RectF

    init {
        val gap = dp(GAP_DP)
        val row = dp(RANGE_ROW_DP)
        val widths = rangeLabelWidths.map { dp(RADIO_DP + LABEL_END_DP) + it }
        val inARow = widths.sum() + dp(CHOICE_GAP_DP)
        val inAColumn = widths.max()
        // Under the readout they have the well's width; beside it, half.
        val rowUnder = inARow <= well.width()
        stacked = well.height() >= (if (rowUnder) row else row * 2) + gap + dp(MIN_READOUT_DP)
        choicesInARow = if (stacked) rowUnder else inARow <= well.width() / 2f
        val groupWidth = if (choicesInARow) inARow else inAColumn
        val rowHeight = if (choicesInARow || stacked) row else minOf(row, well.height() / 2f)
        val groupHeight = if (choicesInARow) rowHeight else rowHeight * 2
        val left = if (stacked) well.centerX() - groupWidth / 2f else well.right - groupWidth
        val top = if (stacked) well.bottom - groupHeight else well.centerY() - groupHeight / 2f
        choices =
            if (choicesInARow) {
                listOf(
                    RectF(left, top, left + widths[0], top + rowHeight),
                    RectF(left + widths[0] + dp(CHOICE_GAP_DP), top, left + inARow, top + rowHeight),
                )
            } else {
                listOf(
                    RectF(left, top, left + groupWidth, top + rowHeight),
                    RectF(left, top + rowHeight, left + groupWidth, top + rowHeight * 2),
                )
            }
        readout =
            if (stacked) {
                RectF(well.left, well.top, well.right, top - gap)
            } else {
                RectF(well.left, well.top, left - gap, well.bottom)
            }
    }

    override fun cellAt(
        x: Float,
        y: Float,
    ): Int {
        if (x < 0f || y < 0f || x >= width || y >= height) return -1
        val column = (x / columnWidth).toInt().coerceAtMost(COLUMNS - 1)
        val row = (y / rowHeight).toInt().coerceAtMost(ROWS - 1)
        return slots.indexOfFirst { it.contains(column, row) }
    }

    /** Range choice [row] (0 for C to C, 1 for F to F): its radio ring and label. */
    fun rangeRow(row: Int): RectF = RectF(choices[row])

    override fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int = choices.indexOfFirst { it.contains(x, y) }

    override fun cellTarget(index: Int): Rect = slot(index).toRect()

    override fun rangeTarget(row: Int): Rect = choices[row].toRect()

    private fun Slot.toRect() =
        RectF(column * columnWidth, row * rowHeight, (column + columns) * columnWidth, (row + 1) * rowHeight)

    private fun RectF.insetAsButton() = apply { inset(dp(BUTTON_INSET_X_DP), dp(BUTTON_INSET_Y_DP)) }

    private fun RectF.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    /** A cell's place in the grid: [columns] wide from ([column], [row]). */
    private data class Slot(
        val column: Int,
        val row: Int,
        val columns: Int = 1,
    ) {
        fun contains(
            column: Int,
            row: Int,
        ) = row == this.row && column >= this.column && column < this.column + columns
    }

    companion object {
        private const val COLUMNS = 4
        private const val ROWS = 4

        /** A Material button's background inset (`abc_button_inset_*_material`). */
        const val BUTTON_INSET_X_DP = 4f
        const val BUTTON_INSET_Y_DP = 6f

        /** A radio button's minimum height; its ring's column; the space after its label. */
        const val RANGE_ROW_DP = 48f
        const val RADIO_DP = 32f
        private const val LABEL_END_DP = 8f

        /** Between the two choices when they share a row. */
        private const val CHOICE_GAP_DP = 16f

        /** Between the readout and the choices; the least room the readout gets above them. */
        private const val GAP_DP = 8f
        private const val MIN_READOUT_DP = 56f

        /**
         * Each cell's slot: along the top, down the right, back along the bottom, up the left,
         * then the octave's upper note across the top of the middle.
         */
        private val SLOTS =
            listOf(
                Slot(0, 0), Slot(1, 0), Slot(2, 0), Slot(3, 0),
                Slot(3, 1), Slot(3, 2),
                Slot(3, 3), Slot(2, 3), Slot(1, 3), Slot(0, 3),
                Slot(0, 2), Slot(0, 1),
                Slot(1, 1, columns = 2),
            )
    }
}
