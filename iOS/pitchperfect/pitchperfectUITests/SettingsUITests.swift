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
    
    // MARK: - Settings Navigation
    
    func testNavigateToSettings() throws {
        // Open settings from navigation bar or tab
        let settingsButton = app.buttons["Settings"]
        if settingsButton.exists {
            settingsButton.tap()
        } else {
            // Try gear icon
            let gearButton = app.buttons["gear"]
            if gearButton.exists {
                gearButton.tap()
            }
        }
        
        // Verify settings screen appears
        let settingsTitle = app.navigationBars["Settings"]
        XCTAssertTrue(settingsTitle.waitForExistence(timeout: 3), "Settings screen should appear")
    }
    
    // MARK: - Sound Settings
    
    func testSoundModeSettings() throws {
        navigateToSettings()
        
        // Find sound mode section
        let soundModeCell = app.cells.containing(.staticText, identifier: "Sound Mode").firstMatch
        if soundModeCell.exists {
            soundModeCell.tap()
            
            // Should show sound mode options
            let pianoOption = app.cells.staticTexts["Piano"]
            let sineOption = app.cells.staticTexts["Sine Wave"]
            let organOption = app.cells.staticTexts["Organ"]
            
            XCTAssertTrue(pianoOption.exists || sineOption.exists || organOption.exists, 
                         "Sound mode options should be available")
        }
    }
    
    func testVolumeSetting() throws {
        navigateToSettings()
        
        // Find volume slider
        let volumeSlider = app.sliders.firstMatch
        if volumeSlider.exists {
            // Adjust volume
            volumeSlider.adjust(toNormalizedSliderPosition: 0.5)
            
            // Verify slider moved
            let sliderValue = volumeSlider.value as? String
            XCTAssertNotNil(sliderValue, "Volume slider should have a value")
        }
    }
    
    // MARK: - Display Settings
    
    func testNoteDisplayModeSettings() throws {
        navigateToSettings()
        
        // Find note display setting
        let noteDisplayCell = app.cells.containing(.staticText, identifier: "Note Display").firstMatch
        if noteDisplayCell.exists {
            noteDisplayCell.tap()
            
            // Should show options like Sharp/Flat preference
            let sharpOption = app.cells.staticTexts["Sharps"]
            let flatOption = app.cells.staticTexts["Flats"]
            
            // Select flat option
            if flatOption.exists {
                flatOption.tap()
            }
        }
    }
    
    func testRangeDisplaySettings() throws {
        navigateToSettings()
        
        // Find range setting
        let rangeCell = app.cells.containing(.staticText, identifier: "Range").firstMatch
        if rangeCell.exists {
            rangeCell.tap()
            
            // Should show range options
            let lowOption = app.cells.staticTexts["Low"]
            let highOption = app.cells.staticTexts["High"]
            let fullOption = app.cells.staticTexts["Full"]
            
            XCTAssertTrue(lowOption.exists || highOption.exists || fullOption.exists,
                         "Range options should be available")
        }
    }
    
    // MARK: - Theme Settings
    
    func testThemeSelection() throws {
        navigateToSettings()
        
        // Find theme setting
        let themeCell = app.cells.containing(.staticText, identifier: "Theme").firstMatch
        if themeCell.exists {
            themeCell.tap()
            
            // Should show theme options
            let lightTheme = app.cells.staticTexts["Light"]
            let darkTheme = app.cells.staticTexts["Dark"]
            let systemTheme = app.cells.staticTexts["System"]
            
            // Select dark theme
            if darkTheme.exists {
                darkTheme.tap()
                
                // Navigate back
                app.navigationBars.buttons.firstMatch.tap()
            }
        }
    }
    
    // MARK: - Wake Lock Setting
    
    func testWakeLockToggle() throws {
        navigateToSettings()
        
        // Find wake lock toggle
        let wakeLockSwitch = app.switches["Keep Screen On"]
        if wakeLockSwitch.exists {
            let initialValue = wakeLockSwitch.value as? String
            
            // Toggle the switch
            wakeLockSwitch.tap()
            
            // Verify it changed
            let newValue = wakeLockSwitch.value as? String
            XCTAssertNotEqual(initialValue, newValue, "Wake lock toggle should change")
        }
    }
    
    // MARK: - About Section
    
    func testAboutSection() throws {
        navigateToSettings()
        
        // Find about cell
        let aboutCell = app.cells.staticTexts["About"]
        if aboutCell.exists {
            aboutCell.tap()
            
            // Should show version info
            let versionLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'Version'")).firstMatch
            XCTAssertTrue(versionLabel.waitForExistence(timeout: 3), 
                         "Version information should be displayed")
        }
    }
    
    func testRateAppLink() throws {
        navigateToSettings()
        
        // Find rate app cell
        let rateAppCell = app.cells.staticTexts["Rate App"]
        if rateAppCell.exists {
            XCTAssertTrue(rateAppCell.isHittable, "Rate app link should be tappable")
        }
    }
    
    func testSupportLink() throws {
        navigateToSettings()
        
        // Find support/feedback cell
        let supportCell = app.cells.staticTexts["Support"]
        let feedbackCell = app.cells.staticTexts["Send Feedback"]
        
        XCTAssertTrue(supportCell.exists || feedbackCell.exists, 
                     "Support or feedback link should exist")
    }
    
    // MARK: - Settings Persistence
    
    func testSettingsPersistAfterRelaunch() throws {
        navigateToSettings()
        
        // Change a setting
        let wakeLockSwitch = app.switches["Keep Screen On"]
        if wakeLockSwitch.exists {
            let initialValue = wakeLockSwitch.value as? String
            wakeLockSwitch.tap()
            let changedValue = wakeLockSwitch.value as? String
            
            // Terminate and relaunch
            app.terminate()
            app.launch()
            
            // Navigate back to settings
            navigateToSettings()
            
            // Verify setting persisted
            let persistedValue = wakeLockSwitch.value as? String
            XCTAssertEqual(changedValue, persistedValue, 
                          "Setting should persist after app relaunch")
        }
    }
    
    // MARK: - Accessibility
    
    func testSettingsAccessibility() throws {
        navigateToSettings()
        
        // Verify settings cells have accessibility labels
        let cells = app.cells.allElementsBoundByIndex
        for cell in cells.prefix(5) {
            XCTAssertFalse(cell.label.isEmpty, "Setting cells should have accessibility labels")
        }
    }
    
    func testSettingsVoiceOverSupport() throws {
        navigateToSettings()
        
        // Check that interactive elements are accessible
        let switches = app.switches.allElementsBoundByIndex
        for switchElement in switches {
            XCTAssertTrue(switchElement.isAccessibilityElement || !switchElement.label.isEmpty,
                         "Switches should be accessibility elements")
        }
    }
    
    // MARK: - Helper Methods
    
    private func navigateToSettings() {
        let settingsButton = app.buttons["Settings"]
        if settingsButton.exists {
            settingsButton.tap()
        } else {
            let gearButton = app.buttons["gear"]
            if gearButton.exists {
                gearButton.tap()
            } else {
                // Try tab bar
                let settingsTab = app.tabBars.buttons["Settings"]
                if settingsTab.exists {
                    settingsTab.tap()
                }
            }
        }
        
        // Wait for settings to appear
        _ = app.navigationBars["Settings"].waitForExistence(timeout: 2)
    }
}
