//
//  TagListBehaviorTests.swift
//  tagmasterTests
//
//  The screen behind a user-created list: its rows, its empty state, editing,
//  the overflow menu that renames and deletes the list, and what happens when
//  the list disappears from under it.
//

import XCTest
import UIKit
@testable import tagmaster

/// A list screen whose presentations are captured instead of performed, so the
/// rename and delete alerts can be inspected without system keyboard services.
final class ListPresentationFixture: TMTagListController {
    var presented: [UIViewController] = []
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        presented.append(viewControllerToPresent)
        completion?()
    }
}

final class TagListBehaviorTests: TMBehaviorTestCase {

    private static let key = "afterglow-set-k3f9"

    private func seedAfterglow(ids: [Int] = [669, 1478, 122]) {
        ids.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: ids)])
    }

    /// The list screen pushed onto a stack, so the tests that expect it to leave
    /// have somewhere for it to go back to.
    @discardableResult
    private func list(ids: [Int] = [669, 1478, 122]) -> ListPresentationFixture {
        seedAfterglow(ids: ids)
        let list = ListPresentationFixture(listKey: TagListBehaviorTests.key)
        let stack = PushCapturingNavigation(rootViewController: UIViewController())
        stack.pushViewController(list, animated: false)
        mount(stack)
        stack.capturing = true
        navigation = stack
        list.viewWillAppear(false)
        list.viewDidAppear(false)
        settle()
        return list
    }

    private var navigation: PushCapturingNavigation!

    // MARK: - Contents

    func testTheListScreenIsTitledAfterItsListAndShowsItsTagsInOrder() {
        let list = self.list()

        XCTAssertEqual(list.navigationItem.title, "Afterglow set")
        XCTAssertEqual(list.navigationItem.largeTitleDisplayMode, .never)
        XCTAssertEqual(list.tableView.numberOfRows(inSection: 0), 3)
        for (row, tagId) in [669, 1478, 122].enumerated() {
            let cell = list.tableView(list.tableView, cellForRowAt: IndexPath(row: row, section: 0)) as? DPTagCell
            XCTAssertEqual(cell?.tagId, Int32(tagId))
            XCTAssertEqual(cell?.accessoryType, .disclosureIndicator)
        }
        XCTAssertEqual(list.tm_listedTagIds().map(\.intValue), [669, 1478, 122])
    }

    func testTappingARowOpensThatTag() {
        let list = self.list()
        list.tableView(list.tableView, didSelectRowAt: IndexPath(row: 1, section: 0))
        let detail = try? XCTUnwrap(navigation.pushed.last as? DPTagViewController)
        XCTAssertEqual(detail?.tagId, 1478)
    }

    func testAnEmptyListSaysSoAndOffersAWayToFillIt() {
        let list = self.list(ids: [])

        XCTAssertEqual(list.tableView.numberOfRows(inSection: 0), 0)
        let header = try? XCTUnwrap(list.tableView.tableHeaderView)
        XCTAssertNotNil(header.flatMap { label(in: $0, text: "Nothing in Afterglow set yet.") })
        XCTAssertNotNil(header.flatMap {
            label(in: $0, text: "Open a tag and choose Add to list to build this list.")
        })

        let browse = try? XCTUnwrap(header.flatMap { button(in: $0, identifier: "list.browse") })
        XCTAssertEqual(browse?.currentTitle, "Browse Tags")
        XCTAssertGreaterThanOrEqual(browse?.bounds.height ?? 0, 44)
        browse?.sendActions(for: .touchUpInside)
        XCTAssertTrue(navigation.pushed.last is DPBrowseViewController)

        XCTAssertFalse(list.editButtonItem.isEnabled, "There is nothing to edit yet")
    }

    func testAFilledListDropsItsEmptyStateAndEnablesEdit() {
        let list = self.list()
        XCTAssertNil(list.tableView.tableHeaderView)
        XCTAssertTrue(list.editButtonItem.isEnabled)
    }

    // MARK: - Editing the tags in a list

    func testEveryRowCanBeRemovedAndReordered() {
        let list = self.list()
        let table = list.tableView!
        for row in 0..<table.numberOfRows(inSection: 0) {
            XCTAssertTrue(list.tableView(table, canEditRowAt: IndexPath(row: row, section: 0)))
            XCTAssertTrue(list.tableView(table, canMoveRowAt: IndexPath(row: row, section: 0)))
        }
    }

    func testRemovingARowTakesThatTagOutOfTheList() {
        let list = self.list()
        list.tableView(list.tableView, commit: .delete, forRowAt: IndexPath(row: 1, section: 0))

        XCTAssertEqual(TMTagLists.ids(for: TagListBehaviorTests.key), [669, 122])
        XCTAssertEqual(list.tableView.numberOfRows(inSection: 0), 2)
    }

    func testRemovingTheLastRowBringsBackTheEmptyState() {
        let list = self.list(ids: [669])
        list.tableView(list.tableView, commit: .delete, forRowAt: IndexPath(row: 0, section: 0))
        settle()

        XCTAssertEqual(list.tableView.numberOfRows(inSection: 0), 0)
        XCTAssertNotNil(list.tableView.tableHeaderView)
        XCTAssertFalse(list.editButtonItem.isEnabled)
    }

    func testDraggingARowReordersTheStoredList() {
        let list = self.list()
        list.tableView(list.tableView,
                       moveRowAt: IndexPath(row: 0, section: 0),
                       to: IndexPath(row: 2, section: 0))
        XCTAssertEqual(TMTagLists.ids(for: TagListBehaviorTests.key), [1478, 122, 669])
    }

    // MARK: - Managing the list itself

    func testTheOverflowMenuRenamesAndDeletesTheList() {
        let list = self.list()
        let overflow = try? XCTUnwrap(list.navigationItem.rightBarButtonItems?.last)
        XCTAssertEqual(list.navigationItem.rightBarButtonItems?.first, list.editButtonItem)
        XCTAssertEqual(overflow?.accessibilityIdentifier, "list.menu")
        XCTAssertEqual(overflow?.accessibilityLabel, "List options")

        let titles = overflow?.menu?.children.compactMap { ($0 as? UIAction)?.title }
        XCTAssertEqual(titles, ["Rename list…", "Delete list…"])
        XCTAssertEqual((overflow?.menu?.children.last as? UIAction)?.attributes.contains(.destructive), true)
    }

    func testRenamingTheListRetitlesTheScreen() {
        let list = self.list()
        list.promptRename()

        let alert = try? XCTUnwrap(list.presented.last as? UIAlertController)
        XCTAssertEqual(alert?.title, "Rename list")
        XCTAssertEqual(alert?.textFields?.first?.text, "Afterglow set")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Rename"])

        alert?.tm_type("Afterglow encore")
        alert?.tm_fire("Rename")
        settle()

        XCTAssertEqual(TMTagLists.name(for: TagListBehaviorTests.key), "Afterglow encore")
        XCTAssertEqual(list.navigationItem.title, "Afterglow encore")
        list.presented.removeAll()
    }

    func testDeletingTheListConfirmsFirstAndThenLeavesTheScreen() {
        let list = self.list()
        list.confirmDelete()
        let alert = try? XCTUnwrap(list.presented.last as? UIAlertController)
        XCTAssertEqual(alert?.title, "Delete “Afterglow set”?")
        XCTAssertEqual(alert?.message,
                       "This removes the list and its 3 tags from your lists. Tags stay in the catalog.")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Delete"])
        XCTAssertEqual(alert?.actions.last?.style, .destructive)
        XCTAssertEqual(TMTagLists.customKeys(), [TagListBehaviorTests.key])

        alert?.tm_fire("Delete")
        XCTAssertEqual(TMTagLists.customKeys(), [])
        waitUntil("the screen leaves with its list") { !self.navigation.viewControllers.contains(list) }
        list.presented.removeAll()
    }

    func testAnEmptyListSaysSoInItsDeleteConfirmation() {
        let list = self.list(ids: [])
        list.confirmDelete()
        XCTAssertEqual((list.presented.last as? UIAlertController)?.message, "This list is empty.")
        list.presented.removeAll()
    }

    func testAListDeletedElsewhereTakesItsScreenWithIt() {
        let list = self.list()
        // As a sync from another device would: straight through the registry.
        TMTagLists.deleteList(TagListBehaviorTests.key)
        waitUntil("the screen leaves") { !self.navigation.viewControllers.contains(list) }
    }

    func testAListRenamedElsewhereRetitlesTheScreen() {
        let list = self.list()
        TMTagLists.renameList(TagListBehaviorTests.key, to: "Tuesday set")
        settle()
        XCTAssertEqual(list.navigationItem.title, "Tuesday set")
    }

    func testTagsAddedElsewhereAppearInTheList() {
        let list = self.list(ids: [669])
        seedCachedTag(id: 1478, title: "Tag 1478")
        TMTagLists.add(1478, to: TagListBehaviorTests.key)
        settle()
        XCTAssertEqual(list.tableView.numberOfRows(inSection: 0), 2)
    }

    // MARK: - Naming rules
    // The alert never closes on a name the registry would reject.

    private func nameAlert(excluding key: String? = nil) -> UIAlertController {
        key.map { TMListNamePrompt.renameAlert(for: $0) { _ in } }
            ?? TMListNamePrompt.createAlert { _ in }
    }

    private func type(_ text: String, into alert: UIAlertController) {
        alert.tm_type(text)
    }

    func testTheNewListAlertRefusesEveryNameTheRegistryWouldReject() {
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: [])])
        let alert = nameAlert()
        let create = alert.actions.first { $0.title == "Create" }!

        XCTAssertFalse(create.isEnabled, "An empty field cannot create a list")
        XCTAssertEqual(alert.message, "For example “Afterglow set”", "…but is not yet an error")

        for (typed, problem) in [("   ", "Give the list a name."),
                                 ("Afterglow set", "You already have a list with that name."),
                                 ("afterglow SET", "You already have a list with that name."),
                                 ("Favorites", "That name is used by a built-in list."),
                                 ("Teachable Tags", "That name is used by a built-in list."),
                                 (String(repeating: "x", count: 61), "Keep the name under 61 characters.")] {
            type(typed, into: alert)
            XCTAssertFalse(create.isEnabled, "“\(typed)” must not be creatable")
            XCTAssertEqual(alert.message, typed.trimmingCharacters(in: .whitespaces).isEmpty
                           ? "For example “Afterglow set”" : problem)
        }

        type("Chorus warmups", into: alert)
        XCTAssertTrue(create.isEnabled)
        XCTAssertEqual(alert.message, "For example “Afterglow set”", "A usable name shows the hint again")
    }

    func testRenamingAListMayKeepItsOwnName() {
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: [])])
        let alert = nameAlert(excluding: TagListBehaviorTests.key)
        let rename = alert.actions.first { $0.title == "Rename" }!

        XCTAssertTrue(rename.isEnabled, "Its own name is not a duplicate")
        type("Favorites", into: alert)
        XCTAssertFalse(rename.isEnabled)
    }

    func testNamesAreTrimmedAndCollapsedBeforeTheyAreStored() {
        let alert = TMListNamePrompt.createAlert { name in
            XCTAssertEqual(name, "Afterglow set")
            _ = TMTagLists.createList(named: name)
        }
        type("  Afterglow   set  ", into: alert)
        alert.tm_fire("Create")
        XCTAssertEqual(TMTagLists.customKeys().map { TMTagLists.name(for: $0) }, ["Afterglow set"])
    }

    // MARK: - Screenshots

    func testCaptureTheListScreen() {
        let list = self.list()
        list.tableView.reloadData()
        capture("ios-list-screen")

        list.setEditing(true, animated: false)
        settle()
        capture("ios-list-edit")
        XCTAssertTrue(list.isEditing)
        list.setEditing(false, animated: false)
    }
}
