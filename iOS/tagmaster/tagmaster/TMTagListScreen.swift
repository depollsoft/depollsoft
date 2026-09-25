//
//  TMTagListScreen.swift
//  tagmaster
//
//  One list of tags: Teachable Tags or a list the user made. Rows open their
//  tag; Edit reorders and removes; a custom list's overflow menu renames and
//  deletes the list itself. A list deleted on another device takes its screen
//  with it rather than leaving a stale title over an empty table.
//

import SwiftUI
import UIKit

@MainActor
@Observable
final class TMTagListModel: TMTagListing {
    enum Kind: Equatable {
        case teachable
        case custom(String)
    }

    let kind: Kind
    let store: TMTagStore
    var navigator: TMNavigator?

    private(set) var ids: [Int] = []
    private(set) var name = ""
    var isEditing = false
    var namePrompt: TMNamePrompt?
    var deletePrompt: TMDeletePrompt?
    /// The tag open beside the list in an expanded split, whose row stays lit.
    private(set) var selectedTagId: Int?
    private(set) var expanded = false
    /// Set by the view while the user scrolls; a change waits until it is false.
    var isScrolling = false {
        didSet { if oldValue && !isScrolling { applyPendingRefresh() } }
    }
    /// A change that arrived mid-scroll and still has to be shown.
    private(set) var pendingRefresh = false
    /// Asks the view to bring a row into sight (the detail stepped to it).
    private(set) var scrollTarget: Int?

    @ObservationIgnored private var observers: [NSObjectProtocol] = []

    init(kind: Kind, store: TMTagStore = .shared, navigator: TMNavigator? = nil,
         center: NotificationCenter = .default) {
        self.kind = kind
        self.store = store
        self.navigator = navigator
        reload()
        observers.append(center.addObserver(forName: .userDataChanged, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.userDataChanged() }
        })
        observers.append(center.addObserver(forName: .TMTagSelectionDidChange, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.syncSelection() }
        })
    }

    deinit {
        for observer in observers { NotificationCenter.default.removeObserver(observer) }
    }

    var key: String {
        switch kind {
        case .teachable: return TMTagLists.teachableKey
        case .custom(let key): return key
        }
    }

    var isCustom: Bool { kind != .teachable }
    var title: String { isCustom ? name : "Teachable Tags" }
    var backTitle: String { isCustom ? "List" : "Teachable" }
    var canEdit: Bool { !ids.isEmpty }

    var emptyTitle: String { isCustom ? "No tags in \(name) yet." : "No teachable tags yet" }
    var emptyGuidance: String {
        isCustom
            ? "Open any tag and choose Add to list."
            : "Open a tag, choose Favorite and Teachable options, then Mark as Teachable. Your teaching list will appear here."
    }
    var emptyIdentifier: String? { isCustom ? "list.empty.title" : nil }
    var browseIdentifier: String { isCustom ? "list.browse" : "teachable.browse" }

    // MARK: - Reading

    private func reload() {
        pendingRefresh = false
        ids = TMTagLists.ids(for: key)
        name = TMTagLists.name(for: key)
        syncSelection()
    }

    func userDataChanged() {
        if case .custom(let key) = kind, !TMTagLists.customKeys().contains(key) {
            // Gone from another device (or just deleted here): nothing left to show.
            navigator?.removeScreen()
            return
        }
        name = TMTagLists.name(for: key)
        // Replacing the rows mid-scroll would cancel the gesture; the change waits.
        guard !isScrolling else {
            pendingRefresh = true
            return
        }
        reload()
    }

    private func applyPendingRefresh() {
        if pendingRefresh { reload() }
    }

    /// Re-reads the split: chevrons go beside an open tag and its row stays lit.
    func syncSelection() {
        expanded = navigator?.isExpandedSplit ?? false
        let current = expanded ? navigator?.currentSplitTagId : nil
        selectedTagId = current.flatMap { ids.contains($0) ? $0 : nil }
    }

    // MARK: - Acting

    func open(_ tagId: Int) {
        navigator?.showTag(tagId)
        syncSelection()
    }

    func remove(at offsets: IndexSet) {
        for index in offsets.sorted(by: >) where ids.indices.contains(index) {
            TMTagLists.remove(ids[index], from: key)
        }
        reload()
    }

    /// SwiftUI's move: `destination` is the gap the rows are dropped into.
    func move(from offsets: IndexSet, to destination: Int) {
        guard let from = offsets.first, offsets.count == 1 else { return }
        let to = destination > from ? destination - 1 : destination
        TMTagLists.move(in: key, from: from, to: to)
        reload()
    }

    func browse() { navigator?.show(.browse) }

    func promptRename() {
        guard isCustom else { return }
        namePrompt = .rename(key)
    }

    func commitRename(_ name: String) {
        TMTagLists.renameList(key, to: name)
    }

    func confirmDelete() {
        guard isCustom else { return }
        deletePrompt = TMDeletePrompt(key: key)
    }

    func deleteList() {
        TMTagLists.deleteList(key)
        navigator?.removeScreen()
    }

    // MARK: - TMTagListing

    var listedTagIds: [Int] { ids }

    func didStep(to tagId: Int) {
        guard ids.contains(tagId) else { return }
        selectedTagId = tagId
        scrollTarget = tagId
    }
}

struct TMTagListScreen: View {
    @Bindable var model: TMTagListModel

    var body: some View {
        ScrollViewReader { proxy in
            List {
                if model.ids.isEmpty {
                    TMEmptyListHeader(title: model.emptyTitle,
                                      guidance: model.emptyGuidance,
                                      titleIdentifier: model.emptyIdentifier,
                                      browseIdentifier: model.browseIdentifier,
                                      browse: model.browse)
                }
                ForEach(TMListedTag.keyed(model.ids)) { listed in
                    let tagId = listed.tagId
                    let selected = model.selectedTagId == tagId
                    Button { model.open(tagId) } label: {
                        TMTagRow(content: TMTagRowContent(tagId: tagId, tag: model.store.tag(tagId)),
                                 loading: model.store.isLoading(tagId),
                                 showsChevron: !model.expanded && !model.isEditing,
                                 selected: selected)
                    }
                    .tmTagRowAccessibility(TMTagRowContent(tagId: tagId, tag: model.store.tag(tagId)), selected: selected)
                    .tmTagListRow(selected: selected)
                    .tmScrollTarget(listed)
                }
                .onDelete { model.remove(at: $0) }
                .onMove { model.move(from: $0, to: $1) }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .environment(\.editMode, .constant(model.isEditing ? .active : .inactive))
            .tmTracksScrolling($model.isScrolling)
            .accessibilityIdentifier("list.table")
            .onChange(of: model.scrollTarget) { _, target in
                guard let target else { return }
                withAnimation(UIAccessibility.isReduceMotionEnabled ? nil : .default) { proxy.scrollTo(target) }
            }
        }
        .background { TMScreenBackground() }
        .tmNamePrompt($model.namePrompt) { _, name in model.commitRename(name) }
        .tmDeletePrompt($model.deletePrompt) { _ in model.deleteList() }
        .onAppear { model.syncSelection() }
    }
}

/// An empty list's explanation and its way out, as the table header used to be.
struct TMEmptyListHeader: View {
    let title: String
    let guidance: String
    var titleIdentifier: String?
    let browseIdentifier: String
    let browse: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .tmFont(.title2, bold: true)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier(titleIdentifier ?? "")
            Text(guidance)
                .tmFont(.body)
                .foregroundStyle(Color(uiColor: .secondaryLabel))
            Button("Browse Tags", action: browse)
                .tmFont(.body)
                .frame(minHeight: 44)
                .buttonStyle(.borderless)
                .accessibilityIdentifier(browseIdentifier)
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(32)
        .listRowInsets(EdgeInsets())
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
    }
}

extension View {
    /// A tag row in a plain list: full-bleed, clear unless it is the open tag.
    /// `groupedInset`: in a grouped list, separators stop 20 points short of the
    /// trailing edge too, except under a section's last row, where the section's
    /// full-width border is drawn instead.
    @ViewBuilder
    func tmTagListRow(selected: Bool, groupedInset: Bool = false) -> some View {
        let row = listRowInsets(EdgeInsets())
            .listRowBackground(selected ? TMTheme.selectionWash : Color.clear)
            .alignmentGuide(.listRowSeparatorLeading) { _ in 20 }
        if groupedInset {
            row.alignmentGuide(.listRowSeparatorTrailing) { $0.width - 20 }
        } else {
            row
        }
    }

    /// A text row on UITableViewCell's 20-point margins, over the page's watermark.
    @ViewBuilder
    func tmTextRow(groupedInset: Bool = true) -> some View {
        let row = listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            .listRowBackground(Color.clear)
        if groupedInset {
            row.alignmentGuide(.listRowSeparatorTrailing) { $0.width }
        } else {
            row
        }
    }

    /// Mirrors whether the user is scrolling into `isScrolling` (iOS 18 and later).
    @ViewBuilder
    func tmTracksScrolling(_ isScrolling: Binding<Bool>) -> some View {
        if #available(iOS 18.0, *) {
            onScrollPhaseChange { _, phase in
                isScrolling.wrappedValue = phase != .idle
            }
        } else {
            self
        }
    }
}
