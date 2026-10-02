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
        PitchPerfectUsage.songsAddedToSetList()
        return selected.count
    }

    func sectionTitle(_ group: DPAddableSongs) -> String { store.displayName(for: group.list) }
}

/// The checklist itself, hosted by `AddSongsController`.
struct AddSongsList: View {
    let model: AddSongsModel

    var body: some View {
        List {
            ForEach(model.groups, id: \.list.id) { group in
                Section {
                    ForEach(group.songs, id: \.rowID) { song in
                        row(song)
                    }
                } header: {
                    // UIKit's engraved header: the list's own name, 14 pt above, 6 below.
                    PlateHeader(model.sectionTitle(group), uppercased: false)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 14)
                        .padding(.bottom, 6)
                        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                }
            }
        }
        .plateList(fullScreen: true)
        .environment(\.defaultMinListHeaderHeight, 0)
        .instrumentChrome()
        .analyticsScreen("add_songs")
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
                    // Where UIKit's checkmark accessory sat: the content view
                    // shrank for it, 26 pt past the key and 7 px further in.
                    Image(systemName: "checkmark")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(Color(uiColor: .systemBlue))
                        .padding(.leading, 26)
                        .padding(.trailing, 7.0 / 3.0)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 14)
            .padding(.bottom, 15)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .plateRow(margin: 20)
        // The UIKit table's own separator colour, not the system default.
        .listRowSeparatorTint(Plate.hairline)
        .accessibilityLabel("\(song.name ?? ""), \(song.key?.friendlyName() ?? "")")
        .accessibilityAddTraits(chosen ? [.isButton, .isSelected] : .isButton)
    }
}

/// Add songs as the UIKit screen was built: its own navigation controller with
/// UIKit bar items (Close and Select all as separate items on the left, the
/// `.done` Add on the right) over the SwiftUI checklist.
final class AddSongsController: UIHostingController<AddSongsList> {
    let model: AddSongsModel
    var onFinish: () -> Void = {}
    private lazy var selectAllItem: UIBarButtonItem = {
        let item = UIBarButtonItem(title: "Select all", style: .plain, target: self, action: #selector(toggleSelectAll))
        item.accessibilityIdentifier = "setlist.addSongs.selectAll"
        return item
    }()
    private lazy var addItem: UIBarButtonItem = {
        let item = UIBarButtonItem(title: "Add", style: .done, target: self, action: #selector(confirm))
        item.accessibilityIdentifier = "setlist.addSongs.confirm"
        return item
    }()

    init(model: AddSongsModel) {
        self.model = model
        super.init(rootView: AddSongsList(model: model))
        navigationItem.title = "Add songs"
        navigationItem.leftBarButtonItems = [
            BarSymbol.item(systemName: "xmark", target: self, action: #selector(close)),
            selectAllItem,
        ]
        navigationItem.rightBarButtonItem = addItem
        refreshItems()
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    /// Follows the model's choices, as the UIKit screen refreshed its items on every tick.
    private func refreshItems() {
        withObservationTracking {
            selectAllItem.isEnabled = model.canSelectAll
            selectAllItem.title = model.selectAllTitle
            addItem.isEnabled = model.canConfirm
            addItem.title = model.confirmTitle
        } onChange: { [weak self] in
            DispatchQueue.main.async { self?.refreshItems() }
        }
    }

    @objc private func toggleSelectAll() { model.toggleSelectAll() }
    @objc private func close() { finish() }

    @objc private func confirm() {
        guard model.confirm() > 0 else { return }
        finish()
    }

    private func finish() {
        let done = onFinish
        if let presenting = presentingViewController {
            presenting.dismiss(animated: true, completion: done)
        } else {
            done()
        }
    }

    /// The picker as it is presented: a sheet (medium or large) on iPhone, a form sheet on iPad.
    func embeddedInNavigation() -> UINavigationController {
        let navigation = UINavigationController(rootViewController: self)
        if UIDevice.current.userInterfaceIdiom == .pad {
            navigation.modalPresentationStyle = .formSheet
        } else {
            navigation.modalPresentationStyle = .pageSheet
            navigation.sheetPresentationController?.detents = [.medium(), .large()]
        }
        return navigation
    }
}

/// Presents Add songs from the Songs screen while `isPresented` is true.
struct AddSongsPresenter: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let target: DPSongList

    final class Presenter: UIViewController, UIAdaptivePresentationControllerDelegate {
        weak var shown: UINavigationController?
        var close: () -> Void = {}

        func sync(isPresented: Bool, target: DPSongList) {
            if isPresented, shown == nil {
                let controller = AddSongsController(model: AddSongsModel(target: target))
                controller.onFinish = { [weak self] in self?.close() }
                let navigation = controller.embeddedInNavigation()
                shown = navigation
                presentOnTop(navigation, stillWanted: { [weak self] in self?.shown === navigation }) { [weak self] in
                    navigation.presentationController?.delegate = self
                }
            } else if !isPresented, let navigation = shown {
                shown = nil
                if navigation.presentingViewController != nil, !navigation.isBeingDismissed {
                    navigation.dismiss(animated: true)
                }
            }
        }

        /// A swipe down closes the picker without adding.
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            shown = nil
            close()
        }
    }

    func makeUIViewController(context: Context) -> Presenter {
        let presenter = Presenter()
        presenter.view.isHidden = true
        return presenter
    }

    func updateUIViewController(_ presenter: Presenter, context: Context) {
        presenter.close = { isPresented = false }
        presenter.sync(isPresented: isPresented, target: target)
    }
}
