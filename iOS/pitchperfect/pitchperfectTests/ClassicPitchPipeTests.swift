import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// Where the classic grid puts its cells, its readout and its range control.
final class ClassicGeometryTests: XCTestCase {
    /// An iPhone 17's pitch pipe, between its bars.
    private let phone = ClassicGeometry(size: CGSize(width: 402, height: 640), count: 13)

    private func slot(_ geometry: ClassicGeometry, _ index: Int) -> (column: Int, row: Int, columns: Int) {
        let rect = geometry.slots[index]
        let column = geometry.size.width / 4
        let row = geometry.size.height / 4
        return (Int((rect.minX / column).rounded()), Int((rect.minY / row).rounded()), Int((rect.width / column).rounded()))
    }

    func testTwelveCellsRunClockwiseRoundTheEdgeAndTheUpperNoteSpansTheMiddle() {
        XCTAssertEqual(phone.slots.count, 13)
        let expected: [(Int, Int, Int)] = [
            (0, 0, 1), (1, 0, 1), (2, 0, 1), (3, 0, 1), // C C♯ D D♯ across the top
            (3, 1, 1), (3, 2, 1), // E F down the right
            (3, 3, 1), (2, 3, 1), (1, 3, 1), (0, 3, 1), // F♯ G G♯ A back along the bottom
            (0, 2, 1), (0, 1, 1), // A♯ B up the left
            (1, 1, 2), // the upper C, across the top of the middle
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
        let wide = phone.slots[12]
        XCTAssertEqual(phone.cellIndex(at: CGPoint(x: wide.minX + 1, y: wide.maxY - 1)), 12)
        XCTAssertEqual(phone.cellIndex(at: CGPoint(x: -1, y: 10)), -1)
    }

    func testTheWellIsNoCellAndTheRangeControlTakesItsOwnTouches() {
        let well = phone.well
        XCTAssertEqual(phone.cellIndex(at: CGPoint(x: well.midX, y: well.midY)), -1)
        XCTAssertNil(phone.range(at: CGPoint(x: phone.selectorRect.midX, y: phone.selectorRect.midY)))
    }

    func testATallWellStacksTheReadoutOverTheRangeControl() {
        XCTAssertTrue(phone.stacked)
        XCTAssertEqual(phone.selectorRect.midX, phone.well.midX, accuracy: 0.001)
        XCTAssertLessThanOrEqual(phone.selectorRect.maxY, phone.well.maxY)
        XCTAssertGreaterThan(phone.selectorRect.minY, phone.well.maxY - ClassicGeometry.selectorBand)
        XCTAssertEqual(phone.readoutRect.minY, phone.well.minY)
        XCTAssertEqual(phone.readoutRect.maxY, phone.well.maxY - ClassicGeometry.selectorBand - ClassicGeometry.gap, accuracy: 0.001)
        XCTAssertEqual(phone.readoutRect.width, phone.well.width)
    }

    func testAShortWellPutsTheRangeControlBesideTheReadout() {
        // A phone turned on its side.
        let turned = ClassicGeometry(size: CGSize(width: 874, height: 300), count: 13)
        XCTAssertFalse(turned.stacked)
        XCTAssertEqual(turned.selectorRect.maxX, turned.well.maxX, accuracy: 0.001)
        XCTAssertEqual(turned.selectorRect.midY, turned.well.midY, accuracy: 0.001)
        XCTAssertEqual(turned.readoutRect.minX, turned.well.minX)
        XCTAssertEqual(turned.readoutRect.maxX, turned.selectorRect.minX - ClassicGeometry.gap, accuracy: 0.001)
        XCTAssertEqual(turned.readoutRect.height, turned.well.height)
    }

    func testATurnedPhonesReadoutIsTooShortForTwoLines() {
        // An iPhone 17 on its side, between its bars and the tab bar.
        let turned = ClassicGeometry(size: CGSize(width: 750, height: 200), count: 13)
        XCTAssertFalse(turned.stacked)
        XCTAssertLessThan(turned.readoutRect.height, ClassicGeometry.minReadout, "so the readout reads as one line")
        XCTAssertGreaterThanOrEqual(phone.readoutRect.height, ClassicGeometry.minReadout, "portrait keeps two")
    }

    func testAWideRangeControlTakesAtMostHalfAShortWell() {
        let turned = ClassicGeometry(size: CGSize(width: 874, height: 300), count: 13,
                                     selectorSize: CGSize(width: 900, height: 32))
        XCTAssertEqual(turned.selectorRect.width, turned.well.width / 2, accuracy: 0.001)
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
        XCTAssertEqual(notes.count, 13, "the whole inclusive octave")
    }

    func testEveryCellSoundsWhileHeldTheUpperNoteIncluded() {
        for index in notes.indices {
            model.touchBegan(id: 1, at: center(index))
            XCTAssertEqual(model.playingIndices, [index], "cell \(index)")
            model.touchEnded(id: 1)
            XCTAssertFalse(model.anyPlaying, "cell \(index)")
        }
        XCTAssertEqual(model.spokenName(at: 12), "C, octave 5")
    }

    func testAChordOfFingersSoundsTogether() {
        for (id, index) in [0, 4, 7, 12].enumerated() { model.touchBegan(id: id, at: center(index)) }
        XCTAssertEqual(model.playingIndices, [0, 4, 7, 12])
        model.touchEnded(id: 1)
        XCTAssertEqual(model.playingIndices, [0, 7, 12], "lifting one finger stops only its note")
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
        model.touchBegan(id: 2, at: center(12))
        model.touchEnded(id: 2)
        XCTAssertEqual(model.playingIndices, [5, 12])
        model.touchBegan(id: 3, at: center(5))
        model.touchEnded(id: 3)
        XCTAssertEqual(model.playingIndices, [12])
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
        XCTAssertEqual(notes.last.map { "\($0.friendlyName!)\($0.octave)" }, "F5")
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
        let upper = try XCTUnwrap(app.ui.element(label: "C, octave 5"))
        let lower = try XCTUnwrap(app.ui.element(label: "C, octave 4"))
        XCTAssertTrue(upper.accessibilityTraits.contains(.button))
        XCTAssertEqual(upper.accessibilityFrame.width, lower.accessibilityFrame.width * 2, accuracy: 1,
                       "the upper note spans the middle two columns")
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
        XCTAssertTrue(app.ui.exists(label: "F, octave 5"))
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
