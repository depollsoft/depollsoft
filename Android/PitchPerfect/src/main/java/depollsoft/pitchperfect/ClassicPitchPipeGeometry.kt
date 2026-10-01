package depollsoft.pitchperfect

import android.graphics.Rect
import android.graphics.RectF

/**
 * Where the classic pitch pipe's parts sit, for a face of [width] x [height] pixels at [density]
 * pixels per dp. This is the grid the app had before the Laboratory Instrument: four rows of four
 * slots, twelve cells round the edge running clockwise from the top left through one octave, and
 * the middle (once just the range choices) a well holding the readout above the range choices.
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
    private val slots = SLOTS.take(cellCount.coerceIn(0, CELLS))

    private fun dp(value: Float) = value * density

    /**
     * Cell [index]'s whole slot. A finger anywhere in it, the margin round the drawn button
     * included, plays the cell, so none lands between two notes.
     */
    fun slot(index: Int): RectF {
        val (column, row) = slots[index]
        return RectF(column * columnWidth, row * rowHeight, (column + 1) * columnWidth, (row + 1) * rowHeight)
    }

    /** The button drawn in cell [index]'s slot, inset as a Material button's background was. */
    fun button(index: Int): RectF = slot(index).insetAsButton()

    override val cellCenters: List<FloatArray> =
        slots.indices.map { i -> slot(i).let { floatArrayOf(it.centerX(), it.centerY()) } }

    /** The bare middle of the grid: the readout and the range choices. */
    val well: RectF = RectF(columnWidth, rowHeight, columnWidth * 3, rowHeight * 3).insetAsButton()

    /** Whether the readout fills the well's top half and the choices its bottom half (a tall well), or they sit side by side. */
    val stacked: Boolean

    /** Whether the two choices sit side by side, rather than one above the other. */
    val choicesInARow: Boolean

    private val choices: List<RectF>

    /** Where the readout is centred. */
    val readout: RectF

    init {
        val row = dp(RANGE_ROW_DP)
        val widths = rangeLabelWidths.map { dp(RADIO_DP + LABEL_END_DP) + it }
        val inARow = widths.sum() + dp(CHOICE_GAP_DP)
        val half = well.height() / 2f
        // Under the readout they have the well's width; beside it, half.
        val rowUnder = inARow <= well.width()
        stacked = half >= maxOf(if (rowUnder) row else row * 2, dp(MIN_READOUT_DP))
        choicesInARow = if (stacked) rowUnder else inARow <= well.width() / 2f
        val choiceHeight = if (choicesInARow || stacked) row else minOf(row, well.height() / 2f)
        val groupWidth = if (choicesInARow) inARow else widths.max()
        val groupHeight = if (choicesInARow) choiceHeight else choiceHeight * 2
        val left = if (stacked) well.centerX() - groupWidth / 2f else well.right - groupWidth
        val middle = if (stacked) well.centerY() + half / 2f else well.centerY()
        val top = middle - groupHeight / 2f
        choices =
            if (choicesInARow) {
                listOf(
                    RectF(left, top, left + widths[0], top + choiceHeight),
                    RectF(left + widths[0] + dp(CHOICE_GAP_DP), top, left + inARow, top + choiceHeight),
                )
            } else {
                listOf(
                    RectF(left, top, left + groupWidth, top + choiceHeight),
                    RectF(left, top + choiceHeight, left + groupWidth, top + choiceHeight * 2),
                )
            }
        readout =
            if (stacked) {
                RectF(well.left, well.top, well.right, well.centerY())
            } else {
                RectF(well.left, well.top, left - dp(GAP_DP), well.bottom)
            }
    }

    /** A button in a single slot: every label is sized from this. */
    val singleButton: RectF get() = RectF(0f, 0f, columnWidth, rowHeight).insetAsButton()

    override fun cellAt(
        x: Float,
        y: Float,
    ): Int {
        if (x < 0f || y < 0f || x >= width || y >= height) return -1
        val column = (x / columnWidth).toInt().coerceAtMost(COLUMNS - 1)
        val row = (y / rowHeight).toInt().coerceAtMost(ROWS - 1)
        return slots.indexOf(column to row)
    }

    /** Range choice [row] (0 for C to B, 1 for F to E): its radio ring and label. */
    fun rangeRow(row: Int): RectF = RectF(choices[row])

    override fun rangeRowAt(
        x: Float,
        y: Float,
    ): Int = choices.indexOfFirst { it.contains(x, y) }

    override fun cellTarget(index: Int): Rect = slot(index).toRect()

    override fun rangeTarget(row: Int): Rect = choices[row].toRect()

    private fun RectF.insetAsButton() = apply { inset(dp(BUTTON_INSET_X_DP), dp(BUTTON_INSET_Y_DP)) }

    private fun RectF.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    companion object {
        /** The old grid showed one octave: C to B, or F to E. */
        const val CELLS = 12
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

        /** Between the readout and the choices beside it; the least room the readout gets. */
        private const val GAP_DP = 8f
        private const val MIN_READOUT_DP = 56f

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
