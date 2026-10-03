import XCTest

final class ConsentUITests: XCTestCase {
    override class func setUp() {
        super.setUp()
        // Prepare first-launch services before the individual case budgets,
        // as the other native UI suites do on a fresh simulator.
        let app = XCUIApplication()
        app.launchArguments = ["--uitesting", "-telemetry.chosen", "YES",
                               "-telemetry.analytics", "NO", "-telemetry.crashes", "NO",
                               "-depollsoft.pitchperfect.LoginShown", "YES"]
        app.launchEnvironment["STORE_SCREENSHOTS"] = "1"
        app.launch()
        app.terminate()
    }

    func testIndependentChoicesPersistAfterRelaunch() {
        let app = saveIndependentChoices()
        relaunch(app)
        let analytics = app.switches["Usage analytics"]
        XCTAssertFalse(analytics.exists, "Remembered choices should not prompt again")
        openPrivacy(app)
        XCTAssertEqual(analytics.value as? String, "1")
        XCTAssertEqual(app.switches["Crash reports"].value as? String, "0")
    }

    func testChoicesCanBeRevokedAndStayRevokedAfterRelaunch() {
        let app = saveIndependentChoices()
        openPrivacy(app)
        XCTAssertEqual(app.switches["Usage analytics"].value as? String, "1")
        app.buttons["Decline both"].tap()
        relaunch(app)
        XCTAssertFalse(app.switches["Usage analytics"].exists,
                       "Declining both is a remembered choice")
        openPrivacy(app)
        XCTAssertEqual(app.switches["Usage analytics"].value as? String, "0")
        XCTAssertEqual(app.switches["Crash reports"].value as? String, "0")
    }

    private func saveIndependentChoices() -> XCUIApplication {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--reset-privacy-for-testing", "-FIRDebugEnabled"]
        app.launch()
        let analytics = app.switches["Usage analytics"]
        let crashes = app.switches["Crash reports"]
        XCTAssertTrue(analytics.exists || analytics.waitForExistence(timeout: 20))
        XCTAssertEqual(analytics.value as? String, "0")
        XCTAssertEqual(crashes.value as? String, "0")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Privacy choices - default off"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        // A SwiftUI Toggle's element spans its row; the switch itself sits inside it.
        let knob = analytics.switches.firstMatch
        (knob.exists ? knob : analytics).tap()
        XCTAssertEqual(analytics.value as? String, "1")
        app.navigationBars.buttons["Save choices"].tap()
        return app
    }

    private func relaunch(_ app: XCUIApplication) {
        app.terminate()
        app.launchArguments = ["-FIRDebugEnabled", "-depollsoft.pitchperfect.LoginShown", "YES"]
        app.launch()
    }

    private func openPrivacy(_ app: XCUIApplication) {
        let settingsButton = app.buttons["Settings"].firstMatch
        XCTAssertTrue(settingsButton.exists || settingsButton.waitForExistence(timeout: 15))
        let settings = settingsButton
        settings.tap()
        // A button in SwiftUI Settings. A SwiftUI list only creates the rows it
        // has laid out, so scroll until the row exists and can be tapped.
        let privacy = app.buttons["Privacy choices"].firstMatch
        for _ in 0..<6 where !(privacy.exists && privacy.isHittable) { app.swipeUp() }
        XCTAssertTrue(privacy.exists || privacy.waitForExistence(timeout: 5))
        privacy.tap()
        let analytics = app.switches["Usage analytics"]
        XCTAssertTrue(analytics.exists || analytics.waitForExistence(timeout: 5))
    }
}
