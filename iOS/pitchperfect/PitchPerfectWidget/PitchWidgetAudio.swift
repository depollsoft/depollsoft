import AppIntents
import AVFoundation
import Foundation
import WidgetKit

let widgetKind = "PitchPerfectPitchPipe"

enum PitchRange: String, AppEnum {
    case cToC
    case fToF

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Octave Range")
    static let caseDisplayRepresentations: [PitchRange: DisplayRepresentation] = [
        .cToC: "C to C",
        .fToF: "F to F",
    ]
}

enum WidgetSharedDefaults {
    static let productionGroup = "group.depollsoft.pitchperfect"
    static let privateGroup = "group.depollsoft.pitchperfect.private"

    /// The group a build variant is meant to use.
    static func suiteName(for bundleIdentifier: String?) -> String {
        if bundleIdentifier?.hasPrefix("depollsoft.pitchperfect.private") == true {
            return privateGroup
        }
        return productionGroup
    }

    /// The group this process can actually open. A preview build signed with
    /// the other variant's entitlement still lands on a container both the
    /// app and the widget share; `UserDefaults(suiteName:)` alone would fall
    /// back to a private store in each process and they would never meet.
    static let suiteName: String = {
        let preferred = suiteName(for: Bundle.main.bundleIdentifier)
        let candidates = [preferred, privateGroup, productionGroup]
        return candidates.first {
            FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: $0) != nil
        } ?? preferred
    }()

    static var defaults: UserDefaults? {
        UserDefaults(suiteName: suiteName)
    }

}

enum WidgetPitchState {
    private static let key = "activePitches"
    private static let legacyKey = "activePitch"
    private static var defaults: UserDefaults? { WidgetSharedDefaults.defaults }

    static var activePitches: Set<Int> {
        guard let defaults else { return [] }
        if let values = defaults.array(forKey: key) as? [Int] {
            return Set(values.filter { (0..<13).contains($0) })
        }
        guard defaults.object(forKey: legacyKey) != nil else { return [] }
        let value = defaults.integer(forKey: legacyKey)
        return (0..<13).contains(value) ? [value] : []
    }

    static func set(_ values: Set<Int>) {
        guard let defaults else { return }
        defaults.set(values.filter { (0..<13).contains($0) }.sorted(), forKey: key)
        defaults.removeObject(forKey: legacyKey)
        defaults.synchronize()
    }
}

enum WidgetRangeState {
    private static let key = "pitchRange"

    static var rawValue: String? {
        WidgetSharedDefaults.defaults?.string(forKey: key)
    }

    static func set(_ rawValue: String?) {
        guard let defaults = WidgetSharedDefaults.defaults else { return }
        if let rawValue {
            defaults.set(rawValue, forKey: key)
        } else {
            defaults.removeObject(forKey: key)
        }
        defaults.synchronize()
    }
}

/// The A4 the app's Settings chose, shared so the widget's pitches match.
enum WidgetTuningState {
    private static let key = "referencePitch"
    static let standard = 440.0

    static var referencePitch: Double {
        let value = WidgetSharedDefaults.defaults?.double(forKey: key) ?? 0
        return (400...480).contains(value) ? value : standard
    }

    static func set(_ value: Double) {
        guard let defaults = WidgetSharedDefaults.defaults else { return }
        defaults.set(value, forKey: key)
        defaults.synchronize()
    }
}

/// The sound the app's Settings chose (a DPNoteSound id), shared so the
/// widget's cells play it too.
enum WidgetSoundState {
    private static let key = "noteSound"
    static let pitchPipe = "pitchPipe"

    static var sound: String {
        WidgetSharedDefaults.defaults?.string(forKey: key) ?? pitchPipe
    }

    static func set(_ value: String) {
        guard let defaults = WidgetSharedDefaults.defaults else { return }
        defaults.set(value, forKey: key)
        defaults.synchronize()
    }
}

/// Plays a cell in a MIDI instrument through the app's own player. The widget
/// target doesn't link pitchperfectlib, so the app registers these at launch;
/// the intent runs in the app process, where they are set.
@MainActor
enum WidgetInstrumentHook {
    /// Starts `sound` at `frequency` (tuned, Hz); nil when `sound` isn't an
    /// instrument or can't play, and the cell then plays a tone. `ended` is
    /// called on the main queue if the note falls silent without being
    /// stopped (its instrument wouldn't load, or another note took its
    /// sampler), so the cell goes dark.
    static var start: ((_ sound: String, _ frequency: Double,
                        _ ended: @escaping @Sendable () -> Void) -> AnyObject?)?
    static var stop: ((AnyObject) -> Void)?
    /// Asks the app to give up the audio session once its own sounds stop:
    /// the widget's last tone stopped while the app was still sounding.
    static var releaseSessionWhenIdle: (() -> Void)?
    /// Whether the app is still sounding anything of its own (an instrument
    /// note or its release, a pitch pipe or wave note), so the widget leaves
    /// the shared audio session active.
    static var isSounding: (() -> Bool)?
    /// Tells the app a cell started sounding, for its analytics and review
    /// counts; the widget target has neither.
    static var pitchStarted: (() -> Void)?
    /// Tells the app a cell stopped sounding, for its review counts.
    static var pitchStopped: (() -> Void)?
}

/// The note a widget cell started, set once `start` returns, for its `ended`
/// callback to compare against (both run on the main actor).
private final class StartedNote: @unchecked Sendable {
    var note: AnyObject?
}

/// Whether the widget's own tone players are sounding, readable from any
/// thread (the app's MIDI player checks it before giving up the session).
enum WidgetToneActivity {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var sounding = false

    static var isSounding: Bool {
        lock.withLock { sounding }
    }

    static func set(_ value: Bool) {
        lock.withLock { sounding = value }
    }
}

/// Lets a widget-process intent stop a tone the app process owns.
enum WidgetPlaybackBridge {
    static let stopNotificationName = "depollsoft.pitchperfect.widget.stop"

    static func requestStop() {
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinNotifyCenter(),
            CFNotificationName(stopNotificationName as CFString),
            nil,
            nil,
            true
        )
    }

    /// Call once from the app process; the observer outlives the app delegate.
    static func installStopObserver() {
        CFNotificationCenterAddObserver(
            CFNotificationCenterGetDarwinNotifyCenter(),
            nil,
            { _, _, _, _, _ in
                Task { @MainActor in
                    WidgetTonePlayer.shared.stop()
                    WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
                }
            },
            stopNotificationName as CFString,
            nil,
            .deliverImmediately
        )
    }
}

private enum WidgetToneError: Error {
    case playbackDidNotStart
    case invalidPitch
}

@MainActor
final class WidgetTonePlayer {
    static let shared = WidgetTonePlayer()

    private var players: [Int: AVAudioPlayer] = [:]
    /// Gives the audio session back to other apps; tests count the calls.
    var deactivateSession: () -> Void = {
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    /// Cells sounding in a MIDI instrument, by the app player's token.
    private var instrumentNotes: [Int: AnyObject] = [:]

    /// A wave cell's loop fades in and out over 20 ms, as waves do in the app
    /// (docs/pitchperfect-note-sounds.md): a square starting at full level, or
    /// any wave cut mid-cycle, clicks. The pitch pipe's loop starts and stops
    /// as it always has.
    static let waveFade: TimeInterval = 0.02
    private var waveCells: Set<Int> = []
    /// Wave loops still fading out; the session stays active until they stop.
    private var fadingOut: [AVAudioPlayer] = []
    /// Runs work on the main actor after a delay; tests run it when they choose.
    var later: (TimeInterval, @escaping @MainActor () -> Void) -> Void = { delay, work in
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { MainActor.assumeIsolated(work) }
    }

    /// Read the actual players, not a persisted or optimistically rendered state.
    var activePitches: Set<Int> {
        Set(players.filter { $0.value.isPlaying }.keys).union(instrumentNotes.keys)
    }

    /// Each cell owns its loop. Toggling one never stops another sounding cell.
    func toggle(pitchIndex: Int, frequency: Double) throws {
        guard (0..<13).contains(pitchIndex), frequency.isFinite, (20...20_000).contains(frequency) else {
            throw WidgetToneError.invalidPitch
        }
        players = players.filter { $0.value.isPlaying }
        if let note = instrumentNotes.removeValue(forKey: pitchIndex) {
            // The app's MIDI player is still playing the release; it gives
            // up the session itself once it falls silent.
            WidgetInstrumentHook.stop?(note)
            return
        }
        if let playing = players.removeValue(forKey: pitchIndex) {
            end(playing, cell: pitchIndex)
            balanceVolume()
            deactivateIfSilent()
            return
        }
        let sound = WidgetSoundState.sound
        let started = StartedNote()
        let ended: @Sendable () -> Void = { [weak self] in
            MainActor.assumeIsolated {
                guard let self, let note = started.note, self.instrumentNotes[pitchIndex] === note else { return }
                self.instrumentEnded(cell: pitchIndex)
            }
        }
        if let note = WidgetInstrumentHook.start?(sound, frequency, ended) {
            started.note = note
            instrumentNotes[pitchIndex] = note
            return
        }

        do {
            if players.isEmpty {
                let session = AVAudioSession.sharedInstance()
                try session.setCategory(.playback, mode: .default)
                try session.setActive(true)
            }
            let nextPlayer = try AVAudioPlayer(
                data: PlayWidgetPitchIntent.loopingTone(frequency: frequency, sound: sound)
            )
            nextPlayer.numberOfLoops = -1
            players[pitchIndex] = nextPlayer
            balanceVolume()
            let fades = PlayWidgetPitchIntent.waveValue(sound, phase: 0, step: 0) != nil
            if fades {
                waveCells.insert(pitchIndex)
                nextPlayer.volume = 0
            }
            nextPlayer.prepareToPlay()
            guard nextPlayer.play() else {
                throw WidgetToneError.playbackDidNotStart
            }
            if fades {
                nextPlayer.setVolume(1, fadeDuration: Self.waveFade)
            }
            WidgetToneActivity.set(true)
        } catch {
            waveCells.remove(pitchIndex)
            players.removeValue(forKey: pitchIndex)?.stop()
            balanceVolume()
            deactivateIfSilent()
            throw error
        }
    }

    func stop() {
        players.forEach { end($0.value, cell: $0.key) }
        players.removeAll()
        instrumentNotes.values.forEach { WidgetInstrumentHook.stop?($0) }
        instrumentNotes.removeAll()
        WidgetPitchState.set([])
        deactivateIfSilent()
    }

    /// A cell's instrument note fell silent on its own: the cell goes dark.
    private func instrumentEnded(cell: Int) {
        instrumentNotes.removeValue(forKey: cell)
        WidgetPitchState.set(activePitches)
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        deactivateIfSilent()
    }

    /// Stops a cell's loop: a wave fades out first, then stops.
    private func end(_ player: AVAudioPlayer, cell: Int) {
        guard waveCells.remove(cell) != nil else {
            player.stop()
            return
        }
        player.setVolume(0, fadeDuration: Self.waveFade)
        fadingOut.append(player)
        later(Self.waveFade + 0.01) { [weak self] in
            player.stop()
            self?.fadingOut.removeAll { $0 === player }
            self?.deactivateIfSilent()
        }
    }

    private func balanceVolume() {
        // The app sums every sounding note at full scale, so chords from the
        // widget must not be quieter than the same chord in the app.
        players.values.forEach { $0.volume = 1 }
    }

    /// Gives up the session after the widget's last tone stops, unless the
    /// app is still sounding something (an instrument's release included).
    private func deactivateIfSilent() {
        WidgetToneActivity.set(!players.isEmpty || !fadingOut.isEmpty)
        guard players.isEmpty, fadingOut.isEmpty, instrumentNotes.isEmpty else { return }
        guard !(WidgetInstrumentHook.isSounding?() ?? false) else {
            // The app still sounds: the last of its sounds to stop gives the
            // session up instead.
            WidgetInstrumentHook.releaseSessionWhenIdle?()
            return
        }
        deactivateSession()
    }
}

/// Runs in the containing app process when a widget cell is pressed. This
/// source is intentionally compiled into both the app and widget targets so
/// WidgetKit can discover the intent and the app can execute it.
struct PlayWidgetPitchIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "Sound Pitch"
    static let openAppWhenRun = false
    static let loopDuration = 4.0

    @Parameter(title: "Pitch")
    var pitchIndex: Int

    @Parameter(title: "Frequency")
    var frequency: Double

    init() {}

    init(pitchIndex: Int, frequency: Double) {
        self.pitchIndex = pitchIndex
        self.frequency = frequency
    }

    /// Serialize playback and its published snapshot together so rapid taps
    /// cannot overwrite another note's state after leaving the main actor.
    func perform() async throws -> some IntentResult {
        defer { WidgetCenter.shared.reloadTimelines(ofKind: widgetKind) }
        try await MainActor.run {
            let player = WidgetTonePlayer.shared
            defer { WidgetPitchState.set(player.activePitches) }
            let wasSounding = player.activePitches.contains(pitchIndex)
            try player.toggle(pitchIndex: pitchIndex, frequency: frequency)
            if !wasSounding, player.activePitches.contains(pitchIndex) { WidgetInstrumentHook.pitchStarted?() }
            if wasSounding, !player.activePitches.contains(pitchIndex) { WidgetInstrumentHook.pitchStopped?() }
        }
        return .result()
    }

    static func loopingTone(frequency: Double, sound: String = WidgetSoundState.pitchPipe) -> Data {
        let cycles = max(1, Int((frequency * loopDuration).rounded()))
        let seamlessFrequency = Double(cycles) / loopDuration
        return tone(frequency: seamlessFrequency, duration: loopDuration, fadeEdges: false, sound: sound)
    }

    /// The pitch pipe voice shared by the iOS app (DPAudioSynthesizer) and
    /// Android (PitchAudioTrackGenerator): a sine driven three times past
    /// full scale and hard-clipped, which gives the reed-like edge.
    static func pitchPipeSample(frequency: Double, time: Double) -> Int16 {
        let driven = sin(2 * .pi * frequency * time) * 3
        let clipped = min(1.0, max(-1.0, driven))
        return Int16(clipped * Double(Int16.max))
    }

    /// A wave as DPWaveRender plays it in the app (docs/pitchperfect-note-sounds.md),
    /// at phase `p` for a step of `dt`, before its level and ramp; nil for a
    /// sound that isn't a wave.
    static func waveValue(_ sound: String, phase p: Double, step dt: Double) -> Double? {
        func polyBlep(_ t: Double) -> Double {
            if t < dt { let x = t / dt; return x + x - x * x - 1 }
            if t > 1 - dt { let x = (t - 1) / dt; return x * x + x + x + 1 }
            return 0
        }
        switch sound {
        case "sine": return sin(2 * .pi * p)
        case "triangle": return 1 - 4 * abs((p + 0.25).truncatingRemainder(dividingBy: 1) - 0.5)
        case "square": return (p < 0.5 ? 1 : -1) + polyBlep(p) - polyBlep((p + 0.5).truncatingRemainder(dividingBy: 1))
        case "sawtooth": return (2 * p - 1) - polyBlep(p)
        default: return nil
        }
    }

    /// The tone's samples: a wave for a wave sound (at -1 dBFS, as in the
    /// app), otherwise the pitch pipe voice. A whole number of cycles over
    /// the tone loops seamlessly either way.
    static func samples(frequency: Double, frames: Int, sampleRate: Int, sound: String) -> [Double] {
        let dt = frequency / Double(sampleRate)
        guard waveValue(sound, phase: 0, step: dt) != nil else {
            return (0..<frames).map {
                Double(pitchPipeSample(frequency: frequency, time: Double($0) / Double(sampleRate)))
            }
        }
        return (0..<frames).map { frame in
            let phase = (Double(frame) * dt).truncatingRemainder(dividingBy: 1)
            return 0.89 * waveValue(sound, phase: phase, step: dt)! * Double(Int16.max)
        }
    }

    static func tone(frequency: Double, duration: Double, fadeEdges: Bool = true, sound: String = WidgetSoundState.pitchPipe) -> Data {
        let sampleRate = 44_100
        let frames = Int(Double(sampleRate) * duration)
        var pcm = Data(capacity: frames * 2)
        let fadeFrames = sampleRate / 40
        let voice = samples(frequency: frequency, frames: frames, sampleRate: sampleRate, sound: sound)
        for frame in 0..<frames {
            let attack = min(1.0, Double(frame) / Double(fadeFrames))
            let release = min(1.0, Double(frames - frame) / Double(fadeFrames))
            let envelope = fadeEdges ? min(attack, release) : 1.0
            var value = Int16((voice[frame] * envelope).rounded()).littleEndian
            withUnsafeBytes(of: &value) { pcm.append(contentsOf: $0) }
        }

        var data = Data("RIFF".utf8)
        var riffSize = UInt32(36 + pcm.count).littleEndian
        withUnsafeBytes(of: &riffSize) { data.append(contentsOf: $0) }
        data.append(Data("WAVEfmt ".utf8))
        var formatSize = UInt32(16).littleEndian
        var audioFormat = UInt16(1).littleEndian
        var channels = UInt16(1).littleEndian
        var rate = UInt32(sampleRate).littleEndian
        var byteRate = UInt32(sampleRate * 2).littleEndian
        var blockAlign = UInt16(2).littleEndian
        var bits = UInt16(16).littleEndian
        withUnsafeBytes(of: &formatSize) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &audioFormat) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &channels) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &rate) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &byteRate) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &blockAlign) { data.append(contentsOf: $0) }
        withUnsafeBytes(of: &bits) { data.append(contentsOf: $0) }
        data.append(Data("data".utf8))
        var dataSize = UInt32(pcm.count).littleEndian
        withUnsafeBytes(of: &dataSize) { data.append(contentsOf: $0) }
        data.append(pcm)
        return data
    }
}
