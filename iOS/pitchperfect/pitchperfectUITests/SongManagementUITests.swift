import XCTest

// Keep the native swipe interaction; list content and editor actions run in pitchperfectTests.
final class SongManagementUITests: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
        navigateToSongs()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    private func navigateToSongs() {
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 5) {
            let songsTab = tabBar.buttons["Songs"]
            if songsTab.exists {
                songsTab.tap()
                Thread.sleep(forTimeInterval: 0.5)
            }
        }
    }

    func testCanSwipeOnSongIfExists() throws {
        let table = app.tables.firstMatch
        guard table.waitForExistence(timeout: 2) else {
            XCTFail("Songs table not found")
            return
        }
        
        guard table.cells.count > 0 else {
            throw XCTSkip("No songs in list to test")
        }
        
        // Swipe left on first song
        let firstCell = table.cells.element(boundBy: 0)
        firstCell.swipeLeft()
        Thread.sleep(forTimeInterval: 0.3)
        
        // Swipe right to dismiss actions
        firstCell.swipeRight()
        Thread.sleep(forTimeInterval: 0.3)
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle swipe gestures")
    }
}
