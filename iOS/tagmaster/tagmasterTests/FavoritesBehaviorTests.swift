//
//  FavoritesBehaviorTests.swift
//  tagmasterTests
//
//  In-process replacements for FavoritesUITests and the home-screen half of
//  tagmasterUITests. The XCUITests seeded favourites with the launch argument
//  `-depollsoft.pitchperfect.lists`, which is only a UserDefaults override, so
//  these tests write the same key and drive the real DPHomeViewController.
//
//  Home now has three groups - destinations, Lists, Favorites - so the same
//  tests also cover the Lists group: its rows, its editing and its menus.
//

import XCTest
import UIKit
@testable import tagmaster

/// Home's sections, as `TMHomeSection` names them in DPHomeViewController.h.
private let navigationSection = 0
private let listsSection = 1
private let favoritesSection = 2

final class FavoritesBehaviorTests: TMBehaviorTestCase {

    private var navigation: PushCapturingNavigation!

    @discardableResult
    private func home(favorites: [Int] = [],
                      lists: [(key: String, name: String, ids: [Int])] = []) -> DPHomeViewController {
        // Every favourite row must resolve from the cache. An uncached row sends
        // DPTagCell to the live catalog and reloads its row from the completion,
        // which would land in whichever test is running by then.
        favorites.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
        if !favorites.isEmpty || !lists.isEmpty { seedLists(favorite: favorites, lists: lists) }
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

    private func detailTitles(_ home: DPHomeViewController, section: Int) -> [String?] {
        let table = home.tableView!
        return (0..<table.numberOfRows(inSection: section)).map {
            home.tableView(table, cellForRowAt: IndexPath(row: $0, section: section)).detailTextLabel?.text
        }
    }

    private static let twoLists: [(key: String, name: String, ids: [Int])] = [
        (key: "afterglow-set-k3f9", name: "Afterglow set", ids: [669, 1478]),
        (key: "chorus-warmups-aa12", name: "Chorus warmups", ids: [122])
    ]

    // MARK: - Home list contents
    // Replaces tagmasterUITests.testAppHasContent / testMainContentExists /
    // testFavoritesOrListExists and FavoritesUITests.testHasListOrContent /
    // testFavoritesLabelExists, which only asserted that *something* existed.

    func testHomeListsEveryNavigationRowInOrder() {
        let home = self.home()
        XCTAssertEqual(home.numberOfSections(in: home.tableView), 3)
        XCTAssertEqual(rowTitles(home, section: navigationSection),
                       ["Browse", "Random Tag", "Open Tag", "Settings"])
    }

    func testHomeNamesItsGroups() {
        let home = self.home(favorites: [669])
        XCTAssertNil(home.tableView(home.tableView, titleForHeaderInSection: navigationSection))
        XCTAssertEqual(home.tableView(home.tableView, titleForHeaderInSection: listsSection), "Lists")
        XCTAssertEqual(home.tableView(home.tableView, titleForHeaderInSection: favoritesSection), "Favorites")
    }

    func testHomeExplainsHowToAddFavoritesWhenThereAreNone() {
        let home = self.home()
        XCTAssertEqual(home.tableView.numberOfRows(inSection: favoritesSection), 0)
        XCTAssertEqual(home.tableView(home.tableView, titleForFooterInSection: favoritesSection),
                       "No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite.")
    }

    // MARK: - The Lists group

    func testListsGroupHoldsTeachableThenEveryCustomListThenNewList() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)

        XCTAssertEqual(rowTitles(home, section: listsSection),
                       ["Teachable Tags", "Afterglow set", "Chorus warmups", "New list…"])
        XCTAssertEqual(detailTitles(home, section: listsSection), ["0 tags", "2 tags", "1 tag", nil])
    }

    func testListsGroupIsJustTeachableAndNewListWhenNoListsExist() {
        let home = self.home()
        XCTAssertEqual(rowTitles(home, section: listsSection), ["Teachable Tags", "New list…"])
    }

    func testEveryListsRowIsIdentifiedAndReachable() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)
        let table = home.tableView!
        let identifiers = (0..<table.numberOfRows(inSection: listsSection)).map {
            home.tableView(table, cellForRowAt: IndexPath(row: $0, section: listsSection)).accessibilityIdentifier
        }
        XCTAssertEqual(identifiers, ["home.lists.teachable",
                                     "home.list.afterglow-set-k3f9",
                                     "home.list.chorus-warmups-aa12",
                                     "home.lists.new"])
        let newList = home.tableView(table, cellForRowAt: IndexPath(row: 3, section: listsSection))
        XCTAssertNotNil(newList.imageView?.image, "New list carries the plus symbol")
    }

    func testTappingTeachableAndACustomListOpensThoseScreens() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)

        home.tableView(home.tableView, didSelectRowAt: IndexPath(row: 0, section: listsSection))
        XCTAssertTrue(navigation.pushed.last is DPTeachableTagsController)

        home.tableView(home.tableView, didSelectRowAt: IndexPath(row: 2, section: listsSection))
        let list = try? XCTUnwrap(navigation.pushed.last as? TMTagListController)
        XCTAssertEqual(list?.listKey, "chorus-warmups-aa12")
    }

    func testOnlyCustomListRowsCanBeEditedOrMoved() {
        let home = self.home(favorites: [669], lists: FavoritesBehaviorTests.twoLists)
        let table = home.tableView!

        for row in 0..<table.numberOfRows(inSection: navigationSection) {
            let path = IndexPath(row: row, section: navigationSection)
            XCTAssertFalse(home.tableView(table, canEditRowAt: path))
            XCTAssertFalse(home.tableView(table, canMoveRowAt: path))
        }
        // Teachable Tags (0) and New list (3) are permanent.
        for (row, editable) in [(0, false), (1, true), (2, true), (3, false)] {
            let path = IndexPath(row: row, section: listsSection)
            XCTAssertEqual(home.tableView(table, canEditRowAt: path), editable, "Lists row \(row)")
            XCTAssertEqual(home.tableView(table, canMoveRowAt: path), editable, "Lists row \(row)")
        }
        XCTAssertTrue(home.tableView(table, canEditRowAt: IndexPath(row: 0, section: favoritesSection)))
    }

    func testDraggingAListStaysBetweenTeachableAndNewList() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)
        let source = IndexPath(row: 2, section: listsSection)

        for proposed in [IndexPath(row: 0, section: listsSection),
                         IndexPath(row: 2, section: navigationSection)] {
            XCTAssertEqual(home.tableView(home.tableView, targetIndexPathForMoveFromRowAt: source,
                                          toProposedIndexPath: proposed),
                           IndexPath(row: 1, section: listsSection))
        }
        for proposed in [IndexPath(row: 3, section: listsSection),
                         IndexPath(row: 0, section: favoritesSection)] {
            XCTAssertEqual(home.tableView(home.tableView, targetIndexPathForMoveFromRowAt: source,
                                          toProposedIndexPath: proposed),
                           IndexPath(row: 2, section: listsSection))
        }
    }

    func testReorderingAListRowReordersTheStoredLists() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)
        home.tableView(home.tableView,
                       moveRowAt: IndexPath(row: 1, section: listsSection),
                       to: IndexPath(row: 2, section: listsSection))

        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-aa12", "afterglow-set-k3f9"])
        home.tableView.reloadData()
        XCTAssertEqual(rowTitles(home, section: listsSection),
                       ["Teachable Tags", "Chorus warmups", "Afterglow set", "New list…"])
    }

    func testDeletingAListRowAsksBeforeAnythingIsLost() {
        let home = HomePresentationFixture(style: .grouped)
        seedLists(lists: FavoritesBehaviorTests.twoLists)
        mountCapturingPushes(home)
        home.viewDidAppear(false)
        settle()

        home.tableView(home.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: listsSection))
        let alert = try? XCTUnwrap(home.requestedPresentation as? UIAlertController)
        XCTAssertEqual(alert?.title, "Delete “Afterglow set”?")
        XCTAssertEqual(alert?.message,
                       "This removes the list and its 2 tags from your lists. Tags stay in the catalog.")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Delete"])
        XCTAssertEqual(alert?.actions.last?.style, .destructive)
        XCTAssertEqual(TMTagLists.customKeys().count, 2, "Nothing goes before the confirmation is answered")

        alert?.tm_fire("Delete")
        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-aa12"])
        home.requestedPresentation = nil
    }

    func testASwipedListRowStaysOpenUnderTheConfirmationAndClosesOnlyOnce() throws {
        let home = HomePresentationFixture(style: .grouped)
        seedLists(lists: FavoritesBehaviorTests.twoLists)
        mountCapturingPushes(home)
        home.viewDidAppear(false)
        settle()
        // What a swipe leaves behind: the table is editing a row although the
        // screen as a whole is not.
        home.tableView.setEditing(true, animated: false)

        home.tableView(home.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: listsSection))
        XCTAssertTrue(home.tableView.isEditing,
                      "The swipe holds its place while the confirmation is up")
        XCTAssertFalse(home.isEditing)

        let alert = try XCTUnwrap(home.requestedPresentation as? UIAlertController)
        alert.tm_fire("Cancel")
        settle()
        XCTAssertFalse(home.tableView.isEditing, "Answering the alert closes the swipe")
        XCTAssertEqual(TMTagLists.customKeys().count, 2, "Cancel keeps the list")
        home.requestedPresentation = nil
    }

    func testDeletingFromTheEditButtonLeavesEditModeAlone() throws {
        let home = HomePresentationFixture(style: .grouped)
        seedLists(lists: FavoritesBehaviorTests.twoLists)
        mountCapturingPushes(home)
        home.viewDidAppear(false)
        settle()
        home.setEditing(true, animated: false)

        home.tableView(home.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: listsSection))
        let alert = try XCTUnwrap(home.requestedPresentation as? UIAlertController)
        alert.tm_fire("Delete")
        settle()

        XCTAssertTrue(home.isEditing, "A red-circle delete does not end the edit session")
        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-aa12"])
        home.setEditing(false, animated: false)
        home.requestedPresentation = nil
    }

    func testAListRowOffersRenameAndDeleteFromItsContextMenu() {
        let home = self.home(lists: FavoritesBehaviorTests.twoLists)
        XCTAssertNotNil(home.tableView(home.tableView,
                                       contextMenuConfigurationForRowAt: IndexPath(row: 1, section: listsSection),
                                       point: .zero))
        let menu = home.tm_menu(forListKey: "afterglow-set-k3f9")
        XCTAssertEqual(menu?.children.compactMap { ($0 as? UIAction)?.title }, ["Rename…", "Delete…"])
        XCTAssertEqual((menu?.children.last as? UIAction)?.attributes.contains(.destructive), true)

        XCTAssertNil(home.tableView(home.tableView,
                                    contextMenuConfigurationForRowAt: IndexPath(row: 0, section: listsSection),
                                    point: .zero),
                     "Teachable Tags cannot be renamed or deleted")
        XCTAssertNil(home.tableView(home.tableView,
                                    contextMenuConfigurationForRowAt: IndexPath(row: 3, section: listsSection),
                                    point: .zero))
    }

    func testNewListRowNamesAndCreatesTheListInPlace() {
        let home = HomePresentationFixture(style: .grouped)
        mountCapturingPushes(home)
        home.viewDidAppear(false)
        settle()

        home.tableView(home.tableView, didSelectRowAt: IndexPath(row: 1, section: listsSection))
        let alert = try? XCTUnwrap(home.requestedPresentation as? UIAlertController)
        XCTAssertEqual(alert?.title, "New list")
        XCTAssertEqual(alert?.message, "For example “Easy tags” or “High and lows”")
        XCTAssertEqual(alert?.textFields?.first?.placeholder, "List name")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Create"])

        alert?.tm_type("Afterglow set")
        alert?.tm_fire("Create")
        XCTAssertEqual(TMTagLists.customKeys().map { TMTagLists.name(for: $0) }, ["Afterglow set"])
        XCTAssertEqual(rowTitles(home, section: listsSection),
                       ["Teachable Tags", "Afterglow set", "New list…"])
        home.requestedPresentation = nil
    }

    func testRenamingAListFromHomeRewritesItsRow() {
        let home = HomePresentationFixture(style: .grouped)
        seedLists(lists: FavoritesBehaviorTests.twoLists)
        mountCapturingPushes(home)
        home.viewDidAppear(false)
        settle()

        home.promptRenameList("afterglow-set-k3f9")
        let alert = try? XCTUnwrap(home.requestedPresentation as? UIAlertController)
        XCTAssertEqual(alert?.title, "Rename list")
        XCTAssertEqual(alert?.textFields?.first?.text, "Afterglow set")

        alert?.tm_type("Afterglow encore")
        alert?.tm_fire("Rename")
        XCTAssertEqual(rowTitles(home, section: listsSection),
                       ["Teachable Tags", "Afterglow encore", "Chorus warmups", "New list…"])
        home.requestedPresentation = nil
    }

    // MARK: - Seeded favourites
    // Replaces FavoritesUITests' reliance on the launch-argument fixture.

    func testFavoritesSeededInDefaultsBecomeHomeRowsInOrder() {
        let seeded = [669, 1478, 122]
        let home = self.home(favorites: seeded)

        XCTAssertEqual(DPAppDelegate.favorites(), seeded,
                       "The lists defaults key is the app's own favourites storage")
        XCTAssertEqual(home.tableView.numberOfRows(inSection: favoritesSection), seeded.count)

        for (row, tagId) in seeded.enumerated() {
            let cell = home.tableView(home.tableView, cellForRowAt: IndexPath(row: row, section: favoritesSection))
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
        let cell = home.tableView(home.tableView, cellForRowAt: IndexPath(row: 0, section: favoritesSection))

        XCTAssertEqual(cell.accessibilityLabel?.contains("Cheer Up, Charlie"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Tag ID 669"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Sheet music available"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Learning tracks available"), true)
    }

    // MARK: - Editing
    // Replaces FavoritesUITests.testSwipeOnFavoriteIfExists, which only checked
    // that a swipe did not crash.

    func testDeletingAFavoriteRowRemovesThatFavorite() {
        let home = self.home(favorites: [669, 1478, 122])
        home.tableView(home.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: favoritesSection))

        XCTAssertEqual(DPAppDelegate.favorites(), [669, 122])
        home.tableView.reloadData()
        XCTAssertEqual(home.tableView.numberOfRows(inSection: favoritesSection), 2)
    }

    func testReorderingAFavoriteRowReordersTheStoredList() {
        let home = self.home(favorites: [669, 1478, 122])
        home.tableView(home.tableView,
                       moveRowAt: IndexPath(row: 0, section: favoritesSection),
                       to: IndexPath(row: 2, section: favoritesSection))
        XCTAssertEqual(DPAppDelegate.favorites(), [1478, 122, 669])
    }

    func testDraggingAFavoriteIntoAnotherGroupKeepsItInFavorites() {
        let home = self.home(favorites: [669, 1478])
        let target = home.tableView(home.tableView,
                                    targetIndexPathForMoveFromRowAt: IndexPath(row: 1, section: favoritesSection),
                                    toProposedIndexPath: IndexPath(row: 2, section: navigationSection))
        XCTAssertEqual(target, IndexPath(row: 0, section: favoritesSection))
    }

    func testEditButtonIsEnabledForFavoritesOrForListsOfTheirOwn() {
        let empty = self.home()
        XCTAssertFalse(empty.editButtonItem.isEnabled)

        let favorited = self.home(favorites: [669])
        XCTAssertTrue(favorited.editButtonItem.isEnabled)

        let listed = self.home(lists: FavoritesBehaviorTests.twoLists)
        XCTAssertTrue(listed.editButtonItem.isEnabled, "Lists alone are worth an Edit button")
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
            (3, DPSettingsController.self)
        ]
        for (row, destination) in expected {
            home.tableView(home.tableView, didSelectRowAt: IndexPath(row: row, section: navigationSection))
            XCTAssertTrue(type(of: navigation.pushed.last!) == destination,
                          "Row \(row) should open \(destination)")
        }
        XCTAssertEqual(navigation.pushed.count, expected.count)
    }

    func testTappingAFavoriteOpensThatTag() {
        let home = self.home(favorites: [669, 1478])

        home.tableView(home.tableView, didSelectRowAt: IndexPath(row: 0, section: favoritesSection))

        let detail = try? XCTUnwrap(navigation.pushed.last as? DPTagViewController)
        XCTAssertEqual(detail?.tagId, 669)
    }

    // MARK: - Reacting to changes elsewhere

    func testHomeReportsItsFavoritesAsTheListSourceForStepping() {
        let home = self.home(favorites: [669, 1478, 122])
        XCTAssertEqual(home.tm_listedTagIds().map(\.intValue), [669, 1478, 122])
    }

    func testAListAddedElsewhereAppearsOnHome() {
        let home = self.home(lists: [FavoritesBehaviorTests.twoLists[0]])
        _ = TMTagLists.createList(named: "Chorus warmups")
        settle()

        XCTAssertEqual(rowTitles(home, section: listsSection),
                       ["Teachable Tags", "Afterglow set", "Chorus warmups", "New list…"])
    }

    // MARK: - Screenshot

    func testCaptureHomeWithLists() {
        let home = self.home(favorites: [669, 1478], lists: FavoritesBehaviorTests.twoLists)
        home.tableView.reloadData()
        capture("ios-home-lists")
        XCTAssertEqual(home.numberOfSections(in: home.tableView), 3)
    }
}

/// Home, with its presentations captured instead of performed, so the alerts
/// the list rows raise can be inspected without system keyboard services.
final class HomePresentationFixture: DPHomeViewController {
    var requestedPresentation: UIViewController?

    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        requestedPresentation = viewControllerToPresent
        completion?()
    }
}

extension UIAlertController {
    /// Types into the alert's field the way the keyboard would, so the
    /// validation that gates the confirming action actually runs.
    func tm_type(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let field = textFields?.first else {
            XCTFail("This alert has no text field", file: file, line: line)
            return
        }
        field.text = text
        field.sendActions(for: .editingChanged)
    }

    /// Runs the handler of the named action, the way tapping it would.
    func tm_fire(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let action = actions.first(where: { $0.title == title }) else {
            XCTFail("No “\(title)” action in this alert", file: file, line: line)
            return
        }
        XCTAssertTrue(action.isEnabled, "“\(title)” is disabled", file: file, line: line)
        typealias Handler = @convention(block) (UIAlertAction) -> Void
        guard action.responds(to: Selector(("handler"))), let raw = action.value(forKey: "handler") else {
            XCTFail("UIAlertAction no longer exposes its handler", file: file, line: line)
            return
        }
        let handler = unsafeBitCast(raw as AnyObject, to: Handler.self)
        handler(action)
    }
}
