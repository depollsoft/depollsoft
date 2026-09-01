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

        snap("tour-pipe")
        for tab in ["Notes", "Keys", "Songs"] {
            let button = app.tabBars.buttons[tab]
            if button.waitForExistence(timeout: 5) {
                button.tap()
                sleep(1)
                snap("tour-\(tab.lowercased())")
            }
        }
        app.tabBars.buttons["Pitch Pipe"].tap()
        sleep(1)
        let gear = app.navigationBars.buttons.firstMatch
        if gear.waitForExistence(timeout: 5) {
            gear.tap()
            sleep(1)
            snap("tour-settings")
        }
    }
}
