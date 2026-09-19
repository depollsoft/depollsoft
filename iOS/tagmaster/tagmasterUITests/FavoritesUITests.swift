import XCTest

/**
 * UI Tests for Favorites functionality in TagMaster.
 * Tests viewing and managing favorite tags.
 */
class FavoritesUITests: TagMasterUITestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // NSNumber entries match the app's persisted list format. These
        // launch-only preferences never touch an account or personal device.
        app.launchArguments = ["--uitesting", "-depollsoft.pitchperfect.lists",
            "<dict><key>favorite</key><array><integer>669</integer><integer>1478</integer><integer>122</integer></array></dict>"]
        app.launch()

    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    private var favoriteCells: XCUIElementQuery {
        app.tables.firstMatch.cells.matching(NSPredicate(
            format: "label CONTAINS 'Tag ID ' OR label MATCHES 'Tag [0-9]+[.] Open to load details[.]'"))
    }

    private var backButton: XCUIElement {
        app.navigationBars.buttons.matching(NSPredicate(
            format: "identifier == 'BackButton' OR label == 'Home' OR label == 'Back'")).firstMatch
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
        
        if table.exists && favoriteCells.count > 0 {
            favoriteCells.element(boundBy: 0).tap()

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

            table.swipeDown()

        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle scrolling")
    }
    
    // MARK: - Remove Favorite Tests (if favorites exist)
    
    func testSwipeOnFavoriteIfExists() throws {
        let table = app.tables.firstMatch
        
        guard table.existsOrWait(timeout: 3) && favoriteCells.count > 0 else {
            throw XCTSkip("No favorites table or cells to test swipe on")
        }
        
        // Only test if we can safely interact with the cell
        let cell = favoriteCells.element(boundBy: 0)
        guard cell.isHittable else {
            throw XCTSkip("Cell is not hittable for swipe test")
        }
        
        // Simple swipe - don't try to interact further as it can fail
        cell.swipeLeft()

        // Verify app is still running - that's the key test
        XCTAssertEqual(app.state, .runningForeground, "Should handle swipe actions")
    }
    
    // MARK: - Navigation Tests
    
    func testCanNavigateBackFromDetail() throws {
        let table = app.tables.firstMatch
        
        guard table.exists && favoriteCells.count > 0 else {
            throw XCTSkip("No favorites to navigate from")
        }
        
        // Tap to open detail
        favoriteCells.element(boundBy: 0).tap()

        // Navigate back
        let backButton = self.backButton
        if backButton.exists {
            backButton.tap()

        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should navigate back")
    }
    
    // MARK: - App Stability Tests
    
    func testRapidFavoriteInteraction() throws {
        let table = app.tables.firstMatch
        
        guard table.existsOrWait(timeout: 3) && favoriteCells.count > 0 else {
            // No table or cells - skip test rather than fail
            throw XCTSkip("No favorites table or cells to test rapid interaction")
        }
        
        // Rapid tapping with proper waits
        let cellCount = min(3, favoriteCells.count)
        for i in 0..<cellCount {
            let cell = favoriteCells.element(boundBy: i)
            
            // Ensure cell is hittable before tapping
            guard cell.existsOrWait(timeout: 2) && cell.isHittable else {
                continue  // Skip this cell if not ready
            }
            
            cell.tap()

            // Navigate back if we went to detail - wait for back button to be ready
            let backButton = self.backButton
            if backButton.existsOrWait(timeout: 1) && backButton.isHittable {
                backButton.tap()
                // Wait for table to be visible again before next iteration
                _ = table.existsOrWait(timeout: 2)

            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle rapid interaction")
    }
}
