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
        /// An exact content offset, as UITableView restored it.
        case offset(CGFloat)
    }

    /// Where a list was left: its exact offset once the rows have reported the
    /// list's scroll view, otherwise the first row showing.
    enum Place: Equatable {
        case offset(CGFloat)
        case row(ObjectIdentifier)
    }

    /// Where the rows should scroll once laid out: a list's saved place, or an added song.
    var pendingScrollTarget: ScrollTarget?
    /// Each list keeps its own place, however the switch came about.
    @ObservationIgnored private(set) var savedPlaces: [String: Place] = [:]
    @ObservationIgnored private var visibleRows: [ObjectIdentifier: Int] = [:]
    /// The List's scroll view, reported by its rows, so a list's exact offset
    /// can be filed and restored as UITableView's was.
    @ObservationIgnored weak var listScrollView: UIScrollView?
    @ObservationIgnored private let selectionFeedback = UISelectionFeedbackGenerator()
    /// The list the rows are showing, so a switch can be told apart from a change within it.
    @ObservationIgnored private var shownListId: String

    init(store: DPSongsModel = .sharedInstance, player: NotePlayer = .shared) {
        self.store = store
        self.player = player
        self.prompts = SetListPromptModel(model: store)
        shownListId = store.currentListId
        selectionFeedback.prepare()
        songsObserver = NotificationCenter.default.addObserver(
            forName: .songsChanged, object: nil, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.songsChanged() }
        }
    }

    deinit {
        if let songsObserver { NotificationCenter.default.removeObserver(songsObserver) }
    }

    /// Every change re-renders; a switch of list (from here, the Set Lists screen
    /// or another device) also files the old list's place and restores the new one's.
    private func songsChanged() {
        let listId = store.currentListId
        if listId != shownListId {
            if let place = currentPlace { savedPlaces[shownListId] = place }
            visibleRows = [:]
            shownListId = listId
            switch savedPlaces[listId] {
            case .offset(let y): pendingScrollTarget = .offset(y)
            case .row(let id): pendingScrollTarget = .row(id, anchor: .top)
            case nil: pendingScrollTarget = .top
            }
        }
        revision += 1
    }

    /// The song rows' contents changed in place (an edited title or key), which
    /// the store does not announce.
    func contentChanged() { revision += 1 }

    /// Every row is redrawn when this changes. A List drops a row update made
    /// while a presented editor is still going away, so the rows are redrawn
    /// again once it has gone.
    private(set) var rowGeneration = 0

    /// The editor has finished going away.
    func editorDidClose() { rowGeneration += 1 }

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

    private var currentPlace: Place? {
        if let list = listScrollView, list.window != nil { return .offset(list.contentOffset.y) }
        return visibleRows.min { $0.value < $1.value }.map { .row($0.key) }
    }

    /// A list comes back where it was left, clamped to what it now holds; a
    /// list never shown starts at the top.
    func restoreOffset(_ target: ScrollTarget) {
        guard let list = listScrollView else { return }
        list.layoutIfNeeded()
        let top = -list.adjustedContentInset.top
        let bottom = max(top, list.contentSize.height - list.bounds.height + list.adjustedContentInset.bottom)
        let y: CGFloat
        if case .offset(let saved) = target { y = min(max(saved, top), bottom) } else { y = top }
        list.setContentOffset(CGPoint(x: list.contentOffset.x, y: y), animated: false)
    }

    // MARK: Editing

    /// Animated, as `setEditing:animated:YES` and the bar's animated item swap were.
    func edit() { withAnimation { isEditing = true } }
    func doneEditing() { withAnimation { isEditing = false } }

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

    /// Momentary rows are lit while pressed; with Toggle Notes a row is lit while
    /// its note sounds, so pressing a sounding row goes dark at touch-down.
    func isLit(_ song: DPPitchedSong) -> Bool {
        guard player.toggleNotes() else { return pressedSong == song.rowID }
        guard let note = song.key?.note else { return false }
        return player.isPlaying(note)
    }

    /// A VoiceOver double-tap on a row: its note for a moment, or toggled.
    func activate(_ song: DPPitchedSong) {
        guard let note = song.key?.note else { return }
        player.activate(note)
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
            self?.editor = nil
            guard saved else { return }
            list.addSong(song)
            list.storeValue()
            self?.pendingScrollTarget = .row(ObjectIdentifier(song), anchor: .center)
        }
    }

    func editSong(_ song: DPPitchedSong) {
        let list = currentList
        editor = SongEditorRequest(song: song, isNew: false) { [weak self] saved in
            self?.editor = nil
            guard saved else { return }
            list.storeValue()
            // The row shows the new title and key (UIKit reloaded the table).
            self?.contentChanged()
        }
    }

    // MARK: Set lists

    func select(listId: String) {
        guard store.currentListId != listId else { return }
        stopSoundingRows()
        store.currentListId = listId
    }

    /// A tap on a selector position: the selection click, then the switch.
    func choosePosition(listId: String) {
        selectionFeedback.selectionChanged()
        selectionFeedback.prepare()
        select(listId: listId)
    }

    func promptNewSetList() {
        prompts.create { [weak self] name in
            guard let self, let created = self.store.createList(named: name) else { return }
            self.stopSoundingRows()
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
        // The natural next step is pruning the copy, so edit mode stays on.
        store.currentListId = copy.id
        UIAccessibility.post(notification: .announcement, argument: "Duplicated as \(store.displayName(for: copy))")
    }

    func confirmDelete(_ list: DPSongList) {
        guard list.id != DPSongsModel.defaultListId else { return }
        prompts.delete(list) { [weak self] in
            guard let self else { return }
            let wasCurrent = self.store.currentListId == list.id
            if wasCurrent { self.stopSoundingRows() }
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
        .background(SongEditorPresenter(request: model.editor, didClose: model.editorDidClose).frame(width: 0, height: 0))
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
                ForEach(SongRowData.rows(songs, generation: model.rowGeneration)) { row in
                    let song = row.song
                    let index = row.index
                    SongRow(title: row.title, key: song.key, isEditing: model.isEditing, lit: model.isLit(song),
                            press: { pressed in pressed ? model.press(song) : model.release(song) },
                            activate: { model.activate(song) },
                            edit: { model.editSong(song) })
                        .plateRow(contentIndent: model.isEditing ? SongRow.editingIndent : 0,
                                  trailingOverhang: model.isEditing ? 40 : 0)
                        .id(ObjectIdentifier(song))
                        // Reordering is edit mode's; outside it a long press is a held note.
                        .moveDisabled(!model.isEditing)
                        .onAppear { model.rowAppeared(song, at: index) }
                        .onDisappear { model.rowDisappeared(song) }
                        .background(ListScrollViewReporter { model.listScrollView = $0 }.frame(width: 0, height: 0))
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
            .modifier(ScrollMemory(model: model, songs: songs, proxy: proxy))
        }
    }
}

/// Puts a list back exactly where it was left, as the UIKit table's saved
/// content offset did: the model reads and writes the List's own scroll view,
/// which each row reports (SwiftUI's ScrollPosition does not drive a List).
private struct ScrollMemory: ViewModifier {
    let model: SongListModel
    let songs: [DPPitchedSong]
    let proxy: ScrollViewProxy

    func body(content: Content) -> some View {
        content.onChange(of: model.pendingScrollTarget) { _, target in
            guard let target else { return }
            model.pendingScrollTarget = nil
            switch target {
            case .top, .offset:
                // After SwiftUI has handed the new rows to the list.
                DispatchQueue.main.async { model.restoreOffset(target) }
            case .row(let id, let anchor):
                withAnimation(anchor == .top ? nil : .default) { proxy.scrollTo(id, anchor: anchor) }
            }
        }
    }
}

/// Finds the collection view a List row lives in and hands it to `found`.
private struct ListScrollViewReporter: UIViewRepresentable {
    let found: (UIScrollView) -> Void

    final class Reporter: UIView {
        var found: (UIScrollView) -> Void = { _ in }
        override func didMoveToWindow() {
            super.didMoveToWindow()
            guard window != nil else { return }
            var view = superview
            while let current = view, !(current is UICollectionView) { view = current.superview }
            if let list = view as? UIScrollView { found(list) }
        }
    }

    func makeUIView(context: Context) -> Reporter {
        let reporter = Reporter()
        reporter.isUserInteractionEnabled = false
        reporter.isAccessibilityElement = false
        return reporter
    }

    func updateUIView(_ reporter: Reporter, context: Context) { reporter.found = found }
}

/// A row as the List sees it. The song is one object across edits, so the row
/// carries its title and key as values: a List reloads a row only when its
/// element changes, not when the object behind it does (UIKit reloaded the table).
private struct SongRowData: Identifiable, Equatable {
    let id: ObjectIdentifier
    let index: Int
    let title: String
    let keyName: String
    /// Bumped when every row must be redrawn (see SongListModel.rowGeneration).
    let generation: Int
    let song: DPPitchedSong

    static func rows(_ songs: [DPPitchedSong], generation: Int) -> [SongRowData] {
        songs.enumerated().map { index, song in
            SongRowData(id: song.rowID, index: index, title: song.name ?? "",
                        keyName: "\(song.key?.friendlyName() ?? "")\(song.key?.numAccidentals ?? 0)",
                        generation: generation, song: song)
        }
    }

    static func == (lhs: SongRowData, rhs: SongRowData) -> Bool {
        lhs.id == rhs.id && lhs.index == rhs.index && lhs.title == rhs.title && lhs.keyName == rhs.keyName
            && lhs.generation == rhs.generation
    }
}

/// Condensed title, monospaced key readout, and a lit plate while the note
/// sounds; in edit mode the detail-disclosure button that opens the editor.
struct SongRow: View {
    /// The title and key as values: the song is one object across edits, so a
    /// row given only the object would never redraw when its title or key changes.
    let title: String
    let key: DPKey?
    let isEditing: Bool
    let lit: Bool
    let press: (Bool) -> Void
    let activate: () -> Void
    let edit: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            SongRowFace(title: title, key: key, lit: lit, isEditing: isEditing)
                .notePress(began: { press(true) }, ended: { press(false) }, activate: activate)
                .accessibilityLabel("\(title), \(key?.friendlyName() ?? "")")
            if isEditing {
                Button(action: edit) {
                    Image(uiImage: SongRow.detailDisclosure)
                        .renderingMode(.template)
                        .foregroundStyle(Color(uiColor: .systemBlue))
                }
                .buttonStyle(.borderless)
                .padding(.leading, 8)
                .offset(y: -1.0 / 3.0)
                .accessibilityLabel("More Info")
                .accessibilityIdentifier("song.edit")
                // UIKit rules the accessory off from the reorder control.
                Color(uiColor: .separator)
                    .frame(width: 1)
                    .padding(.leading, 22.0 / 3.0)
                    .padding(.trailing, 15)
                    .accessibilityHidden(true)
            }
        }
    }

    /// How far edit mode moves a row's content in, past the delete control.
    static let editingIndent: CGFloat = 124.0 / 3.0

    /// The system detail-disclosure glyph, exactly as UIKit's button draws it.
    static let detailDisclosure: UIImage = UIButton(type: .detailDisclosure).image(for: .normal) ?? UIImage()
}

private struct SongRowFace: View {
    let title: String
    let key: DPKey?
    let lit: Bool
    let isEditing: Bool

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(title)
                .font(Plate.text(20))
                .foregroundStyle(lit ? Plate.onLit : Plate.ink)
                .lineLimit(1)
            Spacer(minLength: 16)
            KeyReadout(key: key, color: lit ? Plate.onLit : Plate.inkSecondary)
                .fixedSize()
                .layoutPriority(1)
                .offset(y: isEditing ? -1.0 / 3.0 : 0)
        }
        // Editing indents the row past the delete control and hands the trailing
        // edge to the accessory, as the UIKit cell's content view did.
        .padding(.leading, isEditing ? 24 : 20)
        .padding(.trailing, isEditing ? 11 : 20)
        // 14 pt above and below the title (whose UILabel rounded its height up,
        // setting the text 2 px lower), plus the 1 pt a self-sizing UIKit cell
        // adds for its separator.
        .padding(.top, 14 + 2.0 / 3.0)
        .padding(.bottom, 15 - 2.0 / 3.0)
        // The lit plate cross-dissolves in 0.12 s, as setHighlighted:animated: did.
        .background(lit ? Plate.lit : Color.clear)
        .animation(.easeInOut(duration: 0.12), value: lit)
        .contentShape(Rectangle())
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

/// Presents the song editor exactly as the UIKit screen did: a navigation
/// controller asking for 320 × 480 (a form sheet on iPad, a page sheet on iPhone),
/// sliding up for a new song and flipping over for an existing one. SwiftUI's
/// sheets have neither the flip nor the form size before iOS 18.
struct SongEditorPresenter: UIViewControllerRepresentable {
    let request: SongEditorRequest?
    /// Called once the editor has gone, however it closed.
    var didClose: () -> Void = {}

    final class Presenter: UIViewController, UIAdaptivePresentationControllerDelegate {
        private weak var presentedEditor: UIViewController?
        private var shownId: UUID?
        private var request: SongEditorRequest?
        var didClose: () -> Void = {}

        func sync(_ request: SongEditorRequest?) {
            guard request?.id != shownId else { return }
            if let editor = presentedEditor, editor.presentingViewController != nil, !editor.isBeingDismissed {
                let closed = didClose
                editor.dismiss(animated: true) { closed() }
            }
            presentedEditor = nil
            shownId = request?.id
            self.request = request
            guard let request else { return }
            let navigation = UINavigationController(rootViewController: SongEditorController(request: request))
            navigation.preferredContentSize = CGSize(width: 320, height: 480)
            navigation.modalTransitionStyle = request.isNew ? .coverVertical : .flipHorizontal
            navigation.modalPresentationStyle = .automatic
            presentedEditor = navigation
            // Present once this controller is in a window; SwiftUI may update it first.
            DispatchQueue.main.async { [weak self] in
                guard let self, self.presentedEditor === navigation else { return }
                var presenter: UIViewController = self
                while let next = presenter.presentedViewController { presenter = next }
                presenter.present(navigation, animated: true)
                navigation.presentationController?.delegate = self
            }
        }

        /// A swipe down closes the editor without saving.
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            let request = self.request
            presentedEditor = nil
            shownId = nil
            self.request = nil
            request?.completion(false)
            didClose()
        }
    }

    func makeUIViewController(context: Context) -> Presenter {
        let presenter = Presenter()
        presenter.view.isHidden = true
        return presenter
    }

    func updateUIViewController(_ presenter: Presenter, context: Context) {
        presenter.didClose = didClose
        presenter.sync(request)
    }
}
