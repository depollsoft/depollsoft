import XCTest

// Process startup and Firebase-rendered authentication still need the UI runner.
final class PitchPerfectUITests: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    func testAppLaunches() throws {
        XCTAssertTrue(app.state == .runningForeground, "App should be running in foreground")
    }

    func testAuthProvidersRenderAndPhoneEntryOpens() throws {
        app.terminate()
        app.launchArguments = [
            "-depollsoft.pitchperfect.LoginShown", "NO",
        ]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()

        let loginButton = app.buttons["Sign up or log in"]
        XCTAssertTrue(loginButton.waitForExistence(timeout: 5))
        loginButton.tap()
        XCTAssertTrue(app.textFields["email-field"].waitForExistence(timeout: 5))

        for provider in [
            "Sign in with Google",
            "Sign in with Facebook",
            "Sign in with Apple",
            "Sign in with Phone",
        ] {
            XCTAssertTrue(
                app.buttons[provider].exists,
                "Missing provider button: (provider)",
            )
        }

        let providerScreenshot = XCTAttachment(screenshot: app.screenshot())
        providerScreenshot.name = "Fixed auth provider buttons"
        providerScreenshot.lifetime = .keepAlways
        add(providerScreenshot)

        app.buttons["Sign in with Phone"].tap()
        XCTAssertEqual(app.state, .runningForeground)
        XCTAssertTrue(app.textFields.firstMatch.waitForExistence(timeout: 5))
    }

    func testOpeningLoginScreenDoesNotCrash() throws {
        app.terminate()
        app.launchArguments = [
            "-depollsoft.pitchperfect.LoginShown", "NO",
        ]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()

        let loginButton = app.buttons["Sign up or log in"]
        XCTAssertTrue(
            loginButton.waitForExistence(timeout: 5),
            "Login prompt should be visible",
        )
        loginButton.tap()

        XCTAssertTrue(
            app.textFields["email-field"].waitForExistence(timeout: 5),
            "Firebase login screen should open",
        )
        XCTAssertEqual(
            app.state,
            .runningForeground,
            "Opening the login screen must not terminate the app",
        )
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}
