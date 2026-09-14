//
//  DPTagTracksController.swift
//  tagmaster
//
//  Created by David Poll on 8/1/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

// Playback ownership spans fetching, decoding, and the inline balance player.
import Foundation
import AVFoundation
import os

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

extension DPTagTracksController: UITableViewDelegate {
    // Factory and presentation hooks keep the real loading flow testable without network requests.
    @objc dynamic func makeTrackLoader(url: URL, cacheKey: String) -> TMTrackLoader { TMTrackLoader(url: url, cacheKey: cacheKey) }
    @objc dynamic var playbackReadyTimeout: TimeInterval { 30 }

    /// Shows a decoded track in the inline player and starts it.
    @objc dynamic func presentPlayer(for track: DPTrack, buffer: AVAudioPCMBuffer) {
        guard let playerView else { return }
        playerView.load(track: track, buffer: buffer)
        playerView.isHidden = false
        view.setNeedsLayout()
        playerView.play()
        UIAccessibility.post(notification: .layoutChanged, argument: playerView.playPauseButton)
    }

    /// Cancels an in-flight load. Playback already in the inline player is left alone.
    @objc func cancelPlayback() {
        let session = playbackSession
        playbackSession = nil
        session?.cancel()
    }

    /// Cancels loading and stops the inline player, e.g. when the page or tag goes away.
    @objc func stopPlayback() {
        cancelPlayback()
        guard let playerView else { return }
        playerView.unload()
        playerView.isHidden = true
        if isViewLoaded { view.setNeedsLayout() }
    }

    @objc func playbackViewWillDisappear() {
        playbackHasLeft = true
        stopPlayback()
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard !playbackHasLeft, indexPath.row >= 0, indexPath.row < tag.tracks.count,
              busyIndicator.busyCount == 0 else { return }
        let selectedTag = tag!
        let track = selectedTag.tracks[indexPath.row]
        cancelPlayback()
        if let playerView, playerView.track === track, playerView.player.isLoaded {
            // Same track again: restart it rather than reloading.
            playerView.stop()
            playerView.play()
            return
        }
        // Android stops the old track as soon as a new one is chosen.
        playerView?.stop()

        let cell = tableView.cellForRow(at: indexPath)
        let spinner = TMBarberPoleLoadingView(operationName: "Loading track")
        spinner.isAccessibilityElement = false
        spinner.startAnimating()
        cell?.accessoryView = spinner
        cell?.accessibilityLabel = "\(track.title ?? "Track"), loading"
        let busy = busyIndicator!
        busy.incrementBusyCount()

        // Prefer the offline copy under the existing cache key; otherwise download into it.
        let cachedPath = DPFileCache.path(forKey: track.source.cacheKey) ?? ""
        let isCached = !cachedPath.isEmpty && FileManager.default.fileExists(atPath: cachedPath)
        let sourceURL: URL = isCached ? URL(fileURLWithPath: cachedPath) : track.source.uri
        let loader = makeTrackLoader(url: sourceURL, cacheKey: track.source.cacheKey)
        let session = TMTrackPlaybackSession(track: track, loader: loader) { [weak cell] in
            // Do not depend on the page surviving to balance its busy indicator.
            spinner.stopAnimating()
            busy.decrementBusyCount()
            if cell?.accessoryView === spinner {
                cell?.accessoryView = nil
                cell?.accessibilityLabel = nil
            }
        }
        playbackSession = session
        session.start(timeoutInterval: playbackReadyTimeout, onReady: { [weak self, weak session] buffer in
            guard let self, let session, self.playbackSession === session,
                  !self.playbackHasLeft, self.tag === selectedTag else { return }
            self.playbackSession = nil
            self.presentPlayer(for: track, buffer: buffer)
        }, onFailure: { [weak self, weak session, weak tableView] in
            guard let self, let session, self.playbackSession === session,
                  !self.playbackHasLeft, self.tag === selectedTag else { return }
            // The failed session stays current so only its own retry can start a fresh one.
            self.tm_showError("The learning track couldn't be played. Check your connection and try again.") { [weak self, weak session, weak tableView] in
                guard let self, let session, let tableView, self.playbackSession === session,
                      !self.playbackHasLeft, self.tag === selectedTag,
                      let row = self.tag.tracks.firstIndex(where: { $0 === track }) else { return }
                self.tableView(tableView, didSelectRowAt: IndexPath(row: row, section: 0))
            }
        })
    }
}
