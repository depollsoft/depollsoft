import XCTest
import UIKit

/// Real UI probes against the running app. These assert on named elements and
/// on what a tap actually does, rather than falling back to "something exists".
final class TMDeskUITests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10))
    }

    override func tearDownWithError() throws {
        app = nil
    }

    private var homeTable: XCUIElement { app.tables.firstMatch }

    private func destination(_ name: String) -> XCUIElement {
        let tab = app.tabBars.buttons[name]
        if tab.exists { return tab }
        let sidebar = app.cells[name]
        return sidebar.exists ? sidebar : app.buttons[name].firstMatch
    }

    private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = XCUIDevice.shared.orientation.isLandscape ? "landscape-" + name : name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testNativeScreenInventory() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        capture("home-iOS")
        app.staticTexts["Find a tag"].tap()
        XCTAssertTrue(app.searchFields["Search tags by title or lyrics"].waitForExistence(timeout: 5))
        capture("search-iOS")
        destination("Browse").tap()
        XCTAssertTrue(app.segmentedControls.firstMatch.waitForExistence(timeout: 5))
        capture("browse-iOS")
        destination("Home").tap()
        app.staticTexts["Open Tag ID"].tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        alert.textFields.firstMatch.tap()
        alert.textFields.firstMatch.typeText("1")
        alert.buttons["Open"].tap()
        let picker = app.segmentedControls.firstMatch
        XCTAssertTrue(picker.waitForExistence(timeout: 15))
        XCTAssertGreaterThanOrEqual(picker.frame.height, 44)
        capture("summary-iOS")
        for section in ["Details", "Tracks", "Videos"] {
            picker.buttons[section].tap()
            XCTAssertTrue(picker.buttons[section].isSelected)
            capture(section.lowercased() + "-iOS")
        }
        app.navigationBars.buttons["Share tag"].tap()
        XCTAssertTrue(app.otherElements["ActivityListView"].waitForExistence(timeout: 5))
        capture("share-iOS")
    }

    func testTabletLandscapeWorkspace() throws {
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        XCTAssertTrue(destination("Home").waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Find a tag"].isHittable)
        try testNativeScreenInventory()
    }

    func testDarkLargeText() throws {
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL",
                                "-AppleInterfaceStyle", "Dark"]
        app.launch()
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Find a tag"].isHittable)
        capture("home-dark-large-iOS")
        app.staticTexts["Find a tag"].tap()
        XCTAssertTrue(app.searchFields["Search tags by title or lyrics"].waitForExistence(timeout: 5))
        app.swipeUp()
        let sort = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Sort by, ")).firstMatch
        XCTAssertTrue(sort.waitForExistence(timeout: 5))
        sort.tap()
        XCTAssertTrue(app.buttons["Rating"].waitForExistence(timeout: 5))
        app.buttons["Rating"].tap()
        XCTAssertTrue(app.buttons["Sort by, Rating"].exists)
        capture("search-dark-large-iOS")
    }

    private func openReviewTag() {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        app.staticTexts["Open Tag ID"].tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        alert.textFields.firstMatch.tap()
        alert.textFields.firstMatch.typeText("1")
        alert.buttons["Open"].tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Sound the key")).firstMatch
            .waitForExistence(timeout: 20))
    }

    private func captureReviewDetails(darkLarge: Bool) {
        let suffix = darkLarge ? "-dark-large-iOS" : "-iOS"
        if !darkLarge && UIDevice.current.userInterfaceIdiom == .phone {
            let pitch = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Sound the key")).firstMatch
            // The pitch wrapper has its own 2pt inset on each side.
            XCTAssertGreaterThanOrEqual(pitch.frame.width, app.frame.width - 40)
            XCTAssertLessThanOrEqual(pitch.frame.width, app.frame.width - 32)
            XCTAssertTrue(app.staticTexts["Barbershop"].exists)
            XCTAssertLessThan(app.staticTexts["Barbershop"].frame.height, 30)
        }
        capture("summary" + suffix)
        if darkLarge {
            app.buttons["Tag section, Summary"].tap()
            app.buttons["Details"].tap()
        } else {
            app.segmentedControls.firstMatch.buttons["Details"].tap()
        }
        XCTAssertTrue(app.staticTexts["Bobby Gray, Jr"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Bobby Gray, Jr"].exists)
        let arranger = app.staticTexts["Bobby Gray, Jr"]
        XCTAssertGreaterThan(arranger.frame.width, 0)
        XCTAssertLessThanOrEqual(arranger.frame.maxX, app.frame.maxX)
        XCTAssertGreaterThan(arranger.frame.height, 0)
        XCTAssertTrue(app.staticTexts["New Tradition"].exists)
        XCTAssertFalse(app.buttons["New Tradition"].exists)
        XCTAssertTrue(app.buttons["Daniel Gillis"].exists)
        capture("details" + suffix)
        if darkLarge && !app.staticTexts["New Tradition"].isHittable {
            app.swipeUp()
            XCTAssertTrue(app.staticTexts["New Tradition"].isHittable)
            capture("details-attribution" + suffix)
        }
    }

    func testReviewSummaryAndAttributionLight() {
        openReviewTag()
        captureReviewDetails(darkLarge: false)
    }

    func testReviewTabletLandscapeSummaryAndAttributionLight() throws {
        try XCTSkipUnless(UIDevice.current.userInterfaceIdiom == .pad)
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        openReviewTag()
        captureReviewDetails(darkLarge: false)
    }

    func testReviewSummaryAndAttributionDarkLarge() {
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL",
                                "-AppleInterfaceStyle", "Dark"]
        app.launch()
        openReviewTag()
        captureReviewDetails(darkLarge: true)
    }

    private func reviewSummaryRatingLarge(dark: Bool) {
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL",
                                "-AppleInterfaceStyle", dark ? "Dark" : "Light"]
        app.launch()
        openReviewTag()
        let rate = app.buttons["Rate"]
        let stars = app.staticTexts["Rated 3.17 out of 5"]
        let rating = app.staticTexts["3.17 out of 5"]
        // Scope to Summary rather than an offscreen destination's scroll view.
        let summary = app.scrollViews.containing(.button, identifier: "Rate").firstMatch
        XCTAssertTrue(summary.exists)
        for _ in 0..<4 {
            if stars.isHittable && rating.isHittable && rate.isHittable { break }
            let start = summary.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.75))
            let end = summary.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5))
            start.press(forDuration: 0.05, thenDragTo: end)
        }
        XCTAssertTrue(stars.isHittable)
        XCTAssertTrue(rating.isHittable)
        XCTAssertTrue(rate.isHittable)
        XCTAssertGreaterThanOrEqual(rate.frame.height, 44)
        XCTAssertGreaterThanOrEqual(rate.frame.width, 44)
        XCTAssertGreaterThanOrEqual(rate.frame.minY, rating.frame.maxY)
        XCTAssertGreaterThanOrEqual(rating.frame.minY, stars.frame.maxY)
        XCTAssertLessThanOrEqual(stars.frame.maxX, app.frame.maxX - 16)
        XCTAssertLessThanOrEqual(rating.frame.maxX, app.frame.maxX - 16)
        if dark { capture("summary-dark-large-iOS") }
        rate.tap()
        XCTAssertTrue(app.sheets["Rating"].waitForExistence(timeout: 5))
        // Anchored action sheets use a popover on current iOS, which omits the
        // cancel action in favor of the native outside-tap dismissal region.
        let cancel = app.buttons["Cancel"].firstMatch
        if cancel.exists {
            cancel.tap()
        } else {
            let dismissRegion = app.otherElements["PopoverDismissRegion"]
            XCTAssertTrue(dismissRegion.exists)
            dismissRegion.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85)).tap()
        }
        expectation(for: NSPredicate(format: "exists == false"), evaluatedWith: app.sheets["Rating"])
        waitForExpectations(timeout: 5)
    }

    func testReviewSummaryRatingLightLarge() {
        reviewSummaryRatingLarge(dark: false)
    }

    func testReviewSummaryRatingDarkLarge() {
        reviewSummaryRatingLarge(dark: true)
    }

    // MARK: - Destinations

    func testTheThreeDestinationsArePresentAndNamed() throws {
        for name in ["Home", "Browse", "Search"] {
            XCTAssertTrue(destination(name).waitForExistence(timeout: 10),
                          "\(name) must be reachable from the persistent navigation")
        }
    }

    func testHomeListsTheApprovedDeskActions() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        for label in ["Find a tag", "Random Tag", "Open Tag ID", "Teachable Tags"] {
            XCTAssertTrue(app.staticTexts[label].waitForExistence(timeout: 5),
                          "Home must offer \(label)")
        }
        XCTAssertTrue(app.staticTexts["Favorites"].exists, "Favorites is a section on Home")
    }

    func testFindATagMovesToTheSearchDestination() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        app.staticTexts["Find a tag"].tap()

        let field = app.searchFields["Search tags by title or lyrics"]
        XCTAssertTrue(field.waitForExistence(timeout: 5),
                      "Find a tag lands on the search field, not a stacked copy of Home")
        XCTAssertTrue(app.buttons["Search Tags"].exists, "The search action is a real button")
    }

    func testOpenTagIdAsksForANumber() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        app.staticTexts["Open Tag ID"].tap()

        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        XCTAssertTrue(alert.staticTexts["Enter Tag ID"].exists)
        XCTAssertTrue(alert.buttons["Open"].exists)
        alert.buttons["Cancel"].tap()
        XCTAssertTrue(homeTable.waitForExistence(timeout: 5))
    }

    func testSettingsOpensFromTheNavigationBar() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        app.navigationBars.buttons["Settings"].tap()

        XCTAssertTrue(app.staticTexts["Account"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Random Tag Filters"].exists)
        XCTAssertTrue(app.buttons["Clear Favorites"].exists)

        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(homeTable.waitForExistence(timeout: 5))
    }

    func testBrowseOffersItsFourCollections() throws {
        destination("Browse").tap()
        let picker = app.segmentedControls.firstMatch
        XCTAssertTrue(picker.waitForExistence(timeout: 10))
        for title in ["Latest", "Top Rated", "Downloads", "Classic"] {
            XCTAssertTrue(picker.buttons[title].exists, "Browse must offer \(title)")
        }
        picker.buttons["Classic"].tap()
        XCTAssertTrue(picker.buttons["Classic"].isSelected)
    }

    func testTeachableListExplainsItselfWhenEmpty() throws {
        XCTAssertTrue(homeTable.waitForExistence(timeout: 10))
        app.staticTexts["Teachable Tags"].tap()

        let heading = app.staticTexts["No teachable tags yet"]
        let list = app.tables["teachableTags"]
        XCTAssertTrue(heading.waitForExistence(timeout: 5) || list.cells.count > 0,
                      "An empty teaching list says so; a populated one shows tags")
    }
}
