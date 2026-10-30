//
//  DuoTourUITests.swift
//  pitchperfectUITests
//
//  An opt-in walk through every tab and Settings that writes a screenshot of
//  each, for checking the app in each iPhone Duo pose: Device Hub sets the
//  pose (closed, partly folded, open, turned), this only looks. It skips unless
//  DUO_TOUR_DIR names a directory (TEST_RUNNER_DUO_TOUR_DIR on the xcodebuild
//  command line); DUO_TOUR_POSE, if set, prefixes the file names.
//

import XCTest

final class DuoTourUITests: XCTestCase {
    private var app: XCUIApplication!
    private var directory = ""
    private var prefix = ""

    override func setUpWithError() throws {
        let environment = ProcessInfo.processInfo.environment
        guard let directory = environment["DUO_TOUR_DIR"], !directory.isEmpty else {
            throw XCTSkip("Set TEST_RUNNER_DUO_TOUR_DIR to tour the app in an iPhone Duo pose")
        }
        try FileManager.default.createDirectory(atPath: directory, withIntermediateDirectories: true)
        self.directory = directory
        prefix = environment["DUO_TOUR_POSE"].map { "\($0)-" } ?? ""
    }

    func testTourEveryTabAndSettings() throws {
        launch()
        tab("Pitch Pipe")
        try snap("pipe")
        for (title, name) in [("Notes", "notes"), ("Keys", "keys"), ("Songs", "songs")] {
            tab(title)
            try snap(name)
        }
        tab("Pitch Pipe")
        let gear = app.buttons["gearshape"].firstMatch
        XCTAssertTrue(gear.waitForExistence(timeout: 10), "No Settings button")
        gear.tap()
        try snap("settings")
    }

    /// Both faces at rest and sounding (notes latch, so a tap holds one), in light
    /// and in dark.
    func testTourTheFacesSounding() throws {
        for (theme, look) in [("1", "light"), ("2", "dark")] {
            for (classic, face) in [("NO", "ring"), ("YES", "classic")] {
                launch(["-depollsoft.pitchperfect.ToggleNote", "YES", "-depollsoft.pitchperfect.theme", theme,
                        "-depollsoft.pitchperfect.ClassicPitchPipe", classic])
                tab("Pitch Pipe")
                let note = app.descendants(matching: .any)
                    .matching(NSPredicate(format: "label == %@", "E, octave 4")).firstMatch
                XCTAssertTrue(note.waitForExistence(timeout: 10), "No E on the \(face)")
                try snap("\(face)-\(look)")
                note.tap()
                try snap("\(face)-\(look)-sounding")
                note.tap()
                app.terminate()
            }
        }
    }

    private func launch(_ arguments: [String] = []) {
        app = XCUIApplication()
        // As for store capture: no ad-consent or tracking prompt, the banner's slot without a test ad.
        app.launchEnvironment["STORE_SCREENSHOTS"] = "1"
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO",
                                "-depollsoft.pitchperfect.LoginShown", "YES"] + arguments
        app.launch()
    }

    /// An iPhone Duo has two displays, and the app's own screenshot is the outer
    /// one's (black while the Duo is open): take the display the app's window
    /// fills. The inner display held upright reports its size sideways, and its
    /// shot may come out sideways too.
    private func screenshot() -> XCUIScreenshot {
        let window = app.windows.firstMatch.frame.size
        func fills(_ shot: XCUIScreenshot, turned: Bool) -> Bool {
            let size = shot.image.size
            let (width, height) = turned ? (size.height, size.width) : (size.width, size.height)
            return abs(width - window.width) < 1 && abs(height - window.height) < 1
        }
        let shots = XCUIScreen.screens.map { $0.screenshot() }
        if let shot = shots.first(where: { fills($0, turned: false) }) { return shot }
        if let shot = shots.dropFirst().first(where: { fills($0, turned: true) }) { return shot }
        return app.screenshot()
    }

    private func snap(_ name: String) throws {
        // Let bars settle into whichever axis the pose gives them.
        Thread.sleep(forTimeInterval: 1.5)
        let screenshot = screenshot()
        try screenshot.pngRepresentation.write(to: URL(fileURLWithPath: directory)
            .appendingPathComponent("\(prefix)\(name).png"))
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = "\(prefix)\(name)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// A tab's item is a tab bar button at the foot, or a button down the side bar.
    private func tab(_ name: String) {
        let candidates = [app.tabBars.buttons[name].firstMatch, app.buttons[name].firstMatch,
                          app.otherElements[name].firstMatch]
        let item = candidates.first { $0.exists } ?? app.descendants(matching: .any)[name].firstMatch
        XCTAssertTrue(item.waitForExistence(timeout: 10), "No \(name) tab")
        item.tap()
    }
}
