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
import SwiftUI
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

/// A tag the catalog opens, and the detail model the shell shows it with.
@MainActor
final class TMCatalogDetail {
    let tagId: Int32
    let source: TMTagListSource?
    var model: TagDetailModel?

    init(tagId: Int32, source: TMTagListSource?) {
        self.tagId = tagId
        self.source = source
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

    // MARK: - The app's shell

    func appWindow(_ style: UIUserInterfaceStyle) -> UIWindow {
        if let old = self.window {
            old.rootViewController?.dismiss(animated: false)
            old.isHidden = true
            old.rootViewController = nil
        }
        let window = ScreenCatalog.makeWindow(style: style)
        self.window = window
        return window
    }

    /// iPhone: the app's stack with the tag pushed over Home.
    func showOnPhone(_ detail: TMCatalogDetail, style: UIUserInterfaceStyle = .light) {
        let window = appWindow(style)
        let router = TMRouter()
        window.rootViewController = UIHostingController(rootView: TMStackRoot(router: router))
        window.makeKeyAndVisible()
        ScreenCatalog.settle(0.3)
        router.showTag(detail.tagId, source: detail.source)
        detail.model = router.path.last?.tagModel
        ScreenCatalog.settle(0.5)
    }

    /// iPad: the app's split, Home in the list column and the tag beside it
    /// (or, with no tag, the placeholder).
    func showOnPad(_ detail: TMCatalogDetail?, style: UIUserInterfaceStyle = .light) {
        let window = appWindow(style)
        let router = TMRouter()
        window.rootViewController = UIHostingController(rootView: TMSplitRoot(router: router))
        window.makeKeyAndVisible()
        ScreenCatalog.settle(0.5)
        if let detail {
            router.showTag(detail.tagId, source: detail.source)
            detail.model = router.detail
        }
        ScreenCatalog.settle(0.5)
    }

    var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }

    func snap(_ name: String, settle seconds: TimeInterval = 0.8) {
        ScreenCatalog.capture(name, window: window, settle: seconds)
    }

    // MARK: - Driving the detail

    func makeDetail(tagId: Int32, source: TMTagListSource? = nil) -> TMCatalogDetail {
        TMCatalogDetail(tagId: tagId, source: source)
    }

    func waitForLoad(_ detail: TMCatalogDetail) {
        spinUntil("the detail settles", timeout: 5) { detail.model.map { !$0.fetchPending } ?? false }
    }

    func select(page: Int, in detail: TMCatalogDetail) {
        detail.model?.selectedPage = TagDetailModel.Page(rawValue: page)!
        ScreenCatalog.settle(0.6)
    }

    /// Stops the quartet so it is captured at rest.
    func stillQuartet(in detail: TMCatalogDetail) {
        detail.model?.applicationActive = false
    }

    func showActions(in detail: TMCatalogDetail) {
        detail.model?.showActions()
    }

    func showPicker(in detail: TMCatalogDetail) {
        guard let model = detail.model else { return }
        model.showListPicker(from: model.expanded ? .toolbar : .actions)
    }

    func showRating(in detail: TMCatalogDetail) {
        detail.model?.summary.showRating()
    }

    func dismissPresentations(in detail: TMCatalogDetail) {
        detail.model?.actionsPresented = false
        detail.model?.pickerSource = nil
        detail.model?.summary.ratingDialogPresented = false
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
        snap("phone-picker-light", settle: 3)
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
            showOnPad(detail, style: style)
            waitForLoad(detail)
            snap("pad-summary-\(suffix)")
            select(page: 1, in: detail)
            snap("pad-details-\(suffix)")
            select(page: 2, in: detail)
            snap("pad-tracks-\(suffix)")
            select(page: 3, in: detail)
            snap("pad-videos-\(suffix)")
        }
    }

    func testPadPlaceholder() {
        guard isPad else { return }
        for style in [UIUserInterfaceStyle.light, .dark] {
            showOnPad(nil, style: style)
            snap("pad-placeholder-\(style == .dark ? "dark" : "light")")
        }
    }

    func testPadLoadingAndPicker() {
        guard isPad else { return }
        held = TMHeldTagLoad()
        let pending = makeDetail(tagId: TMHeldTagLoad.heldId, source: TMCatalogSource([TMHeldTagLoad.heldId].map(Int.init)))
        showOnPad(pending)
        ScreenCatalog.settle(0.3)
        stillQuartet(in: pending)
        snap("pad-loading-light")
        held?.release()
        held = nil
        waitForLoad(pending)

        seedFullTag()
        let detail = makeDetail(tagId: 1809, source: TMCatalogSource([1809]))
        showOnPad(detail)
        waitForLoad(detail)
        showPicker(in: detail)
        snap("pad-picker-light", settle: 3)
        dismissPresentations(in: detail)
    }
}
