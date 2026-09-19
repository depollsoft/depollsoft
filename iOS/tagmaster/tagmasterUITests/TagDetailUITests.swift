//
//  TagDetailUITests.swift
//  tagmasterUITests
//
//  What is left of the XCUITest suites after the behaviour they asserted moved
//  in-process to tagmasterTests. Only two kinds of test remain here:
//
//  1. StoreScreenshotTests, which scripts/release/capture.py drives to produce
//     the App Store screenshot tour.
//  2. TagMasterPolishUITests, reduced to the cases that genuinely need a real
//     app process: the software keyboard, the live sheet-music download, the
//     accessibility frames of tab targets across a real device rotation, and
//     the dismissal of system-owned sheets (share and sign-in).
//
//  Everything else — the tab bar contract, the detail controls, rating, the
//  teachable empty state, settings, search and favourites — is now covered by
//  FavoritesBehaviorTests, SearchBehaviorTests, TagDetailBehaviorTests and
//  AppPolishTests in the tagmasterTests target.
//

import XCTest
import UIKit

// Prepare the fresh simulator's first app launch before individual case budgets.
// XCTest bounds suite startup separately from each regression case.
class TagMasterUITestCase: XCTestCase {
    private static var warmedApplication = false

    override class func setUp() {
        super.setUp()
        guard !warmedApplication else { return }
        let application = XCUIApplication()
        application.launchArguments = ["--uitesting"]
        application.launch()
        application.terminate()
        warmedApplication = true
    }
}

// XCTest's existence waiter polls after one second even for a visible element.
// Keep the bounded wait for asynchronous content, with no delay when ready.
extension XCUIElement {
    func existsOrWait(timeout: TimeInterval) -> Bool {
        exists || waitForExistence(timeout: timeout)
    }
}

// Strict regressions: no conditional passes when the requested screen is missing.
extension TagMasterPolishUITests {
    private func assertNativeTabs(_ titles: [String]) throws {
        let bar = app.tabBars["page-tab-bar"]
        XCTAssertTrue(bar.existsOrWait(timeout: 5))
        let buttons = titles.map { bar.buttons["page-\($0)"] }
        XCTAssertEqual(bar.buttons.count, 4)
        let screen = app.windows.firstMatch.frame
        let barFrame = bar.frame
        XCTAssertGreaterThan(barFrame.minY, screen.midY)
        XCTAssertLessThanOrEqual(barFrame.maxY, screen.maxY + 1)
        var previous: CGRect?
        var selectedCount = 0
        for (index, button) in buttons.enumerated() {
            XCTAssertTrue(button.existsOrWait(timeout: 5))
            // Read stable attributes in one snapshot instead of repeatedly
            // traversing the accessibility tree on the simulator.
            let snapshot = try button.snapshot()
            XCTAssertEqual(snapshot.label, titles[index])
            XCTAssertTrue(button.isHittable)
            let frame = snapshot.frame
            if snapshot.isSelected { selectedCount += 1 }
            XCTAssertGreaterThanOrEqual(frame.height, 44)
            XCTAssertGreaterThanOrEqual(frame.width, 44)
            XCTAssertTrue(barFrame.insetBy(dx: -1, dy: -1).contains(frame))
            // The native selected lens can enlarge its AX frame beyond its slot.
            // Check disjoint 44pt activation targets, not custom equal-slot bounds.
            let target = CGRect(x: frame.midX - 22, y: frame.midY - 22, width: 44, height: 44)
            if let previous { XCTAssertFalse(previous.intersects(target)) }
            previous = target
        }
        XCTAssertEqual(selectedCount, 1)
        // Browse is in the iPad primary column; Detail is in secondary.
        if UIDevice.current.userInterfaceIdiom == .pad {
            let bars = app.navigationBars.allElementsBoundByIndex.filter { !$0.frame.isEmpty }
            XCTAssertTrue(bars.contains { $0.frame.minX <= barFrame.minX + 1 && $0.frame.maxX >= barFrame.maxX - 1 })
        }
    }

}

final class TagMasterPolishUITests: TagMasterUITestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting", "-depollsoft.pitchperfect.lists",
            "<dict><key>favorite</key><array><integer>1809</integer></array></dict>"]
        app.launch()
    }

    private func capture(_ screen: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = "tagmaster-ios-after-\(screen)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func openTag() {
        let favorite = app.tables.firstMatch.cells.matching(NSPredicate(
            format: "label CONTAINS 'Tag ID 1809' OR label == 'Tag 1809. Open to load details.'")).firstMatch
        XCTAssertTrue(favorite.existsOrWait(timeout: 5))
        favorite.tap()
        assertTagLoaded()
    }

    private func assertTagLoaded() {
        let share = app.navigationBars.buttons["Share"]
        XCTAssertTrue(share.existsOrWait(timeout: 20))
        if !share.isEnabled {
            let enabled = NSPredicate(format: "enabled == true")
            expectation(for: enabled, evaluatedWith: share)
            waitForExpectations(timeout: 10)
        }
        XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 5))
    }

    func testDetailTabsInPortraitAndLandscape() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        openTag()
        app.buttons["page-Details"].tap()
        try assertTabsSurviveRotation(["Summary", "Details", "Tracks", "Videos"], selected: "Details")
    }

    func testBrowseTabsInPortraitAndLandscape() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        app.tables.staticTexts["Browse"].tap()
        XCTAssertTrue(app.tables.cells.firstMatch.existsOrWait(timeout: 30))
        app.buttons["page-Classic"].tap()
        try assertTabsSurviveRotation(["Latest", "Rating", "Downloads", "Classic"], selected: "Classic")
    }

    private func assertTabsSurviveRotation(_ titles: [String], selected: String) throws {
        XCUIDevice.shared.orientation = .landscapeLeft
        try assertNativeTabs(titles)
        XCTAssertTrue(app.buttons["page-\(selected)"].isSelected)
        XCUIDevice.shared.orientation = .portrait
        try assertNativeTabs(titles)
        XCTAssertTrue(app.buttons["page-\(selected)"].isSelected)
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
        XCTAssertTrue(lowest.existsOrWait(timeout: 5))
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
        let deadline = Date().addingTimeInterval(10)
        var retries = 0
        while Date() < deadline && !key.existsOrWait(timeout: 5) {
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
        var previousFrame = CGRect.null
        let landscape = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            let frame = key.frame
            let viewport = self.app.frame
            defer { previousFrame = frame }
            // Hittability remains true during rotation. Require stable geometry
            // inside the landscape viewport before measuring the tap target.
            return viewport.width > viewport.height && viewport.contains(frame)
                && frame == previousFrame && key.isHittable
                && frame.width >= 44 && frame.height >= 44
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [landscape], timeout: 5), .completed)
        print("TM_LAYOUT_PROBE native landscape key frame=\(key.frame)")
        XCTAssertGreaterThanOrEqual(key.frame.width, 44)
        XCTAssertGreaterThanOrEqual(key.frame.height, 44)
        layoutCapture("sheet-defect-landscape")
    }

    func testSearchKeyboardInPortraitAndLandscape() throws {
        let orientation = XCUIDevice.shared.orientation
        defer { XCUIDevice.shared.orientation = orientation }
        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(app.navigationBars.buttons["Search"].existsOrWait(timeout: 10))
        layoutCapture("home")
        app.navigationBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.existsOrWait(timeout: 5))
        field.tap()
        field.typeText("harmony")
        XCTAssertTrue(app.keyboards.firstMatch.existsOrWait(timeout: 5))
        let searchAction = app.navigationBars.buttons["Search"].firstMatch
        XCTAssertTrue(searchAction.isHittable)
        layoutCapture("search-keyboard")
        XCUIDevice.shared.orientation = .landscapeLeft
        field.tap()
        field.typeText(" quartet")
        XCTAssertTrue(app.keyboards.firstMatch.existsOrWait(timeout: 5))
        XCTAssertTrue(searchAction.isHittable)
        layoutCapture("search-keyboard-landscape")
    }

    func testSettingsLoginDismissal() throws {
        let settings = app.tables.staticTexts["Settings"]
        if !settings.isHittable { app.tables.firstMatch.swipeUp() }
        settings.tap()
        XCTAssertTrue(app.staticTexts["Log in to back up and synchronize your tag lists."].existsOrWait(timeout: 5))
        layoutCapture("settings")
        let login = app.tables.staticTexts["Log In"]
        // Never tap Log Out on an existing account.
        XCTAssertTrue(login.exists, "This read-only journey requires the signed-out entry")
        login.tap()
        XCTAssertTrue(app.buttons["Close"].existsOrWait(timeout: 5))
        layoutCapture("login-entry")
        app.buttons["Close"].tap()
        XCTAssertTrue(app.tables.staticTexts["Log In"].existsOrWait(timeout: 5))
    }

    func testDetailShareDismissal() throws {
        openTag()
        app.navigationBars.buttons["Share"].tap()
        XCTAssertTrue(app.collectionViews["activityCollectionView"].existsOrWait(timeout: 10))
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
        XCTAssertTrue(app.buttons["5 stars"].existsOrWait(timeout: 5))
        capture("rating")
        reachLowestRatingAndCancel()
        XCTAssertEqual(app.state, .runningForeground)
        let shareReady = XCTNSPredicateExpectation(predicate: NSPredicate(format: "hittable == true"),
                                                   object: app.navigationBars.buttons["Share"])
        XCTAssertEqual(XCTWaiter.wait(for: [shareReady], timeout: 10), .completed)
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
            XCTAssertTrue(app.tables.staticTexts["Browse"].existsOrWait(timeout: 15))
        }
        func snap(_ name: String) {
            Thread.sleep(forTimeInterval: 2)
            attachStoreScreenshot(name)
        }
        func open(_ id: String) {
            home()
            let item = app.tables.staticTexts["Open Tag"]
            if !item.isHittable { app.tables.firstMatch.swipeUp() }
            item.tap()
            let alert = app.alerts["Open Tag"]
            XCTAssertTrue(alert.existsOrWait(timeout: 5))
            alert.textFields.firstMatch.tap()
            alert.textFields.firstMatch.typeText(id)
            alert.buttons["Open"].tap()
            let share = app.navigationBars.buttons["Share"]
            // The live catalog occasionally fails a request. Exercise the app's
            // Retry action, but never capture its error or loading state.
            for _ in 0..<2 {
                if share.existsOrWait(timeout: 30) { break }
                let retry = app.buttons["Retry"].firstMatch
                if retry.exists { retry.tap() }
            }
            XCTAssertTrue(share.existsOrWait(timeout: 90))
            expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: share)
            waitForExpectations(timeout: 90)
            XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 10))
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
            XCTAssertTrue(item.existsOrWait(timeout: 10))
            item.tap()
            XCTAssertTrue(item.isSelected)
            if title == "Tracks" {
                let lead = app.tables.cells.containing(.staticText, identifier: "Lead").firstMatch
                XCTAssertTrue(lead.existsOrWait(timeout: 15))
                lead.tap()
                let transport = app.buttons["tagmaster.trackPlayer.playPause"]
                XCTAssertTrue(transport.existsOrWait(timeout: 10))
                expectation(for: NSPredicate(format: "enabled == true"), evaluatedWith: transport)
                waitForExpectations(timeout: 90)
                if transport.label == "Pause" { transport.tap() }
            }
            if title == "Videos" {
                XCTAssertTrue(app.tables.cells.firstMatch.existsOrWait(timeout: 30))
                app.tables.firstMatch.swipeUp()
                Thread.sleep(forTimeInterval: 8)
            }
            snap(name)
        }
        home()
        XCTAssertTrue(app.tables.staticTexts["Cheer Up, Charlie"].existsOrWait(timeout: 60))
        if UIDevice.current.userInterfaceIdiom == .pad {
            // Keep home distinct from the detail-only scenes, which use tag 122.
            app.tables.staticTexts["Their Hearts Were Full Of Spring"].tap()
            XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 30))
        }
        snap("01-home")
        app.tables.staticTexts["Browse"].tap()
        let classic = app.buttons["page-Classic"]
        XCTAssertTrue(classic.existsOrWait(timeout: 15))
        classic.tap()
        XCTAssertTrue(app.tables.cells.firstMatch.existsOrWait(timeout: 90))
        if UIDevice.current.userInterfaceIdiom == .pad {
            app.tables.cells.firstMatch.tap()
            XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 30))
        }
        snap("02-browse")
        home()
        app.navigationBars.buttons["Search"].tap()
        XCTAssertTrue(app.searchFields.firstMatch.existsOrWait(timeout: 10))
        snap("03-search")
        app.searchFields.firstMatch.tap()
        app.searchFields.firstMatch.typeText("Lone Prairie")
        app.keyboards.buttons["Search"].tap()
        let result = app.tables.cells.matching(NSPredicate(format: "label CONTAINS %@", "Lone Prairie")).firstMatch
        // UISearchController can consume the keyboard action while dismissing
        // its presentation. Submit the retained query from the navigation bar.
        if !result.existsOrWait(timeout: 5) {
            app.navigationBars.buttons["Search"].tap()
        }
        XCTAssertTrue(result.existsOrWait(timeout: 90))
        if UIDevice.current.userInterfaceIdiom == .pad {
            result.tap()
            XCTAssertTrue(app.buttons["Rate tag"].existsOrWait(timeout: 30))
        }
        snap("04-results")
    }
}

private extension XCTestCase {
    func attachStoreScreenshot(_ name: String) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let banner = springboard.descendants(matching: .any)["NotificationShortLookView"].firstMatch
        // Fresh simulators can announce system features during a capture tour.
        // Dismiss the real banner, and retry if one arrives during the screenshot.
        for _ in 0..<3 {
            if banner.exists {
                banner.swipeUp()
                let dismissed = XCTNSPredicateExpectation(
                    predicate: NSPredicate(format: "exists == false"), object: banner)
                XCTAssertEqual(XCTWaiter.wait(for: [dismissed], timeout: 5), .completed)
            }
            let screenshot = XCUIScreen.main.screenshot()
            if banner.exists { continue }
            let attachment = XCTAttachment(screenshot: screenshot)
            attachment.name = "store-\(name)"
            attachment.lifetime = .keepAlways
            add(attachment)
            return
        }
        XCTFail("A system notification is covering the store screenshot: \(name)")
    }
}
