import XCTest
import UIKit

final class TMDensityUITests: XCTestCase {
    private var app: XCUIApplication!
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
    }
    override func tearDownWithError() throws {
        app.terminate()
        app.launchArguments = ["--density-cleanup"]
        app.launch()
        XCTAssertTrue(app.otherElements["density.restored"].waitForExistence(timeout: 10))
        app.terminate()
        XCUIDevice.shared.orientation = .portrait
    }
    private func launch(_ count: Int, large: Bool = false) {
        app.terminate()
        app.launchArguments = ["--density-fixture", String(count)]
        if large { app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"] }
        app.launch()
        XCTAssertTrue(app.cells["home.favorites"].waitForExistence(timeout: 10))
    }
    private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "ios-" + (UIDevice.current.userInterfaceIdiom == .pad ? "tablet-" : "phone-") + name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
    private func back(_ name: String) { app.navigationBars.buttons[name].tap() }
    private func assertHome(_ count: Int) {
        XCTAssertEqual(app.tables.firstMatch.cells.count, 6)
        for id in ["home.favorites", "home.teachable"] {
            XCTAssertTrue(app.cells[id].isHittable)
            XCTAssertEqual(app.cells[id].value as? String, "\(count) tags")
            XCTAssertGreaterThanOrEqual(app.cells[id].frame.height, 44)
        }
        XCTAssertFalse(app.navigationBars.buttons["Edit favorites"].exists)
        XCTAssertEqual(app.tabBars.count, 0)
    }
    func testZeroOneAndHundredKeepPeerHomeAndEmptyRoutes() {
        for count in [0, 1, 100] {
            launch(count)
            assertHome(count)
            app.cells["home.favorites"].tap()
            XCTAssertTrue(app.navigationBars["Favorites"].waitForExistence(timeout: 5))
            XCTAssertEqual(app.tables["favorites"].cells.count, count)
            if count == 0 { XCTAssertTrue(app.staticTexts["No favorites yet"].exists) }
            back("Home")
            app.cells["home.teachable"].tap()
            XCTAssertTrue(app.navigationBars["Teachable Tags"].waitForExistence(timeout: 5))
            XCTAssertEqual(app.tables["teachableTags"].cells.count, count)
            if count == 0 { XCTAssertTrue(app.staticTexts["No teachable tags yet"].exists) }
            back("Home")
        }
    }
    func testPopulatedRoutesMutationBackAndCaptures() {
        if UIDevice.current.userInterfaceIdiom == .pad { XCUIDevice.shared.orientation = .landscapeLeft }
        launch(100)
        assertHome(100)
        capture("home")
        app.cells["home.favorites"].tap()
        let favorites = app.tables["favorites"]
        XCTAssertTrue(favorites.waitForExistence(timeout: 5))
        XCTAssertTrue(favorites.cells["tag.900005"].isHittable)
        for index in 0..<5 {
            XCTAssertGreaterThanOrEqual(favorites.cells.element(boundBy: index).frame.height, 44)
            XCTAssertLessThanOrEqual(favorites.cells.element(boundBy: index).frame.height, 72)
        }
        capture("favorites")
        let edit = app.navigationBars.buttons["favorites.edit"]
        XCTAssertEqual(edit.label, "Edit favorites")
        edit.tap()
        let reorder = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Reorder'")).firstMatch
        XCTAssertTrue(reorder.exists)
        XCTAssertEqual(edit.label, "Done editing favorites")
        edit.tap()
        XCTAssertEqual(edit.label, "Edit favorites")
        XCTAssertFalse(reorder.exists)
        favorites.cells["tag.900001"].swipeLeft()
        app.buttons["Remove"].tap()
        XCTAssertEqual(favorites.cells.count, 99)
        favorites.swipeUp()
        let visible = favorites.cells.allElementsBoundByIndex.first { $0.isHittable && $0.frame.minY > 120 }!
        let id = visible.identifier
        let originalY = visible.frame.minY
        visible.tap()
        XCTAssertTrue(app.buttons["Details"].waitForExistence(timeout: 10))
        back("Favorites")
        XCTAssertEqual(favorites.cells[id].frame.minY, originalY, accuracy: 2)
        back("Home")
        XCTAssertEqual(app.cells["home.favorites"].value as? String, "99 tags")
        XCTAssertEqual(app.cells["home.teachable"].value as? String, "100 tags")
        app.cells["home.teachable"].tap()
        let teachable = app.tables["teachableTags"]
        XCTAssertTrue(teachable.waitForExistence(timeout: 5))
        XCTAssertTrue(teachable.cells["tag.900001"].exists)
        XCTAssertTrue(teachable.cells["tag.900005"].isHittable)
        capture("teachable")
        back("Home")
        app.staticTexts["Browse"].tap()
        let browse = app.tables["tagResults"]
        XCTAssertTrue(browse.cells["tag.900005"].waitForExistence(timeout: 10))
        XCTAssertTrue(browse.cells["tag.900005"].isHittable)
        capture("browse")
    }
    func testLongTitleGeometryConfirmationCapture() {
        launch(100)
        app.cells["home.favorites"].tap()
        let normalHeight = app.tables["favorites"].cells["tag.900001"].frame.height
        launch(100, large: true)
        let home = app.tables.firstMatch
        for _ in 0..<6 {
            if app.cells["home.favorites"].isHittable { break }
            home.swipeUp()
        }
        app.cells["home.favorites"].tap()
        let favorites = app.tables["favorites"]
        let largeHeight = favorites.cells["tag.900001"].frame.height
        XCTAssertGreaterThan(largeHeight, normalHeight)
        let longTitle = favorites.cells["tag.900008"]
        let top = app.navigationBars.firstMatch.frame.maxY + 8
        for _ in 0..<12 {
            let frame = longTitle.frame
            if frame.minY >= top && frame.maxY < app.frame.maxY - 24 { break }
            if frame.minY > top {
                let distance = min(300, frame.minY - top)
                let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.85))
                start.press(forDuration: 0.1, thenDragTo: start.withOffset(CGVector(dx: 0, dy: -distance)))
            } else {
                favorites.swipeDown(velocity: .slow)
            }
        }
        XCTAssertGreaterThanOrEqual(longTitle.frame.minY, top)
        XCTAssertLessThan(longTitle.frame.maxY, app.frame.maxY - 24)
        print("DENSITY native first row normal=\(normalHeight) XXXL=\(largeHeight); long title frame=\(longTitle.frame)")
        let hierarchy = XCTAttachment(string: favorites.debugDescription)
        hierarchy.name = "ios-phone-native-geometry-synthetic"
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
        capture("long-title-dark-large-confirm")
    }
    func testDarkLargeListWrappingAndCaptures() {
        launch(100, large: true)
        // Both peer entries remain present even when accessibility text requires scrolling.
        for id in ["home.favorites", "home.teachable"] { XCTAssertTrue(app.cells[id].exists) }
        capture("home-dark-large")
        let home = app.tables.firstMatch
        while !app.cells["home.favorites"].isHittable { home.swipeUp() }
        app.cells["home.favorites"].tap()
        let favorites = app.tables["favorites"]
        XCTAssertTrue(favorites.waitForExistence(timeout: 5))
        XCTAssertGreaterThan(favorites.cells["tag.900001"].frame.height, 72)
        capture("favorites-dark-large")
        for _ in 0..<8 {
            if favorites.cells["tag.900008"].isHittable { break }
            favorites.swipeUp()
        }
        let longTitle = favorites.cells["tag.900008"]
        XCTAssertTrue(longTitle.isHittable)
        XCTAssertTrue(longTitle.label.contains("all the way across the room and brings us home"))
        XCTAssertGreaterThan(longTitle.frame.height, 150)
        capture("long-title-dark-large")
        back("Home")
        while !app.cells["home.teachable"].isHittable { home.swipeUp() }
        app.cells["home.teachable"].tap()
        XCTAssertTrue(app.tables["teachableTags"].waitForExistence(timeout: 5))
        capture("teachable-dark-large")
        back("Home")
        while !app.staticTexts["Browse"].isHittable { home.swipeDown() }
        app.staticTexts["Browse"].tap()
        XCTAssertTrue(app.tables["tagResults"].cells.firstMatch.waitForExistence(timeout: 10))
        capture("browse-dark-large")
    }
}
