//
//  TagListsScreenCatalogTests.swift
//  tagmasterTests
//
//  Every list and navigation screen in every state worth looking at, mounted
//  the way the app mounts it: a navigation stack with Home at the root and the
//  opaque dark bar on iPhone, the double-column split with one shared watermark
//  on iPad. Captures land in SCREEN_CATALOG_DIR (see ScreenCatalog.swift); an
//  ordinary run just drives every state.
//
//  The iPhone run captures `iphone-*`, the iPad run `ipad-*`.
//

import XCTest
import UIKit
import ObjectiveC
@testable import tagmaster

/// Answers catalog queries (any `n=` request) with a page of seeded tags, so
/// Browse, Search results and Random Tag render real rows without the network.
/// Everything else stays blocked by TMBlockedNetwork underneath.
final class TMCatalogNetwork {
    enum Mode { case results(available: Int), empty, failure, hold }

    private let method: Method
    private var previous: IMP?
    static var mode: Mode = .results(available: 60)
    static let release = DispatchSemaphore(value: 0)

    init(mode: Mode) {
        TMCatalogNetwork.mode = mode
        method = class_getClassMethod(DPRemoteLocation.self,
                                      #selector(DPRemoteLocation.data(withContentsOf:)))!
        let blocked = method_getImplementation(method)
        typealias Fetch = @convention(c) (AnyObject, Selector, NSURL?, NSErrorPointer) -> NSData?
        let fallback = unsafeBitCast(blocked, to: Fetch.self)
        let selector = #selector(DPRemoteLocation.data(withContentsOf:))
        let answer: @convention(block) (AnyObject, NSURL?, NSErrorPointer) -> NSData? = { cls, url, error in
            guard let url = url as URL?, url.query?.contains("n=") == true else {
                return fallback(cls, selector, url as NSURL?, error)
            }
            switch TMCatalogNetwork.mode {
            case .failure:
                error?.pointee = NSError(domain: NSURLErrorDomain, code: NSURLErrorNotConnectedToInternet)
                return nil
            case .empty:
                return TMCatalogNetwork.page(start: 0, count: 0, available: 0)
            case .hold:
                TMCatalogNetwork.release.wait()
                return TMCatalogNetwork.page(start: 0, count: 0, available: 0)
            case .results(let available):
                let items = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
                let n = Int(items.first { $0.name == "n" }?.value ?? "20") ?? 20
                let start = (Int(items.first { $0.name == "start" }?.value ?? "1") ?? 1) - 1
                return TMCatalogNetwork.page(start: start, count: max(0, min(n, available - start)), available: available)
            }
        }
        previous = method_setImplementation(method, imp_implementationWithBlock(answer))
    }

    func uninstall() {
        if let previous { method_setImplementation(method, previous) }
        previous = nil
    }

    static let titles = ["Lost", "Heart of My Heart", "Goodbye My Coney Island Baby", "Sweet Adeline",
                         "Honey Little Lou", "Last Night Was the End of the World", "Hello Mary Lou",
                         "Wait 'Til the Sun Shines, Nellie", "Down Our Way", "Ring Those Christmas Bells"]

    static func page(start: Int, count: Int, available: Int) -> NSData {
        var xml = "<tags count=\"\(count)\" available=\"\(available)\">"
        for index in start..<(start + count) {
            let title = titles[index % titles.count]
            let aka = index % 3 == 1 ? "<AltTitle>\(title) (Tag)</AltTitle>" : ""
            let sheet = index % 2 == 0 ? "<SheetMusic type=\"pdf\">https://example.invalid/s\(index).pdf</SheetMusic>" : ""
            let tracks = index % 4 != 3 ? "<AllParts type=\"mp3\">https://example.invalid/a\(index).mp3</AllParts>" : ""
            xml += "<tag><id>\(2000 + index)</id><Title>\(title)</Title>\(aka)"
            xml += "<Rating>\(String(format: "%.2f", 3.0 + Double(index % 20) / 10))</Rating>"
            xml += "<Posted>Mon, \(1 + index % 27) Jan 2018</Posted><Downloaded>\(100 + index * 37)</Downloaded>"
            xml += "\(sheet)\(tracks)</tag>"
        }
        xml += "</tags>"
        return xml.data(using: .utf8)! as NSData
    }
}

@MainActor
final class TagListsScreenCatalogTests: TMBehaviorTestCase {
    private var catalogWindow: UIWindow?
    private var catalogNetwork: TMCatalogNetwork?
    private var isPad: Bool { UIDevice.current.userInterfaceIdiom == .pad }
    private var prefix: String { isPad ? "ipad" : "iphone" }

    nonisolated override func setUp() {
        super.setUp()
        ["search.sortBy", "search.sheetMusic", "search.learningTracks", "search.parts", "search.collection",
         "random.minDownloads", "random.minRating", "random.sheetMusic", "random.learningTracks"]
            .forEach { UserDefaults.standard.removeObject(forKey: $0) }
    }

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            tearDownCatalogWindow()
            catalogNetwork?.uninstall()
            catalogNetwork = nil
        }
        super.tearDown()
    }

    private func tearDownCatalogWindow() {
        if let root = catalogWindow?.rootViewController {
            root.presentedViewController?.dismiss(animated: false)
            ScreenCatalog.settle(0.1)
        }
        catalogWindow?.isHidden = true
        catalogWindow?.rootViewController = nil
        catalogWindow = nil
        DPAppDelegate.removeSharedBackground()
        autoreleasepool { RunLoop.current.run(until: Date()) }
    }

    private func network(_ mode: TMCatalogNetwork.Mode) {
        catalogNetwork?.uninstall()
        catalogNetwork = TMCatalogNetwork(mode: mode)
    }

    // MARK: - Shell (mirrors DPAppDelegate's launch)

    private func appearance() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(white: 55.0 / 255.0, alpha: 1)
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        return appearance
    }

    private func style(_ bar: UINavigationBar) {
        let appearance = appearance()
        bar.standardAppearance = appearance
        bar.scrollEdgeAppearance = appearance
        bar.compactAppearance = appearance
        bar.compactScrollEdgeAppearance = appearance
        bar.tintColor = .white
        bar.overrideUserInterfaceStyle = .dark
        bar.barStyle = .black
        bar.isTranslucent = false
    }

    /// Mounts the app's root: Home in a navigation stack, inside the split on iPad.
    @discardableResult
    private func mountApp(style interfaceStyle: UIUserInterfaceStyle = .light) -> UINavigationController {
        tearDownCatalogWindow()
        let window = ScreenCatalog.makeWindow(style: interfaceStyle)
        window.backgroundColor = .systemBackground
        window.tintColor = DPAppDelegate.accentColor()
        let navigation = UINavigationController()
        style(navigation.navigationBar)
        navigation.navigationBar.prefersLargeTitles = true
        navigation.pushViewController(Screens.home(), animated: false)
        if isPad {
            let split = UISplitViewController(style: .doubleColumn)
            DPAppDelegate.installSharedBackground(in: split.view)
            split.preferredDisplayMode = .oneBesideSecondary
            split.preferredSplitBehavior = .tile
            split.minimumPrimaryColumnWidth = 320
            split.maximumPrimaryColumnWidth = 400
            split.preferredPrimaryColumnWidthFraction = 0.36
            split.setViewController(navigation, for: .primary)
            let placeholderClass = NSClassFromString("TMTagPlaceholderController") as! UIViewController.Type
            let detail = UINavigationController(rootViewController: placeholderClass.init())
            style(detail.navigationBar)
            split.setViewController(detail, for: .secondary)
            window.rootViewController = split
        } else {
            window.rootViewController = navigation
        }
        window.makeKeyAndVisible()
        catalogWindow = window
        ScreenCatalog.settle(0.3)
        return navigation
    }

    private func push(_ controller: UIViewController, on navigation: UINavigationController) {
        navigation.pushViewController(controller, animated: false)
        ScreenCatalog.settle(0.3)
    }

    private func shoot(_ name: String, settle: TimeInterval = 0.5) {
        guard let catalogWindow else { return XCTFail("nothing mounted") }
        ScreenCatalog.capture("\(prefix)-\(name)", window: catalogWindow, settle: settle)
    }

    private func present(_ alert: UIViewController, from navigation: UINavigationController) {
        (navigation.topViewController ?? navigation).present(alert, animated: false)
        ScreenCatalog.settle(0.4)
    }

    private func seedFavoritesAndLists() {
        for (index, id) in [1809, 669, 1478, 122].enumerated() {
            seedCachedTag(id: Int32(id), title: ["Lost", "Heart of My Heart", "Honey Little Lou", "Sweet Adeline"][index],
                          withTracks: index != 2, withSheetMusic: index != 1)
        }
        seedLists(favorite: [1809, 669, 1478], teachable: [122, 1809],
                  lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [669, 1478]),
                          (key: "chorus-warmups-list", name: "Chorus warmups", ids: [122]),
                          (key: "empty-list-x1", name: "Road trip", ids: [])])
    }

    private func scrollToBottom(_ scroll: UIScrollView?) {
        guard let scroll else { return }
        scroll.layoutIfNeeded()
        let bottom = max(-scroll.adjustedContentInset.top,
                         scroll.contentSize.height - scroll.bounds.height + scroll.adjustedContentInset.bottom)
        scroll.setContentOffset(CGPoint(x: 0, y: bottom), animated: false)
        ScreenCatalog.settle(0.3)
    }

    private func firstScrollView(in controller: UIViewController?) -> UIScrollView? {
        guard let view = controller?.view else { return nil }
        return (view as? UIScrollView) ?? firstDescendant(of: view) { $0 is UIScrollView } as? UIScrollView
    }

    // MARK: - Home

    func testHome() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            mountApp(style: style)
            shoot("home-empty-\(suffix)")
            seedFavoritesAndLists()
            let navigation = mountApp(style: style)
            shoot("home-populated-\(suffix)")
            if style == .light {
                scrollToBottom(firstScrollView(in: navigation.topViewController))
                shoot("home-populated-bottom-light")
                clearLists()
            }
        }
    }

    func testHomeEditingAndPrompts() {
        seedFavoritesAndLists()
        let navigation = mountApp()
        Screens.setHomeEditing(navigation.topViewController!, true)
        ScreenCatalog.settle(0.4)
        shoot("home-editing-light")
        Screens.setHomeEditing(navigation.topViewController!, false)
        ScreenCatalog.settle(0.3)

        Screens.openTag(navigation.topViewController!)
        ScreenCatalog.settle(0.4)
        shoot("home-open-tag-alert-light")
        navigation.topViewController?.dismiss(animated: false)
        Screens.homeModel(navigation.topViewController!).openTagPrompt = nil
        ScreenCatalog.settle(0.3)

        let model = Screens.homeModel(navigation.topViewController!)
        model.newList()
        ScreenCatalog.settle(0.4)
        shoot("home-new-list-alert-light")
        navigation.topViewController?.dismiss(animated: false)
        model.namePrompt = nil
        ScreenCatalog.settle(0.3)

        model.rename("afterglow-set-k3f9")
        ScreenCatalog.settle(0.4)
        if let field = (navigation.topViewController?.presentedViewController as? UIAlertController)?.textFields?.first {
            field.text = "Chorus warmups"
            field.sendActions(for: .editingChanged)
            ScreenCatalog.settle(0.3)
        }
        shoot("home-rename-list-duplicate-alert-light")
        navigation.topViewController?.dismiss(animated: false)
        model.namePrompt = nil
        ScreenCatalog.settle(0.3)

        model.confirmDelete("afterglow-set-k3f9")
        ScreenCatalog.settle(0.4)
        shoot("home-delete-list-alert-light")
    }

    func testHomeRandomTagFailure() {
        network(.failure)
        let navigation = mountApp()
        Screens.randomTag(navigation.topViewController!)
        let deadline = Date().addingTimeInterval(3)
        while navigation.topViewController?.presentedViewController == nil, Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.02))
        }
        shoot("home-random-error-light")
    }

    // MARK: - Browse

    func testBrowse() {
        network(.results(available: 60))
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            let navigation = mountApp(style: style)
            let browse = Screens.browse()
            push(browse, on: navigation)
            ScreenCatalog.settle(0.6)
            shoot("browse-latest-\(suffix)", settle: 0.8)
            guard style == .light else { continue }
            for (index, page) in ["rating", "downloads", "classic"].enumerated() {
                Screens.selectBrowsePage(browse, index + 1)
                ScreenCatalog.settle(0.6)
                shoot("browse-\(page)-light", settle: 0.8)
            }
        }
    }

    func testBrowseEmptyAndError() {
        network(.empty)
        var navigation = mountApp()
        push(Screens.browse(), on: navigation)
        shoot("browse-empty-light", settle: 0.8)
        network(.failure)
        navigation = mountApp()
        push(Screens.browse(), on: navigation)
        shoot("browse-error-light", settle: 0.8)
    }

    // MARK: - Search and results

    func testSearch() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            let navigation = mountApp(style: style)
            push(Screens.search(), on: navigation)
            shoot("search-\(suffix)")
        }
        UserDefaults.standard.set(2, forKey: "search.sortBy")
        UserDefaults.standard.set(1, forKey: "search.sheetMusic")
        UserDefaults.standard.set(2, forKey: "search.parts")
        UserDefaults.standard.set(1, forKey: "search.collection")
        let navigation = mountApp()
        let search = Screens.search()
        push(search, on: navigation)
        Screens.setSearchText(search, "coney")
        shoot("search-filtered-light")
    }

    func testResults() {
        network(.results(available: 60))
        var navigation = mountApp()
        push(Screens.results(query: "coney"), on: navigation)
        shoot("results-light", settle: 0.8)
        let dark = mountApp(style: .dark)
        push(Screens.results(query: "coney"), on: dark)
        shoot("results-dark", settle: 0.8)

        network(.empty)
        navigation = mountApp()
        push(Screens.results(query: "zzz"), on: navigation)
        shoot("results-empty-light", settle: 0.8)

        network(.failure)
        navigation = mountApp()
        push(Screens.results(query: ""), on: navigation)
        shoot("results-error-light", settle: 0.8)
    }

    // MARK: - Settings

    func testSettings() {
        for style in [UIUserInterfaceStyle.light, .dark] {
            let suffix = style == .dark ? "dark" : "light"
            let navigation = mountApp(style: style)
            push(Screens.settings(), on: navigation)
            shoot("settings-\(suffix)")
        }
        seedFavoritesAndLists()
        let navigation = mountApp()
        push(Screens.settings(), on: navigation)
        shoot("settings-with-lists-light")
        scrollToBottom(firstScrollView(in: navigation.topViewController))
        shoot("settings-bottom-light")
        Screens.clearFavorites(navigation.topViewController!)
        ScreenCatalog.settle(0.4)
        shoot("settings-clear-favorites-alert-light")
    }

    // MARK: - Teachable and custom lists

    func testTeachable() {
        var navigation = mountApp()
        push(Screens.teachable(), on: navigation)
        shoot("teachable-empty-light")
        seedFavoritesAndLists()
        for style in [UIUserInterfaceStyle.light, .dark] {
            navigation = mountApp(style: style)
            push(Screens.teachable(), on: navigation)
            shoot("teachable-\(style == .dark ? "dark" : "light")")
        }
        navigation = mountApp()
        let teachable = Screens.teachable()
        push(teachable, on: navigation)
        teachable.setEditing(true, animated: false)
        ScreenCatalog.settle(0.3)
        shoot("teachable-editing-light")
    }

    func testCustomList() {
        seedFavoritesAndLists()
        var navigation = mountApp()
        push(Screens.list("empty-list-x1"), on: navigation)
        shoot("list-empty-light")
        for style in [UIUserInterfaceStyle.light, .dark] {
            navigation = mountApp(style: style)
            push(Screens.list("afterglow-set-k3f9"), on: navigation)
            shoot("list-\(style == .dark ? "dark" : "light")")
        }
        navigation = mountApp()
        let list = Screens.list("afterglow-set-k3f9")
        push(list, on: navigation)
        list.setEditing(true, animated: false)
        ScreenCatalog.settle(0.3)
        shoot("list-editing-light")
    }
}

/// How the catalog reaches each screen. The one place that knows which
/// implementation is current.
@MainActor
enum Screens {
    static func home() -> UIViewController { TMScreens.home() }
    static func setHomeEditing(_ home: UIViewController, _ editing: Bool) { home.setEditing(editing, animated: false) }
    static func homeModel(_ home: UIViewController) -> TMHomeModel { (home as! TMHostingController).listing as! TMHomeModel }
    static func openTag(_ home: UIViewController) { homeModel(home).openTagPrompt = TMOpenTagPrompt() }
    static func randomTag(_ home: UIViewController) { homeModel(home).randomTag() }
    static func browse() -> UIViewController { TMScreens.browse() }
    static func selectBrowsePage(_ browse: UIViewController, _ index: Int) {
        ((browse as! TMHostingController).listing as! TMBrowseModel).selectedIndex = index
    }
    static func search() -> UIViewController { TMScreens.search() }
    static func setSearchText(_ search: UIViewController, _ text: String) {
        (search as! TMHostingController).searchModel?.text = text
    }
    static func results(query: String) -> UIViewController { TMScreens.results(TMTagQuery(text: query)) }
    static func settings() -> UIViewController {
        TMScreens.settings(account: TMAccount(isSignedIn: { false }, signOut: {}),
                           build: TMBuildInfo(number: "", pullRequest: "?"))
    }
    static func clearFavorites(_ settings: UIViewController) {
        (settings as! TMHostingController).settingsModel?.clearTapped(.favorites)
    }
    static func teachable() -> UIViewController { TMScreens.teachable() }
    static func list(_ key: String) -> UIViewController { TMScreens.list(key: key) }
}
