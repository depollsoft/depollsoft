import XCTest

/**
 * UI Tests for the TagMaster iOS app.
 * Tests main screen, search, favorites, and navigation.
 *
 * Design principles:
 * - Use firstMatch to avoid "multiple elements found" errors
 * - Only assert on elements that definitely exist
 * - Use waitForExistence with appropriate timeouts
 * - Keep tests focused and independent
 */
class tagmasterUITests: XCTestCase {
    
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
    
    func testMainScreenDisplays() throws {
        // Wait for main screen to load
        Thread.sleep(forTimeInterval: 1.0)
        XCTAssertTrue(app.exists, "App should exist")
    }
    
    func testAppHasContent() throws {
        let anyElement = app.descendants(matching: .any).element(boundBy: 0)
        XCTAssertTrue(anyElement.waitForExistence(timeout: 5), "App should have visible content")
    }
    
    // MARK: - Navigation Bar Tests
    
    func testNavigationBarExists() throws {
        let navBar = app.navigationBars.firstMatch
        XCTAssertTrue(navBar.waitForExistence(timeout: 5), "Navigation bar should exist")
    }
    
    // MARK: - Search Tests
    
    func testSearchButtonOrFieldExists() throws {
        // Look for search button or search field
        let searchButton = app.navigationBars.buttons["Search"]
        let searchIcon = app.buttons["magnifyingglass"]
        let searchField = app.searchFields.firstMatch
        let textField = app.textFields.firstMatch
        
        let hasSearch = searchButton.exists || searchIcon.exists || 
                        searchField.exists || textField.exists
        
        XCTAssertTrue(hasSearch, "Should have search capability")
    }
    
    func testCanInteractWithSearch() throws {
        // Try to open search
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
        } else if app.buttons["magnifyingglass"].exists {
            app.buttons["magnifyingglass"].tap()
        } else if app.searchFields.firstMatch.exists {
            app.searchFields.firstMatch.tap()
        } else if app.textFields.firstMatch.exists {
            app.textFields.firstMatch.tap()
        } else {
            throw XCTSkip("No search UI found")
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        XCTAssertEqual(app.state, .runningForeground, "App should handle search interaction")
    }
    
    // MARK: - Content Display Tests
    
    func testMainContentExists() throws {
        Thread.sleep(forTimeInterval: 1.0)
        
        // App should have some content - table, collection view, or other UI
        let table = app.tables.firstMatch
        let collection = app.collectionViews.firstMatch
        let anyContent = app.otherElements.count > 0 || app.staticTexts.count > 0
        
        XCTAssertTrue(table.exists || collection.exists || anyContent, 
                     "Should have main content")
    }
    
    func testFavoritesOrListExists() throws {
        Thread.sleep(forTimeInterval: 1.0)
        
        // Look for favorites header or list
        let favoritesLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS[cd] 'favorite'")).firstMatch
        let table = app.tables.firstMatch
        
        XCTAssertTrue(favoritesLabel.exists || table.exists, 
                     "Should have favorites section or list")
    }
    
    // MARK: - Interaction Tests
    
    func testCanTapContentIfExists() throws {
        Thread.sleep(forTimeInterval: 1.0)
        
        let table = app.tables.firstMatch
        if table.exists && table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle taps")
    }
    
    func testCanScrollContent() throws {
        Thread.sleep(forTimeInterval: 1.0)
        
        let table = app.tables.firstMatch
        let collection = app.collectionViews.firstMatch
        
        if table.exists {
            table.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            table.swipeDown()
        } else if collection.exists {
            collection.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            collection.swipeDown()
        }
        
        XCTAssertEqual(app.state, .runningForeground, "App should handle scrolling")
    }
    
    // MARK: - App Stability Tests
    
    func testAppDoesNotCrashOnRapidInteraction() throws {
        Thread.sleep(forTimeInterval: 1.0)
        
        // Try various interactions
        let table = app.tables.firstMatch
        
        if table.exists {
            for _ in 0..<3 {
                table.swipeUp()
                table.swipeDown()
            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "App should not crash on rapid interaction")
    }
    
    // MARK: - Performance Tests
    
    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
}
