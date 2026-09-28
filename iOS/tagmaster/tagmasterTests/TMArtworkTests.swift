//
//  TMArtworkTests.swift
//  tagmasterTests
//
//  The barber pole, the quartet staff and the watermark, drawn in SwiftUI from
//  the shared vector artwork. Replaces the UIKit layer tests of the views they
//  took over from.
//

import SwiftUI
import XCTest
@testable import tagmaster

@MainActor
final class TMArtworkTests: XCTestCase {
    private func render(_ view: some View, size: CGSize, dark: Bool = false) -> UIImage {
        let renderer = ImageRenderer(content: view
            .frame(width: size.width, height: size.height)
            .environment(\.colorScheme, dark ? .dark : .light))
        renderer.scale = 2
        return renderer.uiImage ?? UIImage()
    }

    private func pixels(_ image: UIImage) -> Data {
        image.cgImage?.dataProvider?.data as Data? ?? Data()
    }

    // MARK: - Motion

    func testArtworkMovesOnlyWhenAskedInFrontAndWithoutReduceMotion() {
        XCTAssertTrue(TMMotion.moves(animating: true, reduceMotion: false, scenePhase: .active))
        XCTAssertFalse(TMMotion.moves(animating: false, reduceMotion: false, scenePhase: .active))
        XCTAssertFalse(TMMotion.moves(animating: true, reduceMotion: true, scenePhase: .active))
        XCTAssertFalse(TMMotion.moves(animating: true, reduceMotion: false, scenePhase: .inactive))
        XCTAssertFalse(TMMotion.moves(animating: true, reduceMotion: false, scenePhase: .background))
    }

    func testTheStripesTravelOneStepPerTurnAndRestAtZero() {
        let duration = TimeInterval(TMLoaderDurationSeconds)
        let full = TMLoaderStripeStep * TMLoaderPhaseMultiplier
        XCTAssertEqual(TMBarberPole.phase(elapsed: 0, moving: true), 0)
        XCTAssertEqual(TMBarberPole.phase(elapsed: duration / 2, moving: true), full / 2, accuracy: 0.001)
        XCTAssertEqual(TMBarberPole.phase(elapsed: duration * 1.25, moving: true), full / 4, accuracy: 0.001,
                       "Each turn starts where the last one began")
        XCTAssertEqual(TMBarberPole.phase(elapsed: duration / 2, moving: false), 0, "Still, the pole rests")
    }

    // MARK: - Drawing

    func testThePoleKeepsItsTwoSizes() {
        let compact = UIHostingController(rootView: TMBarberPole(compact: true, animating: false))
        let compactSize = compact.sizeThatFits(in: CGSize(width: 300, height: 300))
        XCTAssertEqual(compactSize.width, TMLoaderCompactWidth, accuracy: 0.5)
        XCTAssertEqual(compactSize.height, TMLoaderCompactHeight, accuracy: 0.5)
        let full = UIHostingController(rootView: TMBarberPole(compact: false, animating: false))
        let size = full.sizeThatFits(in: CGSize(width: 300, height: 300))
        XCTAssertEqual(size.height, 68)
        XCTAssertGreaterThanOrEqual(size.width, TMLoaderArtworkWidth)
    }

    func testTheStripesMoveAndTheMetalFollowsTheSurface() {
        let size = CGSize(width: 34, height: 68)
        func pole(phase: CGFloat, metalDark: Bool) -> UIImage {
            render(Canvas { context, canvasSize in
                TMBarberPole.draw(in: &context, size: canvasSize, compact: false, metalDark: metalDark, phase: phase)
            }, size: size)
        }
        let rest = pixels(pole(phase: 0, metalDark: false))
        XCTAssertFalse(rest.isEmpty)
        XCTAssertNotEqual(rest, pixels(pole(phase: TMLoaderStripeStep / 2, metalDark: false)), "The stripes turn")
        XCTAssertNotEqual(rest, pixels(pole(phase: 0, metalDark: true)), "Dark surfaces get the light metal")
        XCTAssertEqual(rest, pixels(pole(phase: TMLoaderStripeStep * TMLoaderPhaseMultiplier, metalDark: false)),
                       "A whole turn lands back where it started")
    }

    func testTheQuartetDrawsItsChordAtRestInBothAppearances() {
        let light = pixels(render(TMQuartetStaff(animating: false), size: TMQuartetStaff.size))
        let dark = pixels(render(TMQuartetStaff(animating: false), size: TMQuartetStaff.size, dark: true))
        XCTAssertFalse(light.isEmpty)
        XCTAssertNotEqual(light, dark, "The staff and notes take the appearance's colours")
    }

    // MARK: - Watermark

    func testTheWatermarkSitsInTheOldCanvasProportions() {
        let rect = CGRect(x: 0, y: 0, width: 480, height: 800)
        let bounds = TMLogoShape().path(in: rect).boundingRect
        XCTAssertTrue(rect.insetBy(dx: -1, dy: -1).contains(bounds), "The pole fits its canvas")
        XCTAssertEqual(bounds.midX, rect.midX + 1.5, accuracy: 2, "Centred as the UIKit layer placed it")
        XCTAssertGreaterThan(bounds.height, rect.height * 0.9, "It fills the canvas's height")

        let wide = TMLogoShape().path(in: CGRect(x: 0, y: 0, width: 1200, height: 800)).boundingRect
        XCTAssertEqual(wide.height, bounds.height, accuracy: 1, "A wider screen does not stretch it")
        XCTAssertEqual(wide.midX, 600 + 1.5, accuracy: 2)
    }
}
