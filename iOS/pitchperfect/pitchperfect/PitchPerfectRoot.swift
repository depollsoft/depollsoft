//
//  PitchPerfectRoot.swift
//  pitchperfect
//
//  The app's four tabs, each its own navigation stack, plus what the whole app
//  does regardless of tab: the stored theme, the wake lock, auth callbacks and
//  the privacy prompt on activation.
//

import SwiftUI
import UIKit

enum PitchPerfectTab: Hashable {
    case pitchPipe, notes, keys, songs
}

/// The models behind the tabs, made once for the app (or a test) and kept for
/// as long as it runs, so every tab comes back as it was left.
@Observable
@MainActor
final class PitchPerfectModels {
    var tab = PitchPerfectTab.pitchPipe
    let pitchPipe: PitchPipeModel
    let notes: NotesModel
    let keys: KeysModel
    let songs: SongListModel

    init(pitchPipe: PitchPipeModel? = nil, notes: NotesModel? = nil, keys: KeysModel? = nil,
         songs: SongListModel? = nil) {
        self.pitchPipe = pitchPipe ?? PitchPipeModel()
        self.notes = notes ?? NotesModel()
        self.keys = keys ?? KeysModel()
        self.songs = songs ?? SongListModel()
    }
}

struct PitchPerfectRoot: View {
    @Bindable var models: PitchPerfectModels
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        TabView(selection: $models.tab) {
            NavigationStack { PitchPipeScreen(model: models.pitchPipe) }
                .tabItem { TabLabel(title: "Pitch Pipe", image: "pitchpipe.png") }
                .tag(PitchPerfectTab.pitchPipe)
            NavigationStack { NotesScreen(model: models.notes) }
                .tabItem { TabLabel(title: "Notes", image: "notes.png") }
                .tag(PitchPerfectTab.notes)
            NavigationStack { KeysScreen(model: models.keys) }
                .tabItem { TabLabel(title: "Keys", image: "keys.png") }
                .tag(PitchPerfectTab.keys)
            NavigationStack { SongListScreen(model: models.songs) }
                .tabItem { TabLabel(title: "Songs", image: "songs.png") }
                .tag(PitchPerfectTab.songs)
        }
        // On iPhone the tab bar is tinted with the label colour; the iPad's top
        // tab bar kept the system accent, as UIKit drew them.
        .tint(UIDevice.current.userInterfaceIdiom == .pad ? nil : Color(uiColor: .label))
        .onAppear {
            DPTheme.applyStoredAppearance()
            WakeLock.apply()
        }
        .onReceive(NotificationCenter.default.publisher(for: .settingsChanged)) { _ in WakeLock.apply() }
        .onOpenURL { DPAppDelegate.handle(url: $0) }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { DPAppDelegate.sceneDidBecomeActive() }
        }
    }
}

/// The tab icons ship as template images so Liquid Glass never morphs or flickers them.
private struct TabLabel: View {
    let title: String
    let image: String

    var body: some View {
        Label {
            Text(title)
        } icon: {
            Image(uiImage: (UIImage(named: image) ?? UIImage()).withRenderingMode(.alwaysTemplate))
        }
    }
}

/// Keeps the screen awake while Wake Lock is on. (The UIKit app stored the
/// setting but never applied it.)
enum WakeLock {
    @MainActor
    static func apply(settings: DPSettingsModel = .sharedInstance,
                      application: UIApplication = .shared) {
        application.isIdleTimerDisabled = settings.wakeLock
    }
}
