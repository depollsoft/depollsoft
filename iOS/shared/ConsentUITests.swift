import XCTest

final class ConsentUITests: XCTestCase {
    func testIndependentChoicesPersistAndCanBeRevoked() {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launchArguments = ["--reset-privacy-for-testing", "-FIRDebugEnabled"]
        app.launch()
        let analytics = app.switches["Usage analytics"]
        let crashes = app.switches["Crash reports"]
        XCTAssertTrue(analytics.waitForExistence(timeout: 20))
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
        app.terminate()
        app.launchArguments = ["-FIRDebugEnabled", "-depollsoft.pitchperfect.LoginShown", "YES"]
        app.launch()
        XCTAssertFalse(analytics.exists, "Remembered choices should not prompt again")
        openPrivacy(app)
        XCTAssertEqual(analytics.value as? String, "1")
        XCTAssertEqual(crashes.value as? String, "0")
        app.buttons["Decline both"].tap()
        app.terminate()
        app.launch()
        openPrivacy(app)
        XCTAssertEqual(analytics.value as? String, "0")
        XCTAssertEqual(crashes.value as? String, "0")
    }

    private func openPrivacy(_ app: XCUIApplication) {
        let settingsButton = app.buttons["Settings"].firstMatch
        XCTAssertTrue(settingsButton.waitForExistence(timeout: 15))
        let settings = settingsButton
        settings.tap()
        // A button in SwiftUI Settings. A SwiftUI list only creates the rows it
        // has laid out, so scroll until the row exists and can be tapped.
        let privacy = app.buttons["Privacy choices"].firstMatch
        _ = privacy.waitForExistence(timeout: 3)
        for _ in 0..<6 where !(privacy.exists && privacy.isHittable) { app.swipeUp() }
        XCTAssertTrue(privacy.waitForExistence(timeout: 5))
        privacy.tap()
        XCTAssertTrue(app.switches["Usage analytics"].waitForExistence(timeout: 5))
    }
}
