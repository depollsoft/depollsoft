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
import AVFoundation
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

    func testTheSheetMusicButtonIsUIKitsFilledButton() throws {
        seedCachedTag(id: 1809)
        _ = mountDetail()
        let buttons = descendants(of: UIButton.self, in: window).filter { $0.configuration?.title == "Sheet Music"
            || $0.title(for: .normal) == "Sheet Music" }
        let button = try XCTUnwrap(buttons.first)
        XCTAssertEqual(button.configuration?.cornerStyle, .medium)
        XCTAssertEqual(button.configuration?.contentInsets, NSDirectionalEdgeInsets(top: 8, leading: 44, bottom: 8, trailing: 44))
        XCTAssertTrue(String(describing: button.configuration?.image).contains("system: doc.richtext)"))
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

    // MARK: Helpers

    private func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }
}

// MARK: - Round 3: checked against independent UIKit fixtures and real outcomes

@MainActor
final class TagMasterRound3Tests: TMBehaviorTestCase {
    /// A reading once layout has settled: the same non-nil value held for a third of
    /// a second, within `timeout`, instead of a fixed pause. (Two back-to-back
    /// readings can agree mid-layout on a loaded runner.)
    func stable<T: Equatable>(_ description: String, timeout: TimeInterval = 10, _ read: () -> T?) -> T? {
        var last: T?
        var current: T?
        var heldSince = Date()
        spinUntil(description, timeout: timeout) {
            current = read()
            if current == nil || current != last { heldSince = Date() }
            last = current
            return current != nil && Date().timeIntervalSince(heldSince) >= 0.3
        }
        return current
    }

    private func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }

    // MARK: Backdrops

    func testThePlaceholderAndEveryColumnDrawTheirOwnSurfaceBesideTheList() throws {
        // Beside the list, a screen draws its colour and the pole where the window's
        // one watermark lies: the same pixel as the whole-window pole at that point.
        let window = CGRect(x: 0, y: 0, width: 1000, height: 800)
        func render(_ backdrop: TMBackdrop, frame: CGRect) -> UIImage {
            let host = UIHostingController(rootView: TMScreenBackground().environment(\.tmBackdrop, backdrop))
            let container = UIWindow(frame: window)
            container.rootViewController = UIViewController()
            container.isHidden = false
            host.view.frame = frame
            container.rootViewController?.view.addSubview(host.view)
            host.view.layoutIfNeeded()
            ScreenCatalog.settle(0.1)
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            return UIGraphicsImageRenderer(bounds: host.view.bounds, format: format).image { _ in
                host.view.drawHierarchy(in: host.view.bounds, afterScreenUpdates: true)
            }
        }
        let whole = render(.own, frame: window)
        let column = CGRect(x: 400, y: 0, width: 600, height: 800)
        let slice = render(.windowSlice, frame: column)
        var matches = 0, poleInSlice = 0
        for y in stride(from: 80, to: 740, by: 20) {
            for x in stride(from: 10, to: 590, by: 20) {
                let a = try XCTUnwrap(whole.tmPixel(x: x + 400, y: y)), b = try XCTUnwrap(slice.tmPixel(x: x, y: y))
                if abs(Int(a.r) - Int(b.r)) <= 6 { matches += 1 }
                if b.r < 235 { poleInSlice += 1 }
            }
        }
        XCTAssertGreaterThan(poleInSlice, 20, "The slice shows part of the pole")
        XCTAssertGreaterThan(Double(matches) / Double(33 * 29), 0.97, "The slice lines up with the window's pole")
    }

    // MARK: Summary

    func testKeyAndSheetMusicShareOneHeightThatGrowsAndShrinksWithTheText() throws {
        seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        // Tall enough that the key and Sheet Music stay on screen at the largest text.
        mountInNavigation(detail, size: CGSize(width: 375, height: 1800))
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending }
        spinUntil("the key is shown", timeout: 5) { UIDriver(self.window).exists(id: "summary.key") }
        // The key's drawn outline, not its slot: its top and bottom edges where they
        // run straight, clear of the rounded corners and of the centred content.
        func heights() throws -> (key: CGFloat, sheet: CGFloat) {
            let button = try XCTUnwrap(descendants(of: UIButton.self, in: window)
                .first { $0.title(for: .normal) == "Sheet Music" })
            var result: (key: CGFloat, sheet: CGFloat) = (0, 0)
            spinUntil("the key and Sheet Music settle", timeout: 5) {
                guard let slot = UIDriver(self.window).element(id: "summary.key")?.accessibilityFrame else { return false }
                let image = ScreenCatalog.image(of: self.window)
                let x = slot.minX + 12
                var rows: [CGFloat] = []
                for y in stride(from: slot.minY - 6, to: slot.maxY + 6, by: 1 / image.scale) {
                    let px = Int(x * image.scale), py = Int(y * image.scale)
                    guard let p = image.tmPixel(x: px, y: py) else { continue }
                    if Int(p.b) - Int(p.r) > 60 { rows.append(y) }
                }
                guard let first = rows.first, let last = rows.last else { return false }
                let drawn = last - first + 1 / image.scale
                let sheet = button.bounds.height
                let settled = abs(drawn - result.key) < 0.5 && abs(sheet - result.sheet) < 0.5
                result = (drawn, sheet)
                return settled
            }
            return result
        }
        let small = try heights()
        XCTAssertEqual(small.key, small.sheet, accuracy: 1, "UIKit held both to one height")
        window.traitOverrides.preferredContentSizeCategory = .accessibilityExtraExtraLarge
        let large = try heights()
        XCTAssertEqual(large.key, large.sheet, accuracy: 1, "The key's outline grows with Sheet Music")
        XCTAssertGreaterThan(large.sheet, small.sheet + 10, "Both grow with the text")
        window.traitOverrides.preferredContentSizeCategory = .large
        let back = try heights()
        XCTAssertEqual(back.key, back.sheet, accuracy: 1)
        XCTAssertEqual(back.sheet, small.sheet, accuracy: 0.5, "Both shrink back, not held at the larger height")
    }

    func testTheKeysOutlineStretchesToATallerPartner() throws {
        // Whichever of the two is taller, both are drawn at its height, outline and all.
        let model = TagSummaryModel(busy: TMBusyCount())
        let host = UIHostingController(rootView: TMMatchedHeightStack {
            TMKeyNoteButton(model: model, title: "Bb", fillsHeight: true)
                .layoutValue(key: TMMatchesHeight.self, value: true)
            Color.clear.frame(height: 120)
                .layoutValue(key: TMGapBefore.self, value: 16)
                .layoutValue(key: TMMatchesHeight.self, value: true)
        }.frame(width: 300).fixedSize(horizontal: false, vertical: true).padding(20))
        let window = mount(host, size: CGSize(width: 340, height: 400))
        var drawn: CGFloat = 0
        spinUntil("the outline is drawn", timeout: 5) {
            let image = ScreenCatalog.image(of: window)
            var rows: [CGFloat] = []
            let x: CGFloat = 20 + 12
            for y in stride(from: CGFloat(0), to: 400, by: 1 / image.scale) {
                guard let p = image.tmPixel(x: Int(x * image.scale), y: Int(y * image.scale)) else { continue }
                if Int(p.b) - Int(p.r) > 60 { rows.append(y) }
            }
            guard let first = rows.first, let last = rows.last else { return false }
            drawn = last - first + 1 / image.scale
            return true
        }
        XCTAssertEqual(drawn, 120, accuracy: 1, "The key's outline takes its partner's height")
    }

    func testPageContentMatchesAUIKitReadableGuideAtEveryWidthAndTextSize() throws {
        for size in [CGSize(width: 375, height: 800), CGSize(width: 844, height: 390), CGSize(width: 1200, height: 800)] {
            for category in [UIContentSizeCategory.large, .accessibilityExtraExtraExtraLarge] {
                // The UIKit page (DPTagPageControllerBase): a bare container with 16-point
                // margins filling a scroll view's width, content on its readable guide.
                let fixture = UIViewController()
                let scroller = UIScrollView(frame: CGRect(origin: .zero, size: size))
                let container = UIView(frame: CGRect(origin: .zero, size: CGSize(width: size.width, height: 100)))
                container.directionalLayoutMargins = NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)
                fixture.view.addSubview(scroller)
                scroller.addSubview(container)
                let fixtureWindow = mount(fixture, size: size)
                fixtureWindow.traitOverrides.preferredContentSizeCategory = category
                container.layoutIfNeeded()
                let expected = container.readableContentGuide.layoutFrame.width

                let page = UIHostingController(rootView: TMPageScroll {
                    Color.red.frame(height: 20).accessibilityElement().accessibilityLabel("probe.content")
                })
                mount(page, size: size)
                window.traitOverrides.preferredContentSizeCategory = category
                settle()
                let content = try XCTUnwrap(stable("the page lays out") {
                    UIDriver(self.window).element(label: "probe.content")?.accessibilityFrame.width
                })
                XCTAssertEqual(content, expected, accuracy: 1, "\(size.width) \(category.rawValue)")
            }
        }
    }

    func testPickerRowsPlaceIconAndTextWhereTheUIKitPickerDid() throws {
        // Where the icon's ink and the text's ink start in the "New list…" row: the
        // icon is accent-coloured, the text neutral dark.
        func inkEdges(in window: UIWindow, row: CGRect) -> (iconMid: CGFloat, text: CGFloat)? {
            let image = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
                window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
            }
            var iconXs: [Int] = [], textX: Int?
            for y in stride(from: Int(row.minY + row.height * 0.25), to: Int(row.maxY - row.height * 0.25), by: 1) {
                for x in Int(row.minX)..<Int(row.maxX) {
                    guard let p = image.tmPixel(x: x, y: y, scaled: true) else { continue }
                    let (r, g, b) = (Int(p.r), Int(p.g), Int(p.b))
                    if b - r > 60 { iconXs.append(x) }
                    if max(r, g, b) < 90, abs(r - b) < 20, x > (iconXs.max() ?? 0) + 2 { textX = min(textX ?? x, x) }
                }
            }
            guard let low = iconXs.min(), let high = iconXs.max(), let textX else { return nil }
            return (CGFloat(low + high + 1) / 2, CGFloat(textX))
        }
        // As presented: a half-height sheet on iPhone; a 340-point popover on iPad.
        // Both are compact width.
        let pad = UIDevice.current.userInterfaceIdiom == .pad
        let size = CGSize(width: pad ? 340 : 375, height: 500)
        let fixture = TMLegacyPickerFixture()
        let fixtureWindow = mount(fixture, size: size)
        fixtureWindow.traitOverrides.horizontalSizeClass = .compact
        settle()
        let cell = try XCTUnwrap(fixture.tableView.cellForRow(at: IndexPath(row: 0, section: 0)))
        let uikit = try XCTUnwrap(inkEdges(in: fixtureWindow, row: cell.convert(cell.bounds, to: nil)))

        let picker = UIHostingController(rootView: TMListPicker(model: TMListPickerModel(tagId: 1809)))
        mount(picker, size: size)
        window.traitOverrides.horizontalSizeClass = .compact
        settle()
        let row = try XCTUnwrap(stable("the picker lays out") {
            UIDriver(self.window).element(id: "picker.row.new")?.accessibilityFrame
        })
        let swiftUI = try XCTUnwrap(inkEdges(in: window, row: row))
        XCTAssertEqual(swiftUI.iconMid, uikit.iconMid, accuracy: 1, "icon")
        XCTAssertEqual(swiftUI.text, uikit.text, accuracy: 1, "text")
    }

    func testThePlayersSlidersEndTogetherAtEveryTextSize() throws {
        let tag = seedCachedTag(id: 1809)
        let detail = TagDetailViewController()
        detail.tagId = 1809
        mountInNavigation(detail)
        spinUntil("the detail settles", timeout: 5) { detail.model.tag != nil }
        detail.model.selectedPage = .tracks
        let track = try XCTUnwrap(tag.tracks.first as? DPTrack)
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 22050, channels: 2))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 2205))
        buffer.frameLength = 2205
        detail.model.tracks.present(track, buffer: buffer)
        defer { detail.model.tracks.stopPlayback() }
        for category in [UIContentSizeCategory.large, .extraExtraExtraLarge] {
            window.traitOverrides.preferredContentSizeCategory = category
            func visibleSliders() -> [UISlider] {
                descendants(of: UISlider.self, in: window).filter { $0.window != nil && !$0.isHidden }
                    .sorted { $0.convert($0.bounds, to: nil).minY < $1.convert($1.bounds, to: nil).minY }
            }
            _ = stable("the player lays out at \(category.rawValue)") { () -> [CGRect]? in
                let frames = visibleSliders().map { $0.convert($0.trackRect(forBounds: $0.bounds), to: nil) }
                return frames.count == 2 ? frames : nil
            }
            let sliders = visibleSliders()
            XCTAssertEqual(sliders.count, 2)
            guard sliders.count == 2 else { return }
            let position = sliders[0].convert(sliders[0].bounds, to: nil)
            let balance = sliders[1]
            // UIKit: the balance slider, its L and R images inside it, spanned exactly the
            // position slider's width. The fixture is that slider at that frame.
            let fixture = UISlider(frame: position)
            fixture.minimumValueImage = UIImage(systemName: "l.circle")
            fixture.maximumValueImage = UIImage(systemName: "r.circle")
            fixture.layoutIfNeeded()
            let expected = fixture.trackRect(forBounds: fixture.bounds).offsetBy(dx: position.minX, dy: 0)
            let actual = balance.convert(balance.trackRect(forBounds: balance.bounds), to: nil)
            XCTAssertEqual(actual.minX, expected.minX, accuracy: 1, "\(category.rawValue): balance track start")
            XCTAssertEqual(actual.maxX, expected.maxX, accuracy: 1, "\(category.rawValue): balance track end")
        }
    }

    // MARK: The bar

    func testBarItemsUseUIKitsInkWhenEnabledAndDisabled() throws {
        // The brightest ink in an item's area (the 99.5th percentile, clear of the
        // glass rim), a UIKit bar against the SwiftUI one, for a title and an image.
        func ink(_ image: UIImage, _ rect: CGRect) -> Int {
            var values: [Int] = []
            for y in stride(from: rect.minY, to: rect.maxY, by: 1 / image.scale) {
                for x in stride(from: rect.minX, to: rect.maxX, by: 1 / image.scale) {
                    if let p = image.tmPixel(x: Int(x * image.scale), y: Int(y * image.scale)) { values.append(Int(p.r)) }
                }
            }
            values.sort()
            return values.isEmpty ? 0 : values[values.count * 995 / 1000]
        }
        /// Whether any layer under `layer` is still running an animation that ends: a
        /// fade or a move. The glass bar keeps its elements' "match-*" animations
        /// attached for good (infinite duration), so those never count.
        func animating(_ layer: CALayer) -> Bool {
            let finite = (layer.animationKeys() ?? []).contains { key in
                guard let animation = layer.animation(forKey: key) else { return false }
                return animation.duration.isFinite && animation.repeatCount.isFinite
                    && animation.repeatCount < .greatestFiniteMagnitude && animation.repeatDuration.isFinite
            }
            return finite || (layer.sublayers ?? []).contains(where: animating)
        }
        /// The two items' ink once the bar has settled: no finite animation left on the bar,
        /// and the same reading held for half a second. A steady reading alone was not
        /// enough: on a loaded CI runner the renderer can stall mid-fade for longer than
        /// that (a disabled Edit read 66 against UIKit's 81).
        func inks(_ controller: UIViewController) -> (left: Int, right: Int) {
            let window = mount(controller, size: CGSize(width: 390, height: 300))
            var last: (left: Int, right: Int)?
            var heldSince = Date()
            var reading = (left: 0, right: 0)
            spinUntil("the bar settles", timeout: 15) {
                guard let bar = self.descendants(of: UINavigationBar.self, in: window).first(where: { $0.window != nil }) else { return false }
                let frame = bar.convert(bar.bounds, to: nil)
                guard frame.height > 0 else { return false }
                if animating(bar.layer) {
                    last = nil
                    return false
                }
                let image = ScreenCatalog.image(of: window)
                reading = (ink(image, CGRect(x: 0, y: frame.minY, width: 110, height: frame.height)),
                           ink(image, CGRect(x: frame.maxX - 80, y: frame.minY, width: 80, height: frame.height)))
                if last.map({ $0 != reading }) ?? true { heldSince = Date() }
                last = reading
                return Date().timeIntervalSince(heldSince) >= 0.5
            }
            return reading
        }
        for enabled in [true, false] {
            let plain = UIViewController()
            plain.title = "Home"
            let edit = UIBarButtonItem(title: "Edit", style: .plain, target: nil, action: nil)
            let gear = UIBarButtonItem(image: UIImage(systemName: "gearshape", withConfiguration:
                UIImage.SymbolConfiguration(pointSize: 17, weight: .regular, scale: .medium)), style: .plain, target: nil, action: nil)
            edit.isEnabled = enabled
            gear.isEnabled = enabled
            plain.navigationItem.leftBarButtonItem = edit
            plain.navigationItem.rightBarButtonItem = gear
            let navigation = UINavigationController(rootViewController: plain)
            TMBarAppearance.apply(to: navigation.navigationBar)
            let uikit = inks(navigation)
            let swiftUI = UIHostingController(rootView: NavigationStack {
                Color(uiColor: .systemBackground).ignoresSafeArea()
                    .navigationTitle("Home").navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) { TMEditButton(isEditing: .constant(false)).disabled(!enabled) }
                        ToolbarItem(placement: .topBarTrailing) { TMBarButton("gearshape", label: "Settings") {}.disabled(!enabled) }
                    }
                    .tmCharcoalBar()
            })
            let ours = inks(swiftUI)
            // The inks match to a level or two at 3x; at 2x a symbol's thin strokes
            // rasterize a little differently in UIKit and SwiftUI, never by more than 12.
            XCTAssertEqual(ours.left, uikit.left, accuracy: 12, "Edit, enabled \(enabled)")
            XCTAssertEqual(ours.right, uikit.right, accuracy: 12, "gear, enabled \(enabled)")
        }
    }

    func testTheBarRepairsEachPropertyOnItsOwnAndKeepsHomesFont() throws {
        let bar = UINavigationBar(frame: CGRect(x: 0, y: 0, width: 375, height: 44))
        TMBarAppearance.apply(to: bar)
        XCTAssertEqual(bar.standardAppearance.backgroundColor, TMBarAppearance.charcoal)
        // SwiftUI's kind of rewrite: the background stays charcoal, the title colour goes.
        let font = TMHomeTitle.handwritingFont(.headline, size: 22, maximum: 26)
        let corrupted = bar.standardAppearance.copy()
        corrupted.titleTextAttributes = [.font: font, .foregroundColor: UIColor.black]
        corrupted.largeTitleTextAttributes = [.foregroundColor: UIColor.label]
        bar.standardAppearance = corrupted
        bar.scrollEdgeAppearance = corrupted
        bar.tintColor = .systemBlue
        TMBarAppearance.apply(to: bar)
        for appearance in [bar.standardAppearance, bar.scrollEdgeAppearance, bar.compactAppearance,
                           bar.compactScrollEdgeAppearance].compactMap({ $0 }) {
            XCTAssertEqual(appearance.backgroundColor, TMBarAppearance.charcoal)
            XCTAssertEqual(appearance.titleTextAttributes[.foregroundColor] as? UIColor, .white)
            XCTAssertEqual(appearance.largeTitleTextAttributes[.foregroundColor] as? UIColor, .white)
        }
        XCTAssertEqual(bar.standardAppearance.titleTextAttributes[.font] as? UIFont, font, "Home's face survives")
        XCTAssertEqual(bar.tintColor, .white)
        // A bar already right is left alone: no new appearance objects.
        let settled = bar.standardAppearance
        TMBarAppearance.apply(to: bar)
        XCTAssertTrue(bar.standardAppearance === settled)
    }

    func testTheListMenuUsesTheBarInkLikeEveryOtherBarItem() throws {
        seedLists(lists: [(key: "set", name: "Set", ids: [1809])])
        let router = TMRouter()
        let shell = mountShell(router)
        router.showList(key: "set")
        UIDriver(shell).wait(5) { UIDriver(shell).exists(id: "list.menu") }
        let menu = try XCTUnwrap(UIDriver(shell).element(id: "list.menu"))
        let frame = menu.accessibilityFrame
        XCTAssertFalse(frame.isEmpty)
        let image = UIGraphicsImageRenderer(bounds: shell.bounds).image { _ in
            shell.drawHierarchy(in: shell.bounds, afterScreenUpdates: true)
        }
        // The symbol's strokes are white on the charcoal bar, never black.
        var brightest = 0
        for y in stride(from: Int(frame.minY) + 4, to: Int(frame.maxY) - 4, by: 1) {
            for x in stride(from: Int(frame.minX) + 4, to: Int(frame.maxX) - 4, by: 1) {
                if let pixel = image.tmPixel(x: x, y: y, scaled: true) { brightest = max(brightest, Int(pixel.r)) }
            }
        }
        XCTAssertGreaterThan(brightest, 200, "The menu's symbol is drawn in the bar's white ink")
    }

    // MARK: Launch

    func testTheWindowHasTheAccentBeforeItsFirstFrame() {
        let fresh = ScreenCatalog.makeWindow()
        fresh.tintColor = nil
        fresh.rootViewController = UIHostingController(rootView: TMRootView(router: TMRouter()))
        fresh.isHidden = false
        // One layout pass, no run-loop turn: nothing has been drawn yet.
        fresh.rootViewController?.view.layoutIfNeeded()
        XCTAssertEqual(fresh.tintColor, DPAppDelegate.accentColor)
        fresh.isHidden = true
    }

    func testPrivacyChoicesStaysPendingUntilUIKitAcceptsThePresentation() {
        let offer = TMPrivacyOffer()
        offer.retryDelay = 0.01
        var readiness: [TMPrivacyOffer.Readiness] = []
        var presented: [UIViewController] = []
        offer.hasChosen = { false }
        offer.readiness = { readiness.isEmpty ? .notYet : readiness.removeFirst() }
        offer.present = { root in
            presented.append(root)
            // Only a root in a window can present; UIKit refuses the others.
            if root.viewIfLoaded?.window != nil { root.present(UIViewController(), animated: false) }
        }
        // Not ready, then a root that is not in a window: refused, so still pending.
        let detached = UIViewController()
        readiness = [.notYet, .ready(detached)]
        offer.sceneBecameActive()
        spinUntil("both attempts ran") { presented.count == 1 && readiness.isEmpty }
        ScreenCatalog.settle(0.05)
        XCTAssertFalse(offer.shown)
        XCTAssertTrue(offer.pending, "A refused presentation leaves the offer waiting")
        // Busy (an alert is up): this activation's offer ends and stays pending.
        readiness = [.busy]
        offer.sceneBecameActive()
        ScreenCatalog.settle(0.1)
        XCTAssertFalse(offer.shown)
        XCTAssertTrue(offer.pending)
        // Leaving the active phase cancels attempts in flight.
        let root = UIViewController()
        mount(root)
        readiness = [.notYet, .ready(root)]
        offer.sceneBecameActive()
        offer.sceneResigned()
        ScreenCatalog.settle(0.2)
        XCTAssertNil(root.presentedViewController, "A stale attempt does not present")
        // The next activation presents, and then the offer is done.
        readiness = [.ready(root)]
        offer.sceneBecameActive()
        spinUntil("presented") { root.presentedViewController != nil }
        XCTAssertTrue(offer.shown)
        XCTAssertFalse(offer.pending)
        root.dismiss(animated: false)
    }

    // MARK: Editing

    func testEditingHomeDeletesAndReordersFavoritesThroughItsOwnControls() throws {
        seedCachedTag(id: 1809)
        seedCachedTag(id: 42, title: "Other")
        seedLists(favorite: [1809, 42])
        let home = TMScreens.home(navigator: RecordingNavigator(), catalog: TMFixtureCatalog(available: 1).catalog)
        let driver = mountScreen(home, size: CGSize(width: 375, height: 1200))
        let model = try XCTUnwrap(home.listing as? TMHomeModel)
        model.isEditing = true
        driver.wait { driver.elements(labelPrefix: "Remove, Lost").count == 1 }
        // The rows are not controls while editing, but each keeps its delete control
        // and its reorder handle.
        XCTAssertEqual(driver.elements.filter { $0.accessibilityLabel == "drag" }.count, 2, "Both favourites can be dragged")
        XCTAssertFalse(driver.elements.contains {
            ($0.accessibilityLabel ?? "").hasPrefix("Lost.") && !$0.accessibilityTraits.contains(.notEnabled)
        }, "The row itself is not offered as a control")
        // Delete, as the row's delete control does (the real tap and drag run in
        // RealAppFlowUITests: SwiftUI's edit controls do not take in-process activation).
        XCTAssertEqual(driver.elements.filter { $0.accessibilityLabel == "remove" }.count, 2, "Both rows keep a delete control")
        model.removeFavorites(at: IndexSet(integer: 0))
        driver.wait { model.favorites == [42] }
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [42], "The favourite is gone from the saved list")
        driver.wait { driver.elements(labelPrefix: "Remove, Lost").isEmpty }
        // Reorder, as the drag's drop does, while the rows stay disabled.
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        driver.wait { model.favorites == [42, 1809] }
        model.moveFavorite(from: IndexSet(integer: 1), to: 0)
        driver.wait { model.favorites == [1809, 42] }
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [1809, 42])
        // Top to bottom on screen, as the rows now stand.
        func order() -> [String] {
            driver.elements.filter { ($0.accessibilityLabel ?? "").hasPrefix("Remove, ") }
                .sorted { $0.accessibilityFrame.minY < $1.accessibilityFrame.minY }
                .compactMap(\.accessibilityLabel)
        }
        driver.wait { order().count == 2 && order()[0].contains("Tag ID 1809") }
        XCTAssertEqual(order().first?.contains("Tag ID 1809"), true, "The rows show the new order: \(order())")
        XCTAssertTrue(model.isEditing, "Editing continues")
    }

    func testRowsWhileEditingAreNotOfferedAsControlsButKeepTheirEditingActions() throws {
        seedLists(favorite: [1809], lists: [(key: "set", name: "Set", ids: [])])
        let navigator = RecordingNavigator()
        let home = TMScreens.home(navigator: navigator, catalog: TMFixtureCatalog(available: 1).catalog)
        let driver = mountScreen(home, size: CGSize(width: 375, height: 1200))
        let model = try XCTUnwrap(home.listing as? TMHomeModel)
        XCTAssertTrue(driver.isEnabled(id: "home.list.set"))
        model.isEditing = true
        driver.wait { !driver.isEnabled(id: "home.list.set") }
        for id in ["home.list.set", "home.lists.teachable"] {
            XCTAssertFalse(driver.isEnabled(id: id), "\(id) is not offered as a control while editing")
            _ = driver.element(id: id)?.accessibilityActivate()
        }
        ScreenCatalog.settle(0.2)
        XCTAssertTrue(navigator.destinations.isEmpty, "Activating a row while editing does nothing")
        let actions = driver.element(id: "home.list.set")?.accessibilityCustomActions?.map(\.name) ?? []
        // SwiftUI's edit-mode delete control reads "Remove"; UIKit's read "Delete".
        let deleteControls = driver.labels.filter { $0.hasPrefix("Remove, Set") }
        XCTAssertTrue(actions.contains { $0.localizedCaseInsensitiveContains("delete") } || !deleteControls.isEmpty,
                      "Delete stays: \(actions) \(driver.labels)")
        model.isEditing = false
        driver.wait { driver.isEnabled(id: "home.list.set") }
        driver.tap(id: "home.list.set")
        XCTAssertFalse(navigator.destinations.isEmpty, "Out of editing the row opens its list")
    }
}

/// The old list picker's table (TMListPickerController) with its "New list…" row.
final class TMLegacyPickerFixture: UITableViewController {
    init() { super.init(style: .insetGrouped) }
    required init?(coder: NSCoder) { fatalError("created in code") }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 1 }
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.textLabel?.font = .preferredFont(forTextStyle: .body)
        cell.textLabel?.text = "New list…"
        cell.imageView?.image = UIImage(systemName: "plus.circle")
        cell.imageView?.tintColor = DPAppDelegate.accentColor
        return cell
    }
}

extension UIImage {
    /// The pixel at a point, in points (`scaled`) or in the image's pixels.
    func tmPixel(x: Int, y: Int, scaled: Bool = false) -> (r: UInt8, g: UInt8, b: UInt8, a: UInt8)? {
        guard let cgImage else { return nil }
        let px = scaled ? Int(CGFloat(x) * scale) : x, py = scaled ? Int(CGFloat(y) * scale) : y
        guard px >= 0, py >= 0, px < cgImage.width, py < cgImage.height,
              let crop = cgImage.cropping(to: CGRect(x: px, y: py, width: 1, height: 1)) else { return nil }
        var bytes = [UInt8](repeating: 0, count: 4)
        let context = CGContext(data: &bytes, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
        context?.draw(crop, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        return (bytes[0], bytes[1], bytes[2], bytes[3])
    }
}
