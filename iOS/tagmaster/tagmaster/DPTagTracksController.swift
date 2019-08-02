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
    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let track = self.tag.tracks[indexPath.row]
        
        DispatchQueue.global().async {
            let tempFile = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(track.source.cacheKey)
            do {
                try Data(contentsOf: track.source.uri).write(to: tempFile)
                DispatchQueue.main.async {
                    let player = AVPlayer(url: tempFile)
                    let playerController = AVPlayerViewController()
                    playerController.showsPlaybackControls = true
                    playerController.player = player
                    self.present(playerController, animated: true) {
                        player.play()
                    }
                }
            } catch {
                
            }
        }
    }
}
