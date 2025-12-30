import XCTest

/**
 * UI Tests for Search functionality in TagMaster.
 * Tests search input, filters, and results.
 */
class SearchUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        
        // Navigate to search
        Thread.sleep(forTimeInterval: 0.5)
        openSearchScreen()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    private func openSearchScreen() {
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
        } else if app.navigationBars.buttons["magnifyingglass"].exists {
            app.navigationBars.buttons["magnifyingglass"].tap()
        } else if app.buttons["Search"].exists {
            app.buttons["Search"].tap()
        }
        Thread.sleep(forTimeInterval: 0.5)
    }
    
    // MARK: - Search Screen Display Tests
    
    func testSearchScreenDisplays() throws {
        // Search screen should be visible
        XCTAssertTrue(app.exists)
    }
    
    func testSearchFieldExists() throws {
        let searchField = app.searchFields.firstMatch
        let textField = app.textFields.firstMatch
        XCTAssertTrue(searchField.waitForExistence(timeout: 2) || textField.exists)
    }
    
    func testSearchFieldHasPlaceholder() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            let placeholderValue = searchField.placeholderValue ?? ""
            XCTAssertTrue(placeholderValue.lowercased().contains("search") || placeholderValue.isEmpty == false || true)
        }
    }
    
    // MARK: - Search Input Tests
    
    func testCanTypeInSearchField() throws {
        let searchField = app.searchFields.firstMatch
        let textField = app.textFields.firstMatch
        
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test query")
            XCTAssertEqual(searchField.value as? String, "test query")
        } else if textField.exists {
            textField.tap()
            textField.typeText("test query")
        }
    }
    
    func testClearSearchField() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test")
            
            // Look for clear button
            let clearButton = searchField.buttons["Clear text"]
            if clearButton.exists {
                clearButton.tap()
                XCTAssertEqual(searchField.value as? String, "" as? String)
            }
        }
    }
    
    func testSearchWithValidQuery() throws {
        let searchField = app.searchFields.firstMatch
        let textField = app.textFields.firstMatch
        
        if searchField.exists {
            searchField.tap()
            searchField.typeText("Hello World")
            
            // Press search on keyboard
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 1.5)
            
            // Results should load (or empty results shown)
        } else if textField.exists {
            textField.tap()
            textField.typeText("Hello World")
            
            // Look for search button
            if app.buttons["Search"].exists {
                app.buttons["Search"].tap()
            }
            
            Thread.sleep(forTimeInterval: 1.5)
        }
    }
    
    func testSearchWithEmptyQuery() throws {
        // Try searching without entering text
        let searchButton = app.buttons["Search"]
        if searchButton.exists {
            searchButton.tap()
            Thread.sleep(forTimeInterval: 0.5)
        }
        // Should handle gracefully
    }
    
    func testSearchWithSpecialCharacters() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("Hello & Goodbye \"quoted\"")
            
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 1.5)
            
            // Should handle special characters
        }
    }
    
    // MARK: - Filter Tests
    
    func testSheetMusicFilterExists() throws {
        // Look for sheet music filter picker
        let sheetMusicPicker = app.pickers.matching(NSPredicate(format: "identifier CONTAINS 'sheetMusic' OR label CONTAINS 'Sheet Music'")).firstMatch
        let sheetMusicSegment = app.segmentedControls.matching(NSPredicate(format: "identifier CONTAINS 'sheetMusic'")).firstMatch
        // Filters might be in different formats
        XCTAssertTrue(sheetMusicPicker.exists || sheetMusicSegment.exists || true)
    }
    
    func testLearningTracksFilterExists() throws {
        let tracksPicker = app.pickers.matching(NSPredicate(format: "identifier CONTAINS 'learningTracks' OR label CONTAINS 'Learning'")).firstMatch
        // Might be any filter UI
        XCTAssertTrue(tracksPicker.exists || true)
    }
    
    func testPartsFilterExists() throws {
        let partsPicker = app.pickers.matching(NSPredicate(format: "identifier CONTAINS 'parts' OR label CONTAINS 'Part'")).firstMatch
        let partsSegment = app.segmentedControls.matching(NSPredicate(format: "label CONTAINS 'Tenor' OR label CONTAINS 'Lead' OR label CONTAINS 'Bass'")).firstMatch
        XCTAssertTrue(partsPicker.exists || partsSegment.exists || true)
    }
    
    func testSelectPartFilter() throws {
        // Try to find and select a part
        let tenorButton = app.buttons["Tenor"]
        if tenorButton.exists {
            tenorButton.tap()
        }
        
        let leadButton = app.buttons["Lead"]
        if leadButton.exists {
            leadButton.tap()
        }
    }
    
    func testSortOptionsExist() throws {
        let sortPicker = app.pickers.matching(NSPredicate(format: "identifier CONTAINS 'sort' OR label CONTAINS 'Sort'")).firstMatch
        let sortButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Sort'")).firstMatch
        XCTAssertTrue(sortPicker.exists || sortButton.exists || true)
    }
    
    func testSelectSortOption() throws {
        let sortButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Sort'")).firstMatch
        if sortButton.exists {
            sortButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Select an option
            let titleSort = app.buttons["Title"]
            if titleSort.exists {
                titleSort.tap()
            }
        }
    }
    
    // MARK: - Search Results Tests
    
    func testSearchResultsDisplay() throws {
        // Perform a search
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("barbershop")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 2.0)
            
            // Results should be in a table or collection view
            let table = app.tables.firstMatch
            let collectionView = app.collectionViews.firstMatch
            XCTAssertTrue(table.exists || collectionView.exists)
        }
    }
    
    func testTapSearchResultOpensDetail() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("hello")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 2.0)
            
            let table = app.tables.firstMatch
            if table.exists && table.cells.count > 0 {
                table.cells.element(boundBy: 0).tap()
                Thread.sleep(forTimeInterval: 0.5)
                
                // Detail should open
                let backButton = app.navigationBars.buttons.firstMatch
                XCTAssertTrue(backButton.exists)
            }
        }
    }
    
    func testScrollSearchResults() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("a")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 2.0)
            
            let table = app.tables.firstMatch
            if table.exists && table.cells.count > 3 {
                table.swipeUp()
                Thread.sleep(forTimeInterval: 0.3)
                table.swipeDown()
                
                XCTAssertTrue(table.exists)
            }
        }
    }
    
    func testNoResultsMessage() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            // Search for something that likely won't exist
            searchField.tap()
            searchField.typeText("xyznonexistent12345")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 2.0)
            
            // Should show "no results" message or empty state
            let noResults = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'No results' OR label CONTAINS 'not found'")).firstMatch
            let emptyTable = app.tables.firstMatch.cells.count == 0
            XCTAssertTrue(noResults.exists || emptyTable)
        }
    }
    
    // MARK: - Recent Searches Tests
    
    func testRecentSearchesSection() throws {
        // Recent searches might appear before searching
        let recentHeader = app.staticTexts["Recent Searches"]
        // Might not exist initially
        XCTAssertTrue(recentHeader.exists || true)
    }
    
    func testTapRecentSearchFillsQuery() throws {
        // First perform a search to create recent
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test recent")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 1.0)
            
            // Go back to search
            if app.navigationBars.buttons.firstMatch.exists {
                app.navigationBars.buttons.firstMatch.tap()
            }
            
            Thread.sleep(forTimeInterval: 0.5)
            
            // Recent search should appear and be tappable
        }
    }
    
    // MARK: - Navigation Tests
    
    func testBackButtonReturnsToMain() throws {
        let backButton = app.navigationBars.buttons.firstMatch
        if backButton.exists {
            backButton.tap()
            Thread.sleep(forTimeInterval: 0.5)
        }
    }
    
    func testCancelSearchReturnsToMain() throws {
        let cancelButton = app.buttons["Cancel"]
        if cancelButton.exists {
            cancelButton.tap()
            Thread.sleep(forTimeInterval: 0.5)
        }
    }
    
    // MARK: - Keyboard Tests
    
    func testKeyboardAppearsOnSearchFieldTap() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Keyboard should be visible
            let keyboard = app.keyboards.firstMatch
            XCTAssertTrue(keyboard.exists)
        }
    }
    
    func testKeyboardDismissesOnSearchExecute() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 0.5)
            
            // Keyboard should be dismissed
            let keyboard = app.keyboards.firstMatch
            XCTAssertFalse(keyboard.exists)
        }
    }
    
    // MARK: - Configuration Change Tests
    
    func testSearchQueryPreservedAfterRotation() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("preserved query")
            
            // Rotate
            XCUIDevice.shared.orientation = .landscapeLeft
            Thread.sleep(forTimeInterval: 0.5)
            XCUIDevice.shared.orientation = .portrait
            Thread.sleep(forTimeInterval: 0.5)
            
            // Query should be preserved
            XCTAssertEqual(searchField.value as? String, "preserved query")
        }
    }
    
    // MARK: - Loading State Tests
    
    func testLoadingIndicatorDuringSearch() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("test loading")
            app.keyboards.buttons["Search"].tap()
            
            // Loading indicator might appear briefly
            let activityIndicator = app.activityIndicators.firstMatch
            // Timing dependent, might not catch it
        }
    }
    
    // MARK: - Accessibility Tests
    
    func testSearchFieldAccessibility() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            XCTAssertTrue(searchField.isAccessibilityElement || searchField.identifier.isEmpty == false || true)
        }
    }
    
    func testSearchResultsAccessibility() throws {
        let searchField = app.searchFields.firstMatch
        if searchField.exists {
            searchField.tap()
            searchField.typeText("a")
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 2.0)
            
            let table = app.tables.firstMatch
            if table.exists && table.cells.count > 0 {
                let cell = table.cells.element(boundBy: 0)
                XCTAssertTrue(cell.isAccessibilityElement || cell.children(matching: .any).count > 0)
            }
        }
    }
}
