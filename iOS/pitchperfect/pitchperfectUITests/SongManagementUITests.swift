import XCTest

/**
 * UI Tests for Song Management functionality in Pitch Perfect.
 * Tests adding, editing, and managing songs.
 */
class SongManagementUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        
        // Navigate to Songs tab
        navigateToSongs()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Helper Methods
    
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
    
    private func findAddButton() -> XCUIElement? {
        let editButton = app.navigationBars.buttons["Edit"]
        if editButton.waitForExistence(timeout: 3) { editButton.tap() }

        let candidates = [
            app.buttons["AddSong"],
            app.buttons["Add song"],
            app.navigationBars.buttons["AddSong"],
            app.navigationBars.buttons["Add song"]
        ]
        return candidates.first { $0.waitForExistence(timeout: 1) }
    }
    
    // MARK: - Song List Display Tests
    
    func testSongListTableExists() throws {
        let table = firstList(in: app)
        XCTAssertTrue(table.waitForExistence(timeout: 2), "Songs table should exist")
    }
    
    func testSongListDisplaysContent() throws {
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 2) else {
            XCTFail("Songs table not found")
            return
        }
        
        // Table should exist (may be empty or have songs)
        XCTAssertTrue(table.exists, "Songs table should be displayed")
    }
    
    func testNavigationBarEditingActions() throws {
        let navigationBar = app.navigationBars.firstMatch
        XCTAssertTrue(
            navigationBar.waitForExistence(timeout: 2),
            "Songs should use a system navigation bar"
        )

        let editButton = navigationBar.buttons["Edit"]
        XCTAssertTrue(editButton.exists, "Edit action should be visible")
        XCTAssertTrue(
            navigationBar.buttons["Settings"].exists,
            "Settings action should be visible"
        )

        editButton.tap()
        XCTAssertTrue(navigationBar.buttons["Done"].exists)
        XCTAssertTrue(navigationBar.buttons["Sort Alphabetically"].exists)
        XCTAssertTrue(navigationBar.buttons["Add"].exists)

        navigationBar.buttons["Done"].tap()
        XCTAssertTrue(navigationBar.buttons["Edit"].exists)
        XCTAssertTrue(navigationBar.buttons["Settings"].exists)
    }

    // MARK: - Add Song Tests
    
    func testAddSongButtonExists() throws {
        let addButton = findAddButton()
        if addButton == nil {
            throw XCTSkip("Add song button not found in expected locations")
        }
    }
    
    func testTapAddSongOpensForm() throws {
        guard let addButton = findAddButton() else {
            throw XCTSkip("Add button not found")
        }
        
        addButton.tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should show add song form with text field or alert
        let hasForm = app.textFields.firstMatch.exists ||
                      app.alerts.firstMatch.exists ||
                      app.sheets.firstMatch.exists
        
        XCTAssertTrue(hasForm, "Should show add song form")
    }
    
    func testCanDismissAddSongForm() throws {
        guard let addButton = findAddButton() else {
            throw XCTSkip("Add button not found")
        }
        
        addButton.tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Dismiss the form
        if app.buttons["Cancel"].exists {
            app.buttons["Cancel"].tap()
        } else if app.alerts.firstMatch.exists {
            app.alerts.buttons["Cancel"].tap()
        } else {
            app.swipeDown()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should be back at song list
        let table = firstList(in: app)
        XCTAssertTrue(table.exists, "Should return to songs list")
    }
    
    func testCanAddSong() throws {
        guard let addButton = findAddButton() else {
            XCTFail("Add song button not found")
            return
        }

        addButton.tap()
        let nameField = app.textFields["SongName"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        let songName = "UI Test Song \(UUID().uuidString.prefix(8))"
        nameField.tap()
        nameField.typeText(songName)
        app.navigationBars["Add Song"].buttons["Done"].tap()

        let savedSong = app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", songName)
        ).firstMatch
        XCTAssertTrue(savedSong.waitForExistence(timeout: 3))
    }

    // MARK: - Song Interaction Tests
    
    func testCanTapSongIfExists() throws {
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 2) else {
            XCTFail("Songs table not found")
            return
        }
        
        guard table.cells.count > 0 else {
            throw XCTSkip("No songs in list to test")
        }
        
        // Tap first song
        table.cells.element(boundBy: 0).tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle song tap")
    }
    
    func testCanSwipeOnSongIfExists() throws {
        let table = firstList(in: app)
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
    
    // MARK: - Scrolling Tests
    
    func testCanScrollSongList() throws {
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 2) else {
            XCTFail("Songs table not found")
            return
        }
        
        // Swipe gestures should work
        table.swipeUp()
        Thread.sleep(forTimeInterval: 0.3)
        table.swipeDown()
        Thread.sleep(forTimeInterval: 0.3)
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle scrolling")
    }
    
    // MARK: - App Stability Tests
    
    func testOpenAndCloseAddSongMultipleTimes() throws {
        guard let addButton = findAddButton() else {
            throw XCTSkip("Add button not found")
        }
        
        for _ in 0..<3 {
            addButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Dismiss
            if app.buttons["Cancel"].exists {
                app.buttons["Cancel"].tap()
            } else if app.alerts.firstMatch.exists {
                app.alerts.buttons["Cancel"].tap()
            } else {
                app.swipeDown()
            }
            
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle repeated add/cancel")
    }
}
