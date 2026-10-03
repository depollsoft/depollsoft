//
//  PitchPerfectAnalyticsTests.swift
//  pitchperfectTests
//
//  What Pitch Perfect reports (docs/analytics.md) and where it offers a review:
//  the real models and screens, with Google Analytics and the store replaced.
//  The rules themselves are shared code, tested in Tag Master's ReviewPromptTests.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// Records what would have gone to Google Analytics.
@MainActor
private final class AnalyticsRecorder {
    private(set) var events: [(name: String, parameters: [String: String])] = []
    private(set) var properties: [(name: String, value: String)] = []

    init() {
        UsageAnalytics.sink = UsageAnalytics.Sink(
            logEvent: { [unowned self] name, parameters in events.append((name, parameters)) },
            setUserProperty: { [unowned self] name, value in properties.append((name, value)) })
    }

    func stop() { UsageAnalytics.sink = .nowhere }

    var names: [String] { events.map(\.name).filter { $0 != "screen_view" } }
    var sources: [String] { events.filter { $0.name == "pitch_played" }.compactMap { $0.parameters["source"] } }
    var screens: [String] { events.filter { $0.name == "screen_view" }.compactMap { $0.parameters["screen_name"] } }
    func clear() { events = []; properties = [] }
}

@MainActor
final class PitchPerfectAnalyticsTests: PitchPerfectTestCase {
    private var analytics: AnalyticsRecorder!
    private var previousPrompt: ReviewPrompt!
    private var previousTracker: ScreenTracker!
    private var prompt: ReviewPrompt!
    private var reviewDefaults: UserDefaults!
    private let reviewSuite = "PitchPerfectAnalyticsTests.review"
    private var uptime: TimeInterval = 100

    override func setUp() async throws {
        try await super.setUp()
        analytics = AnalyticsRecorder()
        previousTracker = ScreenTracker.shared
        ScreenTracker.shared = ScreenTracker()
        previousPrompt = ReviewPrompt.shared
        prompt = ReviewPrompt()
        reviewDefaults = UserDefaults(suiteName: reviewSuite)
        reviewDefaults.removePersistentDomain(forName: reviewSuite)
        prompt.install(defaults: reviewDefaults)
        prompt.uptime = { [unowned self] in uptime }
        prompt.after = { _, _ in }
        prompt.presentStoreReview = { _ in XCTFail("no store in tests") }
        ReviewPrompt.shared = prompt
        PitchPerfectUsage.resetForTesting()
    }

    override func tearDown() async throws {
        prompt.reset()
        ReviewPrompt.shared = previousPrompt
        ScreenTracker.shared = previousTracker
        reviewDefaults.removePersistentDomain(forName: reviewSuite)
        analytics.stop()
        try await super.tearDown()
    }

    /// A day of use is counted (the review policy saw a pitch played).
    private var daysOfUse: Int { prompt.policy?.activeDays ?? 0 }

    // MARK: Pitches

    func testAPressOnThePitchPipeIsReportedOnceAndSlidingIsNot() {
        let model = PitchPipeModel()
        model.geometry = InstrumentGeometry(size: CGSize(width: 402, height: 640), count: model.notes.count)
        model.touchBegan(id: 1, at: model.geometry.cellCenters[0])
        model.touchMoved(id: 1, to: model.geometry.cellCenters[1])
        model.touchMoved(id: 1, to: model.geometry.cellCenters[2])
        model.touchEnded(id: 1)
        model.activate(cell: 4)
        model.stopAll()
        XCTAssertEqual(analytics.sources, ["pitch_pipe", "pitch_pipe"])
        XCTAssertEqual(daysOfUse, 1)
    }

    func testTheClassicGridReportsItsOwnSource() {
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        let model = PitchPipeModel()
        model.classicGeometry = ClassicGeometry(size: CGSize(width: 402, height: 640), count: model.notes.count)
        let slot = model.classicGeometry.slots[0]
        model.touchBegan(id: 1, at: CGPoint(x: slot.midX, y: slot.midY))
        model.touchEnded(id: 1)
        XCTAssertEqual(analytics.sources, ["classic_pitch_pipe"])
    }

    func testStoppingAToggledNoteIsNotAPitchPlayed() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let model = PitchPipeModel()
        model.geometry = InstrumentGeometry(size: CGSize(width: 402, height: 640), count: model.notes.count)
        model.touchBegan(id: 1, at: model.geometry.cellCenters[0])
        model.touchEnded(id: 1)
        model.touchBegan(id: 2, at: model.geometry.cellCenters[0])
        model.touchEnded(id: 2)
        XCTAssertEqual(analytics.sources, ["pitch_pipe"])
        XCTAssertFalse(model.anyPlaying)
    }

    func testRowsReportTheirScreen() throws {
        let note = try XCTUnwrap(PitchPipeModel().notes.first)
        NotePlayer.shared.pressBegan(note, source: .notes)
        NotePlayer.shared.pressEnded(note)
        NotePlayer.shared.activate(note, source: .keys) { _, work in work() }
        seedSongs(["Blue Skies"])
        let songs = SongListModel()
        let song = try XCTUnwrap(songs.songs.first)
        songs.press(song)
        songs.release(song)
        NotePlayer.shared.pressBegan(note)
        NotePlayer.shared.pressEnded(note)
        XCTAssertEqual(analytics.sources, ["notes", "keys", "song"], "a press with no source (Settings' preview) is not reported")
    }

    func testANoteHeldForMinutesIsRecentWhenItStops() throws {
        let note = try XCTUnwrap((DPNote.commonNotes() as? [DPNote])?.first)
        NotePlayer.shared.play(note)
        // As if it had been sounding for ten minutes.
        reviewDefaults.set(Date().timeIntervalSince1970 - 600, forKey: "review.lastSoundAt")
        NotePlayer.shared.stop(note)
        XCTAssertEqual(reviewDefaults.double(forKey: "review.lastSoundAt"), Date().timeIntervalSince1970, accuracy: 5,
                       "the quiet a review waits for starts when the note stops")
    }

    func testASoundKeepsTheReviewAwayForFiveMinutes() {
        let model = PitchPipeModel()
        model.activate(cell: 0)
        model.stopAll()
        let stored = reviewDefaults.double(forKey: "review.lastSoundAt")
        XCTAssertEqual(stored, Date().timeIntervalSince1970, accuracy: 5)
    }

    // MARK: Tasks

    func testANewSongIsReportedAndIsAFinishedTask() throws {
        let songs = SongListModel()
        songs.addSong()
        let request = try XCTUnwrap(songs.editor)
        request.song.name = "Blue Skies"
        request.completion(true)
        songs.editSong(request.song)
        try XCTUnwrap(songs.editor).completion(true)
        XCTAssertEqual(analytics.names, ["song_added"], "an edit is not a new song")
        XCTAssertTrue(prompt.hasFreshTaskForTesting)
    }

    func testCancellingANewSongIsNotATask() throws {
        let songs = SongListModel()
        songs.addSong()
        try XCTUnwrap(songs.editor).completion(false)
        XCTAssertTrue(analytics.names.isEmpty)
        XCTAssertFalse(prompt.hasFreshTaskForTesting)
    }

    func testCreatingASetListIsReportedFromEitherScreen() {
        let songs = SongListModel()
        songs.promptNewSetList()
        songs.prompts.name = "Saturday show"
        songs.prompts.confirmName()
        let lists = SetListsModel()
        lists.promptCreate()
        lists.prompts.name = "Sunday show"
        lists.prompts.confirmName()
        lists.promptRename(DPSongsModel.sharedInstance.currentList)
        lists.prompts.name = "Sunday matinee"
        lists.prompts.confirmName()
        XCTAssertEqual(analytics.names, ["set_list_created", "set_list_created"], "a rename is not a new set list")
        XCTAssertTrue(prompt.hasFreshTaskForTesting)
    }

    func testAddingSongsFromAnotherSetListIsReported() throws {
        seedSongs(["Blue Skies", "Shenandoah"])
        let target = try customList(named: "Saturday show")
        let model = AddSongsModel(target: target)
        XCTAssertEqual(model.confirm(), 0)
        XCTAssertTrue(analytics.names.isEmpty, "nothing chosen, nothing added")
        model.toggleSelectAll()
        XCTAssertEqual(model.confirm(), 2)
        XCTAssertEqual(analytics.names, ["songs_added_to_set_list"])
    }

    func testAddingToASetListDeletedMeanwhileIsNotReported() throws {
        seedSongs(["Blue Skies"])
        let target = try customList(named: "Saturday show")
        let model = AddSongsModel(target: target)
        model.toggleSelectAll()
        // Deleted on another device while Add songs was open.
        XCTAssertTrue(DPSongsModel.sharedInstance.deleteList(target))
        model.confirm()
        XCTAssertTrue(analytics.names.isEmpty)
        XCTAssertFalse(prompt.hasFreshTaskForTesting)
    }

    // MARK: Settings

    func testSettingsAreUserPropertiesSentWhenTheyChange() {
        PitchPerfectUsage.reportSettings()
        XCTAssertEqual(Dictionary(uniqueKeysWithValues: analytics.properties.map { ($0.name, $0.value) }),
                       ["pitch_pipe_style": "radial", "note_sound": "pitchPipe", "reference_pitch": "440"])
        analytics.clear()
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        DPSettingsModel.sharedInstance.referencePitch = 442
        PitchPerfectUsage.reportSettings()
        PitchPerfectUsage.reportSettings()
        XCTAssertEqual(analytics.properties.map(\.name).sorted(), ["pitch_pipe_style", "reference_pitch"])
        XCTAssertEqual(analytics.properties.first { $0.name == "pitch_pipe_style" }?.value, "classic")
        XCTAssertEqual(analytics.properties.first { $0.name == "reference_pitch" }?.value, "442")
    }

    // MARK: Screens

    func testTabsAndSheetsAreReportedAsTheyComeToTheFront() throws {
        let app = try launch()
        ScreenCatalog.settle()
        app.show(tab: 3)
        app.show(tab: 1)
        app.show(tab: 3)
        app.songs.addSong()
        ScreenCatalog.settle(0.6)
        app.songs.editor?.completion(false)
        ScreenCatalog.settle(0.8)
        XCTAssertEqual(analytics.screens, ["pitch_pipe", "songs", "notes", "songs", "song_editor", "songs"])
    }

    func testSetListsAndSettingsAreScreens() throws {
        let app = try launch()
        app.show(tab: 3)
        app.songs.manageSetLists()
        settle { app.navigationTitles.contains("Set Lists") }
        ScreenCatalog.settle(0.2)
        app.songs.showingManage = false
        settle { !app.navigationTitles.contains("Set Lists") }
        ScreenCatalog.settle(0.2)
        app.songs.showingSettings = true
        settle { app.navigationTitles.contains("Settings") }
        ScreenCatalog.settle(0.2)
        XCTAssertEqual(Array(analytics.screens.suffix(4)), ["songs", "set_lists", "songs", "settings"])
        app.resetSettings()
    }

    // MARK: Where a review may be asked for

    func testOnlyTheSongsTabIsCalm() throws {
        let app = try launch()
        prompt.taskFinished()
        XCTAssertFalse(prompt.hasCalmScreenForTesting, "the pitch pipe is never calm")
        app.show(tab: 3)
        XCTAssertTrue(prompt.hasCalmScreenForTesting)
        app.show(tab: 2)
        XCTAssertFalse(prompt.hasCalmScreenForTesting)
    }
}
