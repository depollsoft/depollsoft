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

    func testDetailTabBarNamesEveryPageAndMeetsItsTargetContract() {
        seedCachedTag()
        let detail = loadedDetail()
        assertTabContract(detail, titles: Self.detailTabs)
        assertTabTargets(detail, titles: Self.detailTabs)
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


    // The empty Teachable Tags state now lives in TagListBehaviorTests.

    // Browse's tabs, Settings and tag rows are covered in QueryBehaviorTests and SettingsBehaviorTests.



}
