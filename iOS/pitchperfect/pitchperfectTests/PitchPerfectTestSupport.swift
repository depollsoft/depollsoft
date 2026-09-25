//
//  PitchPerfectTestSupport.swift
//  pitchperfectTests
//
//  Mounting the app for hosted tests, and answering the alerts it presents.
//

import FirebaseCore
import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

extension UIAlertController {
    /// Types into the alert's field the way the keyboard would, so the
    /// validation that gates the confirming action actually runs.
    func pp_type(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let field = textFields?.first else {
            return XCTFail("This alert has no text field", file: file, line: line)
        }
        field.text = text
        field.sendActions(for: .editingChanged)
        NotificationCenter.default.post(name: UITextField.textDidChangeNotification, object: field)
        ScreenCatalog.settle(0.1)
    }

    /// Runs the handler of the named action, the way tapping it would.
    func pp_fire(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let action = actions.first(where: { $0.title == title }) else {
            return XCTFail("No “\(title)” action in this alert", file: file, line: line)
        }
        XCTAssertTrue(action.isEnabled, "“\(title)” is disabled", file: file, line: line)
        typealias Handler = @convention(block) (UIAlertAction) -> Void
        guard action.responds(to: Selector(("handler"))), let raw = action.value(forKey: "handler") else {
            return XCTFail("UIAlertAction no longer exposes its handler", file: file, line: line)
        }
        unsafeBitCast(raw as AnyObject, to: Handler.self)(action)
    }
}

/// Drives the app into catalog states: through its controls where a finger
/// would, through its models where a gesture cannot be synthesized.
@MainActor
final class HostedApp {
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
        models.keys.mode = .minor
        ScreenCatalog.settle(0.2)
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

    /// The titles of every navigation bar on screen, sheets included.
    var navigationTitles: [String] {
        descendants(of: UINavigationBar.self, in: window).compactMap { $0.topItem?.title }
    }

    func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }
}


/// Every hosted test starts from the same local state (no Firebase listeners,
/// no ad requests, no animations, one empty set list, nothing sounding) and
/// puts back whatever it changed.
@MainActor
class PitchPerfectTestCase: XCTestCase {
    private var savedDefaults: [String: Any] = [:]
    private var savedLists: [String: DPSongList] = [:]
    private(set) var app: HostedApp?

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
            NotificationCenter.default.post(name: .settingsChanged, object: DPSettingsModel.sharedInstance)
            UIView.setAnimationsEnabled(true)
            unsetenv("STORE_SCREENSHOTS")
        }
    }

    func stopAll() {
        NotePlayer.shared.stop(DPNote.commonNotes() as! [DPNote])
    }

    /// Mounts the whole app, full screen, in `style`.
    func launch(_ style: UIUserInterfaceStyle = .light) throws -> HostedApp {
        // The app applies the stored theme to its windows; store the one wanted.
        UserDefaults.standard.set(style == .dark ? 2 : 0, forKey: "depollsoft.pitchperfect.theme")
        let app = try HostedApp(style: style)
        self.app = app
        return app
    }

    /// Songs in My Songs, each in the given (or middle) major key.
    @discardableResult
    func seedSongs(_ names: [String], keys: [DPKey]? = nil) -> [DPPitchedSong] {
        let majors = DPKey.majorKeys() as! [DPKey]
        let songs = names.enumerated().map { index, name in
            let song = DPPitchedSong()
            song.name = name
            song.key = keys?[index] ?? majors[6]
            return song
        }
        DPSongsModel.sharedInstance.defaultSongList.songs = songs
        return songs
    }

    func customList(named name: String) throws -> DPSongList {
        try XCTUnwrap(DPSongsModel.sharedInstance.createList(named: name))
    }

    func settle(_ seconds: TimeInterval = 5, file: StaticString = #filePath, line: UInt = #line,
                until condition: () -> Bool) {
        let deadline = Date().addingTimeInterval(seconds)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.01))
        }
        XCTAssertTrue(condition(), "Timed out waiting", file: file, line: line)
    }
}
