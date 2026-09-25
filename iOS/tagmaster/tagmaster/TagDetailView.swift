//
//  TagDetailView.swift
//  tagmaster
//
//  The tag detail: four pages (Summary, Details, Tracks, Videos) under a tab
//  bar, the quartet while the tag is on its way, and a bar that grows the
//  saved-list toggles and the list steppers when the detail sits beside a list.
//

import SwiftUI

struct TagDetailScreen: View {
    @Bindable var model: TagDetailModel
    @Environment(\.tmAccent) private var accent
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        ZStack {
            if model.isEmpty {
                TagLoadingView(model: model)
            } else {
                // Beside a list the detail runs under the floating list column; the
                // pages and their bar belong to the horizontal safe area, not the bounds.
                // TabView grows into any safe area its edges touch; a hair of padding
                // keeps it off the column's unsafe strip, where the UIKit bar never went. (Drawing
                // it back a pixel with an offset counts as touching again.)
                pages
                    .padding(.horizontal, model.hasHorizontalSafeArea ? 1 / displayScale : 0)
            }
        }
        .navigationTitle(model.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }


        .focusEffectDisabled()
        .tmFollowsUIKitTint()

        .alert(TMRecoverableError.title,
               isPresented: Binding(get: { model.error != nil }, set: { if !$0 { model.error = nil } }),
               presenting: model.error) { error in
            if let retry = error.retry {
                Button("Retry", action: retry)
            }
            Button("Cancel", role: .cancel) {}
        } message: { error in
            Text(error.message)
        }
        .onAppear { model.screenVisible = true }
        .onDisappear { model.screenVisible = false }
    }

    private var pages: some View {
        TabView(selection: $model.selectedPage) {
            TagSummaryPage(model: model.summary)
                .background(TMPageTabBarBridge())
                .tabItem { Label(TagDetailModel.Page.summary.title, systemImage: TagDetailModel.Page.summary.symbol).accessibilityIdentifier("page-\(TagDetailModel.Page.summary.title)").environment(\.symbolVariants, .none) }
                .tag(TagDetailModel.Page.summary)
            TagDetailsPage(model: model)
                .background(TMPageTabBarBridge())
                .tabItem { Label(TagDetailModel.Page.details.title, systemImage: TagDetailModel.Page.details.symbol).accessibilityIdentifier("page-\(TagDetailModel.Page.details.title)").environment(\.symbolVariants, .none) }
                .tag(TagDetailModel.Page.details)
            TagTracksPage(model: model.tracks)
                .background(TMPageTabBarBridge())
                .tabItem { Label(TagDetailModel.Page.tracks.title, systemImage: TagDetailModel.Page.tracks.symbol).accessibilityIdentifier("page-\(TagDetailModel.Page.tracks.title)").environment(\.symbolVariants, .none) }
                .tag(TagDetailModel.Page.tracks)
            TagVideosPage(model: model)
                .background(TMPageTabBarBridge())
                .tabItem { Label(TagDetailModel.Page.videos.title, systemImage: TagDetailModel.Page.videos.symbol).accessibilityIdentifier("page-\(TagDetailModel.Page.videos.title)").environment(\.symbolVariants, .none) }
                .tag(TagDetailModel.Page.videos)
        }
        .tint(accent)
        .tmPageTabBar()
    }

    // MARK: Bar

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        if model.showsSteppers {
            ToolbarItemGroup(placement: .topBarTrailing) {
                TMBarButton("chevron.up", label: "Previous tag", action: model.stepToPreviousTag)
                    .disabled(!model.hasPreviousTag)
                TMBarButton("chevron.down", label: "Next tag", action: model.stepToNextTag)
                    .disabled(!model.hasNextTag)
            }
        }
        if !model.isEmpty {
            if model.expanded {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    TMBarButton(model.isFavorite ? "heart.fill" : "heart",
                                label: model.isFavorite ? "Remove Favorite" : "Add Favorite",
                                action: model.toggleFavorite)
                    TMBarButton(model.isTeachable ? "person.2.fill" : "person.2",
                                label: model.isTeachable ? "Unmark as Teachable" : "Mark as Teachable",
                                action: model.toggleTeachable)
                    TMBarButton("text.badge.plus", label: "Add to list") { model.showListPicker(from: .toolbar) }
                        .accessibilityIdentifier("tag.addToList")
                        .tmListPicker(model: model, source: .toolbar)
                    refreshItem
                    shareItem
                }
            } else {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    refreshItem
                    TMBarButton("tag", label: "Favorite and Teachable options", action: model.showActions)
                        .accessibilityIdentifier("tag.actions")
                        .background(TMActionSheet(isPresented: $model.actionsPresented, actions: model.tagActions))
                        .tmListPicker(model: model, source: .actions)
                    shareItem
                }
            }
        }
    }

    @ViewBuilder
    private var refreshItem: some View {
        if model.fetchPending {
            TMBarberPole(compact: true, darkSurface: true, animating: model.screenVisible)
                .accessibilityElement()
                .accessibilityLabel("Refreshing tag")
                .accessibilityAddTraits(.isStaticText)
        } else {
            TMBarButton("arrow.clockwise", label: "Refresh", action: model.refresh)
        }
    }

    @ViewBuilder
    private var shareItem: some View {
        if let url = model.shareURL, let message = model.shareMessage {
            ShareLink(item: url, message: Text(message)) {
                TMBarButton.symbol("square.and.arrow.up")
            }
            .accessibilityLabel("Share")
        }
    }

}

/// A bar button drawn the way `+[DPAppDelegate barButtonItemWithSystemName:]` draws
/// one: the symbol at 17 pt regular, medium scale, and a spoken name.
struct TMBarButton: View {
    let systemName: String
    let label: String
    let action: () -> Void

    init(_ systemName: String, label: String, action: @escaping () -> Void) {
        self.systemName = systemName
        self.label = label
        self.action = action
    }

    /// The same image a UIBarButtonItem gets, so the bar lays it out identically.
    static func symbol(_ name: String) -> some View {
        let configuration = UIImage.SymbolConfiguration(pointSize: 17, weight: .regular, scale: .medium)
        let image = UIImage(systemName: name, withConfiguration: configuration) ?? UIImage()
        // UIKit centres a bar image by its alignment rect; SwiftUI centres its bounds.
        let insets = image.alignmentRectInsets
        return Image(uiImage: image)
            .renderingMode(.template)
            .offset(x: (insets.right - insets.left) / 2, y: (insets.bottom - insets.top) / 2)
    }

    var body: some View {
        Button(action: action) { TMBarButton.symbol(systemName) }
            .accessibilityLabel(label)
    }
}

extension View {
    /// The detail's page bar: always a bottom tab bar (never the iPad top bar or a
    /// sidebar), never minimizing, in the system's own material.
    func tmPageTabBar() -> some View {
        modifier(TMPageTabBarModifier())
    }
}

private struct TMPageTabBarModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .tabViewStyle(.tabBarOnly)
                .tabBarMinimizeBehavior(.never)
        } else if #available(iOS 18.0, *) {
            content.tabViewStyle(.tabBarOnly)
        } else {
            content
        }
    }
}

// MARK: - Loading

/// What stands in for the pages until the tag arrives: the quartet gathering,
/// or, if the tag could not be loaded, why not and a way to try again.
struct TagLoadingView: View {
    let model: TagDetailModel
    @AccessibilityFocusState private var statusFocused: Bool
    @State private var announcedGeneration = -1

    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: 8) {
                    if model.initialPending {
                        TMQuartetStaff(animating: model.quartetMoving)
                            .padding(.bottom, 12)
                    }
                    Text(model.loadingHeading)
                        .font(.title3)
                        .foregroundStyle(Color(.label))
                        .multilineTextAlignment(.center)
                        .accessibilityHidden(true)
                    Text(model.loadingStatus)
                        .font(.body)
                        .foregroundStyle(Color(TagLoadingView.statusColor))
                        .multilineTextAlignment(.center)
                        .accessibilityLabel(model.loadingStatusSpoken)
                        .accessibilityIdentifier("tag.loadingStatus")
                        .accessibilityFocused($statusFocused)
                    if model.loadFailed {
                        Button("Retry", action: model.retryInitialLoad)
                            .font(.body)
                            .frame(minWidth: 80, minHeight: 44)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 24)
                .padding(.vertical, 24)
                .frame(maxWidth: .infinity, minHeight: proxy.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .background(Color(.systemBackground).ignoresSafeArea())
        .accessibilityIdentifier("tag.initialLoading")
        .onChange(of: model.quartetMoving, initial: true) { _, moving in
            // Announced once per request, when the screen can actually be heard.
            guard moving, announcedGeneration != model.requestGeneration else { return }
            announcedGeneration = model.requestGeneration
            statusFocused = true
        }
    }

    /// Secondary label's translucent gray falls below 4.5:1 on white at body size.
    static let statusColor = UIColor { traits in
        traits.userInterfaceStyle == .dark ? .secondaryLabel : .darkGray
    }
}

/// Gives the TabView's tab bar what the UIKit page bar had, which SwiftUI has no
/// modifiers for: the identifiers the UI tests and store capture find it by, the
/// accent, and a compact width so iPad keeps a bottom bar rather than moving the
/// pages to its top bar. Only the bar is compact; each page keeps its column's
/// real width class.
struct TMPageTabBarBridge: UIViewControllerRepresentable {
    var titles: [String] = TagDetailModel.Page.allCases.map(\.title)

    final class Controller: UIViewController {
        var titles: [String] = []

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            configure()
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            configure()
        }

        override func viewWillLayoutSubviews() {
            super.viewWillLayoutSubviews()
            configure()
        }

        func configure() {
            guard let tabs = tabBarController else { return }
            let bar = tabs.tabBar
            if bar.accessibilityIdentifier != "page-tab-bar" { bar.accessibilityIdentifier = "page-tab-bar" }
            for (item, title) in zip(bar.items ?? [], titles) where item.accessibilityIdentifier != "page-\(title)" {
                item.accessibilityIdentifier = "page-\(title)"
            }
            if !tabs.traitOverrides.contains(UITraitHorizontalSizeClass.self)
                || tabs.traitOverrides.horizontalSizeClass != .compact {
                tabs.traitOverrides.horizontalSizeClass = .compact
            }
            // The pages sit over the detail's own watermark, as the UIKit pages did.
            if tabs.view.backgroundColor != .clear { tabs.view.backgroundColor = .clear }
            let columnClass = tabs.parent?.traitCollection.horizontalSizeClass ?? .unspecified
            for page in tabs.viewControllers ?? [] {
                if page.viewIfLoaded?.backgroundColor != .clear { page.viewIfLoaded?.backgroundColor = .clear }
                let overrides = page.traitOverrides
                if !overrides.contains(UITraitHorizontalSizeClass.self) || overrides.horizontalSizeClass != columnClass {
                    page.traitOverrides.horizontalSizeClass = columnClass
                }
            }
        }
    }

    func makeUIViewController(context: Context) -> Controller {
        let controller = Controller()
        controller.titles = titles
        controller.view.isUserInteractionEnabled = false
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.titles = titles
        controller.configure()
    }
}
