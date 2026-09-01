import SwiftUI
import AVKit
import QuickLook
import FirebaseAuth

extension View {
    @ViewBuilder
    func tagMasterTabBarStyle() -> some View {
        if #available(iOS 18.0, *) {
            tabViewStyle(.tabBarOnly)
        } else {
            self
        }
    }
}

// MARK: - App state and deep links

@MainActor
final class TagMasterAppState: ObservableObject {
    static let shared = TagMasterAppState()
    @Published var pendingTagID: Int32?
    private init() {}
}

@MainActor
@objcMembers
public final class TagMasterDeepLinkRouter: NSObject {
    public static func openTag(withId tagId: Int32) {
        TagMasterAppState.shared.pendingTagID = tagId
    }
}

// MARK: - Testable query and filter values

struct TagFilterValues: Equatable {
    var learningTracksIndex = 0
    var sheetMusicIndex = 1
    var minimumRatingIndex = 2
    var minimumDownloadsIndex = 2

    var learningTracks: NSNumber? { Self.triState(learningTracksIndex) }
    var sheetMusic: NSNumber? { Self.triState(sheetMusicIndex) }
    var minimumRating: NSNumber? {
        minimumRatingIndex == 0 ? nil : NSNumber(value: minimumRatingIndex)
    }
    var minimumDownloads: NSNumber? {
        let values = [0, 50, 100, 500, 1000]
        guard values.indices.contains(minimumDownloadsIndex), minimumDownloadsIndex > 0 else { return nil }
        return NSNumber(value: values[minimumDownloadsIndex])
    }

    static func triState(_ index: Int) -> NSNumber? {
        switch index {
        case 1: return true
        case 2: return false
        default: return nil
        }
    }
}

struct TagSearchOptions: Equatable {
    var query = ""
    var sortIndex = 0
    var sheetMusicIndex = 0
    var learningTracksIndex = 0
    var partsIndex = 0
    var collectionIndex = 0

    var sort: DPTagSortOptions { DPTagSortOptions(rawValue: UInt32(sortIndex + 1)) }
    var sheetMusic: NSNumber? { TagFilterValues.triState(sheetMusicIndex) }
    var learningTracks: NSNumber? { TagFilterValues.triState(learningTracksIndex) }
    var parts: NSNumber? { partsIndex == 0 ? nil : NSNumber(value: partsIndex + 2) }
    var collection: DPTagCollection { DPTagCollection(rawValue: UInt32(collectionIndex)) }
}

// MARK: - Shared list state

@MainActor
final class TagListsManager: ObservableObject {
    @Published private(set) var favorites: [Int32] = []
    @Published private(set) var teachables: [Int32] = []
    private var observer: NSObjectProtocol?

    init(center: NotificationCenter = .default) {
        update()
        observer = center.addObserver(forName: .userDataChanged, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.update() }
        }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    func update() {
        favorites = DPAppDelegate.favorites().map { Int32($0) }
        teachables = DPAppDelegate.teachable().map { Int32($0) }
    }

    func deleteFavorites(at offsets: IndexSet) {
        var values = favorites.map(Int.init)
        offsets.sorted(by: >).forEach { values.remove(at: $0) }
        DPAppDelegate.setFavorites(values)
    }

    func moveFavorites(from source: IndexSet, to destination: Int) {
        var values = favorites.map(Int.init)
        values.move(fromOffsets: source, toOffset: destination)
        DPAppDelegate.setFavorites(values)
    }

    func deleteTeachables(at offsets: IndexSet) {
        var values = teachables.map(Int.init)
        offsets.sorted(by: >).forEach { values.remove(at: $0) }
        DPAppDelegate.setTeachable(values)
    }

    func moveTeachables(from source: IndexSet, to destination: Int) {
        var values = teachables.map(Int.init)
        values.move(fromOffsets: source, toOffset: destination)
        DPAppDelegate.setTeachable(values)
    }
}

// MARK: - Home

struct MainAppView: View {
    static func copyrightText(for year: Int) -> String { "Depollsoft © \(year)" }
    @EnvironmentObject private var appState: TagMasterAppState
    @StateObject private var lists = TagListsManager()
    @State private var tagPath: [Int32] = []
    @State private var tagIDInput = ""
    @State private var showOpenTag = false
    @State private var isLoadingRandom = false
    @State private var randomError: String?

    var body: some View {
        NavigationStack(path: $tagPath) {
            List {
                Section("Main") {
                    NavigationLink("Browse", destination: BrowseView())
                    if !lists.teachables.isEmpty {
                        NavigationLink("Teachable Tags", destination: TeachableTagsView())
                    }
                    Button("Random Tag", action: openRandomTag)
                        .disabled(isLoadingRandom)
                    Button("Open Tag") { showOpenTag = true }
                    NavigationLink("Settings", destination: TagSettingsView())
                }

                Section("Favorites") {
                    if lists.favorites.isEmpty {
                        Text("Tags you mark as favorites will appear here.")
                            .foregroundColor(.secondary)
                    }
                    ForEach(lists.favorites, id: \.self) { tagID in
                        NavigationLink(value: tagID) {
                            TagCellView(tagID: tagID)
                        }
                    }
                    .onDelete(perform: lists.deleteFavorites)
                    .onMove(perform: lists.moveFavorites)
                }

                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Link(
                            Self.copyrightText(for: Calendar.current.component(.year, from: Date())),
                            destination: URL(string: "http://apps.depoll.com")!
                        )
                        Link("Content provided by BarbershopTags.com", destination: URL(string: "http://www.barbershoptags.com")!)
                        Link("Terms of Use", destination: URL(string: "http://apps.depoll.com/terms-of-use")!)
                        Link("Donate", destination: URL(string: "http://www.davidpoll.com/applications/tag-master/donate")!)
                    }
                    .font(.caption)
                }
            }
            .navigationDestination(for: Int32.self) { tagID in
                TagDetailHostView(tagID: tagID)
            }
            .overlay {
                if isLoadingRandom { ProgressView("Finding a tag...") }
            }
            .navigationTitle("Tag Master")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) { EditButton() }
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink(destination: SearchView()) {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Search")
                    .accessibilityIdentifier("Search")
                }
            }
            .alert("Open Tag", isPresented: $showOpenTag) {
                TextField("Enter Tag ID", text: $tagIDInput)
                    .keyboardType(.decimalPad)
                Button("Cancel", role: .cancel) { tagIDInput = "" }
                Button("Open") {
                    if let tagID = Int32(tagIDInput), tagID > 0 {
                        tagPath.append(tagID)
                    }
                    tagIDInput = ""
                }
                .disabled(Int32(tagIDInput).map { $0 <= 0 } ?? true)
            }
            .alert("Could Not Find a Random Tag", isPresented: Binding(
                get: { randomError != nil },
                set: { if !$0 { randomError = nil } }
            )) {
                Button("OK", role: .cancel) { randomError = nil }
            } message: {
                Text(randomError ?? "Please try again.")
            }
            .onChange(of: appState.pendingTagID) { _, value in consumeDeepLink(value) }
            .onAppear { consumeDeepLink(appState.pendingTagID) }
        }
    }


    private func consumeDeepLink(_ tagID: Int32?) {
        guard let tagID, tagID > 0 else { return }
        tagPath.append(tagID)
        appState.pendingTagID = nil
    }

    private func openRandomTag() {
        guard !isLoadingRandom else { return }
        isLoadingRandom = true
        let defaults = UserDefaults.standard
        let filters = TagFilterValues(
            learningTracksIndex: defaults.integer(forKey: "random.learningTracks"),
            sheetMusicIndex: defaults.object(forKey: "random.sheetMusic") == nil ? 1 : defaults.integer(forKey: "random.sheetMusic"),
            minimumRatingIndex: defaults.object(forKey: "random.minRating") == nil ? 2 : defaults.integer(forKey: "random.minRating"),
            minimumDownloadsIndex: defaults.object(forKey: "random.minDownloads") == nil ? 2 : defaults.integer(forKey: "random.minDownloads")
        )
        DispatchQueue.global(qos: .userInitiated).async {
            let countResult = queryTags(
                query: nil, count: 0, start: 0, parts: nil,
                learningTracks: filters.learningTracks, sheetMusic: filters.sheetMusic,
                collection: DPTagCollection(rawValue: 0), sort: DPTagSortOptions(rawValue: 0),
                minimumRating: filters.minimumRating, minimumDownloads: filters.minimumDownloads,
                cache: false, fieldList: "id"
            )
            guard countResult.available > 0 else {
                DispatchQueue.main.async {
                    isLoadingRandom = false
                    randomError = "No tags match the current Random Tag filters."
                }
                return
            }
            let chosen = Int32.random(in: 0..<countResult.available)
            let result = queryTags(
                query: nil, count: 1, start: chosen, parts: nil,
                learningTracks: filters.learningTracks, sheetMusic: filters.sheetMusic,
                collection: DPTagCollection(rawValue: 0), sort: DPTagSortOptions(rawValue: 0),
                minimumRating: filters.minimumRating, minimumDownloads: filters.minimumDownloads,
                cache: false, fieldList: "id"
            )
            let tag = (result.tags as? [DPTag])?.first
            DispatchQueue.main.async {
                isLoadingRandom = false
                if let tag { tagPath.append(tag.tagId) }
                else { randomError = "The selected tag could not be loaded." }
            }
        }
    }
}

private func queryTags(
    query: String?, count: Int32, start: Int32, parts: NSNumber?,
    learningTracks: NSNumber?, sheetMusic: NSNumber?, collection: DPTagCollection,
    sort: DPTagSortOptions, minimumRating: NSNumber? = nil,
    minimumDownloads: NSNumber? = nil, cache: Bool = false,
    fieldList: String? = nil
) -> DPTagQueryResult {
    DPTag.query(
        query, numberOfResults: count, start: start, parts: parts,
        learningTracks: learningTracks, sheetMusic: sheetMusic,
        collection: collection, sortBy: sort, minimumRating: minimumRating,
        minimumDownloads: minimumDownloads, cache: cache, fieldList: fieldList
    )
}

// MARK: - Tag rows and query paging

struct TagCellView: View {
    let tagID: Int32
    @State private var tag: DPTag?
    @State private var didFail = false

    init(tagID: Int32, tag: DPTag? = nil) {
        self.tagID = tagID
        _tag = State(initialValue: tag)
    }

    var body: some View {
        Group {
            if let tag {
                VStack(alignment: .leading, spacing: 4) {
                    Text(tag.title ?? "Unknown")
                        .font(.headline)
                    if let alternative = tag.alternativeTitle, !alternative.isEmpty {
                        Text("a.k.a. \(alternative)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    Text(tagDetails(tag))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("Sheet music \(tag.sheetMusicUri == nil ? "unavailable" : "available"), learning tracks \(tag.tracks.isEmpty ? "unavailable" : "available")")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            } else if didFail {
                Button("Tag unavailable. Retry", action: load)
            } else {
                ProgressView().frame(maxWidth: .infinity)
            }
        }
        .padding(.vertical, 4)
        .frame(minHeight: 44)
        .onAppear { if tag == nil { load() } }
    }

    private func tagDetails(_ tag: DPTag) -> String {
        var values = ["ID: \(tag.tagId)"]
        if let posted = tag.posted { values.append("Posted: \(posted.formatted(date: .numeric, time: .omitted))") }
        if tag.rating != 0 { values.insert("Rating: \(String(format: "%.2f", tag.rating))", at: 0) }
        if tag.downloadCount != 0 { values.append("DLs: \(tag.downloadCount)") }
        return values.joined(separator: "  ")
    }

    private func load() {
        didFail = false
        if let cached = DPTag.load(fromCache: tagID) {
            tag = cached
            return
        }
        DispatchQueue.global(qos: .userInitiated).async {
            let loaded = DPTag.load(byId: tagID, refresh: false)
            DispatchQueue.main.async {
                tag = loaded
                didFail = loaded == nil
            }
        }
    }
}

@MainActor
final class TagQueryLoader: ObservableObject {
    typealias Query = (Int32) -> DPTagQueryResult?

    @Published private(set) var tags: [DPTag] = []
    @Published private(set) var isLoading = false
    @Published private(set) var statusText: String?
    @Published private(set) var hasMore = true

    private let query: Query
    private var nextStart: Int32 = 0
    private var generation = 0

    init(query: @escaping Query) { self.query = query }

    func reload() {
        generation += 1
        tags = []
        nextStart = 0
        hasMore = true
        statusText = nil
        isLoading = false
        loadMore()
    }

    func loadMore() {
        guard !isLoading, hasMore else { return }
        isLoading = true
        let start = nextStart
        let requestGeneration = generation
        DispatchQueue.global(qos: .userInitiated).async { [query] in
            let result = query(start)
            let loaded = result?.tags as? [DPTag] ?? []
            DispatchQueue.main.async {
                guard requestGeneration == self.generation else { return }
                self.isLoading = false
                guard let result else {
                    self.statusText = "An error occurred while loading tags."
                    return
                }
                self.tags.append(contentsOf: loaded)
                self.nextStart = result.start + result.count
                self.hasMore = self.nextStart < min(result.available, 1000)
                self.statusText = result.available == 0 ? "No tags could be found that matched your query." : nil
            }
        }
    }
}

struct TagQueryListView: View {
    @StateObject private var loader: TagQueryLoader

    init(query: @escaping TagQueryLoader.Query) {
        _loader = StateObject(wrappedValue: TagQueryLoader(query: query))
    }

    var body: some View {
        List {
            if let statusText = loader.statusText {
                Text(statusText)
                    .accessibilityIdentifier("QueryStatus")
            }
            ForEach(loader.tags, id: \.tagId) { tag in
                NavigationLink(destination: TagDetailHostView(tagID: tag.tagId)) {
                    TagCellView(tagID: tag.tagId, tag: tag)
                }
                .onAppear {
                    if tag.tagId == loader.tags.last?.tagId { loader.loadMore() }
                }
            }
            if loader.isLoading { ProgressView().frame(maxWidth: .infinity) }
        }
        .refreshable { loader.reload() }
        .onAppear { if loader.tags.isEmpty { loader.loadMore() } }
    }
}

// MARK: - Browse and search

struct BrowseView: View {
    var body: some View {
        TabView {
            browsePage(title: "Latest", icon: "History", sort: 2, collection: 0)
            browsePage(title: "Rating", icon: "Favorites", sort: 3, collection: 0)
            browsePage(title: "Downloads", icon: "Downloads", sort: 4, collection: 0)
            browsePage(title: "Classic", icon: "Bookmarks", sort: 5, collection: 1)
        }
        .tagMasterTabBarStyle()
        .navigationTitle("Browse")
    }

    private func browsePage(title: String, icon: String, sort: Int32, collection: Int32) -> some View {
        TagQueryListView { start in
            queryTags(
                query: nil, count: 20, start: start, parts: nil,
                learningTracks: nil, sheetMusic: nil,
                collection: DPTagCollection(rawValue: UInt32(collection)),
                sort: DPTagSortOptions(rawValue: UInt32(sort))
            )
        }
        .tabItem { Label(title, image: icon) }
        .accessibilityIdentifier("Browse\(title)")
    }
}

struct SearchView: View {
    @AppStorage("search.sortBy") private var sortIndex = 0
    @AppStorage("search.sheetMusic") private var sheetMusicIndex = 0
    @AppStorage("search.learningTracks") private var learningTracksIndex = 0
    @AppStorage("search.parts") private var partsIndex = 0
    @AppStorage("search.collection") private var collectionIndex = 0
    @State private var searchText = ""
    @State private var submittedOptions: TagSearchOptions?

    var body: some View {
        Form {
            Section("Search Options") {
                TextField("Search", text: $searchText)
                    .submitLabel(.search)
                    .onSubmit(runSearch)
                    .accessibilityIdentifier("SearchField")
                Picker("Sort By", selection: $sortIndex) {
                    Text("Title").tag(0)
                    Text("Downloads").tag(1)
                    Text("Recent").tag(2)
                    Text("Rating").tag(3)
                }
                triStatePicker("Sheet Music", selection: $sheetMusicIndex)
                triStatePicker("Tracks", selection: $learningTracksIndex)
                Picker("Parts", selection: $partsIndex) {
                    Text("Any").tag(0)
                    ForEach(3...8, id: \.self) { Text("\($0)").tag($0 - 2) }
                }
                Picker("Collection", selection: $collectionIndex) {
                    Text("Any").tag(0)
                    Text("Classic Tags").tag(1)
                    Text("Easy Tags").tag(2)
                }
            }
        }
        .navigationDestination(
            isPresented: Binding(
                get: { submittedOptions != nil },
                set: { if !$0 { submittedOptions = nil } }
            )
        ) {
            searchResults
        }
        .navigationTitle("Search")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Search", action: runSearch)
                    .accessibilityIdentifier("SearchButton")
            }
        }
    }

    @ViewBuilder
    private func triStatePicker(_ title: String, selection: Binding<Int>) -> some View {
        Picker(title, selection: selection) {
            Text("Not Important").tag(0)
            Text("Yes").tag(1)
            Text("No").tag(2)
        }
    }

    @ViewBuilder
    private var searchResults: some View {
        if let options = submittedOptions {
            TagQueryListView { start in
                queryTags(
                    query: options.query, count: 20, start: start, parts: options.parts,
                    learningTracks: options.learningTracks, sheetMusic: options.sheetMusic,
                    collection: options.collection, sort: options.sort
                )
            }
            .navigationTitle(options.query.isEmpty ? "Search Results" : options.query)
        }
    }

    private func runSearch() {
        submittedOptions = TagSearchOptions(
            query: searchText, sortIndex: sortIndex, sheetMusicIndex: sheetMusicIndex,
            learningTracksIndex: learningTracksIndex, partsIndex: partsIndex,
            collectionIndex: collectionIndex
        )
    }
}

struct TeachableTagsView: View {
    @StateObject private var lists = TagListsManager()

    var body: some View {
        List {
            if lists.teachables.isEmpty {
                Text("Tags marked as teachable will appear here.")
                    .foregroundColor(.secondary)
            }
            ForEach(lists.teachables, id: \.self) { tagID in
                NavigationLink(destination: TagDetailHostView(tagID: tagID)) {
                    TagCellView(tagID: tagID)
                }
            }
            .onDelete(perform: lists.deleteTeachables)
            .onMove(perform: lists.moveTeachables)
        }
        .navigationTitle("Teachable Tags")
        .toolbar { EditButton() }
    }
}

// MARK: - Settings and auth

struct TagSettingsView: View {
    @AppStorage("random.learningTracks") private var learningTracksIndex = 0
    @AppStorage("random.sheetMusic") private var sheetMusicIndex = 1
    @AppStorage("random.minRating") private var minimumRatingIndex = 2
    @AppStorage("random.minDownloads") private var minimumDownloadsIndex = 2
    @State private var isLoggedIn = Auth.auth().currentUser != nil
    @State private var showAuth = false
    @State private var clearAction: ClearAction?
    @State private var authError: String?

    enum ClearAction: String, Identifiable {
        case favorites = "favorites"
        case teachables = "teachable tags"
        var id: String { rawValue }
    }

    var body: some View {
        Form {
            Section("Log In") {
                Text("Log in to back up and synchronize your tag lists.")
                Button(isLoggedIn ? "Log Out" : "Log In", action: toggleLogin)
                    .accessibilityIdentifier("Login")
            }
            Section("Favorites") {
                Button("Clear Favorites", role: .destructive) { clearAction = .favorites }
            }
            Section("Teachable Tags") {
                Button("Clear Teachable Tags", role: .destructive) { clearAction = .teachables }
            }
            Section("Random Tag Filters") {
                Picker("Minimum Rating", selection: $minimumRatingIndex) {
                    Text("Any").tag(0)
                    ForEach(1...4, id: \.self) { Text("\($0)").tag($0) }
                }
                Picker("Minimum Downloads", selection: $minimumDownloadsIndex) {
                    Text("Any").tag(0)
                    Text("50").tag(1)
                    Text("100").tag(2)
                    Text("500").tag(3)
                    Text("1000").tag(4)
                }
                triStatePicker("Sheet Music", selection: $sheetMusicIndex)
                triStatePicker("Learning Tracks", selection: $learningTracksIndex)
            }
        }
        .navigationTitle("Settings")
        .background {
            if showAuth {
                TagMasterAuthView(
                    onAuthStateChanged: {
                        showAuth = false
                        refreshAuth()
                    },
                    onDismiss: { showAuth = false }
                )
            }
        }
        .confirmationDialog(
            "Clear \(clearAction?.rawValue ?? "list")?",
            isPresented: Binding(
                get: { clearAction != nil },
                set: { if !$0 { clearAction = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Clear", role: .destructive, action: clearSelectedList)
            Button("Cancel", role: .cancel) { clearAction = nil }
        }
        .alert("Authentication Error", isPresented: Binding(
            get: { authError != nil },
            set: { if !$0 { authError = nil } }
        )) {
            Button("OK", role: .cancel) { authError = nil }
        } message: {
            Text(authError ?? "Please try again.")
        }
        .onAppear(perform: refreshAuth)
    }

    @ViewBuilder
    private func triStatePicker(_ title: String, selection: Binding<Int>) -> some View {
        Picker(title, selection: selection) {
            Text("Not Important").tag(0)
            Text("Yes").tag(1)
            Text("No").tag(2)
        }
    }

    private func toggleLogin() {
        if isLoggedIn {
            do { try Auth.auth().signOut(); refreshAuth() }
            catch { authError = error.localizedDescription }
        } else {
            showAuth = true
        }
    }

    private func refreshAuth() { isLoggedIn = Auth.auth().currentUser != nil }

    private func clearSelectedList() {
        switch clearAction {
        case .favorites: DPAppDelegate.setFavorites([])
        case .teachables: DPAppDelegate.setTeachable([])
        case nil: break
        }
        clearAction = nil
    }
}
