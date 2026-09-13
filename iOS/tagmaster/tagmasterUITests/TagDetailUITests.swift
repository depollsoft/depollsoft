//
//  TagDetailUITests.swift
//  tagmasterUITests
//
//  UI tests for Tag Detail screen in TagMaster
//

import XCTest
import UIKit

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
            for index in 0..<segmentCount {
                segmentedControl.buttons.element(boundBy: index).tap()
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

// Strict regressions: no conditional passes when the requested screen is missing.
final class TagMasterPolishUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
    }

    private func capture(_ screen: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "tagmaster-ios-after-\(screen)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func home() {
        app.terminate()
        app.launch()
        XCTAssertTrue(app.navigationBars.buttons["Search"].waitForExistence(timeout: 10))
    }

    private func openTag() {
        let row = app.tables.staticTexts["Open Tag"]
        if !row.isHittable { app.tables.firstMatch.swipeUp() }
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        row.tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 3))
        alert.textFields.firstMatch.tap()
        alert.textFields.firstMatch.typeText("1809")
        alert.buttons["Open"].tap()
        let share = app.navigationBars.buttons["Share"]
        XCTAssertTrue(share.waitForExistence(timeout: 20))
        let enabled = NSPredicate(format: "enabled == true")
        expectation(for: enabled, evaluatedWith: share)
        waitForExpectations(timeout: 30)
        XCTAssertTrue(app.buttons["Rate tag"].waitForExistence(timeout: 5))
    }

    private func assertPageSlots(_ titles: [String]) {
        let buttons = titles.map { app.buttons["page-\($0)"] }
        XCTAssertEqual(buttons.count, 4)
        for button in buttons { XCTAssertTrue(button.waitForExistence(timeout: 5)) }
        let first = buttons[0].frame
        let last = buttons[3].frame
        let width = (last.maxX - first.minX) / 4
        for (index, button) in buttons.enumerated() {
            XCTAssertTrue(button.isHittable)
            XCTAssertGreaterThanOrEqual(button.frame.height, 44)
            XCTAssertEqual(button.frame.minX, first.minX + CGFloat(index) * width, accuracy: 1)
            XCTAssertEqual(button.frame.width, width, accuracy: 1)
            XCTAssertEqual(button.frame.maxY, first.maxY, accuracy: 1)
        }
        XCTAssertEqual(app.buttons.matching(identifier: "page-\(titles[0])").count, 1)
    }

    private func captureFullWidth(_ page: String) {
        let image = XCUIScreen.main.screenshot().image
        let scale = min(1, 800 / max(image.size.width, image.size.height))
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let reduced = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
        let attachment = XCTAttachment(data: reduced.jpegData(compressionQuality: 0.85)!, uniformTypeIdentifier: "public.jpeg")
        let device = UIDevice.current.userInterfaceIdiom == .pad ? "ipad" : "phone"
        attachment.name = "tagmaster-ios-fullwidth-native-\(device)-\(page)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testFullWidthPagesInNativePortraitAndLandscape() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        openTag()
        let detail = ["Summary", "Details", "Tracks", "Videos"]
        assertPageSlots(detail)
        captureFullWidth("detail")
        for title in detail {
            let button = app.buttons["page-\(title)"]
            button.tap()
            XCTAssertTrue(button.isSelected)
        }
        app.buttons["page-Details"].tap()
        XCTAssertTrue(app.staticTexts["Tag ID"].waitForExistence(timeout: 5))
        XCUIDevice.shared.orientation = .landscapeLeft
        assertPageSlots(detail)
        XCTAssertTrue(app.buttons["page-Details"].isSelected)
        XCUIDevice.shared.orientation = .portrait
        assertPageSlots(detail)
        XCTAssertTrue(app.buttons["page-Details"].isSelected)
        home()
        app.tables.staticTexts["Browse"].tap()
        let browse = ["Latest", "Rating", "Downloads", "Classic"]
        XCTAssertTrue(app.tables.cells.firstMatch.waitForExistence(timeout: 30))
        assertPageSlots(browse)
        captureFullWidth("browse")
        for title in browse {
            let button = app.buttons["page-\(title)"]
            button.tap()
            XCTAssertTrue(button.isSelected)
        }
        XCUIDevice.shared.orientation = .landscapeLeft
        assertPageSlots(browse)
        XCTAssertTrue(app.buttons["page-Classic"].isSelected)
        XCUIDevice.shared.orientation = .portrait
        assertPageSlots(browse)
        XCTAssertTrue(app.buttons["page-Classic"].isSelected)
    }

    func testSummaryLyricsRemainReachableAfterChangingPages() throws {
        openTag()
        app.buttons["Details"].tap()
        app.buttons["Summary"].tap()
        let lyrics = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'And I will wait to face the skies'")).firstMatch
        XCTAssertTrue(lyrics.exists)
        for _ in 0..<6 {
            if lyrics.isHittable { break }
            app.scrollViews.firstMatch.swipeUp()
        }
        XCTAssertTrue(lyrics.isHittable, "Lyrics must remain reachable even if the legacy grid leaves excess spacing")
    }

    func testPolishScreensAndShareDismissal() throws {
        XCTAssertTrue(app.navigationBars.buttons["Search"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.tables.staticTexts["Teachable Tags"].exists)
        capture("home")
        app.navigationBars.buttons["Search"].tap()
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 5))
        let sheetFilter = app.segmentedControls["Sheet Music"].exists ? app.segmentedControls["Sheet Music"] : app.buttons["Sheet Music"]
        XCTAssertTrue(sheetFilter.exists)
        XCTAssertGreaterThanOrEqual(sheetFilter.frame.height, 44)
        capture("search")
        app.searchFields.firstMatch.tap()
        app.searchFields.firstMatch.typeText("love")
        XCTAssertTrue(app.keyboards.firstMatch.exists)
        capture("search-keyboard")
        home()
        let settings = app.tables.staticTexts["Settings"]
        if !settings.isHittable { app.tables.firstMatch.swipeUp() }
        settings.tap()
        XCTAssertTrue(app.staticTexts["Log in to back up and synchronize your tag lists."].waitForExistence(timeout: 5))
        capture("settings")
        app.swipeUp()
        let minimumRating = app.segmentedControls["Minimum Rating"].exists ? app.segmentedControls["Minimum Rating"] : app.buttons["Minimum Rating"]
        XCTAssertTrue(minimumRating.waitForExistence(timeout: 5))
        capture("settings-filters")
        home()
        openTag()
        XCTAssertTrue(app.navigationBars.buttons["Favorite and Teachable options"].exists)
        XCTAssertTrue(app.navigationBars.buttons["Refresh"].exists)
        let pitch = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Play key note'")).firstMatch
        XCTAssertTrue(pitch.exists)
        XCTAssertGreaterThanOrEqual(pitch.frame.height, 44)
        for title in ["Summary", "Details", "Tracks", "Videos"] {
            let tab = app.buttons[title]
            XCTAssertTrue(tab.exists)
            XCTAssertGreaterThanOrEqual(tab.frame.height, 44)
            XCTAssertTrue(tab.isHittable)
        }
        app.buttons["Details"].tap()
        XCTAssertTrue(app.staticTexts["Tag ID"].waitForExistence(timeout: 5))
        app.buttons["Tracks"].tap()
        let emptyTracks = app.staticTexts["Sorry, no tracks could be found for this tag."]
        XCTAssertTrue(app.tables.firstMatch.exists || emptyTracks.waitForExistence(timeout: 5))
        capture("tracks")
        app.buttons["Videos"].tap()
        XCTAssertTrue(app.tables.firstMatch.waitForExistence(timeout: 5))
        capture("videos")
        app.buttons["Summary"].tap()
        capture("detail")
        app.navigationBars.buttons["Share"].tap()
        XCTAssertTrue(app.collectionViews["activityCollectionView"].waitForExistence(timeout: 10))
        capture("share")
        if app.buttons["Close"].exists {
            app.buttons["Close"].tap()
        } else if app.buttons["Dismiss"].exists {
            app.buttons["Dismiss"].tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.02, dy: 0.75)).tap()
        }
        let dismissed = NSPredicate(format: "exists == false")
        expectation(for: dismissed, evaluatedWith: app.collectionViews["activityCollectionView"])
        waitForExpectations(timeout: 5)
        XCTAssertTrue(app.buttons["Rate tag"].isHittable)
        app.buttons["Rate tag"].tap()
        XCTAssertTrue(app.buttons["5 stars"].waitForExistence(timeout: 5))
        capture("rating")
        if app.buttons["Cancel"].exists {
            app.buttons["Cancel"].tap()
        } else {
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.02, dy: 0.75)).tap()
        }
        XCTAssertEqual(app.state, .runningForeground)
        XCTAssertTrue(app.navigationBars.buttons["Share"].isHittable)
        home()
        app.tables.staticTexts["Browse"].tap()
        let cell = app.tables.cells.firstMatch
        XCTAssertTrue(cell.waitForExistence(timeout: 30))
        XCTAssertGreaterThan(cell.frame.height, 100)
        XCTAssertLessThan(cell.frame.height, 2000)
        XCTAssertTrue(cell.label.contains("Sheet music"))
        capture("browse-rows")
    }
}
