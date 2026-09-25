//
//  TMShell.swift
//  tagmaster
//
//  Tag Master's app shell in SwiftUI: the App, a NavigationStack on iPhone and
//  a NavigationSplitView on iPad, each screen's bar, and the charcoal bar the
//  app has always had. The router (TMRouter.swift) owns where things are.
//

import SwiftUI
import UIKit

// MARK: - App

@main
enum TagMasterMain {
    static func main() {
        if NSClassFromString("XCTestCase") != nil {
            UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(TMTestAppDelegate.self))
        } else {
            TagMasterApp.main()
        }
    }
}

struct TagMasterApp: App {
    @UIApplicationDelegateAdaptor(DPAppDelegate.self) private var delegate
    @State private var router = TMRouter()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            TMRootView(router: router)
                .onOpenURL { url in
                    if !DPAppDelegate.handleAuthURL(url) { router.open(url) }
                }
                // Every activation, the first included, offers Privacy choices until
                // they are made; the hop lets the window become key first.
                .onChange(of: scenePhase, initial: true) { _, phase in
                    guard phase == .active else { return }
                    DispatchQueue.main.async {
                        guard let presenter = TMRouteNavigator.topController() else { return }
                        TelemetryConsent.presentIfNeeded(from: presenter)
                    }
                }
        }
    }
}

/// The unit-test host: no UI, only the cache serializers the app registers.
@objc(TMTestAppDelegate)
final class TMTestAppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        DPAppDelegate.configureCacheSerialization()
        return true
    }
}

// MARK: - Environment

extension EnvironmentValues {
    /// True inside the iPad split, which draws one watermark behind both columns;
    /// screens there leave their own backdrop clear.
    @Entry var tmSharedWatermark = false
}

// MARK: - Root

struct TMRootView: View {
    let router: TMRouter
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        if UIDevice.current.userInterfaceIdiom == .pad {
            TMSplitRoot(router: router)
        } else {
            TMStackRoot(router: router)
        }
    }
}

/// iPhone: one stack, Home at its root.
struct TMStackRoot: View {
    @Bindable var router: TMRouter

    var body: some View {
        NavigationStack(path: $router.path) {
            TMHomeRoute(router: router)
                .navigationDestination(for: TMRoute.self) { route in
                    TMRouteScreen(route: route, router: router)
                }
        }
        .onAppear { router.setExpanded(false) }
    }
}

/// iPad: the list stack beside the tag, one watermark behind both.
struct TMSplitRoot: View {
    /// The columns can only be made clear (so one watermark shows behind both) from
    /// iOS 18; before that each screen keeps its own, rather than none showing.
    static var columnsCanBeClear: Bool {
        if #available(iOS 18.0, *) { true } else { false }
    }

    @Bindable var router: TMRouter
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        GeometryReader { geometry in
            NavigationSplitView(columnVisibility: $router.columnVisibility,
                                preferredCompactColumn: $router.preferredCompactColumn) {
                NavigationStack(path: $router.path) {
                    TMHomeRoute(router: router)
                        .navigationDestination(for: TMRoute.self) { route in
                            TMRouteScreen(route: route, router: router)
                        }
                }
                // A comfortable list on both 11- and 13-inch iPads: 36% of the width, 320–400 points.
                .navigationSplitViewColumnWidth(min: 320,
                                                ideal: min(max(geometry.size.width * 0.36, 320), 400),
                                                max: 400)
            } detail: {
                NavigationStack(path: $router.detailPath) {
                    TMDetailColumn(router: router)
                        .navigationDestination(for: TMRoute.self) { route in
                            TMRouteScreen(route: route, router: router)
                        }
                }
            }
            .navigationSplitViewStyle(.balanced)
            .background {
                ZStack {
                    Color(uiColor: .systemBackground)
                    TMWatermark()
                }
                .ignoresSafeArea()
            }
            .environment(\.tmSharedWatermark, sizeClass == .regular && TMSplitRoot.columnsCanBeClear)
        }
        .onChange(of: sizeClass, initial: true) { _, size in
            router.setExpanded(size == .regular)
        }
    }
}

/// The detail column: the chosen tag, or the placeholder before one is chosen.
struct TMDetailColumn: View {
    let router: TMRouter

    var body: some View {
        Group {
            if router.hasDetail {
                TMTagRoute(model: router.detail)
            } else {
                TMTagPlaceholder()
                    .background(TMScreenBackground())
                    .tmCharcoalBar()
            }
        }
        .tmClearColumnBackground()
    }
}

// MARK: - Routes

/// The screen for a route pushed onto a stack.
struct TMRouteScreen: View {
    let route: TMRoute
    let router: TMRouter

    var body: some View {
        Group {
            switch route.kind {
            case .screen(_, let screen):
                TMDestinationScreen(screen: screen)
            case .tag(let model):
                TMTagRoute(model: model)
            case .sheetMusic(let document, let summary):
                TMSheetMusicRoute(document: document, summary: summary, router: router)
            }
        }
        .tmClearColumnBackground()
    }
}

/// Home, the root of the list stack.
struct TMHomeRoute: View {
    let router: TMRouter

    var body: some View {
        TMScreens.home(router.home)
            .tmClearColumnBackground()
    }
}

/// A screen another screen asked for, over the model its route was made with.
struct TMDestinationScreen: View {
    let screen: TMScreenModel

    var body: some View {
        switch screen {
        case .browse(let model): TMScreens.browse(model)
        case .search(let model): TMScreens.search(model)
        case .settings(let model): TMScreens.settings(model)
        case .tagList(let model): TMScreens.tagList(model)
        case .results(let model): TMScreens.results(model)
        }
    }
}

/// The model behind each kind of destination.
enum TMScreenModel {
    /// Whether the screen is a list a linked tag could step through.
    var listsTags: Bool {
        switch self {
        case .tagList, .results: true
        case .browse, .search, .settings: false
        }
    }

    case browse(TMBrowseModel)
    case search(TMSearchModel)
    case settings(TMSettingsModel)
    case tagList(TMTagListModel)
    case results(TMQueryModel)

    @MainActor
    init(_ destination: TMDestination, navigator: TMRouteNavigator, router: TMRouter) {
        switch destination {
        case .browse:
            let model = TMBrowseModel(catalog: router.catalog, navigator: navigator)
            // Each page is its own list: a tag opened from Latest keeps stepping
            // through Latest when Rating shows, as each UIKit page was its own source.
            for page in model.pages {
                let pageNavigator = TMRouteNavigator(router: router, routeId: navigator.routeId)
                pageNavigator.source.listing = page
                page.navigator = pageNavigator
                page.owner = pageNavigator.source
            }
            navigator.source.listing = model
            self = .browse(model)
        case .search:
            self = .search(TMSearchModel(navigator: navigator))
        case .settings:
            self = .settings(TMSettingsModel(navigator: navigator, account: router.account, build: router.build))
        case .teachable:
            let model = TMTagListModel(kind: .teachable, navigator: navigator)
            navigator.source.listing = model
            self = .tagList(model)
        case .list(let key):
            let model = TMTagListModel(kind: .custom(key), navigator: navigator)
            navigator.source.listing = model
            self = .tagList(model)
        case .results(let query):
            let model = TMQueryModel(query: query, catalog: router.catalog, navigator: navigator)
            model.owner = navigator.source
            navigator.source.listing = model
            self = .results(model)
        }
    }
}

/// A tag, on a stack or in the detail column.
struct TMTagRoute: View {
    let model: TagDetailModel

    var body: some View {
        TagDetailScreen(model: model)
            .background(TMScreenBackground())
            .background {
                // Beside the floating list the column has a horizontal safe area; the pages keep to it.
                GeometryReader { proxy in
                    let horizontal = proxy.safeAreaInsets.leading > 0 || proxy.safeAreaInsets.trailing > 0
                    Color.clear.onChange(of: horizontal, initial: true) { _, now in
                        if model.hasHorizontalSafeArea != now { model.hasHorizontalSafeArea = now }
                    }
                }
            }
            .background(TMUndoHost(undoManager: model.undoManager))
            .tmCharcoalBar()
    }
}

/// The sheet music reader, with the full-screen toggle beside a list.
struct TMSheetMusicRoute: View {
    let document: TMSheetMusicDocument
    let summary: TagSummaryModel
    let router: TMRouter
    @State private var state = TMSheetMusicState()

    var body: some View {
        TMSheetMusicScreen(document: document, summary: summary, state: state)
            .tmCharcoalBar()
            .onAppear {
                state.toggleFullScreen = { [weak router] in router?.toggleFullScreen() }
            }
            .onChange(of: router.expanded, initial: true) { _, expanded in state.besideList = expanded }
            .onChange(of: router.isFullScreen, initial: true) { _, full in state.fullScreen = full }
            .onDisappear { if router.isFullScreen { router.toggleFullScreen() } }
    }
}

// MARK: - Screens and their bars

/// Each list screen with its bar, as it stands on a stack.
@MainActor
enum TMScreens {
    static func home(_ model: TMHomeModel) -> some View {
        TMLive {
            TMHomeScreen(model: model)
                .navigationTitle("Tag Master")
                .navigationBarTitleDisplayMode(.large)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) { TMEditButton(isEditing: bindEditing(model)).disabled(!model.canEdit) }
                    // Search stays outermost; Settings sits beside it as an icon, as on Android.
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        TMBarButton("gearshape", label: "Settings", action: model.settings)
                        TMBarButton("magnifyingglass", label: "Search", action: model.search)
                    }
                }
                .tmCharcoalBar(backTitle: "Home", homeTitle: true)
                .onAppear { model.reload() }
        }
    }

    static func tagList(_ model: TMTagListModel) -> some View {
        TMLive {
            TMTagListScreen(model: model)
                .navigationTitle(model.title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        if model.isCustom {
                            Menu {
                                Button("Rename list…", systemImage: "pencil", action: model.promptRename)
                                Button("Delete list…", systemImage: "trash", role: .destructive, action: model.confirmDelete)
                            } label: {
                                TMBarButton.symbol("ellipsis.circle", scale: .large)
                            }
                            .accessibilityLabel("List options")
                            .accessibilityIdentifier("list.menu")
                        }
                    }
                    // UIKit gave the list menu and Edit a glass each.
                    if #available(iOS 26.0, *) {
                        ToolbarSpacer(.fixed, placement: .topBarTrailing)
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        TMEditButton(isEditing: Binding(get: { model.isEditing }, set: { model.isEditing = $0 }))
                            .disabled(!model.canEdit)
                    }
                }
                .tmCharcoalBar(backTitle: model.backTitle)
                .onAppear { model.syncSelection() }
        }
    }

    static func browse(_ model: TMBrowseModel) -> some View {
        TMLive {
            TMBrowseScreen(model: model)
                .navigationTitle("Browse")
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
        }
    }

    static func results(_ model: TMQueryModel) -> some View {
        TMLive {
            TMQueryScreen(model: model)
                .navigationTitle(model.title)
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
                .onAppear { model.syncSelection() }
        }
    }

    static func search(_ model: TMSearchModel) -> some View {
        TMLive {
            TMSearchScreen(model: model)
                .navigationTitle("Search")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    // Runs the search with the current text and options; also the way to search by options alone.
                    ToolbarItem(placement: .topBarTrailing) {
                        TMBarButton("magnifyingglass", label: "Search", action: model.search)
                    }
                }
                .tmCharcoalBar()
        }
    }

    static func settings(_ model: TMSettingsModel) -> some View {
        TMLive {
            TMSettingsScreen(model: model)
                .navigationTitle("Settings")
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
                .onAppear { model.refresh() }
        }
    }

    private static func bindEditing(_ model: TMHomeModel) -> Binding<Bool> {
        Binding(get: { model.isEditing }, set: { model.isEditing = $0 })
    }
}

/// Rebuilds its content whenever a model it reads changes, wherever it is hosted.
struct TMLive<Content: View>: View {
    @ViewBuilder let content: () -> Content
    var body: some View { content() }
}

/// UIKit's Edit / Done bar button, driving a model's editing flag. (SwiftUI's
/// EditButton only follows the environment's edit mode, which these screens
/// derive from their models instead.)
struct TMEditButton: View {
    @Binding var isEditing: Bool

    var body: some View {
        Button {
            withAnimation { isEditing.toggle() }
        } label: {
            // iOS 26 draws UIKit's done-style item as a checkmark in its own glass.
            if isEditing {
                TMBarButton.symbol("checkmark", scale: .large)
            } else {
                Text("Edit")
            }
        }
        .accessibilityLabel(isEditing ? "Done" : "Edit")
    }
}

// MARK: - Column backgrounds

extension View {
    /// Inside the iPad split the columns let the one shared watermark show through.
    func tmClearColumnBackground() -> some View {
        modifier(TMClearColumnBackground())
    }
}

private struct TMClearColumnBackground: ViewModifier {
    @Environment(\.tmSharedWatermark) private var sharedWatermark

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), sharedWatermark {
            content
                .containerBackground(.clear, for: .navigation)
                .containerBackground(.clear, for: .navigationSplitView)
        } else {
            content
        }
    }
}

// MARK: - The bar

extension View {
    /// The app's charcoal navigation bar, with white titles and items, on the
    /// stack this screen stands on. `backTitle` is what the next screen's back
    /// button says; Home's title is set in the Wickhop handwriting face.
    func tmCharcoalBar(backTitle: String? = nil, homeTitle: Bool = false) -> some View {
        background(TMBarHook(backTitle: backTitle, homeTitle: homeTitle))
    }
}

/// Reaches the UIKit navigation controller SwiftUI draws the stack with, to give
/// its bar the app's appearance, set the back title and, on Home, the
/// handwriting title. SwiftUI has no modifier for any of the three.
private struct TMBarHook: UIViewControllerRepresentable {
    let backTitle: String?
    let homeTitle: Bool

    final class Hook: UIViewController {
        var backTitle: String?
        var homeTitle: TMHomeTitle?

        /// The screen's own controller: the ancestor the navigation controller holds.
        private var screen: UIViewController? {
            var controller: UIViewController? = self
            while let current = controller, !(current.parent is UINavigationController) {
                controller = current.parent
            }
            return controller
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            apply()
        }

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply()
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            if let homeTitle, let screen { homeTitle.detach(from: screen) }
        }

        func apply() {
            guard let screen, let navigation = screen.navigationController else { return }
            // The window carries the accent, as it did in UIKit; the bar's items stay white.
            if let window = navigation.view.window, window.tintColor != DPAppDelegate.accentColor() {
                window.tintColor = DPAppDelegate.accentColor()
            }
            TMBarAppearance.apply(to: navigation.navigationBar)
            if let backTitle, screen.navigationItem.backButtonTitle != backTitle {
                screen.navigationItem.backButtonTitle = backTitle
            }
            // The handwriting belongs to Home's own turn on top: Home re-renders while
            // covered (a favourite toggled in a pushed tag), and the bar is shared.
            if let homeTitle, navigation.topViewController === screen {
                homeTitle.attach(to: screen)
                if screen.navigationItem.titleView !== homeTitle.inlineLabel {
                    screen.navigationItem.titleView = homeTitle.inlineLabel
                }
            }
        }
    }

    func makeUIViewController(context: Context) -> Hook {
        let hook = Hook()
        hook.view.isHidden = true
        hook.view.isUserInteractionEnabled = false
        hook.homeTitle = homeTitle ? TMHomeTitle() : nil
        return hook
    }

    func updateUIViewController(_ hook: Hook, context: Context) {
        hook.backTitle = backTitle
        hook.apply()
    }
}

/// The charcoal bar every Tag Master stack wears.
@MainActor
enum TMBarAppearance {
    static func make() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(white: 55.0 / 255.0, alpha: 1)
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        return appearance
    }

    /// Gives `bar` the charcoal appearance once; Home's title adjusts it afterwards.
    static func apply(to bar: UINavigationBar) {
        guard bar.overrideUserInterfaceStyle != .dark || bar.isTranslucent else { return }
        let appearance = make()
        bar.standardAppearance = appearance
        bar.scrollEdgeAppearance = appearance
        bar.compactAppearance = appearance
        bar.compactScrollEdgeAppearance = appearance
        bar.tintColor = .white
        bar.overrideUserInterfaceStyle = .dark
        bar.barStyle = .black
        // With opaque chrome, keep UIKit's large-title host above the bar background.
        bar.isTranslucent = false
    }
}

/// Makes the detail's undo manager the one shake-to-undo reaches.
private struct TMUndoHost: UIViewRepresentable {
    let undoManager: UndoManager

    final class Host: UIView {
        var manager: UndoManager?
        override var undoManager: UndoManager? { manager }
        override var canBecomeFirstResponder: Bool { true }
        override func didMoveToWindow() {
            super.didMoveToWindow()
            if window != nil { DispatchQueue.main.async { [weak self] in _ = self?.becomeFirstResponder() } }
        }
    }

    func makeUIView(context: Context) -> Host {
        let host = Host()
        host.isUserInteractionEnabled = false
        return host
    }

    func updateUIView(_ host: Host, context: Context) {
        host.manager = undoManager
    }
}
