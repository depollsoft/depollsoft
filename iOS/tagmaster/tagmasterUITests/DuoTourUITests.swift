//
//  DuoTourUITests.swift
//  tagmasterUITests
//
//  Walks the main screens and writes a capture of each, for checking a layout
//  in a pose the in-process catalog cannot reach: an iPhone Duo closed, open,
//  or half folded in Device Hub. It runs only when asked:
//
//    TEST_RUNNER_DUO_TOUR_DIR=/path TEST_RUNNER_DUO_TOUR_POSE=open xcodebuild test \
//      -only-testing:tagmasterUITests/DuoTourUITests ...
//

import XCTest

final class DuoTourUITests: TagMasterUITestCase {
    private var app: XCUIApplication!
    private var directory: URL!
    private var pose = "pose"

    override func setUpWithError() throws {
        let environment = ProcessInfo.processInfo.environment
        guard let path = environment["DUO_TOUR_DIR"], !path.isEmpty else {
            throw XCTSkip("Set TEST_RUNNER_DUO_TOUR_DIR to run the tour")
        }
        directory = URL(fileURLWithPath: path, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        pose = environment["DUO_TOUR_POSE"] ?? pose
        continueAfterFailure = true
        app = XCUIApplication()
        // Three favourites, so a tag beside the list can step to its neighbours.
        app.launchArguments = ["--uitesting", "-depollsoft.pitchperfect.lists",
            "<dict><key>favorite</key><array><integer>1809</integer><integer>42</integer><integer>5038</integer></array></dict>"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        if let appearance = environment["DUO_TOUR_APPEARANCE"] {
            app.launchArguments += ["--appearance", appearance]
        }
        // DUO_TOUR_ATTACH drives an app already running, such as one moved into a
        // resizable session at the inner display's size, instead of relaunching it.
        if environment["DUO_TOUR_ATTACH"] != nil {
            app.activate()
        } else {
            app.launch()
        }
    }

    func testTour() throws {
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 15))
        capture("1-home")

        let favorite = app.buttons.matching(NSPredicate(
            format: "label CONTAINS 'Tag ID 1809' OR label == 'Tag 1809. Open to load details.'")).firstMatch
        let list = app.collectionViews.firstMatch
        for _ in 0..<6 where !favorite.existsOrWait(timeout: 1) || !favorite.isHittable {
            list.swipeUp()
        }
        favorite.tap()
        _ = app.buttons["Rate tag"].existsOrWait(timeout: 20)
        capture("2-tag-summary")

        if app.buttons["page-Tracks"].existsOrWait(timeout: 5) {
            app.buttons["page-Tracks"].tap()
            capture("3-tag-tracks")
        }

        if app.buttons["Next tag"].exists, app.buttons["Next tag"].isEnabled {
            app.buttons["Next tag"].tap()
            _ = app.buttons["Rate tag"].existsOrWait(timeout: 20)
            capture("4-tag-stepped")
        }

        // Back to the list on a stack; beside a list there is nothing to go back from.
        goBack()
        let browse = app.buttons["Browse"].firstMatch
        if browse.existsOrWait(timeout: 5) {
            browse.tap()
            _ = app.tagRows.firstMatch.existsOrWait(timeout: 30)
            capture("5-browse")
            goBack()
        }

        let settings = app.buttons["Settings"].firstMatch
        if settings.existsOrWait(timeout: 5) {
            settings.tap()
            _ = app.buttons["Log In"].existsOrWait(timeout: 5)
            capture("6-settings")
        }
    }

    /// Opens a tag on its Tracks page and starts a part, then waits while the host
    /// opens the device and closes it again (it writes `opened` and `closed` into the
    /// tour directory), and checks after each that the shell changed and the same
    /// tag, page and playing part are still there. Runs only when
    /// TEST_RUNNER_DUO_TOUR_HANDOFF is set as well.
    func testOpeningAndClosingKeepTheTagAndPage() throws {
        guard ProcessInfo.processInfo.environment["DUO_TOUR_HANDOFF"] != nil else {
            throw XCTSkip("Set TEST_RUNNER_DUO_TOUR_HANDOFF to run the fold handoff")
        }
        // A previous run's signals would let this one pass without the pose changing.
        for name in ["opened", "closed", "ready-opened", "ready-closed"] {
            try? FileManager.default.removeItem(at: directory.appendingPathComponent(name))
        }
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 15))
        let favorite = app.buttons.matching(NSPredicate(
            format: "label CONTAINS 'Tag ID 1809' OR label == 'Tag 1809. Open to load details.'")).firstMatch
        let list = app.collectionViews.firstMatch
        for _ in 0..<6 where !favorite.existsOrWait(timeout: 1) || !favorite.isHittable {
            list.swipeUp()
        }
        favorite.tap()
        XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 20))
        app.buttons["page-Tracks"].tap()
        XCTAssertTrue(app.buttons["page-Tracks"].waitUntilSelected(timeout: 5))
        let lead = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Lead'")).firstMatch
        XCTAssertTrue(lead.existsOrWait(timeout: 10))
        lead.tap()
        let player = app.descendants(matching: .any)["tagmaster.trackPlayer.position"]
        XCTAssertTrue(player.existsOrWait(timeout: 30), "The Lead track loads and plays")
        capture("handoff-1-closed")

        for (signal, step, open) in [("opened", "handoff-2-open", true), ("closed", "handoff-3-closed", false)] {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            FileManager.default.createFile(atPath: directory.appendingPathComponent("ready-\(signal)").path, contents: nil)
            let marker = directory.appendingPathComponent(signal).path
            let deadline = Date().addingTimeInterval(90)
            while !FileManager.default.fileExists(atPath: marker), Date() < deadline {
                RunLoop.current.run(until: Date().addingTimeInterval(0.5))
            }
            XCTAssertTrue(FileManager.default.fileExists(atPath: marker), "The host never changed the pose")
            RunLoop.current.run(until: Date().addingTimeInterval(4))
            XCTAssertTrue(app.buttons["page-Tracks"].existsOrWait(timeout: 10), "The tag is still showing (\(signal))")
            XCTAssertTrue(app.buttons["page-Tracks"].isSelected, "Its page survived (\(signal))")
            XCTAssertTrue(app.navigationBars["Lost"].exists || app.staticTexts["Lost"].exists,
                          "It is the same tag (\(signal))")
            // Open, Home's list shows beside the tag; closed, the tag covers it.
            let browse = app.buttons["Browse"]
            XCTAssertEqual(browse.exists && browse.isHittable, open, "The shell changed (\(signal))")
            XCTAssertTrue(player.exists, "The part is still playing (\(signal))")
            capture(step)
        }
    }

    /// The back button, wherever the bar put it: titled on a horizontal bar, a bare
    /// chevron at the top of a vertical one.
    private func goBack() {
        for candidate in [app.navigationBars.buttons["Home"], app.buttons["BackButton"], app.buttons["Back"]] {
            let button = candidate.firstMatch
            if button.exists, button.isHittable {
                button.tap()
                RunLoop.current.run(until: Date().addingTimeInterval(0.8))
                return
            }
        }
    }

    /// Every screen the device has, so both of a Duo's displays are kept.
    private func capture(_ step: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(1.2))
        for (index, screen) in XCUIScreen.screens.enumerated() {
            let name = "tm-\(pose)-\(step)" + (index == 0 ? "" : "-screen\(index)") + ".png"
            try? screen.screenshot().pngRepresentation.write(to: directory.appendingPathComponent(name))
        }
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "tm-\(pose)-\(step)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
