//
//  TMListHosting.swift
//  tagmaster
//
//  How the SwiftUI list screens reach the rest of the app. A screen's model
//  talks to a `TMNavigator` (open a tag, push a list, pop itself) instead of to
//  UIKit, so tests hand it a recording fake. `TMHostingController` carries a
//  screen on the UIKit navigation stack: it is the tag-list source the detail
//  steps through and it owns the bar items, which UIKit draws.
//

import ObjectiveC
import SwiftUI
import UIKit

/// A screen another screen can open.
enum TMDestination: Equatable {
    case browse
    case search
    case settings
    case teachable
    case list(String)
    case results(TMTagQuery)
}

/// Everything a list screen asks of the app around it.
@MainActor
protocol TMNavigator: AnyObject {
    /// True beside an open tag on iPad: rows drop their chevrons and the open tag's row stays lit.
    var isExpandedSplit: Bool { get }
    /// The tag the detail column shows, when expanded.
    var currentSplitTagId: Int? { get }
    func showTag(_ tagId: Int)
    func show(_ destination: TMDestination)
    /// Takes this screen off the stack without disturbing anything pushed above it.
    func removeScreen()
    func openURL(_ url: URL)
    func presentPrivacyChoices()
}

/// Implemented by models that list tags, so the detail can step through them.
@MainActor
protocol TMTagListing: AnyObject {
    var listedTagIds: [Int] { get }
    /// The row lit for the tag open beside the list, if any.
    var selectedTagId: Int? { get }
    func didStep(to tagId: Int)
}

/// Bar items a screen wants, rebuilt whenever the model they read changes.
@MainActor
struct TMChrome {
    var title: String?
    var largeTitle = false
    var backTitle: String?
    var left: [UIBarButtonItem] = []
    var right: [UIBarButtonItem] = []
    var titleView: UIView?
}

/// Hosts one list screen on the UIKit stack.
final class TMHostingController: UIHostingController<AnyView>, TMTagListSource {
    private let chrome: (TMHostingController) -> TMChrome
    weak var listing: TMTagListing?
    /// The models of screens that list no tags, for tests and the catalog.
    var searchModel: TMSearchModel?
    var settingsModel: TMSettingsModel?
    /// The view's own colour beside the shared iPad watermark (grouped screens).
    var pageColor: UIColor = .clear
    var onAppear: ((TMHostingController) -> Void)?
    var onWillDisappear: ((TMHostingController) -> Void)?
    var onDisappear: ((TMHostingController) -> Void)?
    /// Editing follows the UIKit Edit button; screens bind their List to it.
    var onEditingChanged: ((Bool) -> Void)?

    init<Content: View>(_ content: Content, chrome: @escaping (TMHostingController) -> TMChrome) {
        self.chrome = chrome
        super.init(rootView: AnyView(content.tmFollowsTintDimming()))
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("TMHostingController is created in code") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = DPAppDelegate.hasSharedBackground() ? pageColor : .clear
        applyChrome()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        applyChrome()
        onAppear?(self)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        onWillDisappear?(self)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        onDisappear?(self)
    }

    override func setEditing(_ editing: Bool, animated: Bool) {
        super.setEditing(editing, animated: animated)
        onEditingChanged?(editing)
    }

    /// Re-reads the chrome, and again whenever anything it read changes.
    func applyChrome() {
        let items = withObservationTracking {
            chrome(self)
        } onChange: { [weak self] in
            DispatchQueue.main.async { self?.applyChrome() }
        }
        let item = navigationItem
        if item.title != items.title { item.title = items.title }
        item.largeTitleDisplayMode = items.largeTitle ? .always : .never
        if let back = items.backTitle {
            if item.backBarButtonItem?.title != back {
                let backItem = UIBarButtonItem()
                backItem.title = back
                item.backBarButtonItem = backItem
            }
        }
        if item.leftBarButtonItems ?? [] != items.left { item.leftBarButtonItems = items.left }
        if item.rightBarButtonItems ?? [] != items.right { item.rightBarButtonItems = items.right }
        if item.titleView !== items.titleView { item.titleView = items.titleView }
    }

    // MARK: - TMTagListSource

    func tm_listedTagIds() -> [NSNumber] {
        (listing?.listedTagIds ?? []).map { NSNumber(value: $0) }
    }

    func tm_didStep(toTagId tagId: Int32) {
        listing?.didStep(to: Int(tagId))
    }

    /// The tag whose row is lit beside the detail, for Objective-C callers.
    @objc var tm_selectedTagId: NSNumber? { listing?.selectedTagId.map { NSNumber(value: $0) } }
}

/// The navigator for a screen on the UIKit stack.
@MainActor
final class TMUIKitNavigator: TMNavigator {
    weak var controller: UIViewController?

    init(_ controller: UIViewController? = nil) {
        self.controller = controller
    }

    var isExpandedSplit: Bool {
        guard let split = controller?.splitViewController else { return false }
        return !split.isCollapsed
    }

    var currentSplitTagId: Int? {
        guard let controller else { return nil }
        return DPAppDelegate.currentSplitTagId(for: controller)?.intValue
    }

    func showTag(_ tagId: Int) {
        guard let controller else { return }
        DPAppDelegate.showTag(withId: Int32(tagId), from: controller)
    }

    func show(_ destination: TMDestination) {
        guard let navigation = controller?.navigationController else { return }
        navigation.pushViewController(TMScreens.controller(for: destination), animated: true)
    }

    func removeScreen() {
        guard let controller, let navigation = controller.navigationController else { return }
        var stack = navigation.viewControllers
        guard let index = stack.firstIndex(of: controller) else { return }
        if index == stack.count - 1 {
            navigation.popViewController(animated: !UIAccessibility.isReduceMotionEnabled)
        } else {
            // Deeper down (a tag pushed from this list is on top): lift out only this screen.
            stack.remove(at: index)
            navigation.setViewControllers(stack, animated: false)
        }
    }

    func openURL(_ url: URL) {
        UIApplication.shared.open(url)
    }

    func presentPrivacyChoices() {
        guard let controller else { return }
        TelemetryConsent.present(from: controller)
    }
}

/// UIKit bar button items in the app's style (DPAppDelegate's symbol buttons).
@MainActor
enum TMBarItems {
    static func symbol(_ name: String, identifier: String? = nil, action: @escaping () -> Void) -> UIBarButtonItem {
        // DPAppDelegate's factory supplies the symbol configuration and the spoken label.
        let handler = TMBarAction(action)
        let item = DPAppDelegate.barButtonItem(withSystemName: name, target: handler,
                                               action: #selector(TMBarAction.run))!
        objc_setAssociatedObject(item, &TMBarAction.key, handler, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        if let identifier { item.accessibilityIdentifier = identifier }
        return item
    }

    static func menu(_ name: String, label: String, identifier: String, menu: UIMenu) -> UIBarButtonItem {
        let item = UIBarButtonItem(image: UIImage(systemName: name), menu: menu)
        item.accessibilityIdentifier = identifier
        item.accessibilityLabel = label
        return item
    }
}

/// A bar button's target: runs a closure, and lives as long as its item.
final class TMBarAction: NSObject {
    static var key: UInt8 = 0
    private let action: () -> Void
    init(_ action: @escaping () -> Void) { self.action = action }
    @objc func run() { action() }
}
