/*
 * A Kotlin port of TinySoundFont's SoundFont 2 loader: tsf.h v0.9 at commit 853a0a17,
 * https://github.com/schellingb/TinySoundFont. The port keeps TinySoundFont's arithmetic and
 * order of operations so it renders what the C original renders; see TinySoundFont.kt.
 *
 * LICENSE (MIT)
 *
 * Copyright (C) 2017-2025 Bernhard Schelling
 * Based on SFZero, Copyright (C) 2012 Steve Folta (https://github.com/stevefolta/SFZero)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package depollsoft.pitchperfect.lib.sound.soundfont

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.pow

internal const val LOOPMODE_NONE = 0
internal const val LOOPMODE_CONTINUOUS = 1
internal const val LOOPMODE_SUSTAIN = 2

/** Masks to the range of a C `unsigned int`, whose arithmetic TinySoundFont's sample offsets use. */
private fun u32(value: Long): Long = value and 0xFFFFFFFFL

internal fun timecents2Secsd(timecents: Double): Double = 2.0.pow(timecents / 1200.0)

internal fun timecents2Secsf(timecents: Float): Float = 2.0f.pow(timecents / 1200.0f)

internal fun cents2Hertz(cents: Float): Float = 8.176f * 2.0f.pow(cents / 1200.0f)

internal fun decibelsToGain(db: Float): Float = if (db > -100f) 10.0f.pow(db * 0.05f) else 0f

internal fun gainToDecibels(gain: Float): Float = if (gain <= .00001f) -100f else (20.0 * kotlin.math.log10(gain.toDouble())).toFloat()

/** An envelope's times and levels: timecents while loading, then seconds (tsf_envelope). */
internal class Envelope {
    var delay = 0f
    var attack = 0f
    var hold = 0f
    var decay = 0f
    var sustain = 0f
    var release = 0f
    var keynumToHold = 0f
    var keynumToDecay = 0f

    fun copyFrom(other: Envelope) {
        delay = other.delay
        attack = other.attack
        hold = other.hold
        decay = other.decay
        sustain = other.sustain
        release = other.release
        keynumToHold = other.keynumToHold
        keynumToDecay = other.keynumToDecay
    }

    /** Timecents to seconds (tsf_region_envtosecs). */
    fun toSeconds(sustainIsGain: Boolean) {
        // Pin very short EG segments: timecents don't get to zero, and the EG is happier with zero.
        delay = if (delay < -11950.0f) 0.0f else timecents2Secsf(delay)
        attack = if (attack < -11950.0f) 0.0f else timecents2Secsf(attack)
        release = if (release < -11950.0f) 0.0f else timecents2Secsf(release)
        // Hold and decay that depend on the key stay in timecents until the note starts.
        if (keynumToHold == 0f) hold = if (hold < -11950.0f) 0.0f else timecents2Secsf(hold)
        if (keynumToDecay == 0f) decay = if (decay < -11950.0f) 0.0f else timecents2Secsf(decay)
        sustain =
            when {
                sustain < 0.0f -> 0.0f
                sustainIsGain -> decibelsToGain(-sustain / 10.0f)
                else -> 1.0f - (sustain / 1000.0f)
            }
    }
}

/** One sample zone of a preset, with every generator applied (tsf_region). */
internal class Region {
    var loopMode = LOOPMODE_NONE
    var sampleRate = 0L
    var lokey = 0
    var hikey = 127
    var lovel = 0
    var hivel = 127
    var group = 0L
    var offset = 0L
    var end = 0L
    var loopStart = 0L
    var loopEnd = 0L
    var transpose = 0
    var tune = 0
    var pitchKeycenter = 60
    var pitchKeytrack = 0
    var attenuation = 0f
    var pan = 0f
    val ampenv = Envelope()
    val modenv = Envelope()
    var initialFilterQ = 0
    var initialFilterFc = 0
    var modEnvToPitch = 0
    var modEnvToFilterFc = 0
    var modLfoToFilterFc = 0
    var modLfoToVolume = 0
    var delayModLFO = 0f
    var freqModLFO = 0
    var modLfoToPitch = 0
    var delayVibLFO = 0f
    var freqVibLFO = 0
    var vibLfoToPitch = 0

    fun copy(): Region = Region().also { it.copyFrom(this) }

    fun copyFrom(o: Region) {
        loopMode = o.loopMode
        sampleRate = o.sampleRate
        lokey = o.lokey
        hikey = o.hikey
        lovel = o.lovel
        hivel = o.hivel
        group = o.group
        offset = o.offset
        end = o.end
        loopStart = o.loopStart
        loopEnd = o.loopEnd
        transpose = o.transpose
        tune = o.tune
        pitchKeycenter = o.pitchKeycenter
        pitchKeytrack = o.pitchKeytrack
        attenuation = o.attenuation
        pan = o.pan
        ampenv.copyFrom(o.ampenv)
        modenv.copyFrom(o.modenv)
        initialFilterQ = o.initialFilterQ
        initialFilterFc = o.initialFilterFc
        modEnvToPitch = o.modEnvToPitch
        modEnvToFilterFc = o.modEnvToFilterFc
        modLfoToFilterFc = o.modLfoToFilterFc
        modLfoToVolume = o.modLfoToVolume
        delayModLFO = o.delayModLFO
        freqModLFO = o.freqModLFO
        modLfoToPitch = o.modLfoToPitch
        delayVibLFO = o.delayVibLFO
        freqVibLFO = o.freqVibLFO
        vibLfoToPitch = o.vibLfoToPitch
    }

    /** Resets to zero, or for an absolute (instrument) region to the SF2 defaults (tsf_region_clear). */
    fun clear(forRelative: Boolean) {
        copyFrom(Region())
        hikey = 127
        hivel = 127
        pitchKeycenter = 60 // C4
        if (forRelative) return
        pitchKeytrack = 100
        pitchKeycenter = -1
        // SF2 defaults in timecents.
        for (env in arrayOf(ampenv, modenv)) {
            env.delay = -12000.0f
            env.attack = -12000.0f
            env.hold = -12000.0f
            env.decay = -12000.0f
            env.release = -12000.0f
        }
        initialFilterFc = 13500
        delayModLFO = -12000.0f
        delayVibLFO = -12000.0f
    }

    /** Applies one generator; [raw] is its 16-bit amount (tsf_region_operator with an amount). */
    fun apply(genOper: Int, raw: Int) {
        if (genOper >= 59) return
        val s = raw.toShort().toInt()
        val word = raw and 0xFFFF
        when (genOper) {
            0 -> offset = u32(offset + s)
            1 -> end = u32(end + s)
            2 -> loopStart = u32(loopStart + s)
            3 -> loopEnd = u32(loopEnd + s)
            4 -> offset = u32(offset + (s shl 15))
            5 -> modLfoToPitch = s
            6 -> vibLfoToPitch = s
            7 -> modEnvToPitch = s
            8 -> initialFilterFc = s
            9 -> initialFilterQ = s
            10 -> modLfoToFilterFc = s
            11 -> modEnvToFilterFc = s
            12 -> end = u32(end + (s shl 15))
            13 -> modLfoToVolume = s
            17 -> pan = s.toFloat()
            21 -> delayModLFO = s.toFloat()
            22 -> freqModLFO = s
            23 -> delayVibLFO = s.toFloat()
            24 -> freqVibLFO = s
            25 -> modenv.delay = s.toFloat()
            26 -> modenv.attack = s.toFloat()
            27 -> modenv.hold = s.toFloat()
            28 -> modenv.decay = s.toFloat()
            29 -> modenv.sustain = s.toFloat()
            30 -> modenv.release = s.toFloat()
            31 -> modenv.keynumToHold = s.toFloat()
            32 -> modenv.keynumToDecay = s.toFloat()
            33 -> ampenv.delay = s.toFloat()
            34 -> ampenv.attack = s.toFloat()
            35 -> ampenv.hold = s.toFloat()
            36 -> ampenv.decay = s.toFloat()
            37 -> ampenv.sustain = s.toFloat()
            38 -> ampenv.release = s.toFloat()
            39 -> ampenv.keynumToHold = s.toFloat()
            40 -> ampenv.keynumToDecay = s.toFloat()
            43 -> {
                lokey = raw and 0xFF
                hikey = (raw shr 8) and 0xFF
            }
            44 -> {
                lovel = raw and 0xFF
                hivel = (raw shr 8) and 0xFF
            }
            45 -> loopStart = u32(loopStart + (s shl 15))
            48 -> attenuation = s.toFloat()
            50 -> loopEnd = u32(loopEnd + (s shl 15))
            51 -> transpose = s
            52 -> tune = s
            54 -> loopMode = if (word and 3 == 3) LOOPMODE_SUSTAIN else if (word and 3 == 1) LOOPMODE_CONTINUOUS else LOOPMODE_NONE
            56 -> pitchKeytrack = s
            57 -> group = word.toLong()
            58 -> pitchKeycenter = s
        }
    }

    /** Adds a preset's relative region and clamps, in generator order (tsf_region_operator merging). */
    fun merge(m: Region) {
        offset = u32(offset + m.offset)
        end = u32(end + m.end)
        loopStart = u32(loopStart + m.loopStart)
        loopEnd = u32(loopEnd + m.loopEnd)
        modLfoToPitch = clamp(modLfoToPitch + m.modLfoToPitch, -12000, 12000)
        vibLfoToPitch = clamp(vibLfoToPitch + m.vibLfoToPitch, -12000, 12000)
        modEnvToPitch = clamp(modEnvToPitch + m.modEnvToPitch, -12000, 12000)
        initialFilterFc = clamp(initialFilterFc + m.initialFilterFc, 1500, 13500)
        initialFilterQ = clamp(initialFilterQ + m.initialFilterQ, 0, 960)
        modLfoToFilterFc = clamp(modLfoToFilterFc + m.modLfoToFilterFc, -12000, 12000)
        modEnvToFilterFc = clamp(modEnvToFilterFc + m.modEnvToFilterFc, -12000, 12000)
        modLfoToVolume = clamp(modLfoToVolume + m.modLfoToVolume, -960, 960)
        pan = clamp((pan + m.pan) * 0.001f, -0.5f, 0.5f)
        delayModLFO = clamp((delayModLFO + m.delayModLFO) * 1.0f, -12000.0f, 5000.0f)
        freqModLFO = clamp(freqModLFO + m.freqModLFO, -16000, 4500)
        delayVibLFO = clamp((delayVibLFO + m.delayVibLFO) * 1.0f, -12000.0f, 5000.0f)
        freqVibLFO = clamp(freqVibLFO + m.freqVibLFO, -16000, 4500)
        mergeEnvelope(modenv, m.modenv, 1000.0f)
        mergeEnvelope(ampenv, m.ampenv, 1440.0f)
        attenuation = clamp((attenuation + m.attenuation) * 0.01f, 0.0f, 14.4f)
        transpose += m.transpose
        tune += m.tune
        pitchKeytrack += m.pitchKeytrack
    }

    private fun mergeEnvelope(e: Envelope, m: Envelope, sustainMax: Float) {
        e.delay = clamp((e.delay + m.delay) * 1.0f, -12000.0f, 5000.0f)
        e.attack = clamp((e.attack + m.attack) * 1.0f, -12000.0f, 8000.0f)
        e.hold = clamp((e.hold + m.hold) * 1.0f, -12000.0f, 5000.0f)
        e.decay = clamp((e.decay + m.decay) * 1.0f, -12000.0f, 8000.0f)
        e.sustain = clamp((e.sustain + m.sustain) * 1.0f, 0.0f, sustainMax)
        e.release = clamp((e.release + m.release) * 1.0f, -12000.0f, 8000.0f)
        e.keynumToHold = clamp((e.keynumToHold + m.keynumToHold) * 1.0f, -1200.0f, 1200.0f)
        e.keynumToDecay = clamp((e.keynumToDecay + m.keynumToDecay) * 1.0f, -1200.0f, 1200.0f)
    }

    private fun clamp(value: Int, min: Int, max: Int): Int = if (value < min) min else if (value > max) max else value

    private fun clamp(value: Float, min: Float, max: Float): Float = if (value < min) min else if (value > max) max else value
}

/** A playable instrument of the bank: a GM program in a bank, and its zones (tsf_preset). */
class Preset internal constructor(
    val name: String,
    val program: Int,
    val bank: Int,
    internal val regions: Array<Region>,
)

/**
 * A SoundFont 2 bank's presets and sample data, as TinySoundFont loads it. Samples stay 16-bit in
 * the buffer the bank was loaded from, so a memory-mapped file costs no heap; each is read as
 * `sample / 32767.0` exactly as TinySoundFont converts them.
 */
class SoundFontBank private constructor(
    val presets: List<Preset>,
    private val samples: ShortBuffer,
    /** The number of 16-bit samples in the bank. */
    val sampleCount: Int,
) {
    internal fun sample(index: Long): Float =
        if (index < sampleCount) (samples.get(index.toInt()).toDouble() / 32767.0).toFloat() else 0f

    /** The index into [presets] of GM [program] in [bank], or -1 (tsf_get_presetindex). */
    fun presetIndex(bank: Int, program: Int): Int = presets.indexOfFirst { it.program == program && it.bank == bank }

    companion object {
        /** Reads a bank from the whole of [stream]; the sample data lands on the heap. */
        @JvmStatic
        fun load(stream: InputStream): SoundFontBank = load(ByteBuffer.wrap(stream.readBytes()))

        /** Reads a bank from [buffer] (e.g. a memory-mapped file), which it keeps for its samples. */
        @JvmStatic
        fun load(buffer: ByteBuffer): SoundFontBank {
            val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val base = data.position()
            fun fourcc(at: Int) = String(ByteArray(4) { data.get(at + it) }, Charsets.ISO_8859_1)
            require(fourcc(base) == "RIFF" && fourcc(base + 8) == "sfbk") { "Not a SoundFont 2 bank" }
            val riffEnd = base + 8 + data.getInt(base + 4)

            var hydra: Hydra? = null
            var smplAt = -1
            var smplCount = 0
            var at = base + 12
            while (at + 12 <= riffEnd && at + 12 <= data.limit()) {
                val size = data.getInt(at + 4)
                if (fourcc(at) == "LIST") {
                    val listEnd = at + 8 + size
                    when (fourcc(at + 8)) {
                        "pdta" -> hydra = Hydra.read(data, at + 12, listEnd)
                        "sdta" -> {
                            var sub = at + 12
                            while (sub + 8 <= listEnd) {
                                val subSize = data.getInt(sub + 4)
                                if (fourcc(sub) == "smpl" && smplAt < 0 && subSize >= 2) {
                                    smplAt = sub + 8
                                    smplCount = subSize / 2
                                }
                                sub += 8 + subSize
                            }
                        }
                    }
                }
                at += 8 + size
            }
            requireNotNull(hydra) { "SoundFont has no preset data" }
            require(smplAt >= 0) { "SoundFont has no sample data" }

            val samples =
                (data.duplicate().position(smplAt) as ByteBuffer)
                    .slice()
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
            samples.limit(smplCount)
            return SoundFontBank(loadPresets(hydra, smplCount.toLong()), samples, smplCount)
        }

        /** tsf_load_presets: every preset's zones with instrument and preset generators combined. */
        private fun loadPresets(hydra: Hydra, fontSampleCount: Long): List<Preset> {
            val presetNum = hydra.phdrPreset.size - 1
            val presets = arrayOfNulls<Preset>(presetNum)
            for (ph in 0 until presetNum) {
                var sortedIndex = 0
                for (other in 0 until presetNum) {
                    if (other == ph || hydra.phdrBank[other] > hydra.phdrBank[ph]) continue
                    else if (hydra.phdrBank[other] < hydra.phdrBank[ph]) sortedIndex++
                    else if (hydra.phdrPreset[other] > hydra.phdrPreset[ph]) continue
                    else if (hydra.phdrPreset[other] < hydra.phdrPreset[ph]) sortedIndex++
                    else if (other < ph) sortedIndex++
                }

                val regions = ArrayList<Region>()
                var globalRegion = Region().apply { clear(true) }
                val firstBag = hydra.phdrBag[ph]
                for (pb in firstBag until hydra.phdrBag[ph + 1]) {
                    val presetRegion = globalRegion.copy()
                    var hadGenInstrument = false
                    for (pg in hydra.pbagGen[pb] until hydra.pbagGen[pb + 1]) {
                        val oper = hydra.pgenOper[pg]
                        if (oper == GEN_INSTRUMENT) {
                            val whichInst = hydra.pgenAmount[pg]
                            if (whichInst >= hydra.instBag.size) continue
                            var instRegion = Region().apply { clear(false) }
                            val firstInstBag = hydra.instBag[whichInst]
                            for (ib in firstInstBag until hydra.instBag[whichInst + 1]) {
                                val zoneRegion = instRegion.copy()
                                var hadSampleID = false
                                for (ig in hydra.ibagGen[ib] until hydra.ibagGen[ib + 1]) {
                                    val zoneOper = hydra.igenOper[ig]
                                    if (zoneOper != GEN_SAMPLE_ID) {
                                        zoneRegion.apply(zoneOper, hydra.igenAmount[ig])
                                        continue
                                    }
                                    // The preset's key and velocity ranges filter the zone's.
                                    if (zoneRegion.hikey < presetRegion.lokey || zoneRegion.lokey > presetRegion.hikey) continue
                                    if (zoneRegion.hivel < presetRegion.lovel || zoneRegion.lovel > presetRegion.hivel) continue
                                    if (presetRegion.lokey > zoneRegion.lokey) zoneRegion.lokey = presetRegion.lokey
                                    if (presetRegion.hikey < zoneRegion.hikey) zoneRegion.hikey = presetRegion.hikey
                                    if (presetRegion.lovel > zoneRegion.lovel) zoneRegion.lovel = presetRegion.lovel
                                    if (presetRegion.hivel < zoneRegion.hivel) zoneRegion.hivel = presetRegion.hivel

                                    zoneRegion.merge(presetRegion)
                                    zoneRegion.ampenv.toSeconds(sustainIsGain = true)
                                    zoneRegion.modenv.toSeconds(sustainIsGain = false)
                                    zoneRegion.delayModLFO = if (zoneRegion.delayModLFO < -11950.0f) 0.0f else timecents2Secsf(zoneRegion.delayModLFO)
                                    zoneRegion.delayVibLFO = if (zoneRegion.delayVibLFO < -11950.0f) 0.0f else timecents2Secsf(zoneRegion.delayVibLFO)

                                    // Sample positions.
                                    val s = hydra.igenAmount[ig]
                                    zoneRegion.offset = u32(zoneRegion.offset + hydra.shdrStart[s])
                                    zoneRegion.end = u32(zoneRegion.end + hydra.shdrEnd[s])
                                    zoneRegion.loopStart = u32(zoneRegion.loopStart + hydra.shdrStartLoop[s])
                                    zoneRegion.loopEnd = u32(zoneRegion.loopEnd + hydra.shdrEndLoop[s])
                                    if (hydra.shdrEndLoop[s] > 0) zoneRegion.loopEnd = u32(zoneRegion.loopEnd - 1)
                                    if (zoneRegion.loopEnd > fontSampleCount) zoneRegion.loopEnd = fontSampleCount
                                    if (zoneRegion.pitchKeycenter == -1) zoneRegion.pitchKeycenter = hydra.shdrOriginalPitch[s]
                                    zoneRegion.tune += hydra.shdrPitchCorrection[s]
                                    zoneRegion.sampleRate = hydra.shdrSampleRate[s]
                                    if (zoneRegion.end != 0L && zoneRegion.end < fontSampleCount) zoneRegion.end++ else zoneRegion.end = fontSampleCount

                                    regions.add(zoneRegion.copy())
                                    hadSampleID = true
                                }
                                // The instrument's global zone.
                                if (ib == firstInstBag && !hadSampleID) instRegion = zoneRegion
                            }
                            hadGenInstrument = true
                        } else {
                            presetRegion.apply(oper, hydra.pgenAmount[pg])
                        }
                    }
                    // The preset's global zone.
                    if (pb == firstBag && !hadGenInstrument) globalRegion = presetRegion
                }
                presets[sortedIndex] = Preset(hydra.phdrName[ph], hydra.phdrPreset[ph], hydra.phdrBank[ph], regions.toTypedArray())
            }
            return presets.map { requireNotNull(it) }
        }

        private const val GEN_INSTRUMENT = 41
        private const val GEN_SAMPLE_ID = 53
    }
}

/** The pdta chunk's records, as parallel arrays (tsf_hydra); modulators are ignored, as in TinySoundFont. */
private class Hydra(
    val phdrName: Array<String>,
    val phdrPreset: IntArray,
    val phdrBank: IntArray,
    val phdrBag: IntArray,
    val pbagGen: IntArray,
    val pgenOper: IntArray,
    val pgenAmount: IntArray,
    val instBag: IntArray,
    val ibagGen: IntArray,
    val igenOper: IntArray,
    val igenAmount: IntArray,
    val shdrStart: LongArray,
    val shdrEnd: LongArray,
    val shdrStartLoop: LongArray,
    val shdrEndLoop: LongArray,
    val shdrSampleRate: LongArray,
    val shdrOriginalPitch: IntArray,
    val shdrPitchCorrection: IntArray,
) {
    companion object {
        fun read(data: ByteBuffer, start: Int, end: Int): Hydra {
            val chunks = HashMap<String, Pair<Int, Int>>()
            var at = start
            while (at + 8 <= end) {
                val id = String(ByteArray(4) { data.get(at + it) }, Charsets.ISO_8859_1)
                val size = data.getInt(at + 4)
                chunks.putIfAbsent(id, (at + 8) to size)
                at += 8 + size
            }
            fun records(id: String, recordSize: Int): Pair<Int, Int> {
                val (offset, size) = requireNotNull(chunks[id]) { "SoundFont is missing $id" }
                require(size % recordSize == 0) { "SoundFont $id is malformed" }
                return offset to size / recordSize
            }
            fun u16(at: Int) = data.getShort(at).toInt() and 0xFFFF
            fun u32(at: Int) = data.getInt(at).toLong() and 0xFFFFFFFFL

            val (phdr, phdrNum) = records("phdr", 38)
            val (pbag, pbagNum) = records("pbag", 4)
            val (pgen, pgenNum) = records("pgen", 4)
            val (inst, instNum) = records("inst", 22)
            val (ibag, ibagNum) = records("ibag", 4)
            val (igen, igenNum) = records("igen", 4)
            val (shdr, shdrNum) = records("shdr", 46)
            records("pmod", 10)
            records("imod", 10)
            return Hydra(
                phdrName = Array(phdrNum) { i ->
                    val bytes = ByteArray(20) { data.get(phdr + 38 * i + it) }
                    String(bytes, 0, bytes.indexOf(0).let { if (it < 0) 19 else it }, Charsets.ISO_8859_1)
                },
                phdrPreset = IntArray(phdrNum) { u16(phdr + 38 * it + 20) },
                phdrBank = IntArray(phdrNum) { u16(phdr + 38 * it + 22) },
                phdrBag = IntArray(phdrNum) { u16(phdr + 38 * it + 24) },
                pbagGen = IntArray(pbagNum) { u16(pbag + 4 * it) },
                pgenOper = IntArray(pgenNum) { u16(pgen + 4 * it) },
                pgenAmount = IntArray(pgenNum) { u16(pgen + 4 * it + 2) },
                instBag = IntArray(instNum) { u16(inst + 22 * it + 20) },
                ibagGen = IntArray(ibagNum) { u16(ibag + 4 * it) },
                igenOper = IntArray(igenNum) { u16(igen + 4 * it) },
                igenAmount = IntArray(igenNum) { u16(igen + 4 * it + 2) },
                shdrStart = LongArray(shdrNum) { u32(shdr + 46 * it + 20) },
                shdrEnd = LongArray(shdrNum) { u32(shdr + 46 * it + 24) },
                shdrStartLoop = LongArray(shdrNum) { u32(shdr + 46 * it + 28) },
                shdrEndLoop = LongArray(shdrNum) { u32(shdr + 46 * it + 32) },
                shdrSampleRate = LongArray(shdrNum) { u32(shdr + 46 * it + 36) },
                shdrOriginalPitch = IntArray(shdrNum) { data.get(shdr + 46 * it + 40).toInt() and 0xFF },
                shdrPitchCorrection = IntArray(shdrNum) { data.get(shdr + 46 * it + 41).toInt() },
            )
        }
    }
}
