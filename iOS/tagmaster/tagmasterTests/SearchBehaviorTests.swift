//
//  SearchBehaviorTests.swift
//  tagmasterTests
//
//  Search: its field and run action, every option and how it maps onto the
//  catalog query, the options being remembered, and the choice between
//  segments and a menu for each option.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class SearchBehaviorTests: TMBehaviorTestCase {

    private var navigator: RecordingNavigator!

    nonisolated override func setUp() {
        super.setUp()
        MainActor.assumeIsolated {
            TMSearchModel.defaultsKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
            navigator = RecordingNavigator()
        }
    }

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            TMSearchModel.defaultsKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
        }
        super.tearDown()
    }

    private func search() -> (TMHostingController, TMSearchModel, UIDriver) {
        let controller = TMScreens.search(navigator: navigator)
        let driver = mountScreen(controller, size: CGSize(width: 375, height: 1400))
        return (controller, controller.searchModel!, driver)
    }

    // MARK: - Reaching search

    func testSearchScreenOffersASearchFieldInItsNavigationBar() {
        let (controller, _, _) = search()
        let searchController = controller.navigationItem.searchController
        XCTAssertNotNil(searchController, "The field lives in the navigation bar")
        XCTAssertEqual(searchController?.searchBar.placeholder, "Search")
        XCTAssertEqual(controller.navigationItem.hidesSearchBarWhenScrolling, false,
                       "The field stays visible rather than hiding on scroll")
        XCTAssertEqual(controller.navigationItem.title, "Search")
    }

    func testSearchScreenOffersARunSearchAction() {
        let (controller, model, driver) = search()
        XCTAssertEqual(controller.navigationItem.rightBarButtonItem?.accessibilityLabel, "Search")
        model.text = "coney"
        _ = driver
        press(controller.navigationItem.rightBarButtonItem)
        XCTAssertEqual(navigator.destinations, [.results(TMTagQuery(text: "coney", sortBy: DPTagSortTitle))])
    }

    func testTheOptionsAreLabelledAndOffered() {
        let (_, _, driver) = search()
        XCTAssertEqual(TMSearchModel.filters.map(\.title),
                       ["Sort By", "Sheet Music", "Learning Tracks", "Parts", "Collection"])
        XCTAssertEqual(TMSearchModel.filters.map(\.choices), [
            ["Title", "Downloads", "Recent", "Rating"],
            ["Not Important", "Yes", "No"],
            ["Not Important", "Yes", "No"],
            ["Any", "3", "4", "5", "6", "7", "8"],
            ["Any", "Classic Tags", "Easy Tags"],
        ])
        XCTAssertTrue(driver.exists(label: "Search Options"))
        XCTAssertTrue(driver.exists(label: "Searches match titles and lyrics. Leave the field empty to list every tag that matches the options."))
        // At 375pt (iPhone SE) Parts gets 303pt, short of the 308pt its seven 44pt
        // segments need, so it is a menu there, as it was in UIKit; the others are segments.
        let controls = descendants(of: window) { $0 is UISegmentedControl }.compactMap(\.accessibilityLabel)
        XCTAssertEqual(controls, ["Sort By", "Sheet Music", "Learning Tracks", "Collection"], "Each option names itself")
        XCTAssertEqual(driver.elements.first { $0.accessibilityLabel == "Parts" }?.accessibilityValue, "Any")

        let wide = TMScreens.search(navigator: navigator)
        mountScreen(wide, size: CGSize(width: 402, height: 1400))
        XCTAssertEqual(descendants(of: window) { $0 is UISegmentedControl }.compactMap(\.accessibilityLabel),
                       TMSearchModel.filters.map(\.title), "At 402pt every option is a full-size segmented control")
        for control in descendants(of: window, where: { $0 is UISegmentedControl }) {
            XCTAssertGreaterThanOrEqual(control.bounds.height, 44)
        }
    }

    // MARK: - Mapping options onto the query

    func testRunningASearchOpensResultsForTheTextAndTheOptions() {
        let model = TMSearchModel(navigator: navigator)
        model.text = "test query"
        model.search()
        let query = TMTagQuery(text: "test query", sortBy: DPTagSortTitle)
        XCTAssertEqual(navigator.destinations, [.results(query)])
    }

    func testRunningASearchWithNoTextListsEverythingTheOptionsAllow() {
        let model = TMSearchModel(navigator: navigator)
        model.search()
        XCTAssertEqual(navigator.destinations.count, 1)
        XCTAssertEqual(model.query.text, "")
    }

    func testRepeatedSearchesEachOpenTheirOwnResults() {
        let model = TMSearchModel(navigator: navigator)
        for text in ["coney", "lost", "spring"] {
            model.text = text
            model.search()
        }
        XCTAssertEqual(navigator.destinations.count, 3)
        XCTAssertEqual(model.text, "spring", "The typed text stays for when the user comes back")
    }

    func testSortByMapsToTheCatalogsOrders() {
        let model = TMSearchModel()
        for (index, sort) in [DPTagSortTitle, DPTagSortDownloaded, DPTagSortPosted, DPTagSortRating].enumerated() {
            model.selections[0] = index
            XCTAssertEqual(model.query.sortBy.rawValue, sort.rawValue)
        }
    }

    func testSheetMusicAndLearningTracksMapToYesNoOrUnset() {
        let model = TMSearchModel()
        for (index, value) in [nil, true, false].enumerated() {
            model.selections[1] = index
            model.selections[2] = index
            XCTAssertEqual(model.query.sheetMusic, value)
            XCTAssertEqual(model.query.learningTracks, value)
        }
    }

    func testPartsMapToTheNumberOfParts() {
        let model = TMSearchModel()
        model.selections[3] = 0
        XCTAssertNil(model.query.parts, "Any leaves parts unset")
        for index in 1...6 {
            model.selections[3] = index
            XCTAssertEqual(model.query.parts, index + 2)
        }
    }

    func testCollectionMapsToTheCatalogsCollections() {
        let model = TMSearchModel()
        for (index, collection) in [DPTagCollectionNone, DPTagCollectionClassicTags, DPTagCollectionEasyTags].enumerated() {
            model.selections[4] = index
            XCTAssertEqual(model.query.collection.rawValue, collection.rawValue)
        }
    }

    func testTheOptionsAreRememberedBetweenVisits() {
        let model = TMSearchModel()
        model.selections = [2, 1, 2, 3, 1]
        XCTAssertEqual(TMSearchModel.defaultsKeys.map { UserDefaults.standard.integer(forKey: $0) }, [2, 1, 2, 3, 1])
        XCTAssertEqual(TMSearchModel().selections, [2, 1, 2, 3, 1])
    }

    func testTheRealNavigatorPushesAResultsScreen() {
        let search = TMScreens.search()
        let navigation = mountCapturingPushes(search)
        search.searchModel?.text = "coney"
        search.searchModel?.search()
        let results = navigation.pushed.last as? TMHostingController
        XCTAssertEqual((results?.listing as? TMQueryModel)?.query.text, "coney")
    }

    // MARK: - Segments or a menu

    func testFiltersUseSegmentsWhenThereIsRoomAndAMenuWhenThereIsNot() {
        var parts = 3
        let filter = TMSearchModel.filters[3]
        func mountRow(width: CGFloat, size: DynamicTypeSize = .large) -> UIView {
            let host = UIHostingController(rootView: TMFilterRow(filter: filter, selection: Binding(get: { parts }, set: { parts = $0 }))
                .environment(\.dynamicTypeSize, size)
                .frame(width: width))
            mount(host, size: CGSize(width: width, height: 200))
            ScreenCatalog.settle(0.2)
            return host.view
        }
        let wide = mountRow(width: 360)
        let segments = firstDescendant(of: wide) { $0 is UISegmentedControl } as? UISegmentedControl
        XCTAssertNotNil(segments, "Seven 44pt targets fit in 360pt")
        XCTAssertEqual(segments?.selectedSegmentIndex, 3)
        XCTAssertGreaterThanOrEqual(segments?.bounds.height ?? 0, 44, "Parts stays a full-size target")
        for index in 0..<(segments?.numberOfSegments ?? 0) {
            XCTAssertGreaterThanOrEqual(segments?.widthForSegment(at: index) ?? 0, 44)
        }
        segments?.selectedSegmentIndex = 5
        segments?.sendActions(for: .valueChanged)
        XCTAssertEqual(parts, 5, "The segments and the selection are one")

        let narrow = mountRow(width: 280)
        XCTAssertNil(firstDescendant(of: narrow) { $0 is UISegmentedControl }, "Below 308pt the segments give way")
        XCTAssertTrue(UIDriver(window).exists(label: "Parts"), "…to a menu that still names the option")
        XCTAssertEqual(UIDriver(window).elements.first { $0.accessibilityLabel == "Parts" }?.accessibilityValue, "7")

        let large = mountRow(width: 700, size: .accessibility3)
        XCTAssertNil(firstDescendant(of: large) { $0 is UISegmentedControl }, "Accessibility sizes always use the menu")
    }
}
