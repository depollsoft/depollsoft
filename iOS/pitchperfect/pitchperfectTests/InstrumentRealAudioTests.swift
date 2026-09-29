//
//  InstrumentRealAudioTests.swift
//  pitchperfectTests
//
//  The MIDI player through the real audio engine and output, listening to
//  the samplers' mix. They need audio output, which CI runners don't have,
//  so they run only with TEST_RUNNER_PP_REAL_AUDIO=1 (the simulator plays
//  through the Mac). Offline rendering can't catch what they check: the
//  first-tap bug was a race with the real render thread.
//

import AVFoundation
import XCTest
@testable import pitchperfect

final class InstrumentRealAudioTests: XCTestCase {
    private final class Meter {
        private let lock = NSLock()
        private var readings: [(time: TimeInterval, peak: Float)] = []

        func add(_ buffer: AVAudioPCMBuffer) {
            guard let data = buffer.floatChannelData?[0] else { return }
            var peak: Float = 0
            for i in 0..<Int(buffer.frameLength) { peak = max(peak, abs(data[i])) }
            let time = ProcessInfo.processInfo.systemUptime
            lock.withLock { readings.append((time, peak)) }
        }

        func peak(from start: TimeInterval, for length: TimeInterval) -> Float {
            lock.withLock { readings.filter { $0.time >= start && $0.time < start + length }.map(\.peak).max() ?? 0 }
        }
    }

    private var player: MIDINotePlayer!
    private let meter = Meter()

    override func setUpWithError() throws {
        guard ProcessInfo.processInfo.environment["PP_REAL_AUDIO"] == "1" else {
            throw XCTSkip("needs audio output: set TEST_RUNNER_PP_REAL_AUDIO=1")
        }
        let engine = SamplerEngine()
        engine.onOutputConnected = { [meter] mix in
            mix.installTap(onBus: 0, bufferSize: 256, format: nil) { buffer, _ in meter.add(buffer) }
        }
        player = MIDINotePlayer(host: engine, tuning: .bundled)
    }

    private func wait(_ seconds: TimeInterval) {
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    }

    private let c4 = 261.6255653
    private let e4 = 329.6275569

    /// Choosing sound after sound while the output is running, as someone
    /// comparing sounds in Settings does: each load lands on a running
    /// engine, and each preview must still be heard. Before the fix every
    /// preview after the first was silent.
    func testEveryPreviewIsHeardWhileTheOutputRuns() throws {
        var silent: [String] = []
        for sound in DPNoteSound.instruments() {
            let at = ProcessInfo.processInfo.systemUptime
            player.prepare(sound: sound)
            let preview = try XCTUnwrap(player.start(sound: sound, a440Frequency: c4))
            wait(0.8)
            player.stopNote(preview)
            wait(0.2)
            let peak = meter.peak(from: at, for: 1.0)
            if peak < 0.02 { silent.append("\(sound) \(peak)") }
        }
        XCTAssertEqual(silent, [])
    }

    /// A quick tap after the output has stopped for idleness.
    func testAQuickTapAfterTheIdleStopIsHeard() throws {
        var silent: [String] = []
        for sound in DPNoteSound.instruments() {
            player.prepare(sound: sound)
            wait(2.5)
            let at = ProcessInfo.processInfo.systemUptime
            let note = try XCTUnwrap(player.start(sound: sound, a440Frequency: e4))
            wait(0.08)
            player.stopNote(note)
            wait(0.8)
            let peak = meter.peak(from: at, for: 0.9)
            if peak < 0.02 { silent.append("\(sound) \(peak)") }
        }
        XCTAssertEqual(silent, [])
    }
}
