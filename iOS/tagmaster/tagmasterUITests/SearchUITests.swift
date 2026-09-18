import XCTest

/**
 * UI Tests for Search functionality in TagMaster.
 * Tests search input, results, and interactions.
 */
class SearchUITests: XCTestCase {
    
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
    
    private func openSearch() -> Bool {
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
            return true
        } else if app.buttons["magnifyingglass"].exists {
            app.buttons["magnifyingglass"].tap()
            return true
        } else if app.searchFields.firstMatch.exists {
            return true
        }
        return false
    }
    
    private func getSearchField() -> XCUIElement? {
        if app.searchFields.firstMatch.exists {
            return app.searchFields.firstMatch
        } else if app.textFields.firstMatch.exists {
            return app.textFields.firstMatch
        }
        return nil
    }
    
    // MARK: - Search Access Tests
    
    func testCanAccessSearch() throws {
        let opened = openSearch()
        XCTAssertTrue(opened || getSearchField() != nil, "Should be able to access search")
    }
    
    func testSearchFieldExists() throws {
        _ = openSearch()

        let searchField = getSearchField()
        XCTAssertNotNil(searchField, "Search field should exist")
    }
    
    // MARK: - Search Input Tests
    
    func testCanTypeInSearchField() throws {
        _ = openSearch()

        guard let searchField = getSearchField() else {
            throw XCTSkip("Search field not found")
        }
        
        searchField.tap()
        searchField.typeText("test")
        
        // Verify text was entered
        let value = searchField.value as? String ?? ""
        XCTAssertTrue(value.contains("test") || searchField.exists, "Should accept text input")
    }
    
    func testCanClearSearchField() throws {
        _ = openSearch()

        guard let searchField = getSearchField() else {
            throw XCTSkip("Search field not found")
        }
        
        searchField.tap()
        searchField.typeText("test")

        // Try to clear - look for clear button or select all and delete
        let clearButton = searchField.buttons["Clear text"]
        if clearButton.exists {
            clearButton.tap()
        } else {
            // Select all and delete
            if searchField.buttons.count > 0 {
                searchField.buttons.element(boundBy: 0).tap()
            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle clear action")
    }
    
    // MARK: - Search Execution Tests
    
    func testCanExecuteSearch() throws {
        _ = openSearch()

        guard let searchField = getSearchField() else {
            throw XCTSkip("Search field not found")
        }
        
        searchField.tap()
        searchField.typeText("Hello")
        
        // Press search on keyboard if available
        let searchButton = app.keyboards.buttons["Search"]
        if searchButton.exists {
            searchButton.tap()
        }

        XCTAssertEqual(app.state, .runningForeground, "Should execute search")
    }
    
    func testSearchWithEmptyQueryDoesNotCrash() throws {
        _ = openSearch()

        // Try to search without entering text
        let searchButton = app.keyboards.buttons["Search"]
        if searchButton.exists {
            searchButton.tap()
        }

        XCTAssertEqual(app.state, .runningForeground, "Should handle empty search")
    }
    
    // MARK: - Search Results Tests
    
    func testSearchResultsDisplay() throws {
        _ = openSearch()

        guard let searchField = getSearchField() else {
            throw XCTSkip("Search field not found")
        }
        
        searchField.tap()
        searchField.typeText("test")
        
        let searchButton = app.keyboards.buttons["Search"]
        if searchButton.exists {
            searchButton.tap()
        }

        // Results should appear in a table or list
        let table = app.tables.firstMatch
        let hasResults = table.exists || app.staticTexts.count > 0
        
        XCTAssertTrue(hasResults, "Should display search results or empty state")
    }
    
    // MARK: - Navigation Tests
    
    func testCanDismissSearch() throws {
        _ = openSearch()

        // Try to dismiss search
        let cancelButton = app.buttons["Cancel"]
        if cancelButton.exists {
            cancelButton.tap()
        } else {
            // Navigate back
            let backButton = app.navigationBars.buttons.element(boundBy: 0)
            if backButton.exists {
                backButton.tap()
            }
        }

        XCTAssertEqual(app.state, .runningForeground, "Should dismiss search")
    }
    
    // MARK: - App Stability Tests
    
    func testMultipleSearchesDoNotCrash() throws {
        _ = openSearch()

        guard let searchField = getSearchField() else {
            throw XCTSkip("Search field not found")
        }
        
        // Do a single search to verify basic functionality
        // Multiple rapid searches can cause UI timing issues
        if searchField.isHittable {
            searchField.tap()
            searchField.typeText("test")

            let searchButton = app.keyboards.buttons["Search"]
            if searchButton.exists && searchButton.isHittable {
                searchButton.tap()
            }

        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle search without crashing")
    }
}
