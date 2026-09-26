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
        // The Songs list is a SwiftUI List: a collection view, not a table.
        let table = app.collectionViews.firstMatch
        // A cold first launch can take a few seconds to lay out the tab.
        guard table.waitForExistence(timeout: 10) else {
            XCTFail("Songs list not found")
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

    /// The real app builds its song store when SwiftUI creates the App, before
    /// didFinishLaunching; the saved songs must still decode on the next launch.
    func testASavedSongSurvivesARelaunch() throws {
        let title = "Relaunch Check \(Int(Date().timeIntervalSince1970) % 100_000)"
        let edit = app.navigationBars.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 10))
        edit.tap()
        app.navigationBars.buttons["Add"].tap()
        let field = app.textFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(title)
        let entered = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", title), object: field)
        XCTAssertEqual(XCTWaiter.wait(for: [entered], timeout: 30), .completed)
        // Return dismisses focus so SwiftUI has the whole title before Done.
        field.typeText("\n")
        let save = app.navigationBars["Add Song"].buttons["Done"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title), ")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10))
        app.navigationBars.buttons["Done"].tap()

        app.terminate()
        app.launch()
        navigateToSongs()
        let restored = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(title), ")).firstMatch
        XCTAssertTrue(restored.waitForExistence(timeout: 10), "The saved song came back after a relaunch")

        // Leave the simulator's songs as they were for the store screenshot tour.
        restored.swipeLeft()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.waitForExistence(timeout: 5))
        delete.tap()
        XCTAssertTrue(restored.waitForNonExistence(timeout: 5))
    }
}
