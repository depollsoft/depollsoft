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

    // MARK: - Routing

    /// The screen for a destination another screen asked for.
    static func controller(for destination: TMDestination) -> UIViewController {
        switch destination {
        case .browse: return DPBrowseViewController()
        case .search: return DPSearchViewController()
        case .settings: return DPSettingsController()
        case .teachable: return teachable()
        case .list(let key): return list(key: key)
        case .results(let query): return query.makeLegacyController()
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

extension TMTagQuery {
    func makeLegacyController() -> UIViewController {
        let controller = DPTagQueryViewController()
        controller.query = text
        controller.sortBy = sortBy
        controller.collection = collection
        controller.parts = parts.map { NSNumber(value: $0) }
        controller.hasLearningTracks = learningTracks.map { NSNumber(value: $0) }
        controller.hasSheetMusic = sheetMusic.map { NSNumber(value: $0) }
        return controller
    }
}
