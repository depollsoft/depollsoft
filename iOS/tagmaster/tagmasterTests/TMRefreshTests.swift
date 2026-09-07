//
//  TMRefreshTests.swift
//  tagmasterTests
//
//  Hard assertions for the refreshed singing desk: the home structure, the
//  browse collections, the tag detail sections, deep links, storage keys, and
//  the theme's contrast. These check behaviour, not merely that a screen loads.
//

import XCTest
import UIKit
import FirebaseCore
import AVFoundation
@testable import tagmaster

private final class TMPauseSpy: AVPlayer {
    var pauseCount = 0
    override func pause() { pauseCount += 1 }
}

final class TMRefreshTests: XCTestCase {

    private var window: UIWindow!
    private let listsDefaultsKey = "depollsoft.pitchperfect.lists"
    private var originalLists: Any?

    override func setUp() {
        super.setUp()
        originalLists = UserDefaults.standard.object(forKey: listsDefaultsKey)
        window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
    }

    override func tearDown() {
        window.isHidden = true
        window.rootViewController = nil
        window = nil
        UserDefaults.standard.set(originalLists, forKey: listsDefaultsKey)
        super.tearDown()
    }

    private func present(_ controller: UIViewController) {
        window.rootViewController = controller
        window.makeKeyAndVisible()
        _ = controller.view
        controller.view.layoutIfNeeded()
    }

    private func loadedHome() -> DPHomeViewController {
        let home = DPHomeViewController()
        present(UINavigationController(rootViewController: home))
        _ = home.view
        home.view.layoutIfNeeded()
        return home
    }

    /// `navigationItems` stays a private ObjC method; the tests read it the way
    /// the table view does.
    private func deskActions(of home: DPHomeViewController) -> [[String: Any]] {
        let result = home.perform(Selector(("navigationItems")))?.takeUnretainedValue()
        return (result as? [[String: Any]]) ?? []
    }

    // MARK: - Home structure

    func testHomeOffersTheApprovedDeskActionsInOrder() {
        let items = deskActions(of: loadedHome())
        let titles = items.map { $0["title"] as? String ?? "" }
        XCTAssertEqual(titles, ["Find a tag", "Random Tag", "Open Tag ID", "Teachable Tags"],
                       "Home must present the approved singing-desk actions in order")

        for item in items {
            XCTAssertNotNil(item["action"], "\(item["title"] ?? "row") needs an action")
            let symbol = item["symbol"] as? String ?? ""
            XCTAssertNotNil(UIImage(systemName: symbol),
                            "\(item["title"] ?? "row") uses a real SF Symbol")
        }
    }

    func testHomeKeepsFavoritesAsItsSecondSectionWithAnEmptyState() {
        DPAppDelegate.setFavorites([])
        let home = loadedHome()
        let table = home.tableView!

        XCTAssertEqual(home.numberOfSections(in: table), 2)
        XCTAssertEqual(home.tableView(table, titleForHeaderInSection: 1), "Favorites")
        XCTAssertEqual(home.tableView(table, numberOfRowsInSection: 1), 0)

        let emptyFooter = home.tableView(table, titleForFooterInSection: 1)
        XCTAssertNotNil(emptyFooter, "An empty favorites list must explain itself")
        XCTAssertTrue(emptyFooter?.contains("favorite") == true)

        DPAppDelegate.setFavorites([42])
        XCTAssertEqual(home.tableView(table, numberOfRowsInSection: 1), 1)
        XCTAssertNil(home.tableView(table, titleForFooterInSection: 1),
                     "The empty-state footer disappears once there are favorites")
        DPAppDelegate.setFavorites([])
    }

    func testHomeShowsTheWordmarkAndKeepsSettingsAsAUtility() {
        let home = loadedHome()

        let wordmark = home.navigationItem.titleView as? UILabel
        XCTAssertEqual(wordmark?.text, "Tag Master")

        XCTAssertEqual(home.navigationItem.rightBarButtonItem?.accessibilityLabel, "Settings",
                       "Settings lives in the navigation bar as a utility")
        XCTAssertTrue(home.navigationItem.leftBarButtonItem === home.editButtonItem)

        let titles = deskActions(of: home).map { $0["title"] as? String ?? "" }
        XCTAssertFalse(titles.contains("Settings"), "Settings is not a desk action")
        XCTAssertFalse(titles.contains("Browse"), "Browse is a destination, not a home row")
    }

    func testHomeRowsAreDescribedForVoiceOver() {
        let home = loadedHome()
        let cell = home.tableView(home.tableView, cellForRowAt: IndexPath(row: 0, section: 0))
        XCTAssertEqual(cell.accessibilityLabel, "Find a tag")
        XCTAssertFalse((cell.accessibilityHint ?? "").isEmpty)
        XCTAssertEqual(cell.accessoryType, .disclosureIndicator)
    }

    // MARK: - Browse

    func testBrowseExposesFourCollectionsMappedToTheRightQueries() {
        XCTAssertEqual(DPBrowseViewController.collectionTitles(),
                       ["Latest", "Top Rated", "Downloads", "Classic"])

        let browse = DPBrowseViewController()
        present(UINavigationController(rootViewController: browse))

        XCTAssertEqual(browse.queryController(for: 0).sortBy, DPTagSortPosted)
        XCTAssertEqual(browse.queryController(for: 1).sortBy, DPTagSortRating)
        XCTAssertEqual(browse.queryController(for: 2).sortBy, DPTagSortDownloaded)
        XCTAssertEqual(browse.queryController(for: 3).sortBy, DPTagSortClassic)
        XCTAssertEqual(browse.queryController(for: 3).collection, DPTagCollectionClassicTags)
        XCTAssertTrue(browse.queryController(for: 0).embedded,
                      "Embedded lists must not inset themselves from the safe area twice")
    }

    func testBrowseSwitchesTheVisibleCollection() {
        let browse = DPBrowseViewController()
        present(UINavigationController(rootViewController: browse))

        browse.selectCollection(at: 2)
        XCTAssertEqual(browse.selectedCollectionIndex, 2)
        XCTAssertEqual(browse.children.count, 1)
        XCTAssertTrue(browse.children.first === browse.queryController(for: 2))

        browse.selectCollection(at: 3)
        XCTAssertEqual(browse.selectedCollectionIndex, 3)
        XCTAssertEqual(browse.children.count, 1,
                       "Switching collections replaces the child rather than stacking children")
        XCTAssertTrue(browse.children.first === browse.queryController(for: 3))
        XCTAssertFalse(browse.isKind(of: NSClassFromString("DPTabBarController")!),
                       "Browse no longer nests a second tab bar")
    }

    // MARK: - Tag detail

    private func loadedDetail() -> DPTagViewController {
        let detail = DPTagViewController()
        present(UINavigationController(rootViewController: detail))
        _ = detail.view
        detail.view.layoutIfNeeded()
        return detail
    }

    func testDetailKeepsTheAndroidSectionOrder() {
        XCTAssertEqual(DPTagViewController.sectionTitles(),
                       ["Summary", "Details", "Tracks", "Videos"])

        let detail = loadedDetail()
        XCTAssertEqual(detail.viewControllers.count, 4)
        XCTAssertTrue(detail.viewControllers[0] is DPTagSummaryController)
        XCTAssertTrue(detail.viewControllers[1] is DPTagDetailController)
        XCTAssertTrue(detail.viewControllers[2] is DPTagTracksController)
        XCTAssertTrue(detail.viewControllers[3] is DPTagVideoController)
        XCTAssertFalse(detail.isKind(of: NSClassFromString("DPTabBarController")!),
                       "A pushed detail screen must not carry its own tab bar")
    }

    func testDetailSwitchesSectionsAndKeepsShareAnchored() {
        let detail = loadedDetail()

        detail.selectSection(at: 2)
        XCTAssertEqual(detail.selectedSectionIndex, 2)
        XCTAssertTrue(detail.children.contains(detail.viewControllers[2]))

        detail.selectSection(at: 3)
        XCTAssertEqual(detail.selectedSectionIndex, 3)
        XCTAssertFalse(detail.children.contains(detail.viewControllers[2]),
                       "Only the chosen section stays attached in compact width")

        let items = detail.navigationItem.rightBarButtonItems ?? []
        XCTAssertEqual(items.count, 3, "Share, list menu, and refresh")
        XCTAssertEqual(items[0].accessibilityLabel, "Share tag")
        XCTAssertEqual(items[1].menu?.children.count, 2,
                       "List actions ride a menu, which anchors itself on iPad")
        XCTAssertEqual(items[2].accessibilityLabel, "Refresh tag")
    }

    func testDetailListMenuNamesTheActionItWillTake() {
        let detail = loadedDetail()
        detail.tagId = 90210
        DPAppDelegate.removeFavorite(90210)
        DPAppDelegate.removeTeachable(90210)

        var menu = detail.navigationItem.rightBarButtonItems![1].menu!
        XCTAssertEqual((menu.children[0] as? UIAction)?.title, "Add Favorite")
        XCTAssertEqual((menu.children[1] as? UIAction)?.title, "Mark as Teachable")

        // Perform the action the menu would run and confirm the list really changes.
        detail.perform(Selector(("toggleFavorite")))
        XCTAssertTrue(DPAppDelegate.containsFavorite(90210), "The menu action really saves")

        menu = detail.navigationItem.rightBarButtonItems![1].menu!
        XCTAssertEqual((menu.children[0] as? UIAction)?.title, "Remove Favorite",
                       "…and the menu then offers the opposite action")

        DPAppDelegate.removeFavorite(90210)
        DPAppDelegate.removeTeachable(90210)
    }

    // MARK: - Lists, storage, deep links

    func testListsKeepTheirLongStandingDefaultsKey() {
        DPAppDelegate.setFavorites([7, 8])
        DPAppDelegate.setTeachable([9])

        let lists = UserDefaults.standard.dictionary(forKey: listsDefaultsKey)
        XCTAssertNotNil(lists, "The intentionally-named legacy key must survive the refresh")
        XCTAssertEqual(lists?["favorite"] as? [Int], [7, 8])
        XCTAssertEqual(lists?["teachable"] as? [Int], [9])

        DPAppDelegate.setFavorites([])
        DPAppDelegate.setTeachable([])
    }

    func testDeepLinkPushesTheTagOntoTheHomeStack() {
        let root = TMRootController.make()
        present(root)

        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        let delegate = DPAppDelegate()
        delegate.rootController = root
        root.selectedIndex = 2

        let url = URL(string: "tagmaster:///tag/1234")!
        XCTAssertTrue(delegate.application(UIApplication.shared, open: url, options: [:]))

        let home = root.homeNavigationController!
        XCTAssertEqual(root.selectedIndex, 0, "Deep links bring Home forward")
        let top = home.topViewController as? DPTagViewController
        XCTAssertNotNil(top, "A tagmaster:// tag link opens the tag")
        XCTAssertEqual(top?.tagId, 1234)
    }

    func testRootOffersExactlyTheThreeDestinations() {
        let root = TMRootController.make()
        _ = root.view
        XCTAssertEqual(root.viewControllers?.count, 3)
        let titles = (root.viewControllers ?? []).map { $0.tabBarItem.title ?? "" }
        XCTAssertEqual(titles, ["Home", "Browse", "Search"])
        for destination in root.viewControllers ?? [] {
            XCTAssertTrue(destination is UINavigationController,
                          "Each destination keeps its own navigation stack")
        }
    }

    // MARK: - Cells and states

    func testTagCellDescribesMaterialsWithoutRelyingOnColour() {
        let tag = DPTag()
        tag.tagId = 55
        tag.title = "Lida Rose"
        tag.alternativeTitle = "Will I Ever Tell You"
        tag.posted = Date(timeIntervalSince1970: 1_000_000)
        tag.rating = 4.5
        tag.downloadCount = 120

        let cell = DPTagCell(style: .default, reuseIdentifier: "Tag")
        cell.tagInstance = tag

        let label = cell.accessibilityLabel ?? ""
        XCTAssertTrue(label.contains("Lida Rose"))
        XCTAssertTrue(label.contains("a.k.a. Will I Ever Tell You"))
        XCTAssertTrue(label.contains("ID 55"))
        XCTAssertTrue(label.contains("No sheet music"))
        XCTAssertTrue(label.contains("No learning tracks"))
        XCTAssertEqual(DPTagCell.tagHeight(tag), UITableView.automaticDimension)

        cell.contentView.frame = CGRect(x: 0, y: 0, width: 375, height: 0)
        let fitted = cell.contentView.systemLayoutSizeFitting(
            CGSize(width: 375, height: 0),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel)
        XCTAssertGreaterThanOrEqual(fitted.height, 44, "Rows clear the minimum target height")
    }

    func testEmptyStateOnlyShowsAnActionWhenThereIsOne() {
        let view = TMEmptyStateView(frame: CGRect(x: 0, y: 0, width: 320, height: 480))
        view.configure(symbolName: "waveform", title: "No tracks",
                       message: "None posted.", actionTitle: nil, action: nil)
        XCTAssertTrue(findButton(in: view)?.isHidden == true)

        var ran = false
        view.configure(symbolName: "wifi.exclamationmark", title: "Offline",
                       message: "No connection.", actionTitle: "Try Again") { ran = true }
        let button = findButton(in: view)
        XCTAssertEqual(button?.isHidden, false)
        XCTAssertEqual(button?.title(for: .normal), "Try Again")
        button?.sendActions(for: .touchUpInside)
        XCTAssertTrue(ran, "The recovery action actually runs")
    }

    private func findButton(in view: UIView) -> UIButton? {
        for subview in view.subviews {
            if let button = subview as? UIButton { return button }
            if let found = findButton(in: subview) { return found }
        }
        return nil
    }

    func testTeachableListExplainsItselfWhenEmpty() {
        DPAppDelegate.setTeachable([])
        let teachable = DPTeachableTagsController()
        present(UINavigationController(rootViewController: teachable))
        teachable.viewDidAppear(false)

        XCTAssertTrue(teachable.tableView.backgroundView is TMEmptyStateView)
        XCTAssertFalse(teachable.editButtonItem.isEnabled, "Nothing to edit in an empty list")

        DPAppDelegate.setTeachable([11])
        teachable.viewDidAppear(false)
        XCTAssertNil(teachable.tableView.backgroundView)
        XCTAssertTrue(teachable.editButtonItem.isEnabled)
        DPAppDelegate.setTeachable([])
    }

    func testFindActionReturnsToSearchForm() throws {
        let root = TMRootController.make()
        present(root)
        let search = try XCTUnwrap(root.viewControllers?[2] as? UINavigationController)
        search.pushViewController(UIViewController(), animated: false)
        root.selectedIndex = 0
        root.focusSearch()
        XCTAssertEqual(root.selectedIndex, 2)
        XCTAssertEqual(search.viewControllers.count, 1)
        XCTAssertTrue(search.topViewController is DPSearchViewController)
    }

    func testListNotificationUpdatesTeachableEmptyStateImmediately() {
        DPAppDelegate.setTeachable([])
        let controller = DPTeachableTagsController()
        present(UINavigationController(rootViewController: controller))
        XCTAssertNotNil(controller.tableView.backgroundView)
        DPAppDelegate.setTeachable([11])
        XCTAssertNil(controller.tableView.backgroundView)
        XCTAssertTrue(controller.editButtonItem.isEnabled)
        DPAppDelegate.setTeachable([])
    }

    func testDetailCollapsesNarrowRegularWidthAndKeepsMinimumPickerHeight() throws {
        let container = UIViewController()
        let detail = DPTagViewController()
        container.addChild(detail)
        container.view.addSubview(detail.view)
        detail.didMove(toParent: container)
        present(container)
        container.setOverrideTraitCollection(UITraitCollection(horizontalSizeClass: .regular), forChild: detail)
        detail.view.frame = CGRect(x: 0, y: 0, width: 600, height: 800)
        detail.view.setNeedsLayout()
        detail.view.layoutIfNeeded()
        XCTAssertEqual(detail.value(forKey: "usesSplitLayout") as? Bool, false)
        let picker = try XCTUnwrap(detail.value(forKey: "sectionPicker") as? UISegmentedControl)
        XCTAssertEqual(picker.numberOfSegments, 4)
        XCTAssertGreaterThanOrEqual(picker.frame.height, 44)
        detail.view.frame.size.width = 1000
        detail.view.setNeedsLayout()
        detail.view.layoutIfNeeded()
        XCTAssertEqual(detail.value(forKey: "usesSplitLayout") as? Bool, true)
        XCTAssertEqual(picker.numberOfSegments, 3)
        let summary = try XCTUnwrap(detail.viewControllers.first as? DPTagSummaryController)
        summary.view.layoutIfNeeded()
        let stack = try XCTUnwrap(summary.value(forKey: "stack") as? UIStackView)
        XCTAssertGreaterThanOrEqual(stack.bounds.width, summary.view.bounds.width - 40,
                                    "Split content must fill its pane instead of compressing around labels")
    }

}

extension TMRefreshTests {
    // MARK: - Finish review regressions

    func testSummaryRatingFitsNarrowAccessibilityXXXLWidths() throws {
        let container = UIViewController()
        let summary = DPTagSummaryController()
        container.addChild(summary)
        container.view.addSubview(summary.view)
        summary.didMove(toParent: container)
        present(container)
        let tag = DPTag()
        tag.title = "Smile"
        tag.rating = 3.17
        summary.tag = tag
        let row = try XCTUnwrap(summary.value(forKey: "ratingRow") as? UIStackView)
        let stars = try XCTUnwrap(summary.value(forKey: "ratingStars") as? UILabel)
        let rating = try XCTUnwrap(summary.value(forKey: "ratingLabel") as? UILabel)
        let rate = try XCTUnwrap(summary.value(forKey: "ratingButton") as? UIButton)
        for style: UIUserInterfaceStyle in [.light, .dark] {
            for width: CGFloat in [320, 390] {
                container.setOverrideTraitCollection(UITraitCollection(traitsFrom: [
                    UITraitCollection(userInterfaceStyle: style),
                    UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraExtraLarge)
                ]), forChild: summary)
                summary.view.frame = CGRect(x: 0, y: 0, width: width, height: 844)
                summary.view.setNeedsLayout()
                summary.view.layoutIfNeeded()
                XCTAssertEqual(row.axis, .vertical)
                XCTAssertEqual(stars.text, "★★★☆☆")
                XCTAssertEqual(rating.text, "3.17 out of 5")
                for label in [stars, rating] {
                    XCTAssertFalse(label.adjustsFontSizeToFitWidth)
                    XCTAssertGreaterThan(label.font.pointSize, 30)
                    XCTAssertGreaterThanOrEqual(label.bounds.width + 1, label.intrinsicContentSize.width)
                    XCTAssertGreaterThanOrEqual(label.bounds.height + 1,
                                               label.sizeThatFits(CGSize(width: label.bounds.width, height: 1000)).height)
                    let frame = label.convert(label.bounds, to: row)
                    XCTAssertGreaterThanOrEqual(frame.minX, -1)
                    XCTAssertLessThanOrEqual(frame.maxX, row.bounds.width + 1)
                }
                let starsFrame = stars.convert(stars.bounds, to: row)
                let ratingFrame = rating.convert(rating.bounds, to: row)
                XCTAssertLessThanOrEqual(starsFrame.maxY, ratingFrame.minY)
                XCTAssertGreaterThanOrEqual(rate.frame.minY, ratingFrame.maxY + TMTheme.spaceL - 1)
                XCTAssertGreaterThanOrEqual(rate.bounds.height, 44)
                XCTAssertGreaterThanOrEqual(rate.bounds.width, 44)
                XCTAssertLessThanOrEqual(rate.frame.maxY, row.bounds.height + 1)
                XCTAssertTrue(rate.isEnabled)
                XCTAssertTrue(rate.actions(forTarget: summary, forControlEvent: .touchUpInside)?.contains("rate") == true)
            }
        }
        container.setOverrideTraitCollection(UITraitCollection(preferredContentSizeCategory: .large), forChild: summary)
        summary.view.setNeedsLayout()
        summary.view.layoutIfNeeded()
        XCTAssertEqual(row.axis, .horizontal, "Returning to standard text restores the existing row")
    }

    func testPhoneSummaryFillsAvailableWidthAndKeepsRatingAndTypeReadable() throws {
        let container = UIViewController()
        let summary = DPTagSummaryController()
        container.addChild(summary)
        container.view.addSubview(summary.view)
        summary.didMove(toParent: container)
        present(container)
        container.setOverrideTraitCollection(UITraitCollection(traitsFrom: [
            UITraitCollection(horizontalSizeClass: .compact),
            UITraitCollection(preferredContentSizeCategory: .large)
        ]), forChild: summary)
        let tag = DPTag()
        tag.title = "Smile"
        tag.tagType = "Barbershop"
        tag.parts = 4
        tag.rating = 3.17
        tag.writtenKey = "Major:G"
        summary.tag = tag
        for width: CGFloat in [320, 390, 430] {
            summary.view.frame = CGRect(x: 0, y: 0, width: width, height: 844)
            summary.view.setNeedsLayout()
            summary.view.layoutIfNeeded()
            let stack = try XCTUnwrap(summary.value(forKey: "stack") as? UIStackView)
            XCTAssertEqual(stack.bounds.width, width - 2 * TMTheme.spaceL, accuracy: 1)
            for key in ["ratingStars", "ratingLabel", "typeLabel"] {
                let label = try XCTUnwrap(summary.value(forKey: key) as? UILabel)
                XCTAssertGreaterThanOrEqual(label.bounds.width + 1, label.intrinsicContentSize.width,
                                            "\(key) must fit at phone width \(width)")
            }
            let stars = try XCTUnwrap(summary.value(forKey: "ratingStars") as? UILabel)
            XCTAssertEqual(stars.text?.count, 5)
            let rating = try XCTUnwrap(summary.value(forKey: "ratingLabel") as? UILabel)
            XCTAssertEqual(rating.text, "3.17 out of 5")
            let pitch = try XCTUnwrap(summary.value(forKey: "keyButton") as? UIView)
            let button = try XCTUnwrap(pitch.value(forKey: "button") as? UIButton)
            let title = try XCTUnwrap(button.titleLabel)
            XCTAssertGreaterThanOrEqual(title.bounds.width + 1, title.intrinsicContentSize.width)
            XCTAssertLessThanOrEqual(title.bounds.height, title.font.lineHeight + 1)
        }
    }

    func testAttributionWithoutWebsiteIsPrimaryMultilineTextInBothAppearances() throws {
        let container = UIViewController()
        let detail = DPTagDetailController()
        container.addChild(detail)
        container.view.addSubview(detail.view)
        detail.didMove(toParent: container)
        present(container)
        detail.view.frame = CGRect(x: 0, y: 0, width: 390, height: 844)
        let tag = DPTag()
        let name = "Bobby Gray, Jr and the New Tradition singers"
        tag.provider = name
        tag.arranger = name
        tag.sungBy = name
        for style: UIUserInterfaceStyle in [.light, .dark] {
            let traits = UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: style),
                UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraExtraLarge)
            ])
            container.setOverrideTraitCollection(traits, forChild: detail)
            traits.performAsCurrent { detail.tag = tag }
            detail.view.layoutIfNeeded()
            for key in ["postedByValue", "arrangedByValue", "sungByValue"] {
                let value = try XCTUnwrap(detail.value(forKey: key) as? UIStackView)
                XCTAssertEqual(value.arrangedSubviews.count, 1)
                let label = try XCTUnwrap(value.arrangedSubviews.first as? UILabel)
                XCTAssertEqual(label.text, name)
                XCTAssertEqual(label.numberOfLines, 0)
                XCTAssertTrue(label.adjustsFontForContentSizeCategory)
                XCTAssertFalse(label.isUserInteractionEnabled)
                XCTAssertTrue(label.isAccessibilityElement)
                XCTAssertTrue(label.accessibilityTraits.intersection([.button, .link]).isEmpty)
                XCTAssertEqual(label.textColor, TMTheme.primaryText)
                for canvas in [UIColor.systemBackground, TMTheme.canvas, TMTheme.surface] {
                    XCTAssertGreaterThanOrEqual(contrast(label.textColor.resolvedColor(with: traits),
                                                        canvas.resolvedColor(with: traits)), 4.5)
                }
                XCTAssertEqual(label.font.pointSize,
                               UIFont.preferredFont(forTextStyle: .body, compatibleWith: traits).pointSize)
                XCTAssertGreaterThan(label.bounds.height, label.font.lineHeight * 2)
                XCTAssertGreaterThanOrEqual(label.bounds.height + 1,
                    label.sizeThatFits(CGSize(width: label.bounds.width, height: .greatestFiniteMagnitude)).height)
            }
        }
    }

    func testAttributionWebsitesKeepActionsAndRefreshRemovesStaleControls() throws {
        let detail = DPTagDetailController()
        present(detail)
        let tag = DPTag()
        tag.provider = "Posted name"
        tag.arranger = "Arranged name"
        tag.sungBy = "Sung name"
        let url = try XCTUnwrap(URL(string: "https://example.com/attribution"))
        tag.providerWebsite = url
        tag.arrangerWebsite = url
        tag.sungByWebsite = url
        detail.tag = tag
        var buttons: [UIButton] = []
        for (key, name) in [("postedByValue", "Posted name"), ("arrangedByValue", "Arranged name"),
                            ("sungByValue", "Sung name")] {
            let value = try XCTUnwrap(detail.value(forKey: key) as? UIStackView)
            XCTAssertEqual(value.arrangedSubviews.count, 1)
            let button = try XCTUnwrap(value.arrangedSubviews.first as? UIButton)
            XCTAssertEqual(button.title(for: .normal), name)
            XCTAssertTrue(button.isEnabled)
            XCTAssertTrue(button.isUserInteractionEnabled)
            // XCUI asserts the native button role; UIKit lazily supplies its default traits.
            XCTAssertEqual(button.value(forKey: "url") as? URL, url)
            XCTAssertEqual(button.actions(forTarget: button, forControlEvent: .touchUpInside), ["openHyperlink"])
            XCTAssertTrue(button.titleLabel?.adjustsFontForContentSizeCategory == true)
            XCTAssertEqual(button.titleLabel?.numberOfLines, 0)
            buttons.append(button)
        }
        tag.providerWebsite = nil
        tag.arrangerWebsite = nil
        tag.sungByWebsite = nil
        detail.tag = tag
        for button in buttons { XCTAssertNil(button.superview) }
        for key in ["postedByValue", "arrangedByValue", "sungByValue"] {
            let value = try XCTUnwrap(detail.value(forKey: key) as? UIStackView)
            XCTAssertTrue(value.arrangedSubviews.first is UILabel)
        }
        tag.providerWebsite = url
        detail.tag = tag
        let posted = try XCTUnwrap(detail.value(forKey: "postedByValue") as? UIStackView)
        XCTAssertTrue(posted.arrangedSubviews.first is UIButton)
    }

}

extension TMRefreshTests {
    func testPlayerStopsOnDismissalAndInterruption() {
        let controller = TMTrackPlayerController()
        let player = TMPauseSpy()
        controller.player = player
        controller.loadViewIfNeeded()
        player.pauseCount = 0
        controller.viewDidDisappear(false)
        XCTAssertGreaterThan(player.pauseCount, 0)
        player.pauseCount = 0
        NotificationCenter.default.post(name: UIApplication.willResignActiveNotification, object: nil)
        XCTAssertGreaterThan(player.pauseCount, 0)
    }

    func testFailedLaterPageKeepsResultsAndOffersRetry() throws {
        let query = DPTagQueryViewController()
        query.isLoading = true
        query.loadViewIfNeeded()
        let tag = DPTag()
        tag.tagId = 42
        tag.title = "Saved result"
        query.tags = [tag]
        query.isLoading = false
        query.hasMoreResults = false
        query.setValue(true, forKey: "lastFetchFailed")
        query.perform(Selector(("refreshViews")))
        let table = try XCTUnwrap(query.value(forKey: "tagTable") as? UITableView)
        let retry = try XCTUnwrap(table.tableFooterView as? UIButton)
        XCTAssertEqual(retry.title(for: .normal), "Couldn't load more tags. Try Again")
        XCTAssertEqual(retry.actions(forTarget: query, forControlEvent: .touchUpInside), ["retryPage"])
        XCTAssertEqual(query.tags.count, 1)
        XCTAssertTrue(query.tags.first as? DPTag === tag)
    }

}

extension TMRefreshTests {
    // MARK: - Theme

    func testTintClearsBodyContrastInBothAppearances() {
        let light = UITraitCollection(userInterfaceStyle: .light)
        let dark = UITraitCollection(userInterfaceStyle: .dark)

        XCTAssertGreaterThanOrEqual(
            contrast(TMTheme.tint.resolvedColor(with: light),
                     UIColor.systemBackground.resolvedColor(with: light)), 4.5,
            "Tinted text must be legible in light mode")
        XCTAssertGreaterThanOrEqual(
            contrast(TMTheme.tint.resolvedColor(with: dark),
                     UIColor.systemBackground.resolvedColor(with: dark)), 4.5,
            "…and in dark mode")
        XCTAssertGreaterThanOrEqual(
            contrast(TMTheme.ink.resolvedColor(with: dark),
                     UIColor.systemBackground.resolvedColor(with: dark)), 4.5,
            "Charcoal ink inverts for dark mode instead of vanishing")
    }

    func testTypeFollowsTheReadersSetting() {
        let large = UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraLarge)
        let body = TMTheme.bodyFont()
        let bigBody = UIFont.preferredFont(forTextStyle: .body, compatibleWith: large)
        XCTAssertGreaterThan(bigBody.pointSize, body.pointSize,
                             "Body copy follows Dynamic Type rather than a frozen point size")

        let wordmark = TMTheme.wordmarkFont(size: 24, textStyle: .headline)
        XCTAssertGreaterThan(wordmark.pointSize, 0)
        XCTAssertNotEqual(TMTheme.fieldLabelFont().pointSize, 12,
                          "Field labels are no longer pinned at 12pt")
    }

    func testWeightedFontsScaleExactlyOnce() {
        for category: UIContentSizeCategory in [.large, .extraExtraExtraLarge,
                                               .accessibilityExtraExtraExtraLarge] {
            let traits = UITraitCollection(preferredContentSizeCategory: category)
            traits.performAsCurrent {
                for style: UIFont.TextStyle in [.body, .headline, .subheadline, .title1, .largeTitle] {
                    let actual = TMTheme.font(style: style, weight: .semibold)
                    let expected = UIFont.preferredFont(forTextStyle: style, compatibleWith: traits)
                    XCTAssertEqual(actual.pointSize, expected.pointSize, accuracy: 0.01,
                                   "\(style.rawValue) must scale once at \(category.rawValue)")
                }
            }
        }
    }

    private func contrast(_ first: UIColor, _ second: UIColor) -> CGFloat {
        let firstLuminance = luminance(first), secondLuminance = luminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)
    }

    private func luminance(_ color: UIColor) -> CGFloat {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        func channel(_ value: CGFloat) -> CGFloat {
            value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
