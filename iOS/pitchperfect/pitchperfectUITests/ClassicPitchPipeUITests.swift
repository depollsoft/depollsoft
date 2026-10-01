//
//  ClassicPitchPipeUITests.swift
//  pitchperfectUITests
//
//  The classic pitch pipe turned on its side in the real app: only a real
//  rotation gives the landscape bars and safe areas, where the grid's well is
//  short and the readout moves beside the range control.
//

import XCTest

final class ClassicPitchPipeUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
        app.launchArguments = ["--uitesting",
                               "-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO",
                               "-depollsoft.pitchperfect.ClassicPitchPipe", "YES",
                               // Notes latch, so the capture shows one sounding.
                               "-depollsoft.pitchperfect.ToggleNote", "YES"]
        // No ad consent or tracking prompt over the grid (a debug build honours this).
        app.launchEnvironment["STORE_SCREENSHOTS"] = "1"
        app.launch()
    }

    override func tearDownWithError() throws {
        XCUIDevice.shared.orientation = .portrait
    }

    private func element(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    func testTheClassicGridWorksTurnedOnItsSide() throws {
        let range = app.segmentedControls["pitchpipe.range"]
        XCTAssertTrue(range.waitForExistence(timeout: 10), "the classic grid's range control")
        // The range is remembered; start from C to B whatever an earlier run left.
        if range.buttons["F to E"].isSelected { range.buttons["C to B"].tap() }
        XCTAssertTrue(element("B, octave 4").waitForExistence(timeout: 5))
        XCTAssertFalse(element("C, octave 5").exists, "one octave, C to B")

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(range.waitForExistence(timeout: 10))

        // The cells and the range control are on screen and big enough to hit.
        for label in ["C, octave 4", "A sharp, B flat, octave 4", "B, octave 4", "F sharp, G flat, octave 4"] {
            let cell = element(label)
            XCTAssertTrue(cell.isHittable, label)
            XCTAssertGreaterThanOrEqual(cell.frame.height, 44, label)
        }
        XCTAssertTrue(range.isHittable)
        // The short well puts the range control at its right, beside the readout,
        // between the B and E columns and the top and bottom rows.
        let b = element("B, octave 4").frame
        let e = element("E, octave 4").frame
        XCTAssertGreaterThan(range.frame.minX, (b.maxX + e.minX) / 2, "the range control sits at the well's right")
        XCTAssertLessThan(range.frame.maxX, e.minX)
        XCTAssertGreaterThan(range.frame.minY, element("C, octave 4").frame.maxY)
        XCTAssertLessThan(range.frame.maxY, element("A, octave 4").frame.minY)

        element("A, octave 4").tap()
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = "classic-landscape"
        shot.lifetime = .keepAlways
        add(shot)

        range.buttons["F to E"].tap()
        XCTAssertTrue(element("E, octave 5").waitForExistence(timeout: 5))
        range.buttons["C to B"].tap()
        XCTAssertTrue(element("C, octave 4").waitForExistence(timeout: 5))
    }
}
