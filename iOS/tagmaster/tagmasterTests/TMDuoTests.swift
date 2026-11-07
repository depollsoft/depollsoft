//
//  TMDuoTests.swift
//  tagmasterTests
//
//  An iPhone Duo is a phone when it is closed and a tablet-sized window when it
//  is open: the list beside the tag appears and goes as the device opens and
//  closes, and the tag the user was on goes with them, page and all.
//

import AVFoundation
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

    func testABookOnlyOpensWhereItsDividerCanRunDownTheFold() {
        func layout(width: CGFloat, foldX: CGFloat?) -> TMShellLayout {
            TMShellLayout(idiom: .phone, horizontal: .regular, vertical: .regular, width: width,
                          fold: foldX.map { CGRect(x: $0, y: 0, width: 40, height: 669) })
        }
        XCTAssertEqual(layout(width: 867, foldX: 455.5), .book, "The whole inner display: the crease in the middle")
        XCTAssertEqual(layout(width: 867, foldX: nil), .book, "No fold reported: the iPad's 36% list")
        // A window beside another app: the fold 455.5 points in leaves the tag's page
        // under 320, and a divider elsewhere would put the crease through the tag.
        XCTAssertEqual(layout(width: 760, foldX: 455.5), .stack)
        XCTAssertEqual(layout(width: 760, foldX: 4.5), .stack, "Nor with the crease near the window's leading edge")
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
        // No fold: the iPad's list. (A fold too near an edge for two pages keeps the stack.)
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 951, fold: nil), 342.36, accuracy: 0.01)
        XCTAssertEqual(TMBookRoot.listWidth(windowWidth: 1194, fold: nil), 400)
        // Half shut, each page keeps half the fold's 40-point region between its content
        // and the divider: the region's frame already holds iOS's 20-point margins.
        XCTAssertEqual(TMBookRoot.creaseClearance(band: CGRect(x: 455.5, y: 0, width: 40, height: 669)), 20)
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

    /// The old shell's Tracks page can go before the new shell appears: the root has
    /// the router mark and move the tag before it swaps shells, so the part keeps
    /// playing however SwiftUI orders the two, and the new shell is drawn with the tag
    /// already in place.
    func testThePartKeepsPlayingWhenTheOldPageGoesBeforeTheNewShellComes() throws {
        let router = TMRouter()
        router.showTag(1809, source: nil)
        let model = try XCTUnwrap(router.path.last?.tagModel)
        let tracks = model.tracks
        tracks.appeared(page: "stack")

        router.shellWillChange(to: .book)
        XCTAssertTrue(router.detail === model, "Opening: the tag is beside the list before the book is drawn")
        XCTAssertTrue(router.path.isEmpty)
        tracks.disappeared(page: "stack")
        XCTAssertFalse(tracks.hasLeft, "The stack's page went first")
        tracks.appeared(page: "book")
        XCTAssertFalse(tracks.carriedAcross, "The book's page coming clears the mark")

        router.shellWillChange(to: .stack)
        XCTAssertTrue(router.path.last?.tagModel === model, "Closing: the tag is on the stack before it is drawn")
        XCTAssertFalse(router.hasDetail)
        tracks.disappeared(page: "book")
        XCTAssertFalse(tracks.hasLeft, "The book's page went first")
        tracks.appeared(page: "stack again")
        XCTAssertFalse(tracks.hasLeft)

        tracks.disappeared(page: "stack again")
        XCTAssertTrue(tracks.hasLeft, "Leaving the page afterwards still stops the part")
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
        settle()
        // A part playing on the stack's Tracks page.
        let track = try XCTUnwrap(model.tracks.tracks.first)
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 2))
        // Long enough to outlast both swaps: a part that ended would look stopped.
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 44_100 * 30))
        buffer.frameLength = 44_100 * 30
        model.tracks.present(track, buffer: buffer)
        settle()
        XCTAssertTrue(model.tracks.playerVisible)
        // Whether the engine starts depends on the machine's audio; whatever it did,
        // the swap must leave it so: the same part loaded, playing or not as it was.
        let player = model.tracks.player
        let playing = player.player.isPlaying
        func assertThePartIsAsItWas(_ swap: String, line: UInt = #line) {
            XCTAssertTrue(model.tracks.playerVisible, "\(swap): the part is still in the player", line: line)
            XCTAssertTrue(player.track === track && player.isLoaded, "\(swap): the same part, loaded", line: line)
            XCTAssertEqual(player.player.isPlaying, playing, "\(swap): neither paused nor stopped", line: line)
        }

        open(host)
        spinUntil("the split takes the tag") { router.expanded && router.hasDetail }
        XCTAssertTrue(router.detail === model)
        XCTAssertTrue(router.path.isEmpty)
        XCTAssertTrue(model.expanded, "Beside the list the bar grows the steppers and toggles")
        XCTAssertEqual(model.selectedPage, .tracks)
        XCTAssertFalse(model.tracks.hasLeft, "The Tracks page going with the stack left the part alone")
        assertThePartIsAsItWas("Opening")

        close(host)
        spinUntil("the stack takes the tag") { router.path.last?.tagModel === model }
        XCTAssertFalse(router.expanded)
        XCTAssertFalse(router.hasDetail)
        XCTAssertFalse(model.expanded)
        XCTAssertEqual(model.selectedPage, .tracks)
        XCTAssertFalse(model.tracks.hasLeft, "The Tracks page going with the book left the part alone")
        assertThePartIsAsItWas("Closing")
    }

    /// A video playing full screen holds the shell: turning the open Duo from upright
    /// to wide under it would take the Videos page, and the video, down. The book
    /// comes once the video closes.
    func testAVideoPlayingHoldsTheShellUntilItCloses() throws {
        seedCachedTag(id: 1809)
        let router = TMRouter()
        let host = UIHostingController(rootView: TMRootView(router: router))
        host.traitOverrides.horizontalSizeClass = .regular
        host.traitOverrides.verticalSizeClass = .regular
        mount(host, size: CGSize(width: 669, height: 951))
        router.showTag(1809, source: nil)
        settle()
        let model = try XCTUnwrap(router.path.last?.tagModel)
        spinUntil("the tag loads") { model.tag != nil }
        model.selectedPage = .videos
        settle()
        XCTAssertFalse(router.expanded, "Upright, the open Duo is the stack")

        // Any https page: the cover is what matters, so nothing need reach a real site.
        model.watching = URL(string: "https://localhost/video")
        spinUntil("the video comes up") { TMPresentingAnchor.top(host) !== host }
        XCTAssertTrue(router.holdsShell)
        open(host)
        XCTAssertFalse(router.expanded, "Turned wide under the video, the shell holds")
        XCTAssertTrue(router.path.last?.tagModel === model)
        XCTAssertNotNil(model.watching, "and the video is still up")
        XCTAssertTrue(TMPresentingAnchor.top(host) !== host)

        TMPresentingAnchor.top(host).dismiss(animated: false)
        model.watching = nil
        spinUntil("the book comes once it closes") { router.expanded && router.detail === model }
        XCTAssertEqual(model.selectedPage, .videos)
    }

    /// Sign-in, Settings' sheet, holds the shell too: the sheet goes with the page
    /// that presents it.
    func testSignInHoldsTheShellUntilItCloses() throws {
        // Signed out, without Firebase: the test host never configures it.
        let router = TMRouter(account: TMAccount(isSignedIn: { false }, signOut: {}),
                              build: TMBuildInfo(number: "", pullRequest: "?"))
        router.show(.settings)
        guard case .settings(let settings)? = router.path.last?.screen else { return XCTFail("No Settings screen") }
        XCTAssertFalse(router.holdsShell)
        settings.showingSignIn = true
        XCTAssertTrue(router.holdsShell, "Signing in, the shell holds")
        settings.showingSignIn = false
        XCTAssertFalse(router.holdsShell, "and moves once sign-in closes")
    }

    /// The list picker up when the Duo opens or closes: the control that opened it goes
    /// with its shell, and the picker still closes on Done or a swipe, without coming
    /// back when the device turns again.
    func testAListPickerUpAcrossTheHandoffStillCloses() throws {
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
        func picker() -> TMListPickerPresenter.Host? { TMPresentingAnchor.top(host) as? TMListPickerPresenter.Host }

        // From the tag actions, which only the stack's bar has.
        model.showListPicker(from: .actions)
        spinUntil("the picker comes up") { picker() != nil }
        open(host)
        spinUntil("the book takes the tag") { router.expanded && router.hasDetail }
        let first = try XCTUnwrap(picker(), "The picker stays up across the handoff")
        XCTAssertNotNil(first.presentationController?.delegate, "Something still hears it go")
        try XCTUnwrap(first.rootView.onDone)()
        spinUntil("Done closes it") { picker() == nil }
        XCTAssertNil(model.pickerSource)

        // From the chip, which both shells have; swiped away after closing the Duo.
        model.showListPicker(from: .chip)
        spinUntil("the picker comes up from the chip") { picker() != nil }
        close(host)
        spinUntil("the stack takes the tag") { router.path.last?.tagModel === model }
        let second = try XCTUnwrap(picker())
        if let popover = second.popoverPresentationController {
            XCTAssertTrue(popover.sourceView is TMPresentingAnchor && popover.sourceView?.window != nil,
                          "The adopted picker hangs from the chip on screen")
        }
        let presentation = try XCTUnwrap(second.presentationController)
        second.dismiss(animated: false)
        presentation.delegate?.presentationControllerDidDismiss?(presentation)
        spinUntil("the swipe clears the request") { model.pickerSource == nil && picker() == nil }
        open(host)
        spinUntil("the book takes the tag again") { router.expanded && router.hasDetail }
        XCTAssertNil(picker(), "A picker swiped away does not come back")

        // The tag actions sheet left up while the Duo opens: its Add to List opens the
        // picker from the book's own button, since the phone bar's tag button has gone.
        close(host)
        spinUntil("the stack takes the tag once more") { router.path.last?.tagModel === model }
        model.showActions()
        spinUntil("the actions sheet comes up") { TMPresentingAnchor.top(host) is UIAlertController }
        open(host)
        spinUntil("the book takes the tag with the sheet up") { router.expanded && router.hasDetail }
        let add = try XCTUnwrap(model.tagActions.first { $0.title.hasPrefix("Add to List") })
        // Choosing an action closes the sheet first, as UIKit does.
        TMPresentingAnchor.top(host).dismiss(animated: false)
        model.actionsPresented = false
        add.handler()
        XCTAssertEqual(model.pickerSource, .toolbar, "The book's Add to list button presents it")
        model.pickerSource = nil
    }

    /// The tag actions opened on the closed Duo, a centred card with no popover source,
    /// and left up while it opens: the book's control adopts it, and tapped away there
    /// it stays closed.
    func testAnActionSheetFromTheClosedDuoClosesForGoodInTheBook() throws {
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
        model.showActions()
        spinUntil("the actions sheet comes up") { TMPresentingAnchor.top(host) is UIAlertController }
        let sheet = try XCTUnwrap(TMPresentingAnchor.top(host) as? UIAlertController)
        open(host)
        spinUntil("the book takes the tag with the sheet up") { router.expanded && router.hasDetail }
        // A tap outside: UIKit dismisses it and tells its delegate; no action runs.
        let presentation = try XCTUnwrap(sheet.popoverPresentationController)
        sheet.dismiss(animated: false)
        presentation.delegate?.presentationControllerDidDismiss?(presentation)
        spinUntil("the tap clears the request") { !model.actionsPresented }
        settle()
        XCTAssertFalse(TMPresentingAnchor.top(host) is UIAlertController, "and the sheet doesn't come back")
    }

    // MARK: - Tabletop: the music stand

    func testAPageOnlyStandsOnAFoldRightAcrossItWithRoomOnBothSides() {
        let page = CGSize(width: 669, height: 736)
        XCTAssertTrue(TMStandFold.stands(CGRect(x: 0, y: 329.5, width: 669, height: 40), in: page))
        XCTAssertFalse(TMStandFold.stands(CGRect(x: 314.5, y: 0, width: 40, height: 736), in: page),
                       "A fold down the page is a book, not a stand")
        XCTAssertFalse(TMStandFold.stands(CGRect(x: 0, y: 100, width: 669, height: 40), in: page), "Too little page above")
        XCTAssertFalse(TMStandFold.stands(CGRect(x: 0, y: 600, width: 669, height: 40), in: page), "Too little deck below")
    }

    /// Upright and half shut on a table, the tag's words read on the standing half and
    /// what the singer plays lies on the half under the fingers.
    func testInTabletopTheWordsStandAndTheControlsLieBelowTheFold() throws {
        seedCachedTag(id: 1809)
        let upright = CGSize(width: 669, height: 951)
        let fold = CGRect(x: 0, y: 455.5, width: upright.width, height: 40)
        // The band reaches the page in the page's own coordinates, as the real region
        // does; the page's top in the window, measured once it is up, puts it on the crease.
        var pageTop: CGFloat?
        let crease: (CGSize) -> CGRect? = { _ in pageTop.map { fold.offsetBy(dx: 0, dy: -$0) } }
        TMStandFold.simulated = crease
        defer { TMStandFold.simulated = nil }
        let router = TMRouter()
        let host = UIHostingController(rootView: TMRootView(router: router))
        host.traitOverrides.horizontalSizeClass = .regular
        host.traitOverrides.verticalSizeClass = .regular
        mount(host, size: upright)
        router.showTag(1809, source: nil)
        settle()
        let model = try XCTUnwrap(router.path.last?.tagModel)
        spinUntil("the tag loads") { model.tag != nil }
        settle()
        let driver = UIDriver(window)

        // Flat, the page fills the stack: its top is where the stand will start. (The
        // key, which sits high enough on the flat page to be on a small phone's screen.)
        let top = try XCTUnwrap(scroller(holding: try XCTUnwrap(driver.element(id: "summary.key")))).minY
        pageTop = top
        window.frame = CGRect(origin: .zero, size: CGSize(width: upright.width, height: upright.height - 1))
        settle()
        window.frame = CGRect(origin: .zero, size: upright)
        settle()
        // The page stands with an animation: measure once it has.
        spinUntil("the page stands") {
            (driver.element(id: "summary.lyrics").flatMap(scroller(holding:))?.maxY ?? .infinity) <= fold.minY + 1
        }

        // Summary stands its lyrics; the key, Sheet Music and the facts lie below.
        let lyrics = try XCTUnwrap(driver.element(id: "summary.lyrics"), "Summary stands the lyrics")
        let key = try XCTUnwrap(driver.element(id: "summary.key"), "Summary's deck holds the key note")
        let music = try XCTUnwrap(driver.element(label: "Sheet Music"))
        let standing = try XCTUnwrap(scroller(holding: lyrics))
        let deck = try XCTUnwrap(scroller(holding: key))
        XCTAssertEqual(standing.minY, top, accuracy: 1, "The page still starts where it did")
        XCTAssertEqual(standing.maxY, fold.minY, accuracy: 1, "The standing half ends at the fold's band")
        XCTAssertEqual(deck.minY, fold.maxY, accuracy: 1, "and the deck starts below it")
        XCTAssertLessThanOrEqual(lyrics.accessibilityFrame.maxY, standing.maxY, "The lyrics are whole above the fold")
        XCTAssertEqual(key.accessibilityFrame.minY - deck.minY, TMStandDeck.top, accuracy: 1,
                       "First on the deck, not floated down it")
        // Side by side across the page's width, at one height: the page's own buttons.
        XCTAssertEqual(key.accessibilityFrame.minX, 16, accuracy: 1)
        XCTAssertEqual(music.accessibilityFrame.maxX, upright.width - 16, accuracy: 1)
        XCTAssertLessThan(key.accessibilityFrame.maxX, music.accessibilityFrame.minX)
        XCTAssertEqual(key.accessibilityFrame.minY, music.accessibilityFrame.minY, accuracy: 1)
        XCTAssertEqual(key.accessibilityFrame.height, music.accessibilityFrame.height, accuracy: 1)
        XCTAssertEqual(key.accessibilityFrame.height, TMStandDeck.controlHeight, accuracy: 1)
        let tagID = try XCTUnwrap(driver.element(label: "Tag ID"))
        XCTAssertGreaterThan(tagID.accessibilityFrame.minY, key.accessibilityFrame.maxY, "The facts follow on the deck")

        // Details stays whole, with no deck.
        model.selectedPage = .details
        settle()
        XCTAssertFalse(driver.exists(id: "summary.key"), "Details has no deck")

        model.selectedPage = .tracks
        settle()
        let lead = try XCTUnwrap(driver.element(label: "Lead"), "Tracks' deck holds the parts")
        let standingLyrics = try XCTUnwrap(driver.element(id: "summary.lyrics"), "Tracks stands the lyrics too")
        let parts = try XCTUnwrap(scroller(holding: lead))
        XCTAssertLessThanOrEqual(standingLyrics.accessibilityFrame.maxY, fold.minY, "The lyrics stand above the fold")
        XCTAssertGreaterThanOrEqual(parts.minY, fold.maxY, "and the parts lie below it")
        XCTAssertEqual(lead.accessibilityFrame.maxX, upright.width - 16, accuracy: 1, "The parts run the page's width")
        XCTAssertNil(driver.element(label: "Tap a part below to play it."),
                     "No hint on the standing half: it faces the singers, and the parts carry play icons")

        // The player comes in on the deck, under the parts, and the part it holds is marked.
        let track = try XCTUnwrap(model.tracks.tracks.first { $0.title == "Lead" })
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 2))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 44_100))
        buffer.frameLength = 44_100
        model.tracks.present(track, buffer: buffer)
        settle()
        XCTAssertTrue(model.tracks.playerVisible)
        let player = try XCTUnwrap(driver.element(id: "tagmaster.trackPlayer"))
        let shortened = try XCTUnwrap(scroller(holding: try XCTUnwrap(driver.element(label: "Lead"))))
        XCTAssertGreaterThanOrEqual(player.accessibilityFrame.minY, shortened.maxY, "The player lies under the parts")
        XCTAssertEqual(shortened.minY, parts.minY, accuracy: 1, "on the deck")
        let playing = try XCTUnwrap(driver.element(label: "Lead"))
        XCTAssertTrue(["Playing", "Paused"].contains(playing.accessibilityValue ?? ""), "The part in the player is marked")
        XCTAssertGreaterThanOrEqual(playing.accessibilityFrame.minY, shortened.minY - 1, "and kept in view")
        XCTAssertLessThanOrEqual(playing.accessibilityFrame.maxY, shortened.maxY + 1)

        // A part playing on the stand keeps playing as the device is laid flat: the
        // page stays where it was, so it never goes and stops the player.
        TMStandFold.simulated = nil
        window.frame = CGRect(origin: .zero, size: CGSize(width: upright.width, height: upright.height - 1))
        settle()
        XCTAssertTrue(driver.exists(label: "Lead"))
        XCTAssertEqual(driver.element(label: "Lead")?.accessibilityValue ?? "", "", "Laid flat, the page is whole again")
        XCTAssertTrue(model.tracks.playerVisible, "and the part is still in the player")
    }

    /// Folded mid-song, the deck comes up with a part already playing: it opens with
    /// that part in view, not at the top of the list with the player covering it.
    func testADeckComingUpMidSongShowsThePartPlaying() throws {
        seedCachedTag(id: 1809)
        let router = TMRouter()
        mount(UIHostingController(rootView: TMRootView(router: router)), size: CGSize(width: 669, height: 951))
        router.showTag(1809, source: nil)
        settle()
        let model = try XCTUnwrap(router.path.last?.tagModel)
        spinUntil("the tag loads") { model.tag != nil }
        model.selectedPage = .tracks
        settle()
        // The last part playing on the flat page; then only the deck, as a fold brings it.
        let last = try XCTUnwrap(model.tracks.tracks.last)
        let format = try XCTUnwrap(AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 2))
        let buffer = try XCTUnwrap(AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 44_100))
        buffer.frameLength = 44_100
        model.tracks.present(last, buffer: buffer)
        settle()
        model.tracks.carriedAcross = true
        window.frame = CGRect(origin: .zero, size: CGSize(width: 669, height: 372))
        window.rootViewController = UIHostingController(rootView: TMStandPartsDeck(model: model.tracks))
        settle()
        XCTAssertTrue(model.tracks.playerVisible)
        let driver = UIDriver(window)
        // The deck's one scroll view is the parts list.
        let list = try XCTUnwrap(descendants(of: window) { $0 is UIScrollView }.first)
        spinUntil("\(last.title ?? "") is in view on the deck") {
            let parts = list.convert(list.bounds, to: nil)
            // The row, a button; the player under the parts shows the same title as text.
            guard let row = driver.elements.first(where: {
                $0.accessibilityLabel == last.title && $0.accessibilityTraits.contains(.button)
            }) else { return false }
            return ["Playing", "Paused"].contains(row.accessibilityValue ?? "")
                && row.accessibilityFrame.minY >= parts.minY - 1 && row.accessibilityFrame.maxY <= parts.maxY + 1
        }
    }

    /// The window frame of the scroller showing `element`: a half of the stand.
    private func scroller(holding element: NSObject) -> CGRect? {
        let centre = CGPoint(x: element.accessibilityFrame.midX, y: element.accessibilityFrame.midY)
        return descendants(of: window) { $0 is UIScrollView }
            .compactMap { view in view.superview.map { $0.convert(view.frame, to: nil) } }
            .filter { $0.contains(centre) }
            .min { $0.height < $1.height }
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
