//
//  TagDetailScreenCatalogTests.swift
//  tagmasterTests
//
//  Every state of the tag detail, rendered full screen inside the app's own
//  navigation chrome (and, on iPad, beside a list in the split). With
//  SCREEN_CATALOG_DIR set each state is written as a PNG; the SwiftUI detail was
//  diffed against the UIKit detail's captures this way (docs/ios-swiftui.md).
//

import XCTest
import UIKit
import ObjectiveC
@testable import tagmaster

/// Holds `+[DPTag loadTagById:refresh:]` for one tag id until released, so the
/// detail's first-load state can be captured.
final class TMHeldTagLoad {
    static let heldId: Int32 = 7777
    private static let gate = DispatchSemaphore(value: 0)
    private let method: Method
    private var original: IMP?

    init() {
        method = class_getClassMethod(DPTag.self, NSSelectorFromString("loadTagById:refresh:"))!
        let previous = method_getImplementation(method)
        typealias Load = @convention(c) (AnyObject, Selector, Int32, Bool) -> DPTag?
        let call = unsafeBitCast(previous, to: Load.self)
        let block: @convention(block) (AnyObject, Int32, Bool) -> DPTag? = { cls, id, refresh in
            if id == TMHeldTagLoad.heldId { TMHeldTagLoad.gate.wait() }
            return call(cls, NSSelectorFromString("loadTagById:refresh:"), id, refresh)
        }
        original = method_setImplementation(method, imp_implementationWithBlock(block))
    }

    func release() {
        TMHeldTagLoad.gate.signal()
        if let original { method_setImplementation(method, original) }
        original = nil
    }
}

/// A list the detail can step through, as Home or a query list would be.
final class TMCatalogSource: NSObject, TMTagListSource {
    var ids: [NSNumber]
    init(_ ids: [Int]) { self.ids = ids.map { NSNumber(value: $0) } }
    func tm_listedTagIds() -> [NSNumber] { ids }
    func tm_didStep(toTagId tagId: Int32) {}
}

@MainActor
class TagDetailScreenCatalogTests: TMBehaviorTestCase {
    nonisolated(unsafe) private var held: TMHeldTagLoad?

    override func setUp() {
        super.setUp()
        UIView.setAnimationsEnabled(false)
    }

    override func tearDown() {
        held?.release()
        held = nil
        window?.rootViewController?.dismiss(animated: false)
        DPAppDelegate.removeSharedBackground()
        super.tearDown()
    }

    // MARK: - Fixtures

    /// Every optional field filled.
    @discardableResult
    func seedFullTag(id: Int32 = 1809) -> DPTag {
        let tag = seedCachedTag(id: id, title: "Lost")
        tag.version = "Barbershop Harmony Society"
        tag.classicTagNumber = 12
        tag.provider = "The Tag Man"
        tag.providerWebsite = URL(string: "https://example.invalid/provider")
        tag.arrangerWebsite = URL(string: "https://example.invalid/arranger")
        tag.yearArranged = 1998
        tag.sungBy = "The Buffalo Bills"
        tag.sungYear = 1950
        tag.recordingMethod = "Each part is sung by its own voice, balanced left and right."
        tag.teachingVideo = "teach12345"
        tag.teacher = "Tag Teacher"
        let video = DPVideo()
        video.youTubeCode = "video12345"
        video.sungBy = "Main Street"
        video.sungKey = "Bb"
        video.isMultitrack = true
        video.posted = Date(timeIntervalSince1970: 1_650_000_000)
        let solo = DPVideo()
        solo.youTubeCode = "solo123456"
        solo.posted = nil
        tag.videos = [video, solo]
        tag.cache()
        return tag
    }

    /// Only what every tag has.
    @discardableResult
    func seedMinimalTag(id: Int32 = 4243) -> DPTag {
        let tag = seedCachedTag(id: id, title: "Short and sweet", lyrics: nil, withTracks: false, withSheetMusic: false)
        tag.alternativeTitle = nil
        tag.notes = nil
        tag.writtenKey = nil
        tag.arranger = nil
        tag.cache()
        return tag
    }

    // MARK: - Chrome, as DPAppDelegate builds it

    static let navigationAppearance: UINavigationBarAppearance = {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(white: 55.0 / 255.0, alpha: 1)
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        return appearance
    }()

    static func styleBar(_ navigation: UINavigationController) {
        let bar = navigation.navigationBar
        bar.standardAppearance = navigationAppearance
        bar.scrollEdgeAppearance = navigationAppearance
        bar.compactAppearance = navigationAppearance
        bar.compactScrollEdgeAppearance = navigationAppearance
        bar.tintColor = .white
        bar.overrideUserInterfaceStyle = .dark
        bar.barStyle = .black
        bar.isTranslucent = false
    }

    func appWindow(_ style: UIUserInterfaceStyle) -> UIWindow {
        if let old = self.window {
            old.rootViewController?.dismiss(animated: false)
            old.isHidden = true
            old.rootViewController = nil
        }
        let window = ScreenCatalog.makeWindow(style: style)
        window.backgroundColor = .systemBackground
        window.tintColor = DPAppDelegate.accentColor()
        self.window = window
        return window
    }

    /// iPhone: the detail pushed onto a navigation stack above a stand-in list.
    func showOnPhone(_ detail: UIViewController, style: UIUserInterfaceStyle = .light) {
        let window = appWindow(style)
        let root = UIViewController()
        root.title = "Home"
        let navigation = UINavigationController(rootViewController: root)
        Self.styleBar(navigation)
        navigation.navigationBar.prefersLargeTitles = true
        navigation.pushViewController(detail, animated: false)
        window.rootViewController = navigation
        window.makeKeyAndVisible()
    }

    /// iPad: the split, a stand-in list in the primary column, `detail` in the secondary.
    func showOnPad(_ detail: UIViewController, style: UIUserInterfaceStyle = .light) -> UISplitViewController {
        let window = appWindow(style)
        let split = UISplitViewController(style: .doubleColumn)
        DPAppDelegate.installSharedBackground(in: split.view)
        split.preferredDisplayMode = .oneBesideSecondary
        split.preferredSplitBehavior = .tile
        split.minimumPrimaryColumnWidth = 320
        split.maximumPrimaryColumnWidth = 400
        split.preferredPrimaryColumnWidthFraction = 0.36
        let list = UIViewController()
        list.title = "Tag Master"
        list.view.backgroundColor = .clear
        let primary = UINavigationController(rootViewController: list)
        Self.styleBar(primary)
        primary.navigationBar.prefersLargeTitles = true
        split.setViewController(primary, for: .primary)
        let secondary = UINavigationController(rootViewController: detail)
        Self.styleBar(secondary)
        split.setViewController(secondary, for: .secondary)
        window.rootViewController = split
        window.makeKeyAndVisible()
        return split
    }

    var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }

    func snap(_ name: String, settle seconds: TimeInterval = 0.8) {
        ScreenCatalog.capture(name, window: window, settle: seconds)
    }

    // MARK: - Driving the detail (the part that changes with the implementation)

    func makeDetail(tagId: Int32, source: TMTagListSource? = nil) -> TagDetailViewController {
        let detail = TagDetailViewController()
        detail.tagId = tagId
        detail.source = source
        return detail
    }

    func waitForLoad(_ detail: TagDetailViewController) {
        spinUntil("the detail settles", timeout: 5) { !detail.model.fetchPending }
    }

    func select(page: Int, in detail: TagDetailViewController) {
        detail.model.selectedPage = TagDetailModel.Page(rawValue: page)!
        ScreenCatalog.settle(0.6)
    }

    /// Stops the quartet so it is captured at rest.
    func stillQuartet(in detail: TagDetailViewController) {
        detail.model.applicationActive = false
    }

    func showActions(in detail: TagDetailViewController) {
        detail.model.showActions()
    }

    func showPicker(in detail: TagDetailViewController) {
        detail.model.showListPicker(from: detail.model.expanded ? .toolbar : .actions)
    }

    func showRating(in detail: TagDetailViewController) {
        detail.model.summary.showRating()
    }

    func dismissPresentations(in detail: TagDetailViewController) {
        detail.model.actionsPresented = false
        detail.model.pickerSource = nil
        detail.model.summary.ratingDialogPresented = false
        ScreenCatalog.settle(0.5)
    }

    // MARK: - iPhone

    func testPhoneSummary() {
        guard !isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            seedFullTag()
            let detail = makeDetail(tagId: 1809)
            showOnPhone(detail, style: style)
            waitForLoad(detail)
            snap("phone-summary-full-\(style == .dark ? "dark" : "light")")
        }
        seedMinimalTag()
        let minimal = makeDetail(tagId: 4243)
        showOnPhone(minimal)
        waitForLoad(minimal)
        snap("phone-summary-minimal-light")
    }

    func testPhoneSummaryWithListsAndScrolled() {
        guard !isPad else { return }
        seedLists(favorite: [1809], teachable: [1809],
                  lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        seedFullTag()
        let detail = makeDetail(tagId: 1809)
        showOnPhone(detail)
        waitForLoad(detail)
        snap("phone-summary-chips-light")
    }

    func testPhoneDetailsTracksVideos() {
        guard !isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            seedFullTag()
            let detail = makeDetail(tagId: 1809)
            showOnPhone(detail, style: style)
            waitForLoad(detail)
            select(page: 1, in: detail)
            snap("phone-details-full-\(suffix)")
            select(page: 2, in: detail)
            snap("phone-tracks-full-\(suffix)")
            select(page: 3, in: detail)
            snap("phone-videos-full-\(suffix)")
        }
        seedMinimalTag()
        let minimal = makeDetail(tagId: 4243)
        showOnPhone(minimal)
        waitForLoad(minimal)
        select(page: 1, in: minimal)
        snap("phone-details-minimal-light")
        select(page: 2, in: minimal)
        snap("phone-tracks-none-light")
        select(page: 3, in: minimal)
        snap("phone-videos-none-light")
    }

    func testPhoneLoadingAndFailure() {
        guard !isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            held = TMHeldTagLoad()
            let pending = makeDetail(tagId: TMHeldTagLoad.heldId)
            showOnPhone(pending, style: style)
            ScreenCatalog.settle(0.3)
            stillQuartet(in: pending)
            snap("phone-loading-\(style == .dark ? "dark" : "light")")
            held?.release()
            held = nil
            waitForLoad(pending)
            snap("phone-failed-\(style == .dark ? "dark" : "light")")
        }
    }

    func testPhonePresentations() {
        guard !isPad else { return }
        seedLists(favorite: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [])])
        seedFullTag()
        let detail = makeDetail(tagId: 1809)
        showOnPhone(detail)
        waitForLoad(detail)
        showActions(in: detail)
        snap("phone-actions-light", settle: 1.5)
        dismissPresentations(in: detail)
        showPicker(in: detail)
        snap("phone-picker-light", settle: 1.5)
        dismissPresentations(in: detail)
        showRating(in: detail)
        snap("phone-rating-light", settle: 1.5)
        dismissPresentations(in: detail)
    }

    // MARK: - iPad

    func testPadDetailBesideAList() {
        guard isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            seedLists(favorite: [1809])
            seedFullTag()
            seedMinimalTag()
            let source = TMCatalogSource([4243, 1809, 122])
            let detail = makeDetail(tagId: 1809, source: source)
            _ = showOnPad(detail, style: style)
            waitForLoad(detail)
            snap("pad-summary-\(suffix)")
            select(page: 1, in: detail)
            snap("pad-details-\(suffix)")
            select(page: 2, in: detail)
            snap("pad-tracks-\(suffix)")
            select(page: 3, in: detail)
            snap("pad-videos-\(suffix)")
            DPAppDelegate.removeSharedBackground()
        }
    }

    /// The secondary column before any tag is chosen.
    func makePlaceholder() -> UIViewController {
        let type = NSClassFromString("TMTagPlaceholderController") as! UIViewController.Type
        return type.init()
    }

    func testPadPlaceholder() {
        guard isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            _ = showOnPad(makePlaceholder(), style: style)
            snap("pad-placeholder-\(style == .dark ? "dark" : "light")")
            DPAppDelegate.removeSharedBackground()
        }
    }

    func testPadLoadingAndPicker() {
        guard isPad else { return }
        held = TMHeldTagLoad()
        let pending = makeDetail(tagId: TMHeldTagLoad.heldId, source: TMCatalogSource([TMHeldTagLoad.heldId].map(Int.init)))
        _ = showOnPad(pending)
        ScreenCatalog.settle(0.3)
        stillQuartet(in: pending)
        snap("pad-loading-light")
        held?.release()
        held = nil
        waitForLoad(pending)
        DPAppDelegate.removeSharedBackground()

        seedFullTag()
        let detail = makeDetail(tagId: 1809, source: TMCatalogSource([1809]))
        _ = showOnPad(detail)
        waitForLoad(detail)
        showPicker(in: detail)
        snap("pad-picker-light", settle: 1.5)
        dismissPresentations(in: detail)
    }
}
