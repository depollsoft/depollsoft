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
    /// The storage the pre-set-list app wrote, literally (class names are aliases).
    /// A process that has registered nothing is covered by the UI test
    /// `testSongsSavedByTheOldAppDecodeInAFreshProcess`; this pins the format.
    private static let legacyFixture = #"""
    {"*type":"List","*items":[{"*type":"PitchedSong","Id":"0B8E7C1A-5D2F-4C3B-9E61-7A4D2F8C1B30",
     "Name":"Saved Before Launch","Key":{"*type":"Key","KeyType":{"*name":"Major","*type":"KeyType"},
     "NumAccidentals":{"*type":"Primitive","Type":"Integer","Value":-3},
     "Note":{"*type":"Note","Accidental":{"*name":"Flat","*type":"Accidental"},
     "Frequency":{"*type":"Primitive","Type":"Double","Value":311.12698372208092},"FriendlyName":"E",
     "IsPlaying":{"*type":"Primitive","Type":"c","Value":false},
     "Octave":{"*type":"Primitive","Type":"Integer","Value":4}}}}]}
    """#

    func testTheStoreMigratesTheOldAppsSavedSongs() throws {
        let defaults = UserDefaults.standard
        let keys = [DPSongsModel.legacySongsKey, DPSongsModel.songListsKey]
        let saved = keys.map { defaults.object(forKey: $0) }
        defer {
            for (key, value) in zip(keys, saved) {
                if let value { defaults.set(value, forKey: key) } else { defaults.removeObject(forKey: key) }
            }
        }
        let fixture = try JSONSerialization.jsonObject(with: Data(Self.legacyFixture.utf8))
        defaults.set(fixture, forKey: DPSongsModel.legacySongsKey)
        defaults.removeObject(forKey: DPSongsModel.songListsKey)

        let store = DPSongsModel()
        XCTAssertEqual(store.defaultSongList.songs.map(\.name), ["Saved Before Launch"])
        let key = try XCTUnwrap(store.defaultSongList.songs.first?.key)
        XCTAssertEqual(key.friendlyName(), "E")
        XCTAssertEqual(key.numAccidentals, -3)
        XCTAssertNil(defaults.object(forKey: DPSongsModel.legacySongsKey), "migrated once, then removed")
        XCTAssertNotNil(defaults.dictionary(forKey: DPSongsModel.songListsKey), "written in the set-list format")
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

    func testTheTabBarCarriesUIKitsOwnLabelTintOnEveryDevice() throws {
        let app = try launch()
        app.show(tab: 3)
        app.manageSetLists()
        ScreenCatalog.settle(0.3)
        guard !TabBarChrome.isTopTabBar(horizontal: UserInterfaceSizeClass(app.window.traitCollection.horizontalSizeClass)) else {
            XCTAssertNil(TabBarChrome.color(horizontal: .regular), "iPadOS 18's top tab bar kept the system accent")
            XCTAssertNotNil(TabBarChrome.color(horizontal: .compact), "at compact width it is a bottom bar again")
            return
        }
        let tabBar = try XCTUnwrap(app.descendants(of: UITabBar.self, in: app.window).first)
        func rgba(_ color: UIColor) -> [CGFloat] {
            var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
            color.resolvedColor(with: tabBar.traitCollection).getRed(&r, green: &g, blue: &b, alpha: &a)
            return [r, g, b, a]
        }
        XCTAssertEqual(rgba(tabBar.tintColor), rgba(.label), "a pushed screen keeps the bar's label tint")
        // What the glyphs are drawn in: every item, selected or not, in the label colour.
        let glyphs = app.descendants(of: UIImageView.self, in: tabBar).filter { $0.window != nil && !$0.isHidden }
        XCTAssertFalse(glyphs.isEmpty)
        for glyph in glyphs { XCTAssertEqual(rgba(glyph.tintColor), rgba(.label)) }
    }

    /// Under an alert UIKit greys the selected tab (its tint dims) and leaves the
    /// others at full ink. A SwiftUI tint on the TabView inverted that.
    func testAnAlertDimsOnlyTheTabBarsTint() throws {
        let app = try launch()
        app.show(tab: 3)
        // iPadOS 18's top tab bar carries titles only.
        try XCTSkipIf(TabBarChrome.isTopTabBar(horizontal: UserInterfaceSizeClass(app.window.traitCollection.horizontalSizeClass)),
                      "no tab glyphs in the top tab bar")
        let tabBar = try XCTUnwrap(app.descendants(of: UITabBar.self, in: app.window).first)
        XCTAssertEqual(tabBar.tintAdjustmentMode, .normal)
        let alert = UIAlertController(title: "Test", message: nil, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .cancel))
        app.topPresented.present(alert, animated: false)
        settle { tabBar.tintAdjustmentMode == .dimmed }
        // Every item, selected or not, dims from the label colour to the same grey.
        let tints = Set(app.descendants(of: UIImageView.self, in: tabBar).map {
            $0.tintColor.resolvedColor(with: tabBar.traitCollection).description
        })
        XCTAssertEqual(tints.count, 1, "selected and unselected items dim alike: \(tints)")
        alert.dismiss(animated: false)
        settle { tabBar.tintAdjustmentMode == .normal }
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

    /// Reopening Settings finds it where it was left, even at a large text size:
    /// it is one controller for the app's life, as UIKit's was.
    func testSettingsComesBackWhereItWasLeftAtALargeTextSize() throws {
        let app = try launch()
        // On the scene, so the presented sheet takes it too.
        let scene = try XCTUnwrap(ScreenCatalog.scene)
        scene.traitOverrides.preferredContentSizeCategory = .accessibilityExtraExtraExtraLarge
        defer { scene.traitOverrides.preferredContentSizeCategory = .unspecified }
        func openList() throws -> UIScrollView {
            app.openSettings()
            var list: UIScrollView?
            settle { list = self.settingsList(in: app); return list != nil && list!.contentSize.height > list!.bounds.height }
            // As a user would: a moment after the sheet has come up.
            ScreenCatalog.settle(0.3)
            return try XCTUnwrap(list)
        }
        let first = try openList()
        let bottom = first.contentSize.height - first.bounds.height + first.adjustedContentInset.bottom
        let place = min(bottom, 400)
        XCTAssertGreaterThan(place, 40, "large text makes Settings taller than its sheet")
        first.setContentOffset(CGPoint(x: 0, y: place), animated: false)
        app.sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
        let second = try openList()
        XCTAssertTrue(first === second, "one Settings for the app's life, as UIKit presented it")
        settle { abs(second.contentOffset.y - place) < 0.5 }
        app.sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
    }

    private func settingsList(in app: HostedApp) -> UIScrollView? {
        guard app.topPresented !== app.host else { return nil }
        return app.descendants(of: UICollectionView.self, in: app.topPresented.view).first { $0.window != nil }
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
        let screen = TableMargin.Container(width: app.window.bounds.width, traits: app.window.traitCollection, style: .plain)
        XCTAssertEqual(TableMargin.measure(screen, in: app.window), UIDevice.current.userInterfaceIdiom == .pad ? 16 : 20)
    }

    /// Settings measures its margin as a grouped table in its own sheet, which is
    /// what a real grouped UIKit table in the same sheet gets (not the window's).
    func testTheSettingsSheetMeasuresAGroupedTableInItself() throws {
        let app = try launch()
        // Built as the UIKit Settings screen was: a grouped table filling a plain
        // controller's view (a UITableViewController's own table gets other margins).
        let reference = UIViewController()
        let table = UITableView(frame: .zero, style: .grouped)
        table.translatesAutoresizingMaskIntoConstraints = false
        table.insetsLayoutMarginsFromSafeArea = false
        reference.view.addSubview(table)
        NSLayoutConstraint.activate([
            table.leadingAnchor.constraint(equalTo: reference.view.leadingAnchor),
            table.trailingAnchor.constraint(equalTo: reference.view.trailingAnchor),
            table.topAnchor.constraint(equalTo: reference.view.topAnchor),
            table.bottomAnchor.constraint(equalTo: reference.view.bottomAnchor),
        ])
        let sheet = UINavigationController(rootViewController: reference)
        app.host.present(sheet, animated: false)
        settle { reference.view.window != nil && table.bounds.width > 0 }
        table.layoutIfNeeded()
        let uikitMargin = table.layoutMargins.left
        let container = TableMargin.Container(width: sheet.view.bounds.width, traits: sheet.traitCollection, style: .grouped)
        XCTAssertEqual(TableMargin.measure(container, in: app.window), uikitMargin)
        sheet.dismiss(animated: false)
        settle { app.topPresented === app.host }
    }

    /// The rendered positions sit exactly where UIKit's `.fillProportionally`
    /// stack puts the same pieces in a frame of the selector's width.
    func testTheSelectorPositionsAreWhereUIKitsStackPutThem() throws {
        let store = DPSongsModel.sharedInstance
        let lists = [store.defaultSongList, try customList(named: "Saturday show"), try customList(named: "Afterglow")]
        let app = try launch()
        app.show(tab: 3)
        let positions = try lists.map { try XCTUnwrap(app.ui.element(id: "setlist.\($0.id)")).accessibilityFrame }
        let plus = try XCTUnwrap(app.ui.element(id: "setlist.new")).accessibilityFrame
        let rowStart = positions[0].minX
        let width = plus.maxX - rowStart
        let titles = lists.map(store.displayName(for:))
        let stack = StackGeometry.frames(naturals: titles.map(SetListSelectorMetrics.naturalWidth(for:)), width: width)
        for (index, rendered) in positions.enumerated() {
            XCTAssertEqual(rendered.minX - rowStart, stack[index * 2].minX, accuracy: 0.5, "\(titles[index]) starts where UIKit put it")
            XCTAssertEqual(rendered.width, stack[index * 2].width, accuracy: 0.5, "\(titles[index]) is as wide")
        }
        XCTAssertEqual(plus.width, SetListSelectorMetrics.newPositionWidth, accuracy: 0.5, "the + keeps its fixed width")
    }
}

@MainActor
final class CleanupHelperTests: PitchPerfectTestCase {
    func testOneSelectorMeasuresAgainOnlyWhenItsTitlesOrWidthChange() {
        let geometry = SelectorGeometry()
        let titles = ["My Songs", "Saturday show"]
        let first = geometry.frames(titles: titles, width: 370)
        XCTAssertEqual(geometry.frames(titles: titles, width: 370), first)
        let wider = geometry.frames(titles: titles, width: 700)
        XCTAssertNotEqual(wider, first, "a new width is measured")
        XCTAssertEqual(wider.last?.maxX ?? 0, 700, accuracy: 0.5)
        XCTAssertEqual(geometry.frames(titles: titles + ["Afterglow"], width: 700).count, 7,
                       "three positions, two hairlines between them, a hairline and the +")
    }

    func testPresentOnTopPresentsOverWhatIsShownOnlyIfStillWanted() throws {
        let app = try launch()
        let sheet = UIViewController()
        app.host.present(sheet, animated: false)
        settle { app.topPresented === sheet }
        let wanted = UIViewController()
        app.host.presentOnTop(wanted, stillWanted: { true })
        settle { app.topPresented === wanted }
        XCTAssertTrue(wanted.presentingViewController === sheet, "over the sheet already up")
        let withdrawn = UIViewController()
        app.host.presentOnTop(withdrawn, stillWanted: { false })
        ScreenCatalog.settle(0.2)
        XCTAssertNil(withdrawn.presentingViewController, "a presentation withdrawn in the meantime never happens")
        app.host.dismiss(animated: false)
    }

    /// In edit mode the delete, disclosure and reorder controls sit where a real
    /// UITableView in the same place puts them.
    func testEditModeControlsSitWhereUIKitsTablePutsThem() throws {
        seedSongs(["Blue Skies", "Shenandoah"])
        let app = try launch()
        app.editSongs()
        ScreenCatalog.settle(0.6)
        let list = try XCTUnwrap(app.descendants(of: UICollectionView.self, in: app.window).first { $0.window != nil })
        func frame(_ name: String, in root: UIView, relativeTo base: UIView) -> CGRect? {
            var found: UIView?
            func walk(_ view: UIView) {
                if found == nil, String(describing: type(of: view)) == name { found = view }
                view.subviews.forEach(walk)
            }
            walk(root)
            return found.map { $0.convert($0.bounds, to: base) }
        }
        let cell = try XCTUnwrap(list.visibleCells.min { $0.frame.minY < $1.frame.minY })
        let delete = try XCTUnwrap(frame("_UICollectionViewListAccessoryControl", in: cell, relativeTo: list))
        let reorder = try XCTUnwrap(frame("_UICollectionViewListCellReorderControl", in: cell, relativeTo: list))
        let info = try XCTUnwrap(app.descendants(of: UIButton.self, in: cell).first).convert(
            app.descendants(of: UIButton.self, in: cell).first!.bounds, to: list)

        let reference = EditingTableReference(width: list.bounds.width, in: app.window)
        XCTAssertEqual(delete.minX, reference.delete.minX, accuracy: 0.5, "delete control")
        XCTAssertEqual(reorder.minX, reference.reorder.minX, accuracy: 0.5, "reorder control")
        XCTAssertEqual(info.maxX, reference.info.maxX, accuracy: 0.5, "detail disclosure")
    }
}

/// A real UITableView row in edit mode (delete, detail-disclosure editing
/// accessory, reorder), as the UIKit Songs screen had it, laid out off screen.
@MainActor
private final class EditingTableReference: NSObject, UITableViewDataSource {
    private(set) var delete = CGRect.zero
    private(set) var reorder = CGRect.zero
    private(set) var info = CGRect.zero

    init(width: CGFloat, in window: UIWindow) {
        super.init()
        let screen = UIViewController()
        let navigation = UINavigationController(rootViewController: screen)
        navigation.view.frame = CGRect(x: 0, y: 0, width: width, height: 600)
        navigation.view.isHidden = true
        window.addSubview(navigation.view)
        defer { navigation.view.removeFromSuperview() }
        let table = UITableView(frame: CGRect(x: 0, y: 0, width: width, height: 600), style: .plain)
        table.dataSource = self
        screen.view.addSubview(table)
        table.setEditing(true, animated: false)
        navigation.view.layoutIfNeeded()
        table.layoutIfNeeded()
        guard let cell = table.visibleCells.first else { return }
        cell.layoutIfNeeded()
        func frame(_ name: String) -> CGRect {
            var found: UIView?
            func walk(_ view: UIView) {
                if found == nil, String(describing: type(of: view)) == name { found = view }
                view.subviews.forEach(walk)
            }
            walk(cell)
            return found.map { $0.convert($0.bounds, to: table) } ?? .zero
        }
        delete = frame("UITableViewCellEditControl")
        reorder = frame("UITableViewCellReorderControl")
        info = cell.editingAccessoryView.map { $0.convert($0.bounds, to: table) } ?? .zero
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 1 }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .value1, reuseIdentifier: nil)
        cell.textLabel?.text = "Blue Skies"
        cell.editingAccessoryView = UIButton(type: .detailDisclosure)
        return cell
    }

    func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool { true }
    func tableView(_ tableView: UITableView, moveRowAt source: IndexPath, to destination: IndexPath) {}
}

@MainActor
final class RobustnessTests: PitchPerfectTestCase {
    /// The Major/Minor control stays the Keys bar's left item through what
    /// rebuilds bars: Settings opening and closing, and leaving and coming back.
    func testTheKeysBarKeepsItsSegmentedControl() throws {
        let app = try launch()
        func control() -> UISegmentedControl? {
            app.descendants(of: UINavigationBar.self, in: app.window).first { $0.topItem?.title == "Keys" }?
                .topItem?.leftBarButtonItem?.customView as? UISegmentedControl
        }
        app.show(tab: 2)
        settle { control() != nil }
        for _ in 0..<2 {
            app.openSettings()
            settle { app.topPresented !== app.host }
            app.sheet.tap(id: "checkmark")
            settle { app.topPresented === app.host }
            settle { control() != nil }
            app.show(tab: 3)
            app.show(tab: 2)
            settle { control() != nil }
        }
        let keys = try XCTUnwrap(control())
        keys.selectedSegmentIndex = 1
        keys.sendActions(for: .valueChanged)
        XCTAssertEqual(app.models.keys.mode, .minor, "still wired to the model")
        app.models.keys.mode = .major
    }

    func testTheUITestSongStoreHooksSetTheSongsAsideAndBack() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "UITestSongStoreTests"))
        defaults.removePersistentDomain(forName: "UITestSongStoreTests")
        defaults.set(["mine": ["name": "Mine"]], forKey: DPSongsModel.songListsKey)
        UITestSongStore.prepare(["PP_STASH_SONGS": "1", "PP_LEGACY_SONGS_JSON": #"{"*type":"List","*items":[]}"#],
                                defaults: defaults)
        XCTAssertNil(defaults.object(forKey: DPSongsModel.songListsKey), "set aside for the test")
        XCTAssertNotNil(defaults.object(forKey: DPSongsModel.legacySongsKey), "the legacy fixture seeded")
        UITestSongStore.prepare(["PP_STASH_SONGS": "1"], defaults: defaults)
        defaults.set(["test": [:]], forKey: DPSongsModel.songListsKey)
        UITestSongStore.prepare(["PP_UNSTASH_SONGS": "1"], defaults: defaults)
        XCTAssertEqual((defaults.dictionary(forKey: DPSongsModel.songListsKey)?["mine"] as? [String: String])?["name"], "Mine",
                       "a second stash never overwrote the first; the unstash restored it")
        defaults.removePersistentDomain(forName: "UITestSongStoreTests")
    }

    func testSetListsReorderControlsSitWhereUIKitsTablePutsThem() throws {
        _ = try customList(named: "Saturday show")
        let app = try launch()
        app.show(tab: 3)
        app.manageSetLists()
        ScreenCatalog.settle(0.6)
        var reorder: UIView?
        func walk(_ view: UIView) {
            if reorder == nil, view.window != nil, !view.isHidden,
               String(describing: type(of: view)) == "_UICollectionViewListCellReorderControl" { reorder = view }
            view.subviews.forEach(walk)
        }
        settle { walk(app.window); return reorder != nil }
        let control = try XCTUnwrap(reorder)
        var ancestor = control.superview
        while let current = ancestor, !(current is UICollectionView) { ancestor = current.superview }
        let list = try XCTUnwrap(ancestor)
        let frame = control.convert(control.bounds, to: list)
        XCTAssertEqual(frame.maxX, list.bounds.width - TableMargin.lastMeasured(.plain), accuracy: 0.5,
                       "the reorder control ends the table margin from the edge, as UIKit's did")
    }
}
