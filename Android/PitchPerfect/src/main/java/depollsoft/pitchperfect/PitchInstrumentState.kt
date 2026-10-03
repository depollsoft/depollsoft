package depollsoft.pitchperfect

/**
 * The phone's pitch-pipe face: every toggle taps, and a screen reader's activations are silent.
 *
 * @param haptic performs a `HapticFeedbackConstants` effect on the face.
 * @param reduceMotion whether the system asks for animations to be switched off.
 * @param onNoteStarted runs when someone starts a note (see [InstrumentState]).
 */
class PitchInstrumentState(
    model: PitchPipe,
    haptic: (Int) -> Unit = {},
    reduceMotion: () -> Boolean = { false },
    onNoteStarted: () -> Unit = {},
) : InstrumentState<PitchInstrumentGeometry>(
        model,
        ::PitchInstrumentGeometry,
        haptic,
        reduceMotion,
        tapEveryToggle = true,
        screenReaderFeedback = false,
        onNoteStarted = onNoteStarted,
    )
