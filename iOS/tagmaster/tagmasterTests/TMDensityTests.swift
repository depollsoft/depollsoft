import XCTest
import UIKit
@testable import tagmaster

final class TMDensityTests: XCTestCase {
    private var window: UIWindow!
    override func setUp() {
        super.setUp()
        TMDensityFixture.activate(withCount: 100)
        window = UIWindow(frame: CGRect(x: 0, y: 0, width: 402, height: 874))
    }
    override func tearDown() {
        window.isHidden = true
        window.rootViewController = nil
        window = nil
        TMDensityFixture.restore()
        super.tearDown()
    }
    private func present(_ controller: UIViewController) -> UINavigationController {
        let nav = UINavigationController(rootViewController: controller)
        window.rootViewController = nav
        window.makeKeyAndVisible()
        nav.view.layoutIfNeeded()
        controller.view.layoutIfNeeded()
        return nav
    }
    private func settle() { RunLoop.main.run(until: Date().addingTimeInterval(0.4)) }

    func testHomeHasExactlyTwoPeerEntriesForZeroOneAndOneHundred() {
        for count in [0, 1, 100] {
            let ids = Array(900001..<(900001 + count))
            DPAppDelegate.setFavorites(ids, doSave: false)
            DPAppDelegate.setTeachable(ids, doSave: false)
            let home = DPHomeViewController()
            _ = present(home)
            let table = home.tableView!
            XCTAssertEqual(table.numberOfSections, 2)
            XCTAssertEqual(table.numberOfRows(inSection: 0), 4)
            XCTAssertEqual(table.numberOfRows(inSection: 1), 2)
            XCTAssertEqual(home.tableView(table, titleForHeaderInSection: 1), "Your lists")
            for (row, name) in ["Favorites", "Teachable Tags"].enumerated() {
                let path = IndexPath(row: row, section: 1)
                let cell = home.tableView(table, cellForRowAt: path)
                XCTAssertEqual(cell.accessibilityLabel, name)
                XCTAssertEqual(cell.accessibilityValue, "\(count) tags")
                XCTAssertEqual(cell.accessoryType, .disclosureIndicator)
                XCTAssertFalse(home.tableView(table, canEditRowAt: path))
                XCTAssertFalse(cell is DPTagCell)
            }
            XCTAssertNil(home.navigationItem.leftBarButtonItem)
        }
    }
    func testBothEmptyDestinationsHaveTitlesAndKeepTheirPole() throws {
        DPAppDelegate.setFavorites([], doSave: false)
        DPAppDelegate.setTeachable([], doSave: false)
        for controller: UITableViewController in [DPFavoritesViewController(), DPTeachableTagsController()] {
            _ = present(controller)
            XCTAssertEqual(controller.tableView.numberOfRows(inSection: 0), 0)
            XCTAssertEqual(controller.tableView.style, .plain)
            let pole = try XCTUnwrap(controller.tableView.backgroundView)
            XCTAssertTrue(String(describing: type(of: pole)).contains("TMPageBackground"))
            let state = try XCTUnwrap(pole.subviews.first { $0 is TMEmptyStateView })
            XCTAssertFalse(state.isHidden)
            XCTAssertFalse(controller.editButtonItem.isEnabled)
            DPAppDelegate.setFavorites([900001], doSave: false)
            DPAppDelegate.setTeachable([900001], doSave: false)
            XCTAssertTrue(controller.tableView.backgroundView === pole)
            XCTAssertTrue(state.isHidden)
            XCTAssertTrue(controller.editButtonItem.isEnabled)
            DPAppDelegate.setFavorites([], doSave: false)
            DPAppDelegate.setTeachable([], doSave: false)
        }
    }
    func testHomeRoutesRegisterFavoritesAndTeachableWithNativeBack() {
        let home = DPHomeViewController()
        let nav = present(home)
        for row in 0..<2 {
            home.tableView(home.tableView, didSelectRowAt: IndexPath(row: row, section: 1))
            settle()
            let destination = nav.topViewController!
            if row == 0 { XCTAssertTrue(destination is DPFavoritesViewController) }
            else { XCTAssertTrue(destination is DPTeachableTagsController) }
            XCTAssertEqual(destination.title, row == 0 ? "Favorites" : "Teachable Tags")
            XCTAssertNotNil(destination.navigationItem.rightBarButtonItem)
            nav.popViewController(animated: false)
            XCTAssertTrue(nav.topViewController === home)
        }
    }
    func testOverlappingMembershipMutationsAndOrderStayIndependent() {
        let favorites = DPFavoritesViewController()
        _ = present(favorites)
        let teachable = DPTeachableTagsController()
        teachable.loadViewIfNeeded()
        let original = DPAppDelegate.teachable()
        XCTAssertEqual(favorites.tableView.numberOfRows(inSection: 0), 100)
        XCTAssertEqual(teachable.tableView.numberOfRows(inSection: 0), 100)
        favorites.tableView(favorites.tableView, moveRowAt: IndexPath(row: 0, section: 0), to: IndexPath(row: 2, section: 0))
        XCTAssertEqual(Array(DPAppDelegate.favorites().prefix(3)), [900002, 900003, 900001])
        XCTAssertEqual(DPAppDelegate.teachable(), original)
        favorites.tableView(favorites.tableView, commit: .delete, forRowAt: IndexPath(row: 2, section: 0))
        XCTAssertFalse(DPAppDelegate.favorites().contains(900001))
        XCTAssertEqual(DPAppDelegate.teachable(), original)
        let remainingFavorites = DPAppDelegate.favorites()
        teachable.tableView(teachable.tableView, moveRowAt: IndexPath(row: 0, section: 0), to: IndexPath(row: 1, section: 0))
        XCTAssertEqual(Array(DPAppDelegate.teachable().prefix(2)), [900002, 900001])
        teachable.tableView(teachable.tableView, commit: .delete, forRowAt: IndexPath(row: 0, section: 0))
        XCTAssertFalse(DPAppDelegate.teachable().contains(900002))
        XCTAssertEqual(DPAppDelegate.favorites(), remainingFavorites)
    }
    func testBothListsKeepPositionAfterTagAndBack() throws {
        for controller: UITableViewController in [DPFavoritesViewController(), DPTeachableTagsController()] {
            let nav = present(controller)
            let table = controller.tableView!
            table.scrollToRow(at: IndexPath(row: 50, section: 0), at: .top, animated: false)
            table.layoutIfNeeded()
            let offset = table.contentOffset.y
            table.delegate?.tableView?(table, didSelectRowAt: IndexPath(row: 50, section: 0))
            settle()
            XCTAssertEqual((nav.topViewController as? DPTagViewController)?.tagId, 900051)
            nav.popViewController(animated: false)
            settle()
            XCTAssertTrue(nav.topViewController === controller)
            XCTAssertEqual(table.contentOffset.y, offset, accuracy: 1)
        }
    }
    private func fitted(_ tag: DPTag, width: CGFloat, category: UIContentSizeCategory) -> (DPTagCell, CGFloat) {
        var result: (DPTagCell, CGFloat)!
        UITraitCollection(preferredContentSizeCategory: category).performAsCurrent {
            let cell = DPTagCell(style: .default, reuseIdentifier: nil)
            cell.traitOverrides.preferredContentSizeCategory = category
            // This off-window sizing probe supplies semantic fonts explicitly.
            // Native UI tests separately exercise the OS accessibility category.
            for (key, style): (String, UIFont.TextStyle) in [("title", .headline), ("aka", .subheadline), ("details", .footnote)] {
                let label = cell.value(forKey: key) as? UILabel
                label?.adjustsFontForContentSizeCategory = false
                label?.font = UIFont.preferredFont(forTextStyle: style,
                    compatibleWith: UITraitCollection(preferredContentSizeCategory: category))
            }
            cell.tagInstance = tag
            cell.frame = CGRect(x: 0, y: 0, width: width, height: 64)
            cell.layoutIfNeeded()
            let size = cell.contentView.systemLayoutSizeFitting(CGSize(width: width - 36, height: 0),
                withHorizontalFittingPriority: .required, verticalFittingPriority: .fittingSizeLevel)
            result = (cell, size.height)
        }
        return result
    }
    func testDefaultTwoLineDensityAndReadOnlyMaterialSupport() throws {
        let tag = try XCTUnwrap(TMDensityFixture.tags().first)
        for width: CGFloat in [402, 1000] {
            let (cell, height) = fitted(tag, width: width, category: .large)
            XCTAssertGreaterThanOrEqual(height, 54)
            XCTAssertLessThanOrEqual(height, 72)
            let details = try XCTUnwrap(cell.value(forKey: "details") as? UILabel)
            XCTAssertEqual(details.text, "ID 900001 · Sheet music · Learning tracks")
            XCTAssertEqual(details.numberOfLines, 0)
            XCTAssertFalse(details.isUserInteractionEnabled)
            XCTAssertFalse(cell.accessibilityLabel!.contains("3.17"))
            XCTAssertFalse(cell.accessibilityLabel!.contains("1234"))
            XCTAssertTrue(cell.accessibilityLabel!.contains("Has learning tracks"))
        }
        XCTAssertEqual(tag.rating, 3.17)
        XCTAssertEqual(tag.downloadCount, 1234)
        XCTAssertNotNil(tag.posted)
    }
    func testLongTitlesLargeTypeAndAlternateGrowWithoutClipping() throws {
        let tag = TMDensityFixture.tags()[7]
        tag.alternativeTitle = "Another full title, retained for singers who know this name"
        let (normal, normalHeight) = fitted(tag, width: 320, category: .large)
        let (large, largeHeight) = fitted(tag, width: 320, category: .accessibilityExtraExtraExtraLarge)
        let normalTitle = try XCTUnwrap(normal.value(forKey: "title") as? UILabel)
        let largeTitle = try XCTUnwrap(large.value(forKey: "title") as? UILabel)
        print("DENSITY fitted long normal=\(normalHeight) large=\(largeHeight) font=\(normalTitle.font.pointSize)/\(largeTitle.font.pointSize)")
        XCTAssertGreaterThan(largeTitle.font.pointSize, normalTitle.font.pointSize)
        XCTAssertGreaterThan(normalHeight, 72)
        XCTAssertGreaterThan(largeHeight, normalHeight)
        for cell in [normal, large] {
            let title = try XCTUnwrap(cell.value(forKey: "title") as? UILabel)
            let aka = try XCTUnwrap(cell.value(forKey: "aka") as? UILabel)
            XCTAssertEqual(title.numberOfLines, 0)
            XCTAssertEqual(aka.numberOfLines, 0)
            XCTAssertFalse(aka.isHidden)
            XCTAssertTrue(cell.accessibilityLabel!.contains(tag.title!))
            XCTAssertTrue(cell.accessibilityLabel!.contains(tag.alternativeTitle!))
        }
        for alternate in ["", "  ", tag.title!] {
            tag.alternativeTitle = alternate
            let (cell, _) = fitted(tag, width: 402, category: .large)
            XCTAssertTrue(try XCTUnwrap(cell.value(forKey: "aka") as? UILabel).isHidden)
        }
    }
    func testMissingSavedTagShowsRecoverableErrorRatherThanEndlessSpinner() throws {
        let cell = DPTagCell(style: .default, reuseIdentifier: nil)
        cell.tagId = 999999
        let details = try XCTUnwrap(cell.value(forKey: "details") as? UILabel)
        XCTAssertTrue(details.text!.contains("Loading"))
        settle()
        XCTAssertEqual(details.text, "Couldn't load tag. Open to retry.")
        XCTAssertEqual(cell.accessibilityIdentifier, "tag.999999")
    }
    func testNormalDebugLaunchRestoresInterruptedFixtureBeforeStartup() {
        TMDensityFixture.restore()
        let lists = UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.lists") as NSDictionary?
        let browse = UserDefaults.standard.object(forKey: "browse.collection") as? NSNumber
        TMDensityFixture.activate(withCount: 100)
        XCTAssertFalse(TMDensityFixture.launchIfRequested(DPAppDelegate()))
        XCTAssertEqual(UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.lists") as NSDictionary?, lists)
        XCTAssertEqual(UserDefaults.standard.object(forKey: "browse.collection") as? NSNumber, browse)
        XCTAssertNil(TMDensityFixture.tags())
    }
    func testFixtureRestoresPreferencesAndNeverWritesTagCache() {
        TMDensityFixture.restore()
        let beforeLists = UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.lists") as NSDictionary?
        let beforeBrowse = UserDefaults.standard.object(forKey: "browse.collection") as? NSNumber
        let cacheSize = DPTag.getCurrentCacheSize()
        TMDensityFixture.activate(withCount: 100)
        XCTAssertEqual(DPAppDelegate.favorites().count, 100)
        TMDensityFixture.restore()
        XCTAssertEqual(UserDefaults.standard.dictionary(forKey: "depollsoft.pitchperfect.lists") as NSDictionary?, beforeLists)
        XCTAssertEqual(UserDefaults.standard.object(forKey: "browse.collection") as? NSNumber, beforeBrowse)
        XCTAssertEqual(DPTag.getCurrentCacheSize(), cacheSize)
    }
}
