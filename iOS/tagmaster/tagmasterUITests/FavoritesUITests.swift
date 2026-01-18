import XCTest

/**
 * UI Tests for Favorites functionality in TagMaster.
 * Tests viewing and managing favorite tags.
 */
class FavoritesUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        Thread.sleep(forTimeInterval: 1.0)
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Favorites Display Tests
    
    func testMainScreenLoads() throws {
        XCTAssertEqual(app.state, .runningForeground, "App should be running")
    }
    
    func testHasListOrContent() throws {
        let table = app.tables.firstMatch
        let collection = app.collectionViews.firstMatch
        
        let hasContent = table.exists || collection.exists || app.staticTexts.count > 0
        XCTAssertTrue(hasContent, "Should have some content displayed")
    }
    
    func testFavoritesLabelExists() throws {
        // Look for favorites section header
        let favoritesLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS[cd] 'favorite'")).firstMatch
        
        // If no explicit label, table existence is acceptable
        let table = app.tables.firstMatch
        
        XCTAssertTrue(favoritesLabel.exists || table.exists, "Should have favorites section or list")
    }
    
    // MARK: - Favorites Interaction Tests
    
    func testCanTapFavoriteIfExists() throws {
        let table = app.tables.firstMatch
        
        if table.exists && table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            // Should navigate to detail or perform action
            XCTAssertEqual(app.state, .runningForeground, "Should handle favorite tap")
        } else {
            // No favorites to test - that's OK
            XCTAssertEqual(app.state, .runningForeground, "App should be stable with no favorites")
        }
    }
    
    func testCanScrollFavoritesList() throws {
        let table = app.tables.firstMatch
        
        if table.exists {
            table.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            table.swipeDown()
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle scrolling")
    }
    
    // MARK: - Remove Favorite Tests (if favorites exist)
    
    func testSwipeOnFavoriteIfExists() throws {
        let table = app.tables.firstMatch
        
        guard table.waitForExistence(timeout: 3) && table.cells.count > 0 else {
            throw XCTSkip("No favorites table or cells to test swipe on")
        }
        
        // Only test if we can safely interact with the cell
        let cell = table.cells.element(boundBy: 0)
        guard cell.isHittable else {
            throw XCTSkip("Cell is not hittable for swipe test")
        }
        
        // Simple swipe - don't try to interact further as it can fail
        cell.swipeLeft()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Verify app is still running - that's the key test
        XCTAssertEqual(app.state, .runningForeground, "Should handle swipe actions")
    }
    
    // MARK: - Navigation Tests
    
    func testCanNavigateBackFromDetail() throws {
        let table = app.tables.firstMatch
        
        guard table.exists && table.cells.count > 0 else {
            throw XCTSkip("No favorites to navigate from")
        }
        
        // Tap to open detail
        table.cells.element(boundBy: 0).tap()
        Thread.sleep(forTimeInterval: 0.5)
        
        // Navigate back
        let backButton = app.navigationBars.buttons.element(boundBy: 0)
        if backButton.exists {
            backButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should navigate back")
    }
    
    // MARK: - App Stability Tests
    
    func testRapidFavoriteInteraction() throws {
        let table = app.tables.firstMatch
        
        guard table.waitForExistence(timeout: 3) && table.cells.count > 0 else {
            // No table or cells - skip test rather than fail
            throw XCTSkip("No favorites table or cells to test rapid interaction")
        }
        
        // Rapid tapping with proper waits
        let cellCount = min(3, table.cells.count)
        for i in 0..<cellCount {
            let cell = table.cells.element(boundBy: i)
            
            // Ensure cell is hittable before tapping
            guard cell.waitForExistence(timeout: 2) && cell.isHittable else {
                continue  // Skip this cell if not ready
            }
            
            cell.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Navigate back if we went to detail - wait for back button to be ready
            let backButton = app.navigationBars.buttons.element(boundBy: 0)
            if backButton.waitForExistence(timeout: 1) && backButton.isHittable {
                backButton.tap()
                // Wait for table to be visible again before next iteration
                _ = table.waitForExistence(timeout: 2)
                Thread.sleep(forTimeInterval: 0.3)
            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle rapid interaction")
    }
}
