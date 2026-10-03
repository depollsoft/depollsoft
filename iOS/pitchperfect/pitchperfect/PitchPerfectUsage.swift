//
//  PitchPerfectUsage.swift
//  pitchperfect
//
//  What Pitch Perfect reports to analytics and counts toward a review prompt
//  (docs/analytics.md, with the same names as Android), one function per
//  event so every screen reports it the same way.
//

import FirebaseAuth
import Foundation

/// Where a note was started from: `pitch_played`'s `source`.
enum PitchSource: String {
    case pitchPipe = "pitch_pipe"
    case classicPitchPipe = "classic_pitch_pipe"
    case notes, keys, song, widget
}

@MainActor
enum PitchPerfectUsage {
    /// Someone started a note: a day of use, and a sound.
    static func pitchPlayed(_ source: PitchSource) {
        UsageAnalytics.event(UsageAnalytics.pitchPlayed, [UsageAnalytics.source: source.rawValue])
        ReviewPrompt.shared.recordUse()
        ReviewPrompt.shared.recordSound()
    }

    /// The app made a sound that isn't a pitch played (Settings' sound preview).
    static func soundPlayed() {
        ReviewPrompt.shared.recordSound()
    }

    /// A note stopped sounding. The quiet a review waits for runs from the end of
    /// a sound, so a note held (or toggled on) for minutes still counts as recent.
    static func soundStopped() {
        ReviewPrompt.shared.recordSound()
    }

    static func songAdded() {
        UsageAnalytics.event("song_added")
        ReviewPrompt.shared.taskFinished()
    }

    static func setListCreated() {
        UsageAnalytics.event("set_list_created")
        ReviewPrompt.shared.taskFinished()
    }

    static func songsAddedToSetList() {
        UsageAnalytics.event("songs_added_to_set_list")
        ReviewPrompt.shared.taskFinished()
    }

    private static var reportedSettings: [String: String] = [:]

    /// The settings as user properties, each sent when it differs from what was last sent.
    static func reportSettings(_ settings: DPSettingsModel = .sharedInstance) {
        let current = [
            "pitch_pipe_style": settings.classicPitchPipe ? "classic" : "radial",
            "note_sound": settings.noteSound,
            "reference_pitch": String(settings.referencePitch),
        ]
        for (name, value) in current where reportedSettings[name] != value {
            UsageAnalytics.userProperty(name, value)
        }
        reportedSettings = current
    }

    /// Whether any note or widget cell is sounding: no review is asked for then.
    static var isSounding: Bool {
        (DPNote.commonNotes() as? [DPNote] ?? []).contains { $0.isPlaying }
            || !WidgetTonePlayer.shared.activePitches.isEmpty
    }

    private static var settingsObserver: NSObjectProtocol?

    /// Launch: sends analytics to Firebase and starts the review counts. The
    /// unit-test host never calls this, so tests send nothing and never ask.
    static func start() {
        UsageAnalytics.connectToFirebase()
        ReviewPrompt.shared.install()
        ReviewPrompt.shared.isBusy = { isSounding }
        WidgetInstrumentHook.pitchStarted = { pitchPlayed(.widget) }
        WidgetInstrumentHook.pitchStopped = { soundStopped() }
        // Allowed after launch (the first launch's Privacy choices): send what was held back.
        TelemetryConsent.analyticsAllowed = {
            MainActor.assumeIsolated {
                reportedSettings = [:]
                reportSettings()
                UsageAnalytics.signedIn(Auth.auth().currentUser != nil)
            }
        }
        reportSettings()
        settingsObserver = NotificationCenter.default.addObserver(
            forName: .settingsChanged, object: nil, queue: .main
        ) { _ in
            MainActor.assumeIsolated { reportSettings() }
        }
    }

    /// Forgets which settings were sent; for tests.
    static func resetForTesting() {
        reportedSettings = [:]
    }
}
