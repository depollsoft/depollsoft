//
//  FavoritesBehaviorTests.swift
//  tagmasterTests
//
//  In-process replacements for FavoritesUITests and the home-screen half of
//  tagmasterUITests. The XCUITests seeded favourites with the launch argument
//  `-depollsoft.pitchperfect.lists`, which is only a UserDefaults override, so
//  these tests write the same key and drive the real DPHomeViewController.
//

import XCTest
import UIKit
@testable import tagmaster

final class FavoritesBehaviorTests: TMBehaviorTestCase {

    private var navigation: PushCapturingNavigation!

    @discardableResult
    private func home(favorites: [Int] = []) -> DPHomeViewController {
        // Every favourite row must resolve from the cache. An uncached row sends
        // DPTagCell to the live catalog and reloads its row from the completion,
        // which would land in whichever test is running by then.
        favorites.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
        if !favorites.isEmpty { seedLists(favorite: favorites) }
        let home = DPHomeViewController(style: .grouped)
        navigation = mountCapturingPushes(home)
        home.viewWillAppear(false)
        home.viewDidAppear(false)
        settle()
        return home
    }

    private func rowTitles(_ home: DPHomeViewController, section: Int) -> [String] {
        let table = home.tableView!
        return (0..<table.numberOfRows(inSection: section)).map {
            home.tableView(table, cellForRowAt: IndexPath(row: $0, section: section)).textLabel?.text ?? ""
        }
    }

    // MARK: - Home list contents
    // Replaces tagmasterUITests.testAppHasContent / testMainContentExists /
    // testFavoritesOrListExists and FavoritesUITests.testHasListOrContent /
    // testFavoritesLabelExists, which only asserted that *something* existed.

    func testHomeListsEveryNavigationRowInOrder() {
        let home = self.home()
        XCTAssertEqual(home.numberOfSections(in: home.tableView), 2)
        XCTAssertEqual(rowTitles(home, section: 0),
                       ["Browse", "Teachable Tags", "Random Tag", "Open Tag", "Settings"])
    }

    func testHomeNamesItsFavoritesSection() {
        let home = self.home(favorites: [669])
        XCTAssertEqual(home.tableView(home.tableView, titleForHeaderInSection: 1), "Favorites")
        XCTAssertNil(home.tableView(home.tableView, titleForHeaderInSection: 0))
    }

    func testHomeExplainsHowToAddFavoritesWhenThereAreNone() {
        let home = self.home()
        XCTAssertEqual(home.tableView.numberOfRows(inSection: 1), 0)
        XCTAssertEqual(home.tableView(home.tableView, titleForFooterInSection: 1),
                       "No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite.")
    }

    // MARK: - Seeded favourites
    // Replaces FavoritesUITests' reliance on the launch-argument fixture.

    func testFavoritesSeededInDefaultsBecomeHomeRowsInOrder() {
        let seeded = [669, 1478, 122]
        let home = self.home(favorites: seeded)

        XCTAssertEqual(DPAppDelegate.favorites(), seeded,
                       "The lists defaults key is the app's own favourites storage")
        XCTAssertEqual(home.tableView.numberOfRows(inSection: 1), seeded.count)

        for (row, tagId) in seeded.enumerated() {
            let cell = home.tableView(home.tableView, cellForRowAt: IndexPath(row: row, section: 1))
            let tagCell = try? XCTUnwrap(cell as? DPTagCell)
            XCTAssertEqual(tagCell?.tagId, Int32(tagId), "Row \(row) is bound to tag \(tagId)")
            XCTAssertEqual(cell.accessoryType, .disclosureIndicator)
        }
    }

    func testAFavoriteRowAnnouncesItsTagBeforeTheTagHasLoaded() {
        let tagId: Int32 = 990_001
        seedCachedTag(id: tagId)
        let cell = DPTagCell(style: .default, reuseIdentifier: "Tag")
        cell.tagId = tagId
        // Back to the not-yet-loaded state the row starts in.
        cell.tagInstance = nil

        XCTAssertEqual(cell.accessibilityLabel, "Tag 990001. Open to load details.")
    }

    func testFavoriteRowDescribesItsTagOnceLoadedFromCache() {
        let home = self.home(favorites: [669])
        seedCachedTag(id: 669, title: "Cheer Up, Charlie")
        home.tableView.reloadData()
        let cell = home.tableView(home.tableView, cellForRowAt: IndexPath(row: 0, section: 1))

        XCTAssertEqual(cell.accessibilityLabel?.contains("Cheer Up, Charlie"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Tag ID 669"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Sheet music available"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Learning tracks available"), true)
    }

    // MARK: - Editing
    // Replaces FavoritesUITests.testSwipeOnFavoriteIfExists, which only checked
    // that a swipe did not crash.

    func testOnlyFavoriteRowsCanBeEditedAndReordered() {
        let home = self.home(favorites: [669, 1478])
        let table = home.tableView!

        for row in 0..<table.numberOfRows(inSection: 0) {
            let path = IndexPath(row: row, section: 0)
            XCTAssertFalse(home.tableView(table, canEditRowAt: path))
            XCTAssertFalse(home.tableView(table, canMoveRowAt: path))
        }
        for row in 0..<table.numberOfRows(inSection: 1) {
            let path = IndexPath(row: row, section: 1)
            XCTAssertTrue(home.tableView(table, canEditRowAt: path))
            XCTAssertTrue(home.tableView(table, canMoveRowAt: path))
        }
    }

    func testDeletingAFavoriteRowRemovesThatFavorite() {
        let home = self.home(favorites: [669, 1478, 122])
        home.tableView(home.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: 1))

        XCTAssertEqual(DPAppDelegate.favorites(), [669, 122])
        home.tableView.reloadData()
        XCTAssertEqual(home.tableView.numberOfRows(inSection: 1), 2)
    }

    func testReorderingAFavoriteRowReordersTheStoredList() {
        let home = self.home(favorites: [669, 1478, 122])
        home.tableView(home.tableView,
                       moveRowAt: IndexPath(row: 0, section: 1),
                       to: IndexPath(row: 2, section: 1))
        XCTAssertEqual(DPAppDelegate.favorites(), [1478, 122, 669])
    }

    func testDraggingAFavoriteIntoTheNavigationSectionKeepsItInFavorites() {
        let home = self.home(favorites: [669, 1478])
        let target = home.tableView(home.tableView,
                                    targetIndexPathForMoveFromRowAt: IndexPath(row: 1, section: 1),
                                    toProposedIndexPath: IndexPath(row: 2, section: 0))
        XCTAssertEqual(target, IndexPath(row: 0, section: 1))
    }

    func testEditButtonIsOnlyEnabledWhenThereAreFavoritesToEdit() {
        let empty = self.home()
        XCTAssertFalse(empty.editButtonItem.isEnabled)

        let populated = self.home(favorites: [669])
        XCTAssertTrue(populated.editButtonItem.isEnabled)
    }

    // MARK: - Navigation
    // Replaces tagmasterUITests.testCanTapContentIfExists /
    // testSearchButtonOrFieldExists / testCanInteractWithSearch and
    // FavoritesUITests.testCanTapFavoriteIfExists, all of which only asserted
    // that the app stayed in the foreground.

    func testHomeOffersSearchInItsNavigationBar() {
        let home = self.home()
        let search = home.navigationItem.rightBarButtonItem
        XCTAssertNotNil(search)
        XCTAssertEqual(search?.accessibilityLabel, "Search")
        XCTAssertEqual(home.navigationItem.title, "Tag Master")
        XCTAssertEqual(home.navigationItem.leftBarButtonItem, home.editButtonItem)
    }

    func testTappingSearchOpensTheSearchScreen() {
        let home = self.home()
        let search = home.navigationItem.rightBarButtonItem!

        _ = search.target?.perform(search.action, with: search)

        XCTAssertTrue(navigation.pushed.last is DPSearchViewController)
    }

    func testEachNavigationRowOpensItsOwnScreen() {
        let home = self.home()
        let expected: [(row: Int, destination: AnyClass)] = [
            (0, DPBrowseViewController.self),
            (1, DPTeachableTagsController.self),
            (4, DPSettingsController.self)
        ]
        for (row, destination) in expected {
            home.tableView(home.tableView, didSelectRowAt: IndexPath(row: row, section: 0))
            XCTAssertTrue(type(of: navigation.pushed.last!) == destination,
                          "Row \(row) should open \(destination)")
        }
        XCTAssertEqual(navigation.pushed.count, expected.count)
    }

    func testTappingAFavoriteOpensThatTag() {
        let home = self.home(favorites: [669, 1478])

        home.tableView(home.tableView, didSelectRowAt: IndexPath(row: 0, section: 1))

        let detail = try? XCTUnwrap(navigation.pushed.last as? DPTagViewController)
        XCTAssertEqual(detail?.tagId, 669)
    }

    // MARK: - Reacting to changes elsewhere

    func testHomeReportsItsFavoritesAsTheListSourceForStepping() {
        let home = self.home(favorites: [669, 1478, 122])
        XCTAssertEqual(home.tm_listedTagIds().map(\.intValue), [669, 1478, 122])
    }
}
