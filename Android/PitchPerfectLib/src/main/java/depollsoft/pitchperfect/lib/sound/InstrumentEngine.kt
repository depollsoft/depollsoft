package depollsoft.pitchperfect.lib.sound

import depollsoft.pitchperfect.lib.sound.soundfont.SoundFontBank
import depollsoft.pitchperfect.lib.sound.soundfont.TinySoundFont
import kotlin.math.pow

/**
 * Every instrument note Android sounds, in one TinySoundFont synth (docs/pitchperfect-note-sounds.md).
 * Each note gets a MIDI channel of its own, set to its program, its tuning (reference pitch plus the
 * key's measured correction) and its instrument's gain, so a chord's notes are each in tune. Stopping
 * a note is a note-off: the synth plays the instrument's release, and the channel is only reused once
 * that has finished. Not thread-safe: [InstrumentPlayer] serialises notes and rendering.
 */
class InstrumentEngine(bank: SoundFontBank, sampleRate: Int = SAMPLE_RATE) {
    private val synth = TinySoundFont(bank).apply { setOutput(sampleRate, 0f) }

    /** Per channel: the note holding it (0 when free or releasing), its key, and when it last started or stopped. */
    private val heldBy = ArrayList<Long>()
    private val keys = ArrayList<Int>()
    private val lastUsed = ArrayList<Long>()
    private val channelOf = HashMap<Long, Int>()
    private var nextNote = 1L
    private var clock = 0L

    /** Notes started and not yet stopped. */
    val heldCount: Int get() = channelOf.size

    /** Whether nothing is held and every release has finished, so rendering would only give silence. */
    val isIdle: Boolean get() = channelOf.isEmpty() && synth.activeVoiceCount == 0

    /** The channel [note] sounds on, or -1 once it has stopped. */
    fun channelOf(note: Long): Int = channelOf[note] ?: -1

    /** Starts [plan]'s note and returns its id for [stop]. */
    fun start(plan: InstrumentNotePlan): Long {
        val channel = takeChannel()
        if (!synth.channelSetPresetNumber(channel, plan.program)) synth.channelSetPresetNumber(channel, 0)
        synth.channelSetTuning(channel, (plan.pitchCents / 100).toFloat())
        synth.channelSetVolume(channel, 10.0.pow(plan.gainDb / 20).toFloat())
        synth.channelNoteOn(channel, plan.key, InstrumentNote.VELOCITY / 127.0f)
        val note = nextNote++
        heldBy[channel] = note
        keys[channel] = plan.key
        lastUsed[channel] = clock++
        channelOf[note] = channel
        return note
    }

    /** Releases [note]; its instrument's release still plays. */
    fun stop(note: Long) {
        val channel = channelOf.remove(note) ?: return
        heldBy[channel] = 0L
        lastUsed[channel] = clock++
        synth.channelNoteOff(channel, keys[channel])
    }

    /** Renders the next [frames] mono samples into [buffer]. */
    fun render(buffer: ShortArray, frames: Int) = synth.renderShort(buffer, 0, frames)

    /**
     * A channel with nothing sounding on it, so retuning it can't bend a release still ringing. Past
     * [MAX_CHANNELS], the release that has played longest is cut short (10 ms), or failing that the
     * note held longest.
     */
    private fun takeChannel(): Int {
        for (channel in heldBy.indices) {
            if (heldBy[channel] == 0L && synth.channelActiveVoiceCount(channel) == 0) return channel
        }
        if (heldBy.size < MAX_CHANNELS) {
            heldBy.add(0L)
            keys.add(0)
            lastUsed.add(0L)
            return heldBy.size - 1
        }
        val releasing = heldBy.indices.filter { heldBy[it] == 0L }
        val channel = (releasing.ifEmpty { heldBy.indices.toList() }).minBy { lastUsed[it] }
        if (heldBy[channel] != 0L) channelOf.remove(heldBy[channel])
        heldBy[channel] = 0L
        synth.channelSoundsOffAll(channel)
        return channel
    }

    companion object {
        const val SAMPLE_RATE = 44100

        /** More than every cell of the pitch pipe held at once, with their releases. */
        const val MAX_CHANNELS = 32
    }
}
