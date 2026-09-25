//
//  PitchPerfectScreenCatalogTests.swift
//  pitchperfectTests
//
//  Puts every Pitch Perfect screen into each state worth seeing and captures
//  it (see docs/ios-swiftui.md). With SCREEN_CATALOG_DIR unset this is a
//  smoke test that every state can be reached and rendered.
//

import FirebaseCore
import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// Drives the app into catalog states: through its controls where a finger
/// would, through its models where a gesture cannot be synthesized.
@MainActor
final class CatalogApp {
    let window: UIWindow
    let models: PitchPerfectModels
    let host: UIViewController

    init(style: UIUserInterfaceStyle) throws {
        window = ScreenCatalog.makeWindow(style: style)
        models = PitchPerfectModels()
        host = UIHostingController(rootView: PitchPerfectRoot(models: models))
        window.rootViewController = host
        window.makeKeyAndVisible()
        ScreenCatalog.settle()
    }

    func tearDown() {
        host.dismiss(animated: false)
        window.isHidden = true
        window.rootViewController = nil
    }

    var ui: UIDriver { UIDriver(window) }

    var topPresented: UIViewController {
        var top = host
        while let next = top.presentedViewController { top = next }
        return top
    }

    /// The frontmost sheet's own controls.
    var sheet: UIDriver { UIDriver(topPresented.view) }

    func show(tab index: Int) {
        models.tab = [.pitchPipe, .notes, .keys, .songs][index]
        ScreenCatalog.settle()
    }

    var songs: SongListModel { models.songs }

    // MARK: Actions

    /// Toggle mode: each activation starts a note that keeps sounding.
    func pressInstrument(_ labels: [String], toggle: Bool) {
        show(tab: 0)
        for label in labels { ui.tap(label: label) }
    }

    func selectRange(high: Bool) {
        show(tab: 0)
        ui.tap(label: high ? "Octave range F to F" : "Octave range C to C")
    }

    func showMinorKeys() {
        show(tab: 2)
        let control = descendants(of: UISegmentedControl.self, in: window).first!
        control.selectedSegmentIndex = 1
        control.sendActions(for: .valueChanged)
    }

    func pressSong(row: Int) {
        show(tab: 3)
        songs.press(DPSongsModel.sharedInstance.currentList.songs[row])
    }

    func editSongs() {
        show(tab: 3)
        ui.tap(id: "pencil")
    }

    func openSettings() { ui.tap(id: "gearshape") }

    func addSong() { ui.tap(id: "plus") }

    func editSong(row: Int) {
        songs.editSong(DPSongsModel.sharedInstance.currentList.songs[row])
    }

    func typeSongTitle(_ title: String) {
        let field = descendants(of: UITextField.self, in: topPresented.view).first!
        field.text = title
        field.sendActions(for: .editingChanged)
    }

    func pressEditorDone() { sheet.tap(id: "checkmark") }

    func manageSetLists() { songs.manageSetLists() }

    func addSongsFromAnotherList(choose rows: [IndexPath]) {
        songs.addSongsFromAnotherList()
        ScreenCatalog.settle()
        let groups = DPSongsModel.sharedInstance.addableSongs(for: DPSongsModel.sharedInstance.currentList)
        for row in rows {
            let song = groups[row.section].songs[row.row]
            sheet.tap(label: "\(song.name ?? ""), \(song.key?.friendlyName() ?? "")")
        }
    }

    func promptNewSetList(typing text: String?) {
        show(tab: 3)
        ui.tap(id: "setlist.new")
        ScreenCatalog.settle()
        if let text, let alert = topPresented as? UIAlertController {
            alert.pp_type(text)
        }
    }

    func confirmDelete(_ list: DPSongList) { songs.confirmDelete(list) }

    func showLogin() {
        host.present(UIHostingController(rootView: NavigationStack { LoginIntroScreen() }), animated: false)
    }

    func showPrivacyChoices() { TelemetryConsent.present(from: host) }

    func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }
}

@MainActor
final class PitchPerfectScreenCatalogTests: XCTestCase {
    private var savedDefaults: [String: Any] = [:]
    private var savedLists: [String: DPSongList] = [:]
    private var app: CatalogApp?

    override func setUp() async throws {
        await MainActor.run {
            let defaults = UserDefaults.standard
            savedDefaults = defaults.dictionaryRepresentation().filter { $0.key.hasPrefix("depollsoft.pitchperfect.") }
            savedLists = DPSongsModel.sharedInstance.songLists
            setenv("STORE_SCREENSHOTS", "1", 1)
            if FirebaseApp.app() == nil { FirebaseApp.configure() }
            UIView.setAnimationsEnabled(false)
            DPSettingsModel.sharedInstance.detachFromFirestore()
            DPSettingsModel.sharedInstance.toggleNotes = false
            DPSettingsModel.sharedInstance.wakeLock = false
            DPPitchPipeModel().isFromFToF = false
            stopAll()
            DPSongsModel.sharedInstance.songLists = ["default": DPSongList(id: "default")]
            DPSongsModel.sharedInstance.currentListId = "default"
        }
    }

    override func tearDown() async throws {
        await MainActor.run {
            app?.tearDown()
            app = nil
            stopAll()
            let defaults = UserDefaults.standard
            for key in defaults.dictionaryRepresentation().keys where key.hasPrefix("depollsoft.pitchperfect.") {
                defaults.removeObject(forKey: key)
            }
            for (key, value) in savedDefaults { defaults.set(value, forKey: key) }
            DPSongsModel.sharedInstance.songLists = savedLists
            UIView.setAnimationsEnabled(true)
            unsetenv("STORE_SCREENSHOTS")
        }
    }

    private func stopAll() {
        NotePlayer.shared.stop(DPNote.commonNotes() as! [DPNote])
    }

    private func launch(_ style: UIUserInterfaceStyle = .light) throws -> CatalogApp {
        // The app applies the stored theme to its windows; store the one wanted.
        UserDefaults.standard.set(style == .dark ? 2 : 0, forKey: "depollsoft.pitchperfect.theme")
        let app = try CatalogApp(style: style)
        self.app = app
        return app
    }

    private func capture(_ name: String, _ app: CatalogApp, settle: TimeInterval = 0.5) {
        ScreenCatalog.capture(name, window: app.window, settle: settle)
    }

    private static let songTitles = ["Blue Skies", "Down Our Way", "Heart of My Heart", "Shenandoah",
                                     "Sweet Adeline", "The Old Songs", "When You Were Sweet Sixteen",
                                     "You Are My Sunshine"]

    /// Eight songs in several keys, plus two custom set lists.
    @discardableResult
    private func seedSongs(lists: Bool = true) -> [DPSongList] {
        let majors = DPKey.majorKeys() as! [DPKey]
        let minors = DPKey.minorKeys() as! [DPKey]
        let keys = [majors[6], majors[1], majors[8], minors[4], majors[11], majors[3], minors[9], majors[6]]
        DPSongsModel.sharedInstance.defaultSongList.songs = zip(Self.songTitles, keys).map { name, key in
            let song = DPPitchedSong()
            song.name = name
            song.key = key
            return song
        }
        guard lists else { return [] }
        let model = DPSongsModel.sharedInstance
        let saturday = model.createList(named: "Saturday show")!
        model.copySongs(Array(model.defaultSongList.songs.prefix(3)), to: saturday)
        let afterglow = model.createList(named: "Afterglow")!
        return [saturday, afterglow]
    }

    // MARK: - Pitch pipe

    func testPitchPipe() throws {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.show(tab: 0)
            capture("pipe-c-to-c\(style == .dark ? "-dark" : "")", app)
            app.tearDown()
        }
        let app = try launch()
        app.selectRange(high: true)
        capture("pipe-f-to-f", app)
    }

    func testPitchPipeSounding() throws {
        DPSettingsModel.sharedInstance.toggleNotes = true
        PitchInstrumentView.breathingFrozen = true
        defer { PitchInstrumentView.breathingFrozen = false }
        let app = try launch()
        app.show(tab: 0)
        ScreenCatalog.settle()
        app.pressInstrument(["C, octave 4"], toggle: true)
        capture("pipe-one-note", app, settle: 0.01)
        app.pressInstrument(["E, octave 4", "G, octave 4", "A sharp, B flat, octave 4"], toggle: true)
        capture("pipe-barbershop", app, settle: 0.01)
    }

    // MARK: - Notes and keys

    func testNotesAndKeys() throws {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "-dark" : ""
            let app = try launch(style)
            app.show(tab: 1)
            capture("notes\(suffix)", app)
            app.show(tab: 2)
            capture("keys-major\(suffix)", app)
            app.tearDown()
        }
        let app = try launch()
        app.showMinorKeys()
        capture("keys-minor", app)
    }

    // MARK: - Songs

    func testSongsEmpty() throws {
        let app = try launch()
        app.show(tab: 3)
        capture("songs-empty", app)
    }

    func testSongsPopulated() throws {
        seedSongs()
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.show(tab: 3)
            capture("songs\(style == .dark ? "-dark" : "")", app)
            app.tearDown()
        }
        let app = try launch()
        app.pressSong(row: 1)
        capture("songs-pressed", app)
    }

    func testSongsEditing() throws {
        seedSongs()
        let app = try launch()
        app.editSongs()
        capture("songs-editing", app)
    }

    func testSongsCustomLists() throws {
        let lists = seedSongs()
        DPSongsModel.sharedInstance.currentListId = lists[0].id
        let app = try launch()
        app.show(tab: 3)
        capture("songs-custom", app)
        DPSongsModel.sharedInstance.currentListId = lists[1].id
        capture("songs-custom-empty", app)
    }

    func testSetListsScreen() throws {
        seedSongs()
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.editSongs()
            app.manageSetLists()
            capture("setlists\(style == .dark ? "-dark" : "")", app, settle: 0.8)
            app.tearDown()
        }
    }

    func testAddSongsPicker() throws {
        let lists = seedSongs()
        DPSongsModel.sharedInstance.currentListId = lists[1].id
        let app = try launch()
        app.editSongs()
        app.addSongsFromAnotherList(choose: [IndexPath(row: 0, section: 0), IndexPath(row: 2, section: 0)])
        capture("addsongs", app, settle: 0.8)
    }

    func testSetListAlerts() throws {
        let lists = seedSongs()
        let app = try launch()
        app.show(tab: 3)
        app.promptNewSetList(typing: nil)
        capture("alert-new-set-list", app, settle: 0.8)
        app.topPresented.dismiss(animated: false)
        ScreenCatalog.settle()
        app.promptNewSetList(typing: "My Songs")
        capture("alert-new-set-list-error", app, settle: 0.8)
        app.topPresented.dismiss(animated: false)
        ScreenCatalog.settle()
        app.confirmDelete(lists[0])
        capture("alert-delete-set-list", app, settle: 0.8)
    }

    // MARK: - Song editor

    func testSongEditor() throws {
        seedSongs(lists: false)
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.editSongs()
            app.addSong()
            capture("editor-add\(style == .dark ? "-dark" : "")", app, settle: 0.8)
            app.tearDown()
        }
        var app = try launch()
        app.editSongs()
        app.editSong(row: 3)
        capture("editor-edit", app, settle: 0.8)
        app.tearDown()
        app = try launch()
        app.editSongs()
        app.addSong()
        ScreenCatalog.settle()
        app.pressEditorDone()
        capture("editor-title-required", app, settle: 0.8)
    }

    // MARK: - Settings, login, privacy

    func testSettings() throws {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.show(tab: 0)
            app.openSettings()
            capture("settings\(style == .dark ? "-dark" : "")", app, settle: 0.8)
            app.tearDown()
        }
    }

    func testLogin() throws {
        let app = try launch()
        app.show(tab: 0)
        app.showLogin()
        capture("login", app, settle: 0.8)
    }

    func testPrivacyChoices() throws {
        let app = try launch()
        app.show(tab: 0)
        app.showPrivacyChoices()
        capture("privacy", app, settle: 0.8)
    }
}
