//
//  QueryBehaviorTests.swift
//  tagmasterTests
//
//  Catalog results: paging, the empty, failed and retried states, pull to
//  refresh, stepping from the detail, Browse's four pages and the tag rows
//  every list shares. Queries are answered by TMFixtureCatalog, so nothing
//  reaches the network and every request is on record.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class QueryBehaviorTests: TMBehaviorTestCase {

    private var navigator: RecordingNavigator!
    private var fixtures: TMFixtureCatalog!

    nonisolated override func setUp() {
        super.setUp()
        MainActor.assumeIsolated {
            navigator = RecordingNavigator()
            fixtures = TMFixtureCatalog(available: 45)
        }
    }

    private func model(_ query: TMTagQuery = TMTagQuery(text: "coney"), pageSize: Int = 20) -> TMQueryModel {
        TMQueryModel(query: query, catalog: fixtures.catalog, navigator: navigator, pageSize: pageSize)
    }

    private func load(_ model: TMQueryModel) {
        model.fetchNextPage()
        spinUntil("the page lands") { !model.isLoading }
    }

    // MARK: - Paging

    func testTheFirstPageAsksForTwentyFromTheStart() {
        let model = self.model()
        load(model)
        XCTAssertEqual(fixtures.queries.map(\.count), [20])
        XCTAssertEqual(fixtures.queries.map(\.start), [0])
        XCTAssertEqual(fixtures.queries.first?.query.text, "coney")
        XCTAssertEqual(model.tags.count, 20)
        XCTAssertTrue(model.hasMoreResults)
        XCTAssertNil(model.statusText)
        XCTAssertEqual(model.listedTagIds.first, 3000)
    }

    func testPagesFollowOnUntilEveryMatchIsListed() {
        let model = self.model()
        load(model)
        load(model)
        load(model)
        XCTAssertEqual(fixtures.queries.map(\.start), [0, 20, 40])
        XCTAssertEqual(model.tags.count, 45)
        XCTAssertFalse(model.hasMoreResults)
        load(model)
        XCTAssertEqual(fixtures.queries.count, 3, "Nothing more is asked for once everything is listed")
    }

    func testAQueryNeverListsMoreThanItsMaximum() {
        fixtures.available = 5000
        let model = TMQueryModel(query: TMTagQuery(), catalog: fixtures.catalog, pageSize: 20, maxResults: 40)
        load(model)
        load(model)
        XCTAssertFalse(model.hasMoreResults)
    }

    func testOnlyOnePageIsInFlightAtATime() {
        let gate = DispatchSemaphore(value: 0)
        fixtures.gate = gate
        let model = self.model()
        model.fetchNextPage()
        model.fetchNextPage()
        model.refresh()
        gate.signal()
        spinUntil("the page lands") { !model.isLoading }
        XCTAssertEqual(fixtures.queries.count, 1)
    }

    func testTheListAsksForMoreOnceItsLastEighthComesIntoView() {
        let model = self.model()
        load(model)
        model.rowAppeared(10)
        XCTAssertFalse(model.isLoading, "Halfway down is too early")
        model.rowAppeared(16)
        XCTAssertTrue(model.isLoading)
        spinUntil("the next page lands") { !model.isLoading }
        XCTAssertEqual(model.tags.count, 40)
    }

    // MARK: - Empty and failed

    func testAQueryWithNoMatchesSaysSo() {
        fixtures.available = 0
        let model = self.model()
        load(model)
        XCTAssertEqual(model.statusText, "No tags could be found that matched your query.")
        XCTAssertFalse(model.failed)
        XCTAssertFalse(model.hasMoreResults)
    }

    func testAFailedQueryIsNotMistakenForAnEmptyOneAndCanBeRetried() {
        fixtures.available = nil
        let model = self.model()
        load(model)
        XCTAssertEqual(model.statusText, "Tags couldn't be loaded. Check your connection and try again.")
        XCTAssertTrue(model.failed)
        XCTAssertFalse(model.hasMoreResults)

        fixtures.available = 1
        model.refresh()
        spinUntil("the retry lands") { !model.isLoading }
        XCTAssertEqual(model.tags.count, 1)
        XCTAssertFalse(model.failed)
        XCTAssertNil(model.statusText)
        XCTAssertFalse(model.hasMoreResults)
        model.refresh()
        spinUntil("the refresh lands") { !model.isLoading }
        XCTAssertEqual(fixtures.queries.count, 3)
        XCTAssertEqual(model.tags.count, 1, "A refresh starts over rather than appending")
    }

    func testTheLiveCatalogReportsAnUnreachableCatalogAsAFailure() {
        // TMBlockedNetwork refuses every request, as an offline device would.
        let result = TMCatalog.live.query(TMTagQuery(text: "coney"), 20, 0)
        XCTAssertNil(result, "An unreachable catalog is not an empty result")
        XCTAssertFalse(network.attemptedURLs.isEmpty, "The real request path ran")
    }

    /// Every screen's recovery alert: the message, then Retry and Cancel, or Cancel
    /// alone when trying again would not help.
    func testTheRecoveryAlertOffersRetryOnlyWhenItCanHelp() throws {
        struct Host: View {
            @State var recovery: TMRecovery?
            let initial: TMRecovery
            var body: some View {
                Color.clear
                    .tmRecoveryAlert($recovery)
                    .onAppear { recovery = initial }
            }
        }
        var retried = 0
        for (initial, titles) in [
            (TMRecovery(message: "Check your connection and try again.", retry: { retried += 1 }), ["Retry", "Cancel"]),
            (TMRecovery(message: "No results.", retry: nil), ["Cancel"]),
        ] {
            let host = UIHostingController(rootView: Host(initial: initial))
            mount(host)
            spinUntil("the alert shows") { host.presentedViewController is UIAlertController }
            let alert = try XCTUnwrap(host.presentedViewController as? UIAlertController)
            XCTAssertEqual(alert.title, TMRecovery.title)
            XCTAssertEqual(alert.message, initial.message)
            XCTAssertEqual(Set(alert.actions.compactMap(\.title)), Set(titles))
            XCTAssertEqual(alert.actions.first { $0.title == "Cancel" }?.style, .cancel)
            alert.dismiss(animated: false)
            ScreenCatalog.settle(0.1)
        }
        XCTAssertEqual(retried, 0, "Showing the alert retries nothing")
    }

    func testTheMountedFailureOffersRetry() {
        fixtures.available = nil
        let screen = TMScreens.results(TMTagQuery(text: "coney"), navigator: navigator, catalog: fixtures.catalog)
        let driver = mountScreen(screen)
        spinUntil("the failure shows") { driver.exists(label: TMQueryModel.failureMessage) }
        fixtures.available = 2
        driver.tap(label: "Retry")
        spinUntil("rows replace the failure") { driver.elements(labelPrefix: "Fixture 1").count == 1 }
        XCTAssertFalse(driver.exists(label: "Retry"))
    }

    func testPullToRefreshReturnsOnceTheFirstPageHasLanded() async {
        let model = self.model()
        await model.refreshAndWait()
        XCTAssertFalse(model.isLoading)
        XCTAssertEqual(model.tags.count, 20)
    }

    // MARK: - Title and rows

    func testResultsAreTitledAfterTheSearch() {
        XCTAssertEqual(model(TMTagQuery(text: "coney")).title, "coney")
        XCTAssertEqual(model(TMTagQuery(text: "")).title, "Search Results")
        XCTAssertEqual(model(TMTagQuery()).title, "Search Results")
        let screen = TMScreens.results(TMTagQuery(text: "coney"), navigator: navigator, catalog: fixtures.catalog)
        mountScreen(screen)
        XCTAssertEqual(screen.navigationItem.title, "coney")
        XCTAssertEqual(screen.navigationItem.largeTitleDisplayMode, .never)
    }

    func testTappingAResultOpensItsTag() {
        let screen = TMScreens.results(TMTagQuery(text: "coney"), navigator: navigator, catalog: fixtures.catalog)
        let driver = mountScreen(screen)
        spinUntil("rows appear") { driver.elements(labelPrefix: "Fixture 2.").count == 1 }
        driver.elements(labelPrefix: "Fixture 2.").first?.accessibilityActivate()
        ScreenCatalog.settle(0.05)
        XCTAssertEqual(navigator.shownTags, [3002])
    }

    // MARK: - Stepping from the detail

    func testSteppingSelectsTheRowAndFetchesMoreFromTheLastLoadedTag() {
        let model = self.model()
        model.owner = self
        navigator.isExpandedSplit = true
        load(model)
        navigator.currentSplitTagId = 3005
        model.didStep(to: 3005)
        XCTAssertEqual(model.selectedTagId, 3005)
        XCTAssertEqual(model.scrollTarget, 3005)
        XCTAssertFalse(model.isLoading)

        let changed = expectation(forNotification: .TMTagListDidChange, object: self)
        navigator.currentSplitTagId = 3019
        model.didStep(to: 3019)
        XCTAssertTrue(model.isLoading, "The last loaded tag pulls in the next page")
        wait(for: [changed], timeout: 3)
        XCTAssertEqual(model.tags.count, 40)
        XCTAssertEqual(model.selectedTagId, 3019)
    }

    func testTheOpenTagStaysLitOnlyBesideTheDetail() {
        let model = self.model()
        load(model)
        navigator.isExpandedSplit = true
        navigator.currentSplitTagId = 3003
        NotificationCenter.default.post(name: .TMTagSelectionDidChange, object: nil)
        XCTAssertTrue(model.expanded)
        XCTAssertEqual(model.selectedTagId, 3003)
        navigator.isExpandedSplit = false
        model.syncSelection()
        XCTAssertFalse(model.expanded)
        XCTAssertNil(model.selectedTagId)
    }

    func testAPreloadedListFetchesNothing() {
        let tags = [3, 4].map { id -> DPTag in
            let tag = DPTag()
            tag.tagId = Int32(id)
            tag.title = "Tag \(id)"
            return tag
        }
        let screen = TMScreens.results(TMTagQuery(), navigator: navigator, catalog: fixtures.catalog, preloaded: tags)
        mountScreen(screen)
        XCTAssertEqual(screen.tm_listedTagIds().map(\.intValue), [3, 4])
        XCTAssertTrue(fixtures.queries.isEmpty)
    }

    // MARK: - Browse

    private static let browseTabs = ["Latest", "Rating", "Downloads", "Classic"]

    private func browse() -> (TMHostedScreen, TMBrowseModel, UIDriver) {
        let controller = TMScreens.browse(navigator: navigator, catalog: fixtures.catalog)
        let driver = mountScreen(controller)
        return (controller, controller.listing as! TMBrowseModel, driver)
    }

    private func tabBar(in view: UIView) -> UITabBar? {
        firstDescendant(of: view) { $0 is UITabBar } as? UITabBar
    }

    func testBrowseOffersItsFourPagesAlongTheFootOfTheScreen() {
        let (controller, model, _) = browse()
        XCTAssertEqual(controller.navigationItem.title, "Browse")
        XCTAssertEqual(TMBrowsePage.all.map(\.title), Self.browseTabs)
        XCTAssertEqual(model.pages.count, 4)
        let bar = tabBar(in: controller.view)
        XCTAssertNotNil(bar)
        XCTAssertEqual(bar?.items?.map { $0.title ?? "" }, Self.browseTabs)
        if let bar {
            let frame = bar.convert(bar.bounds, to: controller.view)
            XCTAssertGreaterThan(frame.minY, controller.view.safeAreaLayoutGuide.layoutFrame.midY,
                                 "The bar sits along the bottom")
            XCTAssertEqual(bar.items?.filter { $0 == bar.selectedItem }.count, 1)
        }
    }

    func testEachBrowsePageSortsTheCatalogItsOwnWay() {
        XCTAssertEqual(TMBrowsePage.all.map { $0.query.sortBy.rawValue },
                       [DPTagSortPosted, DPTagSortRating, DPTagSortDownloaded, DPTagSortClassic].map(\.rawValue))
        XCTAssertEqual(TMBrowsePage.all.last?.query.collection.rawValue, DPTagCollectionClassicTags.rawValue)
        XCTAssertEqual(TMBrowsePage.all.map(\.symbol), ["clock", "star", "arrow.down.circle", "book"])
    }

    func testSelectingABrowsePageShowsItAndSurvivesASizeChange() {
        let (controller, model, _) = browse()
        spinUntil("Latest loads") { !model.pages[0].tags.isEmpty }
        model.selectedIndex = 3
        ScreenCatalog.settle(0.3)
        XCTAssertEqual(tabBar(in: controller.view)?.selectedItem?.title, "Classic")
        spinUntil("Classic loads") { !model.pages[3].tags.isEmpty }
        XCTAssertEqual(fixtures.queries.last?.query.sortBy.rawValue, DPTagSortClassic.rawValue)
        for size in [TMBehaviorTestCase.landscape, TMBehaviorTestCase.portrait] {
            resize(to: size)
            ScreenCatalog.settle(0.2)
            XCTAssertEqual(tabBar(in: controller.view)?.selectedItem?.title, "Classic",
                           "The open page survives the size change")
        }
        XCTAssertEqual(controller.tm_listedTagIds().map(\.intValue), model.pages[3].listedTagIds,
                       "The detail steps through the page on show")
    }

    func testTappingABrowseTabSelectsThatPage() {
        let (controller, model, driver) = browse()
        _ = driver
        for (index, title) in Self.browseTabs.enumerated().reversed() {
            tapTab(title, in: controller.view)
            ScreenCatalog.settle(0.2)
            XCTAssertEqual(model.selectedIndex, index)
            XCTAssertEqual(tabBar(in: controller.view)?.selectedItem?.title, title)
        }
    }

    // MARK: - Tag rows

    func testATagRowNamesItsTagAndItsMedia() {
        let tag = seedCachedTag(id: 1478, title: "Their Hearts Were Full Of Spring")
        let content = TMTagRowContent(tagId: 1478, tag: tag)
        XCTAssertEqual(content.title, "Their Hearts Were Full Of Spring")
        XCTAssertEqual(content.aka, "a.k.a. Lost (Tag)")
        XCTAssertEqual(content.details, "ID: 1478 Rating: 4.50 Posted: 09/13/20 DLs: 1234")
        XCTAssertTrue(content.hasSheetMusic)
        XCTAssertTrue(content.hasLearningTracks)
        XCTAssertTrue(content.accessibilityLabel.contains("Their Hearts Were Full Of Spring"))
        XCTAssertTrue(content.accessibilityLabel.contains("Sheet music available"))
        XCTAssertTrue(content.accessibilityLabel.contains("Learning tracks available"))
        XCTAssertTrue(content.accessibilityLabel.contains("Tag ID 1478"))
    }

    func testATagRowMarksMissingMediaAsUnavailable() {
        let tag = seedCachedTag(id: 4242, title: "Bare Tag", withTracks: false, withSheetMusic: false)
        let content = TMTagRowContent(tagId: 4242, tag: tag)
        XCTAssertFalse(content.hasSheetMusic)
        XCTAssertFalse(content.hasLearningTracks)
        XCTAssertTrue(content.accessibilityLabel.contains("Sheet music unavailable"))
        XCTAssertTrue(content.accessibilityLabel.contains("Learning tracks unavailable"))
    }

    /// The marks are green checks when the media exists and grey crosses when it
    /// doesn't, as DPTagCell tinted them; the spoken label never depends on colour.
    func testATagRowTintsAvailableMediaGreenAndMissingMediaGrey() throws {
        func greenPixels(_ tag: DPTag) -> Int {
            let host = UIHostingController(rootView: TMTagRow(content: TMTagRowContent(tagId: Int(tag.tagId), tag: tag))
                .frame(width: 320))
            host.overrideUserInterfaceStyle = .light
            mount(host, size: CGSize(width: 320, height: 200))
            ScreenCatalog.settle(0.2)
            guard let cg = ScreenCatalog.image(of: window).cgImage else { return 0 }
            // Redraw into plain 8-bit sRGB so every pixel reads the same way.
            let width = cg.width, height = cg.height
            var pixels = [UInt8](repeating: 0, count: width * height * 4)
            guard let context = CGContext(data: &pixels, width: width, height: height, bitsPerComponent: 8,
                                          bytesPerRow: width * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                                          bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else { return 0 }
            context.draw(cg, in: CGRect(x: 0, y: 0, width: width, height: height))
            // A clearly green pixel: green well above both red and blue.
            return stride(from: 0, to: pixels.count, by: 4).filter { index in
                let r = Int(pixels[index]), g = Int(pixels[index + 1]), b = Int(pixels[index + 2])
                return g > 150 && g - r > 60 && g - b > 40
            }.count
        }
        let full = seedCachedTag(id: 1809, title: "Lost")
        let bare = seedCachedTag(id: 4242, title: "Bare Tag", withTracks: false, withSheetMusic: false)
        XCTAssertGreaterThan(greenPixels(full), 50, "Available media draws green checks")
        XCTAssertEqual(greenPixels(bare), 0, "Missing media draws no green at all")
        XCTAssertTrue(TMTagRowContent(tagId: 1809, tag: full).accessibilityLabel.contains("Sheet music available"))
    }

    /// At a sidebar's width the facts line wraps; each line keeps its own space
    /// instead of spilling over the a.k.a. line above it.
    func testATagRowsWrappedFactsNeverOverlapTheLinesAroundThem() throws {
        let tag = seedCachedTag(id: 1809, title: "Lost")
        let content = TMTagRowContent(tagId: 1809, tag: tag)
        mount(UIHostingController(rootView: TMTagRow(content: content).frame(width: 320)),
              size: CGSize(width: 320, height: 240))
        ScreenCatalog.settle(0.2)
        let driver = UIDriver(window)
        let aka = try XCTUnwrap(driver.element(label: try XCTUnwrap(content.aka))).accessibilityFrame
        let facts = try XCTUnwrap(driver.element(label: content.details)).accessibilityFrame
        let sheet = try XCTUnwrap(driver.element(label: "Sheet music")).accessibilityFrame
        XCTAssertGreaterThan(facts.height, aka.height * 1.5, "The facts wrap onto a second line at 320 points")
        XCTAssertGreaterThanOrEqual(facts.minY, aka.maxY - 0.5, "The facts start below the a.k.a. line")
        XCTAssertGreaterThanOrEqual(sheet.minY, facts.maxY - 0.5, "The media marks start below the facts")
    }

    func testATagRowLeavesOutWhatItsTagLacks() {
        let tag = DPTag()
        tag.tagId = 7
        tag.title = "Plain"
        let content = TMTagRowContent(tagId: 7, tag: tag)
        XCTAssertNil(content.aka)
        XCTAssertEqual(content.details, "ID: 7 Posted: Unknown", "No rating or downloads when there are none")
    }

    func testATagRowGrowsWithTheTextSize() {
        let tag = seedCachedTag(id: 1809)
        func height(_ size: DynamicTypeSize) -> CGFloat {
            let host = UIHostingController(rootView: TMTagRow(content: TMTagRowContent(tagId: 1809, tag: tag))
                .environment(\.dynamicTypeSize, size))
            return host.sizeThatFits(in: CGSize(width: 320, height: CGFloat.greatestFiniteMagnitude)).height
        }
        let normal = height(.large)
        XCTAssertGreaterThan(normal, 100)
        XCTAssertLessThan(normal, 260, "Default catalog rows stay dense")
    }

    func testTheSelectionWashIsTheAccentAtItsOldStrength() {
        for (style, alpha) in [(UIUserInterfaceStyle.light, 0.14), (.dark, 0.22)] {
            let color = UIColor(TMTheme.selectionWash).resolvedColor(with: UITraitCollection(userInterfaceStyle: style))
            var components: (CGFloat, CGFloat, CGFloat, CGFloat) = (0, 0, 0, 0)
            color.getRed(&components.0, green: &components.1, blue: &components.2, alpha: &components.3)
            XCTAssertEqual(components.3, alpha, accuracy: 0.001)
        }
    }

    func testTheTagStoreLoadsUncachedRowsOnceAndPublishesThem() {
        let loads = LockedCounter()
        let tag = DPTag()
        tag.tagId = 12
        tag.title = "Fetched"
        let store = TMTagStore(cached: { _ in nil }, load: { id in
            loads.increment()
            return id == 12 ? tag : nil
        })
        XCTAssertNil(store.tag(12))
        XCTAssertTrue(store.isLoading(12))
        XCTAssertNil(store.tag(12), "A second read waits on the same fetch")
        spinUntil("the fetch lands") { !store.isLoading(12) }
        XCTAssertEqual(store.tag(12)?.title, "Fetched")
        XCTAssertNil(store.tag(13))
        spinUntil("the failed fetch settles") { !store.isLoading(13) }
        XCTAssertNil(store.tag(13), "A failed fetch is not retried on every read")
        XCTAssertEqual(loads.value, 2)
    }
}

/// A thread-safe count, for loaders called off the main thread.
final class LockedCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var count = 0
    var value: Int { lock.lock(); defer { lock.unlock() }; return count }
    func increment() { lock.lock(); count += 1; lock.unlock() }
}
