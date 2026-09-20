//
//  ListPickerBehaviorTests.swift
//  tagmasterTests
//
//  "Add to list": the sheet that puts a tag on any list. Its rows show and
//  change membership as they are tapped, and its last row makes a new list.
//

import XCTest
import UIKit
@testable import tagmaster

/// A picker whose presentations are captured, so its new-list alert can be read.
final class PickerPresentationFixture: TMListPickerController {
    var presented: [UIViewController] = []
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        presented.append(viewControllerToPresent)
        completion?()
    }
}

/// Captures what a screen presents instead of presenting it.
final class PresentationHost: UIViewController {
    var presented: [UIViewController] = []
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        presented.append(viewControllerToPresent)
        completion?()
    }
}

final class ListPickerBehaviorTests: TMBehaviorTestCase {

    private static let afterglow = "afterglow-set-k3f9"
    private static let warmups = "chorus-warmups-aa12"

    private func seedTwoLists(favorite: [Int] = [], teachable: [Int] = []) {
        seedLists(favorite: favorite, teachable: teachable, lists: [
            (key: ListPickerBehaviorTests.afterglow, name: "Afterglow set", ids: [1809]),
            (key: ListPickerBehaviorTests.warmups, name: "Chorus warmups", ids: [])
        ])
    }

    @discardableResult
    private func picker(tagId: Int32 = 1809) -> PickerPresentationFixture {
        let picker = PickerPresentationFixture(tagId: tagId)
        mountInNavigation(picker)
        settle()
        return picker
    }

    private func rows(_ picker: TMListPickerController) -> [(title: String, checked: Bool, identifier: String)] {
        let table = picker.tableView!
        return (0..<table.numberOfRows(inSection: 0)).map { row in
            let cell = picker.tableView(table, cellForRowAt: IndexPath(row: row, section: 0))
            return (cell.textLabel?.text ?? "", cell.accessoryType == .checkmark, cell.accessibilityIdentifier ?? "")
        }
    }

    // MARK: - Contents

    func testThePickerListsEveryListWithTheTagsMembershipAndOffersANewOne() {
        seedTwoLists(favorite: [1809])
        let picker = self.picker()

        XCTAssertEqual(picker.navigationItem.title, "Add to list")
        XCTAssertEqual(rows(picker).map(\.title),
                       ["Favorites", "Teachable Tags", "Afterglow set", "Chorus warmups", "New list…"])
        XCTAssertEqual(rows(picker).map(\.checked), [true, false, true, false, false])
        XCTAssertEqual(rows(picker).map(\.identifier),
                       ["picker.row.favorite", "picker.row.teachable",
                        "picker.row.\(ListPickerBehaviorTests.afterglow)",
                        "picker.row.\(ListPickerBehaviorTests.warmups)", "picker.row.new"])
    }

    func testTheCheckedRowsAreAlsoMarkedSelectedForVoiceOver() {
        seedTwoLists(favorite: [1809])
        let picker = self.picker()
        let favorites = picker.tableView(picker.tableView, cellForRowAt: IndexPath(row: 0, section: 0))
        let teachable = picker.tableView(picker.tableView, cellForRowAt: IndexPath(row: 1, section: 0))

        XCTAssertTrue(favorites.accessibilityTraits.contains(.selected))
        XCTAssertFalse(teachable.accessibilityTraits.contains(.selected))
    }

    func testThePickerOffersDoneToClose() {
        let picker = self.picker()
        let done = picker.navigationItem.rightBarButtonItem
        XCTAssertEqual(done?.title, "Done")
        XCTAssertEqual(done?.accessibilityIdentifier, "picker.done")
    }

    // MARK: - Changing membership

    func testTappingARowAddsTheTagAndTappingItAgainRemovesIt() {
        seedTwoLists()
        let picker = self.picker()

        picker.tableView(picker.tableView, didSelectRowAt: IndexPath(row: 0, section: 0))
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])
        XCTAssertTrue(rows(picker)[0].checked, "The row shows the change at once")

        picker.tableView(picker.tableView, didSelectRowAt: IndexPath(row: 0, section: 0))
        XCTAssertEqual(DPAppDelegate.favorites(), [])
        XCTAssertFalse(rows(picker)[0].checked)
    }

    func testTappingACustomListRowMovesTheTagInAndOutOfThatList() {
        seedTwoLists()
        let picker = self.picker()

        picker.tableView(picker.tableView, didSelectRowAt: IndexPath(row: 3, section: 0))
        XCTAssertEqual(TMTagLists.ids(for: ListPickerBehaviorTests.warmups), [1809])

        picker.tableView(picker.tableView, didSelectRowAt: IndexPath(row: 2, section: 0))
        XCTAssertEqual(TMTagLists.ids(for: ListPickerBehaviorTests.afterglow), [])
    }

    func testTheNewListRowNamesAListAndPutsTheTagStraightIntoIt() {
        let picker = self.picker()
        picker.tableView(picker.tableView, didSelectRowAt: IndexPath(row: 2, section: 0))

        let alert = try? XCTUnwrap(picker.presented.last as? UIAlertController)
        XCTAssertEqual(alert?.title, "New list")
        alert?.tm_type("Afterglow set")
        alert?.tm_fire("Create")
        settle()

        let key = try? XCTUnwrap(TMTagLists.customKeys().first)
        XCTAssertEqual(key.map { TMTagLists.name(for: $0) }, "Afterglow set")
        XCTAssertEqual(key.map { TMTagLists.ids(for: $0) }, [1809])
        XCTAssertEqual(rows(picker).map(\.title), ["Favorites", "Teachable Tags", "Afterglow set", "New list…"])
        XCTAssertTrue(rows(picker)[2].checked)
        picker.presented.removeAll()
    }

    func testAListMadeElsewhereShowsUpInAnOpenPicker() {
        let picker = self.picker()
        _ = TMTagLists.createList(named: "Chorus warmups")
        settle()
        XCTAssertEqual(rows(picker).map(\.title), ["Favorites", "Teachable Tags", "Chorus warmups", "New list…"])
    }

    // MARK: - Presentation

    func testThePickerOpensAsASheetOnIPhoneAndCarriesItsOwnNavigationBar() {
        let host = PresentationHost()
        mount(host)
        TMListPickerController.present(forTagId: 1809, from: host, barButtonItem: nil, sourceView: nil)

        let navigation = try? XCTUnwrap(host.presented.last as? UINavigationController)
        XCTAssertTrue(navigation?.viewControllers.first is TMListPickerController)
        if UIDevice.current.userInterfaceIdiom == .pad {
            XCTAssertEqual(navigation?.modalPresentationStyle, .popover)
        } else {
            XCTAssertEqual(navigation?.modalPresentationStyle, .pageSheet)
            XCTAssertEqual(navigation?.sheetPresentationController?.detents.first,
                           UISheetPresentationController.Detent.medium())
        }
        host.presented.removeAll()
    }

    func testThePickerIsAnchoredToTheBarButtonItOpenedFrom() {
        let host = PresentationHost()
        mount(host)
        let item = UIBarButtonItem()
        TMListPickerController.present(forTagId: 1809, from: host, barButtonItem: item, sourceView: nil)

        let navigation = host.presented.last as? UINavigationController
        if UIDevice.current.userInterfaceIdiom == .pad {
            XCTAssertEqual(navigation?.popoverPresentationController?.barButtonItem, item)
        }
        XCTAssertNotNil(navigation)
        host.presented.removeAll()
    }

    // MARK: - Screenshot

    func testCaptureThePicker() {
        seedTwoLists(favorite: [1809], teachable: [1809])
        let picker = self.picker()
        picker.tableView.reloadData()
        capture("ios-list-picker")
        XCTAssertEqual(picker.tableView.numberOfRows(inSection: 0), 5)
    }
}
