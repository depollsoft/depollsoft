//
//  TMTrackPlayerCaptureTests.swift
//  tagmasterTests
//
//  Mounts the Tracks page with a real local WAV, loads it through the production
//  loader into the inline balance player, and captures light/dark renders.
//

import XCTest
import AVFoundation
@testable import tagmaster

private final class TMCaptureLocation: DPRemoteLocation {
    var key = "capture-\(UUID().uuidString).wav"
    override var cacheKey: String { key }
}

final class TMTrackPlayerCaptureTests: XCTestCase {
    private var window: UIWindow!
    private var files: [URL] = []

    override func tearDown() {
        window?.isHidden = true
        window?.rootViewController = nil
        window = nil
        files.forEach { try? FileManager.default.removeItem(at: $0) }
        super.tearDown()
    }

    private func writeWAV() throws -> URL {
        let url = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("capture-\(UUID().uuidString).wav")
        let rate = 22050.0
        let format = AVAudioFormat(standardFormatWithSampleRate: rate, channels: 2)!
        let frames = AVAudioFrameCount(rate * 12)
        let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: frames)!
        buffer.frameLength = frames
        for i in 0..<Int(frames) {
            let t = Double(i) / rate
            buffer.floatChannelData![0][i] = Float(sin(2 * .pi * 220 * t)) * 0.2
            buffer.floatChannelData![1][i] = Float(sin(2 * .pi * 330 * t)) * 0.2
        }
        let file = try AVAudioFile(forWriting: url, settings: [
            AVFormatIDKey: kAudioFormatLinearPCM, AVSampleRateKey: rate, AVNumberOfChannelsKey: 2,
            AVLinearPCMBitDepthKey: 16, AVLinearPCMIsFloatKey: false, AVLinearPCMIsBigEndianKey: false
        ])
        try file.write(from: buffer)
        files.append(url)
        return url
    }

    private func settle() {
        let done = expectation(description: "settled")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { done.fulfill() }
        wait(for: [done], timeout: 2)
        window.layoutIfNeeded()
    }

    private func capture(_ name: String) {
        settle()
        let size = window.bounds.size
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let image = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        let dir = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("tm-track-player-captures")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        XCTAssertNoThrow(try image.pngData()!.write(to: dir.appendingPathComponent("\(name).png")))
    }

    func testInlinePlayerRendersInLightAndDark() throws {
        for dark in [false, true] {
            let tracks = DPTagTracksController()
            tracks.busyIndicator = DPBusyIndicator()
            let tag = DPTag()
            tag.tagId = 1809
            tag.title = "Lost"
            tag.recordingMethod = "Recorded by a quartet with each voice on its own side."
            let location = TMCaptureLocation()
            location.uri = try writeWAV()
            location.type = "wav"
            tag.tenorTrackUri = location
            let bari = TMCaptureLocation()
            bari.uri = location.uri
            bari.type = "wav"
            tag.baritoneTrackUri = bari
            tracks.tag = tag

            window = UIWindow(frame: CGRect(x: 0, y: 0, width: 393, height: 852))
            window.overrideUserInterfaceStyle = dark ? .dark : .light
            window.tintColor = .systemBlue
            window.backgroundColor = .systemBackground
            window.rootViewController = UINavigationController(rootViewController: tracks)
            window.makeKeyAndVisible()
            settle()
            capture(dark ? "tracks-dark-before" : "tracks-light-before")

            let table = tracks.value(forKey: "partsTable") as! UITableView
            tracks.tableView(table, didSelectRowAt: IndexPath(row: 0, section: 0))
            let shown = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in tracks.playerView?.isHidden == false }, object: nil)
            wait(for: [shown], timeout: 10)
            settle()
            XCTAssertTrue(tracks.playerView.player.isLoaded)
            tracks.playerView.player.pause()
            tracks.playerView.player.seek(to: 4.2)
            tracks.playerView.balanceSlider.value = 0.25
            tracks.playerView.balanceSlider.sendActions(for: .valueChanged)
            tracks.playerView.refresh()
            capture(dark ? "tracks-dark-player" : "tracks-light-player")
            XCTAssertEqual(tracks.playerView.counterLabel.text, "4.2/12.0s")
            tracks.stopPlayback()
            XCTAssertTrue(tracks.playerView.isHidden)
            for key in [location.cacheKey, bari.cacheKey] {
                if let path = DPFileCache.path(forKey: key) { try? FileManager.default.removeItem(atPath: path) }
            }
        }
    }
}
