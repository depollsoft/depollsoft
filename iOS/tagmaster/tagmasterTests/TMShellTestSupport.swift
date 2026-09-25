//
//  TMShellTestSupport.swift
//  tagmasterTests
//
//  Puts the SwiftUI screens on a plain UIKit navigation stack for the behaviour
//  tests, the way the shell's NavigationStack carries them: each screen is the
//  same view with the same bar (TMScreens), hosted by a controller the tests can
//  hold. Screens made without a navigator get a real TMRouter, so what they ask
//  to open lands on its path.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

/// One list screen hosted for a test.
@MainActor
final class TMHostedScreen: UIHostingController<AnyView>, TMTagListSource {
    /// The router a screen made without a navigator reports to.
    private(set) var router: TMRouter?
    private(set) weak var listing: TMTagListing?
    private(set) var searchModel: TMSearchModel?
    private(set) var settingsModel: TMSettingsModel?

    init(_ view: some View, router: TMRouter?, listing: TMTagListing? = nil,
         search: TMSearchModel? = nil, settings: TMSettingsModel? = nil) {
        self.router = router
        self.listing = listing
        searchModel = search
        settingsModel = settings
        super.init(rootView: AnyView(view))
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("created in code") }

    func tm_listedTagIds() -> [NSNumber] {
        (listing?.listedTagIds ?? []).map { NSNumber(value: $0) }
    }

    func tm_didStep(toTagId tagId: Int32) {
        listing?.didStep(to: Int(tagId))
    }
}

extension TMScreens {
    /// A navigator for a screen: the test's own, or a route navigator on a fresh router.
    private static func navigator(_ custom: TMNavigator?) -> (TMNavigator, TMRouter?, TMRouteNavigator?) {
        if let custom { return (custom, nil, nil) }
        let router = TMRouter()
        let navigator = TMRouteNavigator(router: router, routeId: nil)
        return (navigator, router, navigator)
    }

    static func home(navigator custom: TMNavigator? = nil, catalog: TMCatalog = .live) -> TMHostedScreen {
        let (navigator, router, route) = navigator(custom)
        let model = TMHomeModel(catalog: catalog, navigator: navigator)
        route?.source.listing = model
        return TMHostedScreen(home(model), router: router, listing: model)
    }

    static func tagList(_ kind: TMTagListModel.Kind, navigator custom: TMNavigator? = nil) -> TMHostedScreen {
        let (navigator, router, route) = navigator(custom)
        let model = TMTagListModel(kind: kind, navigator: navigator)
        route?.source.listing = model
        return TMHostedScreen(tagList(model), router: router, listing: model)
    }

    static func list(key: String) -> TMHostedScreen { tagList(.custom(key)) }
    static func teachable() -> TMHostedScreen { tagList(.teachable) }

    static func browse(navigator custom: TMNavigator? = nil, catalog: TMCatalog = .live) -> TMHostedScreen {
        let (navigator, router, route) = navigator(custom)
        let model = TMBrowseModel(catalog: catalog, navigator: navigator)
        if let route {
            model.pages.forEach { $0.owner = route.source }
            route.source.listing = model
        }
        return TMHostedScreen(browse(model), router: router, listing: model)
    }

    static func results(_ query: TMTagQuery, navigator custom: TMNavigator? = nil,
                        catalog: TMCatalog = .live, preloaded: [DPTag]? = nil) -> TMHostedScreen {
        let (navigator, router, route) = navigator(custom)
        let model = TMQueryModel(query: query, catalog: catalog, navigator: navigator, preloaded: preloaded)
        if let route {
            model.owner = route.source
            route.source.listing = model
        }
        return TMHostedScreen(results(model), router: router, listing: model)
    }

    static func search(navigator custom: TMNavigator? = nil) -> TMHostedScreen {
        let (navigator, router, _) = navigator(custom)
        let model = TMSearchModel(navigator: navigator)
        return TMHostedScreen(search(model), router: router, search: model)
    }

    static func settings(navigator custom: TMNavigator? = nil, account: TMAccount = .firebase,
                         build: TMBuildInfo = .bundle) -> TMHostedScreen {
        let (navigator, router, _) = navigator(custom)
        let model = TMSettingsModel(navigator: navigator, account: account, build: build)
        return TMHostedScreen(settings(model), router: router, settings: model)
    }

    static func isHome(_ screen: UIViewController) -> Bool {
        (screen as? TMHostedScreen)?.listing is TMHomeModel
    }
}

extension TMRoute {
    /// Whether this route opens the list screen for `key` (Home's list rows reuse Teachable's and the user's).
    func opensList(key: String) -> Bool {
        switch destination {
        case .teachable: return key == TMTagLists.teachableKey
        case .list(let listKey): return listKey == key
        default: return false
        }
    }
}

/// Hosts the SwiftUI tag detail on a UIKit stack for the tests, the way the
/// shell's stacks carry it, and answers what the tests ask of a detail.
@MainActor
final class TagDetailViewController: UIViewController {
    let model: TagDetailModel
    private let hosting: UIHostingController<AnyView>
    /// What the detail asked to open, for tests that follow it.
    private(set) var shownTags: [Int32] = []
    private(set) var shownLists: [String] = []

    init(model: TagDetailModel? = nil) {
        let model = model ?? TagDetailModel()
        self.model = model
        hosting = UIHostingController(rootView: AnyView(TMTagRoute(model: model)))
        super.init(nibName: nil, bundle: nil)
        model.navigator = TMDetailNavigator(
            showTag: { [weak self] id in
                self?.shownTags.append(id)
                self?.model.show(tagId: id)
            },
            showList: { [weak self] key in self?.shownLists.append(key) },
            showSheetMusic: { _ in })
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("created in code") }

    var tagId: Int32 {
        get { model.tagId }
        set { model.show(tagId: newValue) }
    }

    weak var source: TMTagListSource? {
        get { model.source }
        set { model.source = newValue }
    }

    override var navigationItem: UINavigationItem { hosting.navigationItem }
    override var undoManager: UndoManager? { model.undoManager }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(hosting)
        hosting.view.backgroundColor = .clear
        hosting.view.frame = view.bounds
        hosting.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(hosting.view)
        hosting.didMove(toParent: self)
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        let expanded = splitViewController.map { !$0.isCollapsed } ?? false
        if model.expanded != expanded { model.expanded = expanded }
    }

    func stepToPreviousTag() { model.stepToPreviousTag() }
    func stepToNextTag() { model.stepToNextTag() }
}

@MainActor
extension TMBehaviorTestCase {
    /// Mounts the app's own shell around `router`: the phone stack, or the iPad split.
    @discardableResult
    func mountShell(_ router: TMRouter, split: Bool = false,
                    size: CGSize = TMBehaviorTestCase.portrait) -> UIWindow {
        let root: AnyView = split ? AnyView(TMSplitRoot(router: router)) : AnyView(TMStackRoot(router: router))
        if split {
            let splitWindow = ScreenCatalog.makeWindow()
            splitWindow.rootViewController = UIHostingController(rootView: root)
            splitWindow.makeKeyAndVisible()
            window = splitWindow
            settle()
            return splitWindow
        }
        return mount(UIHostingController(rootView: root), size: size)
    }
}

extension UIDriver {
    /// Whether the control labelled `label` is on screen and can be used.
    func isEnabled(label: String) -> Bool {
        let matches = elements.filter { $0.isAccessibilityElement && $0.accessibilityLabel == label }
        guard let element = matches.last else { return false }
        return !element.accessibilityTraits.contains(.notEnabled)
    }
}
