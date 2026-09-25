//
//  AddSongsScreen.swift
//  pitchperfect
//
//  "Add songs": the checklist of every other set list's songs the current list
//  does not already have, one section per source list in list order. Confirming
//  appends deep copies with fresh song ids, in their source order.
//

import SwiftUI
import UIKit

@Observable
@MainActor
final class AddSongsModel {
    let store: DPSongsModel
    let target: DPSongList
    let groups: [DPAddableSongs]
    private(set) var chosen: Set<ObjectIdentifier> = []

    init(target: DPSongList, store: DPSongsModel = .sharedInstance) {
        self.store = store
        self.target = target
        groups = store.addableSongs(for: target)
    }

    private var allSongs: [DPPitchedSong] { groups.flatMap(\.songs) }
    var total: Int { allSongs.count }

    func isChosen(_ song: DPPitchedSong) -> Bool { chosen.contains(song.rowID) }

    func toggle(_ song: DPPitchedSong) {
        if chosen.contains(song.rowID) { chosen.remove(song.rowID) } else { chosen.insert(song.rowID) }
    }

    var canSelectAll: Bool { total > 0 }
    var selectAllTitle: String { total > 0 && chosen.count == total ? "Clear" : "Select all" }

    /// Ticks every offered song, or clears the ticks once they are all in.
    func toggleSelectAll() {
        let all = allSongs.map(\.rowID)
        guard !all.isEmpty else { return }
        chosen = chosen.count == all.count ? [] : Set(all)
    }

    var canConfirm: Bool { !chosen.isEmpty }

    var confirmTitle: String {
        switch chosen.count {
        case 0: return "Add"
        case 1: return "Add 1 song"
        default: return "Add \(chosen.count) songs"
        }
    }

    /// Appends deep copies to the target list, in their source order; returns how many.
    @discardableResult
    func confirm() -> Int {
        let selected = allSongs.filter { chosen.contains($0.rowID) }
        guard !selected.isEmpty else { return 0 }
        store.copySongs(selected, to: target)
        return selected.count
    }

    func sectionTitle(_ group: DPAddableSongs) -> String { store.displayName(for: group.list) }
}

struct AddSongsScreen: View {
    @StateObject private var box: ModelBox<AddSongsModel>
    private var model: AddSongsModel { box.model }
    @Environment(\.dismiss) private var dismiss

    init(target: DPSongList) {
        _box = StateObject(wrappedValue: ModelBox(AddSongsModel(target: target)))
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(model.groups, id: \.list.id) { group in
                    Section {
                        ForEach(group.songs, id: \.rowID) { song in
                            row(song)
                        }
                    } header: {
                        PlateHeader(model.sectionTitle(group))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.top, 26.0 / 3.0)
                            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                    }
                }
            }
            .plateList(fullScreen: true)
            .navigationTitle("Add songs")
            .navigationBarTitleDisplayMode(.inline)
            .instrumentChrome()
            .toolbar {
                ToolbarItemGroup(placement: .topBarLeading) {
                    BarSymbolButton(systemName: "xmark") { dismiss() }
                    Button(model.selectAllTitle) { model.toggleSelectAll() }
                        .disabled(!model.canSelectAll)
                        .accessibilityIdentifier("setlist.addSongs.selectAll")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(model.confirmTitle) {
                        model.confirm()
                        dismiss()
                    }
                    .modifier(ProminentDone())
                    .disabled(!model.canConfirm)
                    .accessibilityIdentifier("setlist.addSongs.confirm")
                }
            }
        }
        .presentationDetents(UIDevice.current.userInterfaceIdiom == .pad ? [.large] : [.medium, .large])
        .presentationDragIndicator(.hidden)
    }

    private func row(_ song: DPPitchedSong) -> some View {
        let chosen = model.isChosen(song)
        return Button { model.toggle(song) } label: {
            HStack(alignment: .center, spacing: 0) {
                Text(song.name ?? "")
                    .font(Plate.text(20))
                    .foregroundStyle(Plate.ink)
                    .lineLimit(1)
                Spacer(minLength: 16)
                KeyReadout(key: song.key, color: Plate.inkSecondary)
                    .fixedSize()
                if chosen {
                    Image(systemName: "checkmark")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color(uiColor: .systemBlue))
                        .padding(.leading, 12)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 14)
            .padding(.bottom, 15)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .plateRow()
        .accessibilityLabel("\(song.name ?? ""), \(song.key?.friendlyName() ?? "")")
        .accessibilityAddTraits(chosen ? [.isButton, .isSelected] : .isButton)
    }
}

/// UIKit's `.done` bar item: the prominent, tinted confirm button.
private struct ProminentDone: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.buttonStyle(.glassProminent).tint(Color(uiColor: .systemBlue))
        } else {
            content.fontWeight(.semibold)
        }
    }
}
