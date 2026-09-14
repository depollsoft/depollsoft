//
//  DPTagTracksController.swift
//  tagmaster
//
//  Created by David Poll on 8/1/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

// Playback ownership spans preparation and the native player's presentation.
import Foundation
import AVKit

@objc final class TMTrackPlayerController: AVPlayerViewController {
    var onDismiss: (() -> Void)?

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if isBeingDismissed || presentingViewController == nil {
            onDismiss?()
        }
    }
}

@objc final class TMTrackPlaybackSession: NSObject {
    let player: AVPlayer
    let playerController = TMTrackPlayerController()
    private var observation: NSKeyValueObservation?
    private var failureNotification: NSObjectProtocol?
    private var timeout: DispatchWorkItem?
    private var settle: (() -> Void)?
    private var onReady: (() -> Void)?
    private var onFailure: (() -> Void)?
    private var ready = false
    private var ended = false

    init(player: AVPlayer, settle: @escaping () -> Void) {
        self.player = player
        self.settle = settle
        super.init()
        playerController.showsPlaybackControls = true
        playerController.player = player
    }

    func start(item: AVPlayerItem, timeoutInterval: TimeInterval,
               onReady: @escaping () -> Void, onFailure: @escaping () -> Void) {
        self.onReady = onReady
        self.onFailure = onFailure
        observation = item.observe(\.status, options: [.initial, .new]) { [weak self] item, _ in
            // Capture the event, not a later status. AVFoundation can notify off-main.
            let status = item.status
            DispatchQueue.main.async { [weak self] in self?.statusChanged(status) }
        }
        failureNotification = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime, object: item, queue: nil
        ) { [weak self] _ in
            DispatchQueue.main.async { [weak self] in self?.fail() }
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

    private func statusChanged(_ status: AVPlayerItem.Status) {
        guard !ended else { return }
        switch status {
        case .readyToPlay:
            guard !ready else { return }
            ready = true
            timeout?.cancel()
            timeout = nil
            settleRow()
            let completion = onReady
            onReady = nil
            completion?()
        case .failed: fail()
        default: break
        }
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
        observation?.invalidate()
        observation = nil
        if let failureNotification { NotificationCenter.default.removeObserver(failureNotification) }
        failureNotification = nil
        timeout?.cancel()
        timeout = nil
        onReady = nil
        onFailure = nil
        playerController.onDismiss = nil
        player.pause()
        player.replaceCurrentItem(with: nil)
        playerController.player = nil
        settleRow()
    }

    deinit { cancel() }
}

extension DPTagTracksController: UITableViewDelegate {
    // Factory methods keep real KVO/notification wiring testable without network requests.
    @objc dynamic func makePlaybackItem(url: URL) -> AVPlayerItem { AVPlayerItem(url: url) }
    @objc dynamic func makePlaybackPlayer(item: AVPlayerItem) -> AVPlayer { AVPlayer(playerItem: item) }
    @objc dynamic var playbackReadyTimeout: TimeInterval { 15 }

    @objc func cancelPlayback() {
        let session = playbackSession
        playbackSession = nil
        session?.cancel()
        if session?.playerController.presentingViewController != nil {
            session?.playerController.dismiss(animated: false)
        }
    }

    @objc func playbackViewWillDisappear() {
        // Full-screen native playback hides this page without leaving the track.
        if let player = playbackSession?.playerController,
           presentedViewController === player,
           !isBeingDismissed, !isMovingFromParent,
           navigationController?.isBeingDismissed != true,
           parent?.isBeingDismissed != true { return }
        playbackHasLeft = true
        cancelPlayback()
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard !playbackHasLeft, indexPath.row >= 0, indexPath.row < tag.tracks.count,
              busyIndicator.busyCount == 0 else { return }
        let selectedTag = tag!
        let track = selectedTag.tracks[indexPath.row]
        cancelPlayback()

        let cell = tableView.cellForRow(at: indexPath)
        let spinner = TMBarberPoleLoadingView(operationName: "Loading track")
        spinner.isAccessibilityElement = false
        spinner.startAnimating()
        cell?.accessoryView = spinner
        cell?.accessibilityLabel = "\(track.title ?? "Track"), loading"
        let busy = busyIndicator!
        busy.incrementBusyCount()

        // Keep the existing stream-first path and offline cache key.
        let cachedPath = DPFileCache.path(forKey: track.source.cacheKey) ?? ""
        let isCached = !cachedPath.isEmpty && FileManager.default.fileExists(atPath: cachedPath)
        let sourceURL: URL = isCached ? URL(fileURLWithPath: cachedPath) : track.source.uri
        let item = makePlaybackItem(url: sourceURL)
        let session = TMTrackPlaybackSession(player: makePlaybackPlayer(item: item)) { [weak cell] in
            // Do not depend on the presenter surviving to balance its busy indicator.
            spinner.stopAnimating()
            busy.decrementBusyCount()
            if cell?.accessoryView === spinner {
                cell?.accessoryView = nil
                cell?.accessibilityLabel = nil
            }
        }
        playbackSession = session
        session.playerController.onDismiss = { [weak self, weak session] in
            guard let self, let session, self.playbackSession === session else { return }
            self.cancelPlayback()
        }
        session.start(item: item, timeoutInterval: playbackReadyTimeout, onReady: { [weak self, weak session] in
            guard let self, let session, self.playbackSession === session,
                  !self.playbackHasLeft, self.tag === selectedTag else { return }
            self.present(session.playerController, animated: true) { [weak self, weak session] in
                guard let self, let session, self.playbackSession === session,
                      !self.playbackHasLeft, session.player.currentItem != nil else { return }
                session.player.play()
            }
            if !isCached { DPTagTracksController.cache(track: track) }
        }, onFailure: { [weak self, weak session, weak tableView] in
            guard let self, let session, self.playbackSession === session,
                  !self.playbackHasLeft, self.tag === selectedTag else { return }
            let showError = { [weak self, weak session, weak tableView] in
                guard let self, let session, self.playbackSession === session,
                      !self.playbackHasLeft, self.tag === selectedTag else { return }
                self.tm_showError("The learning track couldn't be played. Check your connection and try again.") { [weak self, weak session, weak tableView] in
                    guard let self, let session, let tableView, self.playbackSession === session,
                          !self.playbackHasLeft, self.tag === selectedTag,
                          let row = self.tag.tracks.firstIndex(where: { $0 === track }) else { return }
                    self.tableView(tableView, didSelectRowAt: IndexPath(row: row, section: 0))
                }
            }
            // Present recovery from the tracks page only after native playback is gone.
            if session.playerController.presentingViewController != nil {
                session.playerController.dismiss(animated: true, completion: showError)
            } else {
                showError()
            }
        })
    }

    /// Saves a streamed track under its existing DPFileCache key for offline reuse.
    private static func cache(track: DPTrack) {
        guard let url = track.source.uri, !url.isFileURL else { return }
        let key = track.source.cacheKey
        let session = URLSession(configuration: {
            let configuration = URLSessionConfiguration.default
            configuration.timeoutIntervalForRequest = DPRemoteRequestTimeout
            return configuration
        }())
        session.downloadTask(with: url) { location, response, _ in
            defer { session.finishTasksAndInvalidate() }
            guard let location,
                  (response as? HTTPURLResponse).map({ $0.statusCode < 400 }) ?? true,
                  let data = try? Data(contentsOf: location), !data.isEmpty else { return }
            DPFileCache.write(data, forKey: key)
        }.resume()
    }
}
