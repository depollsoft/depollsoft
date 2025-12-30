import XCTest

/**
 * UI Tests for the Pitch Perfect iOS app.
 * Tests pitch pipe interaction, navigation, and core functionality.
 */
class pitchperfectUITests: XCTestCase {
    
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
        XCTAssertTrue(app.state == .runningForeground)
    }
    
    func testMainTabBarIsDisplayed() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 5))
    }
    
    // MARK: - Tab Navigation Tests
    
    func testPitchPipeTabExists() throws {
        let pitchPipeTab = app.tabBars.buttons["Pitch Pipe"]
        XCTAssertTrue(pitchPipeTab.exists)
    }
    
    func testNotesTabExists() throws {
        let notesTab = app.tabBars.buttons["Notes"]
        XCTAssertTrue(notesTab.exists)
    }
    
    func testKeysTabExists() throws {
        let keysTab = app.tabBars.buttons["Keys"]
        XCTAssertTrue(keysTab.exists)
    }
    
    func testSongsTabExists() throws {
        let songsTab = app.tabBars.buttons["Songs"]
        XCTAssertTrue(songsTab.exists)
    }
    
    func testNavigateToPitchPipeTab() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        // Verify pitch pipe view is displayed
        let pitchPipeView = app.otherElements["pitchPipeView"]
        XCTAssertTrue(pitchPipeView.waitForExistence(timeout: 2) || app.buttons["C"].exists)
    }
    
    func testNavigateToNotesTab() throws {
        app.tabBars.buttons["Notes"].tap()
        // Verify notes list is displayed
        let notesList = app.tables["notesList"]
        XCTAssertTrue(notesList.waitForExistence(timeout: 2) || app.tables.firstMatch.exists)
    }
    
    func testNavigateToKeysTab() throws {
        app.tabBars.buttons["Keys"].tap()
        // Verify keys list is displayed
        let keysList = app.tables["keysList"]
        XCTAssertTrue(keysList.waitForExistence(timeout: 2) || app.tables.firstMatch.exists)
    }
    
    func testNavigateToSongsTab() throws {
        app.tabBars.buttons["Songs"].tap()
        // Verify songs list is displayed
        let songsList = app.tables["songsList"]
        XCTAssertTrue(songsList.waitForExistence(timeout: 2) || app.tables.firstMatch.exists)
    }
    
    func testNavigateBetweenAllTabs() throws {
        let tabs = ["Pitch Pipe", "Notes", "Keys", "Songs"]
        for tab in tabs {
            app.tabBars.buttons[tab].tap()
            // Small delay to allow navigation
            Thread.sleep(forTimeInterval: 0.3)
        }
        // Should be on Songs tab now
        XCTAssertTrue(app.tabBars.buttons["Songs"].isSelected)
    }
    
    // MARK: - Pitch Pipe Tests
    
    func testNoteButtonCExists() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteC = app.buttons["C"]
        XCTAssertTrue(noteC.waitForExistence(timeout: 2))
    }
    
    func testAllNoteButtonsExist() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let notes = ["C", "D", "E", "F", "G", "A", "B"]
        for note in notes {
            let noteButton = app.buttons[note]
            XCTAssertTrue(noteButton.exists, "Note button \(note) should exist")
        }
    }
    
    func testTapNoteC() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteC = app.buttons["C"]
        XCTAssertTrue(noteC.waitForExistence(timeout: 2))
        noteC.tap()
        // Note should be selected/playing
        Thread.sleep(forTimeInterval: 0.3)
    }
    
    func testTapNoteD() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteD = app.buttons["D"]
        XCTAssertTrue(noteD.waitForExistence(timeout: 2))
        noteD.tap()
    }
    
    func testTapNoteE() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteE = app.buttons["E"]
        XCTAssertTrue(noteE.waitForExistence(timeout: 2))
        noteE.tap()
    }
    
    func testTapNoteF() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteF = app.buttons["F"]
        XCTAssertTrue(noteF.waitForExistence(timeout: 2))
        noteF.tap()
    }
    
    func testTapNoteG() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteG = app.buttons["G"]
        XCTAssertTrue(noteG.waitForExistence(timeout: 2))
        noteG.tap()
    }
    
    func testTapNoteA() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteA = app.buttons["A"]
        XCTAssertTrue(noteA.waitForExistence(timeout: 2))
        noteA.tap()
    }
    
    func testTapNoteB() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteB = app.buttons["B"]
        XCTAssertTrue(noteB.waitForExistence(timeout: 2))
        noteB.tap()
    }
    
    func testTapMultipleNotes() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Tap C, E, G (C major chord)
        app.buttons["C"].tap()
        Thread.sleep(forTimeInterval: 0.2)
        app.buttons["E"].tap()
        Thread.sleep(forTimeInterval: 0.2)
        app.buttons["G"].tap()
        Thread.sleep(forTimeInterval: 0.3)
    }
    
    func testToggleNoteOnAndOff() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        let noteC = app.buttons["C"]
        XCTAssertTrue(noteC.waitForExistence(timeout: 2))
        
        // Tap to play
        noteC.tap()
        Thread.sleep(forTimeInterval: 0.3)
        
        // Tap again to stop
        noteC.tap()
        Thread.sleep(forTimeInterval: 0.3)
    }
    
    // MARK: - Range Toggle Tests
    
    func testRangeToggleExists() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        // Look for range toggle control
        let rangeControl = app.segmentedControls.firstMatch
        XCTAssertTrue(rangeControl.waitForExistence(timeout: 2) || app.buttons["C-C"].exists || app.buttons["F-F"].exists)
    }
    
    func testSwitchToCToC() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        if let rangeButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'C-C' OR label CONTAINS 'C to C'")).firstMatch as? XCUIElement, rangeButton.exists {
            rangeButton.tap()
        } else if app.segmentedControls.firstMatch.exists {
            app.segmentedControls.buttons["C-C"].tap()
        }
    }
    
    func testSwitchToFToF() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        if let rangeButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'F-F' OR label CONTAINS 'F to F'")).firstMatch as? XCUIElement, rangeButton.exists {
            rangeButton.tap()
        } else if app.segmentedControls.firstMatch.exists {
            app.segmentedControls.buttons["F-F"].tap()
        }
    }
    
    // MARK: - Settings Tests
    
    func testSettingsButtonExists() throws {
        let settingsButton = app.navigationBars.buttons["Settings"]
        if !settingsButton.exists {
            // Try gear icon
            let gearButton = app.navigationBars.buttons["gear"]
            XCTAssertTrue(gearButton.exists || app.buttons["Settings"].exists)
        }
    }
    
    func testOpenSettings() throws {
        // Find and tap settings button
        if app.navigationBars.buttons["Settings"].exists {
            app.navigationBars.buttons["Settings"].tap()
        } else if app.navigationBars.buttons["gear"].exists {
            app.navigationBars.buttons["gear"].tap()
        } else if app.buttons["Settings"].exists {
            app.buttons["Settings"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should see settings view
        let settingsView = app.navigationBars["Settings"]
        XCTAssertTrue(settingsView.waitForExistence(timeout: 2) || app.staticTexts["Settings"].exists)
    }
    
    // MARK: - Keys Screen Tests
    
    func testKeyListDisplaysItems() throws {
        app.tabBars.buttons["Keys"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let table = app.tables.firstMatch
        XCTAssertTrue(table.exists)
        XCTAssertTrue(table.cells.count > 0)
    }
    
    func testSelectCMajorKey() throws {
        app.tabBars.buttons["Keys"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let cMajorCell = app.cells.containing(NSPredicate(format: "label CONTAINS 'C Major' OR label CONTAINS 'C'")).firstMatch
        if cMajorCell.exists {
            cMajorCell.tap()
        }
    }
    
    func testSelectGMajorKey() throws {
        app.tabBars.buttons["Keys"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let gMajorCell = app.cells.containing(NSPredicate(format: "label CONTAINS 'G Major' OR label CONTAINS 'G'")).firstMatch
        if gMajorCell.exists {
            gMajorCell.tap()
        }
    }
    
    func testMajorMinorToggle() throws {
        app.tabBars.buttons["Keys"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Look for major/minor toggle
        let toggleButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Major' OR label CONTAINS 'Minor'")).firstMatch
        if toggleButton.exists {
            toggleButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
            toggleButton.tap()
        }
    }
    
    // MARK: - Songs Screen Tests
    
    func testSongListDisplays() throws {
        app.tabBars.buttons["Songs"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let table = app.tables.firstMatch
        XCTAssertTrue(table.waitForExistence(timeout: 2))
    }
    
    func testAddSongButtonExists() throws {
        app.tabBars.buttons["Songs"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        let addButton = app.navigationBars.buttons["Add"]
        if !addButton.exists {
            let plusButton = app.buttons["+"]
            XCTAssertTrue(plusButton.exists || app.buttons["add"].exists)
        } else {
            XCTAssertTrue(addButton.exists)
        }
    }
    
    func testTapAddSongButton() throws {
        app.tabBars.buttons["Songs"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Find and tap add button
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        } else if app.buttons["add"].exists {
            app.buttons["add"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should show add song form
    }
    
    // MARK: - Performance Tests
    
    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
    
    // MARK: - Accessibility Tests
    
    func testPitchPipeAccessibility() throws {
        app.tabBars.buttons["Pitch Pipe"].tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Verify note buttons have accessibility labels
        let noteC = app.buttons["C"]
        XCTAssertTrue(noteC.exists)
        XCTAssertTrue(noteC.isAccessibilityElement)
    }
    
    func testTabBarAccessibility() throws {
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.exists)
        XCTAssertTrue(tabBar.isAccessibilityElement || tabBar.buttons.count > 0)
    }
}
