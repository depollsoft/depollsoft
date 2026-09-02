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
    }

    func testWidgetDeepLinkOpensInclusiveEndpoint() throws {
        let app = XCUIApplication()
        let url = URL(string: "pitchperfect://note?name=C&accidental=natural&octave=5&range=cToC")!
        app.open(url)

        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let confirmation = springboard.buttons["Open"]
        if confirmation.waitForExistence(timeout: 3) {
            confirmation.tap()
        }

        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 8))
        XCTAssertTrue(
            app.buttons["C, octave 5"].waitForExistence(timeout: 5),
            "The inclusive C5 endpoint should be exposed after a widget deep link"
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
