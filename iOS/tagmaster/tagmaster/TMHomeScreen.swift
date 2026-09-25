//
//  TMHomeScreen.swift
//  tagmaster
//
//  Home: the three ways into the catalog (Browse, Random Tag, Open Tag), the
//  Lists group (Teachable Tags, the user's own lists, New list…) and the
//  Favorites, with the credits underneath. Edit reorders and removes
//  favorites and lists; a list's context menu renames and deletes it.
//

import SwiftUI
import UIKit

/// The Open Tag alert's field: digits only.
struct TMOpenTagPrompt: Identifiable, Equatable {
    let id = UUID()
    var text = ""

    /// Keeps only the digits, as the field's keyboard filter did.
    static func filter(_ text: String) -> String { text.filter(\.isASCIIDigit) }

    var tagId: Int? {
        guard let value = Int32(text), value > 0 else { return nil }
        return Int(value)
    }
}

private extension Character {
    var isASCIIDigit: Bool { ("0"..."9").contains(self) }
}

@MainActor
@Observable
final class TMHomeModel: TMTagListing {
    let store: TMTagStore
    let catalog: TMCatalog
    var navigator: TMNavigator?

    private(set) var favorites: [Int] = []
    private(set) var customKeys: [String] = []
    private(set) var listNames: [String: String] = [:]
    private(set) var listCounts: [String: Int] = [:]
    private(set) var teachableCount = 0

    var isEditing = false
    var isScrolling = false {
        didSet { if oldValue && !isScrolling { applyPendingRefresh() } }
    }
    private(set) var pendingRefresh = false
    /// True while Random Tag is choosing; the row shows its own progress.
    private(set) var randomBusy = false

    var recovery: TMRecovery?
    var namePrompt: TMNamePrompt?
    var deletePrompt: TMDeletePrompt?
    var openTagPrompt: TMOpenTagPrompt?

    private(set) var selectedTagId: Int?
    private(set) var expanded = false
    private(set) var scrollTarget: Int?

    @ObservationIgnored private var observers: [NSObjectProtocol] = []

    init(store: TMTagStore = .shared, catalog: TMCatalog = .live, navigator: TMNavigator? = nil,
         center: NotificationCenter = .default) {
        self.store = store
        self.catalog = catalog
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

    /// The destination rows, top to bottom.
    static let navigationTitles = ["Browse", "Random Tag", "Open Tag"]

    /// Edit only has work to do when there are favorites or lists to reorder or remove.
    var canEdit: Bool { !favorites.isEmpty || !customKeys.isEmpty }

    static let noFavoritesFooter = "No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite."

    static func countLabel(_ count: Int) -> String { count == 1 ? "1 tag" : "\(count) tags" }

    // MARK: - Reading

    func reload() {
        pendingRefresh = false
        favorites = TMTagLists.ids(for: TMTagLists.favoriteKey)
        teachableCount = TMTagLists.ids(for: TMTagLists.teachableKey).count
        customKeys = TMTagLists.customKeys()
        listNames = Dictionary(uniqueKeysWithValues: customKeys.map { ($0, TMTagLists.name(for: $0)) })
        listCounts = Dictionary(uniqueKeysWithValues: customKeys.map { ($0, TMTagLists.ids(for: $0).count) })
        if !canEdit && isEditing { isEditing = false }
        syncSelection()
    }

    /// A change from anywhere (this device or another). It waits while the user
    /// scrolls, then lands, so a rename or reorder from elsewhere is never lost.
    func userDataChanged() {
        guard !isScrolling else {
            pendingRefresh = true
            return
        }
        reload()
    }

    private func applyPendingRefresh() {
        if pendingRefresh { reload() }
    }

    func syncSelection() {
        expanded = navigator?.isExpandedSplit ?? false
        let current = expanded ? navigator?.currentSplitTagId : nil
        selectedTagId = current.flatMap { favorites.contains($0) ? $0 : nil }
    }

    // MARK: - Destinations

    func activate(_ title: String) {
        switch title {
        case "Browse": navigator?.show(.browse)
        case "Random Tag": randomTag()
        case "Open Tag": openTagPrompt = TMOpenTagPrompt()
        default: break
        }
    }

    func search() { navigator?.show(.search) }
    func settings() { navigator?.show(.settings) }
    func openTeachable() { navigator?.show(.teachable) }
    func openList(_ key: String) { navigator?.show(.list(key)) }

    func openTag(_ tagId: Int) {
        navigator?.showTag(tagId)
        syncSelection()
    }

    func commitOpenTag() {
        let prompt = openTagPrompt
        openTagPrompt = nil
        if let tagId = prompt?.tagId { navigator?.showTag(tagId) }
    }

    // MARK: - Random Tag

    /// Picks one tag at random from everything matching the Random Tag filters.
    func randomTag() {
        guard !randomBusy else { return }
        randomBusy = true
        let catalog = self.catalog
        let query = TMRandomTagFilters.query()
        Task.detached(priority: .userInitiated) {
            let outcome: Result<Int, TMRandomFailure>
            if let all = catalog.query(query, 0, 0) {
                if all.available <= 0 {
                    outcome = .failure(.nothingMatches)
                } else {
                    let chosen = Int(arc4random_uniform(UInt32(all.available)))
                    let picked = catalog.query(query, 1, chosen)
                    if let tag = picked?.tags.first as? DPTag {
                        outcome = .success(Int(tag.tagId))
                    } else {
                        outcome = .failure(picked == nil ? .unreachable : .nothingMatches)
                    }
                }
            } else {
                outcome = .failure(.unreachable)
            }
            await MainActor.run { self.finishRandomTag(outcome) }
        }
    }

    enum TMRandomFailure: Error { case nothingMatches, unreachable }

    private func finishRandomTag(_ outcome: Result<Int, TMRandomFailure>) {
        randomBusy = false
        switch outcome {
        case .success(let tagId):
            navigator?.showTag(tagId)
        case .failure(.nothingMatches):
            recovery = TMRecovery(message: "No tag could be selected. Check your connection or adjust Random Tag Filters in Settings, then try again.") { [weak self] in self?.randomTag() }
        case .failure(.unreachable):
            recovery = TMRecovery(message: "A random tag couldn't be loaded. Check your connection and try again.") { [weak self] in self?.randomTag() }
        }
    }

    // MARK: - Lists

    func newList() { namePrompt = .create() }

    func commitNewList(_ name: String) {
        _ = TMTagLists.createList(named: name)
        reload()
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    func rename(_ key: String) { namePrompt = .rename(key) }

    func commitRename(_ key: String, _ name: String) {
        TMTagLists.renameList(key, to: name)
        reload()
    }

    /// Deleting a list is never silent: it asks first.
    func confirmDelete(_ key: String) { deletePrompt = TMDeletePrompt(key: key) }

    func deleteList(_ key: String) {
        TMTagLists.deleteList(key)
        reload()
        UIAccessibility.post(notification: .layoutChanged, argument: nil)
    }

    /// SwiftUI's move within the custom lists: `destination` is the gap they are dropped into.
    func moveList(from offsets: IndexSet, to destination: Int) {
        guard let from = offsets.first, offsets.count == 1 else { return }
        let to = destination > from ? destination - 1 : destination
        TMTagLists.moveList(from: from, to: to)
        reload()
    }

    // MARK: - Favorites

    func removeFavorites(at offsets: IndexSet) {
        for index in offsets.sorted(by: >) where favorites.indices.contains(index) {
            DPAppDelegate.removeFavorite(Int32(favorites[index]))
        }
        reload()
    }

    func moveFavorite(from offsets: IndexSet, to destination: Int) {
        guard let from = offsets.first, offsets.count == 1 else { return }
        let to = destination > from ? destination - 1 : destination
        guard from != to else { return }
        DPAppDelegate.moveFavorite(at: UInt(from), to: UInt(to))
        reload()
    }

    // MARK: - TMTagListing

    var listedTagIds: [Int] { favorites }

    func didStep(to tagId: Int) {
        guard favorites.contains(tagId) else { return }
        selectedTagId = tagId
        scrollTarget = tagId
    }
}

struct TMHomeScreen: View {
    @Bindable var model: TMHomeModel
    @Environment(\.tmTintDimmed) private var dimmed

    var body: some View {
        ScrollViewReader { proxy in
            List {
                Section {
                    ForEach(TMHomeModel.navigationTitles, id: \.self) { title in
                        navigationRow(title)
                    }
                }
                Section {
                    listsRows
                } header: {
                    TMSectionHeader("Lists")
                }
                Section {
                    ForEach(TMListedTag.keyed(model.favorites)) { listed in
                        let tagId = listed.tagId
                        let selected = model.selectedTagId == tagId
                        Button { model.openTag(tagId) } label: {
                            TMTagRow(content: TMTagRowContent(tagId: tagId, tag: model.store.tag(tagId)),
                                     loading: model.store.isLoading(tagId),
                                     showsChevron: !model.expanded && !model.isEditing,
                                     selected: selected)
                        }
                        .tmTagRowAccessibility(TMTagRowContent(tagId: tagId, tag: model.store.tag(tagId)), selected: selected)
                        .tmTagListRow(selected: selected, groupedInset: tagId != model.favorites.last)
                        .tmScrollTarget(listed)
                    }
                    .onDelete { model.removeFavorites(at: $0) }
                    .onMove { model.moveFavorite(from: $0, to: $1) }
                } header: {
                    // A section with no rows sets its header higher than UITableView did.
                    TMSectionHeader("Favorites").padding(.top, model.favorites.isEmpty ? 7 : 0)
                } footer: {
                    if model.favorites.isEmpty { TMSectionFooter(TMHomeModel.noFavoritesFooter).offset(y: -1.0 / 3) }
                }
                Section {
                    TMHomeCredits(afterRows: !model.favorites.isEmpty, open: { model.navigator?.openURL($0) })
                }
                // Under the explanation the credits follow as closely as a table footer did.
                .listSectionSpacing(model.favorites.isEmpty ? 1.0 / 3 : 15)
            }
            .listStyle(.grouped)
            .listSectionSpacing(.custom(15))
            // UITableView ended Home with its footer view. A grouped List leaves more
            // after it: 20pt of margin and 32pt of closing section space on iPhone;
            // in the iPad sidebar the table itself left 8pt more than the List.
            .contentMargins(.bottom, UIDevice.current.userInterfaceIdiom == .pad ? 8 : -32, for: .scrollContent)
            .scrollContentBackground(.hidden)
            .environment(\.editMode, .constant(model.isEditing ? .active : .inactive))
            .tmTracksScrolling($model.isScrolling)
            .onChange(of: model.scrollTarget) { _, target in
                guard let target else { return }
                withAnimation(UIAccessibility.isReduceMotionEnabled ? nil : .default) { proxy.scrollTo(target) }
            }
        }
        .background { TMScreenBackground() }
        .tmNamePrompt($model.namePrompt) { prompt, name in
            if case .rename(let key) = prompt.purpose { model.commitRename(key, name) } else { model.commitNewList(name) }
        }
        .tmDeletePrompt($model.deletePrompt) { model.deleteList($0) }
        .tmRecoveryAlert($model.recovery)
        .tmOpenTagPrompt($model.openTagPrompt) { model.commitOpenTag() }
    }

    private func navigationRow(_ title: String) -> some View {
        let loading = title == "Random Tag" && model.randomBusy
        return Button { if !loading { model.activate(title) } } label: {
            HStack(spacing: 0) {
                Text(title)
                    .tmFont(.body)
                    .foregroundStyle(loading ? Color(uiColor: .secondaryLabel) : Color(uiColor: .label))
                Spacer(minLength: 8)
                if loading {
                    TMBarberPole(compact: true).accessibilityHidden(true)
                } else if !model.isEditing {
                    // A table in edit mode hid every row's accessory.
                    TMDisclosureChevron()
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityLabel(loading ? "Random Tag, loading" : title)
        .tmTextRow(groupedInset: title != TMHomeModel.navigationTitles.last)
        .deleteDisabled(true)
        .moveDisabled(true)
    }

    @ViewBuilder
    private var listsRows: some View {
        Button { model.openTeachable() } label: {
            TMListCountRow(title: "Teachable Tags", count: model.teachableCount, showsChevron: !model.isEditing)
        }
        .accessibilityIdentifier("home.lists.teachable")
        .tmTextRow()
        .deleteDisabled(true)
        .moveDisabled(true)
        ForEach(model.customKeys, id: \.self) { key in
            Button { model.openList(key) } label: {
                TMListCountRow(title: model.listNames[key] ?? key, count: model.listCounts[key] ?? 0,
                               showsChevron: !model.isEditing)
            }
            .accessibilityIdentifier("home.list.\(key)")
            .tmTextRow()
            .contextMenu {
                Button { model.rename(key) } label: { Label("Rename…", systemImage: "pencil") }
                Button(role: .destructive) { model.confirmDelete(key) } label: { Label("Delete…", systemImage: "trash") }
            }
        }
        .onDelete { offsets in
            // Asks first; the row stays until the confirmation is answered.
            if let index = offsets.first, model.customKeys.indices.contains(index) {
                model.confirmDelete(model.customKeys[index])
            }
        }
        .onMove { model.moveList(from: $0, to: $1) }
        Button { model.newList() } label: {
            HStack(spacing: 0) {
                Image(systemName: "plus.circle")
                    .foregroundStyle(TMTheme.tint(DPAppDelegate.accentColor(), dimmed: dimmed))
                    .frame(width: 24)
                    .padding(.trailing, 15)
                Text("New list…").tmFont(.body).foregroundStyle(Color(uiColor: .label))
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .accessibilityIdentifier("home.lists.new")
        .tmTextRow(groupedInset: false)
        .deleteDisabled(true)
        .moveDisabled(true)
    }
}

extension View {
    /// UITableView's inset-grouped spacing above the first section.
    func tmInsetGroupedMetrics() -> some View {
        contentMargins(.top, 15, for: .scrollContent)
            // UITableView ends an inset-grouped table 30pt below its last section (SwiftUI: 20).
            .contentMargins(.bottom, 30, for: .scrollContent)
            .listSectionSpacing(.custom(16.0 / 3))
    }
}

/// A grouped section's title, on UITableView's margins.
struct TMSectionHeader: View {
    let title: String
    init(_ title: String) { self.title = title }

    var body: some View {
        Text(title).padding(.leading, 4).offset(y: 4)
    }
}

/// A grouped section's explanation, on UITableView's margins.
struct TMSectionFooter: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text).padding(.horizontal, 4)
    }
}

/// A list row with its size on the trailing side, as a value1 cell showed it.
struct TMListCountRow: View {
    let title: String
    let count: Int
    var showsChevron = true

    var body: some View {
        HStack(spacing: 0) {
            Text(title).tmFont(.body).foregroundStyle(Color(uiColor: .label))
            Spacer(minLength: 8)
            Text(TMHomeModel.countLabel(count))
                .tmFont(.body)
                .foregroundStyle(Color(uiColor: .secondaryLabel))
            if showsChevron { TMDisclosureChevron().padding(.leading, 8) }
        }
        .contentShape(Rectangle())
    }
}

/// The links under Home: attribution, then developer, terms and donation.
struct TMHomeCredits: View {
    @Environment(\.tmTintDimmed) private var dimmed
    /// Under favourite rows (rather than the empty explanation) UIKit left 2pt more.
    var afterRows = false
    let open: (URL) -> Void

    private var year: Int { Calendar.current.component(.year, from: Date()) }

    var body: some View {
        VStack(spacing: 0) {
            link("Content provided by BarbershopTags.com", "https://www.barbershoptags.com", "home.credit.attribution")
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 4) {
                    developer
                    HStack(spacing: 4) { terms; donate }
                }
                VStack(spacing: 4) {
                    developer
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 4) { terms; donate }
                        VStack(spacing: 4) { terms; donate }
                    }
                }
            }
        }
        .padding(EdgeInsets(top: afterRows ? 10 : 8, leading: 16, bottom: 16, trailing: 16))
        .frame(maxWidth: .infinity)
        .listRowInsets(EdgeInsets())
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listSectionSeparator(.hidden)
        .deleteDisabled(true)
        .moveDisabled(true)
    }

    private var developer: some View { link("DepollSoft © \(String(year))", "https://apps.depoll.com", "home.credit.developer") }
    private var terms: some View { link("Terms of Use", "https://apps.depoll.com/terms-of-use", "home.credit.terms") }
    private var donate: some View {
        link("Donate", "https://www.davidpoll.com/applications/tag-master/donate", "home.credit.donate")
    }

    private func link(_ title: String, _ url: String, _ identifier: String) -> some View {
        Button { open(URL(string: url)!) } label: {
            Text(title)
                .tmFont(.footnote)
                .multilineTextAlignment(.center)
                .foregroundStyle(TMTheme.tint(DPAppDelegate.accentColor(), dimmed: dimmed))
                .padding(4)
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
        .accessibilityAddTraits(.isLink)
        .accessibilityIdentifier(identifier)
    }
}

extension View {
    /// The Open Tag alert: a digits-only field, Cancel and Open.
    func tmOpenTagPrompt(_ prompt: Binding<TMOpenTagPrompt?>, open: @escaping () -> Void) -> some View {
        alert("Open Tag",
              isPresented: Binding(get: { prompt.wrappedValue != nil },
                                   set: { if !$0 { prompt.wrappedValue = nil } }),
              presenting: prompt.wrappedValue) { _ in
            TextField("", text: Binding(get: { prompt.wrappedValue?.text ?? "" },
                                        set: { prompt.wrappedValue?.text = TMOpenTagPrompt.filter($0) }))
                .keyboardType(.decimalPad)
                .submitLabel(.go)
            Button("Cancel", role: .cancel) { prompt.wrappedValue = nil }
            Button("Open", action: open)
        } message: { _ in
            Text("Enter Tag ID")
        }
    }
}

