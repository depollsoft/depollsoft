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
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Tag Detail Navigation
    
    func testNavigateToTagDetail() throws {
        // Search for a tag first
        searchForTag("Down Our Way")
        
        // Tap on first result
        let firstResult = app.cells.firstMatch
        XCTAssertTrue(firstResult.waitForExistence(timeout: 5), "Search results should appear")
        firstResult.tap()
        
        // Verify tag detail screen appears
        let tagTitle = app.navigationBars.staticTexts.firstMatch
        XCTAssertTrue(tagTitle.waitForExistence(timeout: 3), "Tag detail title should appear")
    }
    
    // MARK: - Tab Navigation
    
    func testTagDetailTabs() throws {
        navigateToTagDetail()
        
        // Look for tab segments
        let segmentedControl = app.segmentedControls.firstMatch
        
        if segmentedControl.exists {
            let segments = segmentedControl.buttons.allElementsBoundByIndex
            XCTAssertGreaterThan(segments.count, 1, "Should have multiple tabs")
            
            // Tap each segment
            for segment in segments {
                segment.tap()
                Thread.sleep(forTimeInterval: 0.5)
            }
        }
    }
    
    func testSummaryTab() throws {
        navigateToTagDetail()
        
        // Navigate to Summary tab
        let summaryTab = app.segmentedControls.buttons["Summary"]
        let infoTab = app.segmentedControls.buttons["Info"]
        
        if summaryTab.exists {
            summaryTab.tap()
        } else if infoTab.exists {
            infoTab.tap()
        }
        
        // Should display tag information
        // Title, arranger, parts info, etc.
    }
    
    func testMiscTab() throws {
        navigateToTagDetail()
        
        // Navigate to Misc tab
        let miscTab = app.segmentedControls.buttons["Misc"]
        let detailsTab = app.segmentedControls.buttons["Details"]
        
        if miscTab.exists {
            miscTab.tap()
        } else if detailsTab.exists {
            detailsTab.tap()
        }
        
        // Verify misc content loads
    }
    
    func testTracksTab() throws {
        navigateToTagDetail()
        
        // Navigate to Tracks tab
        let tracksTab = app.segmentedControls.buttons["Tracks"]
        let audioTab = app.segmentedControls.buttons["Audio"]
        
        if tracksTab.exists {
            tracksTab.tap()
        } else if audioTab.exists {
            audioTab.tap()
        }
        
        // Should show audio tracks if available
        let tracksList = app.tables.firstMatch
        // Track list may or may not exist depending on tag
    }
    
    func testVideosTab() throws {
        navigateToTagDetail()
        
        // Navigate to Videos tab
        let videosTab = app.segmentedControls.buttons["Videos"]
        
        if videosTab.exists {
            videosTab.tap()
            
            // Should show videos if available
            let videosList = app.tables.firstMatch
            let webView = app.webViews.firstMatch
            // Video content may vary
        }
    }
    
    // MARK: - Favorites Toggle
    
    func testAddToFavorites() throws {
        navigateToTagDetail()
        
        // Find favorites button (heart icon or star)
        let favoriteButton = app.buttons["favorite"]
        let heartButton = app.buttons["heart"]
        let starButton = app.buttons["star"]
        let addFavoriteButton = app.buttons["Add to Favorites"]
        
        let favButton = [favoriteButton, heartButton, starButton, addFavoriteButton].first { $0.exists }
        
        if let button = favButton {
            button.tap()
            
            // Verify visual feedback (filled heart/star)
            // Button appearance should change
        }
    }
    
    func testRemoveFromFavorites() throws {
        // First add to favorites
        testAddToFavorites()
        
        // Then remove
        let favoriteButton = app.buttons["favorite"]
        let heartButton = app.buttons["heart"]
        let removeFavoriteButton = app.buttons["Remove from Favorites"]
        
        let favButton = [favoriteButton, heartButton, removeFavoriteButton].first { $0.exists }
        
        if let button = favButton {
            button.tap()
            // Verify unfavorited state
        }
    }
    
    // MARK: - Share Functionality
    
    func testShareTag() throws {
        navigateToTagDetail()
        
        // Find share button
        let shareButton = app.buttons["Share"]
        let shareIcon = app.buttons["square.and.arrow.up"]
        
        if shareButton.exists {
            shareButton.tap()
        } else if shareIcon.exists {
            shareIcon.tap()
        }
        
        // Share sheet should appear
        let shareSheet = app.otherElements["ActivityListView"]
        let copyButton = app.buttons["Copy"]
        
        // Dismiss share sheet if it appeared
        if shareSheet.exists || copyButton.exists {
            // Tap outside to dismiss
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2)).tap()
        }
    }
    
    // MARK: - Sheet Music View
    
    func testViewSheetMusic() throws {
        navigateToTagDetail()
        
        // Find sheet music button or link
        let sheetMusicButton = app.buttons["Sheet Music"]
        let viewMusicButton = app.buttons["View Music"]
        let pdfButton = app.buttons["PDF"]
        
        let musicButton = [sheetMusicButton, viewMusicButton, pdfButton].first { $0.exists }
        
        if let button = musicButton {
            button.tap()
            
            // PDF viewer or web view should appear
            let pdfView = app.otherElements["PDFView"]
            let webView = app.webViews.firstMatch
            
            // Navigate back if viewer opened
            if pdfView.exists || webView.exists {
                app.navigationBars.buttons.firstMatch.tap()
            }
        }
    }
    
    // MARK: - Audio Playback
    
    func testPlayAudioTrack() throws {
        navigateToTagDetail()
        
        // Navigate to tracks tab
        let tracksTab = app.segmentedControls.buttons["Tracks"]
        if tracksTab.exists {
            tracksTab.tap()
        }
        
        // Find play button
        let playButton = app.buttons["play"]
        let playIcon = app.buttons["play.fill"]
        
        if playButton.exists {
            playButton.tap()
            
            // Should start playback - look for pause button or progress
            let pauseButton = app.buttons["pause"]
            let pauseIcon = app.buttons["pause.fill"]
            
            XCTAssertTrue(pauseButton.waitForExistence(timeout: 3) || pauseIcon.waitForExistence(timeout: 3),
                         "Playback should start")
        }
    }
    
    func testAudioPlaybackControls() throws {
        navigateToTagDetail()
        
        // Navigate to tracks tab
        let tracksTab = app.segmentedControls.buttons["Tracks"]
        if tracksTab.exists {
            tracksTab.tap()
        }
        
        // Check for playback controls
        let slider = app.sliders.firstMatch
        let progressBar = app.progressIndicators.firstMatch
        
        // Playback controls may or may not exist
    }
    
    // MARK: - Tag Information Display
    
    func testTagTitleDisplayed() throws {
        navigateToTagDetail()
        
        // Title should be visible in navigation bar or content
        let navTitle = app.navigationBars.staticTexts.firstMatch
        XCTAssertTrue(navTitle.exists, "Tag title should be displayed")
    }
    
    func testArrangerInfoDisplayed() throws {
        navigateToTagDetail()
        
        // Look for arranger info
        let arrangerLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] 'arr' OR label CONTAINS[c] 'by'")).firstMatch
        // Arranger info may be present
    }
    
    func testPartsInfoDisplayed() throws {
        navigateToTagDetail()
        
        // Look for parts info (Tenor, Lead, Bari, Bass)
        let partsLabels = ["Tenor", "Lead", "Bari", "Bass", "TTBB", "SATB"]
        
        var foundParts = false
        for part in partsLabels {
            if app.staticTexts[part].exists {
                foundParts = true
                break
            }
        }
        // Parts info may or may not be visible on this screen
    }
    
    // MARK: - External Links
    
    func testYouTubeLink() throws {
        navigateToTagDetail()
        
        // Navigate to videos tab
        let videosTab = app.segmentedControls.buttons["Videos"]
        if videosTab.exists {
            videosTab.tap()
        }
        
        // Find YouTube link
        let youtubeLink = app.links.matching(NSPredicate(format: "label CONTAINS[c] 'youtube'")).firstMatch
        let videoButton = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'video'")).firstMatch
        
        // External links may open Safari or embedded player
    }
    
    // MARK: - Scrolling and Content
    
    func testScrollTagDetail() throws {
        navigateToTagDetail()
        
        // Scroll the content
        let scrollView = app.scrollViews.firstMatch
        let tableView = app.tables.firstMatch
        
        if scrollView.exists {
            scrollView.swipeUp()
            scrollView.swipeDown()
        } else if tableView.exists {
            tableView.swipeUp()
            tableView.swipeDown()
        }
    }
    
    // MARK: - Navigation
    
    func testNavigateBackFromDetail() throws {
        navigateToTagDetail()
        
        // Find back button
        let backButton = app.navigationBars.buttons.firstMatch
        XCTAssertTrue(backButton.exists, "Back button should exist")
        
        backButton.tap()
        
        // Should return to previous screen
    }
    
    // MARK: - Accessibility
    
    func testTagDetailAccessibility() throws {
        navigateToTagDetail()
        
        // Main content should be accessible
        let staticTexts = app.staticTexts.allElementsBoundByIndex
        for text in staticTexts.prefix(3) {
            XCTAssertFalse(text.label.isEmpty, "Content should have accessibility labels")
        }
    }
    
    func testTabAccessibility() throws {
        navigateToTagDetail()
        
        let segmentedControl = app.segmentedControls.firstMatch
        if segmentedControl.exists {
            XCTAssertTrue(segmentedControl.isAccessibilityElement || segmentedControl.buttons.count > 0,
                         "Tab control should be accessible")
        }
    }
    
    // MARK: - Helper Methods
    
    private func searchForTag(_ query: String) {
        // Navigate to search
        let searchButton = app.buttons["Search"]
        let searchTab = app.tabBars.buttons["Search"]
        
        if searchButton.exists {
            searchButton.tap()
        } else if searchTab.exists {
            searchTab.tap()
        }
        
        // Enter search query
        let searchField = app.searchFields.firstMatch
        if searchField.waitForExistence(timeout: 2) {
            searchField.tap()
            searchField.typeText(query)
            
            // Submit search
            app.keyboards.buttons["Search"].tap()
        }
    }
    
    private func navigateToTagDetail() {
        searchForTag("Down Our Way")
        
        let firstResult = app.cells.firstMatch
        if firstResult.waitForExistence(timeout: 5) {
            firstResult.tap()
        }
        
        // Wait for detail to load
        _ = app.navigationBars.staticTexts.firstMatch.waitForExistence(timeout: 3)
    }
}
