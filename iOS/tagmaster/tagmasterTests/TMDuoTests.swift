//
//  TMDuoTests.swift
//  tagmasterTests
//
//  An iPhone Duo is a phone when it is closed and a tablet-sized window when it
//  is open: the list beside the tag appears and goes as the device opens and
//  closes, and the tag the user was on goes with them, page and all.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class TMDuoTests: TMBehaviorTestCase {
    /// The Duo's displays in points: closed (outer) and open (inner, held like a book).
    static let outer = CGSize(width: 466, height: 678)
    static let inner = CGSize(width: 951, height: 669)

    // MARK: - Which shell

    func testOnlyADuoHeldOpenWideShowsTheBook() {
        func layout(_ idiom: UIUserInterfaceIdiom, _ horizontal: UserInterfaceSizeClass,
                    _ vertical: UserInterfaceSizeClass, _ width: CGFloat) -> TMShellLayout {
            TMShellLayout(idiom: idiom, horizontal: horizontal, vertical: vertical, width: width)
        }
        XCTAssertEqual(layout(.phone, .regular, .regular, 867), .book,
                       "An open Duo held wide shows the list beside the tag (its side bar takes 84 of the 951 points)")
        XCTAssertEqual(layout(.phone, .regular, .regular, 669), .stack,
                       "Held upright it is 669 points wide: one stack, as Android's two panes need 720")
        XCTAssertEqual(layout(.phone, .compact, .regular, 382), .stack, "A closed Duo is a phone")
        XCTAssertEqual(layout(.phone, .compact, .compact, 594), .stack, "A closed Duo turned sideways too")
        XCTAssertEqual(layout(.phone, .regular, .compact, 956), .stack,
                       "A large phone in landscape keeps one stack, as it always has")
        XCTAssertEqual(layout(.pad, .compact, .regular, 375), .split,
                       "An iPad keeps its split, which collapses by itself when narrow")
        XCTAssertEqual(layout(.pad, .regular, .regular, 1194), .split)
    }

    func testPopoversFloatWhereTheListSitsBesideTheTag() {
        func traits(_ idiom: UIUserInterfaceIdiom, _ horizontal: UIUserInterfaceSizeClass,
                    _ vertical: UIUserInterfaceSizeClass) -> UITraitCollection {
            UITraitCollection { traits in
                traits.userInterfaceIdiom = idiom
                traits.horizontalSizeClass = horizontal
                traits.verticalSizeClass = vertical
            }
        }
        XCTAssertTrue(traits(.phone, .regular, .regular).tmFloatsPopovers, "An open Duo anchors its menus")
        XCTAssertFalse(traits(.phone, .compact, .regular).tmFloatsPopovers)
        XCTAssertFalse(traits(.phone, .regular, .compact).tmFloatsPopovers, "A large phone in landscape keeps sheets")
        XCTAssertTrue(traits(.pad, .compact, .regular).tmFloatsPopovers, "An iPad's narrow column still floats them")
    }

    func testTheListFillsTheLeadingPageUpToTheCrease() {
        // Open like a book: the fold's 40-point band down the middle, flat or half shut.
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 951, fold: CGRect(x: 455.5, y: 0, width: 40, height: 669)),
                       475.5, "An even split, the divider down the crease")
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 951, fold: CGRect(x: 475.5, y: 0, width: 0, height: 669)), 475.5)
        // No fold, or one too near an edge to leave either page a usable column.
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 951, fold: nil), 342.36, accuracy: 0.01)
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 1194, fold: nil), 400)
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 951, fold: CGRect(x: 280, y: 0, width: 40, height: 669)),
                       342.36, accuracy: 0.01)
        // Half shut, each page keeps half the 40-point crease and iOS's 20-point margin
        // between its content and the divider.
        XCTAssertEqual(TMBookRoot.creaseClearance(band: CGRect(x: 455.5, y: 0, width: 40, height: 669), margin: 20), 40)
    }

    // MARK: - The router carries the tag across

    func testOpeningMovesTheTagOnTopBesideTheList() throws {
        let router = TMRouter()
        router.show(.teachable)
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        model.selectedPage = .tracks

        router.setExpanded(true)

        XCTAssertTrue(router.detail === model, "The same tag, with whatever it is playing, sits beside the list")
        XCTAssertEqual(router.path.map(\.destination), [.teachable], "The list it came from stays in the list column")
        XCTAssertTrue(router.hasDetail)
        XCTAssertTrue(model.expanded)
        XCTAssertEqual(model.selectedPage, .tracks)
        XCTAssertEqual(router.currentSplitTagId, 1809)
    }

    func testTheReaderMovesWithItsTag() throws {
        let router = TMRouter()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        let document = TMSheetMusicDocument(fileURL: URL(fileURLWithPath: "/tmp/sheet.pdf"), title: "Lost", writtenKey: nil)
        router.showSheetMusic(document, summary: model.summary, from: model)
        XCTAssertEqual(router.path.count, 2)

        router.setExpanded(true)

        XCTAssertTrue(router.path.isEmpty)
        XCTAssertTrue(router.detail === model)
        XCTAssertEqual(router.detailPath.count, 1, "The sheet music stays open over its tag")
    }

    func testATagUnderAPushedListStaysOnTheStack() throws {
        let router = TMRouter()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        router.showList(key: TMTagLists.teachableKey)

        router.setExpanded(true)

        XCTAssertEqual(router.path.count, 2, "The list on top is what was showing; nothing moves")
        XCTAssertTrue(router.path.first?.tagModel === model)
        XCTAssertFalse(router.hasDetail)
        XCTAssertFalse(router.detail === model)
    }

    func testClosingPutsTheTagBesideTheListOnTopOfTheStack() throws {
        let router = TMRouter()
        router.setExpanded(true)
        router.show(.teachable)
        router.showTag(1809, source: nil)
        let model = router.detail
        model.selectedPage = .details
        let document = TMSheetMusicDocument(fileURL: URL(fileURLWithPath: "/tmp/sheet.pdf"), title: "Lost", writtenKey: nil)
        router.showSheetMusic(document, summary: model.summary, from: model)

        router.useStack()

        XCTAssertFalse(router.expanded)
        XCTAssertFalse(model.expanded)
        XCTAssertEqual(router.path.count, 3, "Teachable, then the tag, then its reader")
        XCTAssertTrue(router.path[1].tagModel === model)
        if case .sheetMusic = router.path[2].kind {} else { XCTFail("The reader stays on top of its tag") }
        XCTAssertTrue(router.detailPath.isEmpty)
        XCTAssertFalse(router.hasDetail)
        XCTAssertFalse(router.detail === model, "The detail place is free for the next time the device opens")
        XCTAssertEqual(model.selectedPage, .details)
    }

    func testOpeningAndClosingKeepTheSameTagAndPage() throws {
        let router = TMRouter()
        router.useStack()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        model.selectedPage = .videos

        for _ in 0..<2 {
            router.setExpanded(true)
            XCTAssertTrue(router.detail === model)
            router.useStack()
            XCTAssertTrue(router.path.last?.tagModel === model)
        }
        XCTAssertEqual(router.path.count, 1)
        XCTAssertEqual(model.selectedPage, .videos)
    }

    func testOpeningReplacesATagLeftBesideTheList() throws {
        let router = TMRouter()
        router.setExpanded(true)
        router.showTag(42, source: nil)
        let earlier = router.detail
        // The split collapses (an iPad window narrowed), the user goes back to the
        // list and opens another tag on the list stack.
        router.setExpanded(false)
        router.preferredCompactColumn = .sidebar
        router.showTag(1809, source: nil)
        let later = try XCTUnwrap(router.path.last?.tagModel)

        router.setExpanded(true)

        XCTAssertTrue(router.detail === later, "The tag the user was just looking at comes beside the list")
        XCTAssertFalse(router.detail === earlier)
        XCTAssertEqual(router.currentSplitTagId, 1809)
    }

    /// SwiftUI may take the old page down before or after it puts the new one up;
    /// the part keeps playing either way, and leaving the page afterwards still stops it.
    func testAPartPlayingCarriesAcrossInEitherOrder() {
        for oldGoesFirst in [true, false] {
            let detail = TagDetailModel()
            let tracks = detail.tracks
            tracks.appeared(page: "old")
            detail.screenAppeared("old")

            detail.carryAcross()
            if oldGoesFirst {
                tracks.disappeared(page: "old"); detail.screenDisappeared("old")
                tracks.appeared(page: "new"); detail.screenAppeared("new")
            } else {
                tracks.appeared(page: "new"); detail.screenAppeared("new")
                tracks.disappeared(page: "old"); detail.screenDisappeared("old")
            }
            XCTAssertFalse(tracks.hasLeft, "Carrying the tag across does not stop its part")
            XCTAssertTrue(detail.screenVisible, "The new screen is up, whichever order they came in")
            XCTAssertFalse(tracks.carriedAcross)

            tracks.disappeared(page: "new"); detail.screenDisappeared("new")
            XCTAssertTrue(tracks.hasLeft, "Leaving the page afterwards stops the part, as it always has")
            XCTAssertFalse(detail.screenVisible)
        }
    }

    func testTheRouterMarksTheTagItCarries() throws {
        let router = TMRouter()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        router.setExpanded(true)
        XCTAssertTrue(model.tracks.carriedAcross)
        model.tracks.appeared()
        router.useStack()
        XCTAssertTrue(model.tracks.carriedAcross)
    }

    func testAPhoneThatNeverOpensIsUntouched() throws {
        let router = TMRouter()
        router.useStack()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        router.useStack()
        XCTAssertEqual(router.path.count, 1)
        XCTAssertTrue(router.path.last?.tagModel === model)
        XCTAssertFalse(router.hasDetail)
    }

    // MARK: - The shell, opened and closed

    /// The app's own root, in a window that changes shape and size classes as a Duo
    /// does when it opens and closes.
    func testOpeningAndClosingTheShellCarriesTheOpenTagAcross() throws {
        seedCachedTag(id: 1809)
        let router = TMRouter()
        let host = UIHostingController(rootView: TMRootView(router: router))
        host.traitOverrides.horizontalSizeClass = .compact
        host.traitOverrides.verticalSizeClass = .regular
        mount(host, size: TMDuoTests.outer)
        router.showTag(1809, source: nil)
        settle()
        let model = try XCTUnwrap(router.path.last?.tagModel)
        spinUntil("the tag loads") { model.tag != nil }
        model.selectedPage = .tracks

        open(host)
        spinUntil("the split takes the tag") { router.expanded && router.hasDetail }
        XCTAssertTrue(router.detail === model)
        XCTAssertTrue(router.path.isEmpty)
        XCTAssertTrue(model.expanded, "Beside the list the bar grows the steppers and toggles")
        XCTAssertEqual(model.selectedPage, .tracks)

        close(host)
        spinUntil("the stack takes the tag") { router.path.last?.tagModel === model }
        XCTAssertFalse(router.expanded)
        XCTAssertFalse(router.hasDetail)
        XCTAssertFalse(model.expanded)
        XCTAssertEqual(model.selectedPage, .tracks)
    }

    private func open(_ host: UIViewController) {
        window.frame = CGRect(origin: .zero, size: TMDuoTests.inner)
        host.traitOverrides.horizontalSizeClass = .regular
        host.traitOverrides.verticalSizeClass = .regular
        settle()
    }

    private func close(_ host: UIViewController) {
        window.frame = CGRect(origin: .zero, size: TMDuoTests.outer)
        host.traitOverrides.horizontalSizeClass = .compact
        host.traitOverrides.verticalSizeClass = .regular
        settle()
    }
}
