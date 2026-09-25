//
//  ScreenCatalog.swift
//  Shared by the pitchperfectTests and tagmasterTests bundles.
//
//  Renders whole screens the way the device shows them: a full-screen window on
//  the host app's scene (so the real safe areas and Liquid Glass bars apply),
//  captured with drawHierarchy after the run loop settles. The SwiftUI port was
//  checked pixel by pixel against captures of the UIKit screens it replaced;
//  see docs/ios-swiftui.md.
//
//  Captures are written only when SCREEN_CATALOG_DIR is set in the test
//  runner's environment (xcodebuild passes TEST_RUNNER_SCREEN_CATALOG_DIR
//  through), so an ordinary test run just exercises every screen state.
//

import UIKit
import XCTest

@MainActor
enum ScreenCatalog {
    /// Where captures go, or nil for an ordinary run.
    static var directory: URL? {
        guard let path = ProcessInfo.processInfo.environment["SCREEN_CATALOG_DIR"], !path.isEmpty else {
            return nil
        }
        return URL(fileURLWithPath: path, isDirectory: true)
    }

    /// The host app's foreground scene, which gives a window the device's safe areas.
    static var scene: UIWindowScene? {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
    }

    /// A key, visible, full-screen window on the host scene.
    static func makeWindow(style: UIUserInterfaceStyle = .light) -> UIWindow {
        let window: UIWindow
        if let scene {
            window = UIWindow(windowScene: scene)
            window.frame = scene.coordinateSpace.bounds
        } else {
            window = UIWindow(frame: UIScreen.main.bounds)
        }
        window.overrideUserInterfaceStyle = style
        return window
    }

    /// Spins the run loop so pending SwiftUI updates, layout and presentations land.
    static func settle(_ seconds: TimeInterval = 0.35) {
        let deadline = Date().addingTimeInterval(seconds)
        while Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.01))
        }
    }

    /// Renders `window` as it would appear on screen.
    static func image(of window: UIWindow) -> UIImage {
        window.layoutIfNeeded()
        let format = UIGraphicsImageRendererFormat()
        format.scale = window.screen.scale
        format.opaque = true
        return UIGraphicsImageRenderer(bounds: window.bounds, format: format).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
    }

    /// Settles, renders and (when a catalog directory is set) writes `<name>.png`.
    @discardableResult
    static func capture(_ name: String, window: UIWindow, settle seconds: TimeInterval = 0.35,
                        file: StaticString = #filePath, line: UInt = #line) -> UIImage {
        settle(seconds)
        // A system dialog left on the simulator (an "Open in …?" prompt, say) keeps
        // the host inactive; UIKit then never finishes presenting alerts, and the
        // capture silently shows the screen without them.
        XCTAssertEqual(window.windowScene?.activationState, .foregroundActive,
                       "The host app is not frontmost; clear the simulator's system dialogs (or reboot it)",
                       file: file, line: line)
        let image = image(of: window)
        XCTAssertGreaterThan(image.size.width, 0, file: file, line: line)
        if let directory {
            try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            let url = directory.appendingPathComponent(name).appendingPathExtension("png")
            XCTAssertNoThrow(try image.pngData()?.write(to: url), file: file, line: line)
        }
        return image
    }
}
