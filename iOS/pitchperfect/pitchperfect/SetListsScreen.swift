//
//  SetListsScreen.swift
//  pitchperfect
//
//  "Set Lists": the manage screen pushed onto the Songs navigation stack.
//  My Songs is the first row and never moves or leaves; every custom row
//  carries a drag handle and a swipe. Tapping a row switches to that list and
//  returns to Songs — the same thing tapping its position in the selector does
//  — so the list's own actions live in the row's trailing "…" menu instead.
//  Rows are transparent over the score, separated by hairlines, as everywhere.
//

import SwiftUI
import UIKit

@Observable
@MainActor
final class SetListsModel {
    let store: DPSongsModel
    let prompts: SetListPromptModel
    @ObservationIgnored private var songsObserver: NSObjectProtocol?
    private(set) var revision = 0

    init(store: DPSongsModel = .sharedInstance) {
        self.store = store
        prompts = SetListPromptModel(model: store)
        songsObserver = NotificationCenter.default.addObserver(
            forName: .songsChanged, object: nil, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.revision += 1 }
        }
    }

    deinit {
        if let songsObserver { NotificationCenter.default.removeObserver(songsObserver) }
    }

    var lists: [DPSongList] { _ = revision; return store.orderedLists }
    var currentListId: String { _ = revision; return store.currentListId }
    func displayName(_ list: DPSongList) -> String { store.displayName(for: list) }
    func isHome(_ list: DPSongList) -> Bool { list.id == DPSongsModel.defaultListId }
    private var customLists: [DPSongList] { lists.filter { !isHome($0) } }

    /// "12 songs", "1 song", "No songs".
    static func countLabel(_ count: Int) -> String {
        switch count {
        case 0: return "No songs"
        case 1: return "1 song"
        default: return "\(count) songs"
        }
    }

    func accessibilityLabel(_ list: DPSongList) -> String {
        let base = "\(displayName(list)), \(SetListSelectorMetrics.songCountPhrase(list.songs.count))"
        return list.id == currentListId ? "\(base), current" : base
    }

    /// Tapping a row does what tapping its position does: it switches to that list.
    func select(_ list: DPSongList) {
        store.currentListId = list.id
    }

    /// Drag-and-drop among the custom rows; My Songs stays first. Committed once, on drop.
    func move(from source: IndexSet, to destination: Int) {
        var reordered = lists
        reordered.move(fromOffsets: source, toOffset: max(destination, 1))
        guard reordered.first.map(isHome) == true else { return }
        _ = store.reorderLists(reordered.dropFirst().map(\.id))
    }

    /// Moves `list` one step among the custom lists (VoiceOver's Move up / Move down).
    @discardableResult
    func move(_ list: DPSongList, by delta: Int) -> Bool {
        var custom = customLists
        guard let from = custom.firstIndex(where: { $0 === list }) else { return false }
        let to = from + delta
        guard custom.indices.contains(to) else { return false }
        custom.insert(custom.remove(at: from), at: to)
        return store.reorderLists(custom.map(\.id))
    }

    func moveActionNames(_ list: DPSongList) -> [String] {
        guard let index = customLists.firstIndex(where: { $0 === list }) else { return [] }
        var names: [String] = []
        if index > 0 { names.append("Move up") }
        if index < customLists.count - 1 { names.append("Move down") }
        return names
    }

    func promptCreate() {
        prompts.create { [weak self] name in
            guard let self, let created = self.store.createList(named: name) else { return }
            self.store.currentListId = created.id
        }
    }

    func promptRename(_ list: DPSongList) {
        prompts.rename(list) { [weak self] name in _ = self?.store.renameList(list, to: name) }
    }

    func duplicate(_ list: DPSongList) {
        guard let copy = store.duplicateList(list) else { return }
        UIAccessibility.post(notification: .announcement, argument: "Duplicated as \(store.displayName(for: copy))")
    }

    func confirmDelete(_ list: DPSongList) {
        prompts.delete(list) { [weak self] in _ = self?.store.deleteList(list) }
    }
}

struct SetListsScreen: View {
    @State private var model = SetListsModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            ForEach(model.lists, id: \.id) { list in
                row(list)
            }
            .onMove(perform: model.move)
        }
        .plateList(fullScreen: true)
        .environment(\.editMode, .constant(.active))
        .navigationTitle("Set Lists")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                BarSymbolButton(systemName: "plus") { model.promptCreate() }
            }
        }
        .setListPrompts(model.prompts)
    }

    private func row(_ list: DPSongList) -> some View {
        let home = model.isHome(list)
        let current = list.id == model.currentListId
        let name = model.displayName(list)
        return SetListRow(name: name, songCount: list.songs.count, isCurrent: current, movable: !home,
                          menuIdentifier: "setlist.row.menu.\(list.id)") {
            rowMenu(list)
        } select: {
            model.select(list)
            dismiss()
        }
        .plateRow(trailingOverhang: home ? 0 : 40)
        .moveDisabled(home)
        .deleteDisabled(true)
        .swipeActions(edge: .trailing) {
            if !home {
                Button("Rename") { model.promptRename(list) }
                Button("Duplicate") { model.duplicate(list) }
                Button("Delete", role: .destructive) { model.confirmDelete(list) }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(model.accessibilityLabel(list))
        .accessibilityAddTraits(current ? [.isButton, .isSelected] : .isButton)
        .accessibilityIdentifier(current ? "setlist.row.\(list.id).current" : "setlist.row.\(list.id)")
        .accessibilityActions {
            // The reorder control is a drag; VoiceOver and Switch Control reorder
            // through named actions instead, one row at a time.
            ForEach(model.moveActionNames(list), id: \.self) { name in
                Button(name) { model.move(list, by: name == "Move up" ? -1 : 1) }
            }
        }
    }

    /// The actions a row offers, headed by the list they act on.
    @ViewBuilder
    private func rowMenu(_ list: DPSongList) -> some View {
        Section(model.displayName(list)) {
            Button("Rename set list…", systemImage: "pencil") { model.promptRename(list) }
            Button("Duplicate set list", systemImage: "plus.square.on.square") { model.duplicate(list) }
            if !model.isHome(list) {
                Button("Delete set list…", systemImage: "trash", role: .destructive) { model.confirmDelete(list) }
            }
        }
    }
}

/// One manage row: the list's display name, how many songs it holds, the
/// selector's lit indicator when it is the current list, and a "…" menu.
private enum SetListRowGlyph {
    /// The glyph at the size a system UIButton drew it in this row (18.5 pt).
    static let ellipsis = UIImage(systemName: "ellipsis.circle",
                                  withConfiguration: UIImage.SymbolConfiguration(pointSize: 18.5)) ?? UIImage()
}

private struct SetListRow<MenuContent: View>: View {
    private static var ellipsis: UIImage { SetListRowGlyph.ellipsis }

    let name: String
    let songCount: Int
    let isCurrent: Bool
    /// Carries a reorder control, which takes the trailing edge.
    let movable: Bool
    let menuIdentifier: String
    @ViewBuilder let menu: () -> MenuContent
    let select: () -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Button(action: select) {
                HStack(alignment: .center, spacing: 0) {
                    Text(name)
                        .font(Plate.text(20))
                        .foregroundStyle(Plate.ink)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Spacer(minLength: 16)
                    Text(SetListsModel.countLabel(songCount))
                        .font(Plate.mono(14))
                        .foregroundStyle(Plate.inkSecondary)
                        .fixedSize()
                }
                .padding(.leading, 26)
                .padding(.top, 14)
                .padding(.bottom, 15)
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(alignment: .leading) {
                    if isCurrent {
                        Circle()
                            .fill(Plate.lit)
                            .frame(width: SetListSelectorMetrics.dotDiameter, height: SetListSelectorMetrics.dotDiameter)
                            .padding(.leading, SetListSelectorMetrics.dotInset)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Menu(content: menu) {
                // Sized explicitly: a menu label would otherwise scale the glyph to its font.
                Image(uiImage: Self.ellipsis)
                    .renderingMode(.template)
                    .resizable()
                    .frame(width: Self.ellipsis.size.width, height: Self.ellipsis.size.height)
                    .foregroundStyle(Plate.inkSecondary)
                    .frame(width: 44, height: 44)
            }
            .menuIndicator(.hidden)
            .padding(.leading, 8)
            .padding(.trailing, movable ? 4 + 22.0 / 3.0 : 4)
            .accessibilityLabel("Actions for \(name)")
            .accessibilityIdentifier(menuIdentifier)
        }
    }
}
