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

@MainActor
final class PitchPerfectScreenCatalogTests: PitchPerfectTestCase {
    private func capture(_ name: String, _ app: HostedApp, settle: TimeInterval = 0.5) {
        ScreenCatalog.capture(name, window: app.window, settle: settle)
    }

    private static let songTitles = ["Blue Skies", "Down Our Way", "Heart of My Heart", "Shenandoah",
                                     "Sweet Adeline", "The Old Songs", "When You Were Sweet Sixteen",
                                     "You Are My Sunshine"]

    /// Eight songs in several keys, plus two custom set lists.
    @discardableResult
    private func seedCatalog(lists: Bool = true) -> [DPSongList] {
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
        seedCatalog()
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
        seedCatalog()
        let app = try launch()
        app.editSongs()
        capture("songs-editing", app)
    }

    func testSongsCustomLists() throws {
        let lists = seedCatalog()
        DPSongsModel.sharedInstance.currentListId = lists[0].id
        let app = try launch()
        app.show(tab: 3)
        capture("songs-custom", app)
        DPSongsModel.sharedInstance.currentListId = lists[1].id
        capture("songs-custom-empty", app)
    }

    func testSetListsScreen() throws {
        seedCatalog()
        for style in [UIUserInterfaceStyle.light, .dark] {
            let app = try launch(style)
            app.editSongs()
            app.manageSetLists()
            capture("setlists\(style == .dark ? "-dark" : "")", app, settle: 0.8)
            app.tearDown()
        }
    }

    func testAddSongsPicker() throws {
        let lists = seedCatalog()
        DPSongsModel.sharedInstance.currentListId = lists[1].id
        let app = try launch()
        app.editSongs()
        app.addSongsFromAnotherList(choose: [IndexPath(row: 0, section: 0), IndexPath(row: 2, section: 0)])
        capture("addsongs", app, settle: 0.8)
    }

    func testSetListAlerts() throws {
        let lists = seedCatalog()
        let app = try launch()
        app.show(tab: 3)
        app.promptNewSetList(typing: nil)
        capture("alert-new-set-list", app, settle: 0.8)
        (app.topPresented as! UIAlertController).pp_fire("Cancel")
        ScreenCatalog.settle()
        app.promptNewSetList(typing: "My Songs")
        capture("alert-new-set-list-error", app, settle: 0.8)
        (app.topPresented as! UIAlertController).pp_fire("Cancel")
        ScreenCatalog.settle()
        app.confirmDelete(lists[0])
        capture("alert-delete-set-list", app, settle: 0.8)
    }

    // MARK: - Song editor

    func testSongEditor() throws {
        seedCatalog(lists: false)
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
