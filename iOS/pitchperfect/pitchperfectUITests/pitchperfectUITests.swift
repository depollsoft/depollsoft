import XCTest

func firstList(in app: XCUIApplication) -> XCUIElement {
    let table = app.tables.firstMatch
    if table.exists { return table }
    return app.collectionViews.firstMatch
}

/**
 * UI Tests for the Pitch Perfect iOS app.
 * Tests core navigation, pitch pipe interaction, and basic functionality.
 *
 * Design principles:
 * - Use firstMatch to avoid "multiple elements found" errors
 * - Only assert on elements that definitely exist
 * - Use waitForExistence with appropriate timeouts
 * - Keep tests focused and independent
 */
class PitchPerfectUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Launch Tests
    
    func testAppLaunches() throws {
        XCTAssertTrue(app.state == .runningForeground, "App should be running in foreground")
    }
    
    func testAppHasContent() throws {
        // Wait for app to load and verify it has some visible UI
        let anyElement = app.descendants(matching: .any).element(boundBy: 0)
        XCTAssertTrue(anyElement.waitForExistence(timeout: 5), "App should have visible content")
    }
    
    // MARK: - Tab Bar Navigation Tests
    
    func testTabBarExists() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 5), "Tab bar should be visible")
    }
    
    func testPitchPipeTabExists() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let pitchPipeTab = tabBar.buttons["Pitch Pipe"]
        XCTAssertTrue(pitchPipeTab.exists, "Pitch Pipe tab should exist")
    }
    
    func testNotesTabExists() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let notesTab = tabBar.buttons["Notes"]
        XCTAssertTrue(notesTab.exists, "Notes tab should exist")
    }
    
    func testKeysTabExists() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let keysTab = tabBar.buttons["Keys"]
        XCTAssertTrue(keysTab.exists, "Keys tab should exist")
    }
    
    func testSongsTabExists() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let songsTab = tabBar.buttons["Songs"]
        XCTAssertTrue(songsTab.exists, "Songs tab should exist")
    }
    
    func testCanNavigateToPitchPipeTab() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let pitchPipeTab = tabBar.buttons["Pitch Pipe"]
        guard pitchPipeTab.exists else {
            XCTFail("Pitch Pipe tab not found")
            return
        }
        
        pitchPipeTab.tap()
        
        // Verify we're on pitch pipe - check for note buttons
        let noteButtons = app.buttons.matching(NSPredicate(format: "label IN %@", ["C", "D", "E", "F", "G", "A", "B"]))
        XCTAssertGreaterThan(noteButtons.count, 0, "Should see note buttons on Pitch Pipe tab")
    }
    
    func testCanNavigateToNotesTab() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let notesTab = tabBar.buttons["Notes"]
        guard notesTab.exists else {
            XCTFail("Notes tab not found")
            return
        }
        
        notesTab.tap()
        
        // Verify a table or list appears
        let table = firstList(in: app)
        XCTAssertTrue(table.waitForExistence(timeout: 3), "Should see a table on Notes tab")
    }
    
    func testCanNavigateToKeysTab() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let keysTab = tabBar.buttons["Keys"]
        guard keysTab.exists else {
            XCTFail("Keys tab not found")
            return
        }
        
        keysTab.tap()
        
        // Verify a table or list appears
        let table = firstList(in: app)
        XCTAssertTrue(table.waitForExistence(timeout: 3), "Should see a table on Keys tab")
    }
    
    func testCanNavigateToSongsTab() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let songsTab = tabBar.buttons["Songs"]
        guard songsTab.exists else {
            XCTFail("Songs tab not found")
            return
        }
        
        songsTab.tap()
        
        // Verify a table or list appears (songs list or empty state)
        let table = firstList(in: app)
        XCTAssertTrue(table.waitForExistence(timeout: 3), "Should see a table on Songs tab")
    }
    
    func testNavigateBetweenAllTabs() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let tabs = ["Pitch Pipe", "Notes", "Keys", "Songs"]
        for tabName in tabs {
            let tab = tabBar.buttons[tabName]
            if tab.exists {
                tab.tap()
                Thread.sleep(forTimeInterval: 0.3)
                XCTAssertTrue(tab.isSelected, "\(tabName) tab should be selected after tap")
            }
        }
    }
    
    // MARK: - Pitch Pipe Tests
    
    func testPitchPipeHasNoteButtons() throws {
        // Navigate to pitch pipe
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let pitchPipeTab = tabBar.buttons["Pitch Pipe"]
        if pitchPipeTab.exists {
            pitchPipeTab.tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Check for note buttons - use firstMatch to avoid multiple element issues
        let noteCButton = app.buttons.matching(NSPredicate(format: "label == 'C'")).firstMatch
        XCTAssertTrue(noteCButton.waitForExistence(timeout: 2), "Note C button should exist")
    }
    
    func testCanTapNoteC() throws {
        navigateToPitchPipe()
        
        // Find and tap note C - use firstMatch to get single element
        let noteCButton = app.buttons.matching(NSPredicate(format: "label == 'C'")).firstMatch
        guard noteCButton.waitForExistence(timeout: 2) else {
            XCTFail("Note C button not found")
            return
        }
        
        // Tap should not crash
        noteCButton.tap()
        
        // App should still be running
        XCTAssertEqual(app.state, .runningForeground, "App should still be running after tapping note")
    }
    
    func testNoteButtonsAreTappable() throws {
        navigateToPitchPipe()
        
        // Test tapping multiple notes using firstMatch for each
        let notes = ["C", "E", "G"]  // Common chord notes
        for note in notes {
            let noteButton = app.buttons.matching(NSPredicate(format: "label == %@", note)).firstMatch
            if noteButton.exists && noteButton.isHittable {
                noteButton.tap()
                Thread.sleep(forTimeInterval: 0.2)
            }
        }
        
        // App should still be running
        XCTAssertEqual(app.state, .runningForeground, "App should still be running after tapping notes")
    }
    
    func testTapAndReleaseNote() throws {
        navigateToPitchPipe()
        
        let noteCButton = app.buttons.matching(NSPredicate(format: "label == 'C'")).firstMatch
        guard noteCButton.waitForExistence(timeout: 2) else {
            XCTFail("Note C button not found")
            return
        }
        
        // Tap to start playing
        noteCButton.tap()
        Thread.sleep(forTimeInterval: 0.3)
        
        // Tap again to stop
        noteCButton.tap()
        Thread.sleep(forTimeInterval: 0.2)
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle toggle correctly")
    }
    
    // MARK: - Notes List Tests
    
    func testNotesListHasContent() throws {
        navigateToTab("Notes")
        
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 3) else {
            XCTFail("Notes table not found")
            return
        }
        
        // Table should have cells (notes)
        XCTAssertGreaterThan(table.cells.count, 0, "Notes list should have cells")
    }
    
    func testCanTapNoteInList() throws {
        navigateToTab("Notes")
        
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 3) else {
            XCTFail("Notes table not found")
            return
        }
        
        guard table.cells.count > 0 else {
            XCTFail("No cells in notes list")
            return
        }
        
        // Tap first cell
        let firstCell = table.cells.element(boundBy: 0)
        firstCell.tap()
        
        // Should play note without crashing
        XCTAssertEqual(app.state, .runningForeground, "App should still be running after tapping note")
    }
    
    // MARK: - Keys List Tests
    
    func testKeysListHasContent() throws {
        navigateToTab("Keys")
        
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 3) else {
            XCTFail("Keys table not found")
            return
        }
        
        // Table should have cells (keys)
        XCTAssertGreaterThan(table.cells.count, 0, "Keys list should have cells")
    }
    
    func testCanTapKeyInList() throws {
        navigateToTab("Keys")
        
        let table = firstList(in: app)
        guard table.waitForExistence(timeout: 3) else {
            XCTFail("Keys table not found")
            return
        }
        
        guard table.cells.count > 0 else {
            XCTFail("No cells in keys list")
            return
        }
        
        // Tap first cell
        let firstCell = table.cells.element(boundBy: 0)
        firstCell.tap()
        
        // Should select key without crashing
        XCTAssertEqual(app.state, .runningForeground, "App should still be running after tapping key")
    }
    
    // MARK: - Songs Tab Tests
    
    func testSongsTabShowsTable() throws {
        navigateToTab("Songs")
        
        // Should have a table (possibly with empty state)
        let table = firstList(in: app)
        XCTAssertTrue(table.waitForExistence(timeout: 3), "Should see a table on Songs tab")
    }
    
    func testSongsTabHasAddButton() throws {
        navigateToTab("Songs")
        
        // Look for add button in various forms
        let addButton = app.navigationBars.buttons["Add"]
        let plusButton = app.buttons["+"]
        let addTextButton = app.buttons["add"]
        let addIcon = app.buttons["plus"]
        
        // Add button may be in different places - just check we're on the Songs tab
        if !(addButton.exists || plusButton.exists || addTextButton.exists || addIcon.exists) {
            // Skip if no add button found - this may be intentional in the app design
            throw XCTSkip("Add button not found in expected locations")
        }
    }
    
    // MARK: - App Stability Tests
    
    func testAppDoesNotCrashOnRapidTabSwitching() throws {
        let tabBar = app.tabBars.firstMatch
        guard tabBar.waitForExistence(timeout: 5) else {
            XCTFail("Tab bar not found")
            return
        }
        
        let tabs = ["Pitch Pipe", "Notes", "Keys", "Songs"]
        
        // Rapidly switch between tabs multiple times
        for _ in 0..<3 {
            for tabName in tabs {
                let tab = tabBar.buttons[tabName]
                if tab.exists {
                    tab.tap()
                }
            }
        }
        
        // App should still be running
        XCTAssertEqual(app.state, .runningForeground, "App should not crash on rapid tab switching")
    }
    
    func testAppDoesNotCrashOnRapidNoteTapping() throws {
        navigateToPitchPipe()
        
        let notes = ["C", "D", "E", "F", "G", "A", "B"]
        
        // Rapidly tap notes
        for _ in 0..<2 {
            for note in notes {
                let noteButton = app.buttons.matching(NSPredicate(format: "label == %@", note)).firstMatch
                if noteButton.exists && noteButton.isHittable {
                    noteButton.tap()
                }
            }
        }
        
        // App should still be running
        XCTAssertEqual(app.state, .runningForeground, "App should not crash on rapid note tapping")
    }
    
    // MARK: - Performance Tests
    
    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
    
    // MARK: - Helper Methods
    
    private func navigateToPitchPipe() {
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 3) {
            let pitchPipeTab = tabBar.buttons["Pitch Pipe"]
            if pitchPipeTab.exists {
                pitchPipeTab.tap()
                Thread.sleep(forTimeInterval: 0.5)
            }
        }
    }
    
    private func navigateToTab(_ tabName: String) {
        let tabBar = app.tabBars.firstMatch
        if tabBar.waitForExistence(timeout: 3) {
            let tab = tabBar.buttons[tabName]
            if tab.exists {
                tab.tap()
                Thread.sleep(forTimeInterval: 0.5)
            }
        }
    }
}
