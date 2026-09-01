import SwiftUI

private struct SongEditorSelection: Identifiable {
    let song: DPPitchedSong
    let isNew: Bool
    var id: String { song.id }
}

struct SongListView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var showSettings = false
    @State private var editMode: EditMode = .inactive
    @State private var toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    @State private var editorSelection: SongEditorSelection?
    @State private var activeSongIDs: Set<String> = []
    @StateObject private var songListObserver = SongListObserver()

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                List {
                    ForEach(songListObserver.songs, id: \.id) { song in
                        SongRow(
                            song: song,
                            isEditing: editMode.isEditing,
                            isActive: activeSongIDs.contains(song.id),
                            toggleNotes: toggleNotes,
                            onEdit: { openEditor(for: song) },
                            onToggle: { toggle(song) },
                            onPress: { song.play() },
                            onRelease: { song.stop() }
                        )
                        .listRowBackground(Color.clear)
                    }
                    .onDelete(perform: deleteSongs)
                    .onMove(perform: moveSongs)
                }
                .listStyle(.plain)
                .environment(\.editMode, $editMode)
                .onAppear { UITableView.appearance().backgroundColor = .clear }
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                PitchPerfectBannerAd()
                    .frame(height: 50)
                    .accessibilityIdentifier("BannerAd")
            }
            .navigationBarTitle("Pitch Perfect", displayMode: .inline)
            .navigationBarItems(
                leading: Button(editMode.isEditing ? "Done" : "Edit") {
                    editMode = editMode.isEditing ? .inactive : .active
                },
                trailing: toolbarActions
            )
            .sheet(isPresented: $showSettings, onDismiss: refreshSettings) {
                SettingsView()
            }
            .sheet(item: $editorSelection) { selection in
                SongEditorView(song: selection.song, isNew: selection.isNew) {
                    songListObserver.refresh()
                }
            }
            .onChange(of: editMode) { _, _ in stopAllSongs() }
            .onReceive(NotificationCenter.default.publisher(for: DPSettingsModel.settingsChangedNotificationName)) { _ in
                stopAllSongs()
                refreshSettings()
            }
            .onChange(of: scenePhase) { _, phase in
                if phase != .active { stopAllSongs() }
            }
            .onDisappear(perform: stopAllSongs)
        }
        .navigationViewStyle(.stack)
    }

    @ViewBuilder
    private var toolbarActions: some View {
        if editMode.isEditing {
            HStack {
                Button("Sort", action: sortSongs)
                    .accessibilityIdentifier("SortSongs")
                Button(action: addSong) {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add song")
                .accessibilityIdentifier("AddSong")
            }
        } else {
            Button {
                stopAllSongs()
                showSettings = true
            } label: {
                Image(systemName: "gearshape")
            }
            .accessibilityLabel("Settings")
            .accessibilityIdentifier("Settings")
        }
    }

    private func addSong() {
        let song = DPPitchedSong()
        song.name = ""
        let majorKeys = DPKey.majorKeys().compactMap { $0 as? DPKey }
        song.key = majorKeys.isEmpty ? nil : majorKeys[majorKeys.count / 2]
        editorSelection = SongEditorSelection(song: song, isNew: true)
    }

    private func openEditor(for song: DPPitchedSong) {
        editorSelection = SongEditorSelection(song: song, isNew: false)
    }

    private func deleteSongs(at offsets: IndexSet) {
        var songs = DPSongsModel.sharedInstance.defaultSongList.songs
        offsets.sorted(by: >).forEach { songs.remove(at: $0) }
        DPSongsModel.sharedInstance.defaultSongList.songs = songs
        DPSongsModel.sharedInstance.defaultSongList.storeValue()
    }

    private func moveSongs(from source: IndexSet, to destination: Int) {
        var songs = DPSongsModel.sharedInstance.defaultSongList.songs
        songs.move(fromOffsets: source, toOffset: destination)
        DPSongsModel.sharedInstance.defaultSongList.songs = songs
        DPSongsModel.sharedInstance.defaultSongList.storeValue()
    }

    private func sortSongs() {
        DPSongsModel.sharedInstance.defaultSongList.sortSongs()
        DPSongsModel.sharedInstance.defaultSongList.storeValue()
    }

    private func toggle(_ song: DPPitchedSong) {
        if activeSongIDs.contains(song.id) {
            song.stop()
            activeSongIDs.remove(song.id)
        } else {
            song.play()
            activeSongIDs.insert(song.id)
        }
    }

    private func refreshSettings() {
        toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    }

    private func stopAllSongs() {
        songListObserver.songs.forEach { $0.stop() }
        activeSongIDs.removeAll()
    }
}

@MainActor
final class SongListObserver: ObservableObject {
    @Published private(set) var songs: [DPPitchedSong] = []
    private var observer: NSObjectProtocol?

    init(center: NotificationCenter = .default) {
        refresh()
        observer = center.addObserver(
            forName: DPSongsModel.songsChangedNotificationName,
            object: DPSongsModel.sharedInstance.defaultSongList,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.refresh() }
        }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    func refresh() {
        songs = DPSongsModel.sharedInstance.defaultSongList.songs
    }
}

struct SongRow: View {
    let song: DPPitchedSong
    let isEditing: Bool
    let isActive: Bool
    let toggleNotes: Bool
    let onEdit: () -> Void
    let onToggle: () -> Void
    let onPress: () -> Void
    let onRelease: () -> Void

    var body: some View {
        AudioPressSurface(
            toggleMode: toggleNotes,
            isActive: isActive,
            onToggle: isEditing ? onEdit : onToggle,
            onPress: isEditing ? {} : onPress,
            onRelease: isEditing ? {} : onRelease
        ) { highlighted in
            HStack {
                Text(song.name.isEmpty ? "Untitled Song" : song.name)
                    .foregroundColor(.primary)
                Spacer()
                if let key = song.key {
                    Text(key.friendlyName() ?? "Unknown")
                        .font(.body.bold())
                }
            }
            .padding(.vertical, 8)
            .frame(minHeight: 44)
            .background(highlighted && !isEditing ? Color.accentColor.opacity(0.18) : Color.clear)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isEditing ? "Edit song \(song.name ?? "Untitled Song")" : "\(song.name ?? "Untitled Song"), \(song.key?.friendlyName() ?? "No key")")
        .accessibilityValue(isActive ? "Playing" : "Stopped")
    }
}
