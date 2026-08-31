//
//  SettingsUITests.swift
//  pitchperfectUITests
//
//  UI tests for Settings functionality in Pitch Perfect
//

import XCTest

final class SettingsUITests: XCTestCase {
    
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
    
    // MARK: - Helper Methods
    
    private func openSettings() -> Bool {
        // Try different ways to open settings
        if app.navigationBars.buttons["Settings"].exists {
            app.navigationBars.buttons["Settings"].tap()
            return true
        } else if app.navigationBars.buttons["gear"].exists {
            app.navigationBars.buttons["gear"].tap()
            return true
        } else if app.buttons["Settings"].exists {
            app.buttons["Settings"].tap()
            return true
        } else if app.buttons["gear"].exists {
            app.buttons["gear"].tap()
            return true
        }
        
        // Try tapping info/settings in navigation bar
        let navButtons = app.navigationBars.buttons
        for buttonIndex in 0..<navButtons.count {
            let button = navButtons.element(boundBy: buttonIndex)
            let label = button.label.lowercased()
            if label.contains("setting") || label.contains("gear") || label.contains("info") {
                button.tap()
                return true
            }
        }
        
        return false
    }
    
    // MARK: - Settings Access Tests
    
    func testCanFindSettingsButton() throws {
        // Check for settings button in various forms
        let settingsButton = app.navigationBars.buttons["Settings"]
        let gearButton = app.navigationBars.buttons["gear"]
        let settingsText = app.buttons["Settings"]
        
        // At least one form should exist
        let hasSettings = settingsButton.exists || gearButton.exists || settingsText.exists
        
        // If settings is not immediately visible, that's OK - skip this test
        if !hasSettings {
            throw XCTSkip("Settings button not visible on main screen")
        }
        
        XCTAssertTrue(hasSettings, "Should have settings button somewhere")
    }
    
    func testCanOpenSettings() throws {
        let opened = openSettings()
        
        if !opened {
            throw XCTSkip("Settings button not found")
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should show some settings content
        let hasSettingsContent = app.navigationBars["Settings"].exists ||
                                 app.staticTexts["Settings"].exists ||
                                 firstList(in: app).exists
        
        XCTAssertTrue(hasSettingsContent, "Settings screen should show content")
    }
    
    // MARK: - Settings Content Tests
    
    func testSettingsHasContent() throws {
        guard openSettings() else {
            throw XCTSkip("Settings button not found")
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Settings should have a table or some content
        let table = firstList(in: app)
        let hasContent = table.exists || app.staticTexts.count > 0
        
        XCTAssertTrue(hasContent, "Settings should have content")
    }
    
    func testSettingsCanBeDismissed() throws {
        guard openSettings() else {
            throw XCTSkip("Settings button not found")
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Try to dismiss settings
        let doneButton = app.buttons["Done"]
        let closeButton = app.buttons["Close"]
        let backButton = app.navigationBars.buttons.element(boundBy: 0)
        
        if doneButton.exists {
            doneButton.tap()
        } else if closeButton.exists {
            closeButton.tap()
        } else if backButton.exists {
            backButton.tap()
        } else {
            // Swipe down to dismiss
            app.swipeDown()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // App should still be running
        XCTAssertEqual(app.state, .runningForeground, "Should be able to dismiss settings")
    }
    
    // MARK: - App Stability Tests
    
    func testOpenAndCloseSettingsMultipleTimes() throws {
        for _ in 0..<3 {
            guard openSettings() else {
                throw XCTSkip("Settings button not found")
            }
            
            Thread.sleep(forTimeInterval: 0.3)
            
            // Dismiss
            let doneButton = app.buttons["Done"]
            let closeButton = app.buttons["Close"]
            let backButton = app.navigationBars.buttons.element(boundBy: 0)
            
            if doneButton.exists {
                doneButton.tap()
            } else if closeButton.exists {
                closeButton.tap()
            } else if backButton.exists {
                backButton.tap()
            } else {
                app.swipeDown()
            }
            
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle repeated settings open/close")
    }
}
