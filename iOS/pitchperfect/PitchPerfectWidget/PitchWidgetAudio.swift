import AppIntents
import AVFoundation
import Foundation
import WidgetKit

let widgetKind = "PitchPerfectPitchPipe"

enum WidgetPitchState {
    private static let suiteName = "group.depollsoft.pitchperfect"
    private static let key = "activePitch"
    private static var defaults: UserDefaults? { UserDefaults(suiteName: suiteName) }

    static var activePitch: Int? {
        guard let defaults, defaults.object(forKey: key) != nil else { return nil }
        let value = defaults.integer(forKey: key)
        return value >= 0 ? value : nil
    }

    static func set(_ value: Int?) {
        defaults?.set(value ?? -1, forKey: key)
    }
}

private enum WidgetToneError: Error {
    case playbackDidNotStart
}

@MainActor
private final class WidgetTonePlayer {
    static let shared = WidgetTonePlayer()

    private var player: AVAudioPlayer?
    private var playbackID: UUID?

    func start(frequency: Double) throws -> UUID {
        let session = AVAudioSession.sharedInstance()
        player?.stop()
        player = nil
        playbackID = nil

        do {
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true)
            let nextPlayer = try AVAudioPlayer(
                data: PlayWidgetPitchIntent.tone(frequency: frequency, duration: PlayWidgetPitchIntent.duration)
            )
            nextPlayer.prepareToPlay()
            guard nextPlayer.play() else {
                throw WidgetToneError.playbackDidNotStart
            }
            let id = UUID()
            player = nextPlayer
            playbackID = id
            return id
        } catch {
            try? session.setActive(false, options: .notifyOthersOnDeactivation)
            throw error
        }
    }

    func stop(ifCurrent id: UUID) -> Bool {
        guard playbackID == id else { return false }
        player?.stop()
        player = nil
        playbackID = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        return true
    }
}

/// Runs in the containing app process when a widget cell is pressed. This
/// source is intentionally compiled into both the app and widget targets so
/// WidgetKit can discover the intent and the app can execute it.
struct PlayWidgetPitchIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "Sound Pitch"
    static let openAppWhenRun = false
    static let duration = 1.5

    @Parameter(title: "Pitch")
    var pitchIndex: Int

    @Parameter(title: "Frequency")
    var frequency: Double

    init() {}

    init(pitchIndex: Int, frequency: Double) {
        self.pitchIndex = pitchIndex
        self.frequency = frequency
    }

    func perform() async throws -> some IntentResult {
        let playbackID: UUID
        do {
            playbackID = try await MainActor.run {
                try WidgetTonePlayer.shared.start(frequency: frequency)
            }
        } catch {
            WidgetPitchState.set(nil)
            WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
            throw error
        }
        WidgetPitchState.set(pitchIndex)
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)

        // Keep the app process alive until the short pitch completes. A stale
        // intent must not stop or clear a newer pitch after a rapid second tap.
        try? await Task.sleep(for: .seconds(Self.duration))
        let stoppedCurrentPlayback = await MainActor.run {
            WidgetTonePlayer.shared.stop(ifCurrent: playbackID)
        }
        if stoppedCurrentPlayback {
            WidgetPitchState.set(nil)
            WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        }
        return .result()
    }

    static func tone(frequency: Double, duration: Double) -> Data {
        let sampleRate = 44_100
        let frames = Int(Double(sampleRate) * duration)
        var pcm = Data(capacity: frames * 2)
        let fadeFrames = sampleRate / 40
        for frame in 0..<frames {
            let attack = min(1.0, Double(frame) / Double(fadeFrames))
            let release = min(1.0, Double(frames - frame) / Double(fadeFrames))
            let envelope = min(attack, release)
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
