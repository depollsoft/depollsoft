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
        // The range is remembered; start from C to C whatever an earlier run left.
        if range.buttons["F to F"].isSelected { range.buttons["C to C"].tap() }
        XCTAssertTrue(element("C, octave 5").waitForExistence(timeout: 5))

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(range.waitForExistence(timeout: 10))

        // The cells and the range control are on screen and big enough to hit.
        for label in ["C, octave 4", "A sharp, B flat, octave 4", "C, octave 5", "F sharp, G flat, octave 4"] {
            let cell = element(label)
            XCTAssertTrue(cell.isHittable, label)
            XCTAssertGreaterThanOrEqual(cell.frame.height, 44, label)
        }
        XCTAssertTrue(range.isHittable)
        // The short well puts the range control beside the readout, not under it,
        // in the row under the upper note.
        let wide = element("C, octave 5").frame
        XCTAssertGreaterThan(range.frame.minX, wide.midX, "the range control sits at the well's right")
        XCTAssertGreaterThan(range.frame.minY, wide.maxY)
        XCTAssertLessThan(range.frame.maxY, wide.maxY + wide.height)

        element("A, octave 4").tap()
        let shot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        shot.name = "classic-landscape"
        shot.lifetime = .keepAlways
        add(shot)

        range.buttons["F to F"].tap()
        XCTAssertTrue(element("F, octave 5").waitForExistence(timeout: 5))
        range.buttons["C to C"].tap()
        XCTAssertTrue(element("C, octave 5").waitForExistence(timeout: 5))
    }
}
