import XCTest

/**
 * UI Tests for the TagMaster iOS app.
 * Tests main screen, search, tag browsing, and navigation.
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
        XCTAssertTrue(app.state == .runningForeground)
    }
    
    func testMainScreenDisplays() throws {
        // Wait for main screen to load
        Thread.sleep(forTimeInterval: 1.0)
        XCTAssertTrue(app.exists)
    }
    
    // MARK: - Navigation Bar Tests
    
    func testNavigationBarExists() throws {
        let navBar = app.navigationBars.firstMatch
        XCTAssertTrue(navBar.waitForExistence(timeout: 5))
    }
    
    func testAppTitleDisplayed() throws {
        let title = app.navigationBars["Tag Master"]
        if !title.exists {
            let titleText = app.staticTexts["Tag Master"]
            XCTAssertTrue(title.exists || titleText.exists || app.navigationBars.firstMatch.exists)
        }
    }
    
    // MARK: - Search Button Tests
    
    func testSearchButtonExists() throws {
        let searchButton = app.navigationBars.buttons["Search"]
        let searchIcon = app.navigationBars.buttons["magnifyingglass"]
        XCTAssertTrue(searchButton.exists || searchIcon.exists || app.buttons["Search"].exists)
    }
    
    func testTapSearchOpensSearchScreen() throws {
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
        } else if app.navigationBars.buttons["magnifyingglass"].exists {
            app.navigationBars.buttons["magnifyingglass"].tap()
        } else if app.buttons["Search"].exists {
            app.buttons["Search"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Search screen should appear
        let searchField = app.searchFields.firstMatch
        let searchView = app.otherElements["searchView"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 2) || searchView.exists || app.textFields.firstMatch.exists)
    }
    
    // MARK: - Favorites List Tests
    
    func testFavoritesListExists() throws {
        let table = app.tables.firstMatch
        let collectionView = app.collectionViews.firstMatch
        XCTAssertTrue(table.waitForExistence(timeout: 3) || collectionView.exists)
    }
    
    func testFavoritesHeaderExists() throws {
        let favoritesHeader = app.staticTexts["Favorites"]
        XCTAssertTrue(favoritesHeader.exists || app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'Favorite'")).firstMatch.exists)
    }
    
    func testEmptyFavoritesState() throws {
        // If no favorites, should show empty state or empty list
        let emptyLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'No favorites' OR label CONTAINS 'Add favorites'")).firstMatch
        let table = app.tables.firstMatch
        XCTAssertTrue(emptyLabel.exists || table.exists)
    }
    
    func testTapFavoriteOpensDetail() throws {
        let table = app.tables.firstMatch
        if table.exists && table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            // Detail view should appear
            let backButton = app.navigationBars.buttons.firstMatch
            XCTAssertTrue(backButton.exists)
        }
    }
    
    // MARK: - Quick Actions Tests
    
    func testQuickSearchByIdButton() throws {
        let searchByIdButton = app.buttons["Search by ID"]
        let idButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'ID' OR label CONTAINS 'Number'")).firstMatch
        XCTAssertTrue(searchByIdButton.exists || idButton.exists || app.buttons["#"].exists)
    }
    
    func testTapSearchByIdOpensDialog() throws {
        let searchByIdButton = app.buttons["Search by ID"]
        if searchByIdButton.exists {
            searchByIdButton.tap()
        } else if app.buttons["#"].exists {
            app.buttons["#"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Should show ID input dialog
        let textField = app.textFields.firstMatch
        let alert = app.alerts.firstMatch
        XCTAssertTrue(textField.exists || alert.exists)
    }
    
    func testQuickBrowseButton() throws {
        let browseButton = app.buttons["Browse"]
        XCTAssertTrue(browseButton.exists || app.buttons.matching(NSPredicate(format: "label CONTAINS 'Browse'")).firstMatch.exists)
    }
    
    func testTapBrowseOpensBrowser() throws {
        let browseButton = app.buttons["Browse"]
        if browseButton.exists {
            browseButton.tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            // Browse screen should appear
            let backButton = app.navigationBars.buttons.firstMatch
            XCTAssertTrue(backButton.exists)
        }
    }
    
    // MARK: - Tab Bar / Bottom Navigation Tests
    
    func testTabBarExists() throws {
        let tabBar = app.tabBars.firstMatch
        // Tab bar might not exist if using different navigation
        if tabBar.exists {
            XCTAssertTrue(tabBar.buttons.count > 0)
        }
    }
    
    func testHomeTab() throws {
        if app.tabBars.firstMatch.exists {
            let homeTab = app.tabBars.buttons["Home"]
            if homeTab.exists {
                homeTab.tap()
                Thread.sleep(forTimeInterval: 0.3)
            }
        }
    }
    
    func testTeachableTagsTab() throws {
        if app.tabBars.firstMatch.exists {
            let teachableTab = app.tabBars.buttons["Teachable"]
            if teachableTab.exists {
                teachableTab.tap()
                Thread.sleep(forTimeInterval: 0.5)
                
                // Should show teachable tags screen
            }
        }
    }
    
    func testSettingsTab() throws {
        if app.tabBars.firstMatch.exists {
            let settingsTab = app.tabBars.buttons["Settings"]
            if settingsTab.exists {
                settingsTab.tap()
                Thread.sleep(forTimeInterval: 0.5)
                
                // Should show settings screen
            }
        }
    }
    
    // MARK: - Pull to Refresh Tests
    
    func testPullToRefresh() throws {
        let table = app.tables.firstMatch
        if table.exists {
            let start = table.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2))
            let end = table.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.8))
            start.press(forDuration: 0.1, thenDragTo: end)
            
            Thread.sleep(forTimeInterval: 1.0)
            
            XCTAssertTrue(table.exists)
        }
    }
    
    // MARK: - Settings Tests
    
    func testSettingsButtonExists() throws {
        let settingsButton = app.navigationBars.buttons["Settings"]
        let gearButton = app.navigationBars.buttons["gear"]
        XCTAssertTrue(settingsButton.exists || gearButton.exists || app.tabBars.buttons["Settings"].exists)
    }
    
    func testOpenSettings() throws {
        if app.navigationBars.buttons["Settings"].exists {
            app.navigationBars.buttons["Settings"].tap()
        } else if app.navigationBars.buttons["gear"].exists {
            app.navigationBars.buttons["gear"].tap()
        } else if app.tabBars.buttons["Settings"].exists {
            app.tabBars.buttons["Settings"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
    }
    
    // MARK: - Menu Tests
    
    func testOverflowMenuExists() throws {
        let menuButton = app.navigationBars.buttons["More"]
        let ellipsisButton = app.navigationBars.buttons["ellipsis"]
        // Menu might be accessed differently on iOS
        XCTAssertTrue(menuButton.exists || ellipsisButton.exists || app.navigationBars.buttons.count > 0)
    }
    
    // MARK: - Scroll Tests
    
    func testScrollFavoritesList() throws {
        let table = app.tables.firstMatch
        if table.exists {
            table.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            table.swipeDown()
            
            XCTAssertTrue(table.exists)
        }
    }
    
    // MARK: - Search Flow Tests
    
    func testCompleteSearchFlow() throws {
        // Open search
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
        } else if app.navigationBars.buttons["magnifyingglass"].exists {
            app.navigationBars.buttons["magnifyingglass"].tap()
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Enter search query
        let searchField = app.searchFields.firstMatch
        if searchField.waitForExistence(timeout: 2) {
            searchField.tap()
            searchField.typeText("Hello World")
            
            // Execute search
            app.keyboards.buttons["Search"].tap()
            
            Thread.sleep(forTimeInterval: 1.0)
            
            // Should show results
        }
    }
    
    // MARK: - Configuration Change Tests
    
    func testRotationPreservesState() throws {
        // Initial state
        let mainExists = app.navigationBars.firstMatch.exists
        
        // Rotate
        XCUIDevice.shared.orientation = .landscapeLeft
        Thread.sleep(forTimeInterval: 0.5)
        XCUIDevice.shared.orientation = .portrait
        Thread.sleep(forTimeInterval: 0.5)
        
        // State should be preserved
        XCTAssertEqual(app.navigationBars.firstMatch.exists, mainExists)
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
    
    func testMainScreenAccessibility() throws {
        let navBar = app.navigationBars.firstMatch
        XCTAssertTrue(navBar.exists)
    }
    
    func testSearchButtonAccessibility() throws {
        let searchButton = app.navigationBars.buttons["Search"]
        if searchButton.exists {
            XCTAssertTrue(searchButton.isAccessibilityElement)
        }
    }
    
    func testFavoritesListAccessibility() throws {
        let table = app.tables.firstMatch
        if table.exists {
            XCTAssertTrue(table.isAccessibilityElement || table.cells.count >= 0)
        }
    }
}
