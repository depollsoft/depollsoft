//
//  TMBalanceAudioPlayerTests.swift
//  tagmasterTests
//
//  Proves the inline player is a balance, not a pan: each channel keeps its own
//  signal and only ever gets quieter.
//

import XCTest
import AVFoundation
@testable import tagmaster

// Swift classes must be subclassed in Swift. Both Objective-C playback suites
// use this fixture through the TMReviewLoaderControl protocol.
@objc(TMReviewLoader) final class TMReviewLoader: TMTrackLoader {
    @objc var completion: ((AVAudioPCMBuffer?, Error?) -> Void)?
    @objc private(set) var cancels = 0
    @objc var backgroundDelivery = false

    override func load(completion: @escaping (AVAudioPCMBuffer?, Error?) -> Void) {
        self.completion = completion
    }

    // Keep the callback so tests can deliver duplicate or already queued events
    // after cancellation and verify the session's guards.
    override func cancel() { cancels += 1 }

    @objc(succeedWithBuffer:) func succeed(with buffer: AVAudioPCMBuffer) {
        deliver(buffer: buffer, error: nil)
    }

    @objc func fail() {
        deliver(buffer: nil, error: NSError(domain: "TMReviewLoader", code: 1))
    }

    private func deliver(buffer: AVAudioPCMBuffer?, error: Error?) {
        guard let completion else { preconditionFailure("Select a track before delivering its result") }
        if backgroundDelivery {
            DispatchQueue.global().async { completion(buffer, error) }
        } else {
            completion(buffer, error)
        }
    }
}

final class TMBalanceAudioPlayerTests: XCTestCase {
    private let sampleRate = 44100.0
    private var files: [URL] = []

    override func tearDown() {
        files.forEach { try? FileManager.default.removeItem(at: $0) }
        super.tearDown()
    }

    /// 440 Hz on the left and 880 Hz on the right, so cross-mixing would show up as signal in a muted side.
    private func stereoTone(seconds: Double = 0.25) -> AVAudioPCMBuffer {
        let frames = AVAudioFrameCount(sampleRate * seconds)
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2)!
        let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames)!
        buffer.frameLength = frames
        for i in 0..<Int(frames) {
            let t = Double(i) / sampleRate
            buffer.floatChannelData![0][i] = Float(sin(2 * .pi * 440 * t)) * 0.5
            buffer.floatChannelData![1][i] = Float(sin(2 * .pi * 880 * t)) * 0.5
        }
        return buffer
    }

    private func rms(_ buffer: AVAudioPCMBuffer, channel: Int, from: Int = 0, count: Int? = nil) -> Float {
        let n = count ?? (Int(buffer.frameLength) - from)
        guard n > 0 else { return 0 }
        let p = buffer.floatChannelData![channel] + from
        var sum: Float = 0
        for i in 0..<n { sum += p[i] * p[i] }
        return (sum / Float(n)).squareRoot()
    }

    private func writeFile(_ buffer: AVAudioPCMBuffer, name: String) throws -> URL {
        let url = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("\(name)-\(UUID().uuidString).caf")
        let file = try AVAudioFile(forWriting: url, settings: buffer.format.settings)
        try file.write(from: buffer)
        files.append(url)
        return url
    }

    func testGainsMatchAndroidBalanceMath() {
        // Android: right = v/1000, left = (1000 - v)/1000, both divided by the larger one.
        let cases: [(Float, Float, Float)] = [(0, 1, 0), (0.25, 1, 1.0 / 3), (0.5, 1, 1), (0.75, 1.0 / 3, 1), (1, 0, 1)]
        for (balance, left, right) in cases {
            let gains = TMBalanceGains(balance: balance)
            XCTAssertEqual(gains.left, left, accuracy: 0.0001, "balance \(balance)")
            XCTAssertEqual(gains.right, right, accuracy: 0.0001, "balance \(balance)")
        }
        XCTAssertEqual(TMBalanceGains(balance: -3).left, 1)
        XCTAssertEqual(TMBalanceGains(balance: -3).right, 0)
        XCTAssertEqual(TMBalanceGains(balance: .nan).left, 1)
        XCTAssertEqual(TMBalanceGains(balance: .nan).right, 1)
    }

    func testDecodeProducesFloatBufferWithOriginalLength() throws {
        let source = stereoTone()
        let url = try writeFile(source, name: "decode")
        let decoded = try TMBalanceAudioPlayer.decode(fileAt: url)
        XCTAssertEqual(decoded.frameLength, source.frameLength)
        XCTAssertEqual(decoded.format.channelCount, 2)
        XCTAssertEqual(decoded.format.commonFormat, .pcmFormatFloat32)
        XCTAssertFalse(decoded.format.isInterleaved)
        XCTAssertEqual(rms(decoded, channel: 0), rms(source, channel: 0), accuracy: 0.001)
    }

    func testDecodeStopsWhenCancelled() throws {
        let url = try writeFile(stereoTone(seconds: 3), name: "cancel")
        XCTAssertThrowsError(try TMBalanceAudioPlayer.decode(fileAt: url, shouldCancel: { true }))
    }

    func testLoaderEvictsCachedFileThatWillNotDecode() throws {
        let key = "evict-\(UUID().uuidString).mp3"
        DPFileCache.write(Data("not audio".utf8), forKey: key)
        let path = try XCTUnwrap(DPFileCache.path(forKey: key))
        XCTAssertTrue(FileManager.default.fileExists(atPath: path))
        let loader = TMTrackLoader(url: URL(fileURLWithPath: path), cacheKey: key)
        let done = expectation(description: "load finished")
        var receivedError: Error?
        loader.load { buffer, error in
            XCTAssertTrue(Thread.isMainThread)
            XCTAssertNil(buffer)
            receivedError = error
            done.fulfill()
        }
        wait(for: [done], timeout: 5)
        XCTAssertNotNil(receivedError)
        XCTAssertFalse(FileManager.default.fileExists(atPath: path), "A corrupt cache entry must not be offered again on retry")
    }

    func testLoaderNeverCompletesAfterCancel() throws {
        let url = try writeFile(stereoTone(seconds: 2), name: "cancel-load")
        let loader = TMTrackLoader(url: url, cacheKey: "cancel-\(UUID().uuidString)")
        loader.load { _, _ in XCTFail("Completion after cancel") }
        loader.cancel()
        let settled = expectation(description: "drained")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { settled.fulfill() }
        wait(for: [settled], timeout: 3)
    }

    func testDecodeRejectsNonAudio() throws {
        let url = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("junk-\(UUID().uuidString).mp3")
        try Data("not audio".utf8).write(to: url)
        files.append(url)
        XCTAssertThrowsError(try TMBalanceAudioPlayer.decode(fileAt: url))
    }

    func testTransportAndSeeking() throws {
        let player = TMBalanceAudioPlayer()
        // Transport state does not need a host audio device. Keep the real
        // engine graph, as in the rendered-signal tests below.
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2)!
        try player.engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
        XCTAssertFalse(player.isLoaded)
        XCTAssertFalse(player.play())
        player.load(stereoTone(seconds: 2))
        XCTAssertTrue(player.isLoaded)
        XCTAssertEqual(player.duration, 2, accuracy: 0.001)
        XCTAssertEqual(player.currentTime, 0)
        player.seek(to: 1.5)
        XCTAssertEqual(player.currentTime, 1.5, accuracy: 0.001)
        player.seek(to: 99)
        XCTAssertEqual(player.currentTime, 2, accuracy: 0.001, "Seeks clamp to the end")
        player.seek(to: -4)
        XCTAssertEqual(player.currentTime, 0)
        player.seek(to: 0.5)
        player.stop()
        XCTAssertEqual(player.currentTime, 0, "Stop rewinds")
        XCTAssertFalse(player.isPlaying)
        player.setBalance(0.2)
        XCTAssertEqual(player.balance, 0.2, accuracy: 0.0001)
        player.setBalance(7)
        XCTAssertEqual(player.balance, 1)
        player.unload()
        XCTAssertFalse(player.isLoaded)
        XCTAssertEqual(player.duration, 0)
    }

    func testCounterTextMatchesAndroid() {
        XCTAssertEqual(TMTrackPlayerModel.counterText(position: 1.26, length: 12.04), "1.3/12.0s")
        XCTAssertEqual(TMTrackPlayerModel.counterText(position: -1, length: 0), "0.0/0.0s")
    }

    /// Renders the real engine graph offline and checks each output channel's level.
    func testRenderedOutputIsABalanceNotAPan() throws {
        let source = stereoTone()
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2)!
        let expectations: [(Float, Float, Float)] = [(0, 1, 0), (0.25, 1, 1.0 / 3), (0.5, 1, 1), (0.75, 1.0 / 3, 1), (1, 0, 1)]
        let sourceLeft = rms(source, channel: 0), sourceRight = rms(source, channel: 1)
        for (balance, expectedLeft, expectedRight) in expectations {
            let player = TMBalanceAudioPlayer()
            let engine = player.engine
            try engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
            player.load(source)
            player.setBalance(balance)
            XCTAssertTrue(player.play())
            let output = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: source.frameLength)!
            let chunk = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 4096)!
            while output.frameLength < source.frameLength {
                let want = min(4096, source.frameLength - output.frameLength)
                XCTAssertEqual(try engine.renderOffline(want, to: chunk), .success)
                for channel in 0..<2 {
                    (output.floatChannelData![channel] + Int(output.frameLength))
                        .update(from: chunk.floatChannelData![channel], count: Int(chunk.frameLength))
                }
                output.frameLength += chunk.frameLength
            }
            // Skip the mixer's start-up ramp so levels compare against a steady state.
            let skip = 4096
            let outLeft = rms(output, channel: 0, from: skip), outRight = rms(output, channel: 1, from: skip)
            XCTAssertEqual(outLeft / sourceLeft, expectedLeft, accuracy: 0.03, "balance \(balance) left")
            XCTAssertEqual(outRight / sourceRight, expectedRight, accuracy: 0.03, "balance \(balance) right")
            if expectedLeft == 0 { XCTAssertEqual(outLeft, 0, "Muted left must be silent, not a pan of the right") }
            if expectedRight == 0 { XCTAssertEqual(outRight, 0, "Muted right must be silent, not a pan of the left") }
            // Sample-for-sample, each output channel is its own source channel times its gain.
            // A mix of both sources into one ear would match the levels above but fail here.
            var worst: Float = 0
            for i in skip..<Int(source.frameLength) {
                worst = max(worst, abs(output.floatChannelData![0][i] - source.floatChannelData![0][i] * expectedLeft))
                worst = max(worst, abs(output.floatChannelData![1][i] - source.floatChannelData![1][i] * expectedRight))
            }
            XCTAssertLessThan(worst, 0.02, "balance \(balance): output must be the isolated source channel")
            XCTAssertEqual(player.currentTime, player.duration, accuracy: 0.01)
            player.unload()
        }
    }

    func testMonoSourceFeedsBothSides() throws {
        let frames = AVAudioFrameCount(sampleRate * 0.25)
        let mono = AVAudioPCMBuffer(pcmFormat: AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!, frameCapacity: frames)!
        mono.frameLength = frames
        for i in 0..<Int(frames) { mono.floatChannelData![0][i] = Float(sin(2 * .pi * 440 * Double(i) / sampleRate)) * 0.5 }
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2)!
        let player = TMBalanceAudioPlayer()
        let engine = player.engine
        try engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
        player.load(mono)
        player.setBalance(1)
        XCTAssertTrue(player.play())
        let output = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 4096)!
        _ = try engine.renderOffline(4096, to: output)
        _ = try engine.renderOffline(4096, to: output)
        XCTAssertEqual(rms(output, channel: 0), 0, "Balance fully right silences the left copy")
        XCTAssertGreaterThan(rms(output, channel: 1), 0.3)
        player.unload()
    }
}
