import UIKit
import XCTest
@testable import pitchperfect

/// The instrument's rules, driven through its model the way its touch surface
/// and accessibility elements drive it.
@MainActor
final class PitchPipeModelTests: PitchPerfectTestCase {
    private var model: PitchPipeModel!

    override func setUp() async throws {
        try await super.setUp()
        model = PitchPipeModel()
        model.geometry = InstrumentGeometry(size: CGSize(width: 402, height: 640), count: model.notes.count)
    }

    override func tearDown() async throws {
        model.stopAll()
        model = nil
        try await super.tearDown()
    }

    private func center(_ index: Int) -> CGPoint { model.geometry.cellCenters[index] }
    private var notes: [DPNote] { model.notes }

    func testTheLowRangeRunsCToCWithItsNaturalsMarked() {
        XCTAssertFalse(model.isHighRange)
        XCTAssertEqual(notes.count, 13)
        XCTAssertEqual(notes.first.map { "\($0.friendlyName!)\($0.octave)" }, "C4")
        XCTAssertEqual(notes.last.map { "\($0.friendlyName!)\($0.octave)" }, "C5")
        XCTAssertEqual(model.naturals, [true, false, true, false, true, true, false, true, false, true, false, true, true])
    }

    func testAPressSoundsWhileHeldAndStopsOnRelease() {
        model.touchBegan(id: 1, at: center(0))
        XCTAssertTrue(notes[0].isPlaying)
        XCTAssertEqual(model.playingIndices, [0])
        model.touchEnded(id: 1)
        XCTAssertFalse(notes[0].isPlaying)
    }

    func testEachNaturalRespondsToRapidTaps() {
        for _ in 0..<2 {
            for index in [0, 2, 4, 5, 7, 9, 11] {
                model.touchBegan(id: index, at: center(index))
                XCTAssertTrue(notes[index].isPlaying)
                XCTAssertEqual(model.playingIndices, [index])
                model.touchEnded(id: index)
                XCTAssertFalse(model.anyPlaying)
            }
        }
    }

    func testSlidingAFingerHandsTheNoteToTheNextCell() {
        model.touchBegan(id: 1, at: center(0))
        model.touchMoved(id: 1, to: center(1))
        XCTAssertFalse(notes[0].isPlaying)
        XCTAssertTrue(notes[1].isPlaying)
        model.touchMoved(id: 1, to: center(2))
        XCTAssertFalse(notes[1].isPlaying)
        XCTAssertTrue(notes[2].isPlaying)
        // Off the ring the finger lets go of the instrument, as it always has:
        // coming back onto a cell does not sound it.
        model.touchMoved(id: 1, to: model.geometry.faceCenter)
        XCTAssertFalse(model.anyPlaying)
        model.touchMoved(id: 1, to: center(3))
        XCTAssertFalse(model.anyPlaying)
        model.touchEnded(id: 1)
    }

    func testTwoFingersOnOneCellKeepItSoundingUntilBothLift() {
        model.touchBegan(id: 1, at: center(4))
        model.touchBegan(id: 2, at: center(4))
        model.touchEnded(id: 1)
        XCTAssertTrue(notes[4].isPlaying, "the other finger still holds it")
        model.touchEnded(id: 2)
        XCTAssertFalse(notes[4].isPlaying)
    }

    func testAChordOfFingersSoundsTogether() {
        for (id, index) in [0, 4, 7].enumerated() { model.touchBegan(id: id, at: center(index)) }
        XCTAssertEqual(model.playingIndices, [0, 4, 7])
        for id in 0..<3 { model.touchEnded(id: id) }
        XCTAssertFalse(model.anyPlaying)
    }

    func testToggleModeFlipsANoteOnEachPress() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        XCTAssertTrue(model.toggleMode)
        model.touchBegan(id: 1, at: center(0))
        model.touchEnded(id: 1)
        XCTAssertTrue(notes[0].isPlaying, "a toggled note outlives the press")
        model.touchBegan(id: 2, at: center(0))
        model.touchEnded(id: 2)
        XCTAssertFalse(notes[0].isPlaying)
    }

    func testTheRangeSelectorSwitchesRangesAndSilencesTheInstrument() {
        model.touchBegan(id: 1, at: center(0))
        model.touchEnded(id: 9)
        model.touchEnded(id: 1)
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.touchBegan(id: 1, at: center(0))
        XCTAssertTrue(model.anyPlaying)
        let high = model.geometry.rangeHighRect
        model.touchBegan(id: 2, at: CGPoint(x: high.midX, y: high.midY))
        XCTAssertTrue(model.isHighRange)
        XCTAssertTrue(DPPitchPipeModel().isFromFToF, "the range is remembered")
        XCTAssertFalse(model.anyPlaying)
        XCTAssertEqual(notes.first.map { "\($0.friendlyName!)\($0.octave)" }, "F4")
        XCTAssertEqual(notes.last.map { "\($0.friendlyName!)\($0.octave)" }, "F5")
        let low = model.geometry.rangeLowRect
        model.touchBegan(id: 3, at: CGPoint(x: low.midX, y: low.midY))
        XCTAssertFalse(model.isHighRange)
    }

    func testTheRangeSelectorIgnoresAPressWhileAFingerHoldsANote() {
        model.touchBegan(id: 1, at: center(0))
        let high = model.geometry.rangeHighRect
        model.touchBegan(id: 2, at: CGPoint(x: high.midX, y: high.midY))
        XCTAssertFalse(model.isHighRange, "a second finger cannot yank the range from under a held note")
        model.touchEnded(id: 1)
    }

    func testTheReadoutNamesWhatSounds() {
        XCTAssertNil(model.readout)
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.touchBegan(id: 1, at: center(9))
        XCTAssertEqual(model.readout, .init(names: "A4", detail: "440.0 Hz", isChord: false))
        model.touchBegan(id: 2, at: center(0))
        XCTAssertEqual(model.readout?.names, "C4 A4")
        XCTAssertEqual(model.readout?.detail, "MAJOR 6TH")
        model.touchBegan(id: 3, at: center(4))
        XCTAssertEqual(model.readout?.detail, "3 NOTES")
        // Adding the minor seventh over C, E and G makes the barbershop seventh.
        model.touchBegan(id: 4, at: center(9))
        model.touchBegan(id: 5, at: center(7))
        model.touchBegan(id: 6, at: center(10))
        XCTAssertEqual(model.readout?.names, "C4 E4 G4 A\u{266F}4")
        XCTAssertEqual(model.readout, .init(names: "C4 E4 G4 A\u{266F}4", detail: "BARBERSHOP!", isChord: true))
    }

    func testIntervalsAreNamedUpToTheOctave() {
        XCTAssertEqual(PitchPipeModel.intervalName(between: 0, and: 0), "UNISON")
        XCTAssertEqual(PitchPipeModel.intervalName(between: 0, and: 7), "PERFECT 5TH")
        XCTAssertEqual(PitchPipeModel.intervalName(between: 12, and: 0), "OCTAVE")
    }

    func testVoiceOverNamesBothSpellingsOfAnAccidental() {
        XCTAssertEqual(model.spokenName(at: 0), "C, octave 4")
        XCTAssertEqual(model.spokenName(at: 1), "C sharp, D flat, octave 4")
        XCTAssertEqual(model.spokenName(at: 12), "C, octave 5")
    }

    func testVoiceOverActivationSoundsBrieflyOrToggles() {
        model.activate(cell: 0)
        XCTAssertTrue(notes[0].isPlaying)
        settle(3) { !self.notes[0].isPlaying }
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.activate(cell: 2)
        XCTAssertTrue(notes[2].isPlaying)
        model.activate(cell: 2)
        XCTAssertFalse(notes[2].isPlaying)
    }

    func testCellsAreRoundAndTouchTargetsAreGenerous() {
        let geometry = model.geometry
        XCTAssertEqual(geometry.cellCenters.count, 13)
        for index in geometry.cellCenters.indices {
            XCTAssertGreaterThanOrEqual(geometry.cellRect(index).width, 44)
            XCTAssertEqual(geometry.cellIndex(at: geometry.cellCenters[index]), index)
        }
        let edge = CGPoint(x: geometry.cellCenters[0].x + geometry.cellRadius * 1.1, y: geometry.cellCenters[0].y)
        XCTAssertEqual(geometry.cellIndex(at: edge), 0, "the target runs a little past the glass")
        XCTAssertEqual(geometry.cellIndex(at: geometry.faceCenter), -1)
        // The range selector sits in the ring's hole, low above high.
        XCTAssertEqual(geometry.rangeLowRect.maxY, geometry.rangeHighRect.minY)
        XCTAssertGreaterThan(geometry.rangeLowRect.minY, geometry.faceCenter.y)
    }
}

/// The pitch pipe as mounted in the app: its accessibility elements, and what
/// leaving the tab does.
@MainActor
final class PitchPipeScreenTests: PitchPerfectTestCase {
    func testEveryCellAndRangePositionIsAButtonVoiceOverCanActivate() throws {
        let app = try launch()
        let ui = app.ui
        for label in ["C, octave 4", "C sharp, D flat, octave 4", "B, octave 4", "C, octave 5",
                      "Octave range C to C", "Octave range F to F"] {
            let element = try XCTUnwrap(ui.element(label: label), label)
            XCTAssertTrue(element.accessibilityTraits.contains(.button), label)
            XCTAssertGreaterThanOrEqual(element.accessibilityFrame.width, 44, label)
        }
        XCTAssertTrue(ui.element(label: "Octave range C to C")!.accessibilityTraits.contains(.selected))
        ui.tap(label: "Octave range F to F")
        XCTAssertTrue(app.models.pitchPipe.isHighRange)
        XCTAssertTrue(ui.exists(label: "F, octave 5"))
        XCTAssertFalse(ui.exists(label: "C, octave 4"))
        XCTAssertTrue(ui.element(label: "Octave range F to F")!.accessibilityTraits.contains(.selected))
    }

    func testLeavingThePitchPipeStopsItsNotes() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.ui.tap(label: "C, octave 4")
        XCTAssertTrue(DPNote.c4().isPlaying)
        app.show(tab: 1)
        XCTAssertFalse(DPNote.c4().isPlaying)
    }

    func testTheSettingsChangeReachesTheInstrumentAtOnce() throws {
        let app = try launch()
        let model = app.models.pitchPipe
        XCTAssertFalse(model.toggleMode)
        DPSettingsModel.sharedInstance.toggleNotes = true
        XCTAssertTrue(model.toggleMode)
    }
}
