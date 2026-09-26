//
//  PitchPerfectFinalReviewTests.swift
//  pitchperfectTests
//
//  What the last review of the port asked for: the login text without HTML
//  import at run time, Settings presentation that survives transitions and
//  refusals, Settings owned by its window, and the selector and table margins
//  checked against UIKit's own views rather than the port's arithmetic.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class LoginExplanationTests: XCTestCase {
    /// The built text is what the original HTML imports to, run for run.
    func testTheBuiltTextIsWhatTheHTMLImportsTo() throws {
        let imported = try NSAttributedString(
            data: Data(LoginExplanation.html.utf8),
            options: [.documentType: NSAttributedString.DocumentType.html,
                      .characterEncoding: String.Encoding.utf8.rawValue],
            documentAttributes: nil)
        let built = LoginExplanation.text
        XCTAssertEqual(built.string, imported.string)

        func runs(_ text: NSAttributedString) -> [String] {
            var result: [String] = []
            text.enumerateAttributes(in: NSRange(location: 0, length: text.length)) { attributes, range, _ in
                let font = attributes[.font] as? UIFont
                let paragraph = attributes[.paragraphStyle] as? NSParagraphStyle
                let color = (attributes[.foregroundColor] as? UIColor)?.cgColor.components ?? []
                result.append([
                    "\(range.location)+\(range.length)",
                    font?.fontName ?? "-", "\(font?.pointSize ?? 0)",
                    "\(paragraph?.alignment.rawValue ?? -1)", "\(paragraph?.paragraphSpacing ?? -1)",
                    "\(paragraph?.paragraphSpacingBefore ?? -1)", "\(paragraph?.lineSpacing ?? -1)",
                    "\(paragraph?.defaultTabInterval ?? -1)", "\(color)",
                    "\(attributes[.kern] ?? "-")", "\(attributes[.strokeWidth] ?? "-")",
                ].joined(separator: " "))
            }
            return result
        }
        XCTAssertEqual(runs(built), runs(imported))
    }
}

/// A root that refuses the next presentation, as UIKit does when it cannot
/// present at that moment.
private final class RefusingRoot: UIHostingController<PitchPerfectRoot> {
    var refuseNext = true

    override func present(_ controller: UIViewController, animated: Bool, completion: (() -> Void)? = nil) {
        if refuseNext {
            refuseNext = false
            return
        }
        super.present(controller, animated: animated, completion: completion)
    }
}

@MainActor
final class SettingsPresentationTests: PitchPerfectTestCase {
    private func topPresented(in window: UIWindow) -> UIViewController? {
        var top = window.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top === window.rootViewController ? nil : top
    }

    /// Asking for Settings while another sheet is still animating away waits for
    /// it and then shows Settings, instead of losing the request.
    func testSettingsAskedForDuringAnAnimatedDismissalStillAppears() throws {
        let app = try launch()
        UIView.setAnimationsEnabled(true)
        defer { UIView.setAnimationsEnabled(false) }
        let other = UIViewController()
        other.view.backgroundColor = .systemBackground
        app.host.present(other, animated: true)
        app.ui.wait(3) { other.view.window != nil && !other.isBeingPresented }
        other.dismiss(animated: true)
        XCTAssertTrue(other.isBeingDismissed, "the dismissal is still in flight")
        app.openSettings()
        app.ui.wait(5) { self.topPresented(in: app.window) is SettingsHost }
        let settings = try XCTUnwrap(topPresented(in: app.window) as? SettingsHost)
        settings.dismiss(animated: true)
        app.ui.wait(3) { self.topPresented(in: app.window) == nil }
        app.openSettings()
        app.ui.wait(5) { self.topPresented(in: app.window) is SettingsHost }
    }

    /// A refused presentation hands the request back, so the next tap works.
    func testARefusedPresentationLetsTheNextTapTryAgain() throws {
        UserDefaults.standard.set(0, forKey: "depollsoft.pitchperfect.theme")
        let window = ScreenCatalog.makeWindow()
        let models = PitchPerfectModels()
        let root = RefusingRoot(rootView: PitchPerfectRoot(models: models))
        window.rootViewController = root
        window.makeKeyAndVisible()
        defer {
            root.dismiss(animated: false)
            window.isHidden = true
            window.rootViewController = nil
        }
        ScreenCatalog.settle()
        let ui = UIDriver(window)
        ui.tap(id: "gearshape")
        ScreenCatalog.settle(0.3)
        XCTAssertFalse(root.refuseNext, "the first presentation was attempted")
        XCTAssertNil(topPresented(in: window), "UIKit refused it")
        ui.tap(id: "gearshape")
        ui.wait(3) { self.topPresented(in: window) is SettingsHost }
    }

    /// Settings belongs to its window and goes with it.
    func testSettingsIsReleasedWithItsWindow() {
        weak var released: SettingsHost?
        autoreleasepool {
            let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 320, height: 480))
            let host = SettingsHost.shared(for: window)
            XCTAssertTrue(SettingsHost.shared(for: window) === host, "one per window")
            XCTAssertTrue(SettingsHost.existing(for: window) === host)
            released = host
        }
        XCTAssertNil(released, "nothing outlives the window it was made for")
    }
}

@MainActor
final class UIKitReferenceGeometryTests: PitchPerfectTestCase {
    /// The selector's positions, laid out by the port, against the UIKit selector
    /// that shipped before it, at phone and iPad widths and when the names
    /// overflow and the row scrolls.
    func testTheSelectorMatchesTheUIKitSelector() throws {
        let store = DPSongsModel.sharedInstance
        let cases: [(names: [String], widths: [CGFloat])] = [
            (["Saturday show", "Afterglow"], [343, 704]),
            (["Christmas concert rehearsal", "Contest set", "Afterglow", "Sing-out on the green", "Tags"], [343, 704]),
        ]
        for (names, widths) in cases {
            store.songLists = ["default": DPSongList(id: "default")]
            for name in names { try XCTUnwrap(store.createList(named: name)) }
            let titles = store.orderedLists.map(store.displayName(for:))
            for width in widths {
                let reference = UIKitSetListSelectorReference(
                    frame: CGRect(x: 0, y: 0, width: width, height: UIKitSetListSelectorReference.height))
                reference.render(model: store)
                reference.layoutIfNeeded()
                let expected = reference.positionButtons.map(\.frame)
                let frames = SelectorGeometry().frames(titles: titles, width: width)
                // Positions and hairlines alternate; the "+" is last.
                let positions = stride(from: 0, to: frames.count, by: 2).map { frames[$0] }
                XCTAssertEqual(positions.count, expected.count, "\(names) at \(width)")
                for (index, (actual, uikit)) in zip(positions, expected).enumerated() {
                    XCTAssertEqual(actual.minX, uikit.minX, accuracy: 0.5, "\(names) #\(index) at \(width) starts")
                    XCTAssertEqual(actual.width, uikit.width, accuracy: 0.5, "\(names) #\(index) at \(width) width")
                }
            }
        }
    }

    /// Table margins are what a real UIKit table in a navigation screen gets on
    /// this device (16 pt on an iPhone SE, 20 pt on larger phones, 16 pt on iPad).
    func testTableMarginsAreWhatAUIKitTableGetsHere() throws {
        let app = try launch()
        let reference = UIViewController()
        let table = UITableView(frame: .zero, style: .plain)
        table.translatesAutoresizingMaskIntoConstraints = false
        reference.view.addSubview(table)
        NSLayoutConstraint.activate([
            table.leadingAnchor.constraint(equalTo: reference.view.leadingAnchor),
            table.trailingAnchor.constraint(equalTo: reference.view.trailingAnchor),
            table.topAnchor.constraint(equalTo: reference.view.topAnchor),
            table.bottomAnchor.constraint(equalTo: reference.view.bottomAnchor),
        ])
        let navigation = UINavigationController(rootViewController: reference)
        navigation.modalPresentationStyle = .fullScreen
        app.host.present(navigation, animated: false)
        app.ui.wait(3) { table.window != nil && table.bounds.width > 0 }
        table.layoutIfNeeded()
        let screen = TableMargin.Container(width: app.window.bounds.width, traits: app.window.traitCollection,
                                           style: .plain)
        XCTAssertEqual(TableMargin.measure(screen, in: app.window), table.layoutMargins.left)
        navigation.dismiss(animated: false)
    }
}
