# Pitch Perfect note sounds

Settings has a **Sound** choice for the voice every note plays in. The
original voice stays the default. Waves are generated in code, and
instruments are played through each platform's MIDI synth. Both platforms
offer the same sounds and store and sync the choice the same way.

## The sounds

Ids are stored and synced. Labels are shown in the picker, grouped under
the section headings below, in this order; the default has no heading and
comes first. Instruments that sustain on their own come before everything
else, since they make the most useful reference tones.

| id | label | how it sounds |
| --- | --- | --- |
| `pitchPipe` | Pitch Perfect (Loud) | the original voice, unchanged |
| **Sustained** | | General MIDI program |
| `organ` | Organ | 19 |
| `reedOrgan` | Reed Organ | 20 |
| `accordion` | Accordion | 21 |
| `harmonica` | Harmonica | 22 |
| `strings` | Strings | 48 |
| `choir` | Choir | 52 |
| `trumpet` | Trumpet | 56 |
| `clarinet` | Clarinet | 71 |
| `flute` | Flute | 73 |
| **Waves** | | |
| `sine` | Sine | oscillator |
| `triangle` | Triangle | oscillator |
| `square` | Square | oscillator |
| `sawtooth` | Sawtooth | oscillator |
| **Plucked & Struck** | | General MIDI program |
| `piano` | Piano | 0 |
| `electricPiano` | Electric Piano | 4 |
| `harpsichord` | Harpsichord | 6 |
| `vibraphone` | Vibraphone | 11 |
| `guitar` | Guitar | 24 |
| `harp` | Harp | 46 |

Every instrument sounds for as long as its note is held. Plucked and struck
instruments would normally fade out on their own. Their presets in the
bundled soundfont are changed to sustain at full level while the key is
down (`HELD_PROGRAMS` in `subset_soundfont.py`). Every one of their samples
loops, so a held piano note settles at its sample loop's level, about 0 to
10 dB below the attack, and still plays its release when the note stops.
Measured in both FluidSynth and AUSampler. The synth plays everything else
as the sound bank defines it; nothing is pre-rendered or looped by the app.

## Storage and sync

- Local preference: `depollsoft.pitchperfect.NoteSound` on both platforms
  (the iOS widget's app-group key is `noteSound`). Default `pitchPipe`.
- Account document `users/{uid}` field `noteSound` (string), written when the
  user changes it and applied when a snapshot arrives, like `referencePitch`.
- An id this version doesn't know (a newer version's sound, or garbage) reads
  as `pitchPipe`. It is not written back.
- The iOS widget reads the choice from the shared app group, like the tuning.
- A note that is already sounding keeps its voice; the next note played uses
  the new one. This matches how a tuning change applies.
- Wear OS keeps the original voice. It has no synced settings.

## Pitch Perfect (Loud)

Unchanged: a sine driven three times past full scale and hard-clipped, at
the existing sample rates (Android 8 kHz, iOS 44.1 kHz). Keep this path
byte-for-byte as it is today.

## Waves

Generated live at 44 100 Hz, in the same streaming path as the pitch pipe
(Android `PitchAudioTrackGenerator` / iOS `DPAudioSynthesizer`). A wave keeps
a phase `p` in [0, 1) and advances it by `dt = tunedFrequency / 44100` each
sample.

```
polyBlep(t, dt) = t < dt       ? (x = t / dt;       2x - x² - 1)
                : t > 1 - dt   ? (x = (t - 1) / dt; x² + 2x + 1)
                : 0

sine     = sin(2πp)
triangle = 1 - 4 · |((p + 0.25) mod 1) - 0.5|
square   = (p < 0.5 ? 1 : -1) + polyBlep(p, dt) - polyBlep((p + 0.5) mod 1, dt)
sawtooth = (2p - 1) - polyBlep(p, dt)

sample   = 0.89 · value · fadeIn(n) · fadeOut(r)
fadeIn(n)  = n < 882 ? ½ · (1 - cos(π · n / 882)) : 1   // n = samples since the note started
fadeOut(r) = r < 882 ? ½ · (1 + cos(π · r / 882)) : 0   // r = samples since it was released
```

PolyBLEP keeps the square and sawtooth from aliasing on high notes. 0.89 is
-1 dBFS. Both fades last 20 ms (882 samples) on a raised cosine, which is flat
at both ends, so a pure tone starts and stops without a tick. The 5 ms linear
ramp this replaced had a corner at each end, and users heard a click at the
start.

How each platform plays the fades:

- **Android:** the fade-in is in the samples (`WaveSource`). A released note
  fades through `setStereoVolume` in 2 ms raised-cosine steps, because the
  wave already queued (a second at the start, then about 0.2 s) would
  otherwise play at full level. Its track is then paused and flushed.
  How a wave track runs:
  - **Priming:** it writes its whole buffer before `play()`, because a
    streaming track doesn't start until it holds its start threshold (the
    whole buffer). Filling from the watcher thread after `play()` raced the
    start: the first write landed about 40 ms late.
  - **Refills:** after that it writes only what the buffer has room for, so
    its filler never blocks inside `write()`. A blocked write could land old
    audio after the flush.
  - **Reuse:** a pooled wave track is always flushed, so a note never starts
    by replaying the tail of the last one.
  - The pitch pipe's tracks behave exactly as before.
- **iOS:** both fades are applied to the samples. A released wave keeps its
  audio unit running until the fade-out has played.

## Instruments: MIDI

### The note

```
key          = round(69 + 12 · log2(storedFrequency / 440))
               // storedFrequency is the note's A440 frequency (keyNumber is only set for octave 0)
program      = the sound's GM program
correction   = instrument-tuning.json[platform][program].correctionCents[key - 24]
               // keys outside 24…107 use the nearest end's value
if correction is null:   // this synth's instrument is silent at this key
    program = the sound's fallback, if it sounds here, else 0 (piano)
    correction = that program's value for the key
    // fallbacks: harmonica → reed organ (20); accordion → reed organ (20);
    // every other instrument → piano
tuningCents  = 1200 · log2(referencePitch / 440)
pitchCents   = tuningCents + correction
velocity     = 100
```

`shared/pitchperfect/instrument-tuning.json` comes from
`scripts/pitchperfect/measure_instruments.py`. The script plays every key of
every instrument through the synth each platform uses and measures how far it
lands from true pitch. Sound banks are tuned by ear, and some zones are out
by 20–35 cents, so the correction is what keeps a MIDI instrument usable as a
pitch reference. The file also holds `gainDb` for each instrument: the gain
that brings its loudest note in the middle of the range to -4 dBFS. Rerun the
script if the sound bank or the list of instruments changes.

Android's built-in bank is silent in a few places. Harmonica only sounds
from C4 to F♯5, and reed organ only up to F6. Accordion is silent below C3
and above C7, and vibraphone and flute below C2. The free reeds (harmonica,
accordion) fall back to the reed organ, which is closest to a pitch pipe's
reed; any other silent key plays the piano.

Harmonica and reed organ are the instruments closest to a real pitch pipe,
which is itself a free reed.

### Android: a generated MIDI file

Android's built-in General MIDI synth (Sonivox EAS) is reached through
`MediaPlayer`, which plays standard MIDI files. Each note builds a small
type-0 SMF in memory and plays it through a `MediaDataSource`, with no temp
files.

```
MThd  format 0, 1 track, 480 ticks per quarter (120 bpm by default: 960 ticks a second)
MTrk  delta 0  B0 65 00          RPN MSB 0 (pitch bend sensitivity)
      delta 0  B0 64 00          RPN LSB 0
      delta 0  B0 06 03          ±3 semitones
      delta 0  B0 26 00
      delta 0  C0 <program>
      delta 0  E0 <bend LSB> <bend MSB>   bend = clamp(round(8192 + pitchCents / 300 · 8192), 0, 16383)
      delta 0  90 <key> 64       note on, velocity 100
      delta 30 min  80 <key> 00  note off
      delta 0  FF 2F 00
```

- `prepare()` took 3–40 ms on an API 36 emulator and `start()` under 1 ms;
  four notes can play at once. Prepare off the main thread, then start. A
  note always starts, even if it was released while preparing, and fades
  out no sooner than 250 ms after it began, so a quick tap is still heard.
  The first instrument note after launch took 72–105 ms to sound on the
  emulator, while its player thread started; later ones took 7–30 ms.
- Gain: attach a `LoudnessEnhancer` to the player's audio session with
  `gainDb · 100` mB when `gainDb` > 0.
- Stop: ramp `setVolume` from 1 to 0 over about 30 ms, then `stop()` and
  `release()`, so a held note doesn't end in a click.
- A `MediaPlayer` lives only while its note sounds. If a note is still held
  when its 30 minutes run out, play it again.

### iOS: AVAudioUnitSampler

iOS has no built-in General MIDI bank, so the app ships
`shared/pitchperfect/PitchPerfectInstruments.sf2`: the 15 presets taken
unchanged from [GeneralUser GS](https://www.schristiancollins.com/generaluser)
v2.0.3 by S. Christian Collins, 8.8 MB. See
`shared/pitchperfect/GeneralUser-GS-LICENSE.txt`: it is free for use in
software, and its author notes he cannot vouch for the origin of every
sample. `scripts/pitchperfect/subset_soundfont.py` builds it. The subset
renders sample-for-sample the same as the full bank in FluidSynth.

- One `AVAudioEngine` holds a pool of `AVAudioUnitSampler` nodes, each loaded
  with the chosen program via `loadSoundBankInstrument(at:program:bankMSB:
  kAUSampler_DefaultMelodicBankMSB, bankLSB: 0)`.
- AUSampler applies pitch bend to every channel at once, so per-key
  correction needs one sampler per sounding note. A note takes an idle
  sampler, sets `globalTuning = pitchCents` and `overallGain = gainDb`, then
  calls `startNote(key, withVelocity: 100, onChannel: 0)`. Stopping calls
  `stopNote`, which lets the sampler play the instrument's release. The
  sampler goes back to the pool when the release is over (1.5 s). If all
  13 are busy, the note that has sounded longest stops to make room.
  `overallGain` tops out at +12 dB, so `gainDb` is capped there.
- A sampler that is still playing a release, or whose note is being stolen,
  is silenced (MIDI all sound off) before it's retuned for another note, so
  the old tail doesn't slide to the new tuning.
- Hold enough samplers for every note the app can sound at once: 13, one for
  each pitch pipe cell. Create them lazily. When an instrument is chosen,
  and at launch, load it into two idle samplers (not ones sounding or
  releasing), one per queued block, off the main thread: the first note and
  a quick second one don't wait, and a note asked for meanwhile (the Settings
  preview) starts after the first load. A note that started late because of
  a load is stopped as late, so a timed note keeps its length.
- Once no note sounds and every release has played, the engine stops, so
  the app can suspend in the background. It also gives up the audio session
  (`notifyOthersOnDeactivation`) unless a pitch pipe or wave note or a widget
  tone is still sounding. The next note starts the engine again.
- Samplers mix into a submixer that joins the engine's output only when an
  instrument first plays, so launch and preloading don't build the output.
  Tests never start it: wiring AVAudioEngine output has deadlocked hosted
  test runs before (Tag Master's `TMBalanceAudioPlayer.usesAudioHardware`
  works around it), so tests inject a fake note player.

### iOS widget

The widget's intent runs in the app process. Its pitch pipe and wave tones
stay whole-cycle WAV loops played by `AVAudioPlayer`, which are seamless
because the waves are periodic. For an instrument it uses the app's MIDI
player instead, so a held widget note sounds exactly like one played in the
app. The widget target doesn't link pitchperfectlib, so the app registers
the player at launch through a hook in `PitchWidgetAudio.swift`. Stopping an
instrument cell leaves the audio session to the app's player, which gives it
up after the release. The widget gives the session up itself only when its
last tone stops and the app is sounding nothing else.

## Settings UI

The Sound row sits under Tuning and follows that row's pattern on each
platform. Android shows an outlined drop-down field that opens a dialog
listing the sounds, with the section headings. iOS shows a picker row that
pushes a list with sections. Choosing a sound plays a short preview: C4 at the
current tuning for 1 s, or less if the user leaves the screen. The preview
lets the user hear the sound without leaving Settings.
