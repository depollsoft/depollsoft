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

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchArguments += ["-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
        XCTAssertTrue(app.buttons["Browse"].existsOrWait(timeout: 15))
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

    private func attach(_ image: UIImage, _ name: String) {
        let attachment = XCTAttachment(image: image)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// The share of pixels in `region` (unit coordinates of the screenshot) that
    /// carry the watermark's grey: 76/255 of mid grey over the page colour.
    private func watermarkShare(_ image: UIImage, region: CGRect) -> Double {
        guard let cg = image.cgImage else { return 0 }
        let width = cg.width, height = cg.height
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        let context = CGContext(data: &pixels, width: width, height: height, bitsPerComponent: 8,
                                bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(),
                                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        context.draw(cg, in: CGRect(x: 0, y: 0, width: width, height: height))
        let dark = app.windows.firstMatch.exists && UITraitCollection.current.userInterfaceStyle == .dark
        // White page: 255·(1−α) + 128·α ≈ 217; black page ≈ 38.
        let target = dark ? 38.0 : 217.0
        let x0 = Int(region.minX * Double(width)), x1 = Int(region.maxX * Double(width))
        let y0 = Int(region.minY * Double(height)), y1 = Int(region.maxY * Double(height))
        var hits = 0, total = 0
        for y in stride(from: y0, to: y1, by: 3) {
            for x in stride(from: x0, to: x1, by: 3) {
                let i = (y * width + x) * 4
                let r = Double(pixels[i]), g = Double(pixels[i + 1]), b = Double(pixels[i + 2])
                total += 1
                if abs(r - target) < 10, abs(g - target) < 10, abs(b - target) < 10 { hits += 1 }
            }
        }
        return total == 0 ? 0 : Double(hits) / Double(total)
    }

    func testEveryDetailPageShowsTheWatermarkInTheRealApp() throws {
        openTag("1809")
        for title in ["Summary", "Details", "Tracks", "Videos"] {
            let item = app.buttons["page-\(title)"]
            XCTAssertTrue(item.existsOrWait(timeout: 10))
            item.tap()
            Thread.sleep(forTimeInterval: 1.5)
            let shot = XCUIScreen.main.screenshot().image
            attach(shot, "detail-\(title)")
            // The detail column's lower half, where the pole's lower stripes and base
            // show below short content. On iPad the detail is the right-hand column.
            let pad = UIDevice.current.userInterfaceIdiom == .pad
            let region = pad ? CGRect(x: 0.55, y: 0.6, width: 0.4, height: 0.25)
                             : CGRect(x: 0.05, y: 0.62, width: 0.9, height: 0.22)
            let share = watermarkShare(shot, region: region)
            XCTAssertGreaterThan(share, 0.03, "\(title): the barber pole is missing behind the page (\(share))")
        }
    }

    /// Opens the tag the way most people do: a tap on a saved row, an animated push.
    func testDetailOpenedFromAListInEveryAppearance() throws {
        app.terminate()
        app.launchArguments += ["-depollsoft.pitchperfect.lists",
                                "<dict><key>favorite</key><array><integer>1809</integer></array></dict>"]
        for style in [XCUIDevice.Appearance.light, .dark] {
            XCUIDevice.shared.appearance = style
            app.launch()
            let row = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Tag ID 1809' OR label BEGINSWITH 'Tag 1809'")).firstMatch
            for _ in 0..<5 where !row.existsOrWait(timeout: 2) || !row.isHittable { app.collectionViews.firstMatch.swipeUp() }
            row.tap()
            XCTAssertTrue(app.navigationBars.buttons["Share"].existsOrWait(timeout: 60))
            for title in ["Summary", "Details", "Tracks", "Videos"] {
                app.buttons["page-\(title)"].tap()
                Thread.sleep(forTimeInterval: 1.5)
                attach(XCUIScreen.main.screenshot().image, "pushed-\(style == .dark ? "dark" : "light")-\(title)")
            }
            XCUIDevice.shared.orientation = .landscapeLeft
            Thread.sleep(forTimeInterval: 1.5)
            attach(XCUIScreen.main.screenshot().image, "pushed-\(style == .dark ? "dark" : "light")-landscape")
            XCUIDevice.shared.orientation = .portrait
            app.terminate()
        }
        XCUIDevice.shared.appearance = .light
    }

    func testListScreensShowTheWatermarkInTheRealApp() throws {
        attach(XCUIScreen.main.screenshot().image, "home")
        app.buttons["Browse"].firstMatch.tap()
        XCTAssertTrue(app.buttons["page-Latest"].existsOrWait(timeout: 10))
        Thread.sleep(forTimeInterval: 1.5)
        attach(XCUIScreen.main.screenshot().image, "browse")
        app.navigationBars.buttons.element(boundBy: 0).tap()
        let teachable = app.buttons["Teachable Tags"]
        XCTAssertTrue(teachable.existsOrWait(timeout: 5))
        teachable.tap()
        Thread.sleep(forTimeInterval: 1.5)
        let shot = XCUIScreen.main.screenshot().image
        attach(shot, "teachable")
        let pad = UIDevice.current.userInterfaceIdiom == .pad
        let region = pad ? CGRect(x: 0.02, y: 0.6, width: 0.3, height: 0.25)
                         : CGRect(x: 0.05, y: 0.62, width: 0.9, height: 0.22)
        XCTAssertGreaterThan(watermarkShare(shot, region: region), 0.03,
                             "Teachable Tags: the barber pole is missing")
    }
}
