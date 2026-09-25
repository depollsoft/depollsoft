//
//  AppPolishTests.swift
//  tagmasterTests
//
//  In-process replacements for the TagMasterPolishUITests assertions about the
//  page tab bars, the teachable empty state, the settings screen and tag rows.
//  The XCUITests read these through the accessibility tree; the same values are
//  readable directly from the mounted view hierarchy.
//

import XCTest
import UIKit
@testable import tagmaster

/// Substitutes a signed-out account so the settings screen can be mounted
/// without configuring Firebase, exactly as the ObjC layout suite does.
private final class TMSignedOutSettings: DPSettingsController {
    override func isSignedIn() -> Bool { false }
}

final class AppPolishTests: TMBehaviorTestCase {

    // MARK: - Page tab bars
    // Replaces TagMasterPolishUITests.assertNativeTabs, testDetailTabSelection,
    // testBrowseTabSelection, testDetailLayoutInPortrait/Landscape and
    // TagDetailUITests.testDetailTabsIfExist.

    /// The tab buttons UIKit renders, left to right. iOS 26 builds two parallel
    /// stacks of the same buttons for its glass lens, so identical frames are
    /// one button seen twice.
    private func tabButtons(_ pages: TMPageViewController) -> [UIView] {
        var seen = Set<String>()
        return descendants(of: pages.tabBar) { $0 is UIControl && !$0.isHidden }
            .filter { $0.bounds.width > 0 && $0.bounds.height > 0 }
            .sorted { $0.convert($0.bounds, to: nil).minX < $1.convert($1.bounds, to: nil).minX }
            .filter {
                let frame = $0.convert($0.bounds, to: nil)
                return seen.insert("\(round(frame.minX))x\(round(frame.minY))").inserted
            }
    }

    /// Everything about the bar that holds at any size: which pages it offers,
    /// how each is identified, that exactly one is selected, and that the bar
    /// sits along the bottom.
    private func assertTabContract(_ pages: TMPageViewController,
                                   titles: [String],
                                   file: StaticString = #filePath,
                                   line: UInt = #line) {
        let bar = pages.tabBar!
        XCTAssertEqual(bar.accessibilityIdentifier, "page-tab-bar", file: file, line: line)
        XCTAssertFalse(bar.isHidden, file: file, line: line)

        // Every page is named, and carries the identifier it is found by.
        XCTAssertEqual(bar.items?.map { $0.title ?? "" }, titles, file: file, line: line)
        XCTAssertEqual(bar.items?.map { $0.accessibilityIdentifier ?? "" },
                       titles.map { "page-\($0)" }, file: file, line: line)
        XCTAssertEqual(bar.items?.count, pages.viewControllers.count, file: file, line: line)

        // Exactly one page is selected.
        XCTAssertNotNil(bar.selectedItem, file: file, line: line)
        XCTAssertEqual(bar.items?.filter { $0 == bar.selectedItem }.count, 1, file: file, line: line)

        // The bar sits in the lower half of the screen and inside it.
        let barFrame = bar.convert(bar.bounds, to: pages.view)
        let safe = pages.view.safeAreaLayoutGuide.layoutFrame
        XCTAssertGreaterThan(barFrame.minY, safe.midY, file: file, line: line)
        XCTAssertLessThanOrEqual(barFrame.maxY, pages.view.bounds.maxY + 1, file: file, line: line)

        // One rendered button per page, each inside the bar.
        let buttons = tabButtons(pages)
        XCTAssertEqual(buttons.count, titles.count,
                       "One rendered tab per page", file: file, line: line)
        for button in buttons {
            XCTAssertTrue(barFrame.insetBy(dx: -1, dy: -1).contains(button.convert(button.bounds, to: pages.view)),
                          file: file, line: line)
        }
    }

    /// The 44pt target contract, measured on the rendered buttons. Only checked
    /// in the regular-height layout: in compact height UIKit draws a 36pt bar and
    /// extends the touch target beyond the view, which is visible to XCUITest's
    /// accessibility frames but not to view geometry. That case stays a UI test.
    private func assertTabTargets(_ pages: TMPageViewController,
                                  titles: [String],
                                  file: StaticString = #filePath,
                                  line: UInt = #line) {
        var previousTarget: CGRect?
        for button in tabButtons(pages) {
            let frame = button.convert(button.bounds, to: pages.view)
            XCTAssertGreaterThanOrEqual(frame.height, 44, file: file, line: line)
            XCTAssertGreaterThanOrEqual(frame.width, 44, file: file, line: line)
            let target = CGRect(x: frame.midX - 22, y: frame.midY - 22, width: 44, height: 44)
            if let previousTarget {
                XCTAssertFalse(previousTarget.intersects(target),
                               "Neighbouring tab targets must not overlap", file: file, line: line)
            }
            previousTarget = target
        }
    }

    private static let detailTabs = ["Summary", "Details", "Tracks", "Videos"]
    private static let browseTabs = ["Latest", "Rating", "Downloads", "Classic"]

    func testDetailTabBarNamesEveryPageAndMeetsItsTargetContract() {
        seedCachedTag()
        let detail = loadedDetail()
        assertTabContract(detail, titles: Self.detailTabs)
        assertTabTargets(detail, titles: Self.detailTabs)
    }

    func testBrowseTabBarNamesEveryPageAndMeetsItsTargetContract() {
        let browse = DPBrowseViewController()
        quiesceQueries(in: browse)
        mountInNavigation(browse)
        settle()
        assertTabContract(browse, titles: Self.browseTabs)
        assertTabTargets(browse, titles: Self.browseTabs)
    }

    func testSelectingEachDetailTabSelectsThatPage() {
        seedCachedTag()
        let detail = loadedDetail()

        for (index, title) in Self.detailTabs.enumerated() {
            detail.selectedIndex = UInt(index)
            settle()
            XCTAssertEqual(detail.selectedIndex, UInt(index))
            XCTAssertEqual(detail.tabBar.selectedItem?.title, title)
            XCTAssertEqual(detail.tabBar.selectedItem, detail.viewControllers[index].tabBarItem)
            waitUntil("page \(title) is mounted") {
                detail.viewControllers[index].view.isDescendant(of: detail.rootView)
            }
        }
    }

    func testSelectingEachBrowseTabSelectsThatPage() {
        let browse = DPBrowseViewController()
        quiesceQueries(in: browse)
        mountInNavigation(browse)
        settle()

        for (index, title) in Self.browseTabs.enumerated() {
            browse.selectedIndex = UInt(index)
            settle()
            XCTAssertEqual(browse.selectedIndex, UInt(index))
            XCTAssertEqual(browse.tabBar.selectedItem?.title, title)
        }
    }

    // Replaces testDetailTabsInPortraitAndLandscape and
    // testBrowseTabsInPortraitAndLandscape. Device rotation is UIKit's job; the
    // app-level outcome is that the tab set and the selection survive the size
    // change, which a window resize reproduces in-process.

    func testDetailTabsAndSelectionSurviveASizeChange() {
        seedCachedTag()
        let detail = loadedDetail()
        detail.selectedIndex = 1
        settle()

        for size in [TMBehaviorTestCase.landscape, TMBehaviorTestCase.portrait] {
            resize(to: size)
            assertTabContract(detail, titles: Self.detailTabs)
            XCTAssertEqual(detail.tabBar.selectedItem?.title, "Details",
                           "The open page survives the size change")
        }
    }

    func testBrowseTabsAndSelectionSurviveASizeChange() {
        let browse = DPBrowseViewController()
        quiesceQueries(in: browse)
        mountInNavigation(browse)
        settle()
        browse.selectedIndex = 3
        settle()

        for size in [TMBehaviorTestCase.landscape, TMBehaviorTestCase.portrait] {
            resize(to: size)
            assertTabContract(browse, titles: Self.browseTabs)
            XCTAssertEqual(browse.tabBar.selectedItem?.title, "Classic")
        }
    }

    // The empty Teachable Tags state now lives in TagListBehaviorTests.

    // MARK: - Settings
    // Replaces testSettingsFilters and the non-Firebase half of
    // testSettingsLoginDismissal.

    func testSettingsExplainsLoggingInAndOffersItsRandomTagFilters() {
        let settings = TMSignedOutSettings()
        mountInNavigation(settings)
        settle()
        let table = settings.value(forKey: "tableView") as! UITableView
        let source = table.dataSource!

        XCTAssertEqual(settings.title, "Settings")
        XCTAssertEqual(source.tableView?(table, titleForHeaderInSection: 0), "Account")
        XCTAssertEqual(source.tableView?(table, titleForFooterInSection: 0),
                       "Log in to back up and synchronize your tag lists.")

        let account = source.tableView(table, cellForRowAt: IndexPath(row: 0, section: 0))
        XCTAssertEqual(account.textLabel?.text, "Log In")
        XCTAssertEqual(account.accessibilityHint, "Opens the sign-in options")

        XCTAssertEqual(settings.value(forKey: "filterTitles") as? [String],
                       ["Minimum Rating", "Minimum Downloads", "Sheet Music", "Learning Tracks"])
        let minimumRating = settings.value(forKey: "minRating") as! UISegmentedControl
        XCTAssertEqual(minimumRating.accessibilityLabel, "Minimum Rating")
        XCTAssertEqual((0..<minimumRating.numberOfSegments).map { minimumRating.titleForSegment(at: $0) },
                       ["Any", "1", "2", "3", "4"])
        XCTAssertGreaterThanOrEqual(minimumRating.bounds.height, 44)
    }

    func testSignedInSettingsOffersLogOutInstead() {
        final class SignedIn: DPSettingsController {
            override func isSignedIn() -> Bool { true }
        }
        let settings = SignedIn()
        mountInNavigation(settings)
        settle()
        let table = settings.value(forKey: "tableView") as! UITableView

        let account = table.dataSource!.tableView(table, cellForRowAt: IndexPath(row: 0, section: 0))
        XCTAssertEqual(account.textLabel?.text, "Log Out")
        XCTAssertEqual(account.accessibilityHint, "Signs out of Tag Master on this device")
    }

    // MARK: - Tag rows
    // Replaces testBrowseRowContentAndSize, which asserted a row was between
    // 100pt and 2000pt tall and that its label mentioned sheet music.

    func testTagRowNamesItsTagAndSizesItselfToItsContent() {
        let tag = seedCachedTag(id: 1478, title: "Their Hearts Were Full Of Spring")
        let cell = DPTagCell(style: .default, reuseIdentifier: "Tag")
        cell.tagInstance = tag
        cell.frame = CGRect(x: 0, y: 0, width: 375, height: 0)
        cell.layoutIfNeeded()

        let height = cell.contentView.systemLayoutSizeFitting(
            CGSize(width: 375, height: 0),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel).height
        XCTAssertGreaterThan(height, 100)
        XCTAssertLessThan(height, 2000)

        let spoken = try? XCTUnwrap(cell.accessibilityLabel)
        XCTAssertEqual(spoken?.contains("Their Hearts Were Full Of Spring"), true)
        XCTAssertEqual(spoken?.contains("Sheet music available"), true)
        XCTAssertEqual(spoken?.contains("Tag ID 1478"), true)
        XCTAssertEqual(cell.accessibilityTraits.contains(.button), true)
    }

    func testTagRowMarksMissingMediaAsUnavailable() {
        let tag = seedCachedTag(id: 4242, title: "Bare Tag", withTracks: false, withSheetMusic: false)
        let cell = DPTagCell(style: .default, reuseIdentifier: "Tag")
        cell.tagInstance = tag

        XCTAssertEqual(cell.accessibilityLabel?.contains("Sheet music unavailable"), true)
        XCTAssertEqual(cell.accessibilityLabel?.contains("Learning tracks unavailable"), true)
    }
}
