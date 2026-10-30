//
//  TMShell.swift
//  tagmaster
//
//  Tag Master's app shell in SwiftUI: the App, a NavigationStack on iPhone, a
//  NavigationSplitView on iPad and a two-page book on an open iPhone Duo, each
//  screen's bar, and the charcoal bar the app has always had. The router
//  (TMRouter.swift) owns where things are.
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
                .preferredColorScheme(TagMasterApp.testAppearance)
                .onOpenURL { url in
                    if !DPAppDelegate.handleAuthURL(url) { router.open(url) }
                }
                // Every activation, the first included, offers Privacy choices until
                // they are made; leaving the active phase cancels an offer in flight.
                .onChange(of: scenePhase, initial: true) { _, phase in
                    if phase == .active {
                        TMPrivacyOffer.shared.sceneBecameActive()
                    } else {
                        TMPrivacyOffer.shared.sceneResigned()
                    }
                    ScreenTracker.shared.scenePhaseChanged(phase)
                }
        }
    }
}

/// Offers Privacy choices from the active window's root, as the UIKit delegate
/// did, until the user makes them. It never stacks over an alert or the sign-in
/// sheet: a root that is presenting ends this activation's offer, as it did in
/// UIKit. A root not yet able to present (no active key window, not in the
/// window, mid-transition) is retried briefly. The offer counts as made only once
/// UIKit has accepted the presentation, so a refused attempt leaves it pending.
@MainActor
final class TMPrivacyOffer {
    static let shared = TMPrivacyOffer()

    enum Readiness { case ready(UIViewController), notYet, busy }

    var hasChosen: () -> Bool = { TelemetryConsent.hasChosen }
    var readiness: () -> Readiness = { TMPrivacyOffer.activeRoot() }
    var present: (UIViewController) -> Void = { TelemetryConsent.present(from: $0) }
    var retryDelay: TimeInterval = 0.25
    var attempts = 40

    /// Shown this session: UIKit accepted the presentation.
    private(set) var shown = false
    /// An offer is waiting for a root that can present it.
    private(set) var pending = false
    private var generation = 0

    func sceneBecameActive() {
        guard !hasChosen(), !shown else { return }
        pending = true
        generation += 1
        attempt(generation, remaining: attempts, delay: 0)
    }

    /// Stale attempts stop; the offer stays pending for the next activation.
    func sceneResigned() {
        generation += 1
    }

    private func attempt(_ token: Int, remaining: Int, delay: TimeInterval) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [self] in
            guard token == generation, pending else { return }
            guard !hasChosen(), !shown else { pending = false; return }
            switch readiness() {
            case .ready(let root):
                present(root)
                if root.presentedViewController != nil {
                    shown = true
                    pending = false
                } else if remaining > 0 {
                    attempt(token, remaining: remaining - 1, delay: retryDelay)
                }
            case .notYet:
                if remaining > 0 { attempt(token, remaining: remaining - 1, delay: retryDelay) }
            case .busy:
                break
            }
        }
    }

    /// The foreground-active scene's key window root, if it can present now.
    static func activeRoot() -> Readiness {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        guard let scene = scenes.first(where: { $0.activationState == .foregroundActive }),
              let root = scene.keyWindow?.rootViewController,
              root.viewIfLoaded?.window != nil else { return .notYet }
        if root.presentedViewController != nil { return .busy }
        if root.transitionCoordinator != nil || root.isBeingPresented || root.isBeingDismissed { return .notYet }
        return .ready(root)
    }

    /// For tests: forget this session's offer.
    func reset() {
        shown = false
        pending = false
        generation += 1
    }
}

/// The unit-test host: no UI, only the cache serializers the app registers. Apps
/// built with the iOS 27 SDK must adopt the scene life cycle or they do not launch,
/// so it connects one plain scene, with no delegate, that the tests' windows join.
@objc(TMTestAppDelegate)
final class TMTestAppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        DPAppDelegate.configureCacheSerialization()
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
    }
}

// MARK: - Environment

/// How a screen's surface shows the barber pole. The shell sets it per column;
/// `TMScreenBackground` is the one place that acts on it.
enum TMBackdrop: Equatable {
    /// A stack of its own (iPhone, a collapsed split): the screen draws its colour
    /// and a watermark fitted to itself.
    case own
    /// Beside another column: the screen draws its colour and its slice of one
    /// watermark laid over the whole window, so both columns show a single pole.
    case windowSlice
    /// The iPad list column from iOS 26: UIKit's glass sidebar is the surface and
    /// the split's watermark lies beneath it, so the screen draws nothing.
    case glassColumn
}

extension TagMasterApp {
    /// UI tests ask for an appearance with `--appearance dark|light`: the
    /// simulator does not always take XCUIDevice's. Debug builds only.
    static var testAppearance: ColorScheme? {
#if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "--appearance"), index + 1 < arguments.count {
            return arguments[index + 1] == "dark" ? .dark : .light
        }
#endif
        return nil
    }
}

extension EnvironmentValues {
    @Entry var tmBackdrop: TMBackdrop = .own
}

// MARK: - Root

struct TMRootView: View {
    let router: TMRouter
    @Environment(\.horizontalSizeClass) private var horizontal
    @Environment(\.verticalSizeClass) private var vertical

    var body: some View {
        GeometryReader { geometry in
            switch TMShellLayout(idiom: UIDevice.current.userInterfaceIdiom, horizontal: horizontal,
                                 vertical: vertical, width: geometry.size.width,
                                 fold: TMBookRoot.uprightFold(in: geometry)) {
            case .stack: TMStackRoot(router: router)
            case .split: TMSplitRoot(router: router)
            case .book: TMBookRoot(router: router)
            }
        }
    }
}

/// Which shell a window gets. The router carries the open tag across when it
/// changes (an iPhone Duo opening, closing or turning).
enum TMShellLayout: Equatable {
    /// One stack: every phone window, a large phone in landscape included, and an
    /// open iPhone Duo held upright (two panes need 720 points, as on Android).
    case stack
    /// The system split: an iPad, whose split collapses by itself when it narrows.
    case split
    /// An iPhone Duo held open like a book: the list on one page, the tag on the
    /// other. UIKit floats a split's list over a window this size, so the book
    /// keeps the two side by side itself.
    case book

    /// `fold` is the device's upright fold across the window, if it has one. The
    /// book's divider has to run down it, so a fold too near either edge for two
    /// pages (a window beside another app) keeps the stack.
    init(idiom: UIUserInterfaceIdiom, horizontal: UserInterfaceSizeClass?,
         vertical: UserInterfaceSizeClass?, width: CGFloat, fold: CGRect? = nil) {
        if idiom == .pad {
            self = .split
        } else if horizontal == .regular, vertical == .regular, width >= 720,
                  fold.map({ Self.dividerFits($0, windowWidth: width) }) ?? true {
            self = .book
        } else {
            self = .stack
        }
    }

    /// Whether the book's divider can run down `fold` and leave each page 320 points.
    static func dividerFits(_ fold: CGRect, windowWidth: CGFloat) -> Bool {
        fold.midX >= 320 && windowWidth - fold.midX >= 320
    }
}

extension UITraitCollection {
    /// Whether menus and pickers float from their button, as on iPad: an iPad
    /// window of any width, or any window regular both ways (an iPhone Duo
    /// opened out). A large phone in landscape keeps its phone sheets.
    var tmFloatsPopovers: Bool {
        userInterfaceIdiom == .pad || (horizontalSizeClass == .regular && verticalSizeClass == .regular)
    }
}

/// Gives the window the accent as soon as the root is in it, before its first
/// frame, as the UIKit delegate did when it made the window.
private struct TMWindowTint: UIViewRepresentable {
    final class Probe: UIView {
        override func didMoveToWindow() {
            super.didMoveToWindow()
            if let window, window.tintColor != DPAppDelegate.accentColor {
                window.tintColor = DPAppDelegate.accentColor
            }
        }
    }

    func makeUIView(context: Context) -> Probe {
        let probe = Probe()
        probe.isUserInteractionEnabled = false
        probe.isAccessibilityElement = false
        return probe
    }

    func updateUIView(_ probe: Probe, context: Context) {}
}

/// iPhone, and an iPhone Duo folded shut: one stack, Home at its root.
struct TMStackRoot: View {
    @Bindable var router: TMRouter
    @State private var margins = TMTableMargins()

    var body: some View {
        let column = TMColumnTraits(margins: margins)
        NavigationStack(path: $router.path) {
            TMHomeRoute(router: router, column: column)
                .navigationDestination(for: TMRoute.self) { route in
                    TMRouteScreen(route: route, router: router, column: column)
                }
        }
        .background(TMTableMarginReader { margins = $0 })
        .background(TMWindowTint())
        .onAppear { router.useStack() }
    }
}

/// iPad: the list stack beside the tag, one watermark behind both.
struct TMSplitRoot: View {
    /// The list column is a glass sidebar over the split's watermark from iOS 26.
    /// Before that UIKit's list column was a clear view over the watermark, which its
    /// screens reproduce exactly by drawing their colour and window slice; that way
    /// no container's own fill can hide the pole.
    static var columnsCanBeClear: Bool {
        if #available(iOS 26.0, *) { true } else { false }
    }

    /// Each column's backdrop.
    static func backdrops(regular: Bool, columnsCanBeClear: Bool = columnsCanBeClear)
        -> (list: TMBackdrop, detail: TMBackdrop) {
        guard regular else { return (.own, .own) }
        return (columnsCanBeClear ? .glassColumn : .windowSlice, .windowSlice)
    }

    @Bindable var router: TMRouter
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var listMargins = TMTableMargins(plain: 16, grouped: 20)
    @State private var detailMargins = TMTableMargins()

    var body: some View {
        GeometryReader { geometry in
            let backdrops = TMSplitRoot.backdrops(regular: sizeClass == .regular)
            NavigationSplitView(columnVisibility: $router.columnVisibility,
                                preferredCompactColumn: $router.preferredCompactColumn) {
                // A stack does not hand its environment to the screens pushed onto it,
                // so each column's root and destinations are given theirs directly.
                let list = TMColumnTraits(backdrop: backdrops.list, margins: listMargins)
                NavigationStack(path: $router.path) {
                    TMHomeRoute(router: router, column: list)
                        .navigationDestination(for: TMRoute.self) { route in
                            TMRouteScreen(route: route, router: router, column: list)
                        }
                }
                .background(TMTableMarginReader { listMargins = $0 })
                // A comfortable list on both 11- and 13-inch iPads: 36% of the width, 320–400 points.
                .navigationSplitViewColumnWidth(min: 320,
                                                ideal: min(max(geometry.size.width * 0.36, 320), 400),
                                                max: 400)
            } detail: {
                let detail = TMColumnTraits(backdrop: backdrops.detail, margins: detailMargins)
                NavigationStack(path: $router.detailPath) {
                    TMDetailColumn(router: router, column: detail)
                        .navigationDestination(for: TMRoute.self) { route in
                            TMRouteScreen(route: route, router: router, column: detail)
                        }
                }
                .background(TMTableMarginReader { detailMargins = $0 })
            }
            .navigationSplitViewStyle(.balanced)
            .background {
                ZStack {
                    Color(uiColor: .systemBackground)
                    TMWatermark()
                }
                .ignoresSafeArea()
            }
        }
        .background(TMWindowTint())
        .onChange(of: sizeClass, initial: true) { _, size in
            router.setExpanded(size == .regular)
        }
    }
}

/// An iPhone Duo held open like a book: Home's stack on the leading page and the
/// tag (or the placeholder) on the trailing one, both always showing, one
/// watermark behind both. The divider runs down the crease; half shut, both pages
/// keep their content clear of it. The tag's stack, along the display's edge, is
/// the one whose bar items move into the side bar. Reading full screen hides the list.
struct TMBookRoot: View {
    @Bindable var router: TMRouter
    @Environment(\.displayScale) private var displayScale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var listMargins = TMTableMargins()
    @State private var detailMargins = TMTableMargins()
    /// The hinge's state: folding changes the fold's region but not the window's
    /// geometry, so the layout reads this to look at the fold again.
    @State private var hingeStatus = 0

    /// The leading page's width: up to the middle of the device's upright fold
    /// (flat or half shut), an even split; without one, 36% of the window,
    /// 320–400 points, as the iPad's list.
    static func listWidth(windowWidth: CGFloat, fold: CGRect?) -> CGFloat {
        if let fold, TMShellLayout.dividerFits(fold, windowWidth: windowWidth) { return fold.midX }
        return min(max(windowWidth * 0.36, 320), 400)
    }

    /// The device's upright fold across this window, active or flat, if it has one.
    static func uprightFold(in geometry: GeometryProxy) -> CGRect? {
        guard #available(iOS 27.1, *) else { return nil }
        return geometry.reservedRegions(kind: .division, options: .includeInactive)
            .map(\.frame)
            .first { isUpright($0, in: geometry.size) }
    }

    /// How far each page keeps its content from the divider while the device is
    /// half shut: half the fold's region, whose frame holds the crease and the
    /// margins iOS asks interactive content to keep from it. Flat, the display is
    /// one sheet of glass and the columns keep their usual margins.
    static func creaseClearance(in geometry: GeometryProxy) -> CGFloat {
        guard #available(iOS 27.1, *),
              let crease = geometry.reservedRegions(kind: .division).first(where: { isUpright($0.frame, in: geometry.size) })
        else { return 0 }
        return creaseClearance(band: crease.frame)
    }

    static func creaseClearance(band: CGRect) -> CGFloat {
        band.width / 2
    }

    private static func isUpright(_ frame: CGRect, in size: CGSize) -> Bool {
        frame.height > frame.width && frame.height >= size.height / 2
    }

    var body: some View {
        GeometryReader { geometry in
            let _ = hingeStatus
            let showsList = router.columnVisibility != .detailOnly
            let clearance = TMBookRoot.creaseClearance(in: geometry)
            let list = TMColumnTraits(backdrop: .windowSlice, margins: listMargins, crease: (.trailing, clearance))
            // Full screen, the tag's page starts at the display's edge, not at the crease.
            let detail = TMColumnTraits(backdrop: .windowSlice, margins: detailMargins,
                                        crease: (.leading, showsList ? clearance : 0))
            HStack(spacing: 0) {
                if showsList {
                    NavigationStack(path: $router.path) {
                        TMHomeRoute(router: router, column: list)
                            .navigationDestination(for: TMRoute.self) { route in
                                TMRouteScreen(route: route, router: router, column: list)
                            }
                    }
                    .background(TMTableMarginReader { listMargins = $0 })
                    .frame(width: TMBookRoot.listWidth(windowWidth: geometry.size.width,
                                                       fold: TMBookRoot.uprightFold(in: geometry)))
                    .transition(.move(edge: .leading))
                    Rectangle()
                        .fill(Color(uiColor: .separator))
                        .frame(width: 1 / displayScale)
                        .ignoresSafeArea()
                }
                NavigationStack(path: $router.detailPath) {
                    TMDetailColumn(router: router, column: detail)
                        .navigationDestination(for: TMRoute.self) { route in
                            TMRouteScreen(route: route, router: router, column: detail)
                        }
                }
                .background(TMTableMarginReader { detailMargins = $0 })
            }
            .animation(reduceMotion ? nil : .default, value: showsList)
            .background {
                ZStack {
                    Color(uiColor: .systemBackground)
                    TMWatermark()
                }
                .ignoresSafeArea()
            }
        }
        .background(TMWindowTint())
        .hingeStatus($hingeStatus)
        .onAppear { router.setExpanded(true) }
    }
}

/// What a column tells the screens in it: their backdrop and UITableView's
/// margins at the column's size.
struct TMColumnTraits {
    var backdrop: TMBackdrop = .own
    var margins = TMTableMargins()
    /// The column's side that meets an iPhone Duo's crease, in the book, and how far
    /// its content keeps from it (zero while the device lies flat).
    var crease: (edge: HorizontalEdge, clearance: CGFloat)?
    /// A phone's single stack, before its margins are measured.
    static let stack = TMColumnTraits()
}

extension View {
    /// A route's screen in its column: the column's traits, the route's one tint
    /// follower, and inside the split a clear column background, so the split's
    /// watermark lies under the glass list column (the detail column's container
    /// reaches beneath it too; its screens draw their own surface). The clear
    /// background is outermost: `containerBackground` only takes effect on the
    /// root of a column's view.
    func tmRoute(in column: TMColumnTraits) -> some View {
        environment(\.tmBackdrop, column.backdrop)
            .environment(\.tmTableMargin, column.margins.plain)
            .environment(\.tmGroupedMargin, column.margins.grouped)
            .environment(\.tmGroupedTextInset, column.margins.groupedText)
            .tmFollowsUIKitTint()
            .background { if let crease = column.crease { TMCreaseInset(edge: crease.edge, clearance: crease.clearance) } }
            .tmClearColumnBackground(column.backdrop != .own)
    }
}

/// Keeps a book column's bar and screens off the crease: the clearance becomes
/// the column's navigation controller's extra safe area on that side, which
/// UIKit hands to its bar and every screen on it. SwiftUI's own padding stops
/// at the stack's edge and never reaches the UIKit views inside.
private struct TMCreaseInset: UIViewControllerRepresentable {
    let edge: HorizontalEdge
    let clearance: CGFloat

    final class Hook: UIViewController {
        var edge: HorizontalEdge = .leading
        var clearance: CGFloat = 0

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply(animated: false)
        }

        func apply(animated: Bool) {
            var controller: UIViewController? = parent
            while let current = controller, !(current is UINavigationController) { controller = current.parent }
            guard let navigation = controller else { return }
            let leftToRight = navigation.view.effectiveUserInterfaceLayoutDirection == .leftToRight
            let left = (edge == .leading) == leftToRight
            var insets = UIEdgeInsets.zero
            if left { insets.left = clearance } else { insets.right = clearance }
            guard navigation.additionalSafeAreaInsets != insets else { return }
            let change = {
                navigation.additionalSafeAreaInsets = insets
                navigation.view.layoutIfNeeded()
            }
            if animated, !UIAccessibility.isReduceMotionEnabled {
                UIView.animate(withDuration: 0.3, animations: change)
            } else {
                change()
            }
        }
    }

    func makeUIViewController(context: Context) -> Hook {
        let hook = Hook()
        hook.view.isHidden = true
        hook.view.isUserInteractionEnabled = false
        hook.edge = edge
        hook.clearance = clearance
        return hook
    }

    func updateUIViewController(_ hook: Hook, context: Context) {
        hook.edge = edge
        hook.clearance = clearance
        hook.apply(animated: true)
    }
}

/// The detail column: the chosen tag, or the placeholder before one is chosen.
struct TMDetailColumn: View {
    let router: TMRouter
    var column = TMColumnTraits.stack

    var body: some View {
        Group {
            if router.hasDetail {
                // A tag carried in from the stack is a different model: a fresh screen for it.
                TMTagRoute(model: router.detail)
                    .id(ObjectIdentifier(router.detail))
            } else {
                TMTagPlaceholder()
                    .background(TMScreenBackground())
                    .tmCharcoalBar()
            }
        }
        .tmRoute(in: column)
    }
}

// MARK: - Routes

/// The screen for a route pushed onto a stack.
struct TMRouteScreen: View {
    let route: TMRoute
    let router: TMRouter
    var column = TMColumnTraits.stack

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
        .tmRoute(in: column)
    }
}

/// Home, the root of the list stack.
struct TMHomeRoute: View {
    let router: TMRouter
    var column = TMColumnTraits.stack

    var body: some View {
        TMScreens.home(router.home)
            // Home is where a review may be asked for after a finished task,
            // unless a tag is open beside it (a collapsed split shows none beside it).
            .reviewCalmScreen(router.currentSplitTagId == nil)
            .tmRoute(in: column)
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
        // Each page draws its own backdrop (tmTabPageBackground); a second one here
        // would show through the clear pages and double the watermark.
        TagDetailScreen(model: model)
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
            .analyticsScreen("sheet_music")
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
                .analyticsScreen("home")
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
                                TMBarLabel("ellipsis.circle", scale: .large)
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
                .analyticsScreen(model.isCustom ? "tag_list" : "teachable_tags")
        }
    }

    static func browse(_ model: TMBrowseModel) -> some View {
        TMLive {
            TMBrowseScreen(model: model)
                .navigationTitle("Browse")
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
                .analyticsScreen("browse")
        }
    }

    static func results(_ model: TMQueryModel) -> some View {
        TMLive {
            TMQueryScreen(model: model)
                .navigationTitle(model.title)
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
                .onAppear { model.syncSelection() }
                .analyticsScreen("search_results")
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
                .analyticsScreen("search")
        }
    }

    static func settings(_ model: TMSettingsModel) -> some View {
        TMLive {
            TMSettingsScreen(model: model)
                .navigationTitle("Settings")
                .navigationBarTitleDisplayMode(.inline)
                .tmCharcoalBar()
                .onAppear { model.refresh() }
                .analyticsScreen("settings")
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
            TMBarLabel {
                // iOS 26 draws UIKit's done-style item as a checkmark in its own glass;
                // earlier systems drew UIKit's bold "Done".
                if isEditing {
                    if #available(iOS 26.0, *) {
                        TMBarButton.symbol("checkmark", scale: .large)
                    } else {
                        Text("Done").fontWeight(.semibold)
                    }
                } else {
                    Text("Edit").fontWeight(.medium)
                }
            }
        }
        .accessibilityLabel(isEditing ? "Done" : "Edit")
    }
}

// MARK: - Column backgrounds

extension View {
    /// The glass list column shows through its screens to the split's watermark.
    func tmClearColumnBackground(_ clear: Bool) -> some View {
        modifier(TMClearColumnBackground(clear: clear))
    }
}

private struct TMClearColumnBackground: ViewModifier {
    let clear: Bool

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *), clear {
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
        // SwiftUI rewrites the bar's title attributes when it updates the screen's
        // toolbar (a tint dimming behind an alert does); the hook's turn comes after.
        DispatchQueue.main.async { [weak hook] in hook?.apply() }
    }
}

/// The charcoal bar every Tag Master stack wears.
@MainActor
enum TMBarAppearance {
    static let charcoal = UIColor(white: 55.0 / 255.0, alpha: 1)

    /// Gives `bar` the charcoal appearance, checking and repairing each property it
    /// owns on its own: SwiftUI rewrites some (the title colours) and not others, so
    /// one being right says nothing about the rest. Fonts and offsets in the title
    /// attributes (Home's handwriting) are kept; only their colour is the bar's.
    static func apply(to bar: UINavigationBar) {
        let slots: [ReferenceWritableKeyPath<UINavigationBar, UINavigationBarAppearance?>] =
            [\.scrollEdgeAppearance, \.compactAppearance, \.compactScrollEdgeAppearance]
        if let repaired = repaired(bar.standardAppearance) { bar.standardAppearance = repaired }
        for slot in slots {
            let current = bar[keyPath: slot] ?? bar.standardAppearance
            if bar[keyPath: slot] == nil || repaired(current) != nil {
                bar[keyPath: slot] = repaired(current) ?? current.copy()
            }
        }
        if bar.tintColor != .white { bar.tintColor = .white }
        // UIKit's back button stayed white behind an alert; the icons that did grey
        // (Home's) grey through TMBarButton.ink, not through the bar's tint.
        if bar.tintAdjustmentMode != .normal { bar.tintAdjustmentMode = .normal }
        if bar.overrideUserInterfaceStyle != .dark { bar.overrideUserInterfaceStyle = .dark }
        if bar.barStyle != .black { bar.barStyle = .black }
        // With opaque chrome, keep UIKit's large-title host above the bar background.
        if bar.isTranslucent { bar.isTranslucent = false }
    }

    /// A corrected copy of `appearance`, or nil when it is already right.
    static func repaired(_ appearance: UINavigationBarAppearance) -> UINavigationBarAppearance? {
        let white = UIColor.white
        let backgroundRight = appearance.backgroundColor == charcoal && appearance.backgroundEffect == nil
        let titleRight = appearance.titleTextAttributes[.foregroundColor] as? UIColor == white
        let largeRight = appearance.largeTitleTextAttributes[.foregroundColor] as? UIColor == white
        guard !(backgroundRight && titleRight && largeRight) else { return nil }
        let fixed = appearance.copy()
        if !backgroundRight {
            fixed.configureWithOpaqueBackground()
            fixed.backgroundColor = charcoal
            // configureWithOpaqueBackground resets the titles; keep what they carried.
            fixed.titleTextAttributes = appearance.titleTextAttributes
            fixed.largeTitleTextAttributes = appearance.largeTitleTextAttributes
        }
        fixed.titleTextAttributes[.foregroundColor] = white
        fixed.largeTitleTextAttributes[.foregroundColor] = white
        return fixed
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
