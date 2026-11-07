//
//  PitchPerfectFoldTests.swift
//  pitchperfectTests
//
//  Where the pitch pipe goes when an iPhone Duo is partly folded: the radial
//  face splits into a raised display and a ring, the classic grid opens at the
//  fold, and nothing a finger or the eye needs ever straddles the crease. The
//  folds are synthetic (the simulator reports none to a hosted test) but
//  measured: the face's size on the inner display, between its bars and above
//  the banner, and the division region iOS 27.1 reports there with the hinge
//  at 130°: a 40-point frame that holds the crease and the 20 points either
//  side iOS asks interactive content to keep from it.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

final class FoldGeometryTests: XCTestCase {
    /// The inner display held upright, folded across its middle (tabletop).
    private let upright = CGSize(width: 669, height: 703)
    private var acrossFold: CGRect { CGRect(x: 0, y: 373.5, width: upright.width, height: 40) }
    /// Held wide, folded down its middle (book), the side bar on the right.
    private let sideways = CGSize(width: 867, height: 483)
    private var downFold: CGRect { CGRect(x: 455.5, y: 0, width: 40, height: sideways.height) }

    private func ring(_ geometry: InstrumentGeometry) -> CGRect {
        geometry.cellCenters.indices.map { geometry.cellRect($0).insetBy(dx: -geometry.cellRadius * 0.15,
                                                                         dy: -geometry.cellRadius * 0.15) }
            .reduce(CGRect.null) { $0.union($1) }
    }

    // MARK: Radial face

    func testWithoutAFoldTheFaceIsAsItWas() {
        let size = CGSize(width: 402, height: 640)
        let plain = InstrumentGeometry(size: size, count: 13)
        XCTAssertEqual(plain, InstrumentGeometry(size: size, count: 13, fold: nil))
        XCTAssertEqual(plain.posture, .flat)
        XCTAssertNil(plain.display)
        XCTAssertNil(plain.nameplateArea)
        XCTAssertEqual(plain.faceArea, CGRect(origin: .zero, size: size))
        XCTAssertEqual(plain.faceCenter, CGPoint(x: 201, y: 640 * 0.44))
        XCTAssertEqual(plain.ringRadius, min(402, 640 * 0.82) * 0.365, accuracy: 0.0001)
        XCTAssertEqual(plain.rangeLowRect.width, plain.ringRadius * 0.72, accuracy: 0.0001)
    }

    func testAFoldThatMissesTheFaceChangesNothing() {
        let size = CGSize(width: 402, height: 640)
        let plain = InstrumentGeometry(size: size, count: 13)
        for fold in [CGRect(x: 0, y: 700, width: 402, height: 20), CGRect(x: 40, y: 300, width: 100, height: 20)] {
            XCTAssertEqual(InstrumentGeometry(size: size, count: 13, fold: fold), plain, "\(fold)")
        }
    }

    func testTabletopRaisesTheDisplayAboveTheFoldAndSetsTheRingBelowIt() throws {
        let geometry = InstrumentGeometry(size: upright, count: 13, fold: acrossFold)
        XCTAssertEqual(geometry.posture, .tabletop)
        let display = try XCTUnwrap(geometry.display)
        let nameplate = try XCTUnwrap(geometry.nameplateArea)
        XCTAssertLessThanOrEqual(display.maxY, nameplate.minY, "the nameplate is engraved under the display")
        XCTAssertLessThanOrEqual(nameplate.maxY, acrossFold.minY, "both stay above the crease")
        XCTAssertGreaterThanOrEqual(ring(geometry).minY, acrossFold.maxY, "the ring is wholly below it")
        XCTAssertLessThanOrEqual(ring(geometry).maxY, upright.height)
        XCTAssertEqual(geometry.faceCenter.x, upright.width / 2, accuracy: 0.001)
        XCTAssertEqual(display.midX, geometry.faceCenter.x, accuracy: 0.001, "the display is centred over the ring")
    }

    func testTheDisplayIsSizedFromTheRingSoTheCellsStayTheLargestPart() throws {
        for geometry in [InstrumentGeometry(size: upright, count: 13, fold: acrossFold),
                         InstrumentGeometry(size: sideways, count: 13, fold: downFold)] {
            let display = try XCTUnwrap(geometry.display)
            let glass = 2 * (geometry.ringRadius + geometry.cellRadius)
            XCTAssertLessThanOrEqual(display.width, glass * 1.2 + 0.001, "\(geometry.posture)")
            XCTAssertEqual(display.height, display.width * 0.45, accuracy: 0.001, "\(geometry.posture)")
        }
        // Tabletop has the room, so the window is the full 1.2 × the ring there.
        let tabletop = InstrumentGeometry(size: upright, count: 13, fold: acrossFold)
        let glass = 2 * (tabletop.ringRadius + tabletop.cellRadius)
        XCTAssertEqual(try XCTUnwrap(tabletop.display).width, glass * 1.2, accuracy: 0.001)
    }

    func testBookPutsTheDisplayOnTheLeadingPageAndTheRingOnTheOther() throws {
        let geometry = InstrumentGeometry(size: sideways, count: 13, fold: downFold)
        XCTAssertEqual(geometry.posture, .book)
        let display = try XCTUnwrap(geometry.display)
        let nameplate = try XCTUnwrap(geometry.nameplateArea)
        XCTAssertLessThanOrEqual(display.maxX, downFold.minX)
        XCTAssertLessThanOrEqual(nameplate.maxX, downFold.minX)
        XCTAssertGreaterThanOrEqual(ring(geometry).minX, downFold.maxX)
        XCTAssertLessThanOrEqual(ring(geometry).maxX, sideways.width)
        XCTAssertEqual(display.midY + (nameplate.maxY - display.maxY) / 2, geometry.faceCenter.y, accuracy: 1,
                       "the display sits level with the ring")
    }

    func testRightToLeftTurnsTheBookRoundSoTheDisplayStaysOnTheLeadingPage() throws {
        let geometry = InstrumentGeometry(size: sideways, count: 13, fold: downFold, displayOnLeft: false)
        XCTAssertEqual(geometry.posture, .book)
        XCTAssertGreaterThanOrEqual(try XCTUnwrap(geometry.display).minX, downFold.maxX)
        XCTAssertLessThanOrEqual(ring(geometry).maxX, downFold.minX)
        // A fold across has no leading side: the display is always the upper half.
        let tabletop = InstrumentGeometry(size: upright, count: 13, fold: acrossFold, displayOnLeft: false)
        XCTAssertLessThanOrEqual(try XCTUnwrap(tabletop.display).maxY, acrossFold.minY)
    }

    func testFoldedCellsAreFullTouchTargetsAndTheHoleHoldsOnlyTheRangeSelector() {
        for geometry in [InstrumentGeometry(size: upright, count: 13, fold: acrossFold),
                         InstrumentGeometry(size: sideways, count: 13, fold: downFold)] {
            XCTAssertGreaterThanOrEqual(geometry.cellRadius * 2, 44, "\(geometry.posture)")
            // The selector fills the hole's middle, clear of every cell's touch target.
            let selector = geometry.rangeLowRect.union(geometry.rangeHighRect)
            XCTAssertEqual(selector.midX, geometry.faceCenter.x, accuracy: 0.001)
            XCTAssertEqual(selector.midY, geometry.faceCenter.y, accuracy: 0.001)
            let corner = hypot(selector.width / 2, selector.height / 2)
            XCTAssertLessThan(corner, geometry.ringRadius - geometry.cellRadius * 1.15)
            XCTAssertGreaterThan(geometry.rangeLowRect.height, geometry.ringRadius * 0.145,
                                 "with the readout raised, the selector's rows grow")
            // Each row takes a full 44-point target, grown away from the other, still
            // clear of the cells' own.
            for target in [geometry.rangeLowTarget, geometry.rangeHighTarget] {
                XCTAssertGreaterThanOrEqual(target.height, 44, "\(geometry.posture)")
                let far = hypot(target.width / 2, max(abs(target.minY - geometry.faceCenter.y),
                                                      abs(target.maxY - geometry.faceCenter.y)))
                XCTAssertLessThan(far, geometry.ringRadius - geometry.cellRadius * 1.15, "\(geometry.posture)")
            }
            XCTAssertEqual(geometry.rangeLowTarget.maxY, geometry.rangeHighTarget.minY, "the two meet between the rows")
            XCTAssertEqual(geometry.range(at: CGPoint(x: selector.midX, y: geometry.rangeLowTarget.minY + 1)), false)
            XCTAssertEqual(geometry.range(at: CGPoint(x: selector.midX, y: geometry.rangeHighTarget.maxY - 1)), true)
        }
        // Flat, the phone's face is untouched: its rows are their own targets.
        let flat = InstrumentGeometry(size: CGSize(width: 402, height: 640), count: 13)
        XCTAssertEqual(flat.rangeLowTarget, flat.rangeLowRect)
        XCTAssertEqual(flat.rangeHighTarget, flat.rangeHighRect)
    }

    func testTabletopSeatsTheDisplayJustAboveTheCrease() throws {
        let geometry = InstrumentGeometry(size: upright, count: 13, fold: acrossFold)
        let nameplate = try XCTUnwrap(geometry.nameplateArea)
        let gap = acrossFold.minY - nameplate.maxY
        XCTAssertGreaterThan(gap, 0)
        XCTAssertLessThanOrEqual(gap, 30, "the readout stays with the keys below the fold, not up in the sky")
    }

    func testAFoldThatWouldShrinkTheCellsUnderATouchTargetKeepsTheFaceWhole() {
        // A narrow window straddling the fold: the ring's 160-point page would leave
        // 27-point cells, so the whole face takes the 200-point page instead.
        let size = CGSize(width: 440, height: 600)
        let geometry = InstrumentGeometry(size: size, count: 13, fold: CGRect(x: 200, y: 0, width: 80, height: 600))
        XCTAssertEqual(geometry.posture, .flat)
        XCTAssertNil(geometry.display)
        XCTAssertEqual(geometry.faceArea, CGRect(x: 0, y: 0, width: 200, height: 600))
    }

    func testFoldedVoiceOverFramesAreTheTouchCirclesSoNoneIsUnder44Points() {
        // 240-point pages: 40-point cells inside 46-point touch circles.
        let close = InstrumentGeometry(size: CGSize(width: 520, height: 600), count: 13,
                                       fold: CGRect(x: 240, y: 0, width: 40, height: 600))
        XCTAssertEqual(close.posture, .book)
        XCTAssertLessThan(close.cellRadius * 2, 44)
        for geometry in [close, InstrumentGeometry(size: upright, count: 13, fold: acrossFold),
                         InstrumentGeometry(size: sideways, count: 13, fold: downFold)] {
            for index in geometry.cellCenters.indices {
                let frame = geometry.accessibilityRect(index)
                XCTAssertGreaterThanOrEqual(min(frame.width, frame.height), 44, "\(geometry.posture) \(index)")
                XCTAssertEqual(frame.midX, geometry.cellCenters[index].x, accuracy: 0.001)
                XCTAssertEqual(frame.midY, geometry.cellCenters[index].y, accuracy: 0.001)
            }
        }
        // Flat, the phone's face is untouched.
        let flat = InstrumentGeometry(size: CGSize(width: 402, height: 640), count: 13)
        XCTAssertEqual(flat.accessibilityRect(3), flat.cellRect(3))
    }

    func testTouchesFollowTheFoldedFace() throws {
        for geometry in [InstrumentGeometry(size: upright, count: 13, fold: acrossFold),
                         InstrumentGeometry(size: sideways, count: 13, fold: downFold)] {
            for index in geometry.cellCenters.indices {
                XCTAssertEqual(geometry.cellIndex(at: geometry.cellCenters[index]), index)
            }
            XCTAssertEqual(geometry.range(at: CGPoint(x: geometry.rangeLowRect.midX, y: geometry.rangeLowRect.midY)), false)
            XCTAssertEqual(geometry.range(at: CGPoint(x: geometry.rangeHighRect.midX, y: geometry.rangeHighRect.midY)), true)
            // The display is to read, not to play.
            let display = try XCTUnwrap(geometry.display)
            let middle = CGPoint(x: display.midX, y: display.midY)
            XCTAssertEqual(geometry.cellIndex(at: middle), -1)
            XCTAssertNil(geometry.range(at: middle))
        }
    }

    func testAFoldNearTheEdgeKeepsTheFaceWholeOnTheLargerSide() {
        // Beside another app on the inner display, the fold runs down the window's edge.
        let size = CGSize(width: 440, height: 600)
        let edge = CGRect(x: 420, y: 0, width: 24, height: 600)
        let geometry = InstrumentGeometry(size: size, count: 13, fold: edge)
        XCTAssertEqual(geometry.posture, .flat)
        XCTAssertNil(geometry.display)
        XCTAssertEqual(geometry.faceArea.maxX, edge.minX, accuracy: 0.001)
        XCTAssertLessThanOrEqual(ring(geometry).maxX, edge.minX)
        XCTAssertEqual(geometry.faceCenter.y, geometry.faceArea.minY + geometry.faceArea.height * 0.44, accuracy: 0.001)
    }

    // MARK: Classic grid

    func testWithoutAFoldTheGridIsAsItWas() {
        let size = CGSize(width: 402, height: 640)
        let plain = ClassicGeometry(size: size, count: 13)
        XCTAssertEqual(plain, ClassicGeometry(size: size, count: 13, fold: nil))
        XCTAssertEqual(plain.posture, .flat)
        XCTAssertEqual(plain.columns, ClassicGeometry.Span.quarters(from: 0, length: 402))
        XCTAssertEqual(plain.rows, ClassicGeometry.Span.quarters(from: 0, length: 640))
    }

    func testTabletopOpensTheGridBetweenItsMiddleRows() {
        let grid = ClassicGeometry(size: upright, count: 13, fold: acrossFold)
        XCTAssertEqual(grid.posture, .tabletop)
        XCTAssertEqual(grid.rows.first?.start, 0, "the outer margins stay")
        XCTAssertEqual(grid.rows.last?.end ?? 0, upright.height, accuracy: 0.001)
        XCTAssertEqual(grid.rows[1].end, acrossFold.minY, accuracy: 0.001)
        XCTAssertEqual(grid.rows[2].start, acrossFold.maxY, accuracy: 0.001)
        XCTAssertEqual(grid.columns, ClassicGeometry.Span.quarters(from: 0, length: upright.width))
        for (index, slot) in grid.slots.enumerated() {
            XCTAssertFalse(slot.intersects(acrossFold), "cell \(index) straddles the fold")
            XCTAssertEqual(grid.cellIndex(at: CGPoint(x: slot.midX, y: slot.midY)), index)
        }
        XCTAssertEqual(grid.cellIndex(at: CGPoint(x: 20, y: acrossFold.midY)), -1, "the crease plays nothing")
        XCTAssertTrue(grid.stacked)
        XCTAssertLessThanOrEqual(grid.readoutRect.maxY, acrossFold.minY, "the readout is above the fold")
        XCTAssertGreaterThanOrEqual(grid.selectorRect.minY, acrossFold.maxY, "the range control below it")
        XCTAssertEqual(grid.selectorRect.midX, grid.readoutRect.midX, accuracy: 0.001)
    }

    func testBookOpensTheGridBetweenItsMiddleColumns() {
        let grid = ClassicGeometry(size: sideways, count: 13, fold: downFold)
        XCTAssertEqual(grid.posture, .book)
        XCTAssertEqual(grid.columns.first?.start, 0)
        XCTAssertEqual(grid.columns.last?.end ?? 0, sideways.width, accuracy: 0.001)
        XCTAssertEqual(grid.columns[1].end, downFold.minX, accuracy: 0.001)
        XCTAssertEqual(grid.columns[2].start, downFold.maxX, accuracy: 0.001)
        for slot in grid.slots { XCTAssertFalse(slot.intersects(downFold)) }
        XCTAssertFalse(grid.stacked)
        XCTAssertLessThanOrEqual(grid.readoutRect.maxX, downFold.minX, "the readout left of the fold")
        XCTAssertGreaterThanOrEqual(grid.selectorRect.minX, downFold.maxX, "the range control right of it")
        XCTAssertEqual(grid.selectorRect.midY, grid.readoutRect.midY, accuracy: 0.001)
    }

    func testEveryClassicLabelIsSizedFromTheSmallerSideOfTheFold() {
        let offCentre = CGRect(x: 0, y: 300, width: upright.width, height: 24)
        let grid = ClassicGeometry(size: upright, count: 13, fold: offCentre)
        XCTAssertEqual(grid.posture, .tabletop)
        XCTAssertLessThan(grid.rows[0].length, grid.rows[3].length)
        XCTAssertEqual(grid.singleButton.height, grid.rows[0].length - 2 * ClassicGeometry.buttonInset.height,
                       accuracy: 0.001)
    }

    func testAFoldNearTheEdgeKeepsTheGridWholeOnTheLargerSide() {
        let size = CGSize(width: 440, height: 600)
        let edge = CGRect(x: 420, y: 0, width: 24, height: 600)
        let grid = ClassicGeometry(size: size, count: 13, fold: edge)
        XCTAssertEqual(grid.posture, .flat)
        XCTAssertEqual(grid.columns, ClassicGeometry.Span.quarters(from: 0, length: edge.minX))
        for slot in grid.slots { XCTAssertLessThanOrEqual(slot.maxX, edge.minX + 0.001) }
    }
}

/// The whole app with a fold stood in across (and then down) the window: every
/// cell and range position VoiceOver reaches lies clear of the crease, on
/// both faces.
@MainActor
final class FoldedPitchPipeTests: PitchPerfectTestCase {
    override func tearDown() async throws {
        await MainActor.run { Fold.simulated = nil }
        try await super.tearDown()
    }

    /// How far SwiftUI's global space, which `Fold.simulated` is given, sits from the
    /// window's inside the tab's navigation stack: measured on the face itself.
    private func globalOffset() throws -> CGPoint {
        var seen: [CGRect] = []
        Fold.simulated = { frame in seen.append(frame); return nil }
        defer { Fold.simulated = nil }
        let app = try launch()
        defer { app.tearDown() }
        app.show(tab: 0)
        ScreenCatalog.settle(0.3)
        let face = try XCTUnwrap(app.descendants(of: MultiTouchSurface.Surface.self, in: app.window)
            .first { $0.window != nil })
        let global = try XCTUnwrap(seen.last { abs($0.width - face.bounds.width) < 0.5
            && abs($0.height - face.bounds.height) < 0.5 }, "The face's frame reached the simulation")
        let origin = face.convert(CGPoint.zero, to: nil)
        return CGPoint(x: origin.x - global.minX, y: origin.y - global.minY)
    }

    func testEveryControlVoiceOverReachesIsClearOfTheCrease() throws {
        let bounds = try XCTUnwrap(ScreenCatalog.scene).coordinateSpace.bounds
        let folds = [CGRect(x: 0, y: bounds.midY - 12, width: bounds.width, height: 24),
                     CGRect(x: bounds.midX - 12, y: 0, width: 24, height: bounds.height)]
        // The folds are in window coordinates, where VoiceOver's frames are.
        let offset = try globalOffset()
        for classic in [false, true] {
            for fold in folds {
                DPSettingsModel.sharedInstance.classicPitchPipe = classic
                Fold.simulated = { _ in fold.offsetBy(dx: -offset.x, dy: -offset.y) }
                let app = try launch()
                app.show(tab: 0)
                let model = app.models.pitchPipe
                XCTAssertEqual(model.isClassic, classic)
                let cells = classic ? min(model.notes.count, ClassicGeometry.cellCount) : model.notes.count
                let labels = (0..<cells).map { model.spokenName(at: $0) }
                    + (classic ? [] : ["Octave range C to C", "Octave range F to F"])
                for label in labels {
                    let element = try XCTUnwrap(app.ui.element(label: label), label)
                    let frame = element.accessibilityFrame
                    XCTAssertFalse(frame.intersects(fold), "\(label) straddles the fold \(fold) at \(frame)")
                }
                app.tearDown()
            }
        }
    }

    /// Folding to tabletop moves the banner to the top of the upper half and back: the
    /// same banner view each way, so the ad it loaded stays, rather than a new one
    /// being requested on every fold.
    func testTheBannerMovesWithoutBeingRebuilt() throws {
        DPSettingsModel.sharedInstance.classicPitchPipe = false
        let app = try launch()
        app.show(tab: 0)
        ScreenCatalog.settle(0.3)
        func banner() -> BannerHostView? {
            app.descendants(of: BannerHostView.self, in: app.window).first { $0.window != nil }
        }
        func top(_ view: UIView?) -> CGFloat { view.map { $0.convert($0.bounds, to: nil).minY } ?? .nan }
        func face() -> UIView? {
            app.descendants(of: MultiTouchSurface.Surface.self, in: app.window).first { $0.window != nil }
        }
        func relayout() {
            let frame = app.window.frame
            app.window.frame = frame.insetBy(dx: 0, dy: 1)
            ScreenCatalog.settle(0.3)
            app.window.frame = frame
            ScreenCatalog.settle(0.3)
        }
        let flat = try XCTUnwrap(banner())
        XCTAssertGreaterThan(top(flat), top(face()), "Flat, the banner docks below the face")

        Fold.simulated = { frame in CGRect(x: frame.minX, y: frame.midY - 20, width: frame.width, height: 40) }
        relayout()
        XCTAssertTrue(banner() === flat, "Folded, it is the same banner")
        XCTAssertLessThan(top(banner()), top(face()), "on top of the upper half")

        Fold.simulated = nil
        relayout()
        XCTAssertTrue(banner() === flat, "and still the same one laid flat again")
        XCTAssertGreaterThan(top(banner()), top(face()))
        app.tearDown()
    }

    /// Right to left, both faces are still drawn and touched in fixed coordinates,
    /// so every cell VoiceOver reaches has to sit on the drawn cell, not its mirror.
    func testRightToLeftVoiceOverFramesSitOnTheDrawnCells() throws {
        // On this app's own host, not the shared scene, so no later test inherits it.
        for classic in [false, true] {
            DPSettingsModel.sharedInstance.classicPitchPipe = classic
            let app = try launch()
            app.host.traitOverrides.layoutDirection = .rightToLeft
            app.show(tab: 0)
            ScreenCatalog.settle(0.3)
            XCTAssertEqual(app.host.view.effectiveUserInterfaceLayoutDirection, .rightToLeft,
                           "The face is laid out right to left, or this proves nothing")
            let model = app.models.pitchPipe
            let surface = try XCTUnwrap(app.descendants(of: MultiTouchSurface.Surface.self, in: app.window)
                .first { $0.window != nil })
            let origin = surface.convert(CGPoint.zero, to: nil)
            let rects = classic ? model.classicGeometry.slots
                                : model.geometry.cellCenters.indices.map(model.geometry.accessibilityRect)
            XCTAssertFalse(rects.isEmpty)
            for (index, rect) in rects.enumerated() where index < model.notes.count {
                let label = model.spokenName(at: index)
                let frame = try XCTUnwrap(app.ui.element(label: label), label).accessibilityFrame
                let expected = rect.offsetBy(dx: origin.x, dy: origin.y)
                XCTAssertEqual(frame.midX, expected.midX, accuracy: 1, "\(label) (classic \(classic))")
                XCTAssertEqual(frame.midY, expected.midY, accuracy: 1, "\(label) (classic \(classic))")
            }
            app.tearDown()
        }
    }
}

/// A prompt kept off the crease takes the leading page of a book fold, which
/// right to left is the right-hand one.
@MainActor
final class CentredOffTheFoldTests: XCTestCase {
    override func tearDown() async throws {
        await MainActor.run { Fold.simulated = nil }
        try await super.tearDown()
    }

    func testThePromptTakesTheLeadingPageInEitherDirection() throws {
        for direction in [LayoutDirection.leftToRight, .rightToLeft] {
            let window = ScreenCatalog.makeWindow()
            let bounds = window.bounds
            let fold = CGRect(x: bounds.midX - 20, y: 0, width: 40, height: bounds.height)
            Fold.simulated = { _ in fold }
            window.rootViewController = UIHostingController(rootView:
                Color.red
                    .frame(height: 60)
                    .accessibilityElement()
                    .accessibilityLabel("Prompt")
                    .centredOffTheFold()
                    .environment(\.layoutDirection, direction))
            window.isHidden = false
            ScreenCatalog.settle(0.3)
            let frame = try XCTUnwrap(UIDriver(window).element(label: "Prompt")).accessibilityFrame
            if direction == .leftToRight {
                XCTAssertEqual(frame.minX, 0, accuracy: 1, "\(frame)")
                XCTAssertEqual(frame.maxX, fold.minX, accuracy: 1, "\(frame)")
            } else {
                XCTAssertEqual(frame.minX, fold.maxX, accuracy: 1, "\(frame)")
                XCTAssertEqual(frame.maxX, bounds.maxX, accuracy: 1, "\(frame)")
            }
            window.isHidden = true
            window.rootViewController = nil
        }
    }
}
