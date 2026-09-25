//
//  NotePlayer.swift
//  pitchperfect
//
//  Every screen sounds its notes through here. DPNote keeps whether it is
//  playing as a plain property; this makes that observable, so a row lights
//  while its note sounds and goes dark wherever the note is stopped.
//

import Foundation
import Observation

@Observable
@MainActor
final class NotePlayer {
    static let shared = NotePlayer()

    /// Bumped on every start and stop, so views reading `isPlaying` redraw.
    private(set) var revision = 0

    /// Whether a press starts a note that sounds until pressed again. Reads the
    /// setting each time, so a change in Settings applies at once.
    var toggleNotes: () -> Bool = { DPSettingsModel.sharedInstance.toggleNotes }

    func isPlaying(_ note: DPNote) -> Bool {
        _ = revision
        return note.isPlaying
    }

    func play(_ note: DPNote) {
        note.play()
        revision += 1
    }

    func stop(_ note: DPNote) {
        note.stop()
        revision += 1
    }

    func stop(_ notes: [DPNote]) {
        notes.forEach { $0.stop() }
        revision += 1
    }

    /// A finger landed on a row: momentary notes start; toggled ones flip.
    func pressBegan(_ note: DPNote) {
        if toggleNotes(), note.isPlaying {
            stop(note)
        } else {
            play(note)
        }
    }

    /// The finger lifted or slid away: only momentary notes stop.
    func pressEnded(_ note: DPNote) {
        guard !toggleNotes() else { return }
        stop(note)
    }
}
