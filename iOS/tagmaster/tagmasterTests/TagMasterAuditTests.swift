//
//  TagMasterAuditTests.swift
//  tagmasterTests
//
//  Regression tests for what the behavioural review of the SwiftUI port found:
//  each test pins a UIKit behaviour the first port lost, or a bug it brought.
//

import AVFoundation
import Combine
import SafariServices
import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

// MARK: - Tag detail

@MainActor
final class TagDetailAuditTests: TMBehaviorTestCase {
    private func mountDetail(_ tagId: Int32 = 1809, size: CGSize = TMBehaviorTestCase.portrait) -> TagDetailViewController {
        let detail = TagDetailViewController()
        detail.tagId = tagId
        mountInNavigation(detail, size: size)
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending }
        ScreenCatalog.settle(0.3)
        return detail
    }

    private func topPresented() -> UIViewController? {
        var top = window.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top === window.rootViewController ? nil : top
    }

    func testTheKeyNoteStopsWhenTheTagChangesUnderIt() throws {
        let first = seedCachedTag(id: 1809)
        first.writtenKey = "Bb"
        first.cache()
        let second = seedCachedTag(id: 42, title: "Other")
        second.writtenKey = "D"
        second.cache()
        let detail = mountDetail(1809)
        let note = try XCTUnwrap(detail.model.summary.keyNote)
        detail.model.summary.playTimedKeyNote()
        XCTAssertTrue(note.isPlaying)
        detail.model.show(tagId: 42)
        XCTAssertFalse(note.isPlaying, "Stepping to another tag silences the one it was playing")
        XCTAssertFalse(detail.model.summary.keyNotePlaying)
    }

    func testReleasingTheKeyStopsTheNoteItStartedEvenAfterTheTagIsGone() throws {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        let summary = detail.model.summary
        let note = try XCTUnwrap(summary.keyNote)
        summary.pressKey()
        XCTAssertTrue(note.isPlaying)
        detail.model.show(tagId: 999_999)
        summary.releaseKey()
        XCTAssertFalse(note.isPlaying)
    }

    func testTheKeyIsAButtonSoAScrollOrACancelledPressLetsTheNoteGo() {
        seedCachedTag(id: 1809)
        _ = mountDetail()
        // A Button (not a zero-distance drag) sits under the key: scroll views delay
        // and cancel its touches, and its pressed state ends on cancel.
        XCTAssertNotNil(firstDescendant(of: window) { String(describing: type(of: $0)).contains("Button") })
        XCTAssertTrue(UIDriver(window).traits(id: "summary.key").contains(.button))
    }

    func testTheKeyNoteIsWatchedOnlyWhileItSounds() {
        var ticks = 0
        let idle = TagSummaryPage.keyNoteTicks(active: false).sink { _ in ticks += 1 }
        ScreenCatalog.settle(0.15)
        XCTAssertEqual(ticks, 0, "No timer runs while nothing sounds or the page is hidden")
        idle.cancel()
        let active = TagSummaryPage.keyNoteTicks(active: true).sink { _ in ticks += 1 }
        ScreenCatalog.settle(0.15)
        XCTAssertGreaterThan(ticks, 0)
        active.cancel()
    }

    func testRatePresentsUIKitsActionSheetHangingFromRate() throws {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        UIDriver(window).tap(id: "summary.rate")
        spinUntil("the rating sheet is up") { self.topPresented() is UIAlertController }
        let sheet = try XCTUnwrap(topPresented() as? UIAlertController)
        XCTAssertEqual(sheet.preferredStyle, .actionSheet)
        XCTAssertEqual(sheet.title, "Rating")
        XCTAssertEqual(sheet.message, "Rate the tag on a scale of 1-5 stars")
        XCTAssertEqual(sheet.actions.map(\.title), ["5 stars", "4 stars", "3 stars", "2 stars", "1 star", "Cancel"])
        XCTAssertEqual(sheet.actions.last?.style, .cancel)
        XCTAssertNotNil(sheet.popoverPresentationController?.sourceView, "It hangs from Rate, as the UIKit sheet did")
        sheet.dismiss(animated: false)
        detail.model.summary.ratingDialogPresented = false
        ScreenCatalog.settle(0.3)
    }

    func testAtAccessibilitySizesRateMovesUnderTheRatingOnANarrowPhone() throws {
        seedCachedTag(id: 1809)
        window = nil
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let navigation = UINavigationController(rootViewController: detail)
        mount(navigation, size: CGSize(width: 320, height: 568))
        window.traitOverrides.preferredContentSizeCategory = .accessibilityExtraExtraExtraLarge
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending }
        ScreenCatalog.settle(0.5)
        let driver = UIDriver(window)
        let value = try XCTUnwrap(driver.element(label: "Rating out of 5")).accessibilityFrame
        let rate = try XCTUnwrap(driver.element(id: "summary.rate")).accessibilityFrame
        XCTAssertGreaterThanOrEqual(rate.minY, value.maxY - 0.5, "Rate stacks under the number")
        XCTAssertLessThanOrEqual(rate.maxX, window.bounds.maxX + 0.5, "Rate stays on screen")
    }

    func testTheFactsStackAtAccessibilitySizesAsTheUIKitLayoutDid() throws {
        // At the largest size the value cap (eight ems of the body font) no longer
        // fits beside the captions on a phone; a fixed 17-point cap would keep them side by side.
        let large = DynamicTypeSize.large.tmFont(.body)
        let huge = DynamicTypeSize.accessibility5.tmFont(.body)
        XCTAssertEqual(TMFactsLayout(bodyFont: large).bodyFont.pointSize, large.pointSize)
        XCTAssertGreaterThan(huge.pointSize * 8, 402, "The cap outgrows a phone at the largest size")
        XCTAssertLessThan(large.pointSize * 8, 402 - 80)
    }

    func testTheRefreshPoleStandsInTheBarOnItsOwn() throws {
        let loader = TMControlledTagLoader()
        let detail = TagDetailViewController(model: TagDetailModel(loader: loader))
        detail.tagId = 1809
        mountInNavigation(detail)
        loader.finish(0, with: seedCachedTag(id: 1809))
        ScreenCatalog.settle(0.4)
        XCTAssertTrue(UIDriver(window).exists(label: "Refresh"))
        detail.model.refresh()
        ScreenCatalog.settle(0.4)
        XCTAssertTrue(UIDriver(window).exists(id: "tag.refreshing"), "The pole stands where Refresh was")
        XCTAssertFalse(UIDriver(window).exists(label: "Refresh"))
        loader.finish(1, with: detail.model.tag)
        ScreenCatalog.settle(0.4)
        XCTAssertTrue(UIDriver(window).exists(label: "Refresh"))
        XCTAssertFalse(UIDriver(window).exists(id: "tag.refreshing"))
    }

    func testANewlyLoadedTrackTakesVoiceOverToPlayPause() throws {
        let model = TagTracksModel(busy: TMBusyCount())
        let track = DPTrack()
        track.title = "Tenor"
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 22050, channels: 2))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 2205))
        buffer.frameLength = 2205
        XCTAssertEqual(model.player.focusRequest, 0)
        model.present(track, buffer: buffer)
        XCTAssertEqual(model.player.focusRequest, 1, "The player asks for focus on Play/Pause")
        model.stopPlayback()
    }

    func testTheTwoFingerDoubleTapFallbackListensOnTheWindowWithinTheSlider() {
        var centred = 0
        let anchor = TMTwoFingerDoubleTapFallback.Anchor(frame: CGRect(x: 20, y: 100, width: 200, height: 44))
        anchor.action = { centred += 1 }
        let host = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 480))
        host.addSubview(anchor)
        XCTAssertTrue(anchor.recognizer.view === host, "The tap is heard by the window")
        XCTAssertEqual(anchor.recognizer.numberOfTouchesRequired, 2)
        XCTAssertEqual(anchor.recognizer.numberOfTapsRequired, 2)
        XCTAssertFalse(anchor.recognizer.cancelsTouchesInView, "The slider still gets its touches")
        XCTAssertTrue(anchor.accepts(CGPoint(x: 10, y: 10)))
        XCTAssertFalse(anchor.accepts(CGPoint(x: 10, y: 60)), "Fingers off the slider do not count")
        anchor.tapped(anchor.recognizer)
        anchor.removeFromSuperview()
        XCTAssertNil(anchor.recognizer.view, "Leaving the window takes the recognizer with it")
        host.isHidden = true
    }

    func testThePlayersControlsAreNotReadAsHeadings() throws {
        let tag = seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        mountInNavigation(detail)
        spinUntil("the detail settles", timeout: 5) { detail.model.tag != nil }
        detail.model.selectedPage = .tracks
        ScreenCatalog.settle(0.4)
        let track = try XCTUnwrap(tag.tracks.first as? DPTrack)
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 22050, channels: 2))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 2205))
        buffer.frameLength = 2205
        detail.model.tracks.present(track, buffer: buffer)
        ScreenCatalog.settle(0.5)
        let driver = UIDriver(window)
        for id in ["tagmaster.trackPlayer.playPause", "tagmaster.trackPlayer.stop",
                   "tagmaster.trackPlayer.position", "tagmaster.trackPlayer.balance"] {
            XCTAssertTrue(driver.exists(id: id), id)
            XCTAssertFalse(driver.traits(id: id).contains(.header), "\(id) is a control, not a heading")
        }
        detail.model.tracks.stopPlayback()
    }

    func testTheBrowsersDoneClosesTheVideoSoItCanOpenAgain() {
        var finished = 0
        let view = TMSafariView(url: URL(string: "https://www.youtube.com/watch?v=abc")!) { finished += 1 }
        let coordinator = view.makeCoordinator()
        coordinator.onFinish = view.onFinish
        let browser = SFSafariViewController(url: URL(string: "https://www.youtube.com")!)
        browser.delegate = coordinator
        coordinator.safariViewControllerDidFinish(browser)
        XCTAssertEqual(finished, 1)
    }
}

// MARK: - Router and shell

@MainActor
final class TMShellAuditTests: TMBehaviorTestCase {
    func testTheCompactColumnFollowsTheUserAfterTheSplitCollapses() {
        let router = TMRouter()
        router.setExpanded(true)
        router.showTag(1809, source: nil)
        router.setExpanded(false)
        XCTAssertEqual(router.preferredCompactColumn, .detail, "A chosen tag stays on top when the split collapses")
        router.preferredCompactColumn = .sidebar
        XCTAssertEqual(router.preferredCompactColumn, .sidebar, "Back to the list stays on the list")
        router.showTag(42, source: nil)
        XCTAssertEqual(router.preferredCompactColumn, .sidebar, "A tag pushed on the list stack is shown there")
        XCTAssertEqual(router.path.last?.tagModel?.tagId, 42)
    }

    func testCollapsingWithNothingChosenKeepsTheList() {
        let router = TMRouter()
        router.setExpanded(true)
        router.setExpanded(false)
        XCTAssertEqual(router.preferredCompactColumn, .sidebar)
    }

    func testALinkedTagStepsThroughTheListOnTop() throws {
        let router = TMRouter()
        XCTAssertTrue(router.open(URL(string: "tagmaster://tag/1809")!))
        let fromHome = try XCTUnwrap(router.path.last?.tagModel)
        XCTAssertTrue((fromHome.source as? TMListingSource)?.listing === router.home, "Over Home, Home is the list")

        router.popToRoot()
        router.show(.teachable)
        let teachable = try XCTUnwrap(router.path.last)
        XCTAssertTrue(router.open(URL(string: "tagmaster://tag/42")!))
        XCTAssertTrue(router.path.last?.tagModel?.source === teachable.listingSource)

        router.popToRoot()
        router.show(.search)
        XCTAssertTrue(router.open(URL(string: "tagmaster://tag/42")!))
        XCTAssertNil(router.path.last?.tagModel?.source, "Search lists no tags")
    }

    func testEachBrowsePageIsItsOwnListForStepping() throws {
        let router = TMRouter(catalog: TMCatalog { _, _, _ in nil })
        router.setExpanded(true)
        router.show(.browse)
        guard case .browse(let browse)? = router.path.last?.screen else { return XCTFail("Browse is on top") }
        let latest = browse.pages[0], rating = browse.pages[1]
        XCTAssertFalse(latest.owner === rating.owner, "Each page has its own list")
        let tag = DPTag()
        tag.tagId = 1809
        latest.open(tag)
        browse.selectedIndex = 1
        let source = try XCTUnwrap(router.detail.source as? TMListingSource)
        XCTAssertTrue(source.listing === latest, "The tag keeps stepping through Latest when Rating shows")
    }

    func testTheSplitSharesItsWatermarkOnlyWhereTheColumnsCanBeClear() {
        if #available(iOS 18.0, *) {
            XCTAssertTrue(TMSplitRoot.columnsCanBeClear)
        } else {
            XCTAssertFalse(TMSplitRoot.columnsCanBeClear, "iOS 17 keeps each screen's own watermark")
        }
    }

    /// The key commands of controllers and views actually in the window: a screen
    /// covered on a navigation stack is off the responder chain, so its don't count.
    private func keyCommands(in window: UIWindow) -> [UIKeyCommand] {
        var commands: [UIKeyCommand] = []
        func visit(_ controller: UIViewController?) {
            guard let controller, controller.viewIfLoaded?.window === window else { return }
            commands += controller.keyCommands ?? []
            controller.children.forEach(visit)
            visit(controller.presentedViewController)
        }
        visit(window.rootViewController)
        func walk(_ view: UIView) {
            commands += view.keyCommands ?? []
            view.subviews.forEach(walk)
        }
        walk(window)
        return commands
    }

    private func steppingCommands(in window: UIWindow) -> Set<String> {
        Set(keyCommands(in: window).filter { $0.modifierFlags.contains(.command) }.compactMap(\.input)
            .filter { $0 == UIKeyCommand.inputUpArrow || $0 == UIKeyCommand.inputDownArrow })
    }

    func testStepShortcutsStayLiveWhileTheSheetMusicReaderIsOpen() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "The split is iPad only")
        seedCachedTag(id: 1809)
        seedCachedTag(id: 42, title: "Other")
        let source = TMTestSource([1809, 42])
        let router = TMRouter()
        mountShell(router, split: true)
        spinUntil("expanded") { router.expanded }
        router.showTag(1809, source: source)
        spinUntil("loaded", timeout: 5) { router.detail.tag != nil && router.detail.expanded }
        ScreenCatalog.settle(0.4)
        XCTAssertEqual(steppingCommands(in: window), [UIKeyCommand.inputDownArrow],
                       "Beside the list the detail offers ⌘↓ (nothing before the first tag)")
        let file = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("sheet-\(UUID().uuidString).pdf")
        try UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 612, height: 792)).pdfData { $0.beginPage() }.write(to: file)
        defer { try? FileManager.default.removeItem(at: file) }
        router.detail.navigator.showSheetMusic(TMSheetMusicDocument(fileURL: file, title: "Lost", writtenKey: "Bb"))
        ScreenCatalog.settle(0.8)
        XCTAssertEqual(router.detailPath.count, 1, "The reader covers the detail")
        // SwiftUI keeps the covered detail's shortcuts on the scene's root, which is
        // always in the responder chain, as the UIKit delegate forwarded them.
        let host = try XCTUnwrap(window.rootViewController)
        let command = try XCTUnwrap((host.keyCommands ?? []).first {
            $0.input == UIKeyCommand.inputDownArrow && $0.modifierFlags.contains(.command)
        }, "⌘↓ is still registered with the reader open")
        let action = try XCTUnwrap(command.action)
        // The command is SwiftUI's; whoever in the chain from the root view answers it runs it.
        let target = try XCTUnwrap(host.view.target(forAction: action, withSender: command) as? NSObject,
                                   "Something in the responder chain performs ⌘↓")
        _ = target.perform(action, with: command)
        spinUntil("stepped") { router.detail.tagId == 42 }
        XCTAssertTrue(router.detailPath.isEmpty, "Stepping closes the reader over the old tag")
        withExtendedLifetime(source) {}
    }

    func testHomesHandwrittenTitleStaysOnHome() throws {
        let router = TMRouter(catalog: TMCatalog { _, _, _ in nil })
        mountShell(router)
        ScreenCatalog.settle(0.5)
        router.show(.search)
        ScreenCatalog.settle(0.8)
        // Home re-renders while covered, as it does when a favourite changes.
        seedLists(favorite: [1809])
        NotificationCenter.default.post(name: .userDataChanged, object: nil)
        ScreenCatalog.settle(0.5)
        let bar = try XCTUnwrap(firstDescendant(of: window) { $0 is UINavigationBar } as? UINavigationBar)
        let font = bar.standardAppearance.titleTextAttributes[.font] as? UIFont
        XCTAssertFalse(font?.fontName.lowercased().contains("wickhop") ?? false,
                       "Search's title is not set in Home's handwriting")
    }
}

// MARK: - Lists

@MainActor
final class TMListsAuditTests: TMBehaviorTestCase {
    func testAFailedRowAsksAgainAfterAWhileAndWhenTheAppComesBack() {
        let loads = LockedCounter()
        var now = Date(timeIntervalSince1970: 1_000)
        let store = TMTagStore(cached: { _ in nil }, load: { _ in
            loads.increment()
            return nil
        }, now: { now })
        XCTAssertNil(store.tag(12))
        spinUntil("the fetch fails") { !store.isLoading(12) }
        XCTAssertNil(store.tag(12))
        XCTAssertEqual(loads.value, 1, "A failure is not retried on every read")

        now += TMTagStore.retryInterval
        XCTAssertNil(store.tag(12))
        spinUntil("the retry settles") { !store.isLoading(12) }
        XCTAssertEqual(loads.value, 2, "Once the interval passes the row asks again")

        NotificationCenter.default.post(name: UIApplication.didBecomeActiveNotification, object: nil)
        XCTAssertNil(store.tag(12))
        spinUntil("the retry settles") { !store.isLoading(12) }
        XCTAssertEqual(loads.value, 3, "Coming back to the app asks again")

        NotificationCenter.default.post(name: .userDataChanged, object: nil)
        XCTAssertNil(store.tag(12))
        spinUntil("the retry settles") { !store.isLoading(12) }
        XCTAssertEqual(loads.value, 4, "A list change asks again")
    }

    func testAListNamingATagTwiceGivesEachRowItsOwnIdentity() {
        let keyed = TMListedTag.keyed([5, 7, 5, 5])
        XCTAssertEqual(keyed.map(\.id), ["5#0", "7#0", "5#1", "5#2"])
        XCTAssertEqual(Set(keyed.map(\.id)).count, 4)
        XCTAssertEqual(keyed.filter(\.isFirst).map(\.tagId), [5, 7], "Only a tag's first row is a scroll target")
    }

    func testACatalogPageThatRepeatsATagListsItOnce() {
        func tag(_ id: Int32) -> DPTag {
            let tag = DPTag()
            tag.tagId = id
            tag.title = "Tag \(id)"
            return tag
        }
        let pages: [[Int32]] = [[1, 2, 3], [3, 4, 5]]
        let model = TMQueryModel(query: TMTagQuery(sortBy: DPTagSortPosted), catalog: TMCatalog { _, count, start in
            let index = start / max(count, 1)
            guard index < pages.count else { return nil }
            let result = DPTagQueryResult()
            result.tags = pages[index].map(tag)
            result.start = Int32(start)
            result.count = Int32(count)
            result.available = 10
            return result
        }, pageSize: 3)
        model.fetchNextPage()
        spinUntil("the first page lands") { !model.isLoading }
        model.fetchNextPage()
        spinUntil("the second page lands") { !model.isLoading }
        XCTAssertEqual(model.tags.map(\.tagId), [1, 2, 3, 4, 5])
    }

    func testTheSearchKeyStaysEnabledOnAnEmptyField() throws {
        let screen = TMScreens.search()
        mountScreen(screen)
        ScreenCatalog.settle(0.4)
        let field = try XCTUnwrap(firstDescendant(of: window) { $0 is UISearchTextField } as? UISearchTextField)
        XCTAssertFalse(field.enablesReturnKeyAutomatically, "An empty search lists every tag, so Search is always available")
    }

    func testListFontsFollowTheTextSizeWhileOnScreen() throws {
        let content = TMTagRowContent(tagId: 1809, tag: nil)
        let host = UIHostingController(rootView: TMTagRow(content: content).frame(width: 375))
        mount(host)
        ScreenCatalog.settle(0.2)
        let small = host.view.sizeThatFits(CGSize(width: 375, height: CGFloat.greatestFiniteMagnitude)).height
        window.traitOverrides.preferredContentSizeCategory = .accessibilityExtraExtraExtraLarge
        ScreenCatalog.settle(0.3)
        let large = host.view.sizeThatFits(CGSize(width: 375, height: CGFloat.greatestFiniteMagnitude)).height
        XCTAssertGreaterThan(large, small * 1.5, "The row grows with the text size without being rebuilt")
    }
}
