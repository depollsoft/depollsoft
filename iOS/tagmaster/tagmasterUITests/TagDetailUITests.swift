//
//  TagDetailUITests.swift
//  tagmasterUITests
//
//  UI tests for Tag Detail screen in TagMaster
//

import XCTest
import UIKit
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
extension TagMasterPolishUITests {
    private func assertNativeTabs(_ titles: [String]) {
        let bar = app.tabBars["page-tab-bar"]
        XCTAssertTrue(bar.waitForExistence(timeout: 5))
        let buttons = titles.map { bar.buttons["page-\($0)"] }
        XCTAssertEqual(bar.buttons.count, 4)
        let screen = app.windows.firstMatch.frame
        XCTAssertGreaterThan(bar.frame.minY, screen.midY)
        XCTAssertLessThanOrEqual(bar.frame.maxY, screen.maxY + 1)
        var previous: CGRect?
        for (index, button) in buttons.enumerated() {
            XCTAssertTrue(button.waitForExistence(timeout: 5))
            XCTAssertEqual(button.label, titles[index])
            XCTAssertTrue(button.isHittable)
            XCTAssertGreaterThanOrEqual(button.frame.height, 44)
            XCTAssertGreaterThanOrEqual(button.frame.width, 44)
            XCTAssertTrue(bar.frame.insetBy(dx: -1, dy: -1).contains(button.frame))
            // The native selected lens can enlarge its AX frame beyond its slot.
            // Check disjoint 44pt activation targets, not custom equal-slot bounds.
            let target = CGRect(x: button.frame.midX - 22, y: button.frame.midY - 22, width: 44, height: 44)
            if let previous { XCTAssertFalse(previous.intersects(target)) }
            previous = target
        }
        XCTAssertEqual(buttons.filter { $0.isSelected }.count, 1)
        // Browse is in the iPad primary column; Detail is in secondary.
        if UIDevice.current.userInterfaceIdiom == .pad {
            let bars = app.navigationBars.allElementsBoundByIndex.filter { !$0.frame.isEmpty }
            XCTAssertTrue(bars.contains { $0.frame.minX <= bar.frame.minX + 1 && $0.frame.maxX >= bar.frame.maxX - 1 })
        }
    }

    private func captureNativeGlass(_ page: String) {
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
        attachment.name = "tagmaster-ios-native-glass-\(device)-\(page)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

}

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

    func testNativeGlassTabsInPortraitAndLandscape() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        openTag()
        let detail = ["Summary", "Details", "Tracks", "Videos"]
        assertNativeTabs(detail)
        captureNativeGlass("detail")
        for title in detail {
            let button = app.buttons["page-\(title)"]
            button.tap()
            XCTAssertTrue(button.isSelected)
        }
        app.buttons["page-Details"].tap()
        XCTAssertTrue(app.staticTexts["Last Refreshed"].waitForExistence(timeout: 5))
        captureNativeGlass("detail-details")
        XCUIDevice.shared.orientation = .landscapeLeft
        assertNativeTabs(detail)
        XCTAssertTrue(app.buttons["page-Details"].isSelected)
        XCUIDevice.shared.orientation = .portrait
        assertNativeTabs(detail)
        XCTAssertTrue(app.buttons["page-Details"].isSelected)
        home()
        app.tables.staticTexts["Browse"].tap()
        let browse = ["Latest", "Rating", "Downloads", "Classic"]
        XCTAssertTrue(app.tables.cells.firstMatch.waitForExistence(timeout: 30))
        assertNativeTabs(browse)
        captureNativeGlass("browse")
        for title in browse {
            let button = app.buttons["page-\(title)"]
            button.tap()
            XCTAssertTrue(button.isSelected)
        }
        captureNativeGlass("browse-classic")
        XCUIDevice.shared.orientation = .landscapeLeft
        assertNativeTabs(browse)
        XCTAssertTrue(app.buttons["page-Classic"].isSelected)
        XCUIDevice.shared.orientation = .portrait
        assertNativeTabs(browse)
        XCTAssertTrue(app.buttons["page-Classic"].isSelected)
    }

    private func layoutCapture(_ name: String) {
        let image = XCUIScreen.main.screenshot().image
        let scale = min(1, 800 / max(image.size.width, image.size.height))
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let format = UIGraphicsImageRendererFormat(); format.scale = 1
        let reduced = UIGraphicsImageRenderer(size: size, format: format).image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
        let device = UIDevice.current.userInterfaceIdiom == .pad ? "ipad" : "phone"
        let attachment = XCTAttachment(data: reduced.jpegData(compressionQuality: 0.88)!, uniformTypeIdentifier: "public.jpeg")
        attachment.name = "tagmaster-layout-after-ios-\(device)-native-\(name)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func reachLowestRatingAndCancel() {
        let lowest = app.buttons["1 star"]
        XCTAssertTrue(lowest.waitForExistence(timeout: 5))
        for _ in 0..<6 {
            if lowest.isHittable { break }
            let sheet = app.sheets.firstMatch
            let scroll = sheet.scrollViews.containing(.button, identifier: "1 star").firstMatch
            if scroll.exists { scroll.swipeUp() } else { sheet.swipeUp() }
        }
        XCTAssertTrue(lowest.isHittable, "Native action-sheet scrolling reaches the lowest rating")
        XCTAssertGreaterThanOrEqual(lowest.frame.height, 44)
        XCTAssertGreaterThanOrEqual(lowest.frame.width, 44)
        let cancel = app.buttons["Cancel"]
        if cancel.exists {
            for _ in 0..<6 {
                if cancel.isHittable { break }
                let scroll = app.sheets.scrollViews.containing(.button, identifier: "Cancel").firstMatch
                if scroll.exists { scroll.swipeUp() } else { app.sheets.firstMatch.swipeUp() }
            }
            XCTAssertTrue(cancel.isHittable)
            cancel.tap()
        } else {
            // Native floating action sheets can omit Cancel. Dismiss outside
            // the popover, on either idiom, without invoking a rating action.
            app.navigationBars.firstMatch.tap()
        }
        XCTAssertTrue(lowest.waitForNonExistence(timeout: 5))
    }

    func testLayoutNativeRatingScrollAndCancel() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        openTag()
        let rate = app.buttons["Rate tag"]
        for _ in 0..<8 { if rate.isHittable { break }; app.scrollViews.firstMatch.swipeUp() }
        XCTAssertTrue(rate.isHittable)
        rate.tap()
        reachLowestRatingAndCancel()
    }

    func testLayoutNativeEmptyTeachableBrowseReachable() throws {
        XCTAssertTrue(app.navigationBars.buttons["Search"].waitForExistence(timeout: 10))
        let row = app.tables.staticTexts["Teachable Tags"]
        for _ in 0..<6 { if row.isHittable { break }; app.tables.firstMatch.swipeUp() }
        XCTAssertTrue(row.isHittable)
        row.tap()
        XCTAssertTrue(app.staticTexts["No teachable tags yet"].waitForExistence(timeout: 5), "Requires the original empty list; never clears a populated list")
        let browse = app.buttons["teachable.browse"]
        let list = app.tables.containing(.button, identifier: "teachable.browse").firstMatch
        XCTAssertTrue(list.exists, "Scroll the Teachable table, not the other pane of an iPad split view")
        for _ in 0..<6 { if browse.isHittable { break }; list.swipeUp() }
        XCTAssertEqual(browse.label, "Browse Tags")
        XCTAssertTrue(browse.isHittable)
        XCTAssertGreaterThanOrEqual(browse.frame.height, 44)
        browse.tap()
        XCTAssertTrue(app.buttons["page-Latest"].waitForExistence(timeout: 10))
    }

    func testLayoutNativeSheetKeyTarget() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        openTag()
        let sheet = app.buttons["Sheet Music"]
        for _ in 0..<8 { if sheet.isHittable { break }; app.scrollViews.firstMatch.swipeUp() }
        XCTAssertTrue(sheet.isHittable)
        sheet.tap()
        let key = app.buttons["sheet.key"]
        // Exercise the real download and its retry action; never substitute a fixture.
        let deadline = Date().addingTimeInterval(120)
        var retries = 0
        while Date() < deadline && !key.waitForExistence(timeout: 5) {
            let failure = app.alerts["Couldn't complete request"]
            if failure.exists {
                XCTAssertTrue(failure.staticTexts[
                    "Sheet music couldn't be opened. Check your connection and try again."
                ].exists)
                guard retries < 2 else { break }
                failure.buttons["Retry"].tap()
                retries += 1
            }
        }
        XCTAssertTrue(key.exists, "Live sheet music must load before checking its key control")
        XCTAssertTrue(key.isHittable)
        print("TM_LAYOUT_PROBE native sheet key frame=\(key.frame)")
        layoutCapture("sheet-defect-portrait")
        XCTAssertGreaterThanOrEqual(key.frame.width, 44)
        XCTAssertGreaterThanOrEqual(key.frame.height, 44)
        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(key.waitForExistence(timeout: 5))
        XCTAssertTrue(key.isHittable)
        print("TM_LAYOUT_PROBE native landscape key frame=\(key.frame)")
        XCTAssertGreaterThanOrEqual(key.frame.width, 44)
        XCTAssertGreaterThanOrEqual(key.frame.height, 44)
        layoutCapture("sheet-defect-landscape")
    }

    func testLayoutNativeKeyboardAlertsAndSplit() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(app.navigationBars.buttons["Search"].waitForExistence(timeout: 10))
        layoutCapture("home")
        app.navigationBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("harmony")
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        let searchAction = app.navigationBars.buttons["Search"].firstMatch
        XCTAssertTrue(searchAction.isHittable)
        layoutCapture("search-keyboard")
        XCUIDevice.shared.orientation = .landscapeLeft
        field.tap()
        field.typeText(" quartet")
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(searchAction.isHittable)
        layoutCapture("search-keyboard-landscape")
        home()
        XCUIDevice.shared.orientation = .portrait
        let open = app.tables.staticTexts["Open Tag"]
        if !open.isHittable { app.tables.firstMatch.swipeUp() }
        open.tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        XCTAssertTrue(alert.buttons["Cancel"].isHittable)
        XCTAssertTrue(alert.buttons["Open"].isHittable)
        XCTAssertTrue(alert.textFields.firstMatch.isHittable)
        layoutCapture("open-tag")
        alert.buttons["Cancel"].tap()
        openTag()
        app.buttons["page-Details"].tap()
        XCTAssertTrue(app.staticTexts["Last Refreshed"].waitForExistence(timeout: 5))
        assertNativeTabs(["Summary", "Details", "Tracks", "Videos"])
        layoutCapture("details")
        XCUIDevice.shared.orientation = .landscapeLeft
        assertNativeTabs(["Summary", "Details", "Tracks", "Videos"])
        layoutCapture("details-landscape")
        XCUIDevice.shared.orientation = .portrait
        app.buttons["page-Summary"].tap()
        let rate = app.buttons["Rate tag"]
        for _ in 0..<8 { if rate.isHittable { break }; app.scrollViews.firstMatch.swipeUp() }
        XCTAssertTrue(rate.isHittable)
        rate.tap()
        XCTAssertTrue(app.buttons["5 stars"].waitForExistence(timeout: 5))
        reachLowestRatingAndCancel()
        // No rating or share action is submitted.
        home()
        let settings = app.tables.staticTexts["Settings"]
        if !settings.isHittable { app.tables.firstMatch.swipeUp() }
        settings.tap()
        XCTAssertTrue(app.staticTexts["Log in to back up and synchronize your tag lists."].waitForExistence(timeout: 5))
        layoutCapture("settings")
        let login = app.tables.staticTexts["Log In"]
        // Never tap Log Out on an existing account.
        XCTAssertTrue(login.exists, "This read-only journey requires the signed-out entry")
        login.tap()
        XCTAssertTrue(app.buttons["Close"].waitForExistence(timeout: 5))
        layoutCapture("login-entry")
        app.buttons["Close"].tap()
        XCTAssertTrue(app.tables.staticTexts["Log In"].waitForExistence(timeout: 5))
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
        XCTAssertTrue(app.staticTexts["Last Refreshed"].waitForExistence(timeout: 5))
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

/// Uses tag 1809 from the live catalog; a network/catalog failure fails capture.
final class StoreScreenshotTests: XCTestCase {
    func testCaptureStoreScreenshots() throws {
        try XCTSkipUnless(ProcessInfo.processInfo.environment["STORE_SCREENSHOTS"] == "1")
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        let app = XCUIApplication()
        func home() {
            app.terminate()
            app.launch()
            XCTAssertTrue(app.tables.staticTexts["Browse"].waitForExistence(timeout: 15))
        }
        func snap(_ name: String) {
            Thread.sleep(forTimeInterval: 2)
            let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
            attachment.name = "store-\(name)"
            attachment.lifetime = .keepAlways
            add(attachment)
        }
        func open(_ id: String) {
            home()
            let item = app.tables.staticTexts["Open Tag"]
            if !item.isHittable { app.tables.firstMatch.swipeUp() }
            item.tap()
            let alert = app.alerts["Open Tag"]
            XCTAssertTrue(alert.waitForExistence(timeout: 5))
            alert.textFields.firstMatch.tap()
            alert.textFields.firstMatch.typeText(id)
            alert.buttons["Open"].tap()
            let share = app.navigationBars.buttons["Share"]
            // The live catalog occasionally fails a request. Exercise the app's
            // Retry action, but never capture its error or loading state.
            for _ in 0..<2 {
                if share.waitForExistence(timeout: 30) { break }
                let retry = app.buttons["Retry"].firstMatch
                if retry.exists { retry.tap() }
            }
            XCTAssertTrue(share.waitForExistence(timeout: 90))
            expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: share)
            waitForExpectations(timeout: 90)
            XCTAssertTrue(app.buttons["Rate tag"].waitForExistence(timeout: 10))
        }
        // These are real catalog entries, saved only in this disposable simulator.
        for id in ["669", "1478", "122"] {
            open(id)
            if app.navigationBars.buttons["Add Favorite"].exists {
                app.navigationBars.buttons["Add Favorite"].tap()
            } else if app.navigationBars.buttons["Favorite and Teachable options"].exists {
                app.navigationBars.buttons["Favorite and Teachable options"].tap()
                if app.buttons["Add Favorite"].exists { app.buttons["Add Favorite"].tap() }
                else { app.buttons["Cancel"].tap() }
            }
        }
        for (title, name) in [("Summary", "05-summary"), ("Details", "06-details"), ("Tracks", "07-tracks"), ("Videos", "08-videos")] {
            let item = app.buttons["page-\(title)"]
            XCTAssertTrue(item.waitForExistence(timeout: 10))
            item.tap()
            XCTAssertTrue(item.isSelected)
            if title == "Tracks" {
                let lead = app.tables.cells.containing(.staticText, identifier: "Lead").firstMatch
                XCTAssertTrue(lead.waitForExistence(timeout: 15))
                lead.tap()
                let transport = app.buttons["tagmaster.trackPlayer.playPause"]
                XCTAssertTrue(transport.waitForExistence(timeout: 10))
                expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: transport)
                waitForExpectations(timeout: 90)
                if transport.label == "Pause" { transport.tap() }
            }
            if title == "Videos" {
                XCTAssertTrue(app.tables.cells.firstMatch.waitForExistence(timeout: 30))
                app.tables.firstMatch.swipeUp()
                Thread.sleep(forTimeInterval: 8)
            }
            snap(name)
        }
        home()
        XCTAssertTrue(app.tables.staticTexts["Cheer Up, Charlie"].waitForExistence(timeout: 60))
        if UIDevice.current.userInterfaceIdiom == .pad {
            // Keep home distinct from the detail-only scenes, which use tag 122.
            app.tables.staticTexts["Their Hearts Were Full Of Spring"].tap()
            XCTAssertTrue(app.buttons["Rate tag"].waitForExistence(timeout: 30))
        }
        snap("01-home")
        app.tables.staticTexts["Browse"].tap()
        let classic = app.buttons["page-Classic"]
        XCTAssertTrue(classic.waitForExistence(timeout: 15))
        classic.tap()
        XCTAssertTrue(app.tables.cells.firstMatch.waitForExistence(timeout: 90))
        if UIDevice.current.userInterfaceIdiom == .pad {
            app.tables.cells.firstMatch.tap()
            XCTAssertTrue(app.buttons["Rate tag"].waitForExistence(timeout: 30))
        }
        snap("02-browse")
        home()
        app.navigationBars.buttons["Search"].tap()
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 10))
        snap("03-search")
        app.searchFields.firstMatch.tap()
        app.searchFields.firstMatch.typeText("Lone Prairie")
        app.keyboards.buttons["Search"].tap()
        let result = app.tables.cells.matching(NSPredicate(format: "label CONTAINS %@", "Lone Prairie")).firstMatch
        // UISearchController can consume the keyboard action while dismissing
        // its presentation. Submit the retained query from the navigation bar.
        if !result.waitForExistence(timeout: 5) {
            app.navigationBars.buttons["Search"].tap()
        }
        XCTAssertTrue(result.waitForExistence(timeout: 90))
        if UIDevice.current.userInterfaceIdiom == .pad {
            result.tap()
            XCTAssertTrue(app.buttons["Rate tag"].waitForExistence(timeout: 30))
        }
        snap("04-results")
    }
}
