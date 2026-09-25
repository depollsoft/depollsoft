//
//  TMScreens.swift
//  tagmaster
//
//  Builds each SwiftUI list screen with its model, navigator and bar items, for
//  the UIKit stack that still carries them.
//

import SwiftUI
import UIKit

@MainActor
@objc final class TMScreens: NSObject {
    // MARK: - Home

    @objc nonisolated static func home() -> UIViewController { MainActor.assumeIsolated { home(navigator: nil) } }

    /// Home; `navigator` replaces the UIKit one (tests record what Home asks for).
    static func home(navigator custom: TMNavigator?, catalog: TMCatalog = .live) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMHomeModel(catalog: catalog, navigator: custom ?? navigator)
        let search = TMBarItems.symbol("magnifyingglass") { [weak model] in model?.search() }
        let settings = TMBarItems.symbol("gearshape") { [weak model] in model?.settings() }
        let title = TMHomeTitle()
        let controller = TMHostingController(TMHomeScreen(model: model)) { controller in
            let edit = controller.editButtonItem
            edit.isEnabled = model.canEdit
            if controller.isEditing != model.isEditing { controller.setEditing(model.isEditing, animated: true) }
            // Search stays outermost; Settings sits beside it as an icon, as on Android.
            return TMChrome(title: "Tag Master", largeTitle: true, backTitle: "Home",
                            left: [edit], right: [search, settings], titleView: title.inlineLabel)
        }
        controller.onEditingChanged = { model.isEditing = $0 }
        controller.onAppear = { controller in
            model.reload()
            title.attach(to: controller)
        }
        controller.onWillDisappear = { controller in title.detach(from: controller) }
        controller.listing = model
        navigator.controller = controller
        return controller
    }

    @objc nonisolated static func isHome(_ controller: UIViewController) -> Bool {
        MainActor.assumeIsolated { (controller as? TMHostingController)?.listing is TMHomeModel }
    }

    // MARK: - Tag lists

    @objc nonisolated static func teachable() -> UIViewController { MainActor.assumeIsolated { tagList(.teachable) } }

    @objc(listWithKey:) nonisolated static func list(key: String) -> UIViewController {
        MainActor.assumeIsolated { tagList(.custom(key)) }
    }

    static func tagList(_ kind: TMTagListModel.Kind, navigator custom: TMNavigator? = nil) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMTagListModel(kind: kind, navigator: custom ?? navigator)
        var overflow: UIBarButtonItem?
        if model.isCustom {
            overflow = TMBarItems.menu("ellipsis.circle", label: "List options", identifier: "list.menu", menu: UIMenu(children: [
                UIAction(title: "Rename list…", image: UIImage(systemName: "pencil")) { [weak model] _ in model?.promptRename() },
                UIAction(title: "Delete list…", image: UIImage(systemName: "trash"), attributes: .destructive) { [weak model] _ in
                    model?.confirmDelete()
                },
            ]))
        }
        let controller = TMHostingController(TMTagListScreen(model: model)) { controller in
            let edit = controller.editButtonItem
            edit.isEnabled = model.canEdit
            if controller.isEditing != model.isEditing { controller.setEditing(model.isEditing, animated: true) }
            return TMChrome(title: model.title, backTitle: model.backTitle, right: [edit] + (overflow.map { [$0] } ?? []))
        }
        controller.onEditingChanged = { model.isEditing = $0 }
        controller.onAppear = { _ in model.syncSelection() }
        controller.listing = model
        navigator.controller = controller
        return controller
    }

    // MARK: - Catalog

    static func browse(navigator custom: TMNavigator? = nil, catalog: TMCatalog = .live) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMBrowseModel(catalog: catalog, navigator: custom ?? navigator)
        let controller = TMHostingController(TMBrowseScreen(model: model)) { _ in TMChrome(title: "Browse") }
        model.pages.forEach { $0.owner = controller }
        controller.listing = model
        navigator.controller = controller
        return controller
    }

    /// A results screen over tags already in hand, which fetches nothing more.
    @objc(resultsScreenWithTags:) nonisolated static func results(tags: [DPTag]) -> UIViewController {
        MainActor.assumeIsolated { results(TMTagQuery(), preloaded: tags) }
    }

    static func results(_ query: TMTagQuery, navigator custom: TMNavigator? = nil,
                        catalog: TMCatalog = .live, preloaded: [DPTag]? = nil) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMQueryModel(query: query, catalog: catalog, navigator: custom ?? navigator, preloaded: preloaded)
        let controller = TMHostingController(TMQueryScreen(model: model)) { _ in TMChrome(title: model.title) }
        controller.onAppear = { _ in model.syncSelection() }
        model.owner = controller
        controller.listing = model
        navigator.controller = controller
        return controller
    }

    static func search(navigator custom: TMNavigator? = nil) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMSearchModel(navigator: custom ?? navigator)
        // Runs the search with the current text and options; also the way to search by options alone.
        let run = TMBarItems.symbol("magnifyingglass") { [weak model] in model?.search() }
        let controller = TMHostingController(TMSearchScreen(model: model)) { _ in
            TMChrome(title: "Search", right: [run])
        }
        navigator.controller = controller
        controller.searchModel = model
        controller.pageColor = .systemGroupedBackground
        return controller
    }

    static func settings(navigator custom: TMNavigator? = nil, account: TMAccount = .firebase,
                         build: TMBuildInfo = .bundle) -> TMHostingController {
        let navigator = TMUIKitNavigator()
        let model = TMSettingsModel(navigator: custom ?? navigator, account: account, build: build)
        let controller = TMHostingController(TMSettingsScreen(model: model)) { _ in TMChrome(title: "Settings") }
        controller.onAppear = { _ in model.refresh() }
        navigator.controller = controller
        controller.settingsModel = model
        controller.pageColor = .systemGroupedBackground
        return controller
    }

    // MARK: - Routing

    /// The screen for a destination another screen asked for.
    static func controller(for destination: TMDestination) -> UIViewController {
        switch destination {
        case .browse: return browse()
        case .search: return search()
        case .settings: return settings()
        case .teachable: return teachable()
        case .list(let key): return list(key: key)
        case .results(let query): return results(query)
        }
    }

    /// Whether `controller` is the list screen for `key` (Home's list rows reuse it).
    @objc(isListScreen:forKey:) nonisolated static func isListScreen(_ controller: UIViewController, key: String) -> Bool {
        MainActor.assumeIsolated {
            guard let hosting = controller as? TMHostingController,
                  let model = hosting.listing as? TMTagListModel else { return false }
            return model.key == key
        }
    }
}
