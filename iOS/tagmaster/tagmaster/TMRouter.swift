//
//  TMRouter.swift
//  tagmaster
//
//  Where every Tag Master screen goes. The router owns the navigation state the
//  SwiftUI shell draws: the stack (the whole app on iPhone, the list column on
//  iPad), the tag open beside the list, and the reader stacked over it. Screens
//  reach it through `TMNavigator`, one per screen, so each knows which screen it
//  is when it asks to leave.
//

import SwiftUI
import UIKit

extension TMTagQuery: Hashable {
    func hash(into hasher: inout Hasher) {
        hasher.combine(text)
        hasher.combine(sortBy.rawValue)
        hasher.combine(collection.rawValue)
        hasher.combine(parts)
        hasher.combine(learningTracks)
        hasher.combine(sheetMusic)
        hasher.combine(minimumRating)
        hasher.combine(minimumDownloads)
        hasher.combine(fieldList)
    }
}

/// One screen on a stack. Routes compare by identity, so the same list pushed
/// twice is two screens, and a screen can find (and remove) exactly itself.
struct TMRoute: Hashable, Identifiable {
    enum Kind {
        /// A screen another screen asked for, with the model made for it.
        case screen(TMDestination, TMScreenModel)
        /// A tag pushed onto the stack (iPhone, or a collapsed split).
        case tag(TagDetailModel)
        /// The sheet music reader, over the detail that opened it.
        case sheetMusic(TMSheetMusicDocument, TagSummaryModel)
    }

    let id: UUID
    let kind: Kind
    /// The list this screen shows, for a tag a deep link opens over it.
    let listingSource: TMListingSource?

    init(id: UUID = UUID(), kind: Kind, listingSource: TMListingSource? = nil) {
        self.id = id
        self.kind = kind
        self.listingSource = listingSource
    }

    static func == (lhs: TMRoute, rhs: TMRoute) -> Bool { lhs.id == rhs.id }

    /// The screen this route opens, when it is one another screen asked for.
    var destination: TMDestination? {
        if case .screen(let destination, _) = kind { return destination }
        return nil
    }

    /// The model of the screen this route opens.
    var screen: TMScreenModel? {
        if case .screen(_, let model) = kind { return model }
        return nil
    }

    /// The tag this route shows, when it is a pushed tag.
    var tagModel: TagDetailModel? {
        if case .tag(let model) = kind { return model }
        return nil
    }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

/// A list screen as the detail sees it: the tags it shows, for stepping, and
/// the object its "list changed" notifications come from.
final class TMListingSource: NSObject, TMTagListSource {
    weak var listing: TMTagListing?

    init(_ listing: TMTagListing?) {
        self.listing = listing
    }

    func tm_listedTagIds() -> [NSNumber] {
        MainActor.assumeIsolated { (listing?.listedTagIds ?? []).map { NSNumber(value: $0) } }
    }

    func tm_didStep(toTagId tagId: Int32) {
        MainActor.assumeIsolated { listing?.didStep(to: Int(tagId)) }
    }
}

@Observable @MainActor
final class TMRouter {
    /// The phone's whole stack, or the list column's on iPad. Home is its root.
    var path: [TMRoute] = []
    /// Screens stacked on the tag beside the list (the sheet music reader).
    var detailPath: [TMRoute] = []
    /// True when the list and a tag sit side by side (a regular-width iPad).
    private(set) var expanded = false
    /// The split's columns: both, or the tag alone while reading full screen.
    var columnVisibility: NavigationSplitViewVisibility = .doubleColumn
    /// The tag beside the list, reused from tag to tag so its open page survives.
    let detail: TagDetailModel
    /// Whether the detail column shows a tag yet, or still the placeholder.
    private(set) var hasDetail = false
    /// Home, the root of the list stack.
    private(set) var home: TMHomeModel!
    private var homeNavigator: TMRouteNavigator!
    /// What the screens this router opens run against; tests substitute them.
    let catalog: TMCatalog
    let account: TMAccount
    let build: TMBuildInfo

    init(detail: TagDetailModel? = nil, catalog: TMCatalog = .live,
         account: TMAccount = .firebase, build: TMBuildInfo = .bundle) {
        let detail = detail ?? TagDetailModel()
        self.detail = detail
        self.catalog = catalog
        self.account = account
        self.build = build
        wire(detail)
        let navigator = TMRouteNavigator(router: self, routeId: nil)
        homeNavigator = navigator
        home = TMHomeModel(catalog: catalog, navigator: navigator)
        navigator.source.listing = home
    }

    /// The column a collapsed split shows. It is decided when the split collapses
    /// (a chosen tag, never the placeholder), then follows the user: Back to the
    /// list stays on the list, as UIKit's top-column choice only applied at collapse.
    var preferredCompactColumn: NavigationSplitViewColumn = .sidebar

    func setExpanded(_ expanded: Bool) {
        guard self.expanded != expanded else { return }
        self.expanded = expanded
        detail.expanded = expanded
        if !expanded {
            columnVisibility = .doubleColumn
            preferredCompactColumn = hasDetail ? .detail : .sidebar
        }
        selectionChanged()
    }

    /// The tag the detail column shows, when there is one beside the list.
    var currentSplitTagId: Int? { expanded && hasDetail ? Int(detail.tagId) : nil }

    // MARK: - Going places

    /// Opens a tag: beside the list when expanded (the page the user was on
    /// survives), pushed onto the stack everywhere else.
    func showTag(_ tagId: Int32, source: TMTagListSource?) {
        if expanded {
            detail.source = source
            detail.show(tagId: tagId)
            hasDetail = true
            // Should the split collapse now, the chosen tag stays on top.
            preferredCompactColumn = .detail
            detailPath = []
            selectionChanged()
            source?.tm_didStep(toTagId: tagId)
            return
        }
        let model = TagDetailModel()
        model.source = source
        wire(model)
        model.show(tagId: tagId)
        // A collapsed split pushes onto the list stack, so that is the column to show.
        preferredCompactColumn = .sidebar
        path.append(TMRoute(kind: .tag(model)))
    }

    func show(_ destination: TMDestination) {
        let id = UUID()
        let navigator = TMRouteNavigator(router: self, routeId: id)
        let model = TMScreenModel(destination, navigator: navigator, router: self)
        preferredCompactColumn = .sidebar
        path.append(TMRoute(id: id, kind: .screen(destination, model),
                            listingSource: model.listsTags ? navigator.source : nil))
    }

    /// Opens a saved list: Favorites is Home's own section, so it returns to
    /// Home; Teachable Tags and the user's lists push onto the list stack.
    func showList(key: String) {
        if expanded { columnVisibility = .doubleColumn }
        if key == TMTagLists.favoriteKey {
            path.removeAll()
            preferredCompactColumn = .sidebar
            return
        }
        show(key == TMTagLists.teachableKey ? .teachable : .list(key))
    }

    /// Takes the route with `id` off whichever stack holds it, leaving anything above it.
    func remove(routeId id: UUID) {
        path.removeAll { $0.id == id }
        detailPath.removeAll { $0.id == id }
    }

    /// Back to Home with nothing pushed.
    func popToRoot() {
        path.removeAll()
    }

    func showSheetMusic(_ document: TMSheetMusicDocument, summary: TagSummaryModel, from model: TagDetailModel) {
        let route = TMRoute(kind: .sheetMusic(document, summary))
        if expanded && model === detail {
            detailPath.append(route)
        } else {
            path.append(route)
        }
    }

    /// Hides the list for reading, and gives it back.
    func toggleFullScreen() {
        columnVisibility = columnVisibility == .detailOnly ? .doubleColumn : .detailOnly
    }

    var isFullScreen: Bool { columnVisibility == .detailOnly }

    // MARK: - Deep links

    /// Opens `tagmaster://tag/<id>` or `tagmaster:///open/tag/<id>`; false for any other URL.
    @discardableResult
    func open(_ url: URL) -> Bool {
        guard let tagId = TMDeepLink.tagId(in: url) else { return false }
        showTag(tagId, source: topListingSource)
        return true
    }

    /// The list on top of the list stack, which a linked tag steps through as the
    /// UIKit delegate adopted it: Home, a saved list or results, or the list the
    /// pushed tag on top came from. Browse, Search and Settings list nothing.
    var topListingSource: TMTagListSource? {
        guard let top = path.last else { return homeNavigator.source }
        switch top.kind {
        case .tag(let model): return model.source
        case .screen: return top.listingSource
        case .sheetMusic: return nil
        }
    }

    // MARK: - Internals

    private func wire(_ model: TagDetailModel) {
        model.navigator = TMDetailNavigator(
            showTag: { [weak self, weak model] id in
                self?.showTag(id, source: model?.source)
            },
            showList: { [weak self] key in
                self?.showList(key: key)
            },
            showSheetMusic: { [weak self, weak model] document in
                guard let self, let model else { return }
                self.showSheetMusic(document, summary: model.summary, from: model)
            })
    }

    private func selectionChanged() {
        NotificationCenter.default.post(name: .TMTagSelectionDidChange, object: self)
    }
}

/// What one screen asks of the router: it knows its own route, so it can
/// remove exactly itself, and its own listing, so the tags it opens can step.
@MainActor
final class TMRouteNavigator: TMNavigator {
    weak var router: TMRouter?
    /// The screen's route; nil for Home, which never leaves.
    let routeId: UUID?
    let source: TMListingSource

    init(router: TMRouter, routeId: UUID?) {
        self.router = router
        self.routeId = routeId
        source = TMListingSource(nil)
    }

    var isExpandedSplit: Bool { router?.expanded ?? false }
    var currentSplitTagId: Int? { router?.currentSplitTagId }

    func showTag(_ tagId: Int) {
        router?.showTag(Int32(tagId), source: source.listing == nil ? nil : source)
    }

    func show(_ destination: TMDestination) {
        router?.show(destination)
    }

    func removeScreen() {
        guard let routeId else { return }
        router?.remove(routeId: routeId)
    }

    func openURL(_ url: URL) {
        UIApplication.shared.open(url)
    }

    func presentPrivacyChoices() {
        guard let presenter = TMRouteNavigator.topController() else { return }
        TelemetryConsent.present(from: presenter)
    }

    /// The controller everything else is presented over.
    static func topController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow)
        var top = window?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}

/// `tagmaster://` links to a tag, with the exact validation the Objective-C
/// delegate applied: the tag host with one path component, or no host (or
/// `open`) with `/tag/<id>`; no user, password or port; a positive decimal id
/// that fits in an `Int32`, with no sign, spaces or other characters.
enum TMDeepLink {
    static func tagId(in url: URL) -> Int32? {
        guard let scheme = url.scheme, scheme.caseInsensitiveCompare("tagmaster") == .orderedSame,
              url.user == nil, url.password == nil, url.port == nil,
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return nil }
        // Split without normalizing away empty components or trailing slashes.
        let parts = components.path.components(separatedBy: "/")
        let host = url.host ?? ""
        let hostIsTag = host == "tag" && parts.count == 2
        let pathHasTag = (host.isEmpty || host == "open") && parts.count == 3 && parts[1] == "tag"
        guard hostIsTag || pathHasTag, parts.first == "", let identifier = parts.last, !identifier.isEmpty else {
            return nil
        }
        var value: Int32 = 0
        for character in identifier.unicodeScalars {
            guard ("0"..."9").contains(character) else { return nil }
            let digit = Int32(character.value - 48)
            guard value <= (Int32.max - digit) / 10 else { return nil }
            value = value * 10 + digit
        }
        return value == 0 ? nil : value
    }
}
