//
//  DPTagTracksController.swift
//  tagmaster
//
//  Created by David Poll on 8/1/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import AVKit

final class TMTrackPlayerController: AVPlayerViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector: #selector(stopPlayback),
                                               name: UIApplication.willResignActiveNotification, object: nil)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        stopPlayback()
    }

    @objc func stopPlayback() { player?.pause() }
}

extension DPTagTracksController: UITableViewDelegate {
    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row < self.tag.tracks.count else { return }
        let track = self.tag.tracks[indexPath.row]
        // Choosing a part is a real selection: confirm it in the hand before
        // the download finishes.
        TMTheme.selected()
        self.busyIndicator.incrementBusyCount()
        DispatchQueue.global().async {
            let tempFile = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(track.source.cacheKey)
            do {
                try Data(contentsOf: track.source.uri).write(to: tempFile)
                DispatchQueue.main.async {
                    self.busyIndicator.decrementBusyCount()
                    guard self.viewIfLoaded?.window != nil else { return }
                    let player = AVPlayer(url: tempFile)
                    let playerController = TMTrackPlayerController()
                    playerController.showsPlaybackControls = true
                    playerController.player = player
                    self.present(playerController, animated: true) {
                        player.play()
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.busyIndicator.decrementBusyCount()
                    self.reportTrackFailure(title: track.title ?? "That track")
                }
            }
        }
    }

    private func reportTrackFailure(title: String) {
        let alert = UIAlertController(
            title: "Track unavailable",
            message: "\(title) could not be downloaded from BarbershopTags.com. "
                   + "Check your connection and try again.",
            preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
