//
//  DPTagTracksController.swift
//  tagmaster
//
//  Created by David Poll on 8/1/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import AVKit

extension DPTagTracksController: UITableViewDelegate {
    private static let readyTimeout: TimeInterval = 15

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row < self.tag.tracks.count else { return }
        let track = self.tag.tracks[indexPath.row]
        if self.busyIndicator.busyCount > 0 { return }

        // Only the tapped row shows progress; the page and navigation stay usable.
        let cell = tableView.cellForRow(at: indexPath)
        let spinner = TMBarberPoleLoadingView(operationName: "Loading track")
        spinner.isAccessibilityElement = false // The row names the operation once.
        spinner.startAnimating()
        cell?.accessoryView = spinner
        cell?.accessibilityLabel = "\(track.title ?? "Track"), loading"
        self.busyIndicator.incrementBusyCount()

        // Stream straight from the catalog and start as soon as the player is ready;
        // a copy lands in the file cache afterward so the next play is offline.
        let cachedPath = DPFileCache.path(forKey: track.source.cacheKey) ?? ""
        let isCached = !cachedPath.isEmpty && FileManager.default.fileExists(atPath: cachedPath)
        let sourceURL: URL = isCached ? URL(fileURLWithPath: cachedPath) : track.source.uri
        let item = AVPlayerItem(url: sourceURL)
        let player = AVPlayer(playerItem: item)

        var observation: NSKeyValueObservation?
        var finished = false
        let finish: (Bool) -> Void = { [weak self, weak cell] ready in
            guard !finished else { return }
            finished = true
            spinner.stopAnimating()
            observation?.invalidate()
            observation = nil
            guard let self else { return }
            self.busyIndicator.decrementBusyCount()
            if cell?.accessoryView === spinner {
                cell?.accessoryView = nil
                cell?.accessibilityLabel = nil
            }
            guard ready else {
                player.replaceCurrentItem(with: nil)
                self.tm_showError("The learning track couldn't be played. Check your connection and try again.") { [weak self] in
                    self?.tableView(tableView, didSelectRowAt: indexPath)
                }
                return
            }
            let playerController = AVPlayerViewController()
            playerController.showsPlaybackControls = true
            playerController.player = player
            self.present(playerController, animated: true) {
                player.play()
            }
            if !isCached {
                DPTagTracksController.cache(track: track)
            }
        }
        observation = item.observe(\.status, options: [.initial, .new]) { item, _ in
            DispatchQueue.main.async {
                switch item.status {
                case .readyToPlay: finish(true)
                case .failed: finish(false)
                default: break
                }
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + DPTagTracksController.readyTimeout) {
            finish(false)
        }
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
