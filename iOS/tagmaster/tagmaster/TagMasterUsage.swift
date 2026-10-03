//
//  TagMasterUsage.swift
//  tagmaster
//
//  What Tag Master reports to analytics and counts toward a review prompt
//  (docs/analytics.md, with the same names as Android), one function per
//  event so every screen reports it the same way.
//

import FirebaseAuth
import Foundation

@MainActor
enum TagMasterUsage {
    /// Where a key note was played from: `pitch_played`'s `source`.
    enum PitchSource: String {
        case tag
        case sheetMusic = "sheet_music"
    }

    /// A tag's details loaded: a day of use.
    static func tagViewed() {
        UsageAnalytics.event("tag_viewed")
        ReviewPrompt.shared.recordUse()
    }

    static func pitchPlayed(_ source: PitchSource) {
        UsageAnalytics.event(UsageAnalytics.pitchPlayed, [UsageAnalytics.source: source.rawValue])
        ReviewPrompt.shared.recordSound()
    }

    /// A learning track started, named by its row title ("All Parts", "Tenor", "Other 2"...).
    static func learningTrackPlayed(title: String?) {
        UsageAnalytics.event("learning_track_played", ["part": part(forTrackTitle: title)])
        ReviewPrompt.shared.recordSound()
    }

    /// A key note or learning track stopped. The quiet a review waits for runs
    /// from the end of a sound, so a track played for minutes still counts as recent.
    static func soundStopped() {
        ReviewPrompt.shared.recordSound()
    }

    static func videoOpened() {
        UsageAnalytics.event("video_opened")
        ReviewPrompt.shared.recordSound()
    }

    /// Someone put a tag on the list `key`.
    static func tagAddedToList(_ key: String) {
        UsageAnalytics.event("tag_added_to_list", ["list": listKind(key)])
        ReviewPrompt.shared.taskFinished()
    }

    static func tagListCreated() {
        UsageAnalytics.event("tag_list_created")
        ReviewPrompt.shared.taskFinished()
    }

    /// `part` for a track title: the four parts, all parts, or any other track.
    nonisolated static func part(forTrackTitle title: String?) -> String {
        switch title {
        case "All Parts": "all"
        case "Tenor": "tenor"
        case "Lead": "lead"
        case "Baritone": "baritone"
        case "Bass": "bass"
        default: "other"
        }
    }

    /// `list` for a list key: favorites, teachable, or one of the person's own.
    nonisolated static func listKind(_ key: String) -> String {
        switch key {
        case TMTagLists.favoriteKey: "favorites"
        case TMTagLists.teachableKey: "teachable"
        default: "custom"
        }
    }

    /// Whether a key note or a learning track is sounding: no review is asked for then.
    static var isSounding: Bool {
        (DPNote.commonNotes() as? [DPNote] ?? []).contains { $0.isPlaying } || TMTrackPlayerModel.anyPlaying
    }

    /// Launch: sends analytics to Firebase and starts the review counts. The
    /// unit-test host never calls this, so tests send nothing and never ask.
    static func start() {
        UsageAnalytics.connectToFirebase()
        ReviewPrompt.shared.install()
        ReviewPrompt.shared.isBusy = { isSounding }
        // Allowed after launch (the first launch's Privacy choices): send what was held back.
        TelemetryConsent.analyticsAllowed = {
            MainActor.assumeIsolated { UsageAnalytics.signedIn(Auth.auth().currentUser != nil) }
        }
    }
}
