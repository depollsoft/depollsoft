//
//  TagDetailModel.swift
//  tagmaster
//
//  Everything the tag detail knows and does, apart from how it looks: loading
//  the tag (first load, refresh, retry, a tag change mid-flight), whether a
//  neighbouring tag exists in the list that opened it, the saved-list toggles,
//  and which sheet or alert is up. The views in TagDetailView.swift only lay
//  this out.
//

import Foundation
import Observation
import SwiftUI
import UIKit

// MARK: - Loading

/// Fetches a tag, from the cache or the catalog. Completion runs on the main queue.
protocol TMTagLoading {
    func load(_ tagId: Int32, refresh: Bool, completion: @escaping @MainActor (DPTag?) -> Void)
}

/// The catalog/cache contract is synchronous and may raise, so it runs off the main thread.
struct TMCatalogTagLoader: TMTagLoading {
    func load(_ tagId: Int32, refresh: Bool, completion: @escaping @MainActor (DPTag?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            var loaded: DPTag?
            // A raise leaves `loaded` nil: a useful, non-diagnostic recovery state.
            _ = TMTry({ loaded = DPTag.load(byId: tagId, refresh: refresh) }, nil)
            let result = loaded
            DispatchQueue.main.async { MainActor.assumeIsolated { completion(result) } }
        }
    }
}

/// What opened the list picker, so it can be anchored there.
enum TMPickerSource {
    /// The "Add to list" bar button beside a list.
    case toolbar
    /// "Add to List…" in the tag actions sheet.
    case actions
    /// The assist chip under the title.
    case chip
}

/// A failed request the user can try again.
struct TMRecoverableError: Identifiable {
    let id = UUID()
    let message: String
    let retry: (() -> Void)?

    static let title = "Couldn't complete request"
}

/// Counts work in flight across the detail's pages. Refresh waits until it is idle,
/// and a track will not start loading beside another request.
@Observable @MainActor
final class TMBusyCount {
    private(set) var count = 0
    /// How many pieces of work have finished, so a double settlement shows up.
    private(set) var settled = 0
    var isBusy: Bool { count > 0 }
    func begin() { count += 1 }
    func end() {
        count = max(0, count - 1)
        settled += 1
    }
}

/// What the detail asks of the screen around it. The UIKit shell and the SwiftUI
/// shell each answer these their own way.
@MainActor
struct TMDetailNavigator {
    /// Opens another tag in place of this one (stepping through a list).
    var showTag: (Int32) -> Void = { _ in }
    /// Opens a saved list (a chip).
    var showList: (String) -> Void = { _ in }
    /// Pushes the sheet music reader.
    var showSheetMusic: (TMSheetMusicDocument) -> Void = { _ in }
}

/// A downloaded sheet music file and what the reader's bar needs to show beside it.
struct TMSheetMusicDocument: Identifiable, Hashable {
    let fileURL: URL
    let title: String
    let writtenKey: String?
    var id: URL { fileURL }
}

@Observable @MainActor
final class TagDetailModel {
    enum Page: Int, CaseIterable {
        case summary, details, tracks, videos

        var title: String {
            switch self {
            case .summary: "Summary"
            case .details: "Details"
            case .tracks: "Tracks"
            case .videos: "Videos"
            }
        }

        var symbol: String {
            switch self {
            case .summary: "doc.text"
            case .details: "info.circle"
            case .tracks: "waveform"
            case .videos: "play.rectangle"
            }
        }
    }

    // MARK: State

    private(set) var tagId: Int32 = 0
    private(set) var tag: DPTag?
    private(set) var fetchPending = false
    private(set) var loadFailed = false
    private(set) var lastFetchWasRefresh = false
    private(set) var requestGeneration = 0
    var selectedPage: Page = .summary

    /// The list this tag was opened from, for stepping. Held weakly, as the list owns the detail.
    weak var source: TMTagListSource? { didSet { sourceRevision += 1 } }
    /// Beside a list (the split is expanded); the bar grows the toggles and steppers.
    var expanded = false
    /// The column has a horizontal safe area (beside a list on iPad, or landscape on iPhone).
    var hasHorizontalSafeArea = false
    /// On screen, and the app active: the only time the quartet may move.
    var screenVisible = false
    var applicationActive = true

    /// Bumped whenever the saved lists change anywhere, so membership reads re-run.
    private(set) var listsRevision = 0
    /// Bumped when the source's list may have changed.
    private(set) var sourceRevision = 0

    let busy = TMBusyCount()
    let summary: TagSummaryModel
    let tracks: TagTracksModel

    // Presentations
    var actionsPresented = false
    /// Where the "Add to list" picker is anchored while it is up (a popover on iPad).
    var pickerSource: TMPickerSource?
    var error: TMRecoverableError?

    var navigator = TMDetailNavigator()
    let undoManager = UndoManager()

    private let loader: TMTagLoading
    private var observers: [NSObjectProtocol] = []

    init(loader: TMTagLoading = TMCatalogTagLoader()) {
        self.loader = loader
        summary = TagSummaryModel(busy: busy)
        tracks = TagTracksModel(busy: busy)
        summary.detail = self
        tracks.detail = self
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: .userDataChanged, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated {
                self?.listsRevision += 1
                self?.sourceRevision += 1
            }
        })
        observers.append(center.addObserver(forName: .TMTagListDidChange, object: nil, queue: .main) { [weak self] note in
            MainActor.assumeIsolated {
                guard let self, let source = self.source, note.object as AnyObject? === source else { return }
                self.sourceRevision += 1
            }
        })
        observers.append(center.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.applicationActive = false }
        })
        observers.append(center.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.applicationActive = true }
        })
    }

    isolated deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    // MARK: Loading

    /// Shows `id`. Work for the previous tag may finish, but it no longer owns the screen.
    func show(tagId id: Int32) {
        guard id != tagId else { return }
        requestGeneration += 1
        fetchPending = false
        tag = nil
        tracks.stopPlayback()
        tagId = id
        load(refresh: false)
    }

    func load(refresh: Bool) {
        guard !fetchPending else { return }
        let identifier = tagId
        requestGeneration += 1
        let generation = requestGeneration
        fetchPending = true
        loadFailed = false
        lastFetchWasRefresh = refresh
        busy.begin()
        loader.load(identifier, refresh: refresh) { [weak self, busy] loaded in
            // Balance even when the screen has gone away or another request owns it.
            defer { busy.end() }
            guard let self, self.requestGeneration == generation, self.tagId == identifier else { return }
            self.fetchPending = false
            self.loadFailed = loaded == nil
            if let loaded {
                if loaded !== self.tag { self.tracks.stopPlayback() }
                self.tag = loaded
            } else if self.tag != nil, self.screenVisible {
                self.error = TMRecoverableError(
                    message: "The tag couldn't be refreshed. Check your connection and try again. Your saved tags are unchanged.",
                    retry: { [weak self] in
                        guard let self, self.requestGeneration == generation, self.tagId == identifier else { return }
                        self.load(refresh: refresh)
                    })
            }
        }
    }

    func retryInitialLoad() { load(refresh: lastFetchWasRefresh) }

    func refresh() {
        guard !busy.isBusy else { return }
        load(refresh: true)
    }

    var isEmpty: Bool { tag == nil }
    var initialPending: Bool { tag == nil && fetchPending }
    var title: String { tag?.title ?? "Tag" }

    var loadingHeading: String { loadFailed ? "Tag unavailable" : "Gathering the quartet…" }
    var loadingStatus: String {
        if loadFailed { return "Couldn't load tag \(tagId). Check your connection and tag ID, then try again." }
        return initialPending ? "Loading tag \(tagId)…" : "Choose a tag to get started."
    }
    var loadingStatusSpoken: String {
        initialPending ? "\(loadingHeading) \(loadingStatus)" : loadingStatus
    }
    var quartetMoving: Bool { initialPending && screenVisible && applicationActive }

    // MARK: Stepping

    private var sourceIds: [Int] {
        _ = sourceRevision
        return source?.tm_listedTagIds().map(\.intValue) ?? []
    }

    private var sourceIndex: Int? { sourceIds.firstIndex(of: Int(tagId)) }

    var hasPreviousTag: Bool { (sourceIndex ?? 0) > 0 }
    var hasNextTag: Bool { sourceIndex.map { $0 + 1 < sourceIds.count } ?? false }
    var canStep: Bool { source != nil && expanded }
    var showsSteppers: Bool { canStep }

    func step(by delta: Int) {
        guard canStep, let index = sourceIndex else { return }
        let target = index + delta
        guard sourceIds.indices.contains(target) else { return }
        navigator.showTag(Int32(sourceIds[target]))
    }

    func stepToPreviousTag() { step(by: -1) }
    func stepToNextTag() { step(by: 1) }

    // MARK: Saved lists

    var isFavorite: Bool {
        _ = listsRevision
        return TMTagLists.contains(Int(tagId), in: TMTagLists.favoriteKey)
    }

    var isTeachable: Bool {
        _ = listsRevision
        return TMTagLists.contains(Int(tagId), in: TMTagLists.teachableKey)
    }

    func toggleFavorite() {
        guard tag != nil else { return }
        if isFavorite { DPAppDelegate.removeFavorite(tagId) } else { DPAppDelegate.addFavorite(tagId) }
    }

    func toggleTeachable() {
        guard tag != nil else { return }
        if isTeachable { DPAppDelegate.removeTeachable(tagId) } else { DPAppDelegate.addTeachable(tagId) }
    }

    func showActions() {
        guard tag != nil else { return }
        actionsPresented = true
    }

    func showListPicker(from source: TMPickerSource = .toolbar) {
        guard tag != nil else { return }
        pickerSource = source
    }

    func pickerPresented(from source: TMPickerSource) -> Bool { pickerSource == source }

    /// The tag actions sheet, top to bottom.
    var tagActions: [TMSheetAction] {
        [
            TMSheetAction(title: isFavorite ? "Remove Favorite" : "Add Favorite") { [weak self] in self?.toggleFavorite() },
            TMSheetAction(title: isTeachable ? "Unmark as Teachable" : "Mark as Teachable") { [weak self] in self?.toggleTeachable() },
            TMSheetAction(title: "Add to List…") { [weak self] in self?.showListPicker(from: .actions) },
            TMSheetAction(title: "Cancel", style: .cancel) {},
        ]
    }

    // MARK: Sharing

    var shareMessage: String? { tag.map { "\($0.title ?? "") - Tag Master for iOS" } }
    var shareURL: URL? { tag?.tagUri() }
}

extension Notification.Name {
    static let TMTagListDidChange = Notification.Name(rawValue: "TMTagListDidChangeNotification")
    static let TMTagSelectionDidChange = Notification.Name(rawValue: "TMTagSelectionDidChangeNotification")
}
