import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectNavigationTests: PitchPerfectTestCase {
    private func tabBar(_ app: HostedApp) throws -> UITabBar {
        try XCTUnwrap(app.descendants(of: UITabBar.self, in: app.window).first)
    }

    private func navigationTitle(_ app: HostedApp) -> String? {
        app.descendants(of: UINavigationBar.self, in: app.window)
            .first { $0.window != nil && !$0.isHidden && $0.alpha > 0 }?.topItem?.title
    }

    func testTheFourTabsShowInOrderWithTheirTitles() throws {
        let app = try launch()
        XCTAssertEqual(try tabBar(app).items?.map(\.title), ["Pitch Pipe", "Notes", "Keys", "Songs"])
        for (index, title) in ["Pitch Pipe", "Notes", "Keys", "Songs"].enumerated() {
            app.show(tab: index)
            XCTAssertEqual(try tabBar(app).selectedItem?.title, title)
            XCTAssertTrue(app.ui.exists(id: "gearshape") || index == 3, "\(title) offers Settings")
        }
    }

    func testEachTabIsAButtonNamedForItsScreen() throws {
        let app = try launch()
        for title in ["Pitch Pipe", "Notes", "Keys", "Songs"] {
            XCTAssertTrue(app.ui.elements.contains { $0.accessibilityLabel == title && $0.accessibilityTraits.contains(.button) },
                          title)
        }
    }

    func testRapidTabSwitchingKeepsTheChosenTabShowing() throws {
        let app = try launch()
        for _ in 0..<3 {
            for (index, title) in ["Pitch Pipe", "Notes", "Keys", "Songs"].enumerated() {
                app.show(tab: index)
                XCTAssertEqual(try tabBar(app).selectedItem?.title, title)
            }
        }
    }

    func testTheTabIconsAreTemplateImages() throws {
        let app = try launch()
        for item in try tabBar(app).items ?? [] {
            XCTAssertEqual(item.image?.renderingMode, .alwaysTemplate, item.title ?? "")
        }
    }
}

@MainActor
final class NotePlayerTests: PitchPerfectTestCase {
    func testAMomentaryPressSoundsOnlyWhileHeld() {
        let note = DPNote.c4()!
        let player = NotePlayer.shared
        player.pressBegan(note)
        XCTAssertTrue(player.isPlaying(note))
        player.pressEnded(note)
        XCTAssertFalse(player.isPlaying(note))
    }

    func testToggleNotesKeepsANoteUntilPressedAgain() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let note = DPNote.c4()!
        let player = NotePlayer.shared
        player.pressBegan(note)
        player.pressEnded(note)
        XCTAssertTrue(player.isPlaying(note))
        player.pressBegan(note)
        player.pressEnded(note)
        XCTAssertFalse(player.isPlaying(note))
    }

    func testEveryStartAndStopIsObservable() {
        let player = NotePlayer.shared
        let before = player.revision
        player.play(DPNote.c4())
        player.stop(DPNote.c4())
        XCTAssertEqual(player.revision, before + 2)
    }
}

@MainActor
final class NotesAndKeysTests: PitchPerfectTestCase {
    func testTheNotesListNamesEverySpellingAndFrequency() throws {
        let app = try launch()
        app.show(tab: 1)
        let notes = DPNote.prunedNotes() as! [DPNote]
        let middle = notes[notes.count / 2]
        let row = try XCTUnwrap(app.ui.element(label: NoteSpelling.spoken(middle)))
        XCTAssertEqual(row.accessibilityValue, String(format: "%1.2f Hz", middle.frequency))
        XCTAssertEqual(NoteSpelling.spoken(DPNote.c4()), "C 4")
        let sharp = try XCTUnwrap(notes.first { $0.alternate != nil })
        XCTAssertTrue(NoteSpelling.spoken(sharp).contains("sharp"))
        XCTAssertTrue(NoteSpelling.spoken(sharp).contains("flat"))
    }

    func testTheNotesListOpensOnItsMiddleNote() throws {
        let app = try launch()
        app.show(tab: 1)
        let notes = DPNote.prunedNotes() as! [DPNote]
        XCTAssertTrue(app.ui.exists(label: NoteSpelling.spoken(notes[notes.count / 2])))
        XCTAssertFalse(app.ui.exists(label: NoteSpelling.spoken(notes[0])), "the list opens mid-range, not at the top")
    }

    func testLeavingNotesStopsWhatItSounded() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.show(tab: 1)
        let note = DPNote.c4()!
        NotePlayer.shared.pressBegan(note)
        XCTAssertTrue(note.isPlaying)
        app.show(tab: 2)
        XCTAssertFalse(note.isPlaying)
    }

    func testMajorAndMinorShowEveryKeySignature() throws {
        let app = try launch()
        app.show(tab: 2)
        let majors = DPKey.majorKeys() as! [DPKey]
        let minors = DPKey.minorKeys() as! [DPKey]
        XCTAssertEqual(majors.count, 13)
        XCTAssertEqual(minors.count, 13)
        let middle = SongEditorSpeech.name(for: majors[6], minor: false)
        XCTAssertEqual(middle, "C major, no sharps or flats")
        XCTAssertTrue(app.ui.exists(label: middle))
        app.showMinorKeys()
        XCTAssertTrue(app.ui.exists(label: SongEditorSpeech.name(for: minors[6], minor: true)))
        XCTAssertFalse(app.ui.exists(label: middle))
    }

    func testSwitchingModeStopsTheOtherModesNotes() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.show(tab: 2)
        let key = (DPKey.majorKeys() as! [DPKey])[6]
        NotePlayer.shared.pressBegan(key.note)
        XCTAssertTrue(key.note.isPlaying)
        app.showMinorKeys()
        XCTAssertFalse(key.note.isPlaying)
    }
}
