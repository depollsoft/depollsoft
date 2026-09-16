package depollsoft.pitchperfect

/** Owns each finger's note independently so chords survive pointer changes. */
class PitchMultiTouchTracker(
    private val onStart: (Int) -> Unit,
    private val onStop: (Int) -> Unit,
) {
    private val pointerCells = mutableMapOf<Int, Int>()

    fun press(
        pointerId: Int,
        cell: Int,
    ) {
        if (pointerCells[pointerId] == cell) return
        release(pointerId)
        val alreadyHeld = pointerCells.containsValue(cell)
        pointerCells[pointerId] = cell
        if (!alreadyHeld) onStart(cell)
    }

    fun move(
        pointerId: Int,
        cell: Int?,
    ): Boolean {
        val current = pointerCells[pointerId] ?: return false
        if (current == cell) return false
        release(pointerId)
        if (cell != null) press(pointerId, cell)
        return true
    }

    fun release(pointerId: Int) {
        val cell = pointerCells.remove(pointerId) ?: return
        if (!pointerCells.containsValue(cell)) onStop(cell)
    }

    fun clear() {
        for (pointerId in pointerCells.keys.toList()) release(pointerId)
    }
}
