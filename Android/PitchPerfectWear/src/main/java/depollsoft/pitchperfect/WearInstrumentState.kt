package depollsoft.pitchperfect

/**
 * The watch's pitch-pipe face: the crown picks the range, only toggling a note on taps, and a
 * screen reader's activations tap and tick as a finger's do.
 *
 * @param haptic performs a `HapticFeedbackConstants` effect on the face.
 * @param reduceMotion whether the system asks for animations to be switched off.
 */
class WearInstrumentState(
    model: PitchPipe,
    haptic: (Int) -> Unit = {},
    reduceMotion: () -> Boolean = { false },
) : InstrumentState<WearInstrumentGeometry>(
        model,
        ::WearInstrumentGeometry,
        haptic,
        reduceMotion,
        tapEveryToggle = false,
        screenReaderFeedback = true,
    ) {
    /** The crown turned: toward the wearer picks the high range, away picks the low one. */
    fun rotate(axisScroll: Float) {
        if (axisScroll != 0f) selectRange(axisScroll < 0f)
    }
}
