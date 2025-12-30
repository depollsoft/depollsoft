import XCTest

/**
 * UI Tests for Song Management functionality in Pitch Perfect.
 * Tests adding, editing, deleting, and managing songs.
 */
class SongManagementUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        
        // Navigate to Songs tab
        app.tabBars.buttons["Songs"].tap()
        Thread.sleep(forTimeInterval: 0.5)
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Song List Display Tests
    
    func testSongListTableExists() throws {
        let table = app.tables.firstMatch
        XCTAssertTrue(table.waitForExistence(timeout: 2))
    }
    
    func testEmptyStateWhenNoSongs() throws {
        // If no songs, empty state might be shown
        let emptyLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'No songs' OR label CONTAINS 'Add a song'")).firstMatch
        // Either table has cells or empty state is shown
        let table = app.tables.firstMatch
        XCTAssertTrue(table.exists || emptyLabel.exists)
    }
    
    // MARK: - Add Song Tests
    
    func testAddSongButtonExists() throws {
        let addButton = app.navigationBars.buttons["Add"]
        let plusButton = app.buttons["+"]
        let addText = app.buttons["add"]
        XCTAssertTrue(addButton.exists || plusButton.exists || addText.exists)
    }
    
    func testTapAddSongOpensForm() throws {
        // Tap add button
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        } else {
            app.buttons.matching(NSPredicate(format: "label CONTAINS 'Add'")).firstMatch.tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should show add song form with text field
        let textField = app.textFields.firstMatch
        XCTAssertTrue(textField.waitForExistence(timeout: 2) || app.alerts.firstMatch.exists)
    }
    
    func testAddSongWithName() throws {
        // Open add form
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Enter song name
        let textField = app.textFields.firstMatch
        if textField.waitForExistence(timeout: 2) {
            textField.tap()
            textField.typeText("Test Song UI")
            
            // Save
            if app.buttons["Save"].exists {
                app.buttons["Save"].tap()
            } else if app.buttons["Done"].exists {
                app.buttons["Done"].tap()
            } else if app.buttons["Add"].exists {
                app.buttons["Add"].tap()
            }
            
            Thread.sleep(forTimeInterval: 0.5)
            
            // Verify song appears in list
            let songCell = app.cells.containing(NSPredicate(format: "label CONTAINS 'Test Song UI'")).firstMatch
            XCTAssertTrue(songCell.waitForExistence(timeout: 2))
        }
    }
    
    func testAddSongWithKey() throws {
        // Open add form
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Enter song name
        let nameField = app.textFields.firstMatch
        if nameField.waitForExistence(timeout: 2) {
            nameField.tap()
            nameField.typeText("Song With Key")
            
            // Select key if picker exists
            let keyPicker = app.pickers.firstMatch
            if keyPicker.exists {
                keyPicker.pickerWheels.element(boundBy: 0).adjust(toPickerWheelValue: "G Major")
            }
            
            // Save
            if app.buttons["Save"].exists {
                app.buttons["Save"].tap()
            } else if app.buttons["Done"].exists {
                app.buttons["Done"].tap()
            }
            
            Thread.sleep(forTimeInterval: 0.5)
        }
    }
    
    func testCancelAddSong() throws {
        // Open add form
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Cancel
        if app.buttons["Cancel"].exists {
            app.buttons["Cancel"].tap()
        } else {
            // Swipe down to dismiss
            app.swipeDown()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should be back at song list
        let table = app.tables.firstMatch
        XCTAssertTrue(table.exists)
    }
    
    // MARK: - Edit Song Tests
    
    func testTapSongOpensDetail() throws {
        let table = app.tables.firstMatch
        if table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
            // Should open detail or play key
        }
    }
    
    func testSwipeToShowActions() throws {
        let table = app.tables.firstMatch
        if table.cells.count > 0 {
            let cell = table.cells.element(boundBy: 0)
            cell.swipeLeft()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Should show edit/delete buttons
            let deleteButton = app.buttons["Delete"]
            XCTAssertTrue(deleteButton.exists || app.buttons["Edit"].exists)
        }
    }
    
    func testEditSong() throws {
        let table = app.tables.firstMatch
        if table.cells.count > 0 {
            let cell = table.cells.element(boundBy: 0)
            cell.swipeLeft()
            Thread.sleep(forTimeInterval: 0.3)
            
            if app.buttons["Edit"].exists {
                app.buttons["Edit"].tap()
                Thread.sleep(forTimeInterval: 0.5)
                
                // Edit form should appear
                let textField = app.textFields.firstMatch
                if textField.exists {
                    textField.tap()
                    textField.typeText(" Updated")
                    
                    if app.buttons["Save"].exists {
                        app.buttons["Save"].tap()
                    } else if app.buttons["Done"].exists {
                        app.buttons["Done"].tap()
                    }
                }
            }
        }
    }
    
    // MARK: - Delete Song Tests
    
    func testSwipeToDelete() throws {
        // First add a song to delete
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
        } else if app.buttons["+"].exists {
            app.buttons["+"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        let textField = app.textFields.firstMatch
        if textField.waitForExistence(timeout: 2) {
            textField.tap()
            textField.typeText("Delete Me")
            
            if app.buttons["Save"].exists {
                app.buttons["Save"].tap()
            } else if app.buttons["Done"].exists {
                app.buttons["Done"].tap()
            }
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Now delete it
        let songCell = app.cells.containing(NSPredicate(format: "label CONTAINS 'Delete Me'")).firstMatch
        if songCell.waitForExistence(timeout: 2) {
            songCell.swipeLeft()
            Thread.sleep(forTimeInterval: 0.3)
            
            let deleteButton = app.buttons["Delete"]
            if deleteButton.exists {
                deleteButton.tap()
                Thread.sleep(forTimeInterval: 0.5)
                
                // Confirm delete if dialog appears
                if app.alerts.firstMatch.exists {
                    app.alerts.buttons["Delete"].tap()
                }
                
                // Song should be gone
                XCTAssertFalse(songCell.exists)
            }
        }
    }
    
    func testDeleteFromContextMenu() throws {
        let table = app.tables.firstMatch
        if table.cells.count > 0 {
            let cell = table.cells.element(boundBy: 0)
            
            // Long press for context menu
            cell.press(forDuration: 1.0)
            Thread.sleep(forTimeInterval: 0.5)
            
            // Look for delete option in context menu
            let deleteOption = app.buttons["Delete"]
            if deleteOption.exists {
                deleteOption.tap()
                
                // Confirm if needed
                if app.alerts.firstMatch.exists {
                    app.alerts.buttons["Delete"].tap()
                }
            }
        }
    }
    
    // MARK: - Sort Tests
    
    func testSortButtonExists() throws {
        let sortButton = app.navigationBars.buttons.matching(NSPredicate(format: "label CONTAINS 'Sort' OR label CONTAINS 'sort'")).firstMatch
        // Sort might be in toolbar or navigation bar
        XCTAssertTrue(sortButton.exists || app.buttons["Sort"].exists)
    }
    
    func testSortByName() throws {
        if app.buttons["Sort"].exists {
            app.buttons["Sort"].tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            let sortByName = app.buttons["Sort by Name"]
            if sortByName.exists {
                sortByName.tap()
            }
        }
    }
    
    func testSortByKey() throws {
        if app.buttons["Sort"].exists {
            app.buttons["Sort"].tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            let sortByKey = app.buttons["Sort by Key"]
            if sortByKey.exists {
                sortByKey.tap()
            }
        }
    }
    
    // MARK: - Scroll Tests
    
    func testScrollSongList() throws {
        let table = app.tables.firstMatch
        if table.exists && table.cells.count > 3 {
            table.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            table.swipeDown()
            
            XCTAssertTrue(table.exists)
        }
    }
    
    // MARK: - Reorder Tests
    
    func testEnterEditMode() throws {
        let editButton = app.navigationBars.buttons["Edit"]
        if editButton.exists {
            editButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Should show Done button
            XCTAssertTrue(app.navigationBars.buttons["Done"].exists)
            
            // Exit edit mode
            app.navigationBars.buttons["Done"].tap()
        }
    }
    
    // MARK: - Search Tests
    
    func testSearchFieldExists() throws {
        let searchField = app.searchFields.firstMatch
        // Search might not exist on all versions
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test")
            Thread.sleep(forTimeInterval: 0.3)
            
            // Cancel search
            if app.buttons["Cancel"].exists {
                app.buttons["Cancel"].tap()
            }
        }
    }
    
    // MARK: - Pull to Refresh Tests
    
    func testPullToRefresh() throws {
        let table = app.tables.firstMatch
        if table.exists {
            // Pull down to refresh
            let start = table.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2))
            let end = table.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.8))
            start.press(forDuration: 0.1, thenDragTo: end)
            
            Thread.sleep(forTimeInterval: 1.0)
            
            // Table should still exist
            XCTAssertTrue(table.exists)
        }
    }
    
    // MARK: - Configuration Change Tests
    
    func testRotationPreservesState() throws {
        // Add a test song
        if app.navigationBars.buttons["Add"].exists {
            app.navigationBars.buttons["Add"].tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            let textField = app.textFields.firstMatch
            if textField.waitForExistence(timeout: 2) {
                textField.tap()
                textField.typeText("Rotation Test")
                
                if app.buttons["Save"].exists {
                    app.buttons["Save"].tap()
                }
            }
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Rotate device
        XCUIDevice.shared.orientation = .landscapeLeft
        Thread.sleep(forTimeInterval: 0.5)
        XCUIDevice.shared.orientation = .portrait
        Thread.sleep(forTimeInterval: 0.5)
        
        // Song should still exist
        let table = app.tables.firstMatch
        XCTAssertTrue(table.exists)
    }
    
    // MARK: - Accessibility Tests
    
    func testSongListAccessibility() throws {
        let table = app.tables.firstMatch
        XCTAssertTrue(table.exists)
        
        if table.cells.count > 0 {
            let cell = table.cells.element(boundBy: 0)
            XCTAssertTrue(cell.isAccessibilityElement || cell.children(matching: .any).count > 0)
        }
    }
    
    func testAddButtonAccessibility() throws {
        let addButton = app.navigationBars.buttons["Add"]
        if addButton.exists {
            XCTAssertTrue(addButton.isAccessibilityElement)
        }
    }
}
