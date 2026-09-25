//
//  TMSearchScreen.swift
//  tagmaster
//
//  Search: a field in the navigation bar and five options underneath. Running
//  it (the keyboard's Search key or the bar's magnifying glass) opens a results
//  screen for the typed text and the options; an empty field lists every tag
//  the options allow. The options are remembered between visits.
//

import SwiftUI
import UIKit

@MainActor
@Observable
final class TMSearchModel {
    static let filters = [
        TMFilter(title: "Sort By", choices: ["Title", "Downloads", "Recent", "Rating"]),
        TMFilter(title: "Sheet Music", choices: TMRandomTagFilters.presenceChoices),
        TMFilter(title: "Learning Tracks", choices: TMRandomTagFilters.presenceChoices),
        TMFilter(title: "Parts", choices: ["Any", "3", "4", "5", "6", "7", "8"], apportioned: false),
        TMFilter(title: "Collection", choices: ["Any", "Classic Tags", "Easy Tags"]),
    ]
    /// Where each option is remembered, in `filters` order.
    static let defaultsKeys = ["search.sortBy", "search.sheetMusic", "search.learningTracks", "search.parts",
                               "search.collection"]

    var navigator: TMNavigator?
    private let defaults: UserDefaults

    var text = ""
    /// The chosen index of each option, in `filters` order.
    var selections: [Int] {
        didSet {
            for (key, value) in zip(TMSearchModel.defaultsKeys, selections) { defaults.set(value, forKey: key) }
        }
    }

    init(navigator: TMNavigator? = nil, defaults: UserDefaults = .standard) {
        self.navigator = navigator
        self.defaults = defaults
        selections = TMSearchModel.defaultsKeys.map { defaults.integer(forKey: $0) }
    }

    /// The query the text and options describe.
    var query: TMTagQuery {
        var query = TMTagQuery(text: text)
        query.sortBy = [DPTagSortTitle, DPTagSortDownloaded, DPTagSortPosted, DPTagSortRating][safe: selections[0]]
            ?? DPTagSortNone
        query.sheetMusic = TMRandomTagFilters.presence(selections[1])
        query.learningTracks = TMRandomTagFilters.presence(selections[2])
        query.parts = selections[3] > 0 ? selections[3] + 2 : nil
        query.collection = [DPTagCollectionNone, DPTagCollectionClassicTags, DPTagCollectionEasyTags][safe: selections[4]]
            ?? DPTagCollectionNone
        return query
    }

    /// Opens the results. The typed text stays for when the user comes back.
    func search() {
        navigator?.show(.results(query))
    }
}

struct TMSearchScreen: View {
    @Bindable var model: TMSearchModel

    var body: some View {
        List {
            Section {
                ForEach(Array(TMSearchModel.filters.enumerated()), id: \.offset) { index, filter in
                    TMFilterRow(filter: filter, selection: $model.selections[index])
                }
            } header: {
                TMSectionHeader("Search Options")
            } footer: {
                TMSectionFooter("Searches match titles and lyrics. Leave the field empty to list every tag that matches the options.")
            }
        }
        .listStyle(.insetGrouped)
        .tmInsetGroupedMetrics()
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.immediately)
        .searchable(text: $model.text, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search")
        .tmKeepsBarWhileSearching()
        .onSubmit(of: .search) { model.search() }
        .background { TMScreenBackground(grouped: true) }
        .background(TMSearchKeyHook())
    }
}

/// An empty field is a real search here (every tag matching the options), so the
/// keyboard's Search key stays enabled, as the UIKit search bar was set up.
/// SwiftUI's searchable has no modifier for it; this reaches the search bar.
struct TMSearchKeyHook: UIViewControllerRepresentable {
    final class Hook: UIViewController {
        /// The screen's own controller: the ancestor the navigation controller holds.
        var screen: UIViewController? {
            var controller: UIViewController? = self
            while let current = controller, !(current.parent is UINavigationController) {
                controller = current.parent
            }
            return controller
        }

        var searchField: UISearchTextField? { screen?.navigationItem.searchController?.searchBar.searchTextField }

        func apply() {
            guard let field = searchField, field.enablesReturnKeyAutomatically else { return }
            field.enablesReturnKeyAutomatically = false
        }

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply()
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            apply()
        }

        override func viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()
            apply()
        }
    }

    func makeUIViewController(context: Context) -> Hook {
        let hook = Hook()
        hook.view.isHidden = true
        hook.view.isUserInteractionEnabled = false
        return hook
    }

    func updateUIViewController(_ hook: Hook, context: Context) { hook.apply() }
}

extension View {
    /// The bar and its Search action stay put while the field is in use, as the
    /// UIKit search controller (hidesNavigationBarDuringPresentation = NO) kept them.
    @ViewBuilder
    func tmKeepsBarWhileSearching() -> some View {
        if #available(iOS 17.1, *) {
            searchPresentationToolbarBehavior(.avoidHidingContent)
        } else {
            self
        }
    }
}
