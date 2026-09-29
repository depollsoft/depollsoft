//
//  TestRendering.swift
//  Shared by the pitchperfectTests and tagmasterTests bundles.
//
//  The first time a process draws a SwiftUI Canvas (or an alert's material) it
//  builds the renderer's shader cache; libCoreFSCache logs "fopen failed for
//  data file" as it misses. Locally that takes a fraction of a second. On a
//  fresh CI simulator, which renders without a GPU, it once stalled the main
//  thread for over 20 s inside Tag Master's first barber pole and spent a
//  test's whole 30 s budget (testRandomTagExplainsWhenNothingMatchesAndCanRetry).
//  Drawing the app's Canvas views and an alert as the bundle starts pays that
//  cost once, under the run's startup allowance instead of any one test's.
//

import SwiftUI
import UIKit

@MainActor
enum TestRendering {
    private static var warmed = false

    /// Puts `views` and an alert on screen and lets them draw.
    static func warmUp(_ views: [AnyView]) {
        guard !warmed else { return }
        warmed = true
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        let previousKey = scene?.windows.first { $0.isKeyWindow }
        let window = scene.map { UIWindow(windowScene: $0) } ?? UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        let host = UIHostingController(rootView: VStack(spacing: 8) {
            ForEach(views.indices, id: \.self) { views[$0] }
        })
        window.rootViewController = host
        window.makeKeyAndVisible()
        draw(window)

        let alert = UIAlertController(title: "Warming up", message: "Drawn once before the first test.", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        host.present(alert, animated: false)
        let deadline = Date(timeIntervalSinceNow: 120)
        while alert.presentingViewController == nil, Date() < deadline {
            RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.01))
        }
        draw(window)
        alert.dismiss(animated: false)
        RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.1))
        window.rootViewController = nil
        window.isHidden = true
        previousKey?.makeKey()
    }

    /// Lets the render server draw a few frames, then draws in process too.
    private static func draw(_ window: UIWindow) {
        RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.5))
        _ = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
    }
}
