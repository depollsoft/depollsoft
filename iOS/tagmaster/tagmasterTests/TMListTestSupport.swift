//
//  TMListTestSupport.swift
//  tagmasterTests
//
//  What the list-screen tests share: a navigator that records what a screen
//  asks for instead of doing it, a catalog that answers from fixtures, and a
//  way to mount a screen in a navigation stack and drive it through its
//  accessibility tree.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

/// Records every request a list screen makes of the app around it.
@MainActor
final class RecordingNavigator: TMNavigator {
    var isExpandedSplit = false
    var currentSplitTagId: Int?
    private(set) var shownTags: [Int] = []
    private(set) var destinations: [TMDestination] = []
    private(set) var removals = 0
    private(set) var openedURLs: [URL] = []
    private(set) var privacyPresentations = 0

    func showTag(_ tagId: Int) {
        shownTags.append(tagId)
        if isExpandedSplit { currentSplitTagId = tagId }
    }

    func show(_ destination: TMDestination) { destinations.append(destination) }
    func removeScreen() { removals += 1 }
    func openURL(_ url: URL) { openedURLs.append(url) }
    func presentPrivacyChoices() { privacyPresentations += 1 }
}

/// Catalog answers built from titles, recording every query asked.
final class TMFixtureCatalog: @unchecked Sendable {
    private let lock = NSLock()
    private var asked: [(query: TMTagQuery, count: Int, start: Int)] = []
    /// How many tags match; nil makes every query fail as if offline.
    var available: Int?
    /// Holds each answer until released, to catch a screen mid-load.
    var gate: DispatchSemaphore?

    init(available: Int? = 45) { self.available = available }

    var queries: [(query: TMTagQuery, count: Int, start: Int)] {
        lock.lock(); defer { lock.unlock() }
        return asked
    }

    var catalog: TMCatalog {
        TMCatalog { [self] query, count, start in
            lock.lock()
            asked.append((query, count, start))
            let available = self.available
            lock.unlock()
            gate?.wait()
            guard let available else { return nil }
            let result = DPTagQueryResult()
            result.available = Int32(available)
            result.start = Int32(start)
            let end = min(available, start + count)
            result.tags = (start..<max(start, end)).map { index in
                let tag = DPTag()
                tag.tagId = Int32(3000 + index)
                tag.title = "Fixture \(index)"
                tag.posted = Date(timeIntervalSince1970: 1_600_000_000)
                return tag
            }
            result.count = Int32(result.tags.count)
            return result
        }
    }
}

@MainActor
extension TMBehaviorTestCase {
    /// Mounts `screen` above a plain root in a navigation stack and returns a driver.
    @discardableResult
    func mountScreen(_ screen: UIViewController, size: CGSize = TMBehaviorTestCase.portrait) -> UIDriver {
        TMTagStore.shared.reset()
        let navigation = UINavigationController(rootViewController: UIViewController())
        navigation.pushViewController(screen, animated: false)
        mount(navigation, size: size)
        ScreenCatalog.settle(0.2)
        return UIDriver(window)
    }

    /// Spins until SwiftUI has presented an alert over the window, and returns it.
    func presentedAlert(file: StaticString = #filePath, line: UInt = #line) -> UIAlertController? {
        var alert: UIAlertController?
        spinUntil("an alert is presented", file: file, line: line) {
            var top = self.window.rootViewController
            while let next = top?.presentedViewController { top = next }
            alert = top as? UIAlertController
            return alert != nil
        }
        return alert
    }

    /// Presses a bar button the way a tap does.
    func press(_ item: UIBarButtonItem?, file: StaticString = #filePath, line: UInt = #line) {
        guard let item, let action = item.action else {
            XCTFail("No bar button to press", file: file, line: line)
            return
        }
        XCTAssertTrue(UIApplication.shared.sendAction(action, to: item.target, from: item, for: nil), file: file, line: line)
        ScreenCatalog.settle(0.05)
    }

    /// Taps a tab of the bar inside `view` by its title, through the tab's own control.
    func tapTab(_ title: String, in view: UIView, file: StaticString = #filePath, line: UInt = #line) {
        guard let bar = firstDescendant(of: view, where: { $0 is UITabBar }) as? UITabBar else {
            XCTFail("No tab bar", file: file, line: line)
            return
        }
        let controls = descendants(of: bar) { $0 is UIControl && !$0.isHidden && $0.accessibilityLabel == title }
        guard let control = controls.first as? UIControl else {
            XCTFail("No tab \(title); tabs: \(descendants(of: bar) { $0 is UIControl }.map { $0.accessibilityLabel ?? "?" })",
                    file: file, line: line)
            return
        }
        control.sendActions(for: .touchUpInside)
        control.sendActions(for: .primaryActionTriggered)
        ScreenCatalog.settle(0.2)
    }

    /// Dismisses anything presented over the window.
    func dismissPresented() {
        window.rootViewController?.dismiss(animated: false)
        ScreenCatalog.settle(0.1)
    }
}
