package depollsoft.pitchperfect

/**
 * The phone's pitch-pipe face: every toggle taps, and a screen reader's activations are silent.
 *
 * @param haptic performs a `HapticFeedbackConstants` effect on the face.
 * @param reduceMotion whether the system asks for animations to be switched off.
 */
class PitchInstrumentState(
    model: PitchPipe,
    haptic: (Int) -> Unit = {},
    reduceMotion: () -> Boolean = { false },
) : InstrumentState<PitchInstrumentGeometry>(
        model,
        ::PitchInstrumentGeometry,
        haptic,
        reduceMotion,
        tapEveryToggle = true,
        screenReaderFeedback = false,
    )
