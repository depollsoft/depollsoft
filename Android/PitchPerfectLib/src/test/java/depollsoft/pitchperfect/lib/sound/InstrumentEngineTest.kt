package depollsoft.pitchperfect.lib.sound

import depollsoft.pitchperfect.lib.sound.soundfont.SoundFontBanks
import kotlin.math.log2
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real instrument path on the JVM: the shared SoundFont through the Kotlin TinySoundFont. */
class InstrumentEngineTest {
    private val rate = InstrumentEngine.SAMPLE_RATE
    private val engine = InstrumentEngine(SoundFontBanks.shared())

    private fun render(seconds: Double): ShortArray {
        val out = ShortArray((seconds * rate).toInt())
        val chunk = ShortArray(1024)
        var at = 0
        while (at < out.size) {
            val frames = minOf(chunk.size, out.size - at)
            engine.render(chunk, frames)
            chunk.copyInto(out, at, 0, frames)
            at += frames
        }
        return out
    }

    private fun rms(samples: ShortArray, from: Int = 0, to: Int = samples.size): Double =
        sqrt((from until to).sumOf { samples[it].toDouble() * samples[it] } / (to - from))

    /** The pitch from upward zero crossings, over a steady stretch of a flute-like tone. */
    private fun frequency(samples: ShortArray, from: Int, to: Int): Double {
        var first = -1
        var last = -1
        var crossings = 0
        for (i in from + 1 until to) {
            if (samples[i - 1] < 0 && samples[i] >= 0) {
                if (first < 0) first = i else crossings++
                last = i
            }
        }
        return crossings * rate.toDouble() / (last - first)
    }

    private fun plan(program: Int, key: Int, cents: Double = 0.0, gain: Double = 0.0) = InstrumentNotePlan(program, key, cents, gain)

    @Test
    fun eachNoteGetsAChannelOfItsOwn() {
        val a = engine.start(plan(73, 60))
        val b = engine.start(plan(73, 64))
        val c = engine.start(plan(19, 67))
        assertEquals(listOf(0, 1, 2), listOf(a, b, c).map { engine.channelOf(it) })
        assertEquals(3, engine.heldCount)
        engine.stop(b)
        assertEquals(-1, engine.channelOf(b))
        // B's release is still ringing on its channel, so a new note mustn't retune it.
        val d = engine.start(plan(73, 72))
        assertEquals(3, engine.channelOf(d))
    }

    @Test
    fun aChannelIsReusedOnceItsReleaseHasFinished() {
        val a = engine.start(plan(73, 69))
        render(0.5)
        engine.stop(a)
        render(3.0)
        assertTrue(engine.isIdle)
        assertEquals(0, engine.channelOf(engine.start(plan(73, 69))))
    }

    @Test
    fun aNotesTuningShiftsItsPitch() {
        engine.start(plan(73, 69))
        val a = render(1.5)
        val at440 = frequency(a, rate / 2, rate * 3 / 2)
        val sharp = InstrumentEngine(SoundFontBanks.shared())
        sharp.start(plan(73, 69, cents = 50.0))
        val b = ShortArray(rate * 3 / 2).also { sharp.render(it, it.size) }
        val up = frequency(b, rate / 2, rate * 3 / 2)
        assertEquals(50.0, 1200 * log2(up / at440), 1.0)
    }

    @Test
    fun aNotesGainScalesItsLevel() {
        engine.start(plan(73, 69, gain = -12.0))
        val quiet = rms(render(1.0), rate / 4, rate)
        val loud = InstrumentEngine(SoundFontBanks.shared())
        loud.start(plan(73, 69, gain = -6.0))
        val b = ShortArray(rate).also { loud.render(it, it.size) }
        assertEquals(2.0, rms(b, rate / 4, rate) / quiet, 0.02)
    }

    @Test
    fun aHeldPianoStillSoundsAfterThreeSeconds() {
        engine.start(plan(0, 60, gain = -4.6))
        val out = render(3.1)
        val attack = rms(out, 0, rate / 10)
        val atThreeSeconds = rms(out, rate * 3, out.size)
        assertTrue("attack $attack", attack > 1000)
        // The bank holds plucked and struck notes at their sample loop's level while the key is down.
        assertTrue("at 3 s $atThreeSeconds against $attack", atThreeSeconds > attack / 10)
    }

    @Test
    fun stoppingPlaysTheReleaseThenFallsSilent() {
        val note = engine.start(plan(0, 60, gain = -4.6))
        val held = render(1.0)
        engine.stop(note)
        val after = render(4.0)
        assertTrue("the release starts where the note was", rms(after, 0, rate / 50) > rms(held, rate * 9 / 10, rate) / 4)
        assertTrue("the release dies away", after.takeLast(rate).all { it.toInt() == 0 })
        assertTrue(engine.isIdle)
        assertEquals(0, engine.heldCount)
    }

    @Test
    fun aChordSoundsEveryNote() {
        val single = InstrumentEngine(SoundFontBanks.shared())
        single.start(plan(19, 60, gain = -12.0))
        val one = ShortArray(rate).also { single.render(it, it.size) }

        val notes = listOf(60, 64, 67).map { engine.start(plan(19, it, gain = -12.0)) }
        val chord = render(1.0)
        assertFalse(engine.isIdle)
        assertNotEquals(one.toList(), chord.toList())
        assertTrue(rms(chord, rate / 4, rate) > rms(one, rate / 4, rate) * 1.4)
        notes.forEach(engine::stop)
        render(4.0)
        assertTrue(engine.isIdle)
    }
}
