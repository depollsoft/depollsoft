package depollsoft.pitchperfect.lib.sound

/**
 * The voices a note can sound in (docs/pitchperfect-note-sounds.md). [id] is what is stored and
 * synced, so it never changes; the order is the order the picker lists them in, under their
 * [section]'s heading. An instrument's [program] is its General MIDI program in the bundled
 * SoundFont.
 */
enum class NoteSound(val id: String, val kind: Kind, val section: Section, val program: Int = -1) {
    PITCH_PIPE("pitchPipe", Kind.PITCH_PIPE, Section.DEFAULT),
    ORGAN("organ", Kind.INSTRUMENT, Section.SUSTAINED, 19),
    REED_ORGAN("reedOrgan", Kind.INSTRUMENT, Section.SUSTAINED, 20),
    ACCORDION("accordion", Kind.INSTRUMENT, Section.SUSTAINED, 21),
    HARMONICA("harmonica", Kind.INSTRUMENT, Section.SUSTAINED, 22),
    STRINGS("strings", Kind.INSTRUMENT, Section.SUSTAINED, 48),
    CHOIR("choir", Kind.INSTRUMENT, Section.SUSTAINED, 52),
    TRUMPET("trumpet", Kind.INSTRUMENT, Section.SUSTAINED, 56),
    CLARINET("clarinet", Kind.INSTRUMENT, Section.SUSTAINED, 71),
    FLUTE("flute", Kind.INSTRUMENT, Section.SUSTAINED, 73),
    SINE("sine", Kind.WAVE, Section.WAVES),
    TRIANGLE("triangle", Kind.WAVE, Section.WAVES),
    SQUARE("square", Kind.WAVE, Section.WAVES),
    SAWTOOTH("sawtooth", Kind.WAVE, Section.WAVES),
    PIANO("piano", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 0),
    ELECTRIC_PIANO("electricPiano", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 4),
    HARPSICHORD("harpsichord", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 6),
    VIBRAPHONE("vibraphone", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 11),
    GUITAR("guitar", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 24),
    HARP("harp", Kind.INSTRUMENT, Section.PLUCKED_AND_STRUCK, 46),
    ;

    enum class Kind { PITCH_PIPE, WAVE, INSTRUMENT }

    /** The picker's groups, in order; the default sits above them all, without a heading. */
    enum class Section { DEFAULT, SUSTAINED, WAVES, PLUCKED_AND_STRUCK }

    companion object {
        @JvmField val DEFAULT = PITCH_PIPE

        /** The sound stored as [id]; one this version doesn't know (or null) reads as the default. */
        @JvmStatic
        fun fromId(id: String?): NoteSound = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
