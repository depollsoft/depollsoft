//
//  RealAppAppearanceUITests.swift
//  tagmasterUITests
//
//  Checks, in the real app process with the real shell, what the in-process
//  screen catalog cannot: the SwiftUI shell mounts screens with its own timing
//  (animated pushes, live network loads), and a backdrop that only showed while
//  UIKit views were cleared at the right moment went missing in the app while
//  every catalog capture still had it.
//

import XCTest
import UIKit

final class RealAppAppearanceUITests: TagMasterUITestCase {
    private var app: XCUIApplication!
    private var pad: Bool { UIDevice.current.userInterfaceIdiom == .pad }

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
    }

    override func tearDown() {
        XCUIDevice.shared.orientation = .portrait
        XCUIDevice.shared.appearance = .light
        super.tearDown()
    }

    private func launch(_ appearance: XCUIDevice.Appearance) {
        XCUIDevice.shared.appearance = appearance
        app.launchArguments.removeAll { $0 == "--appearance" || $0 == "dark" || $0 == "light" }
        app.launchArguments += ["--appearance", appearance == .dark ? "dark" : "light"]
        app.launch()
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 15))
        // The page must really be in the asked-for appearance, or a check for the
        // pole's grey on that page colour means nothing. The page's foot is plain page.
        let deadline = Date().addingTimeInterval(10)
        var level = pageLevel()
        while (appearance == .dark ? level > 40 : level < 200), Date() < deadline {
            RunLoop.current.run(until: Date().addingTimeInterval(0.3))
            level = pageLevel()
        }
        XCTAssertEqual(appearance == .dark ? level < 40 : level > 200, true,
                       "The app did not take the \(appearance == .dark ? "dark" : "light") appearance (page \(level))")
    }

    /// The brightness of the page at the screen's bottom-right corner.
    private func pageLevel() -> Int {
        let image = XCUIScreen.main.screenshot().image
        guard let cg = image.cgImage else { return -1 }
        var pixel = [UInt8](repeating: 0, count: 4)
        let context = CGContext(data: &pixel, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
                                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
        context?.draw(cg, in: CGRect(x: -CGFloat(cg.width - 8), y: -8, width: CGFloat(cg.width), height: CGFloat(cg.height)))
        return Int(pixel[0])
    }

    /// Opens a live catalog tag through the in-app Open Tag prompt.
    private func openTag(_ id: String) {
        let item = app.buttons["Open Tag"]
        if !item.isHittable { app.collectionViews.firstMatch.swipeUp() }
        item.tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.existsOrWait(timeout: 5))
        alert.textFields.firstMatch.tap()
        alert.textFields.firstMatch.typeText(id)
        alert.buttons["Open"].tap()
        let share = app.navigationBars.buttons["Share"]
        for _ in 0..<2 {
            if share.existsOrWait(timeout: 30) { break }
            let retry = app.buttons["Retry"].firstMatch
            if retry.exists { retry.tap() }
        }
        XCTAssertTrue(share.existsOrWait(timeout: 60), "The tag never loaded")
    }

    /// What the pole looks like in a screenshot region: its share of the pixels and
    /// the box its pixels span (unit coordinates of the region).
    struct PoleReading: CustomStringConvertible {
        var share: Double
        var span: CGSize
        /// The pole is there: enough of its grey, spread over a diagonal band rather
        /// than a stray patch, against the page colour around it.
        var isPole: Bool { share > 0.03 && share < 0.7 && span.width > 0.25 && span.height > 0.4 }
        var description: String { String(format: "share %.3f, span %.2f×%.2f", share, span.width, span.height) }
    }

    /// Reads the region for the watermark grey: 76/255 of mid grey over the plain
    /// page colour, white (≈217) in light appearance and black (≈38) in dark.
    private func readPole(_ image: UIImage, region: CGRect, appearance: XCUIDevice.Appearance) -> PoleReading {
        guard let cg = image.cgImage else { return PoleReading(share: 0, span: .zero) }
        let width = cg.width, height = cg.height
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        let context = CGContext(data: &pixels, width: width, height: height, bitsPerComponent: 8,
                                bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(cg, in: CGRect(x: 0, y: 0, width: width, height: height))
        let target = appearance == .dark ? 38.0 : 217.0
        let x0 = Int(region.minX * Double(width)), x1 = Int(region.maxX * Double(width))
        let y0 = Int(region.minY * Double(height)), y1 = Int(region.maxY * Double(height))
        var hits = 0, total = 0
        var minX = Int.max, maxX = Int.min, minY = Int.max, maxY = Int.min
        for y in stride(from: y0, to: y1, by: 3) {
            for x in stride(from: x0, to: x1, by: 3) {
                let i = (y * width + x) * 4
                let r = Double(pixels[i]), g = Double(pixels[i + 1]), b = Double(pixels[i + 2])
                total += 1
                guard abs(r - target) < 10, abs(g - target) < 10, abs(b - target) < 10 else { continue }
                hits += 1
                minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
            }
        }
        guard hits > 0 else { return PoleReading(share: 0, span: .zero) }
        return PoleReading(share: Double(hits) / Double(total),
                           span: CGSize(width: Double(maxX - minX) / Double(max(1, x1 - x0)),
                                        height: Double(maxY - minY) / Double(max(1, y1 - y0))))
    }

    /// Waits (at most `timeout`) for the pole to show in `region`, then attaches the
    /// screenshot and fails with the last reading if it never did.
    private func assertPole(_ name: String, region: CGRect, appearance: XCUIDevice.Appearance,
                            timeout: TimeInterval = 8, file: StaticString = #filePath, line: UInt = #line) {
        let deadline = Date().addingTimeInterval(timeout)
        var shot = XCUIScreen.main.screenshot().image
        var reading = readPole(shot, region: region, appearance: appearance)
        while !reading.isPole, Date() < deadline {
            RunLoop.current.run(until: Date().addingTimeInterval(0.3))
            shot = XCUIScreen.main.screenshot().image
            reading = readPole(shot, region: region, appearance: appearance)
        }
        let attachment = XCTAttachment(image: shot)
        attachment.name = "\(name) (\(reading))"
        attachment.lifetime = reading.isPole ? .deleteOnSuccess : .keepAlways
        add(attachment)
        XCTAssertTrue(reading.isPole, "\(name): no barber pole behind the screen (\(reading))", file: file, line: line)
    }

    /// The lower part of the screen on a phone; on iPad the detail column's, where
    /// the window's one pole lies beside the list whatever the list column shows.
    private var detailRegion: CGRect {
        pad ? CGRect(x: 0.5, y: 0.55, width: 0.45, height: 0.35) : CGRect(x: 0.05, y: 0.55, width: 0.9, height: 0.3)
    }

    private var landscapeRegion: CGRect {
        pad ? CGRect(x: 0.45, y: 0.45, width: 0.5, height: 0.45) : CGRect(x: 0.1, y: 0.45, width: 0.8, height: 0.45)
    }

    func testTheDetailShowsThePoleOnEveryPageInBothAppearances() throws {
        for appearance in [XCUIDevice.Appearance.light, .dark] {
            let style = appearance == .dark ? "dark" : "light"
            launch(appearance)
            if pad { assertPole("placeholder-\(style)", region: detailRegion, appearance: appearance) }
            openTag("1809")
            for title in ["Summary", "Details", "Tracks", "Videos"] {
                let item = app.buttons["page-\(title)"]
                XCTAssertTrue(item.existsOrWait(timeout: 10))
                item.tap()
                assertPole("detail-\(style)-\(title)", region: detailRegion, appearance: appearance)
            }
            XCUIDevice.shared.orientation = .landscapeLeft
            app.buttons["page-Details"].tap()
            assertPole("detail-\(style)-landscape", region: landscapeRegion, appearance: appearance)
            XCUIDevice.shared.orientation = .portrait
            app.terminate()
        }
    }

    func testListScreensShowThePoleInBothAppearances() throws {
        for appearance in [XCUIDevice.Appearance.light, .dark] {
            let style = appearance == .dark ? "dark" : "light"
            launch(appearance)
            assertPole("home-\(style)", region: detailRegion, appearance: appearance)
            app.buttons["Browse"].firstMatch.tap()
            for title in ["Latest", "Rating", "Downloads", "Classic"] {
                let item = app.buttons["page-\(title)"]
                XCTAssertTrue(item.existsOrWait(timeout: 10))
                item.tap()
                assertPole("browse-\(style)-\(title)", region: detailRegion, appearance: appearance)
            }
            app.navigationBars.buttons["Home"].tap()
            let teachable = app.buttons["home.lists.teachable"]
            XCTAssertTrue(teachable.existsOrWait(timeout: 5))
            teachable.tap()
            assertPole("teachable-\(style)", region: detailRegion, appearance: appearance)
            app.navigationBars.buttons["Home"].tap()
            // Results pushed from Search.
            app.navigationBars.buttons["Search"].tap()
            let field = app.searchFields.firstMatch
            XCTAssertTrue(field.existsOrWait(timeout: 5))
            field.tap()
            field.typeText("Lost\n")
            XCTAssertTrue(app.collectionViews.firstMatch.tagRows.firstMatch.existsOrWait(timeout: 60))
            assertPole("results-\(style)", region: detailRegion, appearance: appearance)
            app.terminate()
        }
    }
}

/// The main journeys through the real app, in the real process and shell, against
/// the live catalog (tag 1809, as the store capture uses).
final class RealAppFlowUITests: TagMasterUITestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // No saved-list launch arguments: a value there shadows every write the app
        // makes for the whole run, so a journey that saves could never see its result.
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 15))
    }

    private func openTag(_ id: String) {
        app.buttons["Open Tag"].tap()
        let alert = app.alerts["Open Tag"]
        XCTAssertTrue(alert.existsOrWait(timeout: 5))
        alert.textFields.firstMatch.typeText(id)
        alert.buttons["Open"].tap()
        let share = app.navigationBars.buttons["Share"]
        for _ in 0..<2 {
            if share.existsOrWait(timeout: 30) { break }
            let retry = app.buttons["Retry"].firstMatch
            if retry.exists { retry.tap() }
        }
        XCTAssertTrue(share.existsOrWait(timeout: 60), "The tag never loaded")
    }

    private func backToHome() {
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        let back = app.navigationBars.buttons["Home"]
        if back.exists { back.tap() }
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 5))
    }

    func testBrowseOpensATagAndItsPagesSwitch() {
        app.buttons["Browse"].tap()
        let row = app.collectionViews.firstMatch.tagRows.firstMatch
        XCTAssertTrue(row.existsOrWait(timeout: 60), "Latest never listed a tag")
        row.tap()
        XCTAssertTrue(app.navigationBars.buttons["Share"].existsOrWait(timeout: 60))
        for page in ["Details", "Tracks", "Videos", "Summary"] {
            let tab = app.buttons["page-\(page)"]
            XCTAssertTrue(tab.existsOrWait(timeout: 5))
            tab.tap()
            XCTAssertTrue(tab.isSelected, "\(page) is the page showing")
        }
    }

    /// Sets whether the open tag is a favourite, through whichever control the bar shows.
    private func setFavorite(_ on: Bool) {
        let direct = app.navigationBars.buttons[on ? "Add Favorite" : "Remove Favorite"]
        if direct.exists { direct.tap(); return }
        let options = app.navigationBars.buttons["Favorite and Teachable options"]
        guard options.existsOrWait(timeout: 3) else { return }
        options.tap()
        let choice = app.buttons[on ? "Add Favorite" : "Remove Favorite"]
        if choice.existsOrWait(timeout: 3) { choice.tap() } else { app.buttons["Cancel"].tap() }
    }

    private func isFavorite() -> Bool {
        let options = app.navigationBars.buttons["Favorite and Teachable options"]
        if app.navigationBars.buttons["Remove Favorite"].exists { return true }
        if app.navigationBars.buttons["Add Favorite"].exists { return false }
        guard options.existsOrWait(timeout: 3) else { return false }
        options.tap()
        let on = app.buttons["Remove Favorite"].existsOrWait(timeout: 3)
        app.buttons["Cancel"].tap()
        return on
    }

    func testFavoritingATagPutsItOnHomeAndRemovingItTakesItOff() {
        openTag("1809")
        // Saved lists persist between runs: start from "not a favourite", and put
        // back whatever was there before.
        let wasFavorite = isFavorite()
        if wasFavorite { setFavorite(false) }
        XCTAssertFalse(isFavorite())
        setFavorite(true)
        XCTAssertTrue(isFavorite(), "The tag is a favourite")
        backToHome()
        let favorite = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Tag ID 1809'")).firstMatch
        for _ in 0..<4 where !favorite.existsOrWait(timeout: 2) { app.collectionViews.firstMatch.swipeUp() }
        XCTAssertTrue(favorite.exists, "The favourite is listed on Home")
        favorite.swipeLeft()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.existsOrWait(timeout: 5))
        delete.tap()
        XCTAssertTrue(favorite.waitForNonExistence(timeout: 5), "Swiping removes the favourite")
        if wasFavorite {
            openTag("1809")
            setFavorite(true)
        }
    }

    func testANewListTakesATagFromTheDetail() {
        let name = "Warmups \(Int.random(in: 1000...9999))"
        let newList = app.buttons["home.lists.new"]
        for _ in 0..<4 where !newList.isHittable { app.collectionViews.firstMatch.swipeUp() }
        newList.tap()
        let alert = app.alerts["New list"]
        XCTAssertTrue(alert.existsOrWait(timeout: 5))
        alert.textFields.firstMatch.typeText(name)
        alert.buttons["Create"].tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", name)).firstMatch
            .existsOrWait(timeout: 5))

        openTag("1809")
        let addToList = app.buttons["summary.chip.add"]
        XCTAssertTrue(addToList.existsOrWait(timeout: 10))
        addToList.tap()
        let warmups = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'picker.row.' AND label == %@", name)).firstMatch
        XCTAssertTrue(warmups.existsOrWait(timeout: 5))
        warmups.tap()
        XCTAssertTrue(warmups.isSelected, "The tag is now in Warmups")
        app.buttons["picker.done"].tap()
        backToHome()
        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", name)).firstMatch
        XCTAssertTrue(row.existsOrWait(timeout: 5))
        XCTAssertTrue(row.label.hasSuffix("1 tag"), "The list counts its one tag: \(row.label)")

        // Deleting asks first, then the list goes; this also leaves Home as it was.
        row.swipeLeft()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.existsOrWait(timeout: 5))
        delete.tap()
        let confirm = app.alerts.firstMatch
        XCTAssertTrue(confirm.existsOrWait(timeout: 5), "Deleting a list asks first")
        XCTAssertTrue(row.exists, "The row stays until the question is answered")
        confirm.buttons["Delete"].tap()
        XCTAssertTrue(row.waitForNonExistence(timeout: 5), "The list is gone")
    }

    func testSearchListsMatchingTags() {
        app.navigationBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.existsOrWait(timeout: 5))
        field.tap()
        field.typeText("Lost\n")
        XCTAssertTrue(app.collectionViews.firstMatch.tagRows.firstMatch.existsOrWait(timeout: 60),
                      "Searching for a title lists tags")
    }
}
