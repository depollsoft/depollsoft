//
//  TagMasterRound2Tests.swift
//  tagmasterTests
//
//  Regression tests for the second comparison of the SwiftUI screens with the
//  UIKit ones (the Codex and Opus audits): each pins a UIKit look or behaviour
//  the port had drifted from.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class TagMasterRound2Tests: TMBehaviorTestCase {
    private func mountDetail(_ tagId: Int32 = 1809, size: CGSize = TMBehaviorTestCase.portrait) -> TagDetailViewController {
        let detail = TagDetailViewController()
        detail.tagId = tagId
        mountInNavigation(detail, size: size)
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending }
        ScreenCatalog.settle(0.3)
        return detail
    }

    // MARK: Tag detail

    func testShareHandsTheActivitySheetTheTitleLineAndTheLink() throws {
        seedCachedTag(id: 1809, title: "Lost")
        let detail = mountDetail()
        let items = try XCTUnwrap(detail.model.shareItems)
        XCTAssertEqual(items.count, 2, "UIKit shared two items, not one item and a message")
        XCTAssertEqual(items.first as? String, "Lost - Tag Master for iOS")
        XCTAssertEqual((items.last as? URL)?.absoluteString, "http://tags.depoll.com/tag.php?id=1809")
        UIDriver(window).tap(label: "Share")
        spinUntil("the activity sheet is up") {
            var top = self.window.rootViewController
            while let next = top?.presentedViewController { top = next }
            return top is UIActivityViewController
        }
    }

    func testANewlyLoadedTagOffersRateAgainButAFailedRefreshKeepsRated() {
        let loader = TMControlledTagLoader()
        let model = TagDetailModel(loader: loader)
        model.show(tagId: 1809)
        let tag = seedCachedTag(id: 1809)
        loader.finish(0, with: tag)
        model.summary.rate(5) { _, _ in true }
        spinUntil("the rating lands") { model.summary.rated }
        model.refresh()
        loader.finish(1, with: nil)
        XCTAssertTrue(model.summary.rated, "A refresh that failed loaded nothing new")
        model.refresh()
        loader.finish(2, with: tag)
        XCTAssertFalse(model.summary.rated, "UIKit's refreshView put Rate back on every loaded tag")
    }

    func testThePageBarKeepsUnselectedItemsInInkWhileTheActionsAreUp() throws {
        seedCachedTag(id: 1809)
        let detail = mountDetail()
        detail.model.showActions()
        spinUntil("the actions are up") {
            var top = self.window.rootViewController
            while let next = top?.presentedViewController { top = next }
            return top is UIAlertController
        }
        let bars = descendants(of: UITabBar.self, in: window)
        let bar = try XCTUnwrap(bars.first)
        XCTAssertEqual(bar.standardAppearance.stackedLayoutAppearance.normal.iconColor, .label)
    }

    func testPageContentTakesTheWidthUIKitsReadableGuideGives() throws {
        // The UIKit pages sat on a 16-point-margin container's readable guide. The
        // page measures that guide in place rather than assuming 672 points, which
        // is not what UIKit gives on every system and page width.
        for width in [375.0, 1200.0] {
            let page = UIHostingController(rootView: TMPageScroll {
                Color.red.frame(height: 20)
                    .accessibilityElement()
                    .accessibilityLabel("probe.content")
            })
            mount(page, size: CGSize(width: width, height: 800))
            settle()
            ScreenCatalog.settle(0.2)
            let probe = try XCTUnwrap(descendants(of: UIView.self, in: window).first { $0 is TMReadableProbe.Probe })
            let guide = probe.readableContentGuide.layoutFrame.width
            let content = try XCTUnwrap(UIDriver(window).element(label: "probe.content")).accessibilityFrame.width
            XCTAssertEqual(content, guide, accuracy: 1, "page \(width)")
            XCTAssertLessThanOrEqual(content, width - 32 + 1)
        }
    }

    func testPickerRowsPlaceTheirTextAsTheLegacyCellDid() throws {
        // Measured from a UITableViewCell: a narrow icon leaves the text 50 points in;
        // person.2 is wide enough to push it 15 points past the icon's edge.
        let heart = try XCTUnwrap(UIImage(systemName: "heart"))
        let pair = try XCTUnwrap(UIImage(systemName: "person.2"))
        XCTAssertEqual(TMPickerRowLayout.textInset(for: heart), 50, accuracy: 0.01)
        XCTAssertEqual(TMPickerRowLayout.textInset(for: pair),
                       TMPickerRowLayout.iconCenter + pair.size.width / 2 + 15, accuracy: 0.01)
        XCTAssertGreaterThan(TMPickerRowLayout.textInset(for: pair), 50)
    }

    func testTheSheetMusicButtonIsUIKitsFilledButton() throws {
        seedCachedTag(id: 1809)
        _ = mountDetail()
        let buttons = descendants(of: UIButton.self, in: window).filter { $0.configuration?.title == "Sheet Music"
            || $0.title(for: .normal) == "Sheet Music" }
        let button = try XCTUnwrap(buttons.first)
        XCTAssertEqual(button.configuration?.cornerStyle, .medium)
        XCTAssertEqual(button.configuration?.contentInsets, NSDirectionalEdgeInsets(top: 8, leading: 44, bottom: 8, trailing: 44))
        XCTAssertEqual(button.configuration?.image, UIImage(systemName: "doc.richtext"))
    }

    func testThePlayersTwoCaptionsShareOneWidthSoBothSlidersEndTogether() {
        // UIKit held the Balance caption to the counter's width, at least 72 points.
        for size in [DynamicTypeSize.large, .xxxLarge] {
            for counter in ["0.0/0.0s", "128.4/256.8s"] {
                let widths = [true, false].map { shows in
                    UIHostingController(rootView: TMPlayerCaption(counter: counter, showsCounter: shows)
                        .environment(\.dynamicTypeSize, size)).sizeThatFits(in: CGSize(width: 500, height: 100)).width
                }
                XCTAssertEqual(widths[0], widths[1], accuracy: 0.01, "\(size) \(counter)")
                XCTAssertGreaterThanOrEqual(widths[0], 72)
            }
        }
    }

    // MARK: Lists

    func testRowsDoNothingWhileHomeIsEditing() {
        seedLists(favorite: [1809], lists: [(key: "set", name: "Set", ids: [])])
        let navigator = RecordingNavigator()
        let home = TMHomeModel(catalog: TMFixtureCatalog(available: 1).catalog, navigator: navigator)
        home.isEditing = true
        home.openTag(1809)
        home.openList("set")
        home.openTeachable()
        home.activate("Browse")
        home.activate("Open Tag")
        home.newList()
        XCTAssertEqual(navigator.shownTags, [])
        XCTAssertTrue(navigator.destinations.isEmpty)
        XCTAssertNil(home.openTagPrompt)
        XCTAssertNil(home.namePrompt)
        home.isEditing = false
        home.openTag(1809)
        XCTAssertEqual(navigator.shownTags, [1809])
    }

    func testAListsRowsDoNothingWhileEditing() {
        seedLists(lists: [(key: "set", name: "Set", ids: [1809])])
        let navigator = RecordingNavigator()
        let list = TMTagListModel(kind: .custom("set"), navigator: navigator)
        list.isEditing = true
        list.open(1809)
        XCTAssertEqual(navigator.shownTags, [])
    }

    func testTheFilterMenuIsUIKitsGrayButtonAtAccessibilitySizes() throws {
        var selection = 0
        let row = TMFilterRow(filter: TMFilter(title: "Parts", choices: ["Any", "3", "4", "5"]),
                              selection: Binding(get: { selection }, set: { selection = $0 }))
        let host = UIHostingController(rootView: List { row }.environment(\.dynamicTypeSize, .accessibility2))
        mount(host)
        settle()
        let button = try XCTUnwrap(descendants(of: UIButton.self, in: window).first { $0.accessibilityLabel == "Parts" })
        XCTAssertTrue(String(describing: button.configuration?.image).contains("chevron.up.chevron.down"))
        XCTAssertEqual(button.configuration?.imagePlacement, .trailing)
        XCTAssertTrue(button.showsMenuAsPrimaryAction)
        XCTAssertEqual(button.title(for: .normal), "Any")
        XCTAssertGreaterThanOrEqual(button.bounds.height, 44)
        let actions = button.menu?.children.compactMap { $0 as? UIAction } ?? []
        XCTAssertEqual(actions.map(\.title), ["Any", "3", "4", "5"])
        XCTAssertEqual(actions.first?.state, .on)
    }

    func testTheShellGivesTheWindowTheAccentBeforeAnyScreenAppears() {
        let router = TMRouter()
        let shell = mountShell(router)
        XCTAssertEqual(shell.tintColor, DPAppDelegate.accentColor())
    }

    // MARK: Helpers

    private func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }
}
