import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// Where the classic grid puts its cells, its readout and its range control.
final class ClassicGeometryTests: XCTestCase {
    /// An iPhone 17's pitch pipe, between its bars.
    private let phone = ClassicGeometry(size: CGSize(width: 402, height: 640), count: 13)
    /// The same phone turned on its side.
    private let turned = ClassicGeometry(size: CGSize(width: 750, height: 200), count: 13)

    private func slot(_ geometry: ClassicGeometry, _ index: Int) -> (column: Int, row: Int) {
        let rect = geometry.slots[index]
        return (Int((rect.minX / (geometry.size.width / 4)).rounded()), Int((rect.minY / (geometry.size.height / 4)).rounded()))
    }

    func testOneOctaveRunsClockwiseRoundTheEdge() {
        XCTAssertEqual(phone.slots.count, 12, "the radial face's thirteenth note is not on the grid")
        let expected: [(Int, Int)] = [
            (0, 0), (1, 0), (2, 0), (3, 0), // C C♯ D D♯ across the top
            (3, 1), (3, 2), // E F down the right
            (3, 3), (2, 3), (1, 3), (0, 3), // F♯ G G♯ A back along the bottom
            (0, 2), (0, 1), // A♯ B up the left
        ]
        for (index, place) in expected.enumerated() {
            let actual = slot(phone, index)
            XCTAssertTrue(actual == place, "cell \(index) is at \(actual), not \(place)")
        }
    }

    func testEachButtonIsItsSlotInsetAsAMaterialButton() {
        for index in phone.slots.indices {
            XCTAssertEqual(phone.buttons[index], phone.slots[index].insetBy(dx: 4, dy: 6))
            XCTAssertEqual(phone.cellIndex(at: CGPoint(x: phone.slots[index].midX, y: phone.slots[index].midY)), index)
        }
    }

    func testAFingerInAButtonsMarginStillPlaysIt() {
        XCTAssertEqual(phone.cellIndex(at: CGPoint(x: 1, y: 1)), 0, "outside the drawn C, inside its slot")
        XCTAssertEqual(phone.cellIndex(at: CGPoint(x: -1, y: 10)), -1)
    }

    func testTheWellIsTheWholeMiddleAndNoCell() {
        let column = phone.size.width / 4
        let row = phone.size.height / 4
        XCTAssertEqual(phone.well, CGRect(x: column, y: row, width: column * 2, height: row * 2).insetBy(dx: 4, dy: 6))
        for point in [CGPoint(x: column * 1.5, y: row * 1.5), CGPoint(x: column * 2.5, y: row * 2.5),
                      CGPoint(x: phone.well.midX, y: phone.well.midY)] {
            XCTAssertEqual(phone.cellIndex(at: point), -1, "\(point)")
        }
        XCTAssertNil(phone.range(at: CGPoint(x: phone.selectorRect.midX, y: phone.selectorRect.midY)),
                     "the range control takes its own touches")
    }

    func testATallWellPutsTheReadoutInItsTopHalfAndTheRangeControlInItsBottomHalf() {
        XCTAssertTrue(phone.stacked)
        let well = phone.well
        XCTAssertEqual(phone.readoutRect, CGRect(x: well.minX, y: well.minY, width: well.width, height: well.height / 2))
        XCTAssertEqual(phone.selectorRect.midX, well.midX, accuracy: 0.001)
        XCTAssertEqual(phone.selectorRect.midY, well.midY + well.height / 4, accuracy: 0.001,
                       "centred in the bottom half")
    }

    func testAShortWellPutsTheRangeControlBesideTheReadout() {
        XCTAssertFalse(turned.stacked, "half the well is under 56")
        let well = turned.well
        XCTAssertEqual(turned.selectorRect.maxX, well.maxX, accuracy: 0.001)
        XCTAssertEqual(turned.selectorRect.midY, well.midY, accuracy: 0.001)
        XCTAssertEqual(turned.readoutRect.minX, well.minX)
        XCTAssertEqual(turned.readoutRect.maxX, turned.selectorRect.minX - ClassicGeometry.gap, accuracy: 0.001)
        XCTAssertEqual(turned.readoutRect.height, well.height)
        XCTAssertGreaterThanOrEqual(turned.readoutRect.height, ClassicGeometry.minReadout, "so it keeps two lines")
    }

    func testATallRangeControlMovesBesideTheReadout() {
        // Half this well (69) holds the readout's 56, but not an 80-high control.
        let tall = ClassicGeometry(size: CGSize(width: 402, height: 300), count: 12,
                                   selectorSize: CGSize(width: 150, height: 80))
        XCTAssertFalse(tall.stacked)
        let fits = ClassicGeometry(size: CGSize(width: 402, height: 300), count: 12)
        XCTAssertTrue(fits.stacked)
    }

    func testAVeryShortFacesReadoutReadsAsOneLine() {
        let squat = ClassicGeometry(size: CGSize(width: 600, height: 120), count: 13)
        XCTAssertFalse(squat.stacked)
        XCTAssertLessThan(squat.readoutRect.height, ClassicGeometry.minReadout)
    }

    func testAWideRangeControlTakesAtMostHalfAShortWell() {
        let wide = ClassicGeometry(size: CGSize(width: 750, height: 200), count: 13,
                                   selectorSize: CGSize(width: 900, height: 32))
        XCTAssertEqual(wide.selectorRect.width, wide.well.width / 2, accuracy: 0.001)
    }
}

/// The instrument's rules through the classic grid: the model hit-tests
/// against whichever face shows.
@MainActor
final class ClassicPitchPipeModelTests: PitchPerfectTestCase {
    private var model: PitchPipeModel!

    override func setUp() async throws {
        try await super.setUp()
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        model = PitchPipeModel()
        model.classicGeometry = ClassicGeometry(size: CGSize(width: 402, height: 640), count: model.notes.count)
    }

    override func tearDown() async throws {
        model.stopAll()
        model = nil
        try await super.tearDown()
    }

    private func center(_ index: Int) -> CGPoint {
        let slot = model.classicGeometry.slots[index]
        return CGPoint(x: slot.midX, y: slot.midY)
    }

    private var notes: [DPNote] { model.notes }

    func testTheSettingShowsTheClassicFace() {
        XCTAssertTrue(model.isClassic)
        XCTAssertEqual(model.classicGeometry.slots.count, 12, "one octave; the model keeps its thirteenth for the radial face")
    }

    func testEveryCellSoundsWhileHeld() {
        for index in 0..<12 {
            model.touchBegan(id: 1, at: center(index))
            XCTAssertEqual(model.playingIndices, [index], "cell \(index)")
            model.touchEnded(id: 1)
            XCTAssertFalse(model.anyPlaying, "cell \(index)")
        }
    }

    func testTheUpperNoteCannotBeReached() {
        let geometry = model.classicGeometry
        var point = CGPoint.zero
        while point.y < geometry.size.height {
            point.x = 0
            while point.x < geometry.size.width {
                XCTAssertNotEqual(geometry.cellIndex(at: point), 12)
                point.x += 10
            }
            point.y += 10
        }
    }

    func testAChordOfFingersSoundsTogether() {
        for (id, index) in [0, 4, 7, 11].enumerated() { model.touchBegan(id: id, at: center(index)) }
        XCTAssertEqual(model.playingIndices, [0, 4, 7, 11])
        model.touchEnded(id: 1)
        XCTAssertEqual(model.playingIndices, [0, 7, 11], "lifting one finger stops only its note")
        for id in [0, 2, 3] { model.touchEnded(id: id) }
        XCTAssertFalse(model.anyPlaying)
    }

    func testASlidingFingerTakesItsNoteAlongAndLetsGoInTheWell() {
        model.touchBegan(id: 1, at: center(1))
        model.touchMoved(id: 1, to: center(2))
        XCTAssertEqual(model.playingIndices, [2])
        let well = model.classicGeometry.well
        model.touchMoved(id: 1, to: CGPoint(x: well.midX, y: well.midY))
        XCTAssertFalse(model.anyPlaying)
        model.touchEnded(id: 1)
    }

    func testToggleModeLatchesNotes() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.touchBegan(id: 1, at: center(5))
        model.touchEnded(id: 1)
        model.touchBegan(id: 2, at: center(11))
        model.touchEnded(id: 2)
        XCTAssertEqual(model.playingIndices, [5, 11])
        model.touchBegan(id: 3, at: center(5))
        model.touchEnded(id: 3)
        XCTAssertEqual(model.playingIndices, [11])
    }

    func testTheWellIsNotTheInstruments() {
        let well = model.classicGeometry.well
        model.touchBegan(id: 1, at: CGPoint(x: well.midX, y: well.midY))
        XCTAssertFalse(model.anyPlaying)
        XCTAssertFalse(model.isHighRange, "the range control takes its own touches")
        model.touchEnded(id: 1)
    }

    func testChangingRangeSilencesTheGrid() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.touchBegan(id: 1, at: center(0))
        XCTAssertTrue(model.anyPlaying)
        model.selectRange(high: true)
        XCTAssertFalse(model.anyPlaying)
        XCTAssertEqual("\(notes[11].friendlyName!)\(notes[11].octave)", "E5", "the grid runs F to E")
    }

    func testTheRangeControlIgnoresAChoiceWhileAFingerHoldsANote() {
        model.touchBegan(id: 1, at: center(0))
        model.pickRange(high: true)
        XCTAssertFalse(model.isHighRange, "a second finger cannot yank the range from under a held note")
        XCTAssertTrue(notes[0].isPlaying)
        model.touchEnded(id: 1)
        model.pickRange(high: true)
        XCTAssertTrue(model.isHighRange)
    }

    func testChangingFaceSilencesTheInstrument() {
        DPSettingsModel.sharedInstance.toggleNotes = true
        model.touchBegan(id: 1, at: center(3))
        XCTAssertTrue(model.anyPlaying)
        DPSettingsModel.sharedInstance.classicPitchPipe = false
        XCTAssertFalse(model.isClassic)
        XCTAssertFalse(model.anyPlaying)
    }

    func testTheSettingIsKeptOnThisDevice() {
        let settings = DPSettingsModel()
        var announced = 0
        let observer = NotificationCenter.default.addObserver(forName: .settingsChanged, object: settings, queue: nil) { _ in
            announced += 1
        }
        defer { NotificationCenter.default.removeObserver(observer) }
        settings.classicPitchPipe = false
        XCTAssertFalse(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ClassicPitchPipe"))
        settings.classicPitchPipe = true
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ClassicPitchPipe"))
        XCTAssertTrue(DPSettingsModel().classicPitchPipe, "stored, so a new launch reads it")
        XCTAssertEqual(announced, 2)
        // What the account carries leaves it alone.
        settings.applyRemote(wakeLock: true, toggleNotes: true, referencePitch: 440)
        XCTAssertTrue(settings.classicPitchPipe)
    }
}

/// The classic face as mounted in the app.
@MainActor
final class ClassicPitchPipeScreenTests: PitchPerfectTestCase {
    private func rangeControl(_ app: HostedApp) -> UISegmentedControl? {
        app.descendants(of: UISegmentedControl.self, in: app.window).first { $0.window != nil }
    }

    func testTheSettingSwapsTheFaceWhileItShows() throws {
        let app = try launch()
        app.show(tab: 0)
        XCTAssertTrue(app.ui.exists(label: "Octave range C to C"), "the radial face by default")
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        app.ui.wait { !app.ui.exists(label: "Octave range C to C") && self.rangeControl(app) != nil }
        for label in ["C, octave 4", "C sharp, D flat, octave 4", "B, octave 4"] {
            let cell = try XCTUnwrap(app.ui.element(label: label), label)
            XCTAssertTrue(cell.accessibilityTraits.contains(.button), label)
        }
        XCTAssertFalse(app.ui.exists(label: "C, octave 5"), "one octave, C to B")
        let control = try XCTUnwrap(rangeControl(app))
        XCTAssertEqual(control.titleForSegment(at: 0), "C to B")
        XCTAssertEqual(control.titleForSegment(at: 1), "F to E")
        DPSettingsModel.sharedInstance.classicPitchPipe = false
        app.ui.wait { app.ui.exists(label: "Octave range C to C") && self.rangeControl(app) == nil }
    }

    func testTheRangeControlSwitchesOctavesAndSilencesTheGrid() throws {
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.show(tab: 0)
        app.ui.tap(label: "C, octave 4")
        XCTAssertTrue(DPNote.c4().isPlaying)
        let control = try XCTUnwrap(rangeControl(app))
        XCTAssertEqual(control.selectedSegmentIndex, 0)
        control.selectedSegmentIndex = 1
        control.sendActions(for: .valueChanged)
        ScreenCatalog.settle(0.2)
        XCTAssertTrue(app.models.pitchPipe.isHighRange)
        XCTAssertFalse(DPNote.c4().isPlaying)
        XCTAssertTrue(app.ui.exists(label: "F, octave 4"))
        XCTAssertTrue(app.ui.exists(label: "E, octave 5"))
        XCTAssertFalse(app.ui.exists(label: "F, octave 5"), "F to E")
    }

    func testSettingsOffersTheClassicPitchPipe() {
        let model = SettingsModel()
        XCTAssertFalse(model.classicPitchPipe)
        model.setClassicPitchPipe(true)
        XCTAssertTrue(DPSettingsModel.sharedInstance.classicPitchPipe)
        XCTAssertTrue(model.classicPitchPipe)
        DPSettingsModel.sharedInstance.classicPitchPipe = false
        XCTAssertFalse(model.classicPitchPipe, "a change from elsewhere shows at once")
    }
}

/// Catalog captures of the classic face (see docs/ios-swiftui.md), one layout per test.
@MainActor
final class ClassicPitchPipeCatalogTests: PitchPerfectTestCase {
    override func setUp() async throws {
        try await super.setUp()
        DPSettingsModel.sharedInstance.classicPitchPipe = true
        PitchInstrumentView.breathingFrozen = true
    }

    override func tearDown() async throws {
        PitchInstrumentView.breathingFrozen = false
        try await super.tearDown()
    }

    private var device: String { UIDevice.current.userInterfaceIdiom == .pad ? "ipad" : "phone" }

    private func capture(_ name: String, _ app: HostedApp) {
        ScreenCatalog.capture("classic-\(device)-\(name)", window: app.window, settle: 0.5)
    }

    func testAtRest() throws {
        let app = try launch()
        app.show(tab: 0)
        capture("rest", app)
    }

    func testOneNote() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.pressInstrument(["A, octave 4"], toggle: true)
        capture("one-note", app)
    }

    func testBarbershopChordInTheDark() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch(.dark)
        app.pressInstrument(["C, octave 4", "E, octave 4", "G, octave 4", "A sharp, B flat, octave 4"], toggle: true)
        capture("barbershop-dark", app)
    }

    func testTurned() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        let app = try launch()
        app.show(tab: 0)
        // The hosted app can't rotate; a landscape phone is the window at its size,
        // with the compact height that shrinks the bars.
        let size = app.window.bounds.size
        app.window.traitOverrides.verticalSizeClass = .compact
        app.window.frame = CGRect(origin: .zero, size: CGSize(width: max(size.width, size.height),
                                                               height: min(size.width, size.height)))
        ScreenCatalog.settle(0.5)
        if UIDevice.current.userInterfaceIdiom == .phone {
            // A phone's well is short turned: the readout goes beside the range control.
            // (An iPad's stays tall enough to stack them.)
            XCTAssertFalse(app.models.pitchPipe.classicGeometry.stacked)
        }
        app.ui.tap(label: "A, octave 4")
        capture("landscape", app)
    }
}
