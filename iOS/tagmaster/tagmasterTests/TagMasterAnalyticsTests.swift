//
//  TagMasterAnalyticsTests.swift
//  tagmasterTests
//
//  What Tag Master reports (docs/analytics.md) and where it offers a review:
//  the real models and shell, with Google Analytics and the store replaced.
//  The rules themselves are tested in ReviewPromptTests.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class TagMasterAnalyticsTests: TMBehaviorTestCase {
    private var analytics: AnalyticsRecorder!
    private var previousPrompt: ReviewPrompt!
    private var previousTracker: ScreenTracker!
    private var prompt: ReviewPrompt!
    private var reviewDefaults: UserDefaults!
    private let reviewSuite = "TagMasterAnalyticsTests.review"

    nonisolated override func setUp() {
        super.setUp()
        MainActor.assumeIsolated {
            analytics = AnalyticsRecorder()
            previousTracker = ScreenTracker.shared
            ScreenTracker.shared = ScreenTracker()
            previousPrompt = ReviewPrompt.shared
            prompt = ReviewPrompt()
            reviewDefaults = UserDefaults(suiteName: reviewSuite)
            reviewDefaults.removePersistentDomain(forName: reviewSuite)
            prompt.install(defaults: reviewDefaults)
            prompt.after = { _, _ in }
            prompt.presentStoreReview = { _ in XCTFail("no store in tests") }
            ReviewPrompt.shared = prompt
        }
    }

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            prompt.reset()
            ReviewPrompt.shared = previousPrompt
            ScreenTracker.shared = previousTracker
            reviewDefaults.removePersistentDomain(forName: reviewSuite)
            analytics.stop()
        }
        super.tearDown()
    }

    private var names: [String] { analytics.events.map(\.name).filter { $0 != "screen_view" } }

    private func loadedModel(_ id: Int32 = 1809) -> (TagDetailModel, TMControlledTagLoader) {
        let loader = TMControlledTagLoader()
        let model = TagDetailModel(loader: loader)
        model.show(tagId: id)
        let tag = DPTag()
        tag.tagId = id
        tag.title = "Lost"
        tag.writtenKey = "Bb"
        loader.finish(0, with: tag)
        return (model, loader)
    }

    // MARK: Tags

    func testOpeningATagIsReportedAndIsADayOfUseButARefreshIsNot() {
        let (model, loader) = loadedModel()
        XCTAssertEqual(names, ["tag_viewed"])
        XCTAssertEqual(prompt.policy?.activeDays, 1)
        model.refresh()
        loader.finish(1, with: model.tag)
        XCTAssertEqual(names, ["tag_viewed"])
    }

    func testAFailedLoadIsNotATagViewed() {
        let loader = TMControlledTagLoader()
        let model = TagDetailModel(loader: loader)
        model.show(tagId: 1809)
        loader.finish(0, with: nil)
        XCTAssertTrue(names.isEmpty)
    }

    func testTheKeyNoteIsAPitchPlayedFromWhereItIsShown() {
        let (model, _) = loadedModel()
        analytics.clear()
        model.summary.pressKey()
        model.summary.releaseKey()
        model.summary.pressKey(from: .sheetMusic)
        model.summary.releaseKey()
        XCTAssertEqual(analytics.events.map(\.parameters), [["source": "tag"], ["source": "sheet_music"]])
        XCTAssertEqual(reviewDefaults.double(forKey: "review.lastSoundAt"), Date().timeIntervalSince1970, accuracy: 5)
    }

    func testTrackTitlesNameTheirPart() {
        XCTAssertEqual(["All Parts", "Tenor", "Lead", "Baritone", "Bass", "Other 1", "Other 4", nil].map(TagMasterUsage.part(forTrackTitle:)),
                       ["all", "tenor", "lead", "baritone", "bass", "other", "other", "other"])
    }

    func testEachPageIsItsOwnScreen() {
        let model = TagDetailModel(loader: TMControlledTagLoader())
        XCTAssertNil(model.screenName, "no tag, no screen")
        model.show(tagId: 1809)
        XCTAssertEqual(TagDetailModel.Page.allCases.map {
            model.selectedPage = $0
            return model.screenName ?? ""
        }, ["tag_summary", "tag_details", "tag_tracks", "tag_videos"])
    }

    // MARK: Lists

    func testAddingToFavoritesOrTeachableIsReportedAndIsAFinishedTask() {
        let (model, _) = loadedModel()
        analytics.clear()
        model.toggleFavorite()
        XCTAssertTrue(prompt.hasFreshTaskForTesting)
        model.toggleFavorite()
        model.toggleTeachable()
        XCTAssertEqual(analytics.events, [.init(name: "tag_added_to_list", parameters: ["list": "favorites"]),
                                          .init(name: "tag_added_to_list", parameters: ["list": "teachable"])],
                       "taking a tag off is not reported")
    }

    func testThePickerReportsAddsAndNewLists() throws {
        seedLists(lists: [(key: "set", name: "Set", ids: [])])
        let picker = TMListPickerModel(tagId: 1809)
        picker.toggle("set")
        picker.toggle("set")
        picker.toggle(TMTagLists.favoriteKey)
        picker.createList(named: "Afterglow")
        XCTAssertEqual(analytics.events, [
            .init(name: "tag_added_to_list", parameters: ["list": "custom"]),
            .init(name: "tag_added_to_list", parameters: ["list": "favorites"]),
            .init(name: "tag_list_created", parameters: [:]),
            .init(name: "tag_added_to_list", parameters: ["list": "custom"]),
        ])
    }

    func testAListDeletedElsewhereIsNotAddedToFromAStalePickerRow() {
        seedLists(lists: [(key: "set", name: "Set", ids: [])])
        let picker = TMListPickerModel(tagId: 1809)
        // Deleted on another device while the picker was open.
        TMTagLists.deleteList("set")
        picker.toggle("set")
        XCTAssertTrue(analytics.events.isEmpty)
        XCTAssertFalse(TMTagLists.allKeys().contains("set"), "the deleted list does not come back")
        XCTAssertFalse(prompt.hasFreshTaskForTesting)
    }

    func testCreatingAListFromHomeIsReportedOnlyWhenItIsMade() {
        let home = TMHomeModel(catalog: TMFixtureCatalog(available: 1).catalog, navigator: RecordingNavigator())
        home.commitNewList("Afterglow")
        home.commitNewList("Favorites")
        XCTAssertEqual(names, ["tag_list_created"], "a rejected name makes no list")
        XCTAssertTrue(prompt.hasFreshTaskForTesting)
    }

    // MARK: Screens and the calm one

    func testTheShellReportsHomeAndTagsAndOnlyHomeIsCalm() {
        seedCachedTag()
        let router = TMRouter()
        mountShell(router)
        settle()
        XCTAssertTrue(prompt.hasCalmScreenForTesting, "Home is calm")
        // Each step settles, so its screen reports before the next replaces it, and then waits
        // for Home to join or leave the calm list, which follows the push or pop finishing.
        router.showTag(1809, source: nil)
        spinUntil("the tag loads") { router.path.last?.tagModel?.tag != nil }
        settle()
        spinUntil("a tag is never calm") { !prompt.hasCalmScreenForTesting }
        router.path.last?.tagModel?.selectedPage = .tracks
        settle()
        router.path.removeAll()
        settle()
        spinUntil("Home is calm again") { prompt.hasCalmScreenForTesting }
        router.show(.search)
        settle()
        spinUntil("Search covers Home") { !prompt.hasCalmScreenForTesting }
        ScreenCatalog.settle(0.2)
        XCTAssertEqual(analytics.screens, ["home", "tag_summary", "tag_tracks", "home", "search"])
        XCTAssertEqual(names, ["tag_viewed"])
    }

    func testHomeBesideAnOpenTagIsNotCalm() throws {
        seedCachedTag()
        let router = TMRouter()
        mountShell(router, split: true)
        settle()
        guard router.expanded else { throw XCTSkip("Only an iPad shows a tag beside Home") }
        XCTAssertTrue(prompt.hasCalmScreenForTesting)
        router.showTag(1809, source: nil)
        settle()
        XCTAssertFalse(prompt.hasCalmScreenForTesting, "the tag beside Home may be sung from")
        router.setExpanded(false)
        settle()
        XCTAssertTrue(prompt.hasCalmScreenForTesting, "a collapsed split shows no tag beside Home")
    }
}
