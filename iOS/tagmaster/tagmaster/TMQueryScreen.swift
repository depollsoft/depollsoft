//
//  TMQueryScreen.swift
//  tagmaster
//
//  A page of catalog results: Browse's four pages and Search's results. Rows
//  load twenty at a time as the list nears its end; pull to refresh starts
//  over; an empty or failed query explains itself, and a failure offers Retry.
//

import SwiftUI
import UIKit

@MainActor
@Observable
final class TMQueryModel: TMTagListing {
    static let emptyMessage = "No tags could be found that matched your query."
    static let failureMessage = "Tags couldn't be loaded. Check your connection and try again."

    let query: TMTagQuery
    let catalog: TMCatalog
    var navigator: TMNavigator?
    /// The screen the detail knows this list by (its `source`); change notices name it.
    @ObservationIgnored weak var owner: AnyObject?
    /// Results fetched per page, and the most a query ever lists.
    let pageSize: Int
    let maxResults: Int

    private(set) var tags: [DPTag] = []
    private(set) var isLoading = false
    private(set) var hasMoreResults = true
    private(set) var failed = false
    /// Why there are no rows, shown in place of them.
    private(set) var statusText: String?
    private(set) var selectedTagId: Int?
    private(set) var expanded = false
    private(set) var scrollTarget: Int?
    /// Where the next page starts.
    private var nextStart = 0
    /// Refreshes finish their pull-to-refresh spinner by resuming these.
    @ObservationIgnored private var refreshWaiters: [CheckedContinuation<Void, Never>] = []
    @ObservationIgnored private var observer: NSObjectProtocol?

    init(query: TMTagQuery, catalog: TMCatalog = .live, navigator: TMNavigator? = nil,
         pageSize: Int = 20, maxResults: Int = 1000, preloaded: [DPTag]? = nil,
         center: NotificationCenter = .default) {
        self.query = query
        if let preloaded {
            tags = preloaded
            nextStart = preloaded.count
            hasMoreResults = false
        }
        self.catalog = catalog
        self.navigator = navigator
        self.pageSize = pageSize
        self.maxResults = maxResults
        observer = center.addObserver(forName: .TMTagSelectionDidChange, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.syncSelection() }
        }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    /// "Search Results" for an empty query, otherwise what was searched for.
    var title: String {
        let text = query.text ?? ""
        return text.isEmpty ? "Search Results" : text
    }

    // MARK: - Loading

    /// Loads the next page, unless one is on its way or there is nothing more.
    func fetchNextPage() {
        guard !isLoading, hasMoreResults else { return }
        isLoading = true
        let catalog = self.catalog, query = self.query, count = pageSize, start = nextStart
        Task.detached(priority: .userInitiated) {
            let result = catalog.query(query, count, start)
            await MainActor.run { self.finish(result) }
        }
    }

    private func finish(_ result: DPTagQueryResult?) {
        if let result {
            let page = result.tags.compactMap { $0 as? DPTag }
            tags += page
            nextStart = Int(result.start) + Int(result.count)
            hasMoreResults = nextStart < min(Int(result.available), maxResults)
            statusText = result.available == 0 ? TMQueryModel.emptyMessage : nil
            failed = false
        } else {
            statusText = TMQueryModel.failureMessage
            failed = true
            hasMoreResults = false
        }
        isLoading = false
        syncSelection()
        // A detail stepping through this list may have gained a neighbour.
        NotificationCenter.default.post(name: .TMTagListDidChange, object: owner ?? self)
        let waiters = refreshWaiters
        refreshWaiters = []
        waiters.forEach { $0.resume() }
    }

    /// Starts over from the first page (pull to refresh, Retry).
    func refresh() {
        guard !isLoading else { return }
        tags = []
        nextStart = 0
        hasMoreResults = true
        failed = false
        statusText = nil
        fetchNextPage()
    }

    /// Refreshes and returns once the first page has landed, for pull to refresh.
    func refreshAndWait() async {
        refresh()
        guard isLoading else { return }
        await withCheckedContinuation { refreshWaiters.append($0) }
    }

    /// The list asks for more once a row in its last eighth comes into view.
    func rowAppeared(_ index: Int) {
        if index >= tags.count * 7 / 8 - 1 { fetchNextPage() }
    }

    // MARK: - Selection

    func open(_ tag: DPTag) {
        navigator?.showTag(Int(tag.tagId))
        syncSelection()
    }

    func syncSelection() {
        expanded = navigator?.isExpandedSplit ?? false
        let current = expanded ? navigator?.currentSplitTagId : nil
        selectedTagId = current.flatMap { id in listedTagIds.contains(id) ? id : nil }
    }

    // MARK: - TMTagListing

    var listedTagIds: [Int] { tags.map { Int($0.tagId) } }

    /// Selects and reveals the row the detail stepped to; stepping onto the last
    /// loaded tag fetches the next page.
    func didStep(to tagId: Int) {
        if listedTagIds.contains(tagId) {
            selectedTagId = tagId
            scrollTarget = tagId
        }
        if let last = tags.last, Int(last.tagId) == tagId, hasMoreResults { fetchNextPage() }
    }
}

struct TMQueryScreen: View {
    @Bindable var model: TMQueryModel

    var body: some View {
        ScrollViewReader { proxy in
            List {
                if let status = model.statusText {
                    TMQueryStatus(message: status, failed: model.failed, retry: model.refresh)
                }
                ForEach(Array(model.tags.enumerated()), id: \.offset) { index, tag in
                    let tagId = Int(tag.tagId)
                    let selected = model.selectedTagId == tagId
                    let content = TMTagRowContent(tagId: tagId, tag: tag)
                    Button { model.open(tag) } label: {
                        TMTagRow(content: content, showsChevron: !model.expanded, selected: selected)
                    }
                    .tmTagRowAccessibility(content, selected: selected)
                    .tmTagListRow(selected: selected, groupedInset: true)
                    .id(tagId)
                    .onAppear { model.rowAppeared(index) }
                }
                if model.isLoading {
                    TMBarberPole()
                        .frame(maxWidth: .infinity)
                        .frame(height: 68)
                        .accessibilityLabel("Loading tags")
                        .accessibilityIdentifier("query.loading.barberpole")
                        .listRowInsets(EdgeInsets())
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .refreshable { await model.refreshAndWait() }
            .onChange(of: model.scrollTarget) { _, target in
                guard let target else { return }
                withAnimation(UIAccessibility.isReduceMotionEnabled ? nil : .default) { proxy.scrollTo(target) }
            }
        }
        .background { TMScreenBackground() }
        .onAppear {
            model.syncSelection()
            if model.tags.isEmpty && model.statusText == nil { model.fetchNextPage() }
        }
    }
}

/// Centered symbol, message and recovery, the way system empty states read.
struct TMQueryStatus: View {
    let message: String
    let failed: Bool
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: failed ? "wifi.exclamationmark" : "magnifyingglass")
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(Color(uiColor: .secondaryLabel))
                .accessibilityHidden(true)
            Text(message)
                .font(TMTheme.font(.body))
                .foregroundStyle(Color(uiColor: .secondaryLabel))
                .multilineTextAlignment(.center)
                .tmLabelMetrics(.body)
            if failed {
                Button("Retry", action: retry)
                    .font(TMTheme.font(.body))
                    .frame(minHeight: 44)
                    .buttonStyle(.borderless)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 48, leading: 32, bottom: 24, trailing: 32))
        .listRowInsets(EdgeInsets())
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
    }
}
