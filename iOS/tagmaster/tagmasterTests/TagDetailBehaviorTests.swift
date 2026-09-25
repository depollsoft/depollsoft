//
//  TagDetailBehaviorTests.swift
//  tagmasterTests
//
//  The SwiftUI tag detail mounted the way the app mounts it, driven through its
//  accessibility tree (UIDriver) the way VoiceOver and the UI tests reach it:
//  what each page says, which controls the bar offers, and that each control
//  does its job. Tags come from the production cache (seeded) or, when missing,
//  from the real catalog path with the network blocked.
//

import SafariServices
import SwiftUI
import XCTest
import UIKit
@testable import tagmaster

@MainActor
final class TagDetailBehaviorTests: TMBehaviorTestCase {
    private var shownLists: [String] = []

    /// Mounts the detail in a navigation stack, as the app pushes it, and waits for the tag.
    private func mountDetail(_ tagId: Int32 = 1809, wait: Bool = true) -> TagDetailViewController {
        let detail = TagDetailViewController()
        detail.tagId = tagId
        mountInNavigation(detail)
        if wait { spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending } }
        ScreenCatalog.settle(0.3)
        return detail
    }

    private func driver() -> UIDriver { UIDriver(window) }

    private func select(_ page: TagDetailModel.Page, in detail: TagDetailViewController) {
        detail.model.selectedPage = page
        ScreenCatalog.settle(0.4)
    }

    // MARK: - Reaching the detail

    func testTheDetailLeavesItsLoadingStateAndOffersItsFourPages() throws {
        seedCachedTag(id: 1809, title: "Lost")
        let detail = mountDetail()
        XCTAssertEqual(detail.tagId, 1809)
        XCTAssertEqual(detail.model.tag?.title, "Lost")
        XCTAssertEqual(detail.navigationItem.title, "Lost")
        XCTAssertFalse(driver().exists(id: "tag.initialLoading"), "The pages replace the loading state")
        let bar = try XCTUnwrap(firstDescendant(of: detail.view) {
            ($0 as? UITabBar)?.accessibilityIdentifier == "page-tab-bar"
        } as? UITabBar)
        XCTAssertEqual(bar.items?.map { $0.title ?? "" }, ["Summary", "Details", "Tracks", "Videos"])
        XCTAssertEqual(bar.items?.map { $0.accessibilityIdentifier ?? "" },
                       ["page-Summary", "page-Details", "page-Tracks", "page-Videos"])
        XCTAssertEqual(bar.selectedItem?.title, "Summary")
    }

    func testAnUnavailableTagSaysSoInsteadOfShowingEmptyPages() {
        // Nothing cached and the catalog blocked: the real load path runs and fails at once.
        let detail = mountDetail(999_999)
        XCTAssertFalse(network.attemptedURLs.isEmpty,
                       "The failure came from the app's own catalog request, not from a short-circuit")
        XCTAssertEqual(Set(network.attemptedURLs.compactMap(\.host)), ["www.barbershoptags.com"])
        XCTAssertNil(detail.model.tag)
        XCTAssertEqual(driver().label(id: "tag.loadingStatus"),
                       "Couldn't load tag 999999. Check your connection and tag ID, then try again.")
        XCTAssertTrue(driver().exists(label: "Retry"), "Retry stays available")
        XCTAssertFalse(driver().exists(label: "Share"), "There is nothing to share without a tag")
        XCTAssertNil(firstDescendant(of: detail.view) { $0 is UITabBar }, "No empty pages")
    }

    func testRetryingAnUnavailableTagRunsTheLoadAgain() {
        let detail = mountDetail(999_998)
        let attempts = network.attemptedURLs.count
        driver().tap(label: "Retry")
        spinUntil("the retry resolves", timeout: 5) { !detail.model.fetchPending }
        XCTAssertGreaterThan(network.attemptedURLs.count, attempts)
        XCTAssertTrue(detail.model.loadFailed)
    }

    // MARK: - The bar

    func testOnIPhoneTheBarOffersRefreshTagActionsAndShare() {
        seedCachedTag()
        _ = mountDetail()
        let driver = driver()
        for label in ["Refresh", "Favorite and Teachable options", "Share"] {
            XCTAssertTrue(driver.exists(label: label), "\(label) is in the bar")
        }
        for label in ["Previous tag", "Next tag", "Add Favorite", "Mark as Teachable"] {
            XCTAssertFalse(driver.exists(label: label), "\(label) belongs beside a list")
        }
    }

    func testTheTagActionsButtonPresentsTheActionSheet() throws {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        driver().tap(label: "Favorite and Teachable options")
        spinUntil("the sheet is up") { detail.presentedViewController is UIAlertController }
        let sheet = try XCTUnwrap(detail.presentedViewController as? UIAlertController)
        XCTAssertEqual(sheet.preferredStyle, .actionSheet)
        XCTAssertEqual(sheet.actions.map { $0.title ?? "" },
                       ["Add Favorite", "Mark as Teachable", "Add to List…", "Cancel"])
        XCTAssertEqual(sheet.actions.last?.style, .cancel)
        sheet.tm_fire("Add Favorite")
        spinUntil("the sheet goes") { detail.presentedViewController == nil }
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])
        XCTAssertFalse(detail.model.actionsPresented)
    }

    func testBesideAListTheBarCarriesTheStepperTogglesAndAddToList() throws {
        // The expanded bar is an iPad layout; on iPhone the model tests cover the same rules.
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "Beside-a-list layout is iPad only")
        seedCachedTag(id: 1809)
        seedCachedTag(id: 4243, title: "Short")
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let source = TMTestSource([4243, 1809])
        detail.source = source
        let split = UISplitViewController(style: .doubleColumn)
        split.preferredDisplayMode = .oneBesideSecondary
        split.setViewController(UINavigationController(rootViewController: UIViewController()), for: .primary)
        split.setViewController(UINavigationController(rootViewController: detail), for: .secondary)
        let splitWindow = ScreenCatalog.makeWindow()
        splitWindow.rootViewController = split
        splitWindow.makeKeyAndVisible()
        window = splitWindow
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending && detail.model.expanded }
        ScreenCatalog.settle(0.3)

        let driver = driver()
        for label in ["Previous tag", "Next tag", "Add Favorite", "Mark as Teachable", "Add to list", "Refresh", "Share"] {
            XCTAssertTrue(driver.exists(label: label), "\(label) is beside the list")
        }
        XCTAssertFalse(driver.exists(label: "Favorite and Teachable options"))
        XCTAssertFalse(driver.element(label: "Previous tag")?.accessibilityTraits.contains(.notEnabled) ?? true)
        XCTAssertTrue(driver.element(label: "Next tag")?.accessibilityTraits.contains(.notEnabled) ?? false,
                      "The last tag in the list has no next")
        XCTAssertTrue(detail.canPerformAction(#selector(TagDetailViewController.stepToPreviousTag), withSender: nil))
        XCTAssertFalse(detail.canPerformAction(#selector(TagDetailViewController.stepToNextTag), withSender: nil))
        XCTAssertEqual(detail.keyCommands?.prefix(2).map(\.discoverabilityTitle), ["Previous Tag", "Next Tag"])

        driver.tap(label: "Add Favorite")
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])
        XCTAssertTrue(driver.exists(label: "Remove Favorite"), "The heart fills and names its next action")
        driver.tap(label: "Mark as Teachable")
        XCTAssertEqual(DPAppDelegate.teachable(), [1809])
        XCTAssertTrue(driver.exists(label: "Unmark as Teachable"))

        // Changes made anywhere else show up in the bar too.
        TMTagLists.remove(1809, from: TMTagLists.favoriteKey)
        ScreenCatalog.settle(0.2)
        XCTAssertTrue(driver.exists(label: "Add Favorite"))

        // The assist chip shares the name; the bar's button is the later element.
        let barButton = try XCTUnwrap(driver.elements.last { $0.isAccessibilityElement && $0.accessibilityLabel == "Add to list" })
        XCTAssertTrue(barButton.accessibilityActivate())
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(detail.model.pickerSource, .toolbar)
        detail.model.pickerSource = nil
        ScreenCatalog.settle(0.5)
    }

    // MARK: - Summary

    func testTheSummaryOffersSheetMusicRatingAndTheKeyAsFullSizeTargets() {
        seedCachedTag(id: 1809, title: "Lost")
        _ = mountDetail()
        let driver = driver()
        XCTAssertTrue(driver.exists(label: "Sheet Music"))
        XCTAssertTrue(driver.exists(id: "summary.rate"))
        XCTAssertEqual(driver.label(id: "summary.rate"), "Rate tag")
        XCTAssertEqual(driver.label(id: "summary.key")?.hasPrefix("Play key note"), true)
        for element in [driver.element(label: "Sheet Music"), driver.element(id: "summary.rate"), driver.element(id: "summary.key")] {
            let frame = element?.accessibilityFrame ?? .zero
            XCTAssertGreaterThanOrEqual(frame.height, 44)
            XCTAssertGreaterThanOrEqual(frame.width, 44)
        }
    }

    func testActivatingTheKeyPlaysItsNoteForAMoment() throws {
        TagSummaryModel.timedKeyNoteDuration = 0.1
        defer { TagSummaryModel.timedKeyNoteDuration = 1.5 }
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        let note = try XCTUnwrap(detail.model.summary.keyNote)
        driver().tap(id: "summary.key")
        XCTAssertTrue(note.isPlaying)
        spinUntil("the timed note stops") { !note.isPlaying }
    }

    func testRateOpensTheRatingChoices() {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        driver().tap(id: "summary.rate")
        XCTAssertTrue(detail.model.summary.ratingDialogPresented)
        detail.model.summary.ratingDialogPresented = false
        ScreenCatalog.settle(0.4)
    }

    func testTheSummaryShowsItsLyricsAndDropsThemWhenThereAreNone() {
        seedCachedTag(id: 1809, lyrics: "And I will wait to face the skies")
        _ = mountDetail()
        XCTAssertTrue(driver().exists(label: "Lyrics"))
        XCTAssertEqual(driver().label(id: "summary.lyrics"), "And I will wait to face the skies")

        seedCachedTag(id: 4243, lyrics: nil)
        _ = mountDetail(4243)
        XCTAssertFalse(driver().exists(label: "Lyrics"))
    }

    func testLyricsRemainAfterVisitingAnotherPage() {
        seedCachedTag(id: 1809, lyrics: "And I will wait to face the skies")
        let detail = mountDetail()
        select(.details, in: detail)
        select(.summary, in: detail)
        XCTAssertEqual(driver().label(id: "summary.lyrics"), "And I will wait to face the skies")
        XCTAssertEqual(driver().label(id: "summary.rate"), "Rate tag", "Rating is still offered")
    }

    // MARK: - Chips

    func testTheChipsNameEveryListTheTagIsOnAndAlwaysOfferToAddAnother() {
        seedLists(favorite: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        seedCachedTag(id: 1809)
        _ = mountDetail()
        let driver = driver()
        XCTAssertEqual(driver.label(id: "summary.chip.favorite"), "Favorites")
        XCTAssertEqual(driver.label(id: "summary.chip.afterglow-set-k3f9"), "Afterglow set")
        XCTAssertEqual(driver.label(id: "summary.chip.add"), "Add to list")
        XCTAssertEqual(driver.label(id: "summary.chip.favorite.remove"), "Remove from Favorites")
        XCTAssertEqual(driver.label(id: "summary.chip.afterglow-set-k3f9.remove"), "Remove from Afterglow set")
        XCTAssertFalse(driver.exists(id: "summary.chip.add.remove"), "Nothing is removed from the assist chip")
        for id in ["summary.chip.favorite", "summary.chip.favorite.remove", "summary.chip.add"] {
            let frame = driver.element(id: id)?.accessibilityFrame ?? .zero
            XCTAssertGreaterThanOrEqual(frame.height, 43.99, "\(id) is a full target")
            XCTAssertGreaterThanOrEqual(frame.width, 43.99, "\(id) is a full target")
        }
    }

    func testAChipOpensItsListTheAddChipOpensThePickerAndRemovalIsUndoable() {
        seedLists(lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [669, 1809, 122])])
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        detail.model.navigator.showList = { [unowned self] in self.shownLists.append($0) }
        let driver = driver()
        // The accessibility tree is rebuilt lazily after the page settles.
        driver.wait { driver.exists(id: "summary.chip.afterglow-set-k3f9") }

        driver.tap(id: "summary.chip.afterglow-set-k3f9")
        XCTAssertEqual(shownLists, ["afterglow-set-k3f9"])

        driver.tap(id: "summary.chip.add")
        XCTAssertEqual(detail.model.pickerSource, .chip)
        detail.model.pickerSource = nil
        ScreenCatalog.settle(0.5)

        driver.tap(id: "summary.chip.afterglow-set-k3f9.remove")
        XCTAssertEqual(TMTagLists.ids(for: "afterglow-set-k3f9"), [669, 122])
        ScreenCatalog.settle(0.2)
        XCTAssertFalse(driver.exists(id: "summary.chip.afterglow-set-k3f9"))
        XCTAssertEqual(detail.undoManager?.undoActionName, "Remove from Afterglow set")
        detail.undoManager?.undo()
        XCTAssertEqual(TMTagLists.ids(for: "afterglow-set-k3f9"), [669, 1809, 122])
        ScreenCatalog.settle(0.2)
        XCTAssertTrue(driver.exists(id: "summary.chip.afterglow-set-k3f9"))
    }

    func testAChipsNameOffersItsRemovalAsAnAccessibilityAction() {
        seedLists(favorite: [1809])
        seedCachedTag(id: 1809)
        _ = mountDetail()
        driver().perform(action: "Remove from Favorites", id: "summary.chip.favorite")
        XCTAssertEqual(DPAppDelegate.favorites(), [])
    }

    func testTheChipsFollowChangesMadeAnywhereElse() {
        seedCachedTag(id: 1809)
        _ = mountDetail()
        XCTAssertFalse(driver().exists(id: "summary.chip.teachable"))
        DPAppDelegate.addTeachable(1809)
        ScreenCatalog.settle(0.2)
        XCTAssertEqual(driver().label(id: "summary.chip.teachable"), "Teachable Tags")
        DPAppDelegate.removeTeachable(1809)
        ScreenCatalog.settle(0.2)
        XCTAssertFalse(driver().exists(id: "summary.chip.teachable"))
    }

    func testTheChipsWrapWithoutOverflowingTheRow() {
        seedLists(favorite: [1809], teachable: [1809], lists: [
            (key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809]),
            (key: "chorus-warmups-list", name: "Chorus warmups for Tuesday evening", ids: [1809])
        ])
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        let driver = driver()
        let ids = ["favorite", "teachable", "afterglow-set-k3f9", "chorus-warmups-list", "add"].map { "summary.chip.\($0)" }
        let frames = ids.compactMap { driver.element(id: $0)?.accessibilityFrame }
        XCTAssertEqual(frames.count, 5)
        XCTAssertGreaterThan(Set(frames.map { $0.minY.rounded() }).count, 1, "Five capsules do not fit one phone-width line")
        let width = detail.view.window!.bounds.width
        for frame in frames { XCTAssertLessThanOrEqual(frame.maxX, width + 0.5) }
    }

    // MARK: - Details, Tracks and Videos

    func testTheDetailsPageShowsWhereTheTagCameFrom() {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        select(.details, in: detail)
        let driver = driver()
        for caption in ["Last Refreshed", "Downloads", "Link", "Posted", "Arranged by"] {
            XCTAssertTrue(driver.exists(label: caption), "\(caption) is shown")
        }
        XCTAssertTrue(driver.exists(label: "BarbershopTags.com"))
        XCTAssertTrue(driver.element(label: "BarbershopTags.com")?.accessibilityTraits.contains(.link) ?? false)
        XCTAssertFalse(driver.element(label: "A. Arranger")?.accessibilityTraits.contains(.link) ?? true,
                       "A name without a website is plain information")
        XCTAssertFalse(driver.exists(label: "Sung by"), "Empty facts are left out")
    }

    func testTheTracksPageApologisesWhenTheTagHasNoTracks() {
        seedCachedTag(id: 4244, withTracks: false)
        let detail = mountDetail(4244)
        select(.tracks, in: detail)
        XCTAssertTrue(driver().exists(label: "Sorry, no tracks could be found for this tag."))
    }

    func testTheTracksPageListsEveryVoicePartAsSomethingToPlay() {
        let tag = seedCachedTag(id: 1809, withTracks: true)
        let detail = mountDetail()
        select(.tracks, in: detail)
        XCTAssertEqual(tag.tracks.map(\.title), ["All Parts", "Tenor", "Lead", "Baritone", "Bass"])
        for title in ["All Parts", "Tenor", "Lead", "Baritone", "Bass"] {
            let row = driver().element(label: title)
            XCTAssertNotNil(row, "\(title) is listed")
            XCTAssertEqual(row?.accessibilityHint, "Plays the learning track")
        }
    }

    func testTheVideosPageDescribesEachVideoAndOpensItInTheApp() throws {
        let tag = seedCachedTag(id: 1809)
        tag.teachingVideo = "teach12345"
        tag.teacher = "Tag Teacher"
        let video = DPVideo()
        video.youTubeCode = "video12345"
        video.sungBy = "Main Street"
        video.sungKey = "Bb"
        video.isMultitrack = true
        video.posted = Date(timeIntervalSince1970: 1_650_000_000)
        tag.videos = [video]
        tag.cache()
        let detail = mountDetail()
        select(.videos, in: detail)
        let driver = driver()
        XCTAssertTrue(driver.exists(label: "Teaching video by Tag Teacher"))
        let posted = DateFormatter()
        posted.dateStyle = .long
        let spoken = "Video sung by Main Street in Bb. Posted \(posted.string(from: video.posted)). Multitrack."
        XCTAssertTrue(driver.exists(label: spoken))
        XCTAssertTrue(driver.exists(label: "Videos open on YouTube inside Tag Master."))
        driver.tap(label: spoken)
        spinUntil("the browser opens") { detail.presentedViewController != nil }
        func hostsBrowser(_ controller: UIViewController) -> Bool {
            controller is SFSafariViewController || controller.children.contains(where: hostsBrowser)
        }
        XCTAssertTrue(hostsBrowser(try XCTUnwrap(detail.presentedViewController)), "YouTube opens in the in-app browser")
        detail.presentedViewController?.dismiss(animated: false)
        ScreenCatalog.settle(0.3)
    }

    func testAVideolessTagSaysSo() {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        select(.videos, in: detail)
        XCTAssertTrue(driver().exists(label: "Sorry, this tag does not have any videos associated with it."))
    }

    // MARK: - The picker

    private func mountPicker(tagId: Int32 = 1809) -> TMListPickerModel {
        let model = TMListPickerModel(tagId: tagId)
        mount(UIHostingController(rootView: TMListPicker(model: model)))
        ScreenCatalog.settle(0.3)
        return model
    }

    func testThePickerListsEveryListWithItsSizeAndTheTagsMembership() {
        seedLists(favorite: [1809], lists: [
            (key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809]),
            (key: "chorus-warmups-list", name: "Chorus warmups", ids: [])
        ])
        _ = mountPicker()
        let driver = driver()
        XCTAssertTrue(driver.exists(label: "Add to list"), "The picker is titled")
        XCTAssertTrue(driver.exists(id: "picker.done"))
        let expected: [(String, String, String, Bool)] = [
            ("favorite", "Favorites", "1 tag", true), ("teachable", "Teachable Tags", "0 tags", false),
            ("afterglow-set-k3f9", "Afterglow set", "1 tag", true), ("chorus-warmups-list", "Chorus warmups", "0 tags", false),
        ]
        for (key, name, count, member) in expected {
            XCTAssertEqual(driver.label(id: "picker.row.\(key)"), name)
            XCTAssertEqual(driver.value(id: "picker.row.\(key)"), count)
            XCTAssertEqual(driver.isSelected(id: "picker.row.\(key)"), member, "\(name) says whether the tag is in it")
        }
        XCTAssertTrue(driver.exists(id: "picker.row.new"))
    }

    func testTappingAPickerRowMovesTheTagInAndOutOfThatList() {
        seedLists(lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [])])
        _ = mountPicker()
        let driver = driver()
        driver.tap(id: "picker.row.favorite")
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])
        XCTAssertTrue(driver.isSelected(id: "picker.row.favorite"))
        driver.tap(id: "picker.row.favorite")
        XCTAssertEqual(DPAppDelegate.favorites(), [])
        driver.tap(id: "picker.row.afterglow-set-k3f9")
        XCTAssertEqual(TMTagLists.ids(for: "afterglow-set-k3f9"), [1809])
    }

    func testThePickersNewListRowNamesAListAndPutsTheTagStraightIntoIt() throws {
        let model = mountPicker()
        driver().tap(id: "picker.row.new")
        XCTAssertTrue(model.namingNewList)
        XCTAssertEqual(model.newListMessage, TMListPickerModel.exampleHint)
        XCTAssertNotNil(model.newListProblem, "An empty name cannot be created")
        model.newListName = "Favorites"
        XCTAssertNotNil(model.newListProblem, "A reserved name is refused")
        XCTAssertEqual(model.newListMessage, model.newListProblem)
        model.newListName = "  Afterglow   set "
        XCTAssertNil(model.newListProblem)
        model.createNewList()
        model.namingNewList = false
        let key = try XCTUnwrap(TMTagLists.customKeys().first)
        XCTAssertEqual(TMTagLists.name(for: key), "Afterglow set")
        XCTAssertEqual(TMTagLists.ids(for: key), [1809])
        ScreenCatalog.settle(0.3)
        XCTAssertTrue(driver().isSelected(id: "picker.row.\(key)"), "A list made elsewhere shows up at once")
    }

    // MARK: - iPad placeholder

    func testThePlaceholderSaysWhatWillOpenThere() {
        mount(TMTagPlaceholderController())
        let driver = driver()
        XCTAssertTrue(driver.exists(label: "Pick a tag"))
        XCTAssertTrue(driver.element(label: "Pick a tag")?.accessibilityTraits.contains(.header) ?? false)
        XCTAssertTrue(driver.exists(label: "Choose a tag from the list. Its summary, tracks, sheet music, and videos open here."))
    }
}
