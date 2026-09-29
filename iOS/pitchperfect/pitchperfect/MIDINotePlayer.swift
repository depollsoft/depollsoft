//
//  MIDINotePlayer.swift
//  pitchperfect
//
//  Plays notes in a General MIDI instrument (docs/pitchperfect-note-sounds.md):
//  an AVAudioEngine with a pool of AVAudioUnitSamplers loaded from the bundled
//  PitchPerfectInstruments.sf2. AUSampler bends every channel at once, so each
//  sounding note gets a sampler of its own, tuned for its key.
//

import AVFoundation
import Foundation

/// One note as the sampler plays it.
struct InstrumentNote: Equatable {
    var program: Int
    var key: Int
    /// The tuning's offset from A440 plus the key's correction, in cents.
    var pitchCents: Double
    var gainDb: Double
}

/// Per-key pitch corrections and gains for each instrument, measured on the
/// synth by scripts/pitchperfect/measure_instruments.py.
struct InstrumentTuning {
    struct Instrument: Decodable, Equatable {
        let gainDb: Double
        /// Cents to add at each key from firstKey; nil where the instrument is silent.
        let correctionCents: [Double?]
    }

    let firstKey: Int
    let lastKey: Int
    let instruments: [Int: Instrument]

    /// The GM program a silent key falls back to.
    static let fallbackProgram = 0
    /// AUSampler's overallGain can't go higher.
    static let maximumGainDb = 12.0

    init(data: Data, platform: String = "ios") throws {
        struct File: Decodable {
            let firstKey: Int
            let lastKey: Int
            let ios: [String: Instrument]
        }
        guard platform == "ios" else { throw CocoaError(.featureUnsupported) }
        let file = try JSONDecoder().decode(File.self, from: data)
        firstKey = file.firstKey
        lastKey = file.lastKey
        var instruments: [Int: Instrument] = [:]
        for (program, instrument) in file.ios {
            guard let number = Int(program), instrument.correctionCents.count == file.lastKey - file.firstKey + 1 else {
                throw CocoaError(.coderReadCorrupt)
            }
            instruments[number] = instrument
        }
        self.instruments = instruments
    }

    static let bundled: InstrumentTuning? = {
        guard let url = Bundle.main.url(forResource: "instrument-tuning", withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return nil }
        return try? InstrumentTuning(data: data)
    }()

    /// How `program` plays MIDI `key` with A4 at `referencePitch` Hz. A key
    /// outside the measured range uses the nearest end's correction; a key the
    /// instrument is silent at plays the piano. Nil for an unmeasured program.
    func note(program: Int, key: Int, referencePitch: Double) -> InstrumentNote? {
        guard let instrument = instruments[program] else { return nil }
        let index = min(max(key, firstKey), lastKey) - firstKey
        var program = program
        var chosen = instrument
        var correction = instrument.correctionCents[index]
        if correction == nil, let fallback = instruments[Self.fallbackProgram] {
            program = Self.fallbackProgram
            chosen = fallback
            correction = fallback.correctionCents[index]
        }
        let tuning = 1200 * log2(referencePitch / 440)
        return InstrumentNote(program: program,
                              key: min(max(key, 0), 127),
                              pitchCents: tuning + (correction ?? 0),
                              gainDb: min(chosen.gainDb, Self.maximumGainDb))
    }
}

/// A sampler the pool can hand a note: AVAudioUnitSampler, or a fake in tests.
protocol NoteSampler: AnyObject {
    func load(program: Int) throws
    func start(_ note: InstrumentNote)
    func stop(key: Int)
    /// Cuts whatever it is sounding, release tail included (MIDI all sound off).
    func silence()
}

/// Where samplers come from, and the output they play through.
protocol NoteSamplerHost: AnyObject {
    func makeSampler() -> NoteSampler?
    /// Starts (or restarts, after an interruption or route change) the output.
    func ensureRunning()
    /// Stops the output once nothing is sounding, so the app can suspend.
    func stopOutput()
    /// Called when the output stopped under the player (a route change, or an
    /// interruption ending); the player then calls ensureRunning in turn.
    var onNeedsRestart: (() -> Void)? { get set }
}

final class MIDINotePlayer: NSObject, DPNoteInstrumentPlayer {
    /// Tests set this off before anything plays: no engine, no session.
    nonisolated(unsafe) static var usesAudioHardware = true

    nonisolated(unsafe) static let shared: MIDINotePlayer = {
        let engine = usesAudioHardware ? SamplerEngine() : nil
        // The session stays active while a pitch pipe or wave note, or one of
        // the widget's tones, still sounds.
        engine?.othersSounding = { DPAudioSynthesizer.runningCount() > 0 || WidgetToneActivity.isSounding }
        return MIDINotePlayer(host: engine, tuning: .bundled)
    }()

    /// As many notes as the app can sound at once: one per pitch pipe cell.
    static let maximumVoices = 13
    /// How long a stopped sampler keeps playing the instrument's release.
    static let releaseTime: TimeInterval = 1.5
    /// Samplers kept loaded with the chosen instrument, so a quick second tap
    /// doesn't wait for a load either.
    static let warmVoices = 2

    /// A note the player gave out: DPNote holds it until it stops the note.
    final class Token: NSObject {
        let note: InstrumentNote
        fileprivate weak var voice: Voice?
        /// When the note was asked for; if it started later, its stop is put
        /// off by as long, so a timed note (the Settings preview) isn't cut short.
        fileprivate var requestedAt: TimeInterval = 0
        fileprivate var delayedStop = false
        init(note: InstrumentNote) { self.note = note }
    }

    fileprivate final class Voice {
        let sampler: NoteSampler
        var program: Int?
        /// The token sounding on it, if any.
        var token: Token?
        var startedAt: TimeInterval = 0
        var releasedAt: TimeInterval = -.infinity
        init(sampler: NoteSampler) { self.sampler = sampler }
    }

    private let host: NoteSamplerHost?
    private let tuning: InstrumentTuning?
    private let perform: (@escaping () -> Void) -> Void
    private let performAfter: (TimeInterval, @escaping () -> Void) -> Void
    private let now: () -> TimeInterval
    /// Only touched inside `perform`.
    private var voices: [Voice] = []
    private let soundingLock = NSLock()
    private var soundingNow = false

    /// Whether a note sounds or a release is still playing: until then the
    /// output keeps running, and the widget leaves the session active.
    var isSounding: Bool {
        soundingLock.withLock { soundingNow }
    }

    private func setSounding(_ value: Bool) {
        soundingLock.withLock { soundingNow = value }
    }

    /// `perform` runs the player's work in order, off the caller's thread (a
    /// serial queue unless a test passes something synchronous).
    /// `performAfter` runs work on the same queue after a delay.
    init(host: NoteSamplerHost?,
         tuning: InstrumentTuning?,
         perform: ((@escaping () -> Void) -> Void)? = nil,
         performAfter: ((TimeInterval, @escaping () -> Void) -> Void)? = nil,
         now: @escaping () -> TimeInterval = { ProcessInfo.processInfo.systemUptime }) {
        self.host = host
        self.tuning = tuning
        if let perform {
            // A test's own queue; delayed work runs straight away unless it
            // passes its own performAfter.
            self.perform = perform
            self.performAfter = performAfter ?? { _, work in perform(work) }
        } else {
            let queue = DispatchQueue(label: "depollsoft.pitchperfect.midi", qos: .userInteractive)
            self.perform = { queue.async(execute: $0) }
            self.performAfter = { delay, work in queue.asyncAfter(deadline: .now() + delay, execute: work) }
        }
        self.now = now
        super.init()
        host?.onNeedsRestart = { [weak self, weak host] in
            self?.perform { host?.ensureRunning() }
        }
    }

    /// How `sound` plays the note stored at `a440Frequency`, at the current tuning.
    func instrumentNote(sound: String, a440Frequency: Double) -> InstrumentNote? {
        let program = DPNoteSound.program(forSound: sound)
        guard program >= 0 else { return nil }
        return tuning?.note(program: Int(program),
                            key: Int(DPNoteSound.key(forA440Frequency: a440Frequency)),
                            referencePitch: DPNote.referencePitch)
    }

    // MARK: DPNoteInstrumentPlayer

    func startNote(_ note: DPNote, sound: String) -> Any? {
        start(sound: sound, a440Frequency: note.frequency)
    }

    func stopNote(_ token: Any) {
        guard let token = token as? Token else { return }
        perform { [self] in end(token) }
    }

    /// Starts a note; nil when there is no engine or the sound isn't an instrument.
    func start(sound: String, a440Frequency: Double) -> Token? {
        guard host != nil, let note = instrumentNote(sound: sound, a440Frequency: a440Frequency) else { return nil }
        let token = Token(note: note)
        token.requestedAt = now()
        perform { [self] in begin(token) }
        return token
    }

    /// Loads `sound`'s instrument into `warmVoices` idle samplers (making them
    /// if needed), so the next notes don't wait for it. One sampler loads per
    /// queued block, so a note asked for meanwhile (the Settings preview)
    /// starts after the first load rather than after all of them; samplers
    /// still playing a release are left alone.
    func prepare(sound: String) {
        let program = DPNoteSound.program(forSound: sound)
        guard program >= 0, host != nil else { return }
        perform { [self] in warm(Int(program), remaining: Self.warmVoices) }
    }

    // MARK: The pool (inside `perform` only)

    private func warm(_ program: Int, remaining: Int) {
        guard remaining > 0 else { return }
        let time = now()
        let ready = voices.filter { $0.token == nil && time - $0.releasedAt >= Self.releaseTime }
        let loaded = ready.filter { $0.program == program }.count
        guard loaded < Self.warmVoices else { return }
        let voice: Voice
        if let idle = ready.first(where: { $0.program != program }) {
            voice = idle
        } else if voices.count < Self.maximumVoices, let made = makeVoice() {
            voices.append(made)
            voice = made
        } else {
            return
        }
        load(program, into: voice)
        perform { [self] in warm(program, remaining: remaining - 1) }
    }

    private func begin(_ token: Token) {
        guard let voice = takeVoice(for: token.note.program) else { return }
        // A sampler still sounding a release (or a stolen note) is cut before
        // it's retuned, so its tail doesn't slide to the new note's tuning.
        if voice.token != nil || now() - voice.releasedAt < Self.releaseTime {
            if let stolen = voice.token { end(stolen, when: now()) }
            voice.sampler.silence()
            voice.releasedAt = -.infinity
        }
        if voice.program != token.note.program {
            load(token.note.program, into: voice)
            guard voice.program == token.note.program else { return }
        }
        setSounding(true)
        host?.ensureRunning()
        voice.sampler.start(token.note)
        voice.token = token
        voice.startedAt = now()
        token.voice = voice
    }

    private func end(_ token: Token) {
        guard let voice = token.voice, voice.token === token else { return }
        let lag = voice.startedAt - token.requestedAt
        if lag > 0.05, !token.delayedStop {
            token.delayedStop = true
            performAfter(lag) { [self] in end(token) }
            return
        }
        end(token, when: now())
    }

    private func end(_ token: Token, when time: TimeInterval) {
        guard let voice = token.voice, voice.token === token else { return }
        voice.sampler.stop(key: token.note.key)
        voice.token = nil
        voice.releasedAt = time
        performAfter(Self.releaseTime + 0.25) { [self] in stopOutputIfIdle() }
    }

    /// Stops the engine once no note sounds and every release is over;
    /// ensureRunning starts it again for the next note.
    private func stopOutputIfIdle() {
        let time = now()
        guard voices.allSatisfy({ $0.token == nil && time - $0.releasedAt >= Self.releaseTime }) else { return }
        setSounding(false)
        host?.stopOutput()
    }

    /// An idle sampler whose release is over, preferring one already loaded
    /// with `program`; else a new one; else the one done releasing soonest;
    /// else the note that has sounded longest gives up its sampler.
    private func takeVoice(for program: Int) -> Voice? {
        let time = now()
        let idle = voices.filter { $0.token == nil && time - $0.releasedAt >= Self.releaseTime }
        if let voice = idle.first(where: { $0.program == program }) ?? idle.first {
            return voice
        }
        if voices.count < Self.maximumVoices, let voice = makeVoice() {
            voices.append(voice)
            return voice
        }
        if let voice = voices.filter({ $0.token == nil }).min(by: { $0.releasedAt < $1.releasedAt }) {
            return voice
        }
        // Every sampler is sounding: the oldest note gives way (begin cuts it).
        return voices.min(by: { $0.startedAt < $1.startedAt })
    }

    private func makeVoice() -> Voice? {
        host?.makeSampler().map(Voice.init)
    }

    private func load(_ program: Int, into voice: Voice) {
        do {
            try voice.sampler.load(program: program)
            voice.program = program
        } catch {
            voice.program = nil
            DPAppLog.log("MIDI: couldn't load program \(program): \(error)")
        }
    }

    // MARK: Tests

    /// The pool's samplers and what each is doing, for tests.
    var voiceStates: [(program: Int?, sounding: Bool)] {
        voices.map { ($0.program, $0.token != nil) }
    }
}

/// The real output: one AVAudioEngine, its samplers mixed into the main mixer.
final class SamplerEngine: NoteSamplerHost {
    private let engine = AVAudioEngine()
    /// Samplers mix here. It joins the engine's output only when a note first
    /// plays: touching mainMixerNode builds the output, which launch and
    /// preloading must not do.
    private let samplerMix = AVAudioMixerNode()
    private(set) var outputConnected = false
    /// Whether anything else in the app is sounding (the pitch pipe and wave
    /// synths, the widget's tones), so stopping here leaves the session alone.
    var othersSounding: () -> Bool = { false }
    private let soundBank = Bundle.main.url(forResource: "PitchPerfectInstruments", withExtension: "sf2")
    private var observers: [NSObjectProtocol] = []

    init() {
        engine.attach(samplerMix)
        let center = NotificationCenter.default
        // A route change or configuration change stops the engine; an
        // interruption's end lets it run again. The next note restarts it too.
        observers.append(center.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: nil) { [weak self] _ in
            self?.restartSoon()
        })
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: nil) { [weak self] note in
            let type = (note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt)
                .flatMap(AVAudioSession.InterruptionType.init(rawValue:))
            if type == .ended { self?.restartSoon() }
        })
    }

    deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    var onNeedsRestart: (() -> Void)?

    private func restartSoon() {
        onNeedsRestart?()
    }

    func makeSampler() -> NoteSampler? {
        guard let soundBank else { return nil }
        let sampler = AVAudioUnitSampler()
        engine.attach(sampler)
        engine.connect(sampler, to: samplerMix, format: nil)
        return EngineSampler(sampler: sampler, soundBank: soundBank)
    }

    func ensureRunning() {
        // An app that never played an instrument has no output to run.
        guard !engine.isRunning, engine.attachedNodes.contains(where: { $0 is AVAudioUnitSampler }) else { return }
        if !outputConnected {
            engine.connect(samplerMix, to: engine.mainMixerNode, format: nil)
            outputConnected = true
        }
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback)
        try? session.setActive(true)
        engine.prepare()
        do {
            try engine.start()
        } catch {
            DPAppLog.log("MIDI: engine didn't start: \(error)")
        }
    }

    func stopOutput() {
        guard engine.isRunning else { return }
        engine.stop()
        // Give the audio back to other apps, unless this app still sounds.
        guard !othersSounding() else { return }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

private final class EngineSampler: NoteSampler {
    let sampler: AVAudioUnitSampler
    let soundBank: URL

    init(sampler: AVAudioUnitSampler, soundBank: URL) {
        self.sampler = sampler
        self.soundBank = soundBank
    }

    func load(program: Int) throws {
        try sampler.loadSoundBankInstrument(at: soundBank, program: UInt8(program),
                                            bankMSB: UInt8(kAUSampler_DefaultMelodicBankMSB),
                                            bankLSB: UInt8(kAUSampler_DefaultBankLSB))
    }

    func start(_ note: InstrumentNote) {
        sampler.globalTuning = Float(note.pitchCents)
        sampler.overallGain = Float(note.gainDb)
        sampler.startNote(UInt8(note.key), withVelocity: 100, onChannel: 0)
    }

    func stop(key: Int) {
        sampler.stopNote(UInt8(key), onChannel: 0)
    }

    func silence() {
        sampler.sendController(120, withValue: 0, onChannel: 0) // all sound off
        sampler.sendController(123, withValue: 0, onChannel: 0) // all notes off
    }
}

extension MIDINotePlayer {
    /// Makes `player` the one notes and the widget's cells play instruments through.
    @MainActor
    static func install(_ player: MIDINotePlayer = .shared) {
        DPNote.instrumentPlayer = player
        WidgetInstrumentHook.start = { [weak player] sound, frequency in
            // The widget passes the tuned frequency; the key comes from A440.
            player?.start(sound: sound, a440Frequency: frequency * 440 / DPNote.referencePitch)
        }
        WidgetInstrumentHook.stop = { [weak player] token in
            player?.stopNote(token)
        }
        WidgetInstrumentHook.isSounding = { [weak player] in
            (player?.isSounding ?? false) || DPAudioSynthesizer.runningCount() > 0
        }
    }
}
