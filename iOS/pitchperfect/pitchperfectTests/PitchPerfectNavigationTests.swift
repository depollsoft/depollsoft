import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectNavigationTests: PitchPerfectControllerTestCase {
    func testStoryboardDisplaysAllFourTabsAndTheirContent() throws {
        try withApp { tabs in
            XCTAssertEqual(tabs.tabBar.items?.map(\.title), ["Pitch Pipe", "Notes", "Keys", "Songs"])
            XCTAssertFalse(tabs.tabBar.isHidden)
            XCTAssertNotNil(tabs.tabBar.window)
            XCTAssertGreaterThan(tabs.view.bounds.width, 0)
            let pipe = try select(0, in: tabs, as: DPPitchPipeViewController.self)
            let instrument = try XCTUnwrap(pipe.value(forKey: "instrumentView") as? DPPitchInstrumentView)
            XCTAssertFalse(instrument.isHidden)
            XCTAssertNotNil(instrument.window)
            XCTAssertTrue((instrument.accessibilityElements as? [UIAccessibilityElement])?
                .contains { $0.accessibilityLabel == "C, octave 4" } == true)
            let notes = try select(1, in: tabs, as: DPNotesViewController.self)
            XCTAssertGreaterThan(try table(in: notes).numberOfRows(inSection: 0), 0)
            let keys = try select(2, in: tabs, as: DPKeysViewController.self)
            XCTAssertEqual(try table(in: keys).numberOfRows(inSection: 0), 13)
            let songs = try select(3, in: tabs, as: DPSongListViewController.self)
            XCTAssertEqual(try table(in: songs).numberOfRows(inSection: 0), 0)
        }
    }

    func testRapidTabSwitchingKeepsTheSelectedControllerVisible() throws {
        try withApp { tabs in
            for _ in 0..<3 {
                for (index, title) in ["Pitch Pipe", "Notes", "Keys", "Songs"].enumerated() {
                    let controller = try select(index, in: tabs, as: UIViewController.self)
                    XCTAssertEqual(controller.navigationItem.title, title)
                    XCTAssertEqual(tabs.tabBar.selectedItem, tabs.tabBar.items?[index])
                }
            }
        }
    }

    func testKeySelectionCanReturnToPitchPipe() throws {
        try withApp { tabs in
            let keys = try select(2, in: tabs, as: DPKeysViewController.self)
            let keyCell = try cell(0, in: table(in: keys))
            let key = try XCTUnwrap(keyCell.value(forKey: "key") as? DPKey)
            assertPressAndRelease(keyCell, note: key.note)
            let pipe = try select(0, in: tabs, as: DPPitchPipeViewController.self)
            XCTAssertEqual(pipe.navigationItem.title, "Pitch Pipe")
            XCTAssertFalse(key.note.isPlaying)
            let instrument = try XCTUnwrap(pipe.value(forKey: "instrumentView") as? DPPitchInstrumentView)
            XCTAssertEqual((instrument.accessibilityElements as? [UIAccessibilityElement])?.count, 15)
        }
    }
}

@MainActor
final class PitchPerfectInstrumentTests: PitchPerfectControllerTestCase {
    private func instrument(in tabs: UITabBarController) throws -> DPPitchInstrumentView {
        let pipe = try select(0, in: tabs, as: DPPitchPipeViewController.self)
        let instrument = try XCTUnwrap(pipe.value(forKey: "instrumentView") as? DPPitchInstrumentView)
        instrument.layoutIfNeeded()
        return instrument
    }

    private func touch(_ label: String, in instrument: DPPitchInstrumentView) throws -> PitchPerfectTestTouch {
        let elements = try XCTUnwrap(instrument.accessibilityElements as? [UIAccessibilityElement])
        let element = try XCTUnwrap(elements.first { $0.accessibilityLabel == label })
        XCTAssertTrue(element.accessibilityTraits.contains(.button))
        let frame = element.accessibilityFrameInContainerSpace
        XCTAssertGreaterThanOrEqual(frame.width, 44)
        XCTAssertGreaterThanOrEqual(frame.height, 44)
        XCTAssertTrue(instrument.bounds.contains(frame))
        let touch = PitchPerfectTestTouch()
        touch.point = CGPoint(x: frame.midX, y: frame.midY)
        XCTAssertTrue(instrument.hitTest(touch.point, with: nil) === instrument)
        return touch
    }

    func testCNotePressAndReleaseUsesTheVisibleHitTarget() throws {
        try withApp { tabs in
            let instrument = try instrument(in: tabs)
            let touch = try touch("C, octave 4", in: instrument)
            let note = try XCTUnwrap(DPNote.c4())
            instrument.touchesBegan([touch], with: nil)
            XCTAssertTrue(note.isPlaying)
            instrument.touchesEnded([touch], with: nil)
            XCTAssertFalse(note.isPlaying)
        }
    }

    func testAllNaturalNotesRespondToRapidTaps() throws {
        try withApp { tabs in
            let instrument = try instrument(in: tabs)
            let notes = try XCTUnwrap(DPPitchPipeModel().notes as? [DPNote])
            for _ in 0..<2 {
                for index in [0, 2, 4, 5, 7, 9, 11] {
                    let note = notes[index]
                    let touch = try touch("\(note.friendlyName!), octave 4", in: instrument)
                    instrument.touchesBegan([touch], with: nil)
                    XCTAssertTrue(note.isPlaying)
                    XCTAssertEqual(notes.filter(\.isPlaying).count, 1)
                    instrument.touchesEnded([touch], with: nil)
                    XCTAssertFalse(notes.contains { $0.isPlaying })
                }
            }
        }
    }

    func testSecondTapStopsAToggledNote() throws {
        try withApp { tabs in
            let instrument = try instrument(in: tabs)
            DPSettingsModel.sharedInstance.toggleNotes = true
            XCTAssertTrue(instrument.toggleMode)
            let touch = try touch("C, octave 4", in: instrument)
            let note = try XCTUnwrap(DPNote.c4())
            instrument.touchesBegan([touch], with: nil)
            instrument.touchesEnded([touch], with: nil)
            XCTAssertTrue(note.isPlaying)
            instrument.touchesBegan([touch], with: nil)
            instrument.touchesEnded([touch], with: nil)
            XCTAssertFalse(note.isPlaying)
        }
    }

    func testLeavingPitchPipeStopsSoundingNotes() throws {
        try withApp { tabs in
            let instrument = try instrument(in: tabs)
            DPSettingsModel.sharedInstance.toggleNotes = true
            let touch = try touch("C, octave 4", in: instrument)
            instrument.touchesBegan([touch], with: nil)
            instrument.touchesEnded([touch], with: nil)
            XCTAssertTrue(DPNote.c4().isPlaying)
            _ = try select(1, in: tabs, as: DPNotesViewController.self)
            XCTAssertFalse(DPNote.c4().isPlaying)
        }
    }
}
