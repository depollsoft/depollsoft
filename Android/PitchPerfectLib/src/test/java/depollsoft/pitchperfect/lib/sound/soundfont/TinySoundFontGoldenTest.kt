package depollsoft.pitchperfect.lib.sound.soundfont

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Kotlin TinySoundFont against the C original: both replay the clips in
 * src/test/resources/soundfont/clips.txt through the shared SoundFont, and golden.raw holds what
 * the C renders (scripts/pitchperfect/tinysoundfont/make_golden.sh makes both files).
 */
class TinySoundFontGoldenTest {
    private class Clip(val name: String, val samples: Int, val events: List<List<String>>)

    private fun resource(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/soundfont/$name")) { "missing test resource $name" }.use { it.readBytes() }

    private fun clips(): List<Clip> {
        val clips = ArrayList<Clip>()
        var name = ""
        var samples = 0
        var events = ArrayList<List<String>>()
        for (line in String(resource("clips.txt")).lines()) {
            val words = line.trim().split(Regex("\\s+"))
            when (words[0]) {
                "clip" -> {
                    name = words[1]
                    samples = words[2].toInt()
                    events = ArrayList()
                }
                "at" -> events.add(words.drop(1))
                "end" -> clips.add(Clip(name, samples, events))
            }
        }
        return clips
    }

    /** Replays [clip] as tsf_golden.c does: render up to each event, apply it, render the rest. */
    private fun render(bank: SoundFontBank, clip: Clip): ShortArray {
        val synth = TinySoundFont(bank).apply { setOutput(44100, 0f) }
        val buffer = ShortArray(clip.samples)
        var rendered = 0
        fun renderTo(target: Int) {
            if (target > rendered) {
                synth.renderShort(buffer, rendered, target - rendered)
                rendered = target
            }
        }
        for (event in clip.events) {
            renderTo(event[0].toInt())
            val channel = event[2].toInt()
            when (event[1]) {
                "preset" -> synth.channelSetPresetNumber(channel, event[3].toInt())
                "tuning" -> synth.channelSetTuning(channel, event[3].toFloat())
                "on" -> synth.channelNoteOn(channel, event[3].toInt(), event[4].toInt() / 127.0f)
                "off" -> synth.channelNoteOff(channel, event[3].toInt())
                else -> error("unknown event $event")
            }
        }
        renderTo(clip.samples)
        return buffer
    }

    @Test
    fun everyClipMatchesTheCOriginal() {
        val bank = SoundFontBanks.shared()
        val golden = ByteBuffer.wrap(resource("golden.raw")).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val clips = clips()
        assertEquals("clips in golden.raw", golden.remaining(), clips.sumOf { it.samples })
        assertEquals(32, clips.size)

        var worst = 0
        var differing = 0
        var total = 0
        val report = StringBuilder()
        for (clip in clips) {
            val expected = ShortArray(clip.samples).also { golden.get(it) }
            val actual = render(bank, clip)
            var clipWorst = 0
            for (i in expected.indices) {
                val diff = abs(expected[i] - actual[i])
                if (diff != 0) differing++
                if (diff > clipWorst) clipWorst = diff
            }
            total += expected.size
            worst = maxOf(worst, clipWorst)
            report.append("${clip.name}: max ${clipWorst}; ")
            assertTrue("${clip.name} is silent", expected.any { abs(it.toInt()) > 1000 })
        }
        println("TinySoundFont golden: worst difference $worst LSB, $differing of $total samples differ; $report")
        // 1e-3 of full scale is 32 LSB; float rounding differences should stay far below it.
        assertTrue("worst difference $worst LSB: $report", worst <= 32)
    }

    @Test
    fun theBankHoldsEveryInstrumentPitchPerfectOffers() {
        val bank = SoundFontBanks.shared()
        val programs = listOf(0, 4, 6, 11, 19, 20, 21, 22, 24, 46, 48, 52, 56, 71, 73)
        assertEquals(programs, bank.presets.map { it.program })
        assertTrue(bank.presets.all { it.bank == 0 && it.regions.isNotEmpty() })
        assertEquals("Grand Piano", bank.presets[bank.presetIndex(0, 0)].name)
        assertEquals("Harmonica", bank.presets[bank.presetIndex(0, 22)].name)
        assertEquals(-1, bank.presetIndex(0, 1))
    }

    @Test
    fun aHeapCopyOfTheBankRendersTheSameAsTheMappedFile() {
        val mapped = SoundFontBanks.shared()
        val heap = SoundFontBank.load(SoundFontBanks.file().inputStream())
        val clip = clips().first { it.name == "chord" }
        assertTrue(render(mapped, clip).contentEquals(render(heap, clip)))
    }

    @Test
    fun thirteenVoicesRenderFasterThanRealTime() {
        val bank = SoundFontBanks.shared()
        val programs = listOf(0, 4, 6, 11, 19, 20, 21, 22, 24, 46, 48, 52, 56)
        fun renderSecond(): Long {
            val synth = TinySoundFont(bank).apply { setOutput(44100, -12f) }
            programs.forEachIndexed { channel, program ->
                synth.channelSetPresetNumber(channel, program)
                synth.channelSetTuning(channel, 0.07f * channel)
                synth.channelNoteOn(channel, 48 + channel * 2, 100 / 127f)
            }
            // Some presets layer several zones on a note, so there are at least as many voices as notes.
            assertTrue(synth.activeVoiceCount >= programs.size)
            val buffer = ShortArray(44100)
            val start = System.nanoTime()
            // In 1024-sample writes, as an AudioTrack filler would.
            var at = 0
            while (at < buffer.size) {
                val count = minOf(1024, buffer.size - at)
                synth.renderShort(buffer, at, count)
                at += count
            }
            return System.nanoTime() - start
        }
        repeat(3) { renderSecond() } // let the JIT warm up
        val best = (0 until 5).minOf { renderSecond() }
        println("TinySoundFont: 1 s of 13 voices rendered in ${best / 1_000_000.0} ms")
        assertTrue("1 s of 13 voices took ${best / 1_000_000} ms", best < 250_000_000L)
    }
}

/** The repo's shared SoundFont, found from the test's working directory and memory-mapped. */
internal object SoundFontBanks {
    private var bank: SoundFontBank? = null

    fun file(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "shared/pitchperfect/PitchPerfectInstruments.sf2")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("shared/pitchperfect/PitchPerfectInstruments.sf2 not found above ${File("").absolutePath}")
    }

    @Synchronized
    fun shared(): SoundFontBank =
        bank ?: FileChannel.open(file().toPath(), StandardOpenOption.READ).use { channel ->
            SoundFontBank.load(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()))
        }.also { bank = it }
}
