#!/usr/bin/env python3
"""Measures Pitch Perfect's MIDI instruments on both platforms' synths.

Every instrument's notes are played through the synth each app uses: iOS's
AVAudioUnitSampler with shared/pitchperfect/PitchPerfectInstruments.sf2, and
Android's built-in Sonivox EAS synth (built here from its source, with the
wavetable Android ships). For each key the script records how far the synth
lands from the true pitch, and whether the instrument sounds there at all.
It also records how loud each instrument plays. The apps undo the pitch error
with pitch bend or tuning, and they skip silent keys.

Writes shared/pitchperfect/instrument-tuning.json. Needs macOS (swiftc),
cmake, git and numpy.

    python3 scripts/pitchperfect/measure_instruments.py
"""

import argparse
import json
import os
import struct
import subprocess
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SOUNDFONT = os.path.join(REPO, "shared", "pitchperfect", "PitchPerfectInstruments.sf2")
OUTPUT = os.path.join(REPO, "shared", "pitchperfect", "instrument-tuning.json")
SONIVOX_REPO = "https://github.com/pedrolcl/sonivox"
SONIVOX_COMMIT = "e3213f76436f4664e2a149c4a451e0df72f4e13e"

PROGRAMS = [0, 4, 6, 11, 19, 20, 21, 22, 24, 46, 48, 52, 56, 71, 73]
KEYS = range(24, 108)  # C1 to B7, Pitch Perfect's notes
RATE = 44100
VELOCITY = 100
TARGET_PEAK_DB = -4.0


def key_frequency(key):
    return 440.0 * 2 ** ((key - 69) / 12)


def midi_file(program, key, hold=3.0):
    """What the Android app plays: program, note on, held, note off."""
    def vlq(value):
        out = [value & 0x7F]
        value >>= 7
        while value:
            out.insert(0, (value & 0x7F) | 0x80)
            value >>= 7
        return bytes(out)
    events = (vlq(0) + bytes([0xC0, program]) + vlq(0) + bytes([0x90, key, VELOCITY])
              + vlq(int(hold * 960)) + bytes([0x80, key, 0]) + vlq(0) + bytes([0xFF, 0x2F, 0]))
    return (b"MThd" + struct.pack(">IHHH", 6, 0, 1, 480)
            + b"MTrk" + struct.pack(">I", len(events)) + events)


def build_sonivox(cache):
    source = os.path.join(cache, "sonivox")
    binary = os.path.join(source, "build", "example", "sonivoxrender")
    if os.path.exists(binary):
        return binary
    if not os.path.exists(source):
        subprocess.run(["git", "clone", "-q", SONIVOX_REPO, source], check=True)
    subprocess.run(["git", "-C", source, "checkout", "-q", SONIVOX_COMMIT], check=True)
    subprocess.run(["cmake", "-S", source, "-B", os.path.join(source, "build"),
                    "-DCMAKE_BUILD_TYPE=Release", "-DBUILD_TESTING=OFF"],
                   check=True, stdout=subprocess.DEVNULL)
    subprocess.run(["cmake", "--build", os.path.join(source, "build"), "-j8"],
                   check=True, stdout=subprocess.DEVNULL)
    return binary


def render_sonivox(binary, program, key, workdir):
    path = os.path.join(workdir, "note.mid")
    with open(path, "wb") as f:
        f.write(midi_file(program, key))
    pcm = subprocess.run([binary, "-r", "0", "-c", "0", path],
                         check=True, capture_output=True).stdout
    return np.frombuffer(pcm, dtype="<i2").astype(np.float64).reshape(-1, 2).mean(axis=1) / 32768


def render_sampler(cache, workdir):
    binary = os.path.join(cache, "sampler-render")
    subprocess.run(["swiftc", "-O", "-suppress-warnings", os.path.join(HERE, "SamplerRender.swift"),
                    "-o", binary], check=True)
    pairs = [f"{p}:{k}" for p in PROGRAMS for k in KEYS]
    subprocess.run([binary, SOUNDFONT, workdir] + pairs, check=True)
    return lambda program, key: np.fromfile(os.path.join(workdir, f"{program}_{key}.f32"), dtype="<f4").astype(np.float64)


def mean_frequency(signal, expected):
    """Average pitch: the fundamental's phase advance over the stretch."""
    spectrum = np.fft.fft(signal)
    bins = np.fft.fftfreq(len(signal), 1 / RATE)
    band = (bins > expected * 2 ** (-0.5 / 12)) & (bins < expected * 2 ** (0.5 / 12))
    phase = np.unwrap(np.angle(np.fft.ifft(spectrum * band)))
    margin = len(signal) // 10
    return (phase[-margin] - phase[margin]) / (2 * np.pi * (len(signal) - 2 * margin) / RATE)


def measure(signal, key):
    """(cents off, peak) or None when the note is silent."""
    peak = float(np.max(np.abs(signal))) if len(signal) else 0.0
    if peak < 1e-3:
        return None
    body = signal[int(RATE * 0.2):int(RATE * 2.9)]
    loud = np.flatnonzero(np.abs(body) > np.max(np.abs(body)) * 0.1)
    if len(loud) < RATE * 0.2:
        body = signal[:int(RATE * 2.9)]
        loud = np.flatnonzero(np.abs(body) > np.max(np.abs(body)) * 0.1)
    body = body[loud[0]:loud[-1] + 1]
    expected = key_frequency(key)
    cents = 1200 * np.log2(mean_frequency(body, expected) / expected)
    return float(cents), peak


def summarize(render, label):
    result = {}
    for program in PROGRAMS:
        readings = {key: measure(render(program, key), key) for key in KEYS}
        corrections = [None if readings[k] is None else round(-readings[k][0], 1) for k in KEYS]
        # Loudness from the middle of the range, where notes mostly get played.
        middle = [r[1] for k, r in readings.items() if r and 48 <= k <= 84]
        loudest = max(middle or [r[1] for r in readings.values() if r])
        gain = round(TARGET_PEAK_DB - 20 * np.log10(loudest), 1)
        silent = [k for k in KEYS if readings[k] is None]
        worst = max(abs(c) for c in corrections if c is not None)
        print(f"{label} program {program:3}: gain {gain:+.1f} dB, worst {worst:.1f} cents"
              + (f", silent at {silent}" if silent else ""))
        result[str(program)] = {"gainDb": gain, "correctionCents": corrections}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--cache", default=os.path.join(tempfile.gettempdir(), "pitchperfect-instruments"))
    args = parser.parse_args()
    os.makedirs(args.cache, exist_ok=True)
    sonivox = build_sonivox(args.cache)
    with tempfile.TemporaryDirectory() as workdir:
        sampler = render_sampler(args.cache, workdir)
        ios = summarize(sampler, "ios")
        android = summarize(lambda p, k: render_sonivox(sonivox, p, k, workdir), "android")
    data = {
        "about": "Per-key pitch corrections (cents to add; null where the instrument is silent) "
                 "and gains for Pitch Perfect's MIDI instruments. Generated by "
                 "scripts/pitchperfect/measure_instruments.py; see docs/pitchperfect-note-sounds.md.",
        "firstKey": KEYS[0],
        "lastKey": KEYS[-1],
        "ios": ios,
        "android": android,
    }
    with open(OUTPUT, "w") as f:
        json.dump(data, f, indent=1)
        f.write("\n")
    print(f"wrote {OUTPUT}")


if __name__ == "__main__":
    main()
