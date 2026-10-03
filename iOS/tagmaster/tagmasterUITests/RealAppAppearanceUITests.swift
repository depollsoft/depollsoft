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

    /// What the pole looks like in a screenshot region: its share of the pixels, the
    /// box they span (unit coordinates of the region), where they sit across three
    /// bands of the region's height, and the grid of cells holding pole pixels.
    struct PoleReading: CustomStringConvertible {
        var share: Double
        var span: CGSize
        /// The mean x (unit) of the pole's pixels in the top, middle and bottom thirds.
        var bandCentres: [Double] = []
        /// Which cells of a 12×12 grid over the region hold pole pixels.
        var cells: Set<Int> = []
        /// The pole is there: enough of its grey, spread over a band that runs
        /// diagonally (its centre moves across the thirds), not a stray patch.
        var isPole: Bool {
            guard share > 0.03, share < 0.7, span.width > 0.25, span.height > 0.4, bandCentres.count == 3 else { return false }
            let drift = bandCentres[2] - bandCentres[0]
            let steady = (bandCentres[1] - bandCentres[0]) * drift >= 0
            return abs(drift) > 0.05 && steady
        }
        /// How well two readings of the same region agree on where the pole is.
        func overlap(with other: PoleReading) -> Double {
            let union = cells.union(other.cells).count
            return union == 0 ? 0 : Double(cells.intersection(other.cells).count) / Double(union)
        }
        var description: String {
            String(format: "share %.3f, span %.2f×%.2f, bands %@", share, span.width, span.height,
                   bandCentres.map { String(format: "%.2f", $0) }.joined(separator: "/"))
        }
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
        var bandSums = [0.0, 0.0, 0.0], bandCounts = [0, 0, 0]
        var cells: Set<Int> = []
        for y in stride(from: y0, to: y1, by: 3) {
            for x in stride(from: x0, to: x1, by: 3) {
                let i = (y * width + x) * 4
                let r = Double(pixels[i]), g = Double(pixels[i + 1]), b = Double(pixels[i + 2])
                total += 1
                guard abs(r - target) < 10, abs(g - target) < 10, abs(b - target) < 10 else { continue }
                hits += 1
                minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
                let ux = Double(x - x0) / Double(max(1, x1 - x0)), uy = Double(y - y0) / Double(max(1, y1 - y0))
                let band = min(2, Int(uy * 3))
                bandSums[band] += ux
                bandCounts[band] += 1
                cells.insert(min(11, Int(uy * 12)) * 12 + min(11, Int(ux * 12)))
            }
        }
        guard hits > 0 else { return PoleReading(share: 0, span: .zero) }
        let centres = zip(bandSums, bandCounts).compactMap { $1 > 0 ? $0 / Double($1) : nil }
        return PoleReading(share: Double(hits) / Double(total),
                           span: CGSize(width: Double(maxX - minX) / Double(max(1, x1 - x0)),
                                        height: Double(maxY - minY) / Double(max(1, y1 - y0))),
                           bandCentres: centres, cells: cells)
    }

    /// Waits (at most `timeout`) for the pole to show in `region`, then attaches the
    /// screenshot and fails with the last reading if it never did.
    @discardableResult
    private func assertPole(_ name: String, region: CGRect, appearance: XCUIDevice.Appearance,
                            timeout: TimeInterval = 8, file: StaticString = #filePath, line: UInt = #line) -> PoleReading {
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
        return reading
    }

    /// The same window-wide pole, in the same place, as `baseline` showed: a slice
    /// placed from a stale position would land a column's width away.
    private func assertSamePole(_ name: String, as baseline: PoleReading, region: CGRect, appearance: XCUIDevice.Appearance,
                                file: StaticString = #filePath, line: UInt = #line) {
        let deadline = Date().addingTimeInterval(8)
        var reading = readPole(XCUIScreen.main.screenshot().image, region: region, appearance: appearance)
        // A page's own text covers some of the pole (about a quarter at most); a slice
        // placed from a stale position lands a column away and overlaps almost nothing.
        while reading.overlap(with: baseline) < 0.5, Date() < deadline {
            RunLoop.current.run(until: Date().addingTimeInterval(0.3))
            reading = readPole(XCUIScreen.main.screenshot().image, region: region, appearance: appearance)
        }
        XCTAssertGreaterThan(reading.overlap(with: baseline), 0.5,
                             "\(name): the pole moved (\(reading) vs \(baseline))", file: file, line: line)
    }

    /// The lower part of the screen on a phone; on iPad the detail column's, where
    /// the window's one pole lies beside the list whatever the list column shows.
    private var detailRegion: CGRect {
        // On iPad the window's pole runs diagonally through the detail column's upper
        // two thirds; below that only its tail reaches in.
        pad ? CGRect(x: 0.42, y: 0.2, width: 0.53, height: 0.55) : CGRect(x: 0.05, y: 0.55, width: 0.9, height: 0.3)
    }

    /// The iPad list column below its rows, where only the page shows. Before iOS 26
    /// the column's screens draw their slice of the window's pole; from iOS 26 the
    /// glass sidebar covers the split's pole, and UIKit's glass showed little of it.
    private var listRegion: CGRect { CGRect(x: 0.02, y: 0.62, width: 0.3, height: 0.33) }

    private var listShowsPole: Bool {
        if #available(iOS 26.0, *) { return false }
        return true
    }

    /// Screenshots stay in portrait pixels: a phone's landscape page bar lies along
    /// their left edge, and its glass must stay out of the region.
    private var landscapeRegion: CGRect {
        pad ? CGRect(x: 0.45, y: 0.45, width: 0.5, height: 0.45) : CGRect(x: 0.22, y: 0.36, width: 0.55, height: 0.3)
    }

    func testTheDetailShowsThePoleOnEveryPageInLightAppearance() {
        assertDetailPole(.light)
    }

    func testTheDetailShowsThePoleOnEveryPageInDarkAppearance() {
        assertDetailPole(.dark)
    }

    private func assertDetailPole(_ appearance: XCUIDevice.Appearance) {
        let style = appearance == .dark ? "dark" : "light"
        launch(appearance)
        // On iPad the placeholder's slice is the window's pole where the detail
        // column lies; every page opened there must show that same pole.
        let baseline: PoleReading? = pad ? assertPole("placeholder-\(style)", region: detailRegion, appearance: appearance) : nil
        openTag("1809")
        for title in ["Summary", "Details", "Tracks", "Videos"] {
            let item = app.buttons["page-\(title)"]
            XCTAssertTrue(item.existsOrWait(timeout: 10))
            item.tap()
            assertPole("detail-\(style)-\(title)", region: detailRegion, appearance: appearance)
            if let baseline {
                assertSamePole("detail-\(style)-\(title)", as: baseline, region: detailRegion, appearance: appearance)
            }
        }
        XCUIDevice.shared.orientation = .landscapeLeft
        app.buttons["page-Details"].tap()
        assertPole("detail-\(style)-landscape", region: landscapeRegion, appearance: appearance)
        XCUIDevice.shared.orientation = .portrait
        if let baseline {
            // Back from landscape with no further interaction: the slice follows the column.
            assertSamePole("detail-\(style)-after-rotation", as: baseline, region: detailRegion, appearance: appearance)
            // A keyboard in the list column leaves the detail's pole where it was.
            app.navigationBars.buttons["Search"].tap()
            let field = app.searchFields.firstMatch
            XCTAssertTrue(field.existsOrWait(timeout: 5))
            field.focusForTyping()
            field.typeText("Lo")
            assertSamePole("detail-\(style)-keyboard", as: baseline, region: detailRegion, appearance: appearance)
        }
        app.terminate()
    }

    func testListScreensShowThePoleInLightAppearance() {
        assertListPole(.light)
    }

    func testListScreensShowThePoleInDarkAppearance() {
        assertListPole(.dark)
    }

    private func assertListPole(_ appearance: XCUIDevice.Appearance) {
        let style = appearance == .dark ? "dark" : "light"
        launch(appearance)
        // A phone's screen is its own column; on iPad the list column's own region.
        let region = pad ? listRegion : detailRegion
        let checksList = !pad || listShowsPole
        func check(_ name: String) {
            if checksList { assertPole(name, region: region, appearance: appearance) }
            if pad {
                // Whatever the list column shows, the detail column keeps its pole.
                assertPole("\(name)-detail", region: detailRegion, appearance: appearance)
            }
        }
        check("home-\(style)")
        app.buttons["Browse"].firstMatch.tap()
        for title in ["Latest", "Rating", "Downloads", "Classic"] {
            let item = app.buttons["page-\(title)"]
            XCTAssertTrue(item.existsOrWait(timeout: 10))
            item.tap()
            check("browse-\(style)-\(title)")
        }
        app.navigationBars.buttons["Home"].tap()
        let teachable = app.buttons["home.lists.teachable"]
        XCTAssertTrue(teachable.existsOrWait(timeout: 5))
        teachable.tap()
        check("teachable-\(style)")
        app.navigationBars.buttons["Home"].tap()
        // Results pushed from Search.
        app.navigationBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.existsOrWait(timeout: 5))
        field.focusForTyping()
        field.typeText("Lost\n")
        XCTAssertTrue(app.collectionViews.firstMatch.tagRows.firstMatch.existsOrWait(timeout: 60))
        check("results-\(style)")
        app.terminate()
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
        confirm.buttons["Delete"].waitUntilHittable(timeout: 3)
        confirm.buttons["Delete"].tap()
        XCTAssertTrue(row.waitForNonExistence(timeout: 5), "The list is gone")
    }

    private func createList(_ name: String) {
        let newList = app.buttons["home.lists.new"]
        for _ in 0..<4 where !newList.isHittable { app.collectionViews.firstMatch.swipeUp() }
        newList.tap()
        let alert = app.alerts["New list"]
        XCTAssertTrue(alert.existsOrWait(timeout: 5))
        alert.textFields.firstMatch.typeText(name)
        alert.buttons["Create"].tap()
        XCTAssertTrue(listRow(name).existsOrWait(timeout: 5))
    }

    private func listRow(_ name: String) -> XCUIElement {
        // While editing SwiftUI prefixes the label with "Remove, ".
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'home.list.' AND (label BEGINSWITH %@ OR label BEGINSWITH %@)",
                                         name, "Remove, \(name)")).firstMatch
    }

    /// Deletes a list while editing: its red delete control (the row's leading edge),
    /// the Delete it reveals, and the question.
    private func deleteWhileEditing(_ row: XCUIElement) {
        row.coordinate(withNormalizedOffset: CGVector(dx: 0.05, dy: 0.5)).tap()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.existsOrWait(timeout: 5), "The delete control reveals Delete")
        delete.tap()
        let confirm = app.alerts.firstMatch
        XCTAssertTrue(confirm.existsOrWait(timeout: 5), "Deleting a list asks first")
        XCTAssertTrue(row.exists, "The row stays until the question is answered")
        // A tap while the alert is still animating in is dropped; answer it once it
        // takes taps, and again if it is still up.
        let answer = confirm.buttons["Delete"]
        for _ in 0..<3 where confirm.exists {
            _ = answer.waitUntilHittable(timeout: 3)
            answer.tap()
            _ = confirm.waitForNonExistence(timeout: 2)
        }
        XCTAssertFalse(confirm.exists, "The question was answered")
    }

    func testEditingHomeReordersAndDeletesListsWithItsControls() {
        removeLeftoverLists()
        let suffix = Int.random(in: 1000...9999)
        let first = "Alpha \(suffix)", second = "Beta \(suffix)"
        createList(first)
        createList(second)
        XCTAssertLessThan(listRow(first).frame.minY, listRow(second).frame.minY, "New lists are added last")

        app.navigationBars.buttons["Edit"].tap()
        XCTAssertFalse(listRow(first).isEnabled, "Rows are not controls while editing")
        // Every list has a reorder handle at its trailing edge while editing (SwiftUI
        // names them all alike); drag the second list's above the first.
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS 'Reorder'")).firstMatch.existsOrWait(timeout: 5),
                      "The lists have reorder handles while editing")
        listRow(second).coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.5))
            .press(forDuration: 0.6, thenDragTo: listRow(first).coordinate(withNormalizedOffset: CGVector(dx: 0.93, dy: 0.2)))
        XCTAssertTrue(waitUntil(5) { self.listRow(second).frame.minY < self.listRow(first).frame.minY },
                      "The dragged list now comes first")

        deleteWhileEditing(listRow(first))
        XCTAssertTrue(listRow(first).waitForNonExistence(timeout: 5), "The list is gone")
        // Leave Home as it was.
        deleteWhileEditing(listRow(second))
        XCTAssertTrue(listRow(second).waitForNonExistence(timeout: 5))
    }

    /// Lists an earlier, interrupted run of these journeys left behind.
    private func removeLeftoverLists() {
        let leftovers = NSPredicate(format: "identifier BEGINSWITH 'home.list.' AND label MATCHES '(Remove, )?(Alpha|Beta|Warmups) [0-9]{4},.*'")
        var guardCount = 0
        while app.buttons.matching(leftovers).firstMatch.exists, guardCount < 10 {
            let row = app.buttons.matching(leftovers).firstMatch
            row.swipeLeft()
            let delete = app.buttons["Delete"].firstMatch
            if delete.existsOrWait(timeout: 3) { delete.tap() }
            let confirm = app.alerts.firstMatch
            if confirm.existsOrWait(timeout: 3) {
                confirm.buttons["Delete"].waitUntilHittable(timeout: 3)
                confirm.buttons["Delete"].tap()
            }
            guardCount += 1
        }
    }

    private func waitUntil(_ timeout: TimeInterval, _ condition: () -> Bool) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition(), Date() < deadline { RunLoop.current.run(until: Date().addingTimeInterval(0.2)) }
        return condition()
    }

    func testSearchListsMatchingTags() {
        app.navigationBars.buttons["Search"].tap()
        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.existsOrWait(timeout: 5))
        field.focusForTyping()
        field.typeText("Lost\n")
        XCTAssertTrue(app.collectionViews.firstMatch.tagRows.firstMatch.existsOrWait(timeout: 60),
                      "Searching for a title lists tags")
    }
}
