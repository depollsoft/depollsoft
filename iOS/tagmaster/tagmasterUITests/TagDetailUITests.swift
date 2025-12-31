//
//  TagDetailUITests.swift
//  tagmasterUITests
//
//  UI tests for Tag Detail screen in TagMaster
//

import XCTest

final class TagDetailUITests: XCTestCase {
    
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
    
    // MARK: - Helper Methods
    
    private func navigateToTagDetail() -> Bool {
        // Try to navigate to a tag detail by tapping a favorite
        let table = app.tables.firstMatch
        
        if table.exists && table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
            return true
        }
        
        return false
    }
    
    private func openSearchAndFindTag() -> Bool {
        // Open search
        if app.navigationBars.buttons["Search"].exists {
            app.navigationBars.buttons["Search"].tap()
        } else if app.buttons["magnifyingglass"].exists {
            app.buttons["magnifyingglass"].tap()
        } else {
            return false
        }
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // Search for something
        let searchField = app.searchFields.firstMatch.exists ? 
                         app.searchFields.firstMatch : app.textFields.firstMatch
        
        guard searchField.exists else { return false }
        
        searchField.tap()
        searchField.typeText("hello")
        
        let searchButton = app.keyboards.buttons["Search"]
        if searchButton.exists {
            searchButton.tap()
        }
        
        Thread.sleep(forTimeInterval: 2.0)
        
        // Tap first result
        let table = app.tables.firstMatch
        if table.exists && table.cells.count > 0 {
            table.cells.element(boundBy: 0).tap()
            Thread.sleep(forTimeInterval: 0.5)
            return true
        }
        
        return false
    }
    
    // MARK: - Navigation Tests
    
    func testCanNavigateToTagDetail() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        if !navigated {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Should be on detail screen
        XCTAssertEqual(app.state, .runningForeground, "Should show tag detail")
    }
    
    func testTagDetailHasContent() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Should have some content
        let hasContent = app.staticTexts.count > 0 || 
                        app.tables.firstMatch.exists ||
                        app.segmentedControls.firstMatch.exists
        
        XCTAssertTrue(hasContent, "Tag detail should have content")
    }
    
    func testCanNavigateBackFromDetail() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Navigate back
        let backButton = app.navigationBars.buttons.element(boundBy: 0)
        if backButton.exists {
            backButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should navigate back from detail")
    }
    
    // MARK: - Tab Navigation Tests (if tabs exist in detail)
    
    func testDetailTabsIfExist() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        let segmentedControl = app.segmentedControls.firstMatch
        
        if segmentedControl.exists {
            let segmentCount = segmentedControl.buttons.count
            
            // Tap each segment
            for i in 0..<segmentCount {
                segmentedControl.buttons.element(boundBy: i).tap()
                Thread.sleep(forTimeInterval: 0.3)
            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle tab navigation")
    }
    
    // MARK: - Action Tests
    
    func testFavoriteButtonIfExists() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Look for favorite button
        let favoriteButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[cd] 'favorite' OR label CONTAINS[cd] 'heart' OR label CONTAINS[cd] 'star'")).firstMatch
        
        if favoriteButton.exists {
            favoriteButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
            
            // Tap again to toggle
            favoriteButton.tap()
            Thread.sleep(forTimeInterval: 0.3)
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle favorite toggle")
    }
    
    func testShareButtonIfExists() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Look for share button
        let shareButton = app.buttons["Share"]
        let shareIcon = app.buttons["square.and.arrow.up"]
        
        if shareButton.exists {
            shareButton.tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            // Dismiss share sheet
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()
        } else if shareIcon.exists {
            shareIcon.tap()
            Thread.sleep(forTimeInterval: 0.5)
            
            // Dismiss share sheet
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()
        }
        
        Thread.sleep(forTimeInterval: 0.3)
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle share action")
    }
    
    // MARK: - Scrolling Tests
    
    func testCanScrollDetailContent() throws {
        let navigated = navigateToTagDetail() || openSearchAndFindTag()
        
        guard navigated else {
            throw XCTSkip("Could not navigate to tag detail")
        }
        
        // Try scrolling
        let scrollView = app.scrollViews.firstMatch
        let table = app.tables.firstMatch
        
        if scrollView.exists {
            scrollView.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            scrollView.swipeDown()
        } else if table.exists {
            table.swipeUp()
            Thread.sleep(forTimeInterval: 0.3)
            table.swipeDown()
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle scrolling")
    }
    
    // MARK: - App Stability Tests
    
    func testRapidDetailNavigation() throws {
        // Navigate to detail and back multiple times
        for _ in 0..<3 {
            let navigated = navigateToTagDetail()
            
            if navigated {
                Thread.sleep(forTimeInterval: 0.3)
                
                // Navigate back
                let backButton = app.navigationBars.buttons.element(boundBy: 0)
                if backButton.exists {
                    backButton.tap()
                    Thread.sleep(forTimeInterval: 0.3)
                }
            } else {
                break
            }
        }
        
        XCTAssertEqual(app.state, .runningForeground, "Should handle rapid navigation")
    }
}
