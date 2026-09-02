//
//  DesignTourUITests.swift
//  pitchperfectUITests
//
//  Captures every screen for design review evidence.
//

import XCTest

final class DesignTourUITests: XCTestCase {
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
