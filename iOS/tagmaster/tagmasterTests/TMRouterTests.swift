//
//  TMRouterTests.swift
//  tagmasterTests
//
//  The shell's routing: deep links, where a tag opens, list shortcuts, screens
//  removing themselves, and the reader's full-screen toggle.
//

import SwiftUI
import XCTest
@testable import tagmaster

@MainActor
final class TMRouterTests: TMBehaviorTestCase {

    // MARK: - Deep links

    func testDeepLinksAcceptTheTwoTagForms() {
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster://tag/1809")!), 1809)
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "TagMaster://tag/42")!), 42, "The scheme is case-insensitive")
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster:///tag/7")!), 7)
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster://open/tag/12")!), 12)
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster://tag/2147483647")!), Int32.max)
    }

    func testDeepLinksRejectEverythingElse() {
        let rejected = [
            "https://tag/1809",                 // another scheme
            "tagmaster://tag/",                 // no id
            "tagmaster://tag/0",                // not a tag
            "tagmaster://tag/-3",               // a sign
            "tagmaster://tag/12a",              // not all digits
            "tagmaster://tag/%2012",            // an encoded space
            "tagmaster://tag/2147483648",       // beyond Int32
            "tagmaster://tag/12/",              // a trailing slash
            "tagmaster://tag/1/2",              // too deep
            "tagmaster://other/tag/1",          // another host
            "tagmaster:///song/1",              // another path
            "tagmaster://user@tag/1",           // a user
            "tagmaster://user:pw@tag/1",        // a password
            "tagmaster://tag:80/1",             // a port
        ]
        for link in rejected {
            XCTAssertNil(TMDeepLink.tagId(in: URL(string: link)!), link)
        }
    }

    /// The grammar the Objective-C delegate's openURL: enforced, case for case.
    func testSupportedRoutesAndIntegerBoundaries() {
        for prefix in ["tagmaster://open/tag/", "tagmaster://tag/", "tagmaster:///tag/"] {
            for (identifier, value) in [("1", 1), ("12", 12), ("00012", 12), ("2147483647", Int(Int32.max))] {
                XCTAssertEqual(TMDeepLink.tagId(in: URL(string: prefix + identifier)!).map(Int.init), value, prefix + identifier)
            }
        }
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "TAGMASTER://tag/1")!), 1)
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster://open/tag/12?source=share#track")!), 12)
        XCTAssertEqual(TMDeepLink.tagId(in: URL(string: "tagmaster://tag/%31")!), 1)
    }

    func testMalformedIDsNeverRoute() {
        for prefix in ["tagmaster://open/tag/", "tagmaster://tag/", "tagmaster:///tag/"] {
            for identifier in ["0", "000", "-1", "+1", "12junk", "1.0", "2147483648", "4294967297",
                               "99999999999999999999999999999999", "%EF%BC%91%EF%BC%92", "%D9%A1%D9%A2",
                               "%2012", "12%20", "12%00", "1%2F2", ""] {
                let url = URL(string: prefix + identifier)!
                XCTAssertNil(TMDeepLink.tagId(in: url), url.absoluteString)
            }
        }
    }

    func testUnrelatedSchemesAndPathsNeverRoute() {
        for link in ["https://open/tag/12", "other://tag/12", "/tag/12", "tagmaster://other/tag/12",
                     "tagmaster://open/extra/tag/12", "tagmaster://open/tag/12/extra", "tagmaster://tag/tag/12",
                     "tagmaster://open/tag//12", "tagmaster://open/tag/12/", "tagmaster://tag/12/",
                     "tagmaster://user@open/tag/12", "tagmaster://open:80/tag/12", "tagmaster://open/not-tag/12"] {
            XCTAssertNil(TMDeepLink.tagId(in: URL(string: link)!), link)
        }
    }

    func testOpeningADeepLinkShowsTheTag() {
        let router = TMRouter()
        XCTAssertTrue(router.open(URL(string: "tagmaster://tag/1809")!))
        XCTAssertEqual(router.path.last?.tagModel?.tagId, 1809)
        XCTAssertFalse(router.open(URL(string: "tagmaster://tag/abc")!))
        XCTAssertEqual(router.path.count, 1)
    }

    // MARK: - Where things open

    func testBesideAListATagOpensInTheDetailColumnAndIsReused() {
        let router = TMRouter()
        router.setExpanded(true)
        XCTAssertEqual(router.preferredCompactColumn, .sidebar, "Nothing chosen: collapsing keeps the list")
        router.showTag(1809, source: nil)
        router.showTag(42, source: nil)
        XCTAssertTrue(router.path.isEmpty)
        XCTAssertEqual(router.detail.tagId, 42)
        XCTAssertEqual(router.currentSplitTagId, 42)
        XCTAssertEqual(router.preferredCompactColumn, .detail, "A chosen tag stays on top when the split collapses")
        router.setExpanded(false)
        XCTAssertNil(router.currentSplitTagId)
        XCTAssertFalse(router.detail.expanded)
    }

    func testSelectionChangesAreAnnounced() {
        let router = TMRouter()
        let announced = expectation(forNotification: .TMTagSelectionDidChange, object: router)
        router.setExpanded(true)
        wait(for: [announced], timeout: 1)
    }

    func testFavoritesReturnsHomeAndOtherListsPush() {
        let router = TMRouter()
        router.show(.browse)
        router.showList(key: TMTagLists.teachableKey)
        XCTAssertEqual(router.path.map(\.destination), [.browse, .teachable])
        router.showList(key: "afterglow")
        XCTAssertEqual(router.path.last?.destination, .list("afterglow"))
        router.showList(key: TMTagLists.favoriteKey)
        XCTAssertTrue(router.path.isEmpty, "Favorites is Home's own section")
    }

    func testAScreenRemovesOnlyItself() throws {
        let router = TMRouter()
        router.show(.list("a"))
        router.showTag(1809, source: nil)
        let list = try XCTUnwrap(router.path.first?.screen)
        guard case .tagList(let model) = list else { return XCTFail("a list screen") }
        model.navigator?.removeScreen()
        XCTAssertEqual(router.path.count, 1, "The tag pushed above the deleted list stays")
        XCTAssertNotNil(router.path.first?.tagModel)
    }

    func testEachRouteGetsItsOwnModelAndListingSource() throws {
        let router = TMRouter()
        router.show(.results(TMTagQuery(text: "coney")))
        guard case .results(let model)? = router.path.last?.screen else { return XCTFail("a results screen") }
        XCTAssertEqual(model.query.text, "coney")
        let navigator = try XCTUnwrap(model.navigator as? TMRouteNavigator)
        XCTAssertTrue(navigator.source.listing === model, "Tags opened here step through these results")
        XCTAssertTrue(model.owner === navigator.source, "…and its changes are announced as that source's")
    }

    // MARK: - Sheet music

    func testTheReaderStacksOverTheTagThatOpenedItAndCanTakeTheWholeScreen() {
        let router = TMRouter()
        router.setExpanded(true)
        router.showTag(1809, source: nil)
        let document = TMSheetMusicDocument(fileURL: URL(fileURLWithPath: "/tmp/sheet.pdf"), title: "Lost", writtenKey: nil)
        router.showSheetMusic(document, summary: router.detail.summary, from: router.detail)
        XCTAssertEqual(router.detailPath.count, 1, "Beside a list the reader opens in the detail column")
        XCTAssertTrue(router.path.isEmpty)
        router.toggleFullScreen()
        XCTAssertTrue(router.isFullScreen)
        router.toggleFullScreen()
        XCTAssertFalse(router.isFullScreen)
        router.showTag(42, source: nil)
        XCTAssertTrue(router.detailPath.isEmpty, "A new tag closes the reader")
    }
}
