//
//  PitchPerfectAuditTests.swift
//  pitchperfectTests
//
//  Behaviour the UIKit screens had that a review of the SwiftUI port found
//  missing or changed: how rows respond to touches and VoiceOver, where lists
//  are left, how the editor presents, and what outlives a closed sheet.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// Only supplies an identity; the surface under test never asks where it is.
private final class FakeTouch: UITouch {}

@MainActor
extension HostedApp {
    /// The visible list's scroll view (Lists are collection views; the set-list
    /// selector is a plain scroll view and is never picked).
    var listScrollView: UIScrollView? {
        descendants(of: UICollectionView.self, in: window)
            .filter { $0.window != nil && !$0.isHidden && $0.bounds.height > 100 }
            .max { $0.contentSize.height < $1.contentSize.height }
    }

    /// The tab's list once SwiftUI has mounted it; a cold CI runner can take a few beats.
    func waitForListScrollView(timeout: TimeInterval = 5) -> UIScrollView? {
        let deadline = Date().addingTimeInterval(timeout)
        while listScrollView == nil, Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.02))
        }
        return listScrollView
    }
}

// MARK: - Rows press as the UIKit cells did

@MainActor
final class TouchPressSurfaceTests: XCTestCase {
    private var began = 0
    private var ended = 0

    private func surface() -> TouchPressSurface.Surface {
        let surface = TouchPressSurface.Surface()
        surface.began = { [unowned self] in self.began += 1 }
        surface.ended = { [unowned self] in self.ended += 1 }
        return surface
    }

    func testAPressLastsUntilTheFingerLiftsWhereverItSlides() {
        let surface = surface()
        let finger = FakeTouch()
        surface.touchesBegan([finger], with: nil)
        XCTAssertEqual(began, 1)
        // A finger that slides off the row keeps delivering to it until it lifts:
        // nothing ends, and nothing begins again when it slides back.
        surface.touchesMoved([finger], with: nil)
        surface.touchesMoved([finger], with: nil)
        XCTAssertEqual(began, 1)
        XCTAssertEqual(ended, 0)
        surface.touchesEnded([finger], with: nil)
        XCTAssertEqual(ended, 1)
    }

    func testAScrollThatCancelsTheTouchEndsThePress() {
        let surface = surface()
        let finger = FakeTouch()
        surface.touchesBegan([finger], with: nil)
        surface.touchesCancelled([finger], with: nil)
        XCTAssertEqual(ended, 1)
        // A stray end after the cancel is not a second press ending.
        surface.touchesEnded([finger], with: nil)
        XCTAssertEqual(ended, 1)
    }

    func testTheSurfaceIsNotItselfAnAccessibilityElement() {
        XCTAssertFalse(surface().isAccessibilityElement)
    }
}

// MARK: - VoiceOver sounds rows

@MainActor
final class RowActivationTests: PitchPerfectTestCase {
    func testActivationSoundsAMomentaryNoteBrieflyAndTogglesAToggledOne() {
        let player = NotePlayer.shared
        let note = DPNote.c4()!
        var scheduled: (TimeInterval, () -> Void)?
        player.activate(note) { delay, work in scheduled = (delay, work) }
        XCTAssertTrue(note.isPlaying)
        XCTAssertEqual(scheduled?.0, NotePlayer.activationDuration)
        scheduled?.1()
        XCTAssertFalse(note.isPlaying)

        DPSettingsModel.sharedInstance.toggleNotes = true
        player.activate(note) { _, _ in XCTFail("a toggled note is not timed") }
        XCTAssertTrue(note.isPlaying)
        player.activate(note) { _, _ in XCTFail("a toggled note is not timed") }
        XCTAssertFalse(note.isPlaying)
    }

    func testNotesAndKeysRowsSoundWhenVoiceOverActivatesThem() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.show(tab: 1)
        let notes = DPNote.prunedNotes() as! [DPNote]
        let note = notes[notes.count / 2]
        let row = try XCTUnwrap(app.ui.element(label: NoteSpelling.spoken(note)))
        XCTAssertTrue(row.accessibilityTraits.contains(.button))
        app.ui.tap(label: NoteSpelling.spoken(note))
        XCTAssertTrue(note.isPlaying, "the Notes row plays for VoiceOver")
        app.ui.tap(label: NoteSpelling.spoken(note))
        XCTAssertFalse(note.isPlaying, "and a second activation stops it, as a second press would")

        app.show(tab: 2)
        let key = (DPKey.majorKeys() as! [DPKey])[6]
        app.ui.tap(label: SongEditorSpeech.name(for: key, minor: false))
        XCTAssertTrue(key.note.isPlaying, "the Keys row plays for VoiceOver")
    }

    func testASongRowSoundsItsKeyWhenVoiceOverActivatesIt() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let song = seedSongs(["Blue Skies"])[0]
        let app = try launch()
        app.show(tab: 3)
        app.ui.tap(label: "Blue Skies, C")
        XCTAssertTrue(song.key.note.isPlaying)
        XCTAssertTrue(app.songs.isLit(song))
        app.ui.tap(label: "Blue Skies, C")
        XCTAssertFalse(song.key.note.isPlaying)
    }
}

// MARK: - Songs

@MainActor
final class SongsAuditTests: PitchPerfectTestCase {
    func testASavedEditShowsInItsRow() throws {
        seedSongs(["Blue Skies"])
        let app = try launch()
        app.show(tab: 3)
        XCTAssertTrue(app.ui.exists(label: "Blue Skies, C"))
        app.ui.tap(id: "pencil")
        app.ui.tap(id: "song.edit")
        settle { app.sheet.exists(id: "checkmark") }
        app.typeSongTitle("Heart of My Heart")
        app.sheet.tap(label: SongEditorSpeech.name(for: (DPKey.majorKeys() as! [DPKey])[7], minor: false))
        app.sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
        ScreenCatalog.settle(0.2)
        app.ui.wait { app.ui.exists(label: "Heart of My Heart, G") }
        XCTAssertFalse(app.ui.exists(label: "Blue Skies, C"),
                       "stored \(DPSongsModel.sharedInstance.currentList.songs.map { "\($0.name ?? "") \($0.key?.friendlyName() ?? "")" }), showing \(app.ui.labels)")
    }

    func testASavedEditRedrawsTheListButACancelledOneDoesNot() throws {
        let song = seedSongs(["Blue Skies"])[0]
        let model = SongListModel()
        let before = model.revision
        model.editSong(song)
        try XCTUnwrap(model.editor).completion(false)
        XCTAssertEqual(model.revision, before)
        XCTAssertNil(model.editor, "cancelling closes the editor")
        model.editSong(song)
        try XCTUnwrap(model.editor).completion(true)
        XCTAssertGreaterThan(model.revision, before, "a save redraws the rows, as UIKit reloaded the table")
        XCTAssertNil(model.editor)
    }

    func testTheEditorSlidesUpForANewSongAndFlipsForAnExistingOne() throws {
        seedSongs(["Blue Skies"])
        let app = try launch()
        app.show(tab: 3)
        app.ui.tap(id: "pencil")
        app.ui.tap(id: "plus")
        settle { app.topPresented !== app.host }
        let adding = try XCTUnwrap(app.topPresented as? UINavigationController)
        XCTAssertEqual(adding.modalTransitionStyle, .coverVertical)
        XCTAssertEqual(adding.preferredContentSize.width, 320,
                       "the small form UIKit gave the editor on iPad, on every system version")
        settle { app.sheet.exists(id: "xmark") }
        app.sheet.tap(id: "xmark")
        settle { app.topPresented === app.host }
        XCTAssertNil(app.songs.editor)

        app.ui.tap(id: "song.edit")
        settle { app.topPresented !== app.host }
        let editing = try XCTUnwrap(app.topPresented as? UINavigationController)
        XCTAssertEqual(editing.modalTransitionStyle, .flipHorizontal)
        settle { app.sheet.exists(id: "checkmark") }
        XCTAssertTrue(app.navigationTitles.contains("Edit Song"))
    }

    func testSwipingTheEditorAwayCancelsIt() throws {
        seedSongs(["Blue Skies"])
        let app = try launch()
        app.show(tab: 3)
        app.ui.tap(id: "pencil")
        app.ui.tap(id: "plus")
        settle { app.topPresented !== app.host }
        let editor = app.topPresented
        let controller = try XCTUnwrap(editor.presentationController)
        editor.dismiss(animated: false)
        controller.delegate?.presentationControllerDidDismiss?(controller)
        XCTAssertNil(app.songs.editor)
        XCTAssertEqual(DPSongsModel.sharedInstance.defaultSongList.songs.map(\.name), ["Blue Skies"])
    }

    func testAToggledRowGoesDarkAsTheFingerLandsToStopIt() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let model = SongListModel()
        let song = seedSongs(["Blue Skies"])[0]
        model.press(song)
        model.release(song)
        XCTAssertTrue(model.isLit(song))
        model.press(song)
        XCTAssertFalse(model.isLit(song), "dark at touch-down, not at lift")
        model.release(song)
        XCTAssertFalse(model.isLit(song))
    }

    func testAListLeftFromTheSetListsScreenComesBackWhereItWas() throws {
        seedSongs((0..<40).map { "Song \($0)" })
        let other = try customList(named: "Saturday show")
        let app = try launch()
        app.show(tab: 3)
        let list = try XCTUnwrap(app.waitForListScrollView())
        let top = -list.adjustedContentInset.top
        list.setContentOffset(CGPoint(x: 0, y: top + 437), animated: false)
        ScreenCatalog.settle(0.2)
        // The Set Lists screen switches through the store, not through the Songs model.
        DPSongsModel.sharedInstance.currentListId = other.id
        ScreenCatalog.settle(0.3)
        DPSongsModel.sharedInstance.currentListId = DPSongsModel.defaultListId
        ScreenCatalog.settle(0.3)
        let restored = try XCTUnwrap(app.waitForListScrollView())
        if #available(iOS 18.0, *) {
            XCTAssertEqual(restored.contentOffset.y, top + 437, accuracy: 0.5, "the exact offset, as UIKit kept it")
        } else {
            XCTAssertGreaterThan(restored.contentOffset.y, top + 300)
        }
    }

    func testAListNeverShownStartsAtTheTop() throws {
        seedSongs((0..<40).map { "Song \($0)" })
        let other = try customList(named: "Saturday show")
        other.songs = (0..<40).map { index in
            let song = DPPitchedSong()
            song.name = "Other \(index)"
            song.key = (DPKey.majorKeys() as! [DPKey])[6]
            return song
        }
        let app = try launch()
        app.show(tab: 3)
        let list = try XCTUnwrap(app.waitForListScrollView())
        list.setContentOffset(CGPoint(x: 0, y: -list.adjustedContentInset.top + 500), animated: false)
        ScreenCatalog.settle(0.2)
        app.songs.select(listId: other.id)
        ScreenCatalog.settle(0.3)
        let shown = try XCTUnwrap(app.waitForListScrollView())
        XCTAssertEqual(shown.contentOffset.y, -shown.adjustedContentInset.top, accuracy: 0.5)
    }

    func testTheChosenPositionStaysInViewAsPositionsChange() throws {
        seedSongs(["Blue Skies"])
        let lists = try (1...6).map { try customList(named: "Set list number \($0)") }
        let app = try launch()
        app.show(tab: 3)
        app.songs.select(listId: lists[5].id)
        ScreenCatalog.settle(0.3)
        func chosenIsVisible() -> Bool {
            guard let chosen = app.ui.element(id: "setlist.\(lists[5].id)") else { return false }
            let frame = chosen.accessibilityFrame
            return app.window.bounds.contains(CGPoint(x: frame.midX, y: frame.midY))
        }
        XCTAssertTrue(chosenIsVisible())
        // Renaming an earlier list widens it; the chosen position must not be pushed off.
        _ = DPSongsModel.sharedInstance.renameList(lists[0], to: "A much, much longer set list name than before")
        ScreenCatalog.settle(0.3)
        XCTAssertTrue(chosenIsVisible())
    }
}

// MARK: - Set Lists

@MainActor
final class SetListsAuditTests: PitchPerfectTestCase {
    func testASwipeOffersDeleteOutermostSoAFullSwipeAsksToDelete() throws {
        let list = try customList(named: "Saturday show")
        let model = SetListsModel()
        XCTAssertEqual(model.swipeActions(for: list), [.delete, .duplicate, .rename])
        XCTAssertEqual(model.swipeActions(for: DPSongsModel.sharedInstance.defaultSongList), [])
    }

    func testDeletingFromTheSwipeAsksFirstAndKeepsTheList() throws {
        let list = try customList(named: "Saturday show")
        let model = SetListsModel()
        model.confirmDelete(list)
        XCTAssertEqual(model.prompts.kind, .delete(list))
        XCTAssertNotNil(DPSongsModel.sharedInstance.songLists[list.id], "nothing leaves until the answer")
        model.prompts.dismiss()
        XCTAssertNotNil(DPSongsModel.sharedInstance.songLists[list.id])
    }
}

// MARK: - Notes and Keys keep their place

@MainActor
final class InstrumentListPlaceTests: PitchPerfectTestCase {
    func testNotesAndKeysOpenMidwayOnceAndThenKeepTheirPlace() throws {
        let app = try launch()
        for tab in [1, 2] {
            app.show(tab: tab)
            let list = try XCTUnwrap(app.waitForListScrollView())
            let top = -list.adjustedContentInset.top
            XCTAssertGreaterThan(list.contentOffset.y, top + 1, "opens midway")
            list.setContentOffset(CGPoint(x: 0, y: top), animated: false)
            ScreenCatalog.settle(0.1)
            app.show(tab: 0)
            app.show(tab: tab)
            let again = try XCTUnwrap(app.waitForListScrollView())
            XCTAssertEqual(again.contentOffset.y, top, accuracy: 0.5,
                           "coming back to the tab keeps the place, as UIKit's viewDidLoad-only scroll did")
        }
    }
}

// MARK: - Settings and account

/// An account whose delete the test answers.
@MainActor
private final class DeferredAccount {
    var signOuts = 0
    var descriptions = 0
    var pending: ((Error?) -> Void)?

    var service: AccountService {
        AccountService(isSignedIn: { true },
                       userDescription: { [self] in self.descriptions += 1; return "Facebook " },
                       signOut: { [self] in self.signOuts += 1 },
                       deleteAccount: { [self] completion in self.pending = completion })
    }
}

@MainActor
final class AccountAuditTests: PitchPerfectTestCase {
    func testTheDeleteTitleIsReadOnceWhenAskedNotOnEveryRedraw() {
        let account = DeferredAccount()
        let model = SettingsModel(account: account.service)
        XCTAssertEqual(account.descriptions, 0, "opening Settings reads no account details")
        model.requestDelete()
        XCTAssertEqual(model.deleteTitle, "Delete Account Facebook ")
        XCTAssertEqual(account.descriptions, 1)
    }

    func testADeleteThatFinishesAfterSettingsClosedStillSignsOut() {
        let account = DeferredAccount()
        var model: SettingsModel? = SettingsModel(account: account.service)
        model?.requestDelete()
        model?.confirmDelete()
        weak var closed = model
        model = nil
        XCTAssertNil(closed, "Settings has gone")
        account.pending?(nil)
        XCTAssertEqual(account.signOuts, 1, "the deleted account is signed out anyway")
    }

    func testAFailedDeleteAfterSettingsClosedKeepsTheAccount() {
        let account = DeferredAccount()
        var model: SettingsModel? = SettingsModel(account: account.service)
        model?.confirmDelete()
        model = nil
        account.pending?(NSError(domain: "test", code: 1))
        XCTAssertEqual(account.signOuts, 0)
    }

    func testStoredSettingsSurviveANewModel() {
        let defaults = UserDefaults.standard
        defaults.set(true, forKey: "depollsoft.pitchperfect.WakeLock")
        defaults.set(true, forKey: "depollsoft.pitchperfect.ToggleNote")
        let fresh = DPSettingsModel()
        XCTAssertTrue(fresh.wakeLock, "a launch no longer turns Wake Lock off")
        XCTAssertTrue(fresh.toggleNotes, "or Toggle Notes")
    }

    func testTheLoginScreenClosesOnlyOnceTheSignInSheetHasGone() {
        let model = LoginIntroModel()
        model.signIn()
        XCTAssertTrue(model.showingSignIn)
        XCTAssertFalse(model.signInSheetChanged(showing: true))
        model.accountArrived()
        XCTAssertFalse(model.signInSheetChanged(showing: true), "not while the sign-in sheet is up")
        model.showingSignIn = false
        XCTAssertTrue(model.signInSheetChanged(showing: false), "once it has gone")

        let cancelled = LoginIntroModel()
        cancelled.signIn()
        cancelled.showingSignIn = false
        XCTAssertFalse(cancelled.signInSheetChanged(showing: false), "a cancelled sign-in leaves the screen up")
    }
}

// MARK: - Presentation that follows a dismissal

@MainActor
final class PresentationAuditTests: PitchPerfectTestCase {
    override func tearDown() async throws {
        await MainActor.run { TelemetryConsent.onDismiss = nil }
        try await super.tearDown()
    }

    func testPrivacyChoicesReportsItsDismissalOnlyOnceItHasGone() throws {
        let app = try launch()
        var presentingWhenReported: UIViewController?? = .none
        TelemetryConsent.onDismiss = { presentingWhenReported = .some(app.host.presentedViewController) }
        TelemetryConsent.present(from: app.host)
        settle { app.host.presentedViewController is PrivacyChoicesHost && app.sheet.exists(label: "Cancel") }
        app.sheet.tap(label: "Cancel")
        settle { presentingWhenReported != nil }
        XCTAssertNil(presentingWhenReported ?? nil,
                     "reported after the sheet has gone, so the ad-consent flow can present")
    }

    func testTheBannerPresentsFromItsOwnScreen() throws {
        let app = try launch()
        // Every banner's presenter is the controller its own screen belongs to.
        let pipeBanner = try XCTUnwrap(app.descendants(of: BannerHostView.self, in: app.window).first { $0.window != nil })
        let owner = try XCTUnwrap(pipeBanner.banner.rootViewController)
        XCTAssertTrue(owner === pipeBanner.owningViewController)
        XCTAssertTrue(pipeBanner.isDescendant(of: owner.view))
        guard UIDevice.current.userInterfaceIdiom == .phone else { return } // Settings carries a banner on iPhone only.
        app.openSettings()
        settle { app.host.presentedViewController != nil }
        ScreenCatalog.settle(0.2)
        let banners = app.descendants(of: BannerHostView.self, in: app.window)
        let settingsBanner = try XCTUnwrap(banners.first { $0.window != nil && $0.isDescendant(of: app.topPresented.view) })
        XCTAssertNotNil(settingsBanner.banner.rootViewController)
        XCTAssertFalse(settingsBanner.banner.rootViewController === app.host,
                       "the root is presenting Settings; the ad's overlay must come from Settings")
        XCTAssertTrue(settingsBanner.banner.rootViewController?.view.window === app.window)
    }
}
