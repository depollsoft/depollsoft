//
//  PitchPerfectListScrollingTests.swift
//  pitchperfectTests
//
//  Settings, the Sound list and the Tuning menu reach their last entry in
//  portrait and landscape, at the default and the largest accessibility text
//  size. Run on the smallest supported iPhone (iPhone SE) too: that is where
//  they are tightest.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectListScrollingTests: PitchPerfectTestCase {
    /// A window size and text size. Rotation is UIKit's job and the hosted app
    /// can't be rotated, so, as Tag Master's tests do, landscape is the window
    /// resized to a landscape phone's size; nil keeps the simulator's screen.
    private struct Layout: CustomStringConvertible {
        let size: CGSize?
        let largestText: Bool
        var description: String {
            let where_ = size.map { "\(Int($0.width))×\(Int($0.height))" } ?? "full screen"
            return where_ + (largestText ? ", largest text" : "")
        }
    }

    /// iPhone SE (3rd generation), the smallest supported iPhone.
    private static let sePortrait = CGSize(width: 375, height: 667)
    private static let seLandscape = CGSize(width: 667, height: 375)

    // One test per layout: each launch and resize takes seconds on a loaded CI
    // runner, and five layouts in one test outran its 30 s budget.
    private static let fullScreen = Layout(size: nil, largestText: false)
    private static let small = Layout(size: sePortrait, largestText: false)
    private static let smallLandscape = Layout(size: seLandscape, largestText: false)
    private static let smallLargestText = Layout(size: sePortrait, largestText: true)
    private static let smallLandscapeLargestText = Layout(size: seLandscape, largestText: true)

    override func tearDown() {
        ScreenCatalog.scene?.traitOverrides.preferredContentSizeCategory = .unspecified
        super.tearDown()
    }

    /// Launches the app with Settings open, laid out as `layout`. Settings is
    /// opened first and the window resized after, as a phone turned with
    /// Settings up: the pitch pipe's landscape layout leaves no room for its
    /// Settings button.
    private func launchSettings(_ layout: Layout) throws -> HostedApp {
        let scene = try XCTUnwrap(ScreenCatalog.scene)
        // On the scene, so the presented sheet takes it too.
        scene.traitOverrides.preferredContentSizeCategory =
            layout.largestText ? .accessibilityExtraExtraExtraLarge : .unspecified
        let app = try launch()
        app.show(tab: 0)
        app.openSettings()
        settle { app.navigationTitles.contains("Settings") && self.visibleList(app) != nil }
        if let size = layout.size {
            app.window.frame = CGRect(origin: .zero, size: size)
            ScreenCatalog.settle(0.5)
        }
        return app
    }

    /// The scrolling list on screen in the presented sheet: the last one in
    /// the hierarchy that is in a window (a list pushed off screen isn't).
    private func visibleList(_ app: HostedApp) -> UIScrollView? {
        app.descendants(of: UICollectionView.self, in: app.topPresented.view).last { $0.window != nil }
    }

    /// Scrolls to the end, repeating until estimated row heights stop moving it.
    private func scrollToBottom(_ list: UIScrollView) {
        for _ in 0..<8 {
            list.layoutIfNeeded()
            let bottom = max(-list.adjustedContentInset.top,
                             list.contentSize.height - list.bounds.height + list.adjustedContentInset.bottom)
            list.setContentOffset(CGPoint(x: list.contentOffset.x, y: bottom), animated: false)
            RunLoop.main.run(until: Date().addingTimeInterval(0.05))
        }
    }

    /// Where `list` shows its rows, in screen coordinates (clear of bars).
    private func visibleRect(of list: UIScrollView) -> CGRect {
        let inside = list.bounds.inset(by: list.adjustedContentInset)
        return list.convert(inside, to: list.window?.screen.coordinateSpace ?? list)
    }

    private func assertFullyVisible(_ element: NSObject?, in list: UIScrollView, _ what: String,
                                    file: StaticString = #filePath, line: UInt = #line) {
        guard let element else {
            XCTFail("\(what) isn't on screen", file: file, line: line)
            return
        }
        let frame = element.accessibilityFrame
        // Two points of slack: a row may tuck a hairline under a bar's edge.
        let visible = visibleRect(of: list).insetBy(dx: -2, dy: -2)
        XCTAssertTrue(visible.contains(frame), "\(what) at \(frame) isn't inside the visible \(visible)",
                      file: file, line: line)
    }

    /// Scrolls `list` down from the top until the row `id` is fully in view,
    /// as a person would to reach it (lists only make rows near the screen).
    private func reveal(id: String, in list: UIScrollView, _ app: HostedApp) {
        list.setContentOffset(CGPoint(x: 0, y: -list.adjustedContentInset.top), animated: false)
        var seen: [String] = []
        for _ in 0..<40 {
            list.layoutIfNeeded()
            RunLoop.main.run(until: Date().addingTimeInterval(0.03))
            let frame = app.sheet.element(id: id)?.accessibilityFrame
            seen.append("\(Int(list.contentOffset.y)):\(frame.map { "x\(Int($0.minX))w\(Int($0.width))y\(Int($0.minY))h\(Int($0.height))" } ?? "-")")
            if let frame, isInView(frame, list) {
                return
            }
            let step = max(20, (list.bounds.height - list.adjustedContentInset.top - list.adjustedContentInset.bottom) / 3)
            let bottom = list.contentSize.height - list.bounds.height + list.adjustedContentInset.bottom
            list.setContentOffset(CGPoint(x: 0, y: min(list.contentOffset.y + step, bottom)), animated: false)
        }
        XCTFail("\(id) never came fully into view: last at \(String(describing: app.sheet.element(id: id)?.accessibilityFrame)), visible \(visibleRect(of: list)), offset \(list.contentOffset.y) of \(list.contentSize.height); seen \(seen.joined(separator: " "))")
    }

    /// Whether `frame` is fully inside the list's visible area; a row taller
    /// than the area (the largest text in landscape) counts once it fills it.
    private func isInView(_ frame: CGRect, _ list: UIScrollView) -> Bool {
        let visible = visibleRect(of: list).insetBy(dx: -2, dy: -2)
        guard frame.minX >= visible.minX, frame.maxX <= visible.maxX else { return false }
        if frame.height > visible.height {
            return frame.minY <= visible.minY && frame.maxY >= visible.maxY
        }
        return frame.minY >= visible.minY && frame.maxY <= visible.maxY
    }

    /// Opens Settings as `layout` and scrolls to the row `id`.
    private func settingsRevealing(_ id: String, _ layout: Layout) throws -> HostedApp {
        let app = try launchSettings(layout)
        var list: UIScrollView?
        settle { list = self.visibleList(app); return list != nil }
        reveal(id: id, in: try XCTUnwrap(list, "\(layout)"), app)
        return app
    }

    func testSettingsScrollsToItsLastRow() throws { try settingsScrollsToItsLastRow(Self.fullScreen) }
    func testSettingsScrollsToItsLastRowOnASmallPhone() throws { try settingsScrollsToItsLastRow(Self.small) }
    func testSettingsScrollsToItsLastRowInLandscape() throws { try settingsScrollsToItsLastRow(Self.smallLandscape) }
    func testSettingsScrollsToItsLastRowAtTheLargestText() throws { try settingsScrollsToItsLastRow(Self.smallLargestText) }
    func testSettingsScrollsToItsLastRowInLandscapeAtTheLargestText() throws { try settingsScrollsToItsLastRow(Self.smallLandscapeLargestText) }

    private func settingsScrollsToItsLastRow(_ layout: Layout) throws {
        do {
            let app = try launchSettings(layout)
            var list: UIScrollView?
            settle { list = self.visibleList(app); return list != nil }
            let settings = try XCTUnwrap(list, "\(layout)")
            scrollToBottom(settings)
            assertFullyVisible(app.sheet.element(label: "Privacy choices"), in: settings,
                               "Privacy choices, \(layout)")
            app.resetSettings()
            app.tearDown()
        }
    }

    func testTheSoundListScrollsToItsLastSoundAndChoosesIt() throws { try soundListScrollsToItsLastSound(Self.fullScreen) }
    func testTheSoundListScrollsToItsLastSoundOnASmallPhone() throws { try soundListScrollsToItsLastSound(Self.small) }
    func testTheSoundListScrollsToItsLastSoundInLandscape() throws { try soundListScrollsToItsLastSound(Self.smallLandscape) }
    func testTheSoundListScrollsToItsLastSoundAtTheLargestText() throws { try soundListScrollsToItsLastSound(Self.smallLargestText) }
    func testTheSoundListScrollsToItsLastSoundInLandscapeAtTheLargestText() throws { try soundListScrollsToItsLastSound(Self.smallLandscapeLargestText) }

    private func soundListScrollsToItsLastSound(_ layout: Layout) throws {
        let last = try XCTUnwrap(DPSettingsModel.noteSoundSections.last?.sounds.last)
        do {
            DPSettingsModel.sharedInstance.noteSound = DPNoteSoundPitchPipe
            let app = try settingsRevealing("settings.sound", layout)
            app.sheet.tap(id: "settings.sound")
            var list: UIScrollView?
            settle { list = self.visibleList(app); return app.navigationTitles.contains("Sound") && app.sheet.exists(id: "sound.pitchPipe") }
            let sounds = try XCTUnwrap(list, "\(layout)")
            scrollToBottom(sounds)
            settle { app.sheet.exists(id: "sound.\(last)") }
            assertFullyVisible(app.sheet.element(id: "sound.\(last)"), in: sounds, "\(last), \(layout)")
            app.sheet.tap(id: "sound.\(last)")
            settle { app.sheet.isSelected(id: "sound.\(last)") }
            XCTAssertEqual(DPSettingsModel.sharedInstance.noteSound, last, "\(layout)")
            app.resetSettings()
            app.tearDown()
        }
        DPSettingsModel.sharedInstance.noteSound = DPNoteSoundPitchPipe
    }

    /// The Sound list opens on the current choice, even the last one.
    func testTheSoundListOpensOnTheCurrentChoice() throws {
        defer { DPSettingsModel.sharedInstance.noteSound = DPNoteSoundPitchPipe }
        for sound in ["harp", "choir"] {
            for layout in [Self.fullScreen, Self.small] {
                DPSettingsModel.sharedInstance.noteSound = sound
                let app = try settingsRevealing("settings.sound", layout)
                app.sheet.tap(id: "settings.sound")
                var list: UIScrollView?
                settle { list = self.visibleList(app); return app.navigationTitles.contains("Sound") && app.sheet.exists(id: "sound.\(sound)") }
                let sounds = try XCTUnwrap(list, "\(sound), \(layout)")
                assertFullyVisible(app.sheet.element(id: "sound.\(sound)"), in: sounds, "\(sound), \(layout)")
                XCTAssertTrue(app.sheet.isSelected(id: "sound.\(sound)"), "\(sound), \(layout)")
                app.resetSettings()
                app.tearDown()
            }
        }
    }

    /// The Tuning picker is a system menu; it must scroll to its last choice too.
    func testTheTuningMenuScrollsToItsLastChoice() throws { try tuningMenuScrollsToItsLastChoice(Self.fullScreen) }
    func testTheTuningMenuScrollsToItsLastChoiceOnASmallPhone() throws { try tuningMenuScrollsToItsLastChoice(Self.small) }
    func testTheTuningMenuScrollsToItsLastChoiceInLandscape() throws { try tuningMenuScrollsToItsLastChoice(Self.smallLandscape) }
    func testTheTuningMenuScrollsToItsLastChoiceAtTheLargestText() throws { try tuningMenuScrollsToItsLastChoice(Self.smallLargestText) }
    func testTheTuningMenuScrollsToItsLastChoiceInLandscapeAtTheLargestText() throws { try tuningMenuScrollsToItsLastChoice(Self.smallLandscapeLargestText) }

    private func tuningMenuScrollsToItsLastChoice(_ layout: Layout) throws {
        let choices = DPSettingsModel.referencePitchChoices(current: 440)
        let firstLabel = SettingsModel.tuningLabel(try XCTUnwrap(choices.first))
        let lastLabel = SettingsModel.tuningLabel(try XCTUnwrap(choices.last))
        do {
            let app = try settingsRevealing("settings.tuning", layout)
            app.sheet.tap(id: "settings.tuning")
            var menuList: UIScrollView?
            // Found by its first choice, which is on screen when it opens.
            settle { menuList = self.menuList(containing: firstLabel); return menuList != nil }
            let menu = try XCTUnwrap(menuList, "the tuning menu, \(layout)")
            scrollToBottom(menu)
            let item = menuItem(labelled: lastLabel, in: menu)
            assertFullyVisible(item, in: menu, "\(lastLabel), \(layout)")
            dismissMenu(menu)
            app.resetSettings()
            app.tearDown()
        }
    }

    /// The system menu's list, found through every window (menus present in their own).
    private func menuList(containing label: String) -> UIScrollView? {
        let windows = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
        for window in windows {
            let lists = allViews(in: window).compactMap { $0 as? UICollectionView }
            if let list = lists.first(where: { menuItem(labelled: label, in: $0) != nil }) {
                return list
            }
        }
        return nil
    }

    private func menuItem(labelled label: String, in list: UIScrollView) -> NSObject? {
        allViews(in: list).first { view in
            view.window != nil && (view.accessibilityLabel == label || (view as? UILabel)?.text == label)
        }
    }

    private func allViews(in root: UIView) -> [UIView] {
        [root] + root.subviews.flatMap(allViews)
    }

    private func dismissMenu(_ menu: UIScrollView) {
        // Escape, as a keyboard or VoiceOver's two-finger scrub would.
        var view: UIView? = menu
        while let current = view {
            if current.accessibilityPerformEscape() { break }
            view = current.superview
        }
        ScreenCatalog.settle(0.5)
    }
}
