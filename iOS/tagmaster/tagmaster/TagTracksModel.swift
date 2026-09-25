//
//  TagTracksModel.swift
//  tagmaster
//
//  The Tracks page's behaviour: bringing a learning track into the inline
//  balance player (from the file cache or the network, with a readiness
//  timeout, one row busy at a time), and the player's own transport state.
//

import AVFoundation
import Foundation
import Observation
import os
import UIKit

/// Fetches a track (from the file cache or the network) and decodes it into a
/// PCM buffer the balance player can render. Subclassed in tests.
@objc class TMTrackLoader: NSObject {
    @objc let url: URL
    @objc let cacheKey: String
    private var task: URLSessionDownloadTask?
    private var session: URLSession?
    private let cancelFlag = OSAllocatedUnfairLock(initialState: false)
    private var isCancelled: Bool { cancelFlag.withLock { $0 } }

    @objc init(url: URL, cacheKey: String) {
        self.url = url
        self.cacheKey = cacheKey
        super.init()
    }

    /// Completion is delivered on the main thread, at most once, and never after `cancel()`.
    @objc func load(completion: @escaping (AVAudioPCMBuffer?, Error?) -> Void) {
        if url.isFileURL {
            decode(url, deleteAfterwards: false, completion: completion)
            return
        }
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = DPRemoteRequestTimeout
        let session = URLSession(configuration: configuration)
        self.session = session
        let key = cacheKey
        task = session.downloadTask(with: url) { [weak self] location, response, error in
            defer { session.finishTasksAndInvalidate() }
            guard let self, !self.isCancelled else { return }
            if let error { self.deliver(nil, error, completion); return }
            // Stay file-backed: the download is moved, never read into memory.
            guard let location,
                  (response as? HTTPURLResponse).map({ $0.statusCode < 400 }) ?? true,
                  let size = (try? FileManager.default.attributesOfItem(atPath: location.path))?[.size] as? NSNumber,
                  size.intValue > 0 else {
                self.deliver(nil, TMTrackLoader.unavailable, completion)
                return
            }
            guard size.intValue <= TMTrackLoader.maximumDownloadBytes else {
                self.deliver(nil, TMTrackLoader.tooLarge, completion)
                return
            }
            if let cached = DPFileCache.path(forKey: key), TMTrackLoader.move(location, to: URL(fileURLWithPath: cached)) {
                self.decode(URL(fileURLWithPath: cached), deleteAfterwards: false, completion: completion)
            } else {
                // The cache refused the file; decode a private copy and drop it afterwards.
                let temporary = URL(fileURLWithPath: NSTemporaryDirectory())
                    .appendingPathComponent("tm-track-\(UUID().uuidString)")
                if TMTrackLoader.move(location, to: temporary) {
                    self.decode(temporary, deleteAfterwards: true, completion: completion)
                } else {
                    self.deliver(nil, TMTrackLoader.unavailable, completion)
                }
            }
        }
        task?.resume()
    }

    /// Largest track the loader will accept from the network. Learning tracks are a few megabytes.
    @objc static let maximumDownloadBytes = 64 * 1024 * 1024

    private static func move(_ source: URL, to destination: URL) -> Bool {
        let manager = FileManager.default
        try? manager.removeItem(at: destination)
        do {
            try manager.moveItem(at: source, to: destination)
            return true
        } catch {
            return false
        }
    }

    @objc func cancel() {
        cancelFlag.withLock { $0 = true }
        task?.cancel()
        task = nil
        session?.invalidateAndCancel()
        session = nil
    }

    private func deliver(_ buffer: AVAudioPCMBuffer?, _ error: Error?, _ completion: @escaping (AVAudioPCMBuffer?, Error?) -> Void) {
        // cancel() runs on main, so checking there makes "never after cancel" exact.
        DispatchQueue.main.async { [weak self] in
            guard let self, !self.isCancelled else { return }
            completion(buffer, error)
        }
    }

    private func decode(_ fileURL: URL, deleteAfterwards: Bool, completion: @escaping (AVAudioPCMBuffer?, Error?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self, !self.isCancelled else {
                if deleteAfterwards { try? FileManager.default.removeItem(at: fileURL) }
                return
            }
            defer { if deleteAfterwards { try? FileManager.default.removeItem(at: fileURL) } }
            do {
                let buffer = try TMBalanceAudioPlayer.decode(fileAt: fileURL, shouldCancel: { self.isCancelled })
                self.deliver(buffer, nil, completion)
            } catch {
                // A cached file that will not decode must not be offered again on retry.
                if !deleteAfterwards, let cached = DPFileCache.path(forKey: self.cacheKey), fileURL.path == cached {
                    try? FileManager.default.removeItem(atPath: cached)
                }
                self.deliver(nil, error, completion)
            }
        }
    }

    static let unavailable = NSError(domain: "TMTrackLoader", code: 1,
                                     userInfo: [NSLocalizedDescriptionKey: "The learning track is unavailable."])
    static let tooLarge = NSError(domain: "TMTrackLoader", code: 2,
                                  userInfo: [NSLocalizedDescriptionKey: "The learning track is too large to load."])
}

/// One attempt to bring a track into the inline player. Owns the loader, the
/// readiness timeout, and the row's busy state until it is settled or cancelled.
@objc final class TMTrackPlaybackSession: NSObject {
    @objc let loader: TMTrackLoader
    @objc let track: DPTrack
    private var timeout: DispatchWorkItem?
    private var settle: (() -> Void)?
    private var onReady: ((AVAudioPCMBuffer) -> Void)?
    private var onFailure: (() -> Void)?
    private var ended = false

    @objc init(track: DPTrack, loader: TMTrackLoader, settle: @escaping () -> Void) {
        self.track = track
        self.loader = loader
        self.settle = settle
        super.init()
    }

    @objc func start(timeoutInterval: TimeInterval,
                     onReady: @escaping (AVAudioPCMBuffer) -> Void, onFailure: @escaping () -> Void) {
        self.onReady = onReady
        self.onFailure = onFailure
        loader.load { [weak self] buffer, _ in
            // Loading can complete off-main; the page only acts on main.
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                if let buffer { self.ready(buffer) } else { self.fail() }
            }
        }
        let timeout = DispatchWorkItem { [weak self] in self?.fail() }
        self.timeout = timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + timeoutInterval, execute: timeout)
    }

    private func settleRow() {
        let completion = settle
        settle = nil
        completion?()
    }

    private func ready(_ buffer: AVAudioPCMBuffer) {
        guard !ended else { return }
        ended = true
        timeout?.cancel()
        timeout = nil
        onFailure = nil
        settleRow()
        let completion = onReady
        onReady = nil
        completion?(buffer)
    }

    private func fail() {
        guard !ended else { return }
        let completion = onFailure
        cancel()
        completion?()
    }

    @objc func cancel() {
        guard !ended else { return }
        ended = true
        loader.cancel()
        timeout?.cancel()
        timeout = nil
        onReady = nil
        onFailure = nil
        settleRow()
    }

    deinit { cancel() }
}

// MARK: - The inline player

/// The balance player's transport, as the inline player shows it. Mirrors Android's MediaPlayerView.
@Observable @MainActor
final class TMTrackPlayerModel {
    let player = TMBalanceAudioPlayer()
    private(set) var track: DPTrack?
    private(set) var isPlaying = false
    private(set) var isLoaded = false
    private(set) var position: TimeInterval = 0
    private(set) var duration: TimeInterval = 0
    private(set) var balance: Float = TMBalanceAudioPlayer.centeredBalance
    /// While a finger is on the scrub bar the timer must not move its thumb.
    var scrubbing = false

    init() {
        player.onProgress = { [weak self] in MainActor.assumeIsolated { self?.refresh() } }
        player.onEnded = { [weak self] in
            MainActor.assumeIsolated {
                self?.player.stop()
                self?.refresh()
            }
        }
        player.onInterrupted = { [weak self] in MainActor.assumeIsolated { self?.refresh() } }
    }

    /// Shows a decoded track. Playback does not start until `play()`.
    func load(track: DPTrack, buffer: AVAudioPCMBuffer) {
        self.track = track
        player.load(buffer)
        scrubbing = false
        refresh()
    }

    func play() { _ = player.play(); refresh() }
    func pause() { player.pause(); refresh() }
    func stop() { player.stop(); refresh() }

    func togglePlayPause() {
        if player.isPlaying { player.pause() } else { _ = player.play() }
        refresh()
    }

    func unload() {
        track = nil
        player.unload()
        refresh()
    }

    func seek(to time: TimeInterval) {
        player.seek(to: time)
        position = player.currentTime
    }

    func setBalance(_ value: Float) {
        player.setBalance(value)
        balance = player.balance
    }

    func centerBalance() { setBalance(TMBalanceAudioPlayer.centeredBalance) }

    func refresh() {
        isPlaying = player.isPlaying
        isLoaded = player.isLoaded
        duration = player.duration
        if !scrubbing { position = player.currentTime }
        balance = player.balance
    }

    var canStop: Bool { isLoaded && (isPlaying || position > 0) }

    /// Android's `%1.1f/%1.1fs` counter.
    nonisolated static func counterText(position: TimeInterval, length: TimeInterval) -> String {
        String(format: "%1.1f/%1.1fs", max(0, position), max(0, length))
    }

    var counterText: String { TMTrackPlayerModel.counterText(position: position, length: duration) }

    var balanceDescription: String {
        let gains = TMBalanceGains(balance: balance)
        let left = Int((gains.left * 100).rounded()), right = Int((gains.right * 100).rounded())
        return left == right ? "Centered" : "Left \(left) percent, right \(right) percent"
    }
}

// MARK: - The page

@Observable @MainActor
final class TagTracksModel {
    weak var detail: TagDetailModel?
    let busy: TMBusyCount
    let player = TMTrackPlayerModel()

    /// Whether the inline player is showing (a track has been brought in).
    private(set) var playerVisible = false
    /// The row whose track is loading, if any.
    private(set) var loadingTrack: DPTrack?
    private(set) var session: TMTrackPlaybackSession?
    /// The page is off screen: nothing may start or present.
    private(set) var hasLeft = false

    // Seams for tests: how a track is fetched, and how long it may take.
    var makeLoader: (URL, String) -> TMTrackLoader = { TMTrackLoader(url: $0, cacheKey: $1) }
    var readyTimeout: TimeInterval = 30
    /// Shows a decoded track in the inline player and starts it. Replaceable in tests.
    var presentPlayer: ((DPTrack, AVAudioPCMBuffer) -> Void)?

    init(busy: TMBusyCount) {
        self.busy = busy
    }

    var tag: DPTag? { detail?.tag }
    var tracks: [DPTrack] { tag?.tracks ?? [] }

    func appeared() { hasLeft = false }

    func disappeared() {
        hasLeft = true
        stopPlayback()
    }

    /// Cancels an in-flight load. Playback already in the player is left alone.
    func cancelLoading() {
        let current = session
        session = nil
        current?.cancel()
    }

    /// Cancels loading and stops the player, e.g. when the page or the tag goes away.
    func stopPlayback() {
        cancelLoading()
        player.unload()
        playerVisible = false
    }

    private func present(_ track: DPTrack, buffer: AVAudioPCMBuffer) {
        player.load(track: track, buffer: buffer)
        playerVisible = true
        player.play()
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    func select(_ track: DPTrack) {
        guard !hasLeft, let tag, tag.tracks.contains(where: { $0 === track }), !busy.isBusy else { return }
        cancelLoading()
        if player.track === track, player.isLoaded {
            // Same track again: restart it rather than reloading.
            player.stop()
            player.play()
            return
        }
        // Android stops the old track as soon as a new one is chosen.
        player.stop()

        loadingTrack = track
        busy.begin()
        let source = track.source!
        // Prefer the offline copy under the existing cache key; otherwise download into it.
        let cachedPath = DPFileCache.path(forKey: source.cacheKey) ?? ""
        let isCached = !cachedPath.isEmpty && FileManager.default.fileExists(atPath: cachedPath)
        let url: URL = isCached ? URL(fileURLWithPath: cachedPath) : source.uri
        let session = TMTrackPlaybackSession(track: track, loader: makeLoader(url, source.cacheKey)) { [weak self, busy] in
            // Balanced whether or not the page survives.
            busy.end()
            MainActor.assumeIsolated {
                if self?.loadingTrack === track { self?.loadingTrack = nil }
            }
        }
        self.session = session
        session.start(timeoutInterval: readyTimeout, onReady: { [weak self, weak session] buffer in
            MainActor.assumeIsolated {
                guard let self, let session, self.session === session, !self.hasLeft, self.tag === tag else { return }
                self.session = nil
                if let presentPlayer = self.presentPlayer {
                    presentPlayer(track, buffer)
                } else {
                    self.present(track, buffer: buffer)
                }
            }
        }, onFailure: { [weak self, weak session] in
            MainActor.assumeIsolated {
                guard let self, let session, self.session === session, !self.hasLeft, self.tag === tag else { return }
                // The failed session stays current so only its own retry can start a fresh one.
                self.detail?.error = TMRecoverableError(
                    message: "The learning track couldn't be played. Check your connection and try again.",
                    retry: { [weak self, weak session] in
                        guard let self, let session, self.session === session, !self.hasLeft, self.tag === tag else { return }
                        self.session = nil
                        self.select(track)
                    })
            }
        })
    }
}
