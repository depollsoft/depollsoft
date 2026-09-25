//
//  TagDetailLayoutTests.swift
//  tagmasterTests
//
//  Geometry of the SwiftUI tag detail across the sizes and text sizes people
//  use it at: the page bar stays a bottom bar inside the column that keeps its
//  page through rotation, text size and navigation; long Summary text hugs its
//  lines and stays reachable; Details facts stay whole and in reading order at
//  every width; video rows stay on screen; and beside a list on iPad the detail
//  follows the split (collapse, full-screen sheet music, one watermark).
//  Frames are read from the accessibility tree, as VoiceOver and the UI tests see them.
//

import XCTest
import UIKit
@testable import tagmaster

@MainActor
final class TagDetailLayoutTests: TMBehaviorTestCase {

    // MARK: - Fixtures

    private func longTag(id: Int32 = 1809) -> DPTag {
        let tag = seedCachedTag(id: id, title: "Lost with a long title that wraps at accessibility sizes",
                                lyrics: Array(repeating: "And I will wait to face the skies,\never roaming in your eyes.\nThere I go lost in your eyes.",
                                              count: 3).joined(separator: "\n"))
        tag.alternativeTitle = "In Your Eyes with another long alternate title"
        tag.classicTagNumber = 42
        tag.notes = "Sing the phrase together, then hold the last chord. Listen to the lead and balance the other parts. Repeat the phrase quietly before returning to full voice."
        tag.provider = "Alexandria Montgomery and the International Harmony Society"
        tag.providerWebsite = URL(string: "https://example.invalid/provider")
        tag.arranger = "Alexandria Montgomery and the International Harmony Society"
        tag.sungBy = "The International Harmony Society Quartet"
        tag.sungByWebsite = URL(string: "https://example.invalid/quartet")
        tag.yearArranged = 2020
        tag.sungYear = 2021
        tag.downloadCount = 1_234_567
        tag.cache()
        return tag
    }

    /// Mounts the detail in a navigation stack in a window of `size` (default: the screen).
    @discardableResult
    private func mountDetail(_ id: Int32 = 1809, size: CGSize? = nil,
                             category: UIContentSizeCategory = .large) -> TagDetailViewController {
        let detail = TagDetailViewController()
        detail.tagId = id
        let window = ScreenCatalog.makeWindow()
        if let size { window.frame = CGRect(origin: .zero, size: size) }
        window.traitOverrides.preferredContentSizeCategory = category
        let root = UIViewController()
        root.title = "Tags"
        let navigation = UINavigationController(rootViewController: root)
        navigation.pushViewController(detail, animated: false)
        window.rootViewController = navigation
        window.makeKeyAndVisible()
        self.window = window
        spinUntil("the detail loads", timeout: 5) { detail.model.tag != nil }
        ScreenCatalog.settle(0.4)
        return detail
    }

    private func resize(_ size: CGSize, category: UIContentSizeCategory) {
        window.frame = CGRect(origin: .zero, size: size)
        window.traitOverrides.preferredContentSizeCategory = category
        ScreenCatalog.settle(0.5)
    }

    private func bar(_ detail: UIViewController) throws -> UITabBar {
        try XCTUnwrap(firstDescendant(of: detail.view) {
            ($0 as? UITabBar)?.accessibilityIdentifier == "page-tab-bar"
        } as? UITabBar)
    }

    private func frame(_ label: String, file: StaticString = #filePath, line: UInt = #line) -> CGRect {
        let frame = UIDriver(window).element(label: label)?.accessibilityFrame
        XCTAssertNotNil(frame, "\(label) is on screen", file: file, line: line)
        return frame ?? .zero
    }

    private func frame(id: String, file: StaticString = #filePath, line: UInt = #line) -> CGRect {
        let frame = UIDriver(window).element(id: id)?.accessibilityFrame
        XCTAssertNotNil(frame, "\(id) is on screen", file: file, line: line)
        return frame ?? .zero
    }

    /// The scroll view of the page on screen.
    private func pageScroll(_ detail: UIViewController) -> UIScrollView? {
        descendants(of: detail.view) { $0 is UIScrollView && !($0 is UITextView) && $0.bounds.height > 200 && $0.window != nil }
            .compactMap { $0 as? UIScrollView }
            .first { $0.frame.minY >= 0 }
    }

    /// The page bar's contract at any size: the four pages named and identified, one
    /// selected, the bar along the bottom inside the detail, and the page's content above it.
    private func assertBar(_ detail: TagDetailViewController, selected: String,
                           file: StaticString = #filePath, line: UInt = #line) throws {
        let bar = try bar(detail)
        XCTAssertFalse(bar.isHidden, file: file, line: line)
        XCTAssertEqual(bar.items?.map { $0.title ?? "" }, ["Summary", "Details", "Tracks", "Videos"], file: file, line: line)
        XCTAssertEqual(bar.items?.map { $0.accessibilityIdentifier ?? "" },
                       ["page-Summary", "page-Details", "page-Tracks", "page-Videos"],
                       "The identifiers UI tests and store capture use survive", file: file, line: line)
        XCTAssertEqual(bar.selectedItem?.title, selected, file: file, line: line)
        let rect = bar.convert(bar.bounds, to: detail.view)
        let safe = detail.view.safeAreaLayoutGuide.layoutFrame
        XCTAssertGreaterThan(rect.minY, safe.midY, "The bar sits along the bottom", file: file, line: line)
        XCTAssertGreaterThanOrEqual(rect.minX, detail.view.bounds.minX - 1, file: file, line: line)
        XCTAssertLessThanOrEqual(rect.maxX, detail.view.bounds.maxX + 1, file: file, line: line)
    }

    // MARK: - Sizes, text sizes and selection

    func testThePageBarKeepsItsPlaceAndPageThroughRotationTextSizeAndNavigation() throws {
        longTag()
        let portrait = window?.bounds.size ?? ScreenCatalog.makeWindow().bounds.size
        let detail = mountDetail()
        let size = window.bounds.size
        _ = portrait
        try assertBar(detail, selected: "Summary")
        detail.model.selectedPage = .details
        ScreenCatalog.settle(0.3)
        for (index, category) in [UIContentSizeCategory.large, .large, .accessibilityExtraExtraExtraLarge,
                                  .accessibilityExtraExtraExtraLarge].enumerated() {
            let landscape = index % 2 == 1
            resize(landscape ? CGSize(width: size.height, height: size.width) : size, category: category)
            try assertBar(detail, selected: "Details")
            XCTAssertTrue(UIDriver(window).exists(label: "Last Refreshed"), "The open page is still Details")
        }
        resize(size, category: .large)

        // Another screen on top and back again keeps the page.
        detail.model.selectedPage = .tracks
        ScreenCatalog.settle(0.3)
        let navigation = try XCTUnwrap(detail.navigationController)
        navigation.pushViewController(UIViewController(), animated: false)
        ScreenCatalog.settle(0.3)
        navigation.popViewController(animated: false)
        ScreenCatalog.settle(0.4)
        try assertBar(detail, selected: "Tracks")
        XCTAssertEqual(detail.model.selectedPage, .tracks)
    }

    func testBesideAListThePageBarStaysInTheDetailColumnAtEveryWidth() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "The split is iPad only")
        seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let sidebar = UIViewController()
        sidebar.title = "Tags"
        let split = UISplitViewController(style: .doubleColumn)
        split.preferredDisplayMode = .oneBesideSecondary
        split.preferredSplitBehavior = .tile
        split.setViewController(UINavigationController(rootViewController: sidebar), for: .primary)
        split.setViewController(UINavigationController(rootViewController: detail), for: .secondary)
        let window = ScreenCatalog.makeWindow()
        window.rootViewController = split
        window.makeKeyAndVisible()
        self.window = window
        spinUntil("the detail loads beside the list", timeout: 5) { detail.model.tag != nil && detail.model.expanded }
        ScreenCatalog.settle(0.4)
        let portrait = window.bounds.size
        for (width, category) in [(portrait.width, UIContentSizeCategory.large),
                                  (600, .accessibilityExtraExtraExtraLarge),
                                  (portrait.height, .accessibilityExtraExtraExtraLarge)] {
            resize(CGSize(width: width, height: min(portrait.width, portrait.height)), category: category)
            guard !split.isCollapsed else { continue }
            try assertBar(detail, selected: "Summary")
            let bar = try bar(detail).convert(try bar(detail).bounds, to: window)
            let side = sidebar.view.convert(sidebar.view.bounds, to: window)
            XCTAssertGreaterThanOrEqual(bar.minX, side.maxX - 1, "The bar stays clear of the list at \(width) pt")
        }
    }

    // MARK: - Summary

    func testLongSummaryTextHugsItsLinesAndEveryLineStaysReachable() throws {
        longTag()
        for (width, category) in [(CGFloat(393), UIContentSizeCategory.large), (834, .large),
                                  (393, .accessibilityExtraExtraExtraLarge), (834, .accessibilityExtraExtraExtraLarge)] {
            let detail = mountDetail(size: CGSize(width: width, height: 1000), category: category)
            let driver = UIDriver(window)
            for label in ["Lost with a long title that wraps at accessibility sizes",
                          "a.k.a. In Your Eyes with another long alternate title"] {
                let text = frame(label)
                XCTAssertGreaterThan(text.width, 0)
                XCTAssertLessThanOrEqual(text.maxX, width + 0.5, "\(label) stays inside the screen at \(width) pt")
            }
            // The rating keeps its whole number beside Rate.
            let rating = frame("Rating out of 5")
            let digits = ("4.50" as NSString).size(withAttributes: [.font: UIFont.preferredFont(forTextStyle: .body,
                compatibleWith: UITraitCollection(preferredContentSizeCategory: category))]).width
            XCTAssertGreaterThanOrEqual(rating.width + 0.5, digits, "The rating is never clipped")

            // Headings sit directly above their text; the last line of the notes can be scrolled to.
            let scroll = try XCTUnwrap(pageScroll(detail))
            scroll.setContentOffset(CGPoint(x: 0, y: max(0, scroll.contentSize.height - scroll.bounds.height
                                                         + scroll.adjustedContentInset.bottom)), animated: false)
            ScreenCatalog.settle(0.3)
            let notesHeading = frame("Notes")
            let notes = try XCTUnwrap(driver.elements.first { ($0.accessibilityLabel ?? "").hasPrefix("Sing the phrase together") })
            let notesFrame = notes.accessibilityFrame
            XCTAssertGreaterThanOrEqual(notesFrame.minY, notesHeading.maxY - 0.5, "Notes start below their heading")
            XCTAssertLessThanOrEqual(notesFrame.minY - notesHeading.maxY, 8, "…directly below it, not after slack")
            let visible = scroll.convert(scroll.bounds, to: nil)
            XCTAssertLessThanOrEqual(notesFrame.maxY, visible.maxY + 1, "The notes' last line can be scrolled on screen")
        }
    }

    func testTheSummaryPutsLyricsBesideTheFactsOnlyWhenThereIsRoom() {
        seedCachedTag(id: 1809)
        for (width, beside) in [(CGFloat(393), false), (834, true)] {
            _ = mountDetail(size: CGSize(width: width, height: 1000))
            let facts = frame("Tag ID")
            let lyrics = frame("Lyrics")
            if beside {
                XCTAssertGreaterThan(lyrics.minX, facts.maxX, "Wide: lyrics sit in their own column")
                XCTAssertLessThan(abs(lyrics.minY - facts.minY), 80, "…level with the facts")
            } else {
                XCTAssertGreaterThan(lyrics.minY, facts.maxY, "Narrow: lyrics follow the facts")
                XCTAssertEqual(lyrics.minX, facts.minX, accuracy: 1)
            }
        }
    }

    // MARK: - Details

    func testDetailsFactsStayWholeAndInReadingOrderAtEveryWidthAndTextSize() throws {
        longTag()
        let captions = ["Last Refreshed", "Downloads", "Link", "Posted by", "Posted", "Arranged by",
                        "Year arranged", "Sung by", "Year sung"]
        for category in [UIContentSizeCategory.large, .extraExtraExtraLarge, .accessibilityExtraExtraExtraLarge] {
            for width in [CGFloat(320), 393, 834] {
                let detail = mountDetail(size: CGSize(width: width, height: 900), category: category)
                detail.model.selectedPage = .details
                ScreenCatalog.settle(0.4)
                let scroll = try XCTUnwrap(pageScroll(detail))
                var lastTop = -CGFloat.greatestFiniteMagnitude
                for caption in captions {
                    // Bring each fact on screen before reading it.
                    let driver = UIDriver(window)
                    var element = driver.element(label: caption)
                    var guardCount = 0
                    while (element == nil || element!.accessibilityFrame.maxY > scroll.convert(scroll.bounds, to: nil).maxY),
                          guardCount < 20 {
                        scroll.setContentOffset(CGPoint(x: 0, y: scroll.contentOffset.y + scroll.bounds.height / 2), animated: false)
                        ScreenCatalog.settle(0.1)
                        element = UIDriver(window).element(label: caption)
                        guardCount += 1
                    }
                    let captionFrame = try XCTUnwrap(element, "\(caption) at \(width) pt").accessibilityFrame
                    let top = captionFrame.minY + scroll.contentOffset.y
                    XCTAssertGreaterThanOrEqual(top, lastTop - 1, "\(caption) follows the fact before it")
                    lastTop = top
                    XCTAssertGreaterThan(captionFrame.width, 0)
                    XCTAssertLessThanOrEqual(captionFrame.maxX, width + 0.5, "\(caption) stays inside the screen")
                }
                scroll.setContentOffset(.zero, animated: false)
                ScreenCatalog.settle(0.2)
                let downloadsCaption = frame("Downloads")
                let downloads = frame("1234567")
                let font = UIFont.preferredFont(forTextStyle: .body,
                                                compatibleWith: UITraitCollection(preferredContentSizeCategory: category))
                XCTAssertGreaterThanOrEqual(downloads.width + 1, ("1234567" as NSString).size(withAttributes: [.font: font]).width,
                                            "The count is never a one-character column")
                XCTAssertLessThanOrEqual(downloads.height, font.lineHeight + 2, "…and stays on one line")
                XCTAssertLessThanOrEqual(downloads.maxX, width + 0.5)
                if category == .large {
                    XCTAssertGreaterThan(downloads.minX, downloadsCaption.maxX, "At the default size a value sits beside its caption")
                } else if category == .accessibilityExtraExtraExtraLarge, width <= 393 {
                    XCTAssertGreaterThanOrEqual(downloads.minY, downloadsCaption.maxY - 1, "At accessibility sizes it stacks under it")
                }
            }
        }
    }

    func testDetailsDropsTheFactsATagDoesNotHave() {
        seedMinimalTag()
        let detail = mountDetail(4243)
        detail.model.selectedPage = .details
        ScreenCatalog.settle(0.4)
        let driver = UIDriver(window)
        for caption in ["Last Refreshed", "Downloads", "Link", "Posted"] {
            XCTAssertTrue(driver.exists(label: caption), "\(caption) is always shown")
        }
        for caption in ["Posted by", "Arranged by", "Year arranged", "Sung by", "Year sung"] {
            XCTAssertFalse(driver.exists(label: caption), "\(caption) is left out when empty")
        }
    }

    // MARK: - Videos

    func testLongVideoMetadataStaysOnScreen() {
        let tag = seedCachedTag(id: 1809)
        let video = DPVideo()
        video.youTubeCode = "video12345"
        video.sungBy = "The International Harmony Society Quartet of the Greater Metropolitan Area"
        video.sungKey = "Bb"
        video.isMultitrack = true
        video.posted = Date(timeIntervalSince1970: 1_650_000_000)
        tag.videos = [video]
        tag.cache()
        for category in [UIContentSizeCategory.large, .accessibilityExtraExtraExtraLarge] {
            let detail = mountDetail(size: CGSize(width: 320, height: 800), category: category)
            detail.model.selectedPage = .videos
            ScreenCatalog.settle(0.4)
            let row = UIDriver(window).elements(labelPrefix: "Video sung by The International").first?.accessibilityFrame
            XCTAssertNotNil(row)
            XCTAssertLessThanOrEqual(row?.maxX ?? .infinity, 320.5, "The row stays inside the screen")
            XCTAssertGreaterThanOrEqual(row?.height ?? 0, 60, "The row grows to hold its lines")
        }
    }

    private func seedMinimalTag() {
        let tag = seedCachedTag(id: 4243, title: "Short and sweet", lyrics: nil, withTracks: false, withSheetMusic: false)
        tag.alternativeTitle = nil
        tag.notes = nil
        tag.arranger = nil
        tag.cache()
    }

    // MARK: - Beside a list (iPad)

    private func padSplit(detail: UIViewController, list: UIViewController) -> UISplitViewController {
        let split = UISplitViewController(style: .doubleColumn)
        split.preferredDisplayMode = .oneBesideSecondary
        split.preferredSplitBehavior = .tile
        DPAppDelegate.installSharedBackground(in: split.view)
        split.setViewController(UINavigationController(rootViewController: list), for: .primary)
        split.setViewController(UINavigationController(rootViewController: detail), for: .secondary)
        let window = ScreenCatalog.makeWindow()
        window.rootViewController = split
        window.makeKeyAndVisible()
        self.window = window
        return split
    }

    func testCollapsingTheSplitDropsTheListControlsAndExpandingBringsThemBack() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "The split is iPad only")
        defer { DPAppDelegate.removeSharedBackground() }
        seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let source = TMTestSource([1809, 42])
        detail.source = source
        let split = padSplit(detail: detail, list: UIViewController())
        spinUntil("expanded", timeout: 5) { detail.model.tag != nil && detail.model.expanded }
        XCTAssertEqual(detail.keyCommands?.count ?? 0, 2)

        window.traitOverrides.horizontalSizeClass = .compact
        spinUntil("collapsed") { split.isCollapsed }
        NotificationCenter.default.post(Notification(name: .TMTagSelectionDidChange, object: split))
        ScreenCatalog.settle(0.4)
        XCTAssertFalse(detail.model.expanded)
        XCTAssertEqual(detail.keyCommands?.count ?? 0, 0, "No stepping without the list beside the tag")
        XCTAssertFalse(UIDriver(window).exists(label: "Next tag"))

        window.traitOverrides.horizontalSizeClass = .regular
        spinUntil("expanded again") { !split.isCollapsed }
        NotificationCenter.default.post(Notification(name: .TMTagSelectionDidChange, object: split))
        ScreenCatalog.settle(0.4)
        XCTAssertTrue(detail.model.expanded)
        XCTAssertEqual(detail.keyCommands?.count ?? 0, 2)
        withExtendedLifetime(source) {}
    }

    func testSheetMusicStaysInTheDetailColumnAndCanGoFullScreen() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "The split is iPad only")
        defer { DPAppDelegate.removeSharedBackground() }
        seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let list = UIViewController()
        let split = padSplit(detail: detail, list: list)
        spinUntil("expanded", timeout: 5) { detail.model.tag != nil && detail.model.expanded }
        let file = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("sheet-\(UUID().uuidString).pdf")
        try UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: 612, height: 792)).pdfData { $0.beginPage() }.write(to: file)
        defer { try? FileManager.default.removeItem(at: file) }
        detail.model.navigator.showSheetMusic(TMSheetMusicDocument(fileURL: file, title: "Lost", writtenKey: "Bb"))
        let navigation = try XCTUnwrap(detail.navigationController)
        spinUntil("the reader is pushed") { navigation.topViewController is TMSheetMusicViewController }
        let reader = try XCTUnwrap(navigation.topViewController as? TMSheetMusicViewController)
        ScreenCatalog.settle(0.5)
        XCTAssertTrue(split.viewController(for: .secondary) === navigation, "The reader opens in the detail column")
        func previewer(_ controller: UIViewController) -> UIViewController? {
            if NSStringFromClass(type(of: controller)).contains("QLPreviewController") { return controller }
            return controller.children.lazy.compactMap(previewer).first
        }
        let quickLook = try XCTUnwrap(previewer(reader), "QuickLook shows the page")
        let pageFrame = quickLook.view.convert(quickLook.view.bounds, to: window)
        let listFrame = list.view.convert(list.view.bounds, to: window)
        XCTAssertGreaterThanOrEqual(pageFrame.minX, listFrame.maxX - 1, "The page is clear of the list")
        let driver = UIDriver(window)
        XCTAssertTrue(driver.exists(label: "Full screen"))
        XCTAssertTrue(driver.exists(id: "sheet.key"))
        XCTAssertTrue(driver.exists(label: "Share"))

        let before = split.preferredDisplayMode
        reader.toggleFullScreen()
        XCTAssertEqual(split.preferredDisplayMode, .secondaryOnly)
        ScreenCatalog.settle(0.4)
        XCTAssertTrue(UIDriver(window).exists(label: "Show list"))
        reader.toggleFullScreen()
        XCTAssertEqual(split.preferredDisplayMode, before)

        reader.toggleFullScreen()
        navigation.popViewController(animated: false)
        ScreenCatalog.settle(0.3)
        XCTAssertEqual(split.preferredDisplayMode, before, "Leaving the reader gives the list back")
    }

    func testTheSplitPaintsOneWatermarkBehindBothColumns() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad, "The split is iPad only")
        defer { DPAppDelegate.removeSharedBackground() }
        seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        let split = padSplit(detail: detail, list: UIViewController())
        spinUntil("loaded", timeout: 5) { detail.model.tag != nil }
        let marks = descendants(of: split.view) { $0.accessibilityIdentifier == "background.logo.vector" }
        XCTAssertEqual(marks.count, 1, "One watermark, drawn behind the whole split")
        XCTAssertTrue(descendants(of: detail.view) { $0.accessibilityIdentifier == "background.logo.vector" }.isEmpty)
        XCTAssertEqual(detail.view.backgroundColor, .clear)
    }

    func testThePlaceholderStaysCenteredAndReadableAtLargeText() {
        for category in [UIContentSizeCategory.large, .accessibilityExtraExtraExtraLarge] {
            let window = ScreenCatalog.makeWindow()
            window.traitOverrides.preferredContentSizeCategory = category
            window.rootViewController = TMTagPlaceholderController()
            window.makeKeyAndVisible()
            self.window = window
            ScreenCatalog.settle(0.3)
            let heading = frame("Pick a tag")
            let body = frame("Choose a tag from the list. Its summary, tracks, sheet music, and videos open here.")
            XCTAssertEqual(heading.midX, window.bounds.midX, accuracy: 1, "Centered")
            XCTAssertGreaterThan(body.minY, heading.maxY - 1, "The explanation follows the heading")
            XCTAssertLessThanOrEqual(body.width, 480.5, "Lines stay a readable length")
            XCTAssertGreaterThanOrEqual(body.minX, 23.5)
            XCTAssertLessThanOrEqual(body.maxX, window.bounds.width - 23.5)
        }
    }
}
