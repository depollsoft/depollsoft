import XCTest
import UIKit

final class TMDeskUITests: XCTestCase {
    var app: XCUIApplication!
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting", "-AppleInterfaceStyle", "Light"]
        app.launch()
        XCTAssertTrue(app.tables.firstMatch.waitForExistence(timeout: 10))
    }
    private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "ios-" + (UIDevice.current.userInterfaceIdiom == .pad ? "tablet-" : "phone-") + name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
    private func back() { app.navigationBars.buttons.element(boundBy: 0).tap() }
    private func openID() {
        app.staticTexts["Open Tag ID"].tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        alert.textFields.firstMatch.tap()
        alert.textFields.firstMatch.typeText("1")
        alert.buttons["Open"].tap()
        XCTAssertTrue(app.buttons["Learning Tracks"].waitForExistence(timeout: 20))
        XCTAssertTrue(app.buttons["Sheet Music"].exists)
    }
    func testCharacterRoutesAndCaptures() {
        let tablet = UIDevice.current.userInterfaceIdiom == .pad
        if tablet { XCUIDevice.shared.orientation = .landscapeLeft }
        defer { XCUIDevice.shared.orientation = .portrait }
        XCTAssertEqual(app.tabBars.count, 0)
        for name in ["Find a tag", "Browse", "Random Tag", "Open Tag ID", "Teachable Tags"] {
            XCTAssertTrue(app.staticTexts[name].isHittable)
        }
        capture("home")
        app.staticTexts["Find a tag"].tap()
        let field = app.searchFields["Search tags by title or lyrics"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText("Smile")
        app.buttons["Search Tags"].tap()
        let results = app.tables["tagResults"]
        XCTAssertTrue(results.cells.firstMatch.waitForExistence(timeout: 20))
        let result = results.cells.containing(.staticText, identifier: "(You Make Me) Smile").firstMatch
        XCTAssertTrue(result.exists)
        result.tap()
        XCTAssertTrue(app.buttons["Learning Tracks"].waitForExistence(timeout: 20))
        app.buttons["Learning Tracks"].tap()
        XCTAssertTrue(app.staticTexts["No learning tracks"].waitForExistence(timeout: 5))
        if !tablet {
            XCTAssertTrue(app.navigationBars["Learning Tracks"].exists)
            back()
            XCTAssertTrue(app.buttons["Learning Tracks"].exists)
        }
        back()
        XCTAssertTrue(results.waitForExistence(timeout: 5))
        back()
        XCTAssertEqual(field.value as? String, "Smile")
        back()
        app.staticTexts["Browse"].tap()
        let collection = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Collection: ")).firstMatch
        XCTAssertTrue(collection.waitForExistence(timeout: 5))
        XCTAssertEqual(app.segmentedControls.count, 0)
        collection.tap()
        app.buttons["Classic"].tap()
        XCTAssertTrue(app.buttons["Collection: Classic"].exists)
        XCTAssertTrue(results.cells.firstMatch.waitForExistence(timeout: 20))
        if tablet { capture("browse") }
        back()
        openID()
        XCTAssertTrue(app.staticTexts["Smile"].exists)
        let pitch = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Sound the key")).firstMatch
        XCTAssertTrue(pitch.exists)
        XCTAssertGreaterThanOrEqual(pitch.frame.height, 44)
        if !tablet {
            XCTAssertGreaterThanOrEqual(pitch.frame.width, app.frame.width - 40)
            XCTAssertLessThanOrEqual(pitch.frame.width, app.frame.width - 32)
        }
        XCTAssertEqual(app.segmentedControls.count, 0)
        capture("tag")
        app.buttons["Learning Tracks"].tap()
        XCTAssertTrue(app.tables["learningTracks"].waitForExistence(timeout: 5))
        if !tablet {
            capture("tracks")
            back()
        }
        app.buttons["Details"].tap()
        XCTAssertTrue(app.staticTexts["Bobby Gray, Jr"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Bobby Gray, Jr"].exists)
        XCTAssertTrue(app.buttons["Daniel Gillis"].exists)
        if !tablet { back() }
        app.navigationBars.buttons["Share tag"].tap()
        XCTAssertTrue(app.otherElements["ActivityListView"].waitForExistence(timeout: 5))
    }

    func testDarkAccessibilityRatingAndMaterialBack() {
        app.terminate()
        app.launchArguments = ["--uitesting", "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL", "-AppleInterfaceStyle", "Dark"]
        app.launch()
        XCTAssertTrue(app.tables.firstMatch.waitForExistence(timeout: 10))
        openID()
        let summary = app.scrollViews.containing(.button, identifier: "Rate").firstMatch
        let rate = app.buttons["Rate"]
        let stars = app.staticTexts["Rated 3.17 out of 5"]
        let rating = app.staticTexts["3.17 out of 5"]
        for _ in 0..<8 {
            if stars.isHittable && rating.isHittable && rate.isHittable { break }
            summary.swipeUp()
        }
        XCTAssertTrue(stars.isHittable)
        XCTAssertTrue(rating.isHittable)
        XCTAssertTrue(rate.isHittable)
        XCTAssertGreaterThanOrEqual(rate.frame.height, 44)
        XCTAssertGreaterThanOrEqual(rate.frame.minY, rating.frame.maxY)
        XCTAssertGreaterThanOrEqual(rating.frame.minY, stars.frame.maxY)
        XCTAssertLessThanOrEqual(stars.frame.maxX, app.frame.maxX - 16)
        for _ in 0..<8 {
            if app.buttons["Learning Tracks"].isHittable { break }
            summary.swipeDown()
        }
        app.buttons["Learning Tracks"].tap()
        XCTAssertTrue(app.navigationBars["Learning Tracks"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.tables["learningTracks"].exists)
        capture("tracks-dark-large")
        back()
        XCTAssertTrue(app.buttons["Learning Tracks"].exists)
        XCTAssertEqual(app.tabBars.count, 0)
    }
    func testCatalogGeometryAndRotatedMaterial() {
        let tablet = UIDevice.current.userInterfaceIdiom == .pad
        if tablet { XCUIDevice.shared.orientation = .landscapeLeft }
        defer { XCUIDevice.shared.orientation = .portrait }
        app.staticTexts["Browse"].tap()
        let collection = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Collection: ")).firstMatch
        XCTAssertTrue(collection.waitForExistence(timeout: 5))
        collection.tap()
        app.buttons["Classic"].tap()
        let results = app.tables["tagResults"]
        XCTAssertTrue(results.cells.element(boundBy: 2).waitForExistence(timeout: 20))
        for index in 0..<3 {
            let cell = results.cells.element(boundBy: index)
            XCTAssertTrue(cell.isHittable)
            XCTAssertGreaterThanOrEqual(cell.frame.height, 44)
            XCTAssertLessThan(cell.frame.height, 200)
        }
        let title = results.staticTexts["I Love to Sing 'Em"]
        XCTAssertTrue(title.exists)
        XCTAssertGreaterThanOrEqual(title.frame.height, 18, "Result titles must not compress to a clipped sliver")
        if tablet { capture("browse") }
        back()
        openID()
        app.buttons["Videos"].tap()
        if !tablet { XCTAssertTrue(app.navigationBars["Videos"].waitForExistence(timeout: 5)) }
        XCUIDevice.shared.orientation = tablet ? .portrait : .landscapeLeft
        if tablet {
            XCTAssertTrue(app.buttons["Learning Tracks"].waitForExistence(timeout: 5))
            XCTAssertTrue(app.tables["tagVideos"].exists)
        } else {
            XCTAssertTrue(app.navigationBars["Videos"].waitForExistence(timeout: 5))
            back()
            XCTAssertTrue(app.buttons["Learning Tracks"].exists)
        }
    }

}
