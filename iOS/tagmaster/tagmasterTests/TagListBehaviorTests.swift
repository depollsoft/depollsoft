//
//  TagListBehaviorTests.swift
//  tagmasterTests
//
//  The screen behind a list: Teachable Tags and the lists users make. Its rows,
//  its empty state, editing, the overflow menu that renames and deletes a
//  list, what happens when the list disappears from under it, and the naming
//  rules every list alert shares.
//

import XCTest
import UIKit
@testable import tagmaster

@MainActor
final class TagListBehaviorTests: TMBehaviorTestCase {

    private static let key = "afterglow-set-k3f9"

    private var navigator: RecordingNavigator!
    private var controller: TMHostedScreen!
    private var driver: UIDriver!
    private var model: TMTagListModel { controller.listing as! TMTagListModel }

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            if window != nil { dismissPresented() }
            driver = nil
            controller = nil
            navigator = nil
        }
        super.tearDown()
    }

    private func seedAfterglow(ids: [Int] = [669, 1478, 122]) {
        ids.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: ids)])
    }

    @discardableResult
    private func list(ids: [Int] = [669, 1478, 122], kind: TMTagListModel.Kind = .custom(key)) -> TMTagListModel {
        if kind == .teachable {
            ids.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
            seedLists(teachable: ids)
        } else {
            seedAfterglow(ids: ids)
        }
        navigator = RecordingNavigator()
        controller = TMScreens.tagList(kind, navigator: navigator)
        driver = mountScreen(controller)
        return model
    }

    // MARK: - Contents

    func testTheListScreenIsTitledAfterItsListAndShowsItsTagsInOrder() {
        list()
        XCTAssertEqual(controller.navigationItem.title, "Afterglow set")
        XCTAssertEqual(controller.navigationItem.largeTitleDisplayMode, .never)
        XCTAssertEqual(controller.navigationItem.backButtonTitle, "List")
        XCTAssertEqual(model.ids, [669, 1478, 122])
        let rows = driver.elements(labelPrefix: "Tag ").compactMap(\.accessibilityLabel)
        XCTAssertEqual(rows.count, 3)
        for (row, tagId) in [669, 1478, 122].enumerated() {
            XCTAssertTrue(rows[row].contains("Tag ID \(tagId)"))
        }
        XCTAssertEqual(controller.tm_listedTagIds().map(\.intValue), [669, 1478, 122])
    }

    func testTeachableTagsIsTheSameScreenOverTheTeachableList() {
        list(ids: [122, 669], kind: .teachable)
        XCTAssertEqual(controller.navigationItem.title, "Teachable Tags")
        XCTAssertEqual(controller.navigationItem.backButtonTitle, "Teachable")
        XCTAssertTrue(driver.exists(label: "Edit"))
        XCTAssertFalse(driver.exists(id: "list.menu"), "Teachable Tags cannot be renamed or deleted")
        XCTAssertEqual(model.ids, [122, 669])
        model.promptRename()
        model.confirmDelete()
        XCTAssertNil(model.namePrompt)
        XCTAssertNil(model.deletePrompt)
    }

    func testTappingARowOpensThatTag() {
        list()
        driver.elements(labelPrefix: "Tag 1478").first?.accessibilityActivate()
        ScreenCatalog.settle(0.05)
        XCTAssertEqual(navigator.shownTags, [1478])
    }

    func testTheRealNavigatorOpensTheTagOnThePhoneStack() {
        seedAfterglow()
        let list = TMScreens.list(key: TagListBehaviorTests.key)
        mountInNavigation(list)
        ScreenCatalog.settle(0.2)
        UIDriver(window).elements(labelPrefix: "Tag 1478").first?.accessibilityActivate()
        let detail = list.router?.path.last?.tagModel
        XCTAssertEqual(detail?.tagId, 1478)
        XCTAssertEqual(detail?.source?.tm_listedTagIds().map(\.intValue), [669, 1478, 122],
                       "The pushed tag steps through the list it came from")
    }

    func testAnEmptyListSaysSoAndOffersAWayToFillIt() {
        list(ids: [])
        XCTAssertTrue(driver.exists(label: "No tags in Afterglow set yet."))
        XCTAssertTrue(driver.traits(id: "list.empty.title").contains(.header))
        XCTAssertTrue(driver.exists(label: "Open any tag and choose Add to list."))
        XCTAssertEqual(driver.label(id: "list.browse"), "Browse Tags")
        driver.tap(id: "list.browse")
        XCTAssertEqual(navigator.destinations, [.browse])
        XCTAssertFalse(driver.isEnabled(label: "Edit"), "There is nothing to edit yet")
    }

    func testAnEmptyTeachableListNamesItselfAndOffersBrowse() {
        list(ids: [], kind: .teachable)
        XCTAssertTrue(driver.exists(label: "No teachable tags yet"))
        XCTAssertTrue(driver.exists(label: "Open a tag, choose Favorite and Teachable options, then Mark as Teachable. Your teaching list will appear here."))
        driver.tap(id: "teachable.browse")
        XCTAssertEqual(navigator.destinations, [.browse])
    }

    func testAFilledListDropsItsEmptyStateAndEnablesEdit() {
        list()
        XCTAssertFalse(driver.exists(id: "list.browse"))
        XCTAssertTrue(driver.isEnabled(label: "Edit"))
    }

    // MARK: - Editing the tags in a list

    func testTheEditButtonDrivesTheListsEditMode() {
        list()
        driver.tap(label: "Edit")
        XCTAssertTrue(model.isEditing)
        XCTAssertTrue(driver.exists(label: "Done"))
        driver.tap(label: "Done")
        XCTAssertFalse(model.isEditing)
    }

    func testRemovingARowTakesThatTagOutOfTheList() {
        list()
        model.remove(at: [1])
        XCTAssertEqual(TMTagLists.ids(for: TagListBehaviorTests.key), [669, 122])
        XCTAssertEqual(model.ids, [669, 122])
    }

    func testRemovingTheLastRowBringsBackTheEmptyState() {
        list(ids: [669])
        model.remove(at: [0])
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(model.ids, [])
        XCTAssertTrue(driver.exists(id: "list.browse"))
        XCTAssertFalse(driver.isEnabled(label: "Edit"))
    }

    func testDraggingARowReordersTheStoredList() {
        list()
        // SwiftUI reports a drop below the last row as the gap at the end.
        model.move(from: [0], to: 3)
        XCTAssertEqual(TMTagLists.ids(for: TagListBehaviorTests.key), [1478, 122, 669])
        model.move(from: [2], to: 0)
        XCTAssertEqual(TMTagLists.ids(for: TagListBehaviorTests.key), [669, 1478, 122])
    }

    // MARK: - Managing the list itself

    func testTheOverflowMenuRenamesAndDeletesTheList() {
        list()
        XCTAssertEqual(driver.label(id: "list.menu"), "List options")
        let edit = driver.element(label: "Edit")?.accessibilityFrame ?? .zero
        let menu = driver.element(id: "list.menu")?.accessibilityFrame ?? .zero
        XCTAssertGreaterThan(edit.midX, menu.midX, "Edit sits outermost")
        // The menu's two actions are the model's; each is exercised below.
    }

    func testRenamingTheListRetitlesTheScreen() {
        list()
        model.promptRename()
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Rename list")
        XCTAssertEqual(alert?.textFields?.first?.text, "Afterglow set")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Rename"])
        alert?.tm_type("Afterglow encore")
        ScreenCatalog.settle(0.1)
        alert?.tm_fire("Rename")
        ScreenCatalog.settle(0.1)
        spinUntil("the list is renamed") { TMTagLists.name(for: TagListBehaviorTests.key) == "Afterglow encore" }
        spinUntil("the screen is retitled") { self.controller.navigationItem.title == "Afterglow encore" }
    }

    func testDeletingTheListConfirmsFirstAndThenLeavesTheScreen() {
        list()
        model.confirmDelete()
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Delete “Afterglow set”?")
        XCTAssertEqual(alert?.message,
                       "This removes the list and its 3 tags from your lists. Tags stay in the catalog.")
        XCTAssertEqual(Set(alert?.actions.map { $0.title ?? "" } ?? []), ["Cancel", "Delete"])
        XCTAssertEqual(alert?.actions.first { $0.title == "Delete" }?.style, .destructive)
        XCTAssertEqual(TMTagLists.customKeys(), [TagListBehaviorTests.key])
        alert?.tm_fire("Delete")
        XCTAssertEqual(TMTagLists.customKeys(), [])
        XCTAssertGreaterThanOrEqual(navigator.removals, 1, "The screen leaves with its list")
    }

    func testAnEmptyListSaysSoInItsDeleteConfirmation() {
        list(ids: [])
        XCTAssertEqual(TMDeletePrompt(key: TagListBehaviorTests.key).message,
                       "“Afterglow set” has no tags. It will be removed from your lists.")
        XCTAssertEqual(TMDeletePrompt(key: TagListBehaviorTests.key).title, "Delete “Afterglow set”?")
    }

    func testAListDeletedElsewhereTakesItsScreenWithIt() {
        list()
        TMTagLists.deleteList(TagListBehaviorTests.key)
        XCTAssertEqual(navigator.removals, 1)
    }

    func testDeletingTheListElsewhereLeavesTheTagOpenedFromItOnTop() {
        seedAfterglow()
        let router = TMRouter()
        mountShell(router)
        router.show(.list(TagListBehaviorTests.key))
        // What tapping a row does on a phone: the tag's detail sits above the list.
        router.showTag(1478, source: nil)
        ScreenCatalog.settle(0.3)
        TMTagLists.deleteList(TagListBehaviorTests.key)
        waitUntil("the list leaves the stack") { router.path.count == 1 }
        XCTAssertEqual(router.path.first?.tagModel?.tagId, 1478,
                       "The tag the user is reading stays put; only its list goes")
    }

    func testATagAddedElsewhereMidScrollAppearsOnceTheScrollStops() {
        list(ids: [669])
        seedCachedTag(id: 1478, title: "Tag 1478")
        model.isScrolling = true
        TMTagLists.add(1478, to: TagListBehaviorTests.key)
        XCTAssertEqual(model.ids, [669], "Replacing rows mid-scroll would cancel the gesture")
        XCTAssertTrue(model.pendingRefresh)
        model.isScrolling = false
        XCTAssertEqual(model.ids, [669, 1478], "The change waited for the list rather than being dropped")
        XCTAssertEqual(controller.tm_listedTagIds().map(\.intValue), [669, 1478])
    }

    func testAListRenamedElsewhereRetitlesTheScreen() {
        list()
        TMTagLists.renameList(TagListBehaviorTests.key, to: "Tuesday set")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(controller.navigationItem.title, "Tuesday set")
    }

    func testTagsAddedElsewhereAppearInTheList() {
        list(ids: [669])
        seedCachedTag(id: 1478, title: "Tag 1478")
        TMTagLists.add(1478, to: TagListBehaviorTests.key)
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(model.ids, [669, 1478])
        XCTAssertEqual(driver.elements(labelPrefix: "Tag ").count, 2)
    }

    // MARK: - Beside an open tag

    func testTheOpenTagStaysLitAndRowsDropTheirChevronsBesideIt() {
        list()
        navigator.isExpandedSplit = true
        navigator.currentSplitTagId = 1478
        NotificationCenter.default.post(name: .TMTagSelectionDidChange, object: nil)
        XCTAssertTrue(model.expanded)
        XCTAssertEqual(model.selectedTagId, 1478)
        controller.tm_didStep(toTagId: 122)
        XCTAssertEqual(model.selectedTagId, 122)
        XCTAssertEqual(model.scrollTarget, 122)
        controller.tm_didStep(toTagId: 5)
        XCTAssertEqual(model.selectedTagId, 122, "A tag not in the list moves nothing")
    }

    // MARK: - Naming rules
    // The alert never closes on a name the registry would reject.

    func testTheNewListPromptRefusesEveryNameTheRegistryWouldReject() {
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: [])])
        var prompt = TMNamePrompt.create()
        XCTAssertEqual(prompt.title, "New list")
        XCTAssertEqual(prompt.actionTitle, "Create")
        XCTAssertFalse(prompt.canConfirm, "An empty field cannot create a list")
        XCTAssertEqual(prompt.message, "For example “Easy tags” or “High and lows”", "…but is not yet an error")

        for (typed, problem) in [("   ", "Give the list a name."),
                                 ("Afterglow set", "You already have a list with that name."),
                                 ("afterglow SET", "You already have a list with that name."),
                                 ("Favorites", "That name is used by a built-in list."),
                                 ("Teachable Tags", "That name is used by a built-in list."),
                                 (String(repeating: "x", count: 61), "Keep the name under 61 characters.")] {
            prompt.text = typed
            XCTAssertFalse(prompt.canConfirm, "“\(typed)” must not be creatable")
            XCTAssertNil(prompt.committedName)
            XCTAssertEqual(prompt.message, typed.trimmingCharacters(in: .whitespaces).isEmpty
                           ? "For example “Easy tags” or “High and lows”" : problem)
        }

        prompt.text = "Chorus warmups"
        XCTAssertTrue(prompt.canConfirm)
        XCTAssertEqual(prompt.message, "For example “Easy tags” or “High and lows”", "A usable name shows the hint again")
    }

    func testTheMountedNewListAlertDisablesCreateUntilTheNameIsUsable() {
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: [])])
        let home = TMScreens.home(navigator: RecordingNavigator())
        mountScreen(home)
        (home.listing as! TMHomeModel).newList()
        let alert = presentedAlert()
        let create = alert?.actions.first { $0.title == "Create" }
        XCTAssertEqual(create?.isEnabled, false)
        alert?.tm_type("Afterglow set")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(alert?.message, "You already have a list with that name.")
        XCTAssertEqual(alert?.actions.first { $0.title == "Create" }?.isEnabled, false)
        alert?.tm_type("Chorus warmups")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(alert?.actions.first { $0.title == "Create" }?.isEnabled, true)
        XCTAssertEqual(alert?.message, "For example “Easy tags” or “High and lows”")
    }

    func testRenamingAListMayKeepItsOwnName() {
        seedLists(lists: [(key: TagListBehaviorTests.key, name: "Afterglow set", ids: [])])
        var prompt = TMNamePrompt.rename(TagListBehaviorTests.key)
        XCTAssertEqual(prompt.title, "Rename list")
        XCTAssertEqual(prompt.actionTitle, "Rename")
        XCTAssertTrue(prompt.canConfirm, "Its own name is not a duplicate")
        XCTAssertNil(prompt.message, "Renaming carries no hint")
        prompt.text = "Favorites"
        XCTAssertFalse(prompt.canConfirm)
    }

    func testNamesAreTrimmedAndCollapsedBeforeTheyAreStored() {
        var prompt = TMNamePrompt.create()
        prompt.text = "  Afterglow   set  "
        XCTAssertEqual(prompt.committedName, "Afterglow set")
    }
}
