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
        let emptyHome = springboard.windows.firstMatch.coordinate(
            withNormalizedOffset: CGVector(dx: 0.5, dy: 0.72)
        )
        emptyHome.press(forDuration: 1.3)

        let editHome = springboard.buttons["Edit Home Screen"]
        if editHome.waitForExistence(timeout: 2) {
            editHome.tap()
        } else {
            let edit = springboard.buttons["Edit"].firstMatch
            if edit.waitForExistence(timeout: 2) {
                edit.tap()
            }
        }
        let addWidget = springboard.buttons["Add Widget"].firstMatch
        XCTAssertTrue(addWidget.waitForExistence(timeout: 5), "SpringBoard should expose Add Widget")
        addWidget.tap()

        let search = springboard.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: 5), "Widget gallery should expose search")
        search.tap()
        search.typeText("Pitch Perfect")

        let pitchPerfect = springboard.staticTexts["Pitch Perfect"].firstMatch
        XCTAssertTrue(pitchPerfect.waitForExistence(timeout: 5), "Pitch Perfect should appear in the widget gallery")
        let resultCell = springboard.cells.containing(.staticText, identifier: "Pitch Perfect").firstMatch
        if resultCell.exists {
            resultCell.tap()
        } else {
            pitchPerfect.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        sleep(2)

        let attachment = XCTAttachment(screenshot: springboard.screenshot())
        attachment.name = "widget-gallery-pitch-pipe"
        attachment.lifetime = .keepAlways
        add(attachment)

        let addButton = springboard.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "Add Widget")
        ).firstMatch
        XCTAssertTrue(addButton.waitForExistence(timeout: 5))
        addButton.tap()
        XCTAssertTrue(springboard.wait(for: .runningForeground, timeout: 5))
        let done = springboard.buttons["Done"].firstMatch
        if done.waitForExistence(timeout: 3) {
            done.tap()
        }

        let c4 = springboard.buttons["C, octave 4"].firstMatch
        XCTAssertTrue(c4.waitForExistence(timeout: 8), "The configured widget should expose C4")
        c4.tap()
        XCTAssertTrue(
            springboard.wait(for: .runningForeground, timeout: 3),
            "Sounding a widget pitch must keep the user on the Home Screen"
        )
        let playing = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value == %@", "Playing"),
            object: c4
        )
        XCTAssertEqual(.completed, XCTWaiter.wait(for: [playing], timeout: 1.2))

        let sounding = XCTAttachment(screenshot: springboard.screenshot())
        sounding.name = "widget-sounding-c4-on-home"
        sounding.lifetime = .keepAlways
        add(sounding)

        c4.tap()
        let stopped = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "value != %@", "Playing"),
            object: c4
        )
        XCTAssertEqual(.completed, XCTWaiter.wait(for: [stopped], timeout: 3))

        let fToF = springboard.buttons["Use F to F range"].firstMatch
        XCTAssertTrue(fToF.waitForExistence(timeout: 3), "The widget should expose its F-to-F control")
        XCTAssertTrue(fToF.isHittable, "The F-to-F control should accept taps")
        fToF.tap()
        XCTAssertTrue(
            springboard.buttons["F, octave 5"].firstMatch.waitForExistence(timeout: 8),
            "Selecting F to F should replace C4 with F5"
        )
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
