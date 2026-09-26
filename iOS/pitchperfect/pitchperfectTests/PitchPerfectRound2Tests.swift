//
//  PitchPerfectRound2Tests.swift
//  pitchperfectTests
//
//  What a second review of the SwiftUI port found the UIKit app did that the
//  port did not: stores that decode before launch finishes, bars and sheets
//  built from the UIKit pieces themselves, table margins and several rows'
//  small behaviours.
//

import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class SongStoreLaunchOrderTests: XCTestCase {
    /// The SwiftUI app builds its song store while creating the App, before
    /// didFinishLaunching. The store must decode saved songs on its own.
    func testAFreshStoreDecodesSavedLegacySongsBeforeLaunchRegistersAnything() throws {
        let defaults = UserDefaults.standard
        let legacyKey = "depollsoft.pitchperfect.Songs"
        let listsKey = "depollsoft.pitchperfect.SongLists"
        let savedLists = defaults.object(forKey: listsKey)
        defer {
            defaults.removeObject(forKey: legacyKey)
            if let savedLists { defaults.set(savedLists, forKey: listsKey) } else { defaults.removeObject(forKey: listsKey) }
        }
        let song = DPPitchedSong()
        song.name = "Saved Before Launch"
        song.key = (DPKey.majorKeys() as! [DPKey])[3]
        SongSerialization.registerAliases()
        let serialized = try XCTUnwrap(DPJsonSerializer.serialize(NSMutableArray(array: [song])) as? [String: Any])
        XCTAssertEqual(serialized["*type"] as? String, "List", "saved under the aliases the store must know")
        defaults.set(serialized, forKey: legacyKey)
        defaults.removeObject(forKey: listsKey)

        let store = DPSongsModel()
        XCTAssertEqual(store.defaultSongList.songs.map(\.name), ["Saved Before Launch"])
        XCTAssertEqual(store.defaultSongList.songs.first?.key?.isEqual(song.key), true)
    }

    func testTheTestHostNoLongerRegistersForTheStore() throws {
        // The unit-test host mirrors the app: the store alone is responsible.
        let source = try String(contentsOfFile: #filePath.replacingOccurrences(
            of: "pitchperfectTests/PitchPerfectRound2Tests.swift", with: "pitchperfect/DPAppDelegate.swift"), encoding: .utf8)
        XCTAssertFalse(source.contains("registerSerializationAliases"))
        let store = try String(contentsOfFile: #filePath.replacingOccurrences(
            of: "pitchperfectTests/PitchPerfectRound2Tests.swift", with: "pitchperfect/DPSongsModel.swift"), encoding: .utf8)
        let initBody = try XCTUnwrap(store.range(of: "public override init() {").map { store[$0.upperBound...].prefix(400) })
        XCTAssertTrue(initBody.contains("SongSerialization.registerAliases()"))
    }
}

@MainActor
final class UIKitChromeTests: PitchPerfectTestCase {
    func testKeysCarriesARealSegmentedBarItemThatSwitchesMode() throws {
        let app = try launch()
        app.show(tab: 2)
        let navigation = try XCTUnwrap(app.descendants(of: UINavigationBar.self, in: app.window)
            .first { $0.topItem?.title == "Keys" })
        let control = try XCTUnwrap(navigation.topItem?.leftBarButtonItem?.customView as? UISegmentedControl)
        XCTAssertEqual(control.numberOfSegments, 2)
        XCTAssertEqual(control.titleForSegment(at: 1), "Minor")
        XCTAssertEqual(control.selectedSegmentIndex, 0)
        control.selectedSegmentIndex = 1
        control.sendActions(for: .valueChanged)
        XCTAssertEqual(app.models.keys.mode, .minor)
        app.models.keys.mode = .major
        ScreenCatalog.settle(0.4)
        XCTAssertEqual(control.selectedSegmentIndex, 0, "the control follows the model")
    }

    func testTheTabBarIsTintedWithTheLabelColourWhereUIKitDrewIt() throws {
        let app = try launch()
        app.show(tab: 3)
        app.manageSetLists()
        ScreenCatalog.settle(0.3)
        let tabBar = try XCTUnwrap(app.descendants(of: UITabBar.self, in: app.window).first)
        if TabTint.usesTopTabBar {
            XCTAssertNil(TabTint.color)
        } else {
            func rgba(_ color: UIColor) -> [CGFloat] {
                var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
                color.resolvedColor(with: tabBar.traitCollection).getRed(&r, green: &g, blue: &b, alpha: &a)
                return [r, g, b, a]
            }
            XCTAssertEqual(rgba(tabBar.tintColor), rgba(.label), "a pushed screen keeps the bar's label tint")
        }
    }

    func testTheEditorKeepsItsBannerPinnedBelowTheFormAsUIKitLaidItOut() throws {
        let song = DPPitchedSong()
        song.name = "Blue Skies"
        let controller = SongEditorController(request: SongEditorRequest(song: song, isNew: false) { _ in })
        controller.loadViewIfNeeded()
        let banners = controller.view.subviews.compactMap { $0 as? BannerHostView }
        if UIDevice.current.userInterfaceIdiom == .phone {
            let banner = try XCTUnwrap(banners.first)
            let pinned = controller.view.constraints.contains {
                ($0.firstItem === banner && $0.firstAttribute == .bottom && $0.secondItem === controller.view.safeAreaLayoutGuide)
            }
            XCTAssertTrue(pinned, "the keyboard covers the banner rather than lifting it")
        } else {
            XCTAssertTrue(banners.isEmpty)
        }
        XCTAssertEqual(controller.children.count, 1, "the form is a hosted child that makes room for the keyboard itself")
        XCTAssertEqual(controller.navigationItem.title, "Edit Song")
    }

    func testAddSongsIsBuiltFromUIKitBarItems() throws {
        let lists = try seedTwoLists()
        DPSongsModel.sharedInstance.currentListId = lists.target.id
        let app = try launch()
        app.show(tab: 3)
        app.songs.addSongsFromAnotherList()
        ScreenCatalog.settle(0.5)
        let navigation = try XCTUnwrap(app.topPresented as? UINavigationController)
        let picker = try XCTUnwrap(navigation.topViewController as? AddSongsController)
        XCTAssertEqual(picker.navigationItem.leftBarButtonItems?.count, 2, "Close and Select all, separately")
        XCTAssertEqual(picker.navigationItem.rightBarButtonItem?.style, .done)
        XCTAssertEqual(picker.navigationItem.rightBarButtonItem?.title, "Add")
        XCTAssertFalse(picker.navigationItem.rightBarButtonItem?.isEnabled ?? true)
        picker.model.toggleSelectAll()
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(picker.navigationItem.rightBarButtonItem?.title, "Add 2 songs")
        XCTAssertEqual(picker.navigationItem.leftBarButtonItems?.last?.title, "Clear")
        XCTAssertTrue(app.sheet.exists(label: "Saturday show"), "the source list's name keeps its own case")
    }

    private func seedTwoLists() throws -> (source: DPSongList, target: DPSongList) {
        let store = DPSongsModel.sharedInstance
        let source = try XCTUnwrap(store.createList(named: "Saturday show"))
        for name in ["Blue Skies", "Shenandoah"] {
            let song = DPPitchedSong()
            song.name = name
            song.key = (DPKey.majorKeys() as! [DPKey])[6]
            source.addSong(song)
        }
        let target = try XCTUnwrap(store.createList(named: "Afterglow"))
        return (source, target)
    }
}

@MainActor
final class RowAndSettingsDetailTests: PitchPerfectTestCase {
    func testTwoHeldSongsAreBothLit() throws {
        let app = try launch()
        app.show(tab: 3)
        let songs = seedSongs(["Blue Skies", "Shenandoah"], keys: [(DPKey.majorKeys() as! [DPKey])[6], (DPKey.majorKeys() as! [DPKey])[8]])
        let model = app.songs
        model.press(songs[0])
        model.press(songs[1])
        XCTAssertTrue(model.isLit(songs[0]), "each held row keeps its own light")
        XCTAssertTrue(model.isLit(songs[1]))
        model.release(songs[1])
        XCTAssertTrue(model.isLit(songs[0]), "releasing one leaves the other lit")
        XCTAssertFalse(model.isLit(songs[1]))
        model.release(songs[0])
    }

    func testSettingsComesBackWhereItWasLeft() {
        SettingsScrollMemory.forget()
        let first = UIScrollView(frame: CGRect(x: 0, y: 0, width: 320, height: 200))
        first.contentSize = CGSize(width: 320, height: 800)
        SettingsScrollMemory.attach(first)
        first.contentOffset = CGPoint(x: 0, y: 240)
        SettingsScrollMemory.save()
        let second = UIScrollView(frame: first.frame)
        second.contentSize = first.contentSize
        SettingsScrollMemory.attach(second)
        ScreenCatalog.settle(0.05)
        XCTAssertEqual(second.contentOffset.y, 240, "a new presentation restores the old place")
        SettingsScrollMemory.forget()
    }

    func testTheLoginExplanationIsTheSelectableUIKitText() throws {
        let app = try launch()
        app.showLogin()
        ScreenCatalog.settle(0.5)
        let text = try XCTUnwrap(app.descendants(of: UITextView.self, in: app.topPresented.view).first)
        XCTAssertFalse(text.isEditable)
        XCTAssertTrue(text.isSelectable, "its text can be selected and copied, as the UITextView's could")
        XCTAssertTrue(text.text.hasPrefix("Recommended:"))
    }

    func testTableMarginsFollowUIKitsForTheDevice() throws {
        let app = try launch()
        let margin = TableMargin.measure(in: app.window)
        XCTAssertEqual(margin, UIDevice.current.userInterfaceIdiom == .pad ? 16 : 20)
    }

    func testTheSelectorPositionsAreWhereUIKitsStackPutThem() {
        let titles = ["My Songs", "Saturday show", "Afterglow"]
        let width: CGFloat = 802
        let spans = SetListSelectorMetrics.positionSpans(titles: titles, width: width)
        XCTAssertEqual(spans.count, 3)
        XCTAssertEqual(spans[0].lowerBound, 0)
        let frames = StackGeometry.frames(naturals: titles.map(SetListSelectorMetrics.naturalWidth(for:)), width: width)
        XCTAssertEqual(frames.last?.width, SetListSelectorMetrics.newPositionWidth, "the + keeps its fixed width")
        XCTAssertEqual(frames.last?.maxX ?? 0, width, accuracy: 0.5, "the row fills the frame")
        XCTAssertEqual(frames[1].width, 1, "hairlines stay one point wide")
    }
}
