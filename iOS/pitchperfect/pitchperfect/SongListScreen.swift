//
//  SongListScreen.swift
//  pitchperfect
//
//  The Songs tab: the set-list selector over the current list's songs. Pressing
//  a song sounds its key. Edit mode deletes, reorders and edits songs and
//  offers the list's song actions; each set list's own actions live on its
//  position (a long press) and on the Set Lists screen.
//

import SwiftUI
import UIKit

@Observable
@MainActor
final class SongListModel {
    let store: DPSongsModel
    let player: NotePlayer
    let prompts: SetListPromptModel
    @ObservationIgnored private var songsObserver: NSObjectProtocol?

    /// Bumped whenever the store posts a change, so everything read from it refreshes.
    private(set) var revision = 0
    var isEditing = false
    /// The song editor sheet, when open.
    var editor: SongEditorRequest?
    var showingAddFrom = false
    var showingManage = false
    var showingSettings = false
    enum ScrollTarget: Equatable {
        case top
        case row(ObjectIdentifier, anchor: UnitPoint)
    }

    /// Where the rows should scroll once laid out: a list's saved place, or an added song.
    var pendingScrollTarget: ScrollTarget?
    /// Each list keeps its own place: the first row showing when it was left.
    @ObservationIgnored private(set) var savedTopRows: [String: ObjectIdentifier] = [:]
    @ObservationIgnored private var visibleRows: [ObjectIdentifier: Int] = [:]
    /// The list the rows are showing, so a switch can be told apart from a change within it.
    @ObservationIgnored private var shownListId: String

    init(store: DPSongsModel = .sharedInstance, player: NotePlayer = .shared) {
        self.store = store
        self.player = player
        self.prompts = SetListPromptModel(model: store)
        shownListId = store.currentListId
        songsObserver = NotificationCenter.default.addObserver(
            forName: .songsChanged, object: nil, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.songsChanged() }
        }
    }

    deinit {
        if let songsObserver { NotificationCenter.default.removeObserver(songsObserver) }
    }

    private func songsChanged() {
        let listId = store.currentListId
        if listId != shownListId {
            shownListId = listId
            pendingScrollTarget = savedTopRows[listId].map { .row($0, anchor: .top) } ?? .top
        }
        revision += 1
    }

    // MARK: Reading

    var lists: [DPSongList] { _ = revision; return store.orderedLists }
    var currentListId: String { _ = revision; return store.currentListId }
    var currentList: DPSongList { _ = revision; return store.currentList }
    var songs: [DPPitchedSong] { currentList.songs }
    var isHome: Bool { currentListId == DPSongsModel.defaultListId }
    func displayName(_ list: DPSongList) -> String { store.displayName(for: list) }
    var canAddFromAnotherList: Bool { _ = revision; return store.hasAddableSongs(for: store.currentList) }

    var emptyText: String {
        isHome
            ? "NO SONGS ON FILE\n\nTap + to add your first song and its key"
            : "NOTHING IN THIS SET LIST\n\nTap + to add a song, or tap the pencil to add songs from another set list"
    }

    var emptyAccessibilityLabel: String {
        isHome
            ? "No songs on file. Tap Add to add your first song and its key."
            : "Nothing in this set list. Tap Add to add a song, or tap Edit to add songs from another set list."
    }

    // MARK: Scroll memory

    func rowAppeared(_ song: DPPitchedSong, at index: Int) { visibleRows[ObjectIdentifier(song)] = index }
    func rowDisappeared(_ song: DPPitchedSong) { visibleRows[ObjectIdentifier(song)] = nil }

    private func rememberPlace() {
        savedTopRows[shownListId] = visibleRows.min { $0.value < $1.value }?.key
        visibleRows = [:]
    }

    // MARK: Editing

    func edit() { isEditing = true }
    func doneEditing() { isEditing = false }

    // MARK: Sounding

    /// The row a finger is on, lit while it stays there.
    private(set) var pressedSong: ObjectIdentifier?

    func press(_ song: DPPitchedSong) {
        guard let note = song.key?.note else { return }
        pressedSong = song.rowID
        player.pressBegan(note)
    }

    func release(_ song: DPPitchedSong) {
        if pressedSong == song.rowID { pressedSong = nil }
        guard let note = song.key?.note else { return }
        player.pressEnded(note)
    }

    /// Lit while pressed, and while a toggled note keeps sounding.
    func isLit(_ song: DPPitchedSong) -> Bool {
        if pressedSong == song.rowID { return true }
        guard player.toggleNotes(), let note = song.key?.note else { return false }
        return player.isPlaying(note)
    }

    func stopSoundingRows() {
        pressedSong = nil
        player.stop(songs.compactMap { $0.key?.note })
    }

    func sort() {
        currentList.sortSongs()
        currentList.storeValue()
    }

    func deleteSongs(at offsets: IndexSet) {
        let list = currentList
        for index in offsets.sorted(by: >) { list.removeSong(atIndex: index) }
        list.storeValue()
    }

    func moveSongs(from source: IndexSet, to destination: Int) {
        let list = currentList
        var reordered = list.songs
        reordered.move(fromOffsets: source, toOffset: destination)
        list.songs = reordered
        list.storeValue()
    }

    func addSong() {
        let song = DPPitchedSong()
        let majors = DPKey.majorKeys() as? [DPKey] ?? []
        song.key = majors[majors.count / 2]
        let list = currentList
        editor = SongEditorRequest(song: song, isNew: true) { [weak self] saved in
            guard saved else { return }
            list.addSong(song)
            list.storeValue()
            self?.pendingScrollTarget = .row(ObjectIdentifier(song), anchor: .center)
        }
    }

    func editSong(_ song: DPPitchedSong) {
        let list = currentList
        editor = SongEditorRequest(song: song, isNew: false) { saved in
            guard saved else { return }
            list.storeValue()
        }
    }

    // MARK: Set lists

    func select(listId: String) {
        guard store.currentListId != listId else { return }
        stopSoundingRows()
        rememberPlace()
        store.currentListId = listId
    }

    func promptNewSetList() {
        prompts.create { [weak self] name in
            guard let self, let created = self.store.createList(named: name) else { return }
            self.stopSoundingRows()
            self.rememberPlace()
            self.store.currentListId = created.id
            self.doneEditing()
        }
    }

    func promptRename(_ list: DPSongList) {
        prompts.rename(list) { [weak self] name in
            _ = self?.store.renameList(list, to: name)
        }
    }

    func duplicate(_ list: DPSongList) {
        guard let copy = store.duplicateList(list) else { return }
        stopSoundingRows()
        rememberPlace()
        // The natural next step is pruning the copy, so edit mode stays on.
        store.currentListId = copy.id
        UIAccessibility.post(notification: .announcement, argument: "Duplicated as \(store.displayName(for: copy))")
    }

    func confirmDelete(_ list: DPSongList) {
        guard list.id != DPSongsModel.defaultListId else { return }
        prompts.delete(list) { [weak self] in
            guard let self else { return }
            let wasCurrent = self.store.currentListId == list.id
            if wasCurrent {
                self.stopSoundingRows()
                self.rememberPlace()
            }
            _ = self.store.deleteList(list)
            // Deleting the list on screen leaves edit mode; deleting another one
            // from its position leaves the tab as it was.
            if wasCurrent { self.doneEditing() }
        }
    }

    func manageSetLists() { showingManage = true }
    func addSongsFromAnotherList() { showingAddFrom = true }
}

/// What the song editor sheet edits, and what to do when it closes.
struct SongEditorRequest: Identifiable {
    let id = UUID()
    let song: DPPitchedSong
    let isNew: Bool
    /// Called with true when the song was saved, false when cancelled.
    let completion: (Bool) -> Void
}

// MARK: - Screen

struct SongListScreen: View {
    @Bindable var model: SongListModel

    var body: some View {
        InstrumentPage {
            VStack(spacing: 0) {
                SetListSelector(model: model)
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 8)
                SongRows(model: model)
            }
        }
        .navigationTitle("Songs")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar { toolbar }
        .environment(\.editMode, .constant(model.isEditing ? .active : .inactive))
        .settingsSheet(isPresented: $model.showingSettings)
        .sheet(item: $model.editor) { request in
            NavigationStack {
                SongEditorScreen(request: request)
            }
        }
        .sheet(isPresented: $model.showingAddFrom) {
            AddSongsScreen(target: model.currentList)
        }
        .navigationDestination(isPresented: $model.showingManage) {
            SetListsScreen()
        }
        .setListPrompts(model.prompts)
        .onDisappear { model.stopSoundingRows() }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            if model.isEditing {
                BarSymbolButton(systemName: "checkmark") { model.doneEditing() }
            } else {
                BarSymbolButton(systemName: "pencil") { model.edit() }
            }
        }
        if model.isEditing {
            ToolbarItemGroup(placement: .topBarTrailing) {
                BarSymbolMenu(systemName: "ellipsis.circle") { songActions }
                BarSymbolButton(systemName: "plus") { model.addSong() }
            }
        } else {
            ToolbarItem(placement: .topBarTrailing) {
                BarSymbolButton(systemName: "gearshape") { model.showingSettings = true }
            }
        }
    }

    /// Editing a set list's songs, and nothing else: whose list this is already
    /// shows in the selector, so renaming, duplicating and deleting it belong to
    /// its own position and to the Set Lists screen, not to this menu.
    @ViewBuilder
    private var songActions: some View {
        Button("Sort Alphabetically", systemImage: "textformat.abc") { model.sort() }
        if model.canAddFromAnotherList {
            Button("Add songs from another set list…", systemImage: "text.badge.plus") {
                model.addSongsFromAnotherList()
            }
        } else {
            // Disabled alone reads as a bug; the subtitle says why.
            Button {} label: {
                Label {
                    Text("Add songs from another set list…")
                    Text("Nothing to add")
                } icon: {
                    Image(systemName: "text.badge.plus")
                }
            }
            .disabled(true)
        }
        Button("Manage set lists…", systemImage: "list.bullet") { model.manageSetLists() }
    }
}

/// Rename, Duplicate, Delete (never for My Songs) and Manage — the actions a
/// list has where it is named: a long press on its position.
struct SetListActions: View {
    let list: DPSongList
    let model: SongListModel

    var body: some View {
        Section(model.displayName(list)) {
            Button("Rename set list…", systemImage: "pencil") { model.promptRename(list) }
            Button("Duplicate set list", systemImage: "plus.square.on.square") { model.duplicate(list) }
            if list.id != DPSongsModel.defaultListId {
                Button("Delete set list…", systemImage: "trash", role: .destructive) { model.confirmDelete(list) }
            }
            Button("Manage set lists…", systemImage: "list.bullet") { model.manageSetLists() }
        }
    }
}

private struct SongRows: View {
    let model: SongListModel

    var body: some View {
        let songs = model.songs
        ScrollViewReader { proxy in
            List {
                ForEach(Array(songs.enumerated()), id: \.element.rowID) { index, song in
                    SongRow(song: song, isEditing: model.isEditing, lit: model.isLit(song),
                            press: { pressed in pressed ? model.press(song) : model.release(song) },
                            edit: { model.editSong(song) })
                        .plateRow()
                        .id(ObjectIdentifier(song))
                        .onAppear { model.rowAppeared(song, at: index) }
                        .onDisappear { model.rowDisappeared(song) }
                }
                .onDelete(perform: model.deleteSongs)
                .onMove(perform: model.moveSongs)
            }
            .plateList()
            .overlay(alignment: .top) {
                if songs.isEmpty {
                    Text(model.emptyText)
                        .font(Font(UIFont(name: "Oswald-Medium", size: 15) ?? .preferredFont(forTextStyle: .callout)))
                        .foregroundStyle(Plate.inkSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                        .padding(.top, 48)
                        .accessibilityLabel(model.emptyAccessibilityLabel)
                        .accessibilityIdentifier("songs.empty")
                }
            }
            .onChange(of: model.pendingScrollTarget) { _, target in
                guard let target else { return }
                model.pendingScrollTarget = nil
                switch target {
                case .top:
                    if let first = songs.first { proxy.scrollTo(ObjectIdentifier(first), anchor: .top) }
                case .row(let id, let anchor):
                    withAnimation(anchor == .top ? nil : .default) { proxy.scrollTo(id, anchor: anchor) }
                }
            }
        }
    }
}

/// Condensed title, monospaced key readout, and a lit plate while the note
/// sounds; in edit mode the detail-disclosure button that opens the editor.
struct SongRow: View {
    let song: DPPitchedSong
    let isEditing: Bool
    let lit: Bool
    let press: (Bool) -> Void
    let edit: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button {} label: { EmptyView() }
                .buttonStyle(SongPressStyle(song: song, lit: lit, press: press))
                .accessibilityLabel("\(song.name ?? ""), \(song.key?.friendlyName() ?? "")")
            if isEditing {
                Button(action: edit) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 22))
                        .foregroundStyle(Color.accentColor)
                }
                .buttonStyle(.borderless)
                .padding(.horizontal, 8)
                .accessibilityLabel("More Info")
                .accessibilityIdentifier("song.edit")
            }
        }
    }
}

private struct SongPressStyle: ButtonStyle {
    let song: DPPitchedSong
    let lit: Bool
    let press: (Bool) -> Void

    func makeBody(configuration: Configuration) -> some View {
        HStack(alignment: .center, spacing: 0) {
            Text(song.name ?? "")
                .font(Plate.text(20))
                .foregroundStyle(lit ? Plate.onLit : Plate.ink)
                .lineLimit(1)
            Spacer(minLength: 16)
            KeyReadout(key: song.key, color: lit ? Plate.onLit : Plate.inkSecondary)
                .fixedSize()
                .layoutPriority(1)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(lit ? Plate.lit : Color.clear)
        .animation(.easeInOut(duration: 0.12), value: lit)
        .contentShape(Rectangle())
        .onChange(of: configuration.isPressed) { _, pressed in press(pressed) }
    }
}

/// The key's name in the measurement face plus its NoteHedz accidental glyph.
struct KeyReadout: View {
    let key: DPKey?
    let color: Color

    private static let size: CGFloat = 18

    var body: some View {
        if let key {
            var text = Text(key.friendlyName() ?? "")
                .font(Plate.mono(Self.size))
                .kerning(Self.size * 0.06)
                .foregroundColor(color)
            if let glyph = Plate.glyph(for: SongEditorSpeech.accidental(of: key)) {
                text = text + Text(glyph).font(Plate.noteHedz(Self.size * 1.2)).foregroundColor(color)
            }
            return AnyView(text)
        }
        return AnyView(EmptyView())
    }
}

extension DPPitchedSong {
    /// A row's identity: the object, since legacy songs can share (or lack) an id.
    var rowID: ObjectIdentifier { ObjectIdentifier(self) }
}
