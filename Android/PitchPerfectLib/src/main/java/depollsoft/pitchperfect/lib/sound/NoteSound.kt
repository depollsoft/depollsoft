package depollsoft.pitchperfect.lib.sound

/**
 * The voices a note can sound in (docs/pitchperfect-note-sounds.md). [id] is what is stored and
 * synced, so it never changes; the order is the order the picker lists them in. An instrument's
 * [program] is its General MIDI program.
 */
enum class NoteSound(val id: String, val kind: Kind, val program: Int = -1) {
    PITCH_PIPE("pitchPipe", Kind.PITCH_PIPE),
    SINE("sine", Kind.WAVE),
    TRIANGLE("triangle", Kind.WAVE),
    SQUARE("square", Kind.WAVE),
    SAWTOOTH("sawtooth", Kind.WAVE),
    PIANO("piano", Kind.INSTRUMENT, 0),
    ELECTRIC_PIANO("electricPiano", Kind.INSTRUMENT, 4),
    HARPSICHORD("harpsichord", Kind.INSTRUMENT, 6),
    VIBRAPHONE("vibraphone", Kind.INSTRUMENT, 11),
    ORGAN("organ", Kind.INSTRUMENT, 19),
    ACCORDION("accordion", Kind.INSTRUMENT, 21),
    GUITAR("guitar", Kind.INSTRUMENT, 24),
    HARP("harp", Kind.INSTRUMENT, 46),
    STRINGS("strings", Kind.INSTRUMENT, 48),
    CHOIR("choir", Kind.INSTRUMENT, 52),
    TRUMPET("trumpet", Kind.INSTRUMENT, 56),
    CLARINET("clarinet", Kind.INSTRUMENT, 71),
    FLUTE("flute", Kind.INSTRUMENT, 73),
    ;

    enum class Kind { PITCH_PIPE, WAVE, INSTRUMENT }

    companion object {
        @JvmField val DEFAULT = PITCH_PIPE

        /** The sound stored as [id]; one this version doesn't know (or null) reads as the default. */
        @JvmStatic
        fun fromId(id: String?): NoteSound = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
