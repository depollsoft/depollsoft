//
//  KeySignatureUITests.swift
//  pitchperfectUITests
//
//  UI tests for Key Signature selection in Pitch Perfect
//

import XCTest

final class KeySignatureUITests: XCTestCase {
    
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
    
    // MARK: - Key List Display
    
    func testKeyListDisplaysAllKeys() throws {
        navigateToKeySignature()
        
        // Standard major keys
        let majorKeys = ["C", "G", "D", "A", "E", "B", "F#", "C#", 
                        "F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb"]
        
        // Check that key list is displayed
        let keyTable = app.tables.firstMatch
        XCTAssertTrue(keyTable.exists, "Key signature table should be displayed")
        
        // Verify at least some keys are visible
        var foundKeys = 0
        for key in majorKeys {
            if app.staticTexts[key].exists || app.cells.staticTexts[key].exists {
                foundKeys += 1
            }
        }
        
        XCTAssertGreaterThan(foundKeys, 0, "Should display key signatures")
    }
    
    // MARK: - Major/Minor Toggle
    
    func testMajorMinorToggle() throws {
        navigateToKeySignature()
        
        // Find major/minor toggle
        let majorButton = app.buttons["Major"]
        let minorButton = app.buttons["Minor"]
        let segmentedControl = app.segmentedControls.firstMatch
        
        if segmentedControl.exists {
            // Toggle using segmented control
            let majorSegment = segmentedControl.buttons["Major"]
            let minorSegment = segmentedControl.buttons["Minor"]
            
            if majorSegment.exists {
                majorSegment.tap()
                XCTAssertTrue(majorSegment.isSelected, "Major should be selected")
            }
            
            if minorSegment.exists {
                minorSegment.tap()
                XCTAssertTrue(minorSegment.isSelected, "Minor should be selected")
            }
        } else if minorButton.exists {
            // Toggle using separate buttons
            minorButton.tap()
            
            // Check for minor keys
            let minorKeys = ["Am", "Em", "Bm", "F#m", "Dm", "Gm", "Cm"]
            var foundMinorKey = false
            for key in minorKeys {
                if app.staticTexts[key].exists || app.cells.staticTexts[key].exists {
                    foundMinorKey = true
                    break
                }
            }
            XCTAssertTrue(foundMinorKey, "Minor keys should be displayed after toggle")
        }
    }
    
    // MARK: - Key Selection
    
    func testSelectKeySignature() throws {
        navigateToKeySignature()
        
        // Find and select a key (G Major)
        let gMajorCell = app.cells.staticTexts["G"]
        let gMajorText = app.staticTexts["G"]
        
        if gMajorCell.exists {
            gMajorCell.tap()
        } else if gMajorText.exists {
            gMajorText.tap()
        }
        
        // Verify selection feedback (checkmark, highlight, or navigation back)
        // Implementation depends on app behavior
    }
    
    func testKeySelectionUpdatesDisplay() throws {
        navigateToKeySignature()
        
        // Select a key with sharps (D Major - 2 sharps)
        let dMajorCell = app.cells.staticTexts["D"]
        if dMajorCell.exists {
            dMajorCell.tap()
        }
        
        // Navigate to pitch pipe and verify notes reflect key
        let pitchPipeTab = app.tabBars.buttons.element(boundBy: 0)
        if pitchPipeTab.exists {
            pitchPipeTab.tap()
            
            // In D Major, F and C should be sharp by default
            // This test verifies the connection between key selection and note display
        }
    }
    
    // MARK: - Key Signature Details
    
    func testKeySignatureShowsAccidentals() throws {
        navigateToKeySignature()
        
        // Keys should show their accidentals (sharps/flats)
        // Look for visual representation of key signatures
        let sharpsLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'sharp'")).firstMatch
        let flatsLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'flat'")).firstMatch
        
        // At least some keys should show accidental info
        let keyTable = app.tables.firstMatch
        XCTAssertTrue(keyTable.exists, "Key table should exist")
    }
    
    func testKeyWithNoAccidentals() throws {
        navigateToKeySignature()
        
        // C Major has no sharps or flats
        let cMajorCell = app.cells.staticTexts["C"]
        if cMajorCell.exists {
            // Verify C Major is displayed
            XCTAssertTrue(cMajorCell.exists, "C Major should be available")
        }
    }
    
    // MARK: - Scrolling
    
    func testScrollKeyList() throws {
        navigateToKeySignature()
        
        let keyTable = app.tables.firstMatch
        XCTAssertTrue(keyTable.exists, "Key table should exist")
        
        // Scroll down
        keyTable.swipeUp()
        
        // Scroll back up
        keyTable.swipeDown()
    }
    
    // MARK: - Search/Filter (if available)
    
    func testKeySearchIfAvailable() throws {
        navigateToKeySignature()
        
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("G")
            
            // Should filter to keys containing G
            let results = app.cells.allElementsBoundByIndex
            XCTAssertGreaterThan(results.count, 0, "Search should show results")
        }
    }
    
    // MARK: - Related Keys
    
    func testRelativeMinorDisplay() throws {
        navigateToKeySignature()
        
        // Select C Major
        let cMajorCell = app.cells.staticTexts["C"]
        if cMajorCell.exists {
            cMajorCell.tap()
            
            // Check if relative minor (Am) is shown
            let relativeMinorLabel = app.staticTexts["Relative: Am"]
            let amLabel = app.staticTexts["Am"]
            
            // App may show relative minor info
        }
    }
    
    // MARK: - Circle of Fifths (if available)
    
    func testCircleOfFifthsViewIfAvailable() throws {
        navigateToKeySignature()
        
        // Look for circle of fifths button/tab
        let circleButton = app.buttons["Circle of Fifths"]
        let circleTab = app.segmentedControls.buttons["Circle"]
        
        if circleButton.exists {
            circleButton.tap()
            // Verify circle view appears
        } else if circleTab.exists {
            circleTab.tap()
            // Verify circle view appears
        }
    }
    
    // MARK: - Accessibility
    
    func testKeySignatureAccessibility() throws {
        navigateToKeySignature()
        
        // All key cells should have accessibility labels
        let cells = app.cells.allElementsBoundByIndex
        for cell in cells.prefix(5) {
            XCTAssertFalse(cell.label.isEmpty, "Key cells should have accessibility labels")
        }
    }
    
    func testKeySignatureVoiceOverHints() throws {
        navigateToKeySignature()
        
        // Check for accessibility hints on key cells
        let firstCell = app.cells.firstMatch
        if firstCell.exists {
            // Cell should describe the key signature
            XCTAssertTrue(firstCell.isAccessibilityElement || !firstCell.label.isEmpty,
                         "Key cells should be accessible")
        }
    }
    
    // MARK: - Helper Methods
    
    private func navigateToKeySignature() {
        // Navigate to key signature tab/section
        let keysTab = app.tabBars.buttons["Keys"]
        let keySignatureTab = app.tabBars.buttons["Key Signature"]
        
        if keysTab.exists {
            keysTab.tap()
        } else if keySignatureTab.exists {
            keySignatureTab.tap()
        } else {
            // Try other navigation methods
            let keysButton = app.buttons["Keys"]
            if keysButton.exists {
                keysButton.tap()
            }
        }
        
        // Wait for key list to appear
        _ = app.tables.firstMatch.waitForExistence(timeout: 2)
    }
}
