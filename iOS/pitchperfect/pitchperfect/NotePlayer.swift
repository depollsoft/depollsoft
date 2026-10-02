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

    /// The A4 the notes are tuned to, so views showing a frequency redraw when
    /// Settings changes it.
    private(set) var referencePitch = DPNote.referencePitch
    @ObservationIgnored private var settingsObserver: NSObjectProtocol?
    @ObservationIgnored private var noteObserver: NSObjectProtocol?

    init() {
        settingsObserver = NotificationCenter.default.addObserver(
            forName: .settingsChanged, object: nil, queue: .main
        ) { [weak self] _ in
            // Every settings change resyncs (an Observable set notifies even when the
            // value is equal), so a tuning applied without passing through here, as
            // at launch, can never leave a stale copy.
            MainActor.assumeIsolated { self?.referencePitch = DPNote.referencePitch }
        }
        // A note that stops on its own (another note took its instrument's
        // sampler) or falls back to the pitch pipe voice redraws its row.
        noteObserver = NotificationCenter.default.addObserver(
            forName: .DPNotePlayingDidChange, object: nil, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.revision += 1 }
        }
    }

    /// The frequency `note` sounds at, at the chosen tuning.
    func frequency(of note: DPNote) -> Double {
        _ = referencePitch
        return note.tunedFrequency
    }

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

    /// Reports a note started from `source` (docs/analytics.md); tests replace it.
    var reportPlayed: (PitchSource) -> Void = { PitchPerfectUsage.pitchPlayed($0) }

    /// A finger landed on a row: momentary notes start; toggled ones flip.
    /// A note it starts is reported as played from `source`.
    func pressBegan(_ note: DPNote, source: PitchSource? = nil) {
        if toggleNotes(), note.isPlaying {
            stop(note)
        } else {
            play(note)
            if let source { reportPlayed(source) }
        }
    }

    /// The finger lifted (or the press was cancelled by a scroll): only
    /// momentary notes stop.
    func pressEnded(_ note: DPNote) {
        guard !toggleNotes() else { return }
        stop(note)
    }

    /// How long a VoiceOver activation sounds a momentary note, as on the pitch pipe.
    static let activationDuration: TimeInterval = 1.5

    /// A VoiceOver double-tap: toggles with Toggle Notes, otherwise sounds the
    /// note for `activationDuration`. (A synthesized touch would start and stop
    /// it in the same instant.)
    func activate(_ note: DPNote, source: PitchSource? = nil,
                  after delay: @escaping (TimeInterval, @escaping () -> Void) -> Void = NotePlayer.later) {
        if toggleNotes() {
            pressBegan(note, source: source)
            return
        }
        play(note)
        if let source { reportPlayed(source) }
        delay(Self.activationDuration) { [weak self] in self?.stop(note) }
    }

    static func later(_ seconds: TimeInterval, _ work: @escaping () -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: work)
    }
}
