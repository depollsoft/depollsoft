//
//  DesignTourUITests.swift
//  pitchperfectUITests
//
//  Captures every screen for design review evidence.
//

import XCTest

final class DesignTourUITests: XCTestCase {
    func testWidgetGalleryShowsPitchPipe() throws {
        let app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        XCUIDevice.shared.press(.home)

        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        addPitchPipeWidget(to: springboard)

        let attachment = XCTAttachment(screenshot: springboard.screenshot())
        attachment.name = "widget-on-home"
        attachment.lifetime = .keepAlways
        add(attachment)

        let c4 = springboard.pitchCell("C, octave 4")
        XCTAssertTrue(c4.waitForExistence(timeout: 8), "The configured widget should expose C4")
        c4.tap()
        XCTAssertTrue(
            springboard.wait(for: .runningForeground, timeout: 3),
            "Sounding a widget pitch must keep the user on the Home Screen"
        )
        let playing = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value == %@", "1"),
            object: c4
        )
        XCTAssertEqual(.completed, XCTWaiter.wait(for: [playing], timeout: 3))
        // A momentary optimistic Toggle flash is not persistent playback state.
        // Observe beyond both the timeline reload and the four-second audio loop.
        for _ in 0..<6 {
            Thread.sleep(forTimeInterval: 1)
            XCTAssertEqual(c4.value as? String, "1", "The sounding note must stay selected after reload")
        }

        let sounding = XCTAttachment(screenshot: springboard.screenshot())
        sounding.name = "widget-sounding-c4-on-home"
        sounding.lifetime = .keepAlways
        add(sounding)

        let e4 = springboard.pitchCell("E, octave 4")
        e4.tap()
        XCTAssertTrue(springboard.staticTexts["2 NOTES"].waitForExistence(timeout: 8))
        for _ in 0..<6 {
            Thread.sleep(forTimeInterval: 1)
            XCTAssertEqual(c4.value as? String, "1", "Adding E must leave C selected")
            XCTAssertEqual(e4.value as? String, "1", "Both sounding notes must stay selected")
        }
        let chord = XCTAttachment(screenshot: springboard.screenshot())
        chord.name = "widget-c4-e4-sustained-on-home"
        chord.lifetime = .keepAlways
        add(chord)

        c4.tap()
        let stopped = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value != %@", "1"),
            object: c4
        )
        XCTAssertEqual(.completed, XCTWaiter.wait(for: [stopped], timeout: 3))
        Thread.sleep(forTimeInterval: 2)
        XCTAssertNotEqual(c4.value as? String, "1", "The second tap must leave the note off")
        XCTAssertEqual(e4.value as? String, "1", "Stopping C must not stop E")
        e4.tap()
        Thread.sleep(forTimeInterval: 2)
        XCTAssertNotEqual(e4.value as? String, "1", "Each note must toggle off independently")

        c4.tap()
        e4.tap()
        XCTAssertTrue(springboard.staticTexts["2 NOTES"].waitForExistence(timeout: 8))
        let fToF = springboard.buttons["Octave range F to F"].firstMatch
        XCTAssertTrue(fToF.waitForExistence(timeout: 3), "The widget should expose its F-to-F control")
        XCTAssertTrue(fToF.isHittable, "The F-to-F control should accept taps")
        fToF.tap()
        XCTAssertTrue(
            springboard.pitchCell("F, octave 5").waitForExistence(timeout: 8),
            "Selecting F to F should replace C4 with F5"
        )
        XCTAssertTrue(springboard.buttons["Octave range F to F"].firstMatch.isSelected)
        Thread.sleep(forTimeInterval: 2)
        XCTAssertNotEqual(springboard.pitchCell("F, octave 4").value as? String, "1")
        XCTAssertNotEqual(springboard.pitchCell("A, octave 4").value as? String, "1")

        let highRange = XCTAttachment(screenshot: springboard.screenshot())
        highRange.name = "widget-f-to-f-on-home"
        highRange.lifetime = .keepAlways
        add(highRange)
    }

    func testCaptureEveryScreen() throws {
        let app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()

        func snap(_ name: String) {
            let attachment = XCTAttachment(screenshot: app.screenshot())
            attachment.name = name
            attachment.lifetime = .keepAlways
            add(attachment)
        }

        func tab(named name: String) -> XCUIElement {
            let candidates = [
                app.tabBars.buttons[name].firstMatch,
                app.buttons[name].firstMatch,
                app.cells[name].firstMatch,
                app.otherElements[name].firstMatch,
            ]
            return candidates.first { $0.exists }
                ?? app.descendants(matching: .any)[name]
        }

        snap("tour-pipe")
        for name in ["Notes", "Keys", "Songs"] {
            let item = tab(named: name)
            if item.waitForExistence(timeout: 5) {
                item.tap()
                sleep(1)
                snap("tour-\(name.lowercased())")
            }
        }
        tab(named: "Pitch Pipe").tap()
        sleep(1)
        let navigationGear = app.navigationBars.buttons.firstMatch
        let gear = navigationGear.exists ? navigationGear : app.buttons["Settings"].firstMatch
        if gear.waitForExistence(timeout: 5) {
            gear.tap()
            sleep(1)
            snap("tour-settings")
        }
    }
}
