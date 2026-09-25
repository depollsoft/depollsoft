//
//  TMBrowseScreen.swift
//  tagmaster
//
//  Browse: the catalog four ways — Latest, Rating, Downloads, Classic — each a
//  page of results behind a tab bar at the foot of the screen, over one
//  watermark. Each page keeps its own results and place when the others show.
//

import SwiftUI
import UIKit

/// One page of Browse.
struct TMBrowsePage: Identifiable, Equatable {
    let title: String
    let symbol: String
    let query: TMTagQuery
    var id: String { title }

    static let all = [
        TMBrowsePage(title: "Latest", symbol: "clock", query: TMTagQuery(sortBy: DPTagSortPosted)),
        TMBrowsePage(title: "Rating", symbol: "star", query: TMTagQuery(sortBy: DPTagSortRating)),
        TMBrowsePage(title: "Downloads", symbol: "arrow.down.circle", query: TMTagQuery(sortBy: DPTagSortDownloaded)),
        TMBrowsePage(title: "Classic", symbol: "book",
                     query: TMTagQuery(sortBy: DPTagSortClassic, collection: DPTagCollectionClassicTags)),
    ]
}

@MainActor
@Observable
final class TMBrowseModel: TMTagListing {
    let pages: [TMQueryModel]
    var selectedIndex = 0

    init(catalog: TMCatalog = .live, navigator: TMNavigator? = nil) {
        pages = TMBrowsePage.all.map { TMQueryModel(query: $0.query, catalog: catalog, navigator: navigator) }
    }

    var selectedPage: TMQueryModel { pages[min(max(selectedIndex, 0), pages.count - 1)] }

    // The detail steps through whichever page is showing.
    var listedTagIds: [Int] { selectedPage.listedTagIds }
    var selectedTagId: Int? { selectedPage.selectedTagId }
    func didStep(to tagId: Int) { selectedPage.didStep(to: tagId) }
}

struct TMBrowseScreen: View {
    @Bindable var model: TMBrowseModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        TabView(selection: $model.selectedIndex) {
            ForEach(Array(TMBrowsePage.all.enumerated()), id: \.offset) { index, page in
                // Each page draws the watermark in the one place Browse's own view held it.
                TMQueryScreen(model: model.pages[index])
                    .background { TMClearTabContainer() }
                    // Only the tab container is compact; pages keep the column's own size class.
                    .environment(\.horizontalSizeClass, sizeClass)
                    .tabItem {
                        Label(page.title, systemImage: page.symbol)
                            // Outline symbols, as UITabBarItem showed them; SwiftUI would fill them.
                            .environment(\.symbolVariants, .none)
                            .accessibilityIdentifier("page-\(page.title)")
                    }
                    .tag(index)
                    // The identifiers the UI tests and store capture find the bar and its tabs by.
                    .background(TMPageTabBarBridge(titles: TMBrowsePage.all.map(\.title)))
            }
        }
        .tint(TMTheme.accent)
        .tmTabBarNeverMinimizes()
        // A compact container keeps the bar at the foot of the screen on iPad too.
        .environment(\.horizontalSizeClass, .compact)
        .onChange(of: model.selectedIndex) { _, _ in
            NotificationCenter.default.post(name: .TMTagListDidChange, object: model.selectedPage.owner)
        }
    }
}

extension View {
    @ViewBuilder
    func tmTabBarNeverMinimizes() -> some View {
        if #available(iOS 26.0, *) {
            tabBarMinimizeBehavior(.never)
        } else {
            self
        }
    }
}

/// Clears the views SwiftUI's TabView puts between a page and the screen (its
/// tab bar controller's view and hosting views), as TMPageViewController's
/// content view was clear, so the watermark behind shows through.
private struct TMClearTabContainer: UIViewRepresentable {
    final class Probe: UIView {
        override func didMoveToWindow() {
            super.didMoveToWindow()
            DispatchQueue.main.async { [weak self] in
                var view = self?.superview
                while let current = view {
                    if current.backgroundColor != nil { current.backgroundColor = .clear }
                    if current.next is UITabBarController { break }
                    view = current.superview
                }
            }
        }
    }

    func makeUIView(context: Context) -> Probe { Probe() }
    func updateUIView(_ view: Probe, context: Context) {}
}
