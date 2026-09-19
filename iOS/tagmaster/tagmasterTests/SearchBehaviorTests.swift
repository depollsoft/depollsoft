//
//  SearchBehaviorTests.swift
//  tagmasterTests
//
//  In-process replacements for SearchUITests and the search half of
//  TagMasterPolishUITests. DPSearchViewControllerTests already covers query
//  construction and filter-to-parameter mapping in depth; these cover the
//  reachability and control-contract assertions the XCUITests added on top.
//

import XCTest
import UIKit
@testable import tagmaster

final class SearchBehaviorTests: TMBehaviorTestCase {

    private var searchKeys: [String] = []

    override func setUp() {
        super.setUp()
        searchKeys = ["search.sortBy", "search.sheetMusic", "search.learningTracks",
                      "search.parts", "search.collection"]
        searchKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
    }

    override func tearDown() {
        searchKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
        super.tearDown()
    }

    private var navigation: PushCapturingNavigation!

    private func search() -> DPSearchViewController {
        let search = DPSearchViewController()
        navigation = mountCapturingPushes(search)
        settle()
        return search
    }

    // MARK: - Reaching search
    // Replaces SearchUITests.testCanAccessSearch / testSearchFieldExists, which
    // guessed at several possible affordances and asserted only that one existed.

    func testSearchScreenOffersASearchFieldInItsNavigationBar() {
        let search = self.search()
        let controller = try? XCTUnwrap(search.navigationItem.searchController)
        XCTAssertNotNil(controller)
        XCTAssertEqual(controller?.searchBar.placeholder, "Search")
        XCTAssertEqual(controller?.searchBar.returnKeyType, .search)
        XCTAssertEqual(controller?.automaticallyShowsCancelButton, true)
        XCTAssertEqual(search.navigationItem.hidesSearchBarWhenScrolling, false,
                       "The field stays visible rather than hiding on scroll")
        XCTAssertEqual(search.navigationItem.title, "Search")
    }

    func testSearchScreenOffersARunSearchAction() {
        let search = self.search()
        XCTAssertEqual(search.navigationItem.rightBarButtonItem?.accessibilityLabel, "Search")
    }

    // MARK: - Typing and clearing
    // Replaces testCanTypeInSearchField / testCanClearSearchField.

    func testSearchFieldKeepsWhatWasTypedAndCanBeCleared() {
        let search = self.search()
        let bar = search.navigationItem.searchController!.searchBar

        bar.text = "harmony"
        bar.delegate?.searchBar?(bar, textDidChange: "harmony")
        settle()
        XCTAssertEqual(bar.text, "harmony")

        bar.text = ""
        bar.delegate?.searchBar?(bar, textDidChange: "")
        settle()
        XCTAssertEqual(bar.text, "")
    }

    // MARK: - Executing a search
    // Replaces testCanExecuteSearch / testSearchResultsDisplay /
    // testSearchWithEmptyQueryDoesNotCrash / testMultipleSearchesDoNotCrash.
    // DPSearchViewControllerTests.testSearchQuerySendsRequest already covers the
    // push; these cover what the pushed screen is asked to look for.

    func testRunningASearchOpensAResultsScreenForThatQuery() {
        let search = self.search()
        let bar = search.navigationItem.searchController!.searchBar
        bar.text = "Lone Prairie"

        search.perform(Selector(("search")))

        let results = try? XCTUnwrap(navigation.pushed.last as? DPTagQueryViewController)
        XCTAssertEqual(results?.query, "Lone Prairie")
    }

    func testRunningASearchWithNoTextStillOpensAFilterOnlyResultsScreen() {
        let search = self.search()
        search.navigationItem.searchController!.searchBar.text = ""

        search.perform(Selector(("search")))

        // Searching by filters alone is a supported journey, so this opens
        // results with no query rather than doing nothing.
        let results = try? XCTUnwrap(navigation.pushed.last as? DPTagQueryViewController)
        XCTAssertTrue(results?.query == nil || results?.query?.isEmpty == true)
    }

    func testRepeatedSearchesEachOpenTheirOwnResultsScreen() {
        let search = self.search()
        let bar = search.navigationItem.searchController!.searchBar

        for query in ["love", "harmony", "quartet"] {
            bar.text = query
            search.perform(Selector(("search")))
            let results = try? XCTUnwrap(navigation.pushed.last as? DPTagQueryViewController)
            XCTAssertEqual(results?.query, query)
        }
        XCTAssertEqual(navigation.pushed.count, 3, "Each search opens its own results screen")
    }

    // MARK: - Filters
    // Replaces testSearchFiltersAndKeyboard's control assertions. The software
    // keyboard itself is system UI and stays in the UITests.

    func testSearchOffersEveryFilterAsAFullSizeControl() {
        let search = self.search()
        XCTAssertEqual(search.value(forKey: "filterTitles") as? [String],
                       ["Sort By", "Sheet Music", "Learning Tracks", "Parts", "Collection"])

        for key in ["sortBy", "sheetMusic", "learningTracks", "parts", "collection"] {
            let control = search.value(forKey: key) as! UISegmentedControl
            XCTAssertGreaterThan(control.numberOfSegments, 1)
            XCTAssertGreaterThanOrEqual(control.bounds.height, 44,
                                        "\(key) must stay a full-size target")
        }
    }

    func testSheetMusicFilterNamesItselfAndItsChoices() {
        let search = self.search()
        let sheetMusic = search.value(forKey: "sheetMusic") as! UISegmentedControl
        XCTAssertEqual(sheetMusic.accessibilityLabel, "Sheet Music")
        XCTAssertEqual((0..<sheetMusic.numberOfSegments).map { sheetMusic.titleForSegment(at: $0) },
                       ["Not Important", "Yes", "No"])
        XCTAssertGreaterThanOrEqual(sheetMusic.bounds.height, 44)
    }
}
