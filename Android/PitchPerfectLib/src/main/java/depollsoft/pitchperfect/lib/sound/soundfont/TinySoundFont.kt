/*
 * A Kotlin port of TinySoundFont's synthesizer: tsf.h v0.9 at commit 853a0a17,
 * https://github.com/schellingb/TinySoundFont. It renders mono only (TSF_MONO) and keeps
 * TinySoundFont's float and double arithmetic in the same order, so its output matches the C
 * original's (TinySoundFontGoldenTest compares them). Like the original it ignores SoundFont
 * modulators, chorus and reverb.
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

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

private const val SEGMENT_NONE = 0
private const val SEGMENT_DELAY = 1
private const val SEGMENT_ATTACK = 2
private const val SEGMENT_HOLD = 3
private const val SEGMENT_DECAY = 4
private const val SEGMENT_SUSTAIN = 5
private const val SEGMENT_RELEASE = 6
private const val SEGMENT_DONE = 7

/** Effects (envelopes, LFOs, filter cutoff) update once per this many samples. */
private const val RENDER_EFFECTSAMPLEBLOCK = 64

/** tsf_render_short converts through a float buffer of this many samples. */
private const val RENDER_SHORTBUFFERBLOCK = 512

/** Grace release time for a quick voice off, to avoid a click. */
private const val FASTRELEASETIME = 0.01f

/** A voice's amplitude or modulation envelope as it runs (tsf_voice_envelope). */
private class VoiceEnvelope {
    var segment = SEGMENT_NONE
    var segmentIsExponential = false
    var isAmpEnv = false
    var midiVelocity = 0
    var level = 0f
    var slope = 0f
    var samplesUntilNextSegment = 0
    val parameters = Envelope()

    fun releaseSamples(outSampleRate: Float): Int = ((if (parameters.release <= 0) FASTRELEASETIME else parameters.release) * outSampleRate).toInt()

    /** Moves on from [activeSegment] to the next segment that has any length (the C switch's fall-through). */
    fun nextSegment(activeSegment: Int, outSampleRate: Float) {
        var segment = activeSegment
        if (segment == SEGMENT_NONE) {
            samplesUntilNextSegment = (parameters.delay * outSampleRate).toInt()
            if (samplesUntilNextSegment > 0) {
                this.segment = SEGMENT_DELAY
                segmentIsExponential = false
                level = 0.0f
                slope = 0.0f
                return
            }
            segment = SEGMENT_DELAY
        }
        if (segment == SEGMENT_DELAY) {
            samplesUntilNextSegment = (parameters.attack * outSampleRate).toInt()
            if (samplesUntilNextSegment > 0) {
                if (!isAmpEnv) {
                    // Mod env attack scales with velocity (velocity 1 is the full duration, 127 is 0.125 of it).
                    samplesUntilNextSegment = (parameters.attack * ((145 - midiVelocity) / 144.0f) * outSampleRate).toInt()
                }
                this.segment = SEGMENT_ATTACK
                segmentIsExponential = false
                level = 0.0f
                slope = 1.0f / samplesUntilNextSegment
                return
            }
            segment = SEGMENT_ATTACK
        }
        if (segment == SEGMENT_ATTACK) {
            samplesUntilNextSegment = (parameters.hold * outSampleRate).toInt()
            if (samplesUntilNextSegment > 0) {
                this.segment = SEGMENT_HOLD
                segmentIsExponential = false
                level = 1.0f
                slope = 0.0f
                return
            }
            segment = SEGMENT_HOLD
        }
        if (segment == SEGMENT_HOLD) {
            samplesUntilNextSegment = (parameters.decay * outSampleRate).toInt()
            if (samplesUntilNextSegment > 0) {
                this.segment = SEGMENT_DECAY
                level = 1.0f
                if (isAmpEnv) {
                    // Following LinuxSampler, as TinySoundFont does.
                    val mysterySlope = -9.226f / samplesUntilNextSegment
                    slope = exp(mysterySlope)
                    segmentIsExponential = true
                    if (parameters.sustain > 0.0f) {
                        // SF2-style decay: "decay" is the time to reach zero, not the sustain level.
                        samplesUntilNextSegment = (ln(parameters.sustain.toDouble()) / mysterySlope).toInt()
                    }
                } else {
                    slope = -1.0f / samplesUntilNextSegment
                    samplesUntilNextSegment = (parameters.decay * (1.0f - parameters.sustain) * outSampleRate).toInt()
                    segmentIsExponential = false
                }
                return
            }
            segment = SEGMENT_DECAY
        }
        if (segment == SEGMENT_DECAY) {
            this.segment = SEGMENT_SUSTAIN
            level = parameters.sustain
            slope = 0.0f
            samplesUntilNextSegment = 0x7FFFFFFF
            segmentIsExponential = false
            return
        }
        if (segment == SEGMENT_SUSTAIN) {
            this.segment = SEGMENT_RELEASE
            samplesUntilNextSegment = releaseSamples(outSampleRate)
            if (isAmpEnv) {
                val mysterySlope = -9.226f / samplesUntilNextSegment
                slope = exp(mysterySlope)
                segmentIsExponential = true
            } else {
                slope = -level / samplesUntilNextSegment
                segmentIsExponential = false
            }
            return
        }
        this.segment = SEGMENT_DONE
        segmentIsExponential = false
        level = 0.0f
        slope = 0.0f
        samplesUntilNextSegment = 0x7FFFFFF // sic: TinySoundFont's value
    }

    fun setup(newParameters: Envelope, midiNoteNumber: Int, midiVelocity: Int, isAmpEnv: Boolean, outSampleRate: Float) {
        parameters.copyFrom(newParameters)
        if (parameters.keynumToHold != 0f) {
            parameters.hold += parameters.keynumToHold * (60.0f - midiNoteNumber)
            parameters.hold = if (parameters.hold < -10000.0f) 0.0f else timecents2Secsf(parameters.hold)
        }
        if (parameters.keynumToDecay != 0f) {
            parameters.decay += parameters.keynumToDecay * (60.0f - midiNoteNumber)
            parameters.decay = if (parameters.decay < -10000.0f) 0.0f else timecents2Secsf(parameters.decay)
        }
        this.midiVelocity = midiVelocity
        this.isAmpEnv = isAmpEnv
        nextSegment(SEGMENT_NONE, outSampleRate)
    }

    fun process(numSamples: Int, outSampleRate: Float) {
        if (slope != 0f) {
            if (segmentIsExponential) level *= slope.pow(numSamples.toFloat()) else level += slope * numSamples
        }
        samplesUntilNextSegment -= numSamples
        if (samplesUntilNextSegment <= 0) nextSegment(segment, outSampleRate)
    }
}

/** A biquad low-pass (tsf_voice_lowpass), from earlevel.com's biquad code. */
private class Lowpass {
    var qInv = 0.0
    var a0 = 0.0
    var a1 = 0.0
    var b1 = 0.0
    var b2 = 0.0
    var z1 = 0.0
    var z2 = 0.0
    var active = false

    fun copyFrom(o: Lowpass) {
        qInv = o.qInv
        a0 = o.a0
        a1 = o.a1
        b1 = o.b1
        b2 = o.b2
        z1 = o.z1
        z2 = o.z2
        active = o.active
    }

    fun setup(fc: Float) {
        val k = tan(Math.PI * fc)
        val kk = k * k
        val norm = 1 / (1 + k * qInv + kk)
        a0 = kk * norm
        a1 = 2 * a0
        b1 = 2 * (kk - 1) * norm
        b2 = (1 - k * qInv + kk) * norm
    }

    fun process(input: Double): Float {
        val out = input * a0 + z1
        z1 = input * a1 + z2 - b1 * out
        z2 = input * a0 - b2 * out
        return out.toFloat()
    }
}

/** A triangle LFO (tsf_voice_lfo). */
private class Lfo {
    var samplesUntil = 0
    var level = 0f
    var delta = 0f

    fun setup(delay: Float, freqCents: Int, outSampleRate: Float) {
        samplesUntil = (delay * outSampleRate).toInt()
        delta = 4.0f * cents2Hertz(freqCents.toFloat()) / outSampleRate
        level = 0f
    }

    fun process(blockSamples: Int) {
        if (samplesUntil > blockSamples) {
            samplesUntil -= blockSamples
            return
        }
        level += delta * blockSamples
        if (level > 1.0f) {
            delta = -delta
            level = 2.0f - level
        } else if (level < -1.0f) {
            delta = -delta
            level = -2.0f - level
        }
    }
}

/** One sounding sample zone (tsf_voice). */
private class Voice {
    var playingPreset = -1
    var playingKey = 0
    var playingChannel = 0
    var heldSustain = false
    var region: Region? = null
    var pitchInputTimecents = 0.0
    var pitchOutputFactor = 0.0
    var sourceSamplePosition = 0.0
    var noteGainDB = 0f
    var panFactorLeft = 0f
    var panFactorRight = 0f
    var playIndex = 0
    var loopStart = 0L
    var loopEnd = 0L
    val ampenv = VoiceEnvelope()
    val modenv = VoiceEnvelope()
    val lowpass = Lowpass()
    val modlfo = Lfo()
    val viblfo = Lfo()
}

/** A MIDI channel's preset and controller state (tsf_channel). */
private class Channel {
    var presetIndex = 0
    var bank = 0
    var pitchWheel = 8192
    var midiPan = 8192
    var midiVolume = 16383
    var midiExpression = 16383
    var midiRPN = 0xFFFF
    var midiData = 0
    var sustain = false
    var panOffset = 0.0f
    var gainDB = 0.0f
    var pitchRange = 2.0f
    var tuning = 0.0f

    /** The pitch shift in semitones that the wheel, its range and the tuning add up to. */
    fun pitchShift(): Float = if (pitchWheel == 8192) tuning else (pitchWheel / 16383.0f * pitchRange * 2.0f) - pitchRange + tuning
}

/**
 * TinySoundFont: plays a [SoundFontBank]'s presets from note-on/off and MIDI channel events, and
 * renders the mix as mono samples. Not thread-safe: the caller serialises events and rendering.
 */
class TinySoundFont(private val bank: SoundFontBank) {
    private val voices = ArrayList<Voice>()
    private var maxVoiceNum = 0
    private var voicePlayIndex = 0
    private var outSampleRate = 44100.0f
    private var globalGainDB = 0f
    private var channels: ArrayList<Channel>? = null
    private var activeChannel = 0
    private val scratchLowpass = Lowpass()
    private val floatScratch = FloatArray(RENDER_SHORTBUFFERBLOCK)

    val presetCount: Int get() = bank.presets.size

    fun presetIndex(bank: Int, program: Int): Int = this.bank.presetIndex(bank, program)

    fun presetName(presetIndex: Int): String? = bank.presets.getOrNull(presetIndex)?.name

    /** Renders at [sampleRate] with [globalGainDb] on every note (tsf_set_output with TSF_MONO). */
    fun setOutput(sampleRate: Int, globalGainDb: Float = 0f) {
        outSampleRate = if (sampleRate >= 1) sampleRate.toFloat() else 44100.0f
        globalGainDB = globalGainDb
    }

    fun setVolume(globalVolume: Float) {
        globalGainDB = if (globalVolume == 1.0f) 0f else -gainToDecibels(1.0f / globalVolume)
    }

    /**
     * Pre-allocates [maxVoices] voices and never grows past them; when all are busy a new note
     * takes the voice furthest into its release (tsf_set_max_voices).
     */
    fun setMaxVoices(maxVoices: Int) {
        val newVoiceNum = if (voices.size > maxVoices) voices.size else maxVoices
        while (voices.size < newVoiceNum) voices.add(Voice())
        maxVoiceNum = newVoiceNum
    }

    fun noteOn(presetIndex: Int, key: Int, vel: Float) {
        val midiVelocity = (vel * 127).toInt().toShort().toInt()
        if (presetIndex < 0 || presetIndex >= bank.presets.size) return
        if (vel <= 0.0f) {
            noteOff(presetIndex, key)
            return
        }

        // Play all matching regions.
        val playIndex = voicePlayIndex++
        for (region in bank.presets[presetIndex].regions) {
            if (key < region.lokey || key > region.hikey || midiVelocity < region.lovel || midiVelocity > region.hivel) continue

            var voice: Voice? = null
            if (region.group != 0L) {
                for (v in voices) {
                    if (v.playingPreset == presetIndex && v.region!!.group == region.group) endQuick(v)
                    else if (v.playingPreset == -1 && voice == null) voice = v
                }
            } else {
                voice = voices.firstOrNull { it.playingPreset == -1 }
            }

            if (voice == null) {
                if (maxVoiceNum != 0) {
                    // Voices are capped: take the one furthest into its release.
                    var bestKillReleaseSamplePos = -999999999
                    for (v in voices) {
                        if (v.ampenv.segment == SEGMENT_RELEASE) {
                            val releaseSamplesDone = v.ampenv.releaseSamples(outSampleRate) - v.ampenv.samplesUntilNextSegment
                            if (releaseSamplesDone > bestKillReleaseSamplePos) {
                                bestKillReleaseSamplePos = releaseSamplesDone
                                voice = v
                            }
                        }
                    }
                    if (voice == null) continue
                    voice.playingPreset = -1
                } else {
                    // Grow by four voices, as TinySoundFont does.
                    repeat(4) { voices.add(Voice()) }
                    voice = voices[voices.size - 4]
                }
            }

            voice.region = region
            voice.playingPreset = presetIndex
            voice.playingKey = key
            voice.playIndex = playIndex
            voice.heldSustain = false
            voice.noteGainDB = globalGainDB - region.attenuation - gainToDecibels(1.0f / vel)

            val channels = channels
            if (channels != null) {
                setupChannelVoice(channels[activeChannel], voice)
            } else {
                calcPitchRatio(voice, 0f)
                // A 3 dB pan law, as TinySoundFont uses.
                voice.panFactorLeft = sqrt(0.5f - region.pan)
                voice.panFactorRight = sqrt(0.5f + region.pan)
            }

            voice.sourceSamplePosition = region.offset.toDouble()

            val doLoop = region.loopMode != LOOPMODE_NONE && region.loopStart < region.loopEnd
            voice.loopStart = if (doLoop) region.loopStart else 0
            voice.loopEnd = if (doLoop) region.loopEnd else 0

            voice.ampenv.setup(region.ampenv, key, midiVelocity, true, outSampleRate)
            voice.modenv.setup(region.modenv, key, midiVelocity, false, outSampleRate)

            val lowpassFc = if (region.initialFilterFc <= 13500) cents2Hertz(region.initialFilterFc.toFloat()) / outSampleRate else 1.0f
            val lowpassFilterQDB = region.initialFilterQ / 10.0f
            voice.lowpass.qInv = 1.0 / 10.0.pow(lowpassFilterQDB / 20.0)
            voice.lowpass.z1 = 0.0
            voice.lowpass.z2 = 0.0
            voice.lowpass.active = lowpassFc < 0.499f
            if (voice.lowpass.active) voice.lowpass.setup(lowpassFc)

            voice.modlfo.setup(region.delayModLFO, region.freqModLFO, outSampleRate)
            voice.viblfo.setup(region.delayVibLFO, region.freqVibLFO, outSampleRate)
        }
    }

    fun noteOff(presetIndex: Int, key: Int) {
        var first: Int = -1
        var last: Int = -1
        for (i in voices.indices) {
            val v = voices[i]
            if (v.playingPreset != presetIndex || v.playingKey != key || v.ampenv.segment >= SEGMENT_RELEASE) continue
            else if (first < 0 || v.playIndex < voices[first].playIndex) {
                first = i
                last = i
            } else if (v.playIndex == voices[first].playIndex) last = i
        }
        if (first < 0) return
        for (i in first..last) {
            val v = voices[i]
            if (i != first && i != last &&
                (v.playIndex != voices[first].playIndex || v.playingPreset != presetIndex || v.playingKey != key || v.ampenv.segment >= SEGMENT_RELEASE)
            ) continue
            end(v)
        }
    }

    fun noteOffAll() {
        for (v in voices) if (v.playingPreset != -1 && v.ampenv.segment < SEGMENT_RELEASE) end(v)
    }

    /** Ends every note quickly and forgets the channels (tsf_reset). */
    fun reset() {
        for (v in voices) {
            if (v.playingPreset != -1 && (v.ampenv.segment < SEGMENT_RELEASE || v.ampenv.parameters.release != 0f)) endQuick(v)
        }
        channels = null
    }

    val activeVoiceCount: Int get() = voices.count { it.playingPreset != -1 }

    /** Voices still sounding on [channel], releases included. Not in TinySoundFont; Pitch Perfect's addition. */
    fun channelActiveVoiceCount(channel: Int): Int = voices.count { it.playingPreset != -1 && it.playingChannel == channel }

    /** Renders [samples] mono samples into [buffer] from [offset], replacing or [mixing] into what's there. */
    fun renderFloat(buffer: FloatArray, offset: Int, samples: Int, mixing: Boolean = false) {
        if (!mixing) buffer.fill(0f, offset, offset + samples)
        for (v in voices) if (v.playingPreset != -1) renderVoice(v, buffer, offset, samples)
    }

    /** As [renderFloat], converted to 16-bit in 512-sample steps exactly as tsf_render_short does. */
    fun renderShort(buffer: ShortArray, offset: Int, samples: Int, mixing: Boolean = false) {
        var remaining = samples
        var at = offset
        while (remaining > 0) {
            val channelSamples = if (remaining > RENDER_SHORTBUFFERBLOCK) RENDER_SHORTBUFFERBLOCK else remaining
            renderFloat(floatScratch, 0, channelSamples, false)
            remaining -= channelSamples
            for (i in 0 until channelSamples) {
                val v = floatScratch[i]
                val converted = if (v < -1.00004566f) -32768 else if (v > 1.00001514f) 32767 else (v * 32767.5f).toInt()
                buffer[at] =
                    if (mixing) {
                        val vi = buffer[at] + converted
                        (if (vi < -32768) -32768 else if (vi > 32767) 32767 else vi).toShort()
                    } else {
                        converted.toShort()
                    }
                at++
            }
        }
    }

    // Channels.

    private fun channel(index: Int): Channel {
        val list = channels ?: ArrayList<Channel>().also { channels = it; activeChannel = 0 }
        while (list.size <= index) list.add(Channel())
        return list[index]
    }

    private fun setupChannelVoice(c: Channel, v: Voice) {
        val newpan = v.region!!.pan + c.panOffset
        v.playingChannel = activeChannel
        v.noteGainDB += c.gainDB
        calcPitchRatio(v, c.pitchShift())
        setPan(v, newpan)
    }

    private fun setPan(v: Voice, newpan: Float) {
        if (newpan <= -0.5f) {
            v.panFactorLeft = 1.0f
            v.panFactorRight = 0.0f
        } else if (newpan >= 0.5f) {
            v.panFactorLeft = 0.0f
            v.panFactorRight = 1.0f
        } else {
            v.panFactorLeft = sqrt(0.5f - newpan)
            v.panFactorRight = sqrt(0.5f + newpan)
        }
    }

    private fun applyPitch(channelIndex: Int, c: Channel) {
        val pitchShift = c.pitchShift()
        for (v in voices) if (v.playingPreset != -1 && v.playingChannel == channelIndex) calcPitchRatio(v, pitchShift)
    }

    fun channelSetPresetIndex(channel: Int, presetIndex: Int) {
        channel(channel).presetIndex = presetIndex and 0xFFFF
    }

    /** Selects GM [program] in the channel's bank, falling back to bank 0; false if the bank lacks it. */
    fun channelSetPresetNumber(channel: Int, program: Int, midiDrums: Boolean = false): Boolean {
        val c = channel(channel)
        var presetIndex: Int
        if (midiDrums) {
            presetIndex = presetIndex(128 or (c.bank and 0x7FFF), program)
            if (presetIndex == -1) presetIndex = presetIndex(128, program)
            if (presetIndex == -1) presetIndex = presetIndex(128, 0)
            if (presetIndex == -1) presetIndex = presetIndex(c.bank and 0x7FFF, program)
        } else {
            presetIndex = presetIndex(c.bank and 0x7FFF, program)
        }
        if (presetIndex == -1) presetIndex = presetIndex(0, program)
        if (presetIndex == -1) return false
        c.presetIndex = presetIndex
        return true
    }

    fun channelSetBank(channel: Int, bank: Int) {
        channel(channel).bank = bank and 0xFFFF
    }

    fun channelSetBankPreset(channel: Int, bank: Int, program: Int): Boolean {
        val c = channel(channel)
        val presetIndex = presetIndex(bank, program)
        if (presetIndex == -1) return false
        c.presetIndex = presetIndex
        c.bank = bank and 0xFFFF
        return true
    }

    fun channelSetPan(channel: Int, pan: Float) {
        val c = channel(channel)
        for (v in voices) if (v.playingPreset != -1 && v.playingChannel == channel) setPan(v, v.region!!.pan + pan - 0.5f)
        c.panOffset = pan - 0.5f
    }

    fun channelSetVolume(channel: Int, volume: Float) {
        val gainDB = gainToDecibels(volume)
        val c = channel(channel)
        if (gainDB == c.gainDB) return
        val gainDBChange = gainDB - c.gainDB
        for (v in voices) if (v.playingPreset != -1 && v.playingChannel == channel) v.noteGainDB += gainDBChange
        c.gainDB = gainDB
    }

    fun channelSetPitchWheel(channel: Int, pitchWheel: Int) {
        val c = channel(channel)
        if (c.pitchWheel == pitchWheel) return
        c.pitchWheel = pitchWheel and 0xFFFF
        applyPitch(channel, c)
    }

    fun channelSetPitchRange(channel: Int, pitchRange: Float) {
        val c = channel(channel)
        if (c.pitchRange == pitchRange) return
        c.pitchRange = pitchRange
        if (c.pitchWheel != 8192) applyPitch(channel, c)
    }

    /** Retunes the channel by [tuning] semitones, sounding notes included. */
    fun channelSetTuning(channel: Int, tuning: Float) {
        val c = channel(channel)
        if (c.tuning == tuning) return
        c.tuning = tuning
        applyPitch(channel, c)
    }

    fun channelSetSustain(channel: Int, sustain: Boolean) {
        val c = channel(channel)
        if (c.sustain == sustain) return
        c.sustain = sustain
        // Turning sustain on only changes how note-offs behave; turning it off ends the held notes.
        if (sustain) return
        for (v in voices) {
            if (v.playingPreset != -1 && v.playingChannel == channel && v.ampenv.segment < SEGMENT_RELEASE && v.heldSustain) end(v)
        }
    }

    fun channelNoteOn(channel: Int, key: Int, vel: Float) {
        val channels = channels ?: return
        if (channel >= channels.size) return
        activeChannel = channel
        if (vel == 0f) {
            channelNoteOff(channel, key)
            return
        }
        noteOn(channels[channel].presetIndex, key, vel)
    }

    fun channelNoteOff(channel: Int, key: Int) {
        var first = -1
        var last = -1
        for (i in voices.indices) {
            val v = voices[i]
            if (v.playingPreset == -1 || v.playingChannel != channel || v.playingKey != key || v.ampenv.segment >= SEGMENT_RELEASE || v.heldSustain) continue
            else if (first < 0 || v.playIndex < voices[first].playIndex) {
                first = i
                last = i
            } else if (v.playIndex == voices[first].playIndex) last = i
        }
        if (first < 0) return
        val sustain = channels!![channel].sustain
        for (i in first..last) {
            val v = voices[i]
            if (i != first && i != last &&
                (v.playIndex != voices[first].playIndex || v.playingPreset == -1 || v.playingChannel != channel || v.playingKey != key || v.ampenv.segment >= SEGMENT_RELEASE)
            ) continue
            // Sustain holds the note: mark it so it ends when sustain lifts.
            if (sustain) v.heldSustain = true else end(v)
        }
    }

    /** Releases every note on the channel, ignoring sustain. */
    fun channelNoteOffAll(channel: Int) {
        for (v in voices) if (v.playingPreset != -1 && v.playingChannel == channel && v.ampenv.segment < SEGMENT_RELEASE) end(v)
    }

    /** Silences every note on the channel within 10 ms. */
    fun channelSoundsOffAll(channel: Int) {
        for (v in voices) {
            if (v.playingPreset != -1 && v.playingChannel == channel && (v.ampenv.segment < SEGMENT_RELEASE || v.ampenv.parameters.release != 0f)) endQuick(v)
        }
    }

    /** Applies a MIDI control change (tsf_channel_midi_control). */
    fun channelMidiControl(channel: Int, controller: Int, controlValue: Int) {
        val c = channel(channel)
        when (controller) {
            7 -> { c.midiVolume = ((c.midiVolume and 0x7F) or (controlValue shl 7)) and 0xFFFF; setChannelVolumeFromMidi(channel, c) }
            39 -> { c.midiVolume = ((c.midiVolume and 0x3F80) or controlValue) and 0xFFFF; setChannelVolumeFromMidi(channel, c) }
            11 -> { c.midiExpression = ((c.midiExpression and 0x7F) or (controlValue shl 7)) and 0xFFFF; setChannelVolumeFromMidi(channel, c) }
            43 -> { c.midiExpression = ((c.midiExpression and 0x3F80) or controlValue) and 0xFFFF; setChannelVolumeFromMidi(channel, c) }
            10 -> { c.midiPan = ((c.midiPan and 0x7F) or (controlValue shl 7)) and 0xFFFF; channelSetPan(channel, c.midiPan / 16383.0f) }
            42 -> { c.midiPan = ((c.midiPan and 0x3F80) or controlValue) and 0xFFFF; channelSetPan(channel, c.midiPan / 16383.0f) }
            6 -> { c.midiData = ((c.midiData and 0x7F) or (controlValue shl 7)) and 0x3FFF; setChannelData(channel, c, controller, controlValue) }
            38 -> { c.midiData = ((c.midiData and 0x3F80) or controlValue) and 0x3FFF; setChannelData(channel, c, controller, controlValue) }
            0 -> c.bank = (0x8000 or controlValue) and 0xFFFF // bank select MSB alone acts like LSB
            32 -> c.bank = ((if (c.bank and 0x8000 != 0) ((c.bank and 0x7F) shl 7) else 0) or controlValue) and 0xFFFF
            101 -> c.midiRPN = (((if (c.midiRPN == 0xFFFF) 0 else c.midiRPN) and 0x7F) or (controlValue shl 7)) and 0xFFFF
            100 -> c.midiRPN = (((if (c.midiRPN == 0xFFFF) 0 else c.midiRPN) and 0x3F80) or controlValue) and 0xFFFF
            98, 99 -> c.midiRPN = 0xFFFF
            64 -> channelSetSustain(channel, controlValue >= 64)
            120 -> channelSoundsOffAll(channel)
            123 -> channelNoteOffAll(channel)
            121 -> {
                c.midiVolume = 16383
                c.midiExpression = 16383
                c.midiPan = 8192
                c.bank = 0
                c.midiRPN = 0xFFFF
                c.midiData = 0
                channelSetVolume(channel, 1.0f)
                channelSetPan(channel, 0.5f)
                channelSetPitchRange(channel, 2.0f)
                channelSetTuning(channel, 0f)
            }
        }
    }

    private fun setChannelVolumeFromMidi(channel: Int, c: Channel) {
        // Cubing gives a decent-sounding MIDI volume curve.
        channelSetVolume(channel, ((c.midiVolume / 16383.0f) * (c.midiExpression / 16383.0f)).pow(3.0f))
    }

    private fun setChannelData(channel: Int, c: Channel, controller: Int, controlValue: Int) {
        when {
            c.midiRPN == 0 -> channelSetPitchRange(channel, (c.midiData shr 7) + 0.01f * (c.midiData and 0x7F))
            c.midiRPN == 1 -> channelSetTuning(channel, c.tuning.toInt() + (c.midiData.toFloat() - 8192.0f) / 8192.0f) // fine tune
            c.midiRPN == 2 && controller == 6 -> channelSetTuning(channel, (controlValue.toFloat() - 64.0f) + (c.tuning - c.tuning.toInt())) // coarse tune
        }
    }

    // Voices.

    private fun end(v: Voice) {
        // With capped voices TinySoundFont assumes rendering runs on another thread, and repeats.
        repeat(if (maxVoiceNum != 0) 2 else 1) {
            v.ampenv.nextSegment(SEGMENT_SUSTAIN, outSampleRate)
            v.modenv.nextSegment(SEGMENT_SUSTAIN, outSampleRate)
            // A sustain-looped sample keeps playing but stops looping.
            if (v.region!!.loopMode == LOOPMODE_SUSTAIN) v.loopEnd = v.loopStart
        }
    }

    private fun endQuick(v: Voice) {
        repeat(if (maxVoiceNum != 0) 2 else 1) {
            v.ampenv.parameters.release = 0.0f
            v.ampenv.nextSegment(SEGMENT_SUSTAIN, outSampleRate)
            v.modenv.parameters.release = 0.0f
            v.modenv.nextSegment(SEGMENT_SUSTAIN, outSampleRate)
        }
    }

    private fun calcPitchRatio(v: Voice, pitchShift: Float) {
        val region = v.region!!
        val note = v.playingKey + region.transpose + region.tune / 100.0
        var adjustedPitch = region.pitchKeycenter + (note - region.pitchKeycenter) * (region.pitchKeytrack / 100.0)
        if (pitchShift != 0f) adjustedPitch += pitchShift
        v.pitchInputTimecents = adjustedPitch * 100.0
        v.pitchOutputFactor = region.sampleRate / (timecents2Secsd(region.pitchKeycenter * 100.0) * outSampleRate)
    }

    private fun renderVoice(v: Voice, output: FloatArray, offset: Int, samples: Int) {
        val region = v.region!!
        val updateModEnv = region.modEnvToPitch != 0 || region.modEnvToFilterFc != 0
        val updateModLFO = v.modlfo.delta != 0f && (region.modLfoToPitch != 0 || region.modLfoToFilterFc != 0 || region.modLfoToVolume != 0)
        val updateVibLFO = v.viblfo.delta != 0f && region.vibLfoToPitch != 0
        val isLooping = v.loopStart < v.loopEnd
        val tmpLoopStart = v.loopStart
        val tmpLoopEnd = v.loopEnd
        val tmpSampleEndDbl = region.end.toDouble()
        val tmpLoopEndDbl = tmpLoopEnd.toDouble() + 1.0
        val loopLength = (tmpLoopEnd - tmpLoopStart) + 1.0
        var tmpSourceSamplePosition = v.sourceSamplePosition
        val tmpLowpass = scratchLowpass
        tmpLowpass.copyFrom(v.lowpass)

        val dynamicLowpass = region.modLfoToFilterFc != 0 || region.modEnvToFilterFc != 0
        val tmpSampleRate = outSampleRate
        val tmpInitialFilterFc: Float
        val tmpModLfoToFilterFc: Float
        val tmpModEnvToFilterFc: Float

        val dynamicPitchRatio = region.modLfoToPitch != 0 || region.modEnvToPitch != 0 || region.vibLfoToPitch != 0
        var pitchRatio: Double
        val tmpModLfoToPitch: Float
        val tmpVibLfoToPitch: Float
        val tmpModEnvToPitch: Float

        val dynamicGain = region.modLfoToVolume != 0
        var noteGain = 0f
        val tmpModLfoToVolume: Float

        if (dynamicLowpass) {
            tmpInitialFilterFc = region.initialFilterFc.toFloat()
            tmpModLfoToFilterFc = region.modLfoToFilterFc.toFloat()
            tmpModEnvToFilterFc = region.modEnvToFilterFc.toFloat()
        } else {
            tmpInitialFilterFc = 0f
            tmpModLfoToFilterFc = 0f
            tmpModEnvToFilterFc = 0f
        }

        if (dynamicPitchRatio) {
            pitchRatio = 0.0
            tmpModLfoToPitch = region.modLfoToPitch.toFloat()
            tmpVibLfoToPitch = region.vibLfoToPitch.toFloat()
            tmpModEnvToPitch = region.modEnvToPitch.toFloat()
        } else {
            pitchRatio = timecents2Secsd(v.pitchInputTimecents) * v.pitchOutputFactor
            tmpModLfoToPitch = 0f
            tmpVibLfoToPitch = 0f
            tmpModEnvToPitch = 0f
        }

        if (dynamicGain) {
            tmpModLfoToVolume = region.modLfoToVolume.toFloat() * 0.1f
        } else {
            noteGain = decibelsToGain(v.noteGainDB)
            tmpModLfoToVolume = 0f
        }

        var numSamples = samples
        var out = offset
        while (numSamples != 0) {
            var blockSamples = if (numSamples > RENDER_EFFECTSAMPLEBLOCK) RENDER_EFFECTSAMPLEBLOCK else numSamples
            numSamples -= blockSamples

            if (dynamicLowpass) {
                val fres = tmpInitialFilterFc + v.modlfo.level * tmpModLfoToFilterFc + v.modenv.level * tmpModEnvToFilterFc
                val lowpassFc = if (fres <= 13500) cents2Hertz(fres) / tmpSampleRate else 1.0f
                tmpLowpass.active = lowpassFc < 0.499f
                if (tmpLowpass.active) tmpLowpass.setup(lowpassFc)
            }

            if (dynamicPitchRatio) {
                pitchRatio =
                    timecents2Secsd(
                        v.pitchInputTimecents + (v.modlfo.level * tmpModLfoToPitch + v.viblfo.level * tmpVibLfoToPitch + v.modenv.level * tmpModEnvToPitch),
                    ) * v.pitchOutputFactor
            }

            if (dynamicGain) noteGain = decibelsToGain(v.noteGainDB + (v.modlfo.level * tmpModLfoToVolume))

            val gainMono = noteGain * v.ampenv.level

            // Update the envelopes and LFOs.
            v.ampenv.process(blockSamples, tmpSampleRate)
            if (updateModEnv) v.modenv.process(blockSamples, tmpSampleRate)
            if (updateModLFO) v.modlfo.process(blockSamples)
            if (updateVibLFO) v.viblfo.process(blockSamples)

            while (blockSamples-- != 0 && tmpSourceSamplePosition < tmpSampleEndDbl) {
                val pos = tmpSourceSamplePosition.toLong()
                val nextPos = if (pos >= tmpLoopEnd && isLooping) tmpLoopStart else pos + 1

                // Linear interpolation.
                val alpha = (tmpSourceSamplePosition - pos).toFloat()
                var value = bank.sample(pos) * (1.0f - alpha) + bank.sample(nextPos) * alpha

                if (tmpLowpass.active) value = tmpLowpass.process(value.toDouble())

                output[out] += value * gainMono
                out++

                tmpSourceSamplePosition += pitchRatio
                if (tmpSourceSamplePosition >= tmpLoopEndDbl && isLooping) tmpSourceSamplePosition -= loopLength
            }

            if (tmpSourceSamplePosition >= tmpSampleEndDbl || v.ampenv.segment == SEGMENT_DONE) {
                v.playingPreset = -1
                return
            }
        }

        v.sourceSamplePosition = tmpSourceSamplePosition
        if (tmpLowpass.active || dynamicLowpass) v.lowpass.copyFrom(tmpLowpass)
    }
}
