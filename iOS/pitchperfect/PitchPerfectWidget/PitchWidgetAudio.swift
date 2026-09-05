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

    /// Read the actual players, not a persisted or optimistically rendered state.
    var activePitches: Set<Int> {
        Set(players.filter { $0.value.isPlaying }.keys)
    }

    /// Each cell owns its loop. Toggling one never stops another sounding cell.
    func toggle(pitchIndex: Int, frequency: Double) throws {
        guard (0..<13).contains(pitchIndex), frequency.isFinite, (20...20_000).contains(frequency) else {
            throw WidgetToneError.invalidPitch
        }
        players = players.filter { $0.value.isPlaying }
        if let playing = players.removeValue(forKey: pitchIndex) {
            playing.stop()
            balanceVolume()
            deactivateIfSilent()
            return
        }

        do {
            if players.isEmpty {
                let session = AVAudioSession.sharedInstance()
                try session.setCategory(.playback, mode: .default)
                try session.setActive(true)
            }
            let nextPlayer = try AVAudioPlayer(
                data: PlayWidgetPitchIntent.loopingTone(frequency: frequency)
            )
            nextPlayer.numberOfLoops = -1
            players[pitchIndex] = nextPlayer
            balanceVolume()
            nextPlayer.prepareToPlay()
            guard nextPlayer.play() else {
                throw WidgetToneError.playbackDidNotStart
            }
        } catch {
            players.removeValue(forKey: pitchIndex)?.stop()
            balanceVolume()
            deactivateIfSilent()
            throw error
        }
    }

    func stop() {
        players.values.forEach { $0.stop() }
        players.removeAll()
        WidgetPitchState.set([])
        deactivateIfSilent()
    }

    private func balanceVolume() {
        // Keep the summed waveforms below full scale even with all 13 cells on.
        let volume = 1 / Float(max(1, players.count))
        players.values.forEach { $0.volume = volume }
    }

    private func deactivateIfSilent() {
        guard players.isEmpty else { return }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
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
            try player.toggle(pitchIndex: pitchIndex, frequency: frequency)
        }
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
