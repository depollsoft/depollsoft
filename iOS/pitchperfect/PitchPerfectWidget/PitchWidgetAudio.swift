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
    static func suiteName(for bundleIdentifier: String?) -> String {
        if bundleIdentifier?.hasPrefix("depollsoft.pitchperfect.private") == true {
            return "group.depollsoft.pitchperfect.private"
        }
        return "group.depollsoft.pitchperfect"
    }

    static var defaults: UserDefaults? {
        UserDefaults(suiteName: suiteName(for: Bundle.main.bundleIdentifier))
    }
}

enum WidgetPitchState {
    private static let key = "activePitch"
    private static var defaults: UserDefaults? { WidgetSharedDefaults.defaults }

    static var activePitch: Int? {
        guard let defaults, defaults.object(forKey: key) != nil else { return nil }
        let value = defaults.integer(forKey: key)
        return value >= 0 ? value : nil
    }

    static func set(_ value: Int?) {
        guard let defaults else { return }
        defaults.set(value ?? -1, forKey: key)
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
                }
            },
            stopNotificationName as CFString,
            nil,
            .deliverImmediately
        )
    }
}

/// Runs in the widget process, so WidgetKit's own post-interaction reload
/// redraws the face without waiting on the app.
struct SelectWidgetRangeIntent: AppIntent {
    static let title: LocalizedStringResource = "Select Pitch Pipe Range"
    static let openAppWhenRun = false

    @Parameter(title: "Range")
    var rangeRawValue: String

    init() {}

    init(range: PitchRange) {
        rangeRawValue = range.rawValue
    }

    func perform() async throws -> some IntentResult {
        WidgetPlaybackBridge.requestStop()
        WidgetRangeState.set(rangeRawValue)
        WidgetPitchState.set(nil)
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        return .result()
    }
}

private enum WidgetToneError: Error {
    case playbackDidNotStart
}

@MainActor
private final class WidgetTonePlayer {
    static let shared = WidgetTonePlayer()

    private var player: AVAudioPlayer?
    private var frequency: Double?

    func play(frequency: Double) throws {
        if player?.isPlaying == true, self.frequency == frequency { return }

        let session = AVAudioSession.sharedInstance()
        player?.stop()
        player = nil
        self.frequency = nil

        do {
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true)
            let nextPlayer = try AVAudioPlayer(
                data: PlayWidgetPitchIntent.loopingTone(frequency: frequency)
            )
            nextPlayer.numberOfLoops = -1
            nextPlayer.prepareToPlay()
            guard nextPlayer.play() else {
                throw WidgetToneError.playbackDidNotStart
            }
            player = nextPlayer
            self.frequency = frequency
        } catch {
            try? session.setActive(false, options: .notifyOthersOnDeactivation)
            throw error
        }
    }

    func stop() {
        player?.stop()
        player = nil
        frequency = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

/// Runs in the containing app process when a widget cell is pressed. This
/// source is intentionally compiled into both the app and widget targets so
/// WidgetKit can discover the intent and the app can execute it.
struct PlayWidgetPitchIntent: SetValueIntent, AudioPlaybackIntent {
    static let title: LocalizedStringResource = "Sound Pitch"
    static let openAppWhenRun = false
    static let loopDuration = 4.0

    /// WidgetKit writes the toggle's new state here before performing.
    @Parameter(title: "Playing")
    var value: Bool

    @Parameter(title: "Pitch")
    var pitchIndex: Int

    @Parameter(title: "Frequency")
    var frequency: Double

    init() {}

    init(pitchIndex: Int, frequency: Double, playing: Bool) {
        self.pitchIndex = pitchIndex
        self.frequency = frequency
        value = playing
    }

    func perform() async throws -> some IntentResult {
        if value {
            do {
                try await MainActor.run {
                    try WidgetTonePlayer.shared.play(frequency: frequency)
                }
            } catch {
                WidgetPitchState.set(nil)
                WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
                throw error
            }
            WidgetPitchState.set(pitchIndex)
        } else {
            await MainActor.run {
                WidgetTonePlayer.shared.stop()
            }
            WidgetPitchState.set(nil)
        }
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        return .result()
    }

    static func loopingTone(frequency: Double) -> Data {
        let cycles = max(1, Int((frequency * loopDuration).rounded()))
        let seamlessFrequency = Double(cycles) / loopDuration
        return tone(frequency: seamlessFrequency, duration: loopDuration, fadeEdges: false)
    }

    static func tone(frequency: Double, duration: Double, fadeEdges: Bool = true) -> Data {
        let sampleRate = 44_100
        let frames = Int(Double(sampleRate) * duration)
        var pcm = Data(capacity: frames * 2)
        let fadeFrames = sampleRate / 40
        for frame in 0..<frames {
            let attack = min(1.0, Double(frame) / Double(fadeFrames))
            let release = min(1.0, Double(frames - frame) / Double(fadeFrames))
            let envelope = fadeEdges ? min(attack, release) : 1.0
            let sample = sin(2 * .pi * frequency * Double(frame) / Double(sampleRate))
            var value = Int16(sample * envelope * 9_000).littleEndian
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
